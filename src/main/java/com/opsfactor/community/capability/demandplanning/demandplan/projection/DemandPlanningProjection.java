package com.opsfactor.community.capability.demandplanning.demandplan.projection;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem.DemandPlanItemKey;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.HistoricoDemandPlanItem;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionPorDfu;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.IncompatibleCalendarException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.ModificacaoAgregadaPlano;
import com.opsfactor.community.platform.utility.Constantes.TipoDemanda;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Quartet;

import jakarta.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Projection em memoria de um Demand Plan.
 *
 * <p>No Community, esta projection e a estrutura central usada pelo Planning
 * Book para consultar e alterar valores em nivel material/location. Ela ainda
 * conhece campos transicionais de Uplift/New Materials porque a entidade fisica
 * e compartilhada com o desenho Enterprise, mas a implementacao Community desta
 * projection bloqueia a escrita funcional desses campos em
 * {@link #validaEscritaKeyFigureEnterpriseCommunity}. Leitura tecnica permanece
 * possivel para compatibilidade com projections, neutralizacoes defensivas e
 * overlays Enterprise que ja migraram leitura ou totalizacao de uma coluna
 * privada sem reabrir escrita no Community.</p>
 *
 * <p>A projection e indexada por periodo, location e material para evitar
 * varreduras repetidas durante calculos e ajustes. Os mapas usam estruturas
 * concorrentes porque o Demand Planning continua processando clusters em
 * paralelo.</p>
 */
@Slf4j
@EqualsAndHashCode(of = "demandPlan")
public class DemandPlanningProjection {

    /**
     * Componentes fisicos que podem receber escrita direta na linha de Demand
     * Plan.
     *
     * <p>No Community, somente `BASELINE` e `AJUSTE_DEMANDA` sao editaveis de
     * verdade. `ITENS_NOVOS` e `UPLIFT` permanecem aqui apenas para permitir
     * neutralizacao defensiva com valor zero em bases transicionais; valor
     * diferente de zero continua exigindo Enterprise.</p>
     */
    private static final Set<Constantes.TipoDemanda> TIPOS_DEMANDA_ESCRITA_DEMAND_PLAN_LINHA = Set.of(
            Constantes.TipoDemanda.BASELINE,
            Constantes.TipoDemanda.ITENS_NOVOS,
            Constantes.TipoDemanda.UPLIFT,
            Constantes.TipoDemanda.AJUSTE_DEMANDA);

    /**
     * A entidade de Demand Plan armazena somente plano irrestrito e restrito.
     * Working plan, historico, budget e nao-atendido sao series/visoes
     * externas a esta escrita direta.
     */
    private static final Set<Constantes.TipoPlano> TIPOS_PLANO_ESCRITA_DEMAND_PLAN_LINHA = Set.of(
            Constantes.TipoPlano.PLANO_IRRESTRITO,
            Constantes.TipoPlano.PLANO_RESTRITO);

    /**
     * Variantes fisicas que possuem total funcional Community na projection.
     *
     * <p>O conjunto e mantido separado do conjunto de escrita para deixar claro
     * que a leitura de `TOTAL` tambem e uma regra funcional da projection, nao
     * um getter fisico da entidade.</p>
     */
    private static final Set<Constantes.TipoPlano> TIPOS_PLANO_LEITURA_TOTAL_DEMAND_PLAN_LINHA = Set.of(
            Constantes.TipoPlano.PLANO_IRRESTRITO,
            Constantes.TipoPlano.PLANO_RESTRITO);

    @Getter
    private final DemandPlan demandPlan;
    @Getter
    private final ClusterEParametrosProjection clusterEParametrosProjection;
    @Getter
    private final UnidadeMedidaProjection unidadeMedidaProjection;
    @Getter
    private final ParametrosDemandPlanProjection parametrosDemandPlanProjection;
    
    @Getter
    @Setter
    private Calendario calendario; // calendário usado para extração dos dados

    // Agora se usa o FiltroDFUProjection, que trata combinacoes completas ou filtradas de material/location.
    @Getter
    private final FiltroDFUProjection filtroDfuProjection;
    
    @Getter
    @Setter
    private boolean planoClientesFinaisConsolidadoEmLocationsInternasOuRegioesComerciais;
    @Getter
    @Setter
    @Nullable
    private PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda modoConsolidacaoDemandaClientesFinais;
    @Getter
    @Setter
    @Nullable
    VersaoMalha versaoMalhaParaConsolidacaoDemandaClientesFinaisEmLocationsInternas;

    /**
     * Define qual unidade de medida deve ser usada em leitura/escrita tecnica.
     *
     * <p>`DP` usa a unidade configurada para o Demand Planning do cluster.
     * `MATERIAL_LOCATION` usa a unidade operacional padrao da combinacao
     * material/location.</p>
     */
    public enum TipoUnidadeMedidaConsiderada {
        DP, MATERIAL_LOCATION
    }

    // Indice principal: apenas um DemandPlanItem por periodo/location/material.
    private Map<Integer,Map<Location,Map<Produto,DemandPlanItem>>> mapaDemandPlanItems = new ConcurrentHashMap<>();
    // Historico salvo pelo forecast: apenas um HistoricoDemandPlanItem por periodo/location/material.
    private Map<Integer,Map<Location,Map<Produto,HistoricoDemandPlanItem>>> mapaHistoricoDemandPlanItems = new ConcurrentHashMap<>();

    public DemandPlanningProjection(DemandPlan demandPlan, 
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            Calendario calendario, 
            FiltroDFUProjection filtroDfuProjection,
            boolean planoClientesFinaisConsolidadoEmLocationsInternasOuRegioesComerciais,
            @Nullable PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda modoConsolidacaoDemandaClientesFinais,
            @Nullable VersaoMalha versaoMalhaParaConsolidacaoDemandaClientesFinaisEmLocationsInternas) {
        this.demandPlan = demandPlan;
        this.unidadeMedidaProjection = unidadeMedidaProjection;
        this.clusterEParametrosProjection = clusterEParametrosProjection;
        this.parametrosDemandPlanProjection = parametrosDemandPlanProjection;
        this.calendario = calendario;
        this.filtroDfuProjection = filtroDfuProjection;
        this.planoClientesFinaisConsolidadoEmLocationsInternasOuRegioesComerciais = planoClientesFinaisConsolidadoEmLocationsInternasOuRegioesComerciais;
        this.modoConsolidacaoDemandaClientesFinais = modoConsolidacaoDemandaClientesFinais;
        this.versaoMalhaParaConsolidacaoDemandaClientesFinaisEmLocationsInternas = versaoMalhaParaConsolidacaoDemandaClientesFinaisEmLocationsInternas;
    }

    // Carga incremental dos indices da projection.
    public void addDemandPlanItem(DemandPlanItem demandPlanItem) {


        Integer posicaoPeriodo = calendario.getPosicaoPeriodo(demandPlanItem.getDataReferencia());

        /*
         * A projection representa uma fotografia unica por periodo/location/
         * material. Sobrescrever a linha faria o Planning Book, a totalizacao
         * e a ponte Demand -> Supply dependerem da ordem de carga do snapshot.
         */
        Map<Produto, DemandPlanItem> demandPlanItemPorMaterial = mapaDemandPlanItems
                .computeIfAbsent(posicaoPeriodo, x -> new ConcurrentHashMap<>())
                .computeIfAbsent(demandPlanItem.getLocation(), x -> new ConcurrentHashMap<>());
        DemandPlanItem demandPlanItemAnterior = demandPlanItemPorMaterial.putIfAbsent(
                demandPlanItem.getProduto(),
                demandPlanItem);
        if (demandPlanItemAnterior != null && demandPlanItemAnterior != demandPlanItem) {
            throw new IllegalStateException(
                    "Demand Planning projection already has a Demand Plan line for period "
                            + posicaoPeriodo
                            + ", location "
                            + demandPlanItem.getLocation().getId()
                            + " and material "
                            + demandPlanItem.getProduto().getId()
                            + ".");
        }

    }

    /**
     * Valida uma linha de Demand Plan antes de indexacao ou escrita direta.
     *
     * <p>Ausencia de linha em uma consulta pontual pode representar falta real
     * de dado e continua retornando zero nos getters. Ja adicionar ou escrever
     * uma linha nula/quebrada corromperia os mapas internos da projection, entao
     * falhamos com mensagem de contrato antes do `ConcurrentHashMap` ou do
     * calendario.</p>
     */
    /**
     * Valida uma linha historica antes de indexar trend/seasonal do forecast.
     *
     * <p>Historico ausente em uma consulta continua sendo ausencia operacional e
     * retorna zero. Uma linha historica nula ou sem chave durante a carga da
     * projection e snapshot quebrado e precisa falhar antes de entrar nos mapas
     * concorrentes.</p>
     */
    public DemandPlanItem getOrAddDemandPlanItem(Location location, Produto material, int posicaoPeriodo, UnidadeMedida unidadeMedida) {

        DemandPlanItem demandPlanItem = getDemandPlanItem(location, material, posicaoPeriodo);
        if (demandPlanItem == null) {
            demandPlanItem = new DemandPlanItem(new DemandPlanItemKey(demandPlan, location, material, getCalendario().getUltimoSegundoPeriodo(posicaoPeriodo)));
            demandPlanItem.setUnidadeMedida(unidadeMedida);
            addDemandPlanItem(demandPlanItem);
        }
        return demandPlanItem;

    }
    public void addHistoricoDemandPlanItem(HistoricoDemandPlanItem historicoDemandPlanItem) {


        Integer posicaoPeriodo = calendario.getPosicaoPeriodo(historicoDemandPlanItem.getDataReferencia());

        /*
         * Historico do forecast tambem e chave unica por periodo/location/
         * material. Uma segunda linha com a mesma chave misturaria trend,
         * sazonalidade e forecast salvo sem indicar qual origem venceu.
         */
        Map<Produto, HistoricoDemandPlanItem> historicoDemandPlanItemPorMaterial = mapaHistoricoDemandPlanItems
                .computeIfAbsent(posicaoPeriodo, x -> new ConcurrentHashMap<>())
                .computeIfAbsent(historicoDemandPlanItem.getLocation(), x -> new ConcurrentHashMap<>());
        HistoricoDemandPlanItem historicoDemandPlanItemAnterior = historicoDemandPlanItemPorMaterial.putIfAbsent(
                historicoDemandPlanItem.getProduto(),
                historicoDemandPlanItem);
        if (historicoDemandPlanItemAnterior != null
                && historicoDemandPlanItemAnterior != historicoDemandPlanItem) {
            throw new IllegalStateException(
                    "Demand Planning projection already has a historical Demand Plan line for period "
                            + posicaoPeriodo
                            + ", location "
                            + historicoDemandPlanItem.getLocation().getId()
                            + " and material "
                            + historicoDemandPlanItem.getProduto().getId()
                            + ".");
        }

    }

    /*
     * Consulta indexada de linhas de Demand Plan e Historico por
     * periodo/location/material. Linha ausente aqui e ausencia operacional, nao
     * snapshot quebrado, por isso os getters superiores podem retornar zero.
     */
    public DemandPlanItem getDemandPlanItem(Location location, Produto material, int posicaoPeriodo) {

        return mapaDemandPlanItems
                .getOrDefault(posicaoPeriodo, new ConcurrentHashMap<>())
                .getOrDefault(location, new ConcurrentHashMap<>())
                .get(material);
    }
    public HistoricoDemandPlanItem getHistoricoDemandPlanItem(Location location, Produto material, int posicaoPeriodo) {

        return mapaHistoricoDemandPlanItems
                .getOrDefault(posicaoPeriodo, new ConcurrentHashMap<>())
                .getOrDefault(location, new ConcurrentHashMap<>())
                .get(material);
    }

    /**
     * Retorna todos os demand plan linhas para um dado período
     * @param posicaoPeriodo
     * @return
     */
    public Set<DemandPlanItem> getDemandPlanItems(int posicaoPeriodo) {

        return mapaDemandPlanItems
                .getOrDefault(posicaoPeriodo, new ConcurrentHashMap<>())
                .values().stream()
                .flatMap(subMapaProdutos -> subMapaProdutos.values().stream())
                .collect(Collectors.toSet());

    }

    /**
     * Retorna todos os demand plan linhas para uma location
     * @param location
     * @return
     */
    public Set<DemandPlanItem> getDemandPlanItems(Location location) {
        return mapaDemandPlanItems
                .values()
                .stream()
                .flatMap(subMapaLocations -> subMapaLocations
                        .getOrDefault(location, new HashMap<>())
                        .values()
                        .stream())
                .collect(Collectors.toSet());
    }

    /**
     * Retorna todos os demand plan linhas para um dado período
     * @param posicaoPeriodo
     * @return 
     */
    public Set<DemandPlanItem> getDemandPlanItems(Produto material, int posicaoPeriodo) {
        
        return mapaDemandPlanItems
                .getOrDefault(posicaoPeriodo, new ConcurrentHashMap<>())
                .values().stream()
                .map(subMapaProduto -> subMapaProduto.get(material))
                .filter(demandPlanItem -> demandPlanItem != null)
                .collect(Collectors.toSet());
    }
    
    /**
     * Retorna valor do demand plan para periodo do calendario target (que pode ser diferente do calendario DP)
     * @param splitTemporalProjectionPorDfu
     * @param posicaoPeriodoCalendarioTarget
     * @param location
     * @param material
     * @param tipoDemanda
     * @param tipoPlano
     * @param unidadeMedida
     * @return 
     */
    public double getValorDemandPlanItemNoCalendarioTargetSplitTemporal(
            SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu,
            int posicaoPeriodoCalendarioTarget, 
            Location location, Produto material,
            Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano, 
            UnidadeMedida unidadeMedida) {
        
        return splitTemporalProjectionPorDfu.getValorNoCalendarioTargetSplitTemporal(
                location, material,
                posicaoPeriodoCalendarioDP -> getValorDemandPlanItem(
                        posicaoPeriodoCalendarioDP, location, material, tipoDemanda, tipoPlano, unidadeMedida), 
                posicaoPeriodoCalendarioTarget);
        
    }

    /**
     * Extração de componentes trend/seasonal
     * Períodos passados : extrai de HistoricoDemandPlanItem
     * Períodos futuros : extrai de DemandPlanItem
     * @param splitTemporalProjectionPorDfu
     * @param posicaoPeriodoCalendarioTarget
     * @param location
     * @param material
     * @param trendSeasonal
     * @param unidadeMedida
     * @return
     * @throws IncompatibleCalendarException
     * @throws UnitOfMeasureConversionException 
     */
    public double getValorDemandPlanItemNoCalendarioTargetSplitTemporal(
            SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu,
            int posicaoPeriodoCalendarioTarget, 
            Location location, Produto material,
            Constantes.TrendSeasonal trendSeasonal, 
            UnidadeMedida unidadeMedida) throws IncompatibleCalendarException, UnitOfMeasureConversionException {

        return splitTemporalProjectionPorDfu.getValorNoCalendarioTargetSplitTemporal(
                location, material,
                posicaoPeriodoCalendarioDPOriginal -> {
                    if (posicaoPeriodoCalendarioDPOriginal <= calendario.getPosicaoPeriodoFinalPassado()) {
                        return getValorHistoricoDemandPlanItem(posicaoPeriodoCalendarioDPOriginal, location, material, trendSeasonal, unidadeMedida);
                    } else {
                        return getValorDemandPlanItem(posicaoPeriodoCalendarioDPOriginal, location, material, trendSeasonal, unidadeMedida);
                    }
                },
                posicaoPeriodoCalendarioTarget);

    }
    
    public void modificaValorDemandPlanItemNoCalendarioTargetSplitTemporal(
            double modificacaoNoCalendarioTarget,
            SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu,
            int posicaoPeriodoCalendarioTarget, 
            Location location, Produto material,
            Constantes.TipoPlano tipoPlano, 
            UnidadeMedida unidadeMedidaValor,
            ModificacaoAgregadaPlano modificacaoAgregadaPlano) throws IncompatibleCalendarException, UnitOfMeasureConversionException {
                
        // Mapa posicaoPeriodoCalendarioDP -> 
        Map<Integer,Double> mapaParticipacaoPeriodosCalendarioDP = splitTemporalProjectionPorDfu
                .getSplitTemporalProjectionCurva(location, material)
                .getMapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem()
                .getOrDefault(posicaoPeriodoCalendarioTarget, new HashMap<>());
        
        // Mapa posicaoPeriodoCalendarioDP -> valor do plano no período DP
        Map<Integer,Double> mapaPlanoPorPeriodoDPCoberto = mapaParticipacaoPeriodosCalendarioDP.entrySet().stream() //.getMapaSetPeriodosTargetDentroDePeriodoOrigem().getOrDefault(posicaoPeriodoCalendarioTarget, new HashSet<>()).stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(), 
                        // por ex. se periodoCalendarioTarget cobre apenas 30% do período DP, retorna 30% da demanda do período DP
                        entry -> entry.getValue() * getValorDemandPlanItem(
                                entry.getKey(), location, material, TipoDemanda.TOTAL, tipoPlano, unidadeMedidaValor)));
                
        if (mapaPlanoPorPeriodoDPCoberto.isEmpty()) return;
        
        double valorDPTotalPeriodosCobertos = mapaPlanoPorPeriodoDPCoberto.values()
                .stream()
                .mapToDouble(x -> x)
                .sum();

        // ajuste não pode ser maior que o próprio plano
        if (modificacaoNoCalendarioTarget < 0) modificacaoNoCalendarioTarget = -Math.min(-modificacaoNoCalendarioTarget, valorDPTotalPeriodosCobertos);
        
        // aplica modificação no calendario target (SNP) proporcionalmente a cada período do calendário origem (DP)
        for (Integer posicaoPeriodoDPCoberto : mapaPlanoPorPeriodoDPCoberto.keySet()) {
            
            double valorPlanoNoPeriodoDP = mapaPlanoPorPeriodoDPCoberto.get(posicaoPeriodoDPCoberto);
            // a modificação só se aplica ao % da demanda coberta pelo período do calendário target
            double modificacaoAAplicarPeriodoDP = modificacaoNoCalendarioTarget * valorPlanoNoPeriodoDP / valorDPTotalPeriodosCobertos;
            
            DemandPlanItem demandPlanItem = getDemandPlanItem(location, material, posicaoPeriodoDPCoberto);
            double valorDemandaAtual = getQuantidadeDemandPlanItemCommunity(
                    demandPlanItem,
                    TipoDemanda.TOTAL,
                    tipoPlano);
            demandPlanItem.setQuantidadeTotal(valorDemandaAtual + modificacaoAAplicarPeriodoDP, tipoPlano, modificacaoAgregadaPlano);
            
        }
        
    }

    /**
     * Retorna a quantidade de uma linha segundo o contrato funcional Community.
     *
     * <p>O schema transicional ainda possui campos de Uplift e New Materials, mas
     * esses campos pertencem ao OpsFactor Enterprise. Por isso, quando um fluxo
     * Community pede {@link TipoDemanda#TOTAL}, o total considerado e somente
     * Baseline + Demand Adjustment. Os demais tipos continuam legiveis porque
     * alguns pontos tecnicos precisam neutralizar dados Enterprise existentes em
     * bases transicionais.</p>
     */
    private double getQuantidadeDemandPlanItemCommunity(
            DemandPlanItem demandPlanItem,
            TipoDemanda tipoDemanda,
            Constantes.TipoPlano tipoPlano) {

        validaTipoDemandaLeituraDemandPlanItem(tipoDemanda);
        validaTipoPlanoLeituraDemandPlanItem(tipoPlano);

        if (demandPlanItem == null) return 0;

        if (tipoDemanda.equals(TipoDemanda.TOTAL)) {
            return getQuantidadeTotalDemandPlanItemCommunity(demandPlanItem, tipoPlano);
        }

        return demandPlanItem.getQuantidade(tipoDemanda, tipoPlano);

    }

    /**
     * Total funcional Community de uma linha de Demand Planning.
     *
     * <p>Este helper e intencionalmente local a projection para evitar que a
     * entidade passe a esconder campos Enterprise em getters fisicos. A entidade
     * continua representando a linha completa; a projection representa a leitura
     * funcional disponivel na edicao Community.</p>
     */
    private double getQuantidadeTotalDemandPlanItemCommunity(
            DemandPlanItem demandPlanItem,
            Constantes.TipoPlano tipoPlano) {

        return switch (tipoPlano) {
            case PLANO_IRRESTRITO -> demandPlanItem.getQuantidadeBaseline()
                    + demandPlanItem.getQuantidadeAjusteDemanda();
            case PLANO_RESTRITO -> demandPlanItem.getQuantidadeBaselineAtendida()
                    + demandPlanItem.getQuantidadeAjusteDemandaAtendida();
            default -> throw getUnsupportedTipoPlanoLeituraTotalException(tipoPlano);
        };

    }
    
    // Leitura, conversao e escrita de valores da projection.
    public double getValorDemandPlanItem( 
            DemandPlanItem demandPlanItem,
            Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida) throws UnitOfMeasureConversionException {
        
        if (demandPlanItem == null) return 0;
        
        double quantidadeRetorno = getQuantidadeDemandPlanItemCommunity(demandPlanItem, tipoDemanda, tipoPlano);
        // converte quantidade no demand plan linha para a unidade de referência do projection
                
        return quantidadeRetorno * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                demandPlanItem.getProduto(), demandPlanItem.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais()), 
                unidadeMedida);
        
    }
    
    public double getValorDemandPlanItem(int posicaoPeriodo, Location location, Produto material, 
            Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida) throws UnitOfMeasureConversionException {
        
        DemandPlanItem demandPlanItem = getDemandPlanItem(location, material, posicaoPeriodo);
        
        return getValorDemandPlanItem(demandPlanItem, tipoDemanda, tipoPlano, unidadeMedida);
        
    }

    public double getValorDemandPlanItem(
            int posicaoPeriodoInicial, int posicaoPeriodoFinal,
            Location location, Produto material,
            Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida) throws UnitOfMeasureConversionException {

        return IntStream.rangeClosed(posicaoPeriodoInicial, posicaoPeriodoFinal)
                .mapToDouble(posicao -> getValorDemandPlanItem(
                        posicao, location,  material,
                        tipoDemanda, tipoPlano,
                        unidadeMedida))
                .sum();

    }

    public double getValorHistoricoDemandPlanItem(int posicaoPeriodo, Location location, Produto material, 
            Constantes.TrendSeasonal trendSeasonal,
            UnidadeMedida unidadeMedida) throws UnitOfMeasureConversionException {
        
        HistoricoDemandPlanItem historicoDemandPlanItem = getHistoricoDemandPlanItem(location, material, posicaoPeriodo);
        if (historicoDemandPlanItem == null) return 0;
        
        double quantidadeRetorno = 0f;
        switch (trendSeasonal) {
            case TREND:
                quantidadeRetorno = historicoDemandPlanItem.getQuantidadeTrend();
                break;
            case SEASONAL:
                quantidadeRetorno = historicoDemandPlanItem.getQuantidadeSeasonal();
                break;
            case TREND_E_SEASONAL:
                quantidadeRetorno = historicoDemandPlanItem.getQuantidadeTrend() + historicoDemandPlanItem.getQuantidadeSeasonal();
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported TrendSeasonal value while reading historical Demand Plan item: "
                                + trendSeasonal);
        }

        // converte quantidade no demand plan linha para a unidade de referência do projection                
        return quantidadeRetorno * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                material, historicoDemandPlanItem.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais()), 
                unidadeMedida);
        
    }
    
    public double getValorDemandPlanItem(int posicaoPeriodo, Location location, Produto material, 
            Constantes.TrendSeasonal trendSeasonal,
            UnidadeMedida unidadeMedida) throws UnitOfMeasureConversionException {
        
        DemandPlanItem demandPlanItem = getDemandPlanItem(location, material, posicaoPeriodo);
        if (demandPlanItem == null) return 0;
        
        return demandPlanItem.getQuantidadeTrendSeason(posicaoPeriodo, location, material, trendSeasonal, unidadeMedida, unidadeMedidaProjection);
        
    }
    
    public UnidadeMedida getUnidadeMedidaConsiderada(Location location, Produto material, Integer posicaoPeriodo, TipoUnidadeMedidaConsiderada tipoUnidadeMedidaConsiderada) {
        switch (tipoUnidadeMedidaConsiderada) {
            case DP:
                DemandPlanItem demandPlanItemReferencia = getDemandPlanItem(location, material, posicaoPeriodo);
                if (demandPlanItemReferencia != null) {
                    return demandPlanItemReferencia.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais());
                } else {
                    ClusterLocations clusterLocations = clusterEParametrosProjection.getClusterLocationsDeLocation(location);
                    ClusterMateriais clusterMateriaisDemandPlanning =
                            clusterEParametrosProjection.getClusterMateriaisDemandPlanning(material, location);
                    return parametrosDemandPlanProjection
                            .getParametrosDemandPlanNivelClusterProjection(clusterLocations, clusterMateriaisDemandPlanning)
                            .getParametrosGeraisDemandPlanningProjection()
                            .getUnidadeMedidaDP();
                }
            case MATERIAL_LOCATION:
                return clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, location);
            default:
                return clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, location);
        }
    }

    public void setValorDemandPlanItemEmKeyFigureStandardDp(
            DemandPlanItem demandPlanItem, 
            double valor,
            Constantes.TipoDemanda tipoDemanda,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaInput) {

        validaTipoDemandaEscritaDemandPlanItem(tipoDemanda);
        validaTipoPlanoEscritaDemandPlanItem(tipoPlano);
        demandPlanItem = demandPlanItem;
                
        Location location = demandPlanItem.getLocation();
        Produto material = demandPlanItem.getProduto();
        Integer posicaoPeriodo = calendario.getPosicaoPeriodo(demandPlanItem.getDataReferencia());
        
        if (Math.abs(valor) < 0.00005) valor = 0f;
        
        // arredonda para múltiplo de vendas
        if (clusterEParametrosProjection.getParametrosGlobais().getDpArredondaParaUnidadeVenda() && valor > 0) {
            UnidadeMedida unidadeMedidaVendas = clusterEParametrosProjection.getDPUnidadeVendas(
                    demandPlanItem.getProduto());

            double quantidadeNaUnidadeMedidaVendas = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                    demandPlanItem.getProduto(), 
                    unidadeMedidaInput, 
                    unidadeMedidaVendas) * valor;

            // divide pelo valor não-arredondado na unidade de medida vendas e multiplica pelo valor arredondado para cima
            valor = valor / quantidadeNaUnidadeMedidaVendas * (int) Math.ceil(quantidadeNaUnidadeMedidaVendas);

        }
        
        if (!demandPlan.getTamanhoBucket().equals(calendario.getTamanhoBucket())) {
            throw new IncompatibleCalendarException("Calendar time bucket different from demand plan time bucket");
        }
        
        UnidadeMedida unidadeMedidaDP = getUnidadeMedidaConsiderada(location, material, posicaoPeriodo, TipoUnidadeMedidaConsiderada.DP);
        
        Double conversaoParaUnidadeMedidaDP;
        try {
            conversaoParaUnidadeMedidaDP = unidadeMedidaProjection.getConversaoParaUnidadeDestino(material, unidadeMedidaInput, unidadeMedidaDP);
        } catch (UnitOfMeasureConversionException unitOfMeasureConversionException) {
            throw new UnitOfMeasureConversionException("No conversion found from input UOM " + unidadeMedidaInput.getId() + " to configured DP UOM " + unidadeMedidaDP.getId()
                    + " for material " + material.getId() + " and location " + location.getId(),
                    unitOfMeasureConversionException);
        }
        
        double valorNaUnidadeMedidaDP = valor * conversaoParaUnidadeMedidaDP;
        validaEscritaKeyFigureEnterpriseCommunity(tipoDemanda, valorNaUnidadeMedidaDP);

        // set é sempre feito na unidade de medida do projection
        demandPlanItem.setUnidadeMedida(unidadeMedidaDP);
        
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                switch (tipoDemanda) {
                    case BASELINE:
                        demandPlanItem.setQuantidadeBaseline(valorNaUnidadeMedidaDP) ;
                        break;
                    case ITENS_NOVOS:
                        demandPlanItem.setQuantidadeItensNovos(valorNaUnidadeMedidaDP);
                        break;
                    case UPLIFT:
                        demandPlanItem.setQuantidadeUplift(valorNaUnidadeMedidaDP);
                        break;
                    case AJUSTE_DEMANDA:
                        demandPlanItem.setQuantidadeAjusteDemanda(valorNaUnidadeMedidaDP);
                        break;
                    default:
                        throw getUnsupportedTipoDemandaEscritaException(tipoDemanda);
                }
                break;
            case PLANO_RESTRITO:
                switch (tipoDemanda) {
                    case BASELINE:
                        demandPlanItem.setQuantidadeBaselineAtendida(valorNaUnidadeMedidaDP);
                        break;
                    case ITENS_NOVOS:
                        demandPlanItem.setQuantidadeItensNovosAtendida(valorNaUnidadeMedidaDP);
                        break;
                    case UPLIFT:
                        demandPlanItem.setQuantidadeUpliftAtendida(valorNaUnidadeMedidaDP);
                        break;
                    case AJUSTE_DEMANDA:
                        demandPlanItem.setQuantidadeAjusteDemandaAtendida(valorNaUnidadeMedidaDP);
                        break;
                    default:
                        throw getUnsupportedTipoDemandaEscritaException(tipoDemanda);
                }            
                break;
            default:
                throw getUnsupportedTipoPlanoEscritaException(tipoPlano);
        }
    }

    public Quartet<DemandPlanItem,Constantes.TipoDemanda,Double,Double> modificaValorDemandPlanItem(
            int posicaoPeriodo, Location location, Produto material,
            double variacao, Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaInput) {

        double valorAtualDemandPlanItem = getValorDemandPlanItem(
                posicaoPeriodo, location, material,
                tipoDemanda, tipoPlano,
                unidadeMedidaInput);

        return setValorDemandPlanItemEmKeyFigureStandardDp(
                posicaoPeriodo, location, material,
                valorAtualDemandPlanItem + variacao,
                tipoDemanda, tipoPlano,
                unidadeMedidaInput);

    }

    /**
     * Seta um valor (quantidade) na unidade de medida do projection
     * @param posicaoPeriodo
     * @param location
     * @param material
     * @param valor
     * @param tipoDemanda
     * @param tipoPlano
     * @param unidadeMedidaInput
     * @return triplet com : 1) demand plan linha modificado 2) key figure afetada 3) valor original 3) novo valor
     */
    public Quartet<DemandPlanItem,Constantes.TipoDemanda,Double,Double> setValorDemandPlanItemEmKeyFigureStandardDp(
            int posicaoPeriodo,
            Location location, Produto material,
            double valor,
            Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaInput) {

        validaTipoDemandaEscritaDemandPlanItem(tipoDemanda);
        validaTipoPlanoEscritaDemandPlanItem(tipoPlano);

        if (Math.abs(valor) < 0.00005) valor = 0f;

        if (!demandPlan.getTamanhoBucket().equals(calendario.getTamanhoBucket())) {
            throw new IncompatibleCalendarException("Calendar time bucket different from demand plan time bucket");
        }
        
        // arredonda para múltiplo de vendas
        if (clusterEParametrosProjection.getParametrosGlobais().getDpArredondaParaUnidadeVenda() && valor > 0) {
            UnidadeMedida unidadeMedidaVendas = clusterEParametrosProjection.getDPUnidadeVendas(material);

            double quantidadeNaUnidadeMedidaVendas = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                    material, 
                    unidadeMedidaInput, 
                    unidadeMedidaVendas) * valor;

            // divide pelo valor não-arredondado na unidade de medida vendas e multiplica pelo valor arredondado para cima
            valor = valor / quantidadeNaUnidadeMedidaVendas * (int) Math.ceil(quantidadeNaUnidadeMedidaVendas);

        }
        
        UnidadeMedida unidadeMedidaDP = getUnidadeMedidaConsiderada(location, material, posicaoPeriodo, TipoUnidadeMedidaConsiderada.DP);
        
        DemandPlanItem demandPlanItem = getDemandPlanItem(location, material, posicaoPeriodo);
        if (demandPlanItem == null) {
            demandPlanItem = new DemandPlanItem(new DemandPlanItemKey(demandPlan, location, material, getCalendario().getUltimoSegundoPeriodo(posicaoPeriodo)));
            demandPlanItem.setUnidadeMedida(unidadeMedidaDP);
            addDemandPlanItem(demandPlanItem);
        }
        
        Double conversaoValorParaUnidadeMedidaDP;
        try {
            conversaoValorParaUnidadeMedidaDP = unidadeMedidaProjection.getConversaoParaUnidadeDestino(material, unidadeMedidaInput, unidadeMedidaDP);
        } catch (UnitOfMeasureConversionException unitOfMeasureConversionException) {
            throw new UnitOfMeasureConversionException("No conversion found from input UOM " + unidadeMedidaInput.getId() + " to configured DP UOM " + unidadeMedidaDP.getId()
                    + " for material " + material.getId() + " and location " + location.getId(),
                    unitOfMeasureConversionException);
        }
        
        // set é sempre feito na unidade de medida do projection
        demandPlanItem.setUnidadeMedida(unidadeMedidaDP);
        double valorOriginalNaUnidadeMedidaDP = demandPlanItem.getQuantidadeNaUnidadeMedidaTarget(tipoDemanda, tipoPlano, unidadeMedidaDP, unidadeMedidaProjection);
        double valorNovoNaUnidadeMedidaDP = valor * conversaoValorParaUnidadeMedidaDP;
        validaEscritaKeyFigureEnterpriseCommunity(tipoDemanda, valorNovoNaUnidadeMedidaDP);
        
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                switch (tipoDemanda) {
                    case BASELINE:
                        demandPlanItem.setQuantidadeBaseline(valorNovoNaUnidadeMedidaDP) ;
                        break;
                    case ITENS_NOVOS:
                        demandPlanItem.setQuantidadeItensNovos(valorNovoNaUnidadeMedidaDP);
                        break;
                    case UPLIFT:
                        demandPlanItem.setQuantidadeUplift(valorNovoNaUnidadeMedidaDP);
                        break;
                    case AJUSTE_DEMANDA:
                        demandPlanItem.setQuantidadeAjusteDemanda(valorNovoNaUnidadeMedidaDP);
                        break;
                    default:
                        throw getUnsupportedTipoDemandaEscritaException(tipoDemanda);
                }
                break;
            case PLANO_RESTRITO:
                switch (tipoDemanda) {
                    case BASELINE:
                        demandPlanItem.setQuantidadeBaselineAtendida(valorNovoNaUnidadeMedidaDP);
                        break;
                    case ITENS_NOVOS:
                        demandPlanItem.setQuantidadeItensNovosAtendida(valorNovoNaUnidadeMedidaDP);
                        break;
                    case UPLIFT:
                        demandPlanItem.setQuantidadeUpliftAtendida(valorNovoNaUnidadeMedidaDP);
                        break;
                    case AJUSTE_DEMANDA:
                        demandPlanItem.setQuantidadeAjusteDemandaAtendida(valorNovoNaUnidadeMedidaDP);
                        break;
                    default:
                        throw getUnsupportedTipoDemandaEscritaException(tipoDemanda);
                }            
                break;
            default:
                throw getUnsupportedTipoPlanoEscritaException(tipoPlano);
        }
        
        // gera o output (usado para log ajustes)
        return Quartet.with(
                demandPlanItem,
                tipoDemanda,
                valorOriginalNaUnidadeMedidaDP,
                valorNovoNaUnidadeMedidaDP);
        
    }
    
    public double getValorDemandPlanItem(int posicaoPeriodo, 
            Collection<Location> locationsTarget, Collection<Produto> materiaisTarget, 
            Constantes.TipoDemanda tipoDemanda, com.opsfactor.community.platform.utility.Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Map<Location,Map<Produto,DemandPlanItem>> subMapaParaPosicaoPeriodo = mapaDemandPlanItems
                .getOrDefault(posicaoPeriodo, new HashMap<>());
        
        double valorAcumulado = 0f;
        
        for (Location location : locationsTarget) {
            
            Map<Produto,DemandPlanItem> subMapaParaPosicaoPeriodoELocation = subMapaParaPosicaoPeriodo.getOrDefault(location, new HashMap<>());
            if (subMapaParaPosicaoPeriodoELocation.isEmpty()) continue;
            
            for (Produto material : materiaisTarget) {
                
                DemandPlanItem demandPlanItem = subMapaParaPosicaoPeriodoELocation.get(material);
                
                if (demandPlanItem != null) {
                    valorAcumulado += 
                            getQuantidadeDemandPlanItemCommunity(demandPlanItem, tipoDemanda, tipoPlano)
                            * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                    material, 
                                    demandPlanItem.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais()), 
                                    unidadeMedidaTarget);
                    
                }
            }
        }
        
        return valorAcumulado;
        
    }
    
    public double getValorDemandPlanItem(
            int posicaoPeriodo, 
            Collection<DFU> dfusTarget, 
            Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        FiltroDFUProjection dfuProjection = new FiltroDFUProjection(dfusTarget, clusterEParametrosProjection);
                
        return getValorDemandPlanItem(posicaoPeriodo, dfuProjection, tipoDemanda, tipoPlano, unidadeMedidaTarget);
        
    }
    
    public double getValorDemandPlanItem(
            int posicaoPeriodo, 
            FiltroDFUProjection filtroDfuProjection,
            Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
                
        Map<Location,Map<Produto,DemandPlanItem>> subMapaParaPosicaoPeriodo = mapaDemandPlanItems
                .getOrDefault(posicaoPeriodo, new ConcurrentHashMap<>());
                
        return filtroDfuProjection.getStreamMateriaisPorLocation().mapToDouble(parLocationEMateriais -> {
            
            Location location = parLocationEMateriais.getValue0();
            
            Map<Produto,DemandPlanItem> subMapaParaPosicaoPeriodoELocation = subMapaParaPosicaoPeriodo.getOrDefault(location, new ConcurrentHashMap<>());
            if (subMapaParaPosicaoPeriodoELocation.isEmpty()) return 0f;
            
            return parLocationEMateriais.getValue1().stream()
                    .mapToDouble(material -> {
                            DemandPlanItem demandPlanItem = subMapaParaPosicaoPeriodoELocation.get(material);

                            if (demandPlanItem != null) {
                                return getQuantidadeDemandPlanItemCommunity(demandPlanItem, tipoDemanda, tipoPlano)
                                        * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                                material, 
                                                demandPlanItem.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais()), 
                                                unidadeMedidaTarget);
                            }
                            return 0f;
                    })
                    .sum();
                
        })
        .sum();
                
    }

    public double getValorDemandPlanItem(
            int posicaoPeriodo,
            FiltroDFUProjection filtroDfuProjection,
            Collection<KeyFigureInterface> keyFiguresTotalizacao,
            UnidadeMedida unidadeMedidaTarget) {

        keyFiguresTotalizacao.forEach(this::validaKeyFigureTotalizacaoDemandPlanningCommunity);

        Set<KeyFigureStandard> keyFigureStandardTotalizacaoSet = keyFiguresTotalizacao.stream()
                .filter(keyFigureInterface -> keyFigureInterface instanceof KeyFigureStandard)
                .map(keyFigureInterface -> (KeyFigureStandard) keyFigureInterface)
                .collect(Collectors.toSet());

        Map<Location,Map<Produto,DemandPlanItem>> subMapaParaPosicaoPeriodo = mapaDemandPlanItems
                .getOrDefault(posicaoPeriodo, new ConcurrentHashMap<>());
        return filtroDfuProjection.getStreamMateriaisPorLocation().parallel().mapToDouble(parLocationEMateriais -> {

            Location location = parLocationEMateriais.getValue0();

            Map<Produto,DemandPlanItem> subMapaParaPosicaoPeriodoELocation = subMapaParaPosicaoPeriodo.getOrDefault(location, new ConcurrentHashMap<>());
            if (subMapaParaPosicaoPeriodoELocation.isEmpty()) return 0f;

            return parLocationEMateriais.getValue1().stream()
                    .mapToDouble(material -> {
                            DemandPlanItem demandPlanItem = subMapaParaPosicaoPeriodoELocation.get(material);

                            if (demandPlanItem != null) {
                                return keyFigureStandardTotalizacaoSet.stream()
                                        .mapToDouble(keyFigureStandard -> demandPlanItem.getQuantidade(keyFigureStandard)
                                                * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                                material,
                                                demandPlanItem.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais()),
                                                unidadeMedidaTarget))
                                        .sum();
                            }
                            return 0f;
                    })
                    .sum();

        })
        .sum();

    }

    public double getValorKeyFigureStandardDpNaoTotalizadora(
            KeyFigureStandard keyFigureStandard,
            int posicaoPeriodo,
            Location location, Produto material,
            UnidadeMedida unidadeMedidaTarget) {
        /*
         * Metodo tecnico compartilhado por projections e rotinas internas. A
         * borda Community bloqueia Uplift/New Materials antes de expor ou editar
         * KFs; aqui os campos continuam legiveis apenas porque a tabela fisica
         * ainda e compartilhada/transicional.
         */
        return switch (keyFigureStandard.getKeyFigureStandardEnum()) {
            case BASELINE -> getValorDemandPlanItem(posicaoPeriodo, location, material, TipoDemanda.BASELINE, com.opsfactor.community.platform.utility.Constantes.TipoPlano.PLANO_IRRESTRITO, unidadeMedidaTarget);
            case ITENS_NOVOS -> getValorDemandPlanItem(posicaoPeriodo, location, material, TipoDemanda.ITENS_NOVOS, com.opsfactor.community.platform.utility.Constantes.TipoPlano.PLANO_IRRESTRITO, unidadeMedidaTarget);
            case UPLIFT -> getValorDemandPlanItem(posicaoPeriodo, location, material, TipoDemanda.UPLIFT, com.opsfactor.community.platform.utility.Constantes.TipoPlano.PLANO_IRRESTRITO, unidadeMedidaTarget);
            case AJUSTE_DEMANDA -> getValorDemandPlanItem(posicaoPeriodo, location, material, TipoDemanda.AJUSTE_DEMANDA, com.opsfactor.community.platform.utility.Constantes.TipoPlano.PLANO_IRRESTRITO, unidadeMedidaTarget);
            default -> throw getUnsupportedDemandPlanningPhysicalKeyFigureException(keyFigureStandard);
        };
    }
    public double getValorKeyFigureStandardDpNaoTotalizadora(
            DemandPlanItem demandPlanItem,
            KeyFigureStandard keyFigureStandard,
            UnidadeMedida unidadeMedidaTarget) {
        /*
         * Mesmo contrato do overload por periodo/location/material: Uplift e
         * Itens Novos sao campos Enterprise/transicionais e nao uma superficie
         * funcional do Community.
         */
        return switch (keyFigureStandard.getKeyFigureStandardEnum()) {
            case BASELINE -> getValorDemandPlanItem(demandPlanItem, TipoDemanda.BASELINE, com.opsfactor.community.platform.utility.Constantes.TipoPlano.PLANO_IRRESTRITO, unidadeMedidaTarget);
            case ITENS_NOVOS -> getValorDemandPlanItem(demandPlanItem, TipoDemanda.ITENS_NOVOS, com.opsfactor.community.platform.utility.Constantes.TipoPlano.PLANO_IRRESTRITO, unidadeMedidaTarget);
            case UPLIFT -> getValorDemandPlanItem(demandPlanItem, TipoDemanda.UPLIFT, com.opsfactor.community.platform.utility.Constantes.TipoPlano.PLANO_IRRESTRITO, unidadeMedidaTarget);
            case AJUSTE_DEMANDA -> getValorDemandPlanItem(demandPlanItem, TipoDemanda.AJUSTE_DEMANDA, com.opsfactor.community.platform.utility.Constantes.TipoPlano.PLANO_IRRESTRITO, unidadeMedidaTarget);
            default -> throw getUnsupportedDemandPlanningPhysicalKeyFigureException(keyFigureStandard);
        };
    }

    private IllegalArgumentException getUnsupportedDemandPlanningPhysicalKeyFigureException(
            KeyFigureStandard keyFigureStandard) {

        return new IllegalArgumentException(
                "DemandPlanningProjection can read this helper only for physical Demand Planning components "
                        + formatEnumValues(TIPOS_DEMANDA_ESCRITA_DEMAND_PLAN_LINHA)
                        + "; received key figure "
                        + (keyFigureStandard == null ? "null" : keyFigureStandard.getKeyFigureStandardEnum())
                        + ". Display, historical, total and Enterprise key figures must be handled by their Planning Book boundary.");

    }

    public double getValorKeyFigureStandardDpNaoTotalizadora(
            int posicaoPeriodo,
            FiltroDFUProjection filtroDfuProjection,
            KeyFigureStandard keyFigureStandard,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {

        Map<Location,Map<Produto,DemandPlanItem>> subMapaParaPosicaoPeriodo = mapaDemandPlanItems
                .getOrDefault(posicaoPeriodo, new ConcurrentHashMap<>());

        return filtroDfuProjection.getStreamMateriaisPorLocation().mapToDouble(parLocationEMateriais -> {

                    Location location = parLocationEMateriais.getValue0();

                    Map<Produto,DemandPlanItem> subMapaParaPosicaoPeriodoELocation = subMapaParaPosicaoPeriodo.getOrDefault(location, new ConcurrentHashMap<>());
                    if (subMapaParaPosicaoPeriodoELocation.isEmpty()) return 0f;

                    return parLocationEMateriais.getValue1().stream()
                            .mapToDouble(material -> {
                                DemandPlanItem demandPlanItem = subMapaParaPosicaoPeriodoELocation.get(material);

                                if (demandPlanItem != null) {
                                    return demandPlanItem.getQuantidade(keyFigureStandard, tipoPlano)
                                            * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                            material,
                                            demandPlanItem.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais()),
                                            unidadeMedidaTarget);
                                }
                                return 0f;
                            })
                            .sum();

                })
                .sum();

    }

    /**
     * Bloqueia KFs Enterprise quando elas seriam usadas como referencia de
     * totalizacao/rateio no Planning Book Community.
     *
     * <p>Getters tecnicos individuais ainda podem ler campos fisicos
     * transicionais para neutralizacao ou compatibilidade. O rateio de ajuste,
     * porem, e comportamento funcional da edicao aberta e nao pode usar Uplift,
     * New Materials, Client Orders, Reference/Comparison Plan ou Direct Demand
     * por dia util como base de distribuicao. Tambem nao deve usar KFs
     * Community de exibicao, como Direct Demand totalizador ou Historical Sales,
     * porque elas nao sao bases editaveis do split.</p>
     */
    private void validaKeyFigureTotalizacaoDemandPlanningCommunity(
            KeyFigureInterface keyFigureInterface) {

        if (keyFigureInterface == null) {
            throw new IllegalArgumentException(
                    "Demand Planning totalization key figure is required.");
        }

        if (keyFigureInterface instanceof KeyFigureStandard keyFigureStandard) {
            validaKeyFigureStandardTotalizacaoDemandPlanningCommunity(keyFigureStandard);
            return;
        }

        /*
         * KFs customizadas dependem da configuracao Enterprise de Planning
         * Views/Key Figures. No Community o Planning Book opera apenas com o
         * conjunto padrao e, portanto, nao pode usa-las como referencia de
         * totalizacao ou rateio.
         */
        throw new RequiresEnterpriseVersionException("Demand Planning custom key figure");

    }

    private void validaKeyFigureStandardTotalizacaoDemandPlanningCommunity(
            KeyFigureStandard keyFigureStandard) {

        KeyFigureStandardEnum keyFigureStandardEnum = keyFigureStandard.getKeyFigureStandardEnum();

        if (keyFigureStandardEnum == null) {
            throw new IllegalArgumentException(
                    "Demand Planning totalization key figure standard enum is required.");
        }

        switch (keyFigureStandardEnum) {
            case BASELINE, ITENS_NOVOS, AJUSTE_DEMANDA -> {
                /*
                 * A projection e uma estrutura tecnica compartilhada. `ITENS_NOVOS`
                 * precisa ser legivel/totalizavel para o overlay Enterprise
                 * somar `Direct Demand` depois que New Products passou a ser uma
                 * decomposicao real do Demand Plan. O Community continua bloqueando
                 * essa KF na fachada de Planning Book antes de chegar aqui.
                 */
            }
            case UPLIFT ->
                    throw new RequiresEnterpriseVersionException("Demand Planning " + keyFigureStandardEnum + " key figure");
            case CARTEIRA ->
                    throw new RequiresEnterpriseVersionException("Demand Planning client orders key figure");
            case DEMANDA_DIRETA_TOTAL_COMPARACAO ->
                    throw new RequiresEnterpriseVersionException("Demand Planning reference/comparison plan key figure");
            case DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL ->
                    throw new RequiresEnterpriseVersionException("Demand Planning Direct Demand per Working Day key figure");
            case DEMANDA_DIRETA_TOTAL_DP, HISTORICO_VENDAS ->
                    throw new IllegalArgumentException("Demand Planning totalization accepts only Baseline, New Products and Demand Adjustment, not " + keyFigureStandardEnum);
            default ->
                    throw new IllegalArgumentException("Demand Planning totalization accepts only Demand Planning base key figures, not " + keyFigureStandardEnum);
        }

    }

    public double getValorTotalKeyFigure(
            KeyFigureInterface keyFigure,
            int posicaoPeriodo,
            FiltroDFUProjection filtroDfuProjection,
            UnidadeMedida unidadeMedidaTarget) {

        if (keyFigure instanceof KeyFigureStandard) {
            validaKeyFigureTotalizacaoDemandPlanningCommunity(keyFigure);
            return getValorKeyFigureStandardDpNaoTotalizadora(
                    posicaoPeriodo,
                    filtroDfuProjection,
                    (KeyFigureStandard) keyFigure,
                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                    unidadeMedidaTarget);
        }

        throw new RequiresEnterpriseVersionException("Demand Planning custom key figure");

    }

    public double getValorTotalKeyFigure(
            KeyFigureInterface keyFigure,
            int posicaoPeriodo,
            Location location, Produto material,
            UnidadeMedida unidadeMedidaTarget) {

        if (keyFigure instanceof KeyFigureStandard) {
            validaKeyFigureTotalizacaoDemandPlanningCommunity(keyFigure);
            return getValorKeyFigureStandardDpNaoTotalizadora(
                    (KeyFigureStandard) keyFigure,
                    posicaoPeriodo,
                    location, material,
                    unidadeMedidaTarget);
        }

        throw new RequiresEnterpriseVersionException("Demand Planning custom key figure");

    }

    public double getValorTotalKeyFigures(
            Collection<KeyFigureInterface> keyFiguresATotalizar,
            int posicaoPeriodo,
            FiltroDFUProjection filtroDfuProjection,
            UnidadeMedida unidadeMedidaTarget) {

        return keyFiguresATotalizar.stream()
                .mapToDouble(keyFigure -> getValorTotalKeyFigure(
                        keyFigure,
                        posicaoPeriodo,
                        filtroDfuProjection,
                        unidadeMedidaTarget))
                .sum();

    }

    public double getValorTotalKeyFigures(
            Collection<KeyFigureInterface> keyFiguresATotalizar,
            int posicaoPeriodo,
            Location location, Produto material,
            UnidadeMedida unidadeMedidaTarget) {

        return keyFiguresATotalizar.stream()
                .mapToDouble(keyFigure -> getValorTotalKeyFigure(
                        keyFigure,
                        posicaoPeriodo,
                        location, material,
                        unidadeMedidaTarget))
                .sum();

    }

    /**
     * Aplica um ajuste em uma KF a um conjunto de DFUs, distribuindo-o de acordo com o valor total original de cada um
     * @return lista de triplets com : 1) demand plan linha modificado 2) valor original 3) novo valor
     * @throws UnitOfMeasureConversionException
     * @throws IncompatibleCalendarException
     */
    public Queue<Quartet<DemandPlanItem,Constantes.TipoDemanda,Double,Double>> setValorDemandPlanItemEmKeyFigureStandardDp(
            int posicaoPeriodo, 
            FiltroDFUProjection filtroDfuProjection,
            Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano,
            Collection<KeyFigureInterface> keyFiguresReferenciaSplitEntreDfus, // usado para se gerar um total de referência para o split entre os DFUs
            double valor, UnidadeMedida unidadeMedidaValor) {
                
        Queue<Quartet<DemandPlanItem,Constantes.TipoDemanda,Double,Double>> queueDemandPlanItemsAjustados = new ConcurrentLinkedQueue<>();
        
        // valor total, do plano para a etapa de workflow atual : usado para dar a proporção-alvo no split entre DFUs
        double valorTotalKfsTotalizacaoDemanda = getValorTotalKeyFigures(
                keyFiguresReferenciaSplitEntreDfus,
                posicaoPeriodo,
                filtroDfuProjection,
                unidadeMedidaValor);
        
        // valor total por ex. 'Ajuste Demanda'
        // este metodo assume um input diretamente no tipo de demanda, não considerando por ex. KF = DEMANDA_DIRETA_TOTAL
        double valorTotalKfSelecionada = getValorDemandPlanItem(posicaoPeriodo, filtroDfuProjection,
                tipoDemanda, tipoPlano, unidadeMedidaValor);
        
        double alteracaoValorTipoDemandaAntesAjuste = valor - valorTotalKfSelecionada;
        // não permite que ajuste faça com que plano total seja negativo
        double alteracaoValorTipoDemanda = Math.max(alteracaoValorTipoDemandaAntesAjuste, -valorTotalKfsTotalizacaoDemanda);
        if (Math.abs(alteracaoValorTipoDemanda) <= 0.0001) return queueDemandPlanItemsAjustados;

        // já existe valor total : distribuir proporcionalmente ao plano total de cada SKU
        if (valorTotalKfsTotalizacaoDemanda > 0.0001) {

            filtroDfuProjection.getStreamMateriaisPorLocation().parallel().forEach(parLocationEMateriais -> {
                Location location = parLocationEMateriais.getValue0();
                Set<Produto> materiais = parLocationEMateriais.getValue1();

                for (Produto material : materiais) {
                    UnidadeMedida unidadeMedidaDPDFU = getUnidadeMedidaConsiderada(location, material, posicaoPeriodo, TipoUnidadeMedidaConsiderada.DP);

                    double valorKfsTotalizacaoDemandaMaterialLocation = getValorTotalKeyFigures(
                            keyFiguresReferenciaSplitEntreDfus,
                            posicaoPeriodo,
                            location, material,
                            unidadeMedidaValor);
                    if (Math.abs(valorKfsTotalizacaoDemandaMaterialLocation) < 0.0001) continue;
                    double participacaoTotalKfsTotalizacaoDemandaNoTotal = valorKfsTotalizacaoDemandaMaterialLocation / valorTotalKfsTotalizacaoDemanda;
                    double valorAjusteNesteMaterialLocation = participacaoTotalKfsTotalizacaoDemandaNoTotal * alteracaoValorTipoDemanda;

                    DemandPlanItem demandPlanItem = getOrAddDemandPlanItem(location, material, posicaoPeriodo, unidadeMedidaDPDFU);
                    double valorAtualKfSelecionadaNoDemandPlanItem = getValorDemandPlanItem(
                            demandPlanItem,
                            tipoDemanda, tipoPlano,
                            unidadeMedidaValor);
                    double novoValorTipoDemandaLinha = valorAtualKfSelecionadaNoDemandPlanItem + valorAjusteNesteMaterialLocation;

                    setValorDemandPlanItemEmKeyFigureStandardDp(
                            demandPlanItem,
                            novoValorTipoDemandaLinha,
                            tipoDemanda, tipoPlano,
                            unidadeMedidaValor);

                    queueDemandPlanItemsAjustados.add(Quartet.with(
                            demandPlanItem,
                            tipoDemanda,
                            valorAtualKfSelecionadaNoDemandPlanItem,
                            novoValorTipoDemandaLinha));
                }

            });

            // valor inicial 0 ou próximo de 0 : distribuir igualmente entre SKUs
   
        } else {

            double valorPorDFU = valor / filtroDfuProjection.getNumeroDFUs();

            filtroDfuProjection.getStreamMateriaisPorLocation().forEach(parLocationEMateriais -> {

                Location location = parLocationEMateriais.getValue0();

                for (Produto material : parLocationEMateriais.getValue1()) {

                    DemandPlanItem demandPlanItem = getOrAddDemandPlanItem(
                            location, material, posicaoPeriodo, unidadeMedidaValor);
                    setValorDemandPlanItemEmKeyFigureStandardDp(
                            demandPlanItem,
                            valorPorDFU,
                            tipoDemanda, tipoPlano,
                            unidadeMedidaValor);

                    queueDemandPlanItemsAjustados.add(
                            Quartet.with(
                                    demandPlanItem,
                                    tipoDemanda,
                                    0.0,
                                    valorPorDFU));
                }

            });
                
        }
        
        return queueDemandPlanItemsAjustados;
        
    }

    /**
     * Bloqueia escrita funcional nas key figures Enterprise mantidas no schema.
     *
     * <p>O Community pode zerar `ITENS_NOVOS` e `UPLIFT` para neutralizar dados
     * legados antes de persistir ou consolidar planos. Qualquer valor diferente
     * de zero representaria executar ou editar uma feature Enterprise pela
     * projection compartilhada, contornando as validações das bordas de Planning
     * Book.</p>
     *
     * <p>O metodo e protegido para que uma subclass Enterprise reabra uma coluna
     * privada ja migrada sem alterar a semantica Community. A implementacao base
     * continua bloqueando valores diferentes de zero para todas as KFs privadas
     * compartilhadas no schema.</p>
     */
    protected void validaEscritaKeyFigureEnterpriseCommunity(
            Constantes.TipoDemanda tipoDemanda,
            double valorNaUnidadeMedidaDP) {

        if ((tipoDemanda == Constantes.TipoDemanda.ITENS_NOVOS
                || tipoDemanda == Constantes.TipoDemanda.UPLIFT)
                && Math.abs(valorNaUnidadeMedidaDP) >= 0.00005) {
            throw new RequiresEnterpriseVersionException("Demand Planning " + tipoDemanda + " key figure adjustment");
        }

    }

    /**
     * Garante que a chamada interna esteja tentando escrever um componente
     * fisico existente na linha de Demand Plan.
     */
    private void validaTipoDemandaEscritaDemandPlanItem(
            Constantes.TipoDemanda tipoDemanda) {

        if (tipoDemanda == null || !TIPOS_DEMANDA_ESCRITA_DEMAND_PLAN_LINHA.contains(tipoDemanda)) {
            throw getUnsupportedTipoDemandaEscritaException(tipoDemanda);
        }

    }

    /**
     * Garante que a escrita direta nao seja usada para series que nao vivem na
     * entidade de Demand Plan.
     */
    private void validaTipoPlanoEscritaDemandPlanItem(
            Constantes.TipoPlano tipoPlano) {

        if (tipoPlano == null || !TIPOS_PLANO_ESCRITA_DEMAND_PLAN_LINHA.contains(tipoPlano)) {
            throw getUnsupportedTipoPlanoEscritaException(tipoPlano);
        }

    }

    /**
     * Leitura tecnica de linhas aceita componentes fisicos compartilhados, mas
     * enum ausente e erro de contrato e nao deve virar NPE no `.equals`.
     */
    private void validaTipoDemandaLeituraDemandPlanItem(
            Constantes.TipoDemanda tipoDemanda) {

        if (tipoDemanda == null) {
            throw new IllegalArgumentException(
                    "DemandPlanningProjection requires demand component to read Demand Plan line; received null.");
        }

    }

    /**
     * Leitura tecnica de linhas exige uma variante fisica do plano informada.
     * Variantes nao suportadas continuam sendo tratadas pela entidade ou pelo
     * helper de total funcional, mas `null` deve falhar com mensagem clara.
     */
    private void validaTipoPlanoLeituraDemandPlanItem(
            Constantes.TipoPlano tipoPlano) {

        if (tipoPlano == null) {
            throw new IllegalArgumentException(
                    "DemandPlanningProjection requires plan variant to read Demand Plan line; received null.");
        }

    }

    private IllegalArgumentException getUnsupportedTipoDemandaEscritaException(
            Constantes.TipoDemanda tipoDemanda) {

        return new IllegalArgumentException(
                "DemandPlanningProjection can write only Demand Plan physical components "
                        + formatEnumValues(TIPOS_DEMANDA_ESCRITA_DEMAND_PLAN_LINHA)
                        + "; received " + (tipoDemanda == null ? "null" : tipoDemanda.name())
                        + ". TOTAL, client orders, historical values and derived key figures must be handled by the Planning Book boundary before persistence.");

    }

    private IllegalArgumentException getUnsupportedTipoPlanoEscritaException(
            Constantes.TipoPlano tipoPlano) {

        return new IllegalArgumentException(
                "DemandPlanningProjection can write only Demand Plan line variants "
                        + formatEnumValues(TIPOS_PLANO_ESCRITA_DEMAND_PLAN_LINHA)
                        + "; received " + (tipoPlano == null ? "null" : tipoPlano.name())
                        + ". Working plan, historical, budget and unmet-demand series are not stored by this direct line writer.");

    }

    private IllegalArgumentException getUnsupportedTipoPlanoLeituraTotalException(
            Constantes.TipoPlano tipoPlano) {

        return new IllegalArgumentException(
                "DemandPlanningProjection can read the Community functional total only from Demand Plan physical variants "
                        + formatEnumValues(TIPOS_PLANO_LEITURA_TOTAL_DEMAND_PLAN_LINHA)
                        + "; received " + (tipoPlano == null ? "null" : tipoPlano.name())
                        + ". Working plan, historical, budget and unmet-demand totals must be resolved by the Planning Book/projection boundary before line aggregation.");

    }

    private static String formatEnumValues(
            Set<? extends Enum<?>> enumValueSet) {

        return enumValueSet.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));

    }

    public Set<DemandPlanItem> getTodosDemandPlanItems() {
        return mapaDemandPlanItems
                .values().stream() // Stream de Map<Location,Map<Produto,DemandPlanItem>>
                .flatMap(subMapa -> subMapa.values().stream() // Stream de Map<Produto,DemandPlanItem>
                .flatMap(subMapa2 -> subMapa2.values().stream())) // Stream de DemandPlanItem
                .collect(Collectors.toSet());   
    }
    
    public Set<DFU> getDFUsComPlanoDemanda() {
        
        HashSet<DFU> dfuSet = new HashSet<>();
        
        for(Map<Location,Map<Produto,DemandPlanItem>> subMapa1 : mapaDemandPlanItems.values()) {            
            for(Entry<Location, Map<Produto,DemandPlanItem>> entry : subMapa1.entrySet()) {
                for(Produto material : entry.getValue().keySet()) {
                    DFU dfu = new DFU(material, entry.getKey());
                    dfuSet.add(dfu);
                }
            }
        }
        
        return dfuSet;

    }

    public Set<Location> getLocationsComPlano() {
        Set<Location> locationsComDemandPlanItem =  mapaDemandPlanItems
                .values()
                .stream()
                .flatMap(subMapa -> subMapa.keySet().stream())
                // para evitar LazyLoadingException se usarmos a location extraída
                .map(locationNoMapa -> clusterEParametrosProjection.getLocationPersistida(locationNoMapa.getId()))
                .collect(Collectors.toSet());
        return locationsComDemandPlanItem;
    }
    public Set<Produto> getMateriaisComPlanoNaLocation(Location location) {
        Set<Produto> materiaisComDemandPlanItem = mapaDemandPlanItems
                .values()
                .stream()
                .flatMap(subMapa -> subMapa.getOrDefault(location, new HashMap<>()).keySet().stream())
                // para evitar LazyLoadingException se usarmos o material extraído
                .map(materialNoMapa -> clusterEParametrosProjection.getMaterialPersistido(materialNoMapa.getId()))
                .collect(Collectors.toSet());
        return materiaisComDemandPlanItem;
    }
    public Set<Produto> getMateriaisComPlano() {
        Set<Produto> materiaisComDemandPlanItem = mapaDemandPlanItems
                .values()
                .stream()
                .flatMap(subMapa -> subMapa.values().stream())
                .flatMap(subMapa -> subMapa.keySet().stream())
                // para evitar LazyLoadingException se usarmos o material extraído
                .map(materialNoMapa -> clusterEParametrosProjection.getMaterialPersistido(materialNoMapa.getId()))
                .collect(Collectors.toSet());
        return materiaisComDemandPlanItem;
    }

}
