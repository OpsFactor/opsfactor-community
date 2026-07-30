package com.opsfactor.community.capability.planningbook.keyfigure.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredViewKeyFigure;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.projection.inmemorybi.BIEmMemoria;
import com.opsfactor.community.platform.projection.inmemorybi.applied.BIProjectionMaterialLocationPeriodo;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionFactory;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanProjectionFactory;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardSupplyPlanning;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureAbstract;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigurePadrao;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionFactory;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionPorDfu;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewKeyFigureRepository;
import com.opsfactor.community.capability.planningbook.keyfigure.service.KeyFigureService;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import com.pivovarit.function.ThrowingConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;

/**
 * Factory das projections de Key Figures exibidas nos Planning Books
 * Community.
 *
 * <p>A factory monta apenas KFs padrao em nivel material/location. Key figures
 * customizadas, selecao livre, apresentacao por caracteristicas e agrupamentos
 * agregados pertencem ao Enterprise e sao bloqueados nas bordas de view/front
 * service.</p>
 */
@Slf4j
@Service
public class KeyFigureProjectionFactory {

    /**
     * Service de catalogo de key figures padrao.
     */
    @Autowired
    private KeyFigureService keyFigureService;

    /**
     * Loader da seleção persistida. É usado somente ao abrir um Planning Book,
     * jamais pela factory de ConfiguredView que também é chamada em lote para
     * outras finalidades.
     */
    @Autowired
    private ConfiguredViewKeyFigureRepository configuredViewKeyFigureRepository;

    /**
     * Factory de vendas historicas Community, restrita a sell-out.
     */
    @Autowired
    private SalesProjectionFactory salesProjectionFactory;

    /**
     * Factory de projections de Supply Plan usadas no Supply Planning Book.
     */
    @Autowired
    private SupplyPlanProjectionFactory supplyPlanProjectionFactory;

    /**
     * Factory de projections de Demand Plan usadas no Demand Planning Book.
     */
    @Autowired
    private DemandPlanProjectionFactory demandPlanProjectionFactory;

    /**
     * Factory de conversoes de UOM em memoria para apresentar valores na
     * unidade escolhida pela view.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /**
     * Factory do split temporal flat usado pelo Community para compatibilizar
     * calendarios DP/SNP sem curvas configuraveis Enterprise.
     */
    @Autowired
    private SplitTemporalProjectionFactory splitTemporalProjectionFactory;

    /**
     * Factory da malha operacional usada por Supply Planning Book e detalhes de
     * demanda indireta.
     */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /**
     * Factory da politica operacional de safety stock. Otimizacao da politica
     * permanece Enterprise.
     */
    @Autowired
    private PoliticaEstoquesProjectionFactory politicaEstoquesProjectionFactory;
    public KeyFigureProjection getKeyFigureProjectionBase(
            Calendario calendario,
            ConfiguredViewProjection configuredViewProjection) {


        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();

        KeyFigureProjection keyFigureProjection = new KeyFigureProjection();

        keyFigureProjection.configuredViewProjection = configuredViewProjection;
        keyFigureProjection.unidadeMedidaProjection = unidadeMedidaProjection;
        keyFigureProjection.supplyNetworkProjection = supplyNetworkProjection;

        keyFigureProjection.biEmMemoriaDFUDataKeyFigure = new BIProjectionMaterialLocationPeriodo<DFUDataKeyFigureAbstract>(
                calendario,
                x -> x.getProduto(), x -> x.getLocation(), x -> x.getData(),
                DFUDataKeyFigureAbstract.class,
                /*
                 * A projection compartilhada sempre precisa dos dois indices
                 * para produzir folhas material/location. O agrupamento
                 * Enterprise e uma transformacao posterior do DTO e nao deve
                 * alterar a granularidade nem os indices da fotografia base.
                 */
                true,
                true);
        BIEmMemoria<DFUDataKeyFigureAbstract> biEmMemoria = keyFigureProjection.biEmMemoriaDFUDataKeyFigure.getBiEmMemoria();

        biEmMemoria.addObjectAttribute("KeyFigure", KeyFigureInterface.class, dfuDataKeyFigure -> dfuDataKeyFigure.getKeyFigure(), true);
        /*
         * Material, location e periodo ja sao tratados por
         * BIProjectionMaterialLocationPeriodo. No Community, qualquer
         * caracteristica dinamica ou key figure customizada falha nas bordas
         * especificas antes de popular este BI.
         */

        return keyFigureProjection;

    }

    public KeyFigureProjection getKeyFigureProjectionDeDemandPlan(DemandPlan demandPlan, ConfiguredViewProjection configuredViewProjection, boolean apagarCacheProjectionsAposInicializacao) {

        ConfiguredView configuredView = configuredViewProjection.getConfiguredView();
        ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
        unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();

        Calendario calendario = demandPlan.getCalendarioDoDemandPlanComNumeroPeriodosHistoricosFixo(
                demandPlan.getPerfilExecucaoDemandPlan(),
                configuredView.getNumeroPeriodosHistoricosDemandPlanningBook());
        KeyFigureProjection keyFigureProjection = getKeyFigureProjectionBase(calendario, configuredViewProjection);

        keyFigureProjection.demandPlan = demandPlan;
        keyFigureProjection.calendario = calendario;

        carregaSelecaoPersistidaDeKeyFigures(keyFigureProjection, configuredViewProjection);

        // popula keyFigureProjection.keyFiguresApresentadosEOrdenados
        atualizaProjectionComKeyFiguresDemandPlanningApresentados(keyFigureProjection, configuredViewProjection);
        aplicaKeyFiguresSomenteLeituraDaSelecaoPersistida(keyFigureProjection, configuredViewProjection);
        // popula keyFigureProjection.keyFiguresTotalizacaoDemanda
        atualizaProjectionComKeyFiguresDemandPlanningTotalizadores(keyFigureProjection, configuredViewProjection);

        // atualiza DemandPlanningProjection no KeyFigureProjection
        atualizaCacheKeyFigureProjectionComDemandPlanningProjection(keyFigureProjection);

        for (KeyFigureInterface keyFigure : keyFigureProjection.keyFiguresApresentadosEOrdenados) {
            atualizaKeyFigureProjectionDemandPlanningComKeyFigure(
                    keyFigureProjection,
                    keyFigure);
        }

        // Atualiza a key figure totalizadora quantitativa: Direct Demand.
        boolean haKeyFigureTotalDpNaVisualizacao = keyFigureProjection.keyFiguresApresentadosEOrdenados.contains(KeyFigureService.getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP));

        /*
         * A lista exibida e a lista que participa da totalizacao sao conceitos
         * distintos. O Community mantem ambas somente com KFs standard, mas o
         * hook permite que o Enterprise materialize uma KF privada que componha
         * Direct Demand mesmo quando a view nao a exibe. A materializacao deve
         * acontecer antes da soma para que o total derivado nunca dependa da
         * ordem posterior de um overlay.
         */
        Set<KeyFigureInterface> keyFiguresDemandPlanningNecessarias = new LinkedHashSet<>(
                keyFigureProjection.keyFiguresApresentadosEOrdenados);
        if (haKeyFigureTotalDpNaVisualizacao) {
            keyFiguresDemandPlanningNecessarias.addAll(
                    keyFigureProjection.keyFiguresTotalizacaoDemanda);
        }
        materializaKeyFiguresDemandPlanningAntesDaTotalizacao(
                keyFigureProjection,
                keyFiguresDemandPlanningNecessarias);

        if (haKeyFigureTotalDpNaVisualizacao) {
            // Atualizacao da key figure que totaliza a demanda direta.
            KeyFigureStandard keyFigureDemandaTotal = KeyFigureService.getKeyFigureStandardDeKeyFigureStandardEnum(
                    KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP);

            // demanda direta nos periodos futuros = soma key figures workflow
            atualizaKeyFigureProjectionDPComKeyFigureSoma(
                    keyFigureProjection,
                    keyFigureProjection.keyFiguresTotalizacaoDemanda,
                    keyFigureDemandaTotal,
                    calendario.getPosicaoPeriodoPresente(),
                    calendario.getPosicaoPeriodoFinalFuturo());

            // demanda direta nos periodos passados = historical sales
            KeyFigureStandard keyFigureHistoricoVendas = KeyFigureService.getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.HISTORICO_VENDAS);
            if (!keyFigureProjection.keyFiguresApresentadosEOrdenados.contains(keyFigureHistoricoVendas)) {
                atualizaKeyFigureProjectionDPParaKfsStandardDp(keyFigureProjection, keyFigureHistoricoVendas);
            }
            atualizaKeyFigureProjectionDPComKeyFigureSoma(
                    keyFigureProjection,
                    List.of(keyFigureHistoricoVendas),
                    keyFigureDemandaTotal,
                    calendario.getPosicaoPeriodoInicial(),
                    calendario.getPosicaoPeriodoFinalPassado());
        }

        if (apagarCacheProjectionsAposInicializacao) limpaCacheKeyFigureProjection(keyFigureProjection);
        return keyFigureProjection;

    }

    /**
     * Materializa fontes privadas de Key Figures antes da recomposicao de
     * {@code Direct Demand}.
     *
     * <p>O Community nao possui fontes privadas nem key figures dinamicas,
     * portanto este ponto de extensao nao faz leituras adicionais. O contrato
     * existe para que um overlay Enterprise possa usar uma unica leitura batch
     * e preencher o BI antes de a soma totalizadora ser calculada. A colecao
     * contem as KFs visiveis e, quando {@code Direct Demand} esta visivel, as
     * KFs usadas somente como componentes internos da totalizacao.</p>
     */
    protected void materializaKeyFiguresDemandPlanningAntesDaTotalizacao(
            KeyFigureProjection keyFigureProjection,
            Collection<KeyFigureInterface> keyFiguresDemandPlanningNecessarias) {

        // Community nao possui Key Figures privadas para materializar.

    }

    /**
     * Reserva a composicao de Comparison Plan para o overlay Enterprise.
     *
     * <p>O Community nao pode montar a segunda projection nem expor a key
     * figure de comparacao. A assinatura compartilhada permite que a SPI da
     * fachada use a factory primaria sem dependencia inversa para o package
     * privado.</p>
     */
    public KeyFigureProjection getKeyFigureProjectionDeDemandPlanComPlanoComparacao(
            DemandPlan demandPlanReferencia,
            DemandPlan demandPlanComparacao,
            ConfiguredViewProjection configuredViewProjection,
            boolean apagarCacheProjectionsAposInicializacao) {

        throw new RequiresEnterpriseVersionException("Demand Planning reference/comparison plan");

    }

    public KeyFigureProjection getKeyFigureProjectionDeSupplyPlan(SupplyPlan supplyPlan, Location location, ConfiguredViewProjection configuredViewProjection, boolean apagarCacheProjectionsAposInicializacao) {

        ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
        unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();

        Calendario calendario = supplyPlan.getCalendarioDoSupplyPlanParaLocationComPeriodoPassadoParaEstoqueInicial(
                clusterEParametrosProjection,
                location);
        KeyFigureProjection keyFigureProjection = getKeyFigureProjectionBase(calendario, configuredViewProjection);

        keyFigureProjection.supplyPlan = supplyPlan;
        keyFigureProjection.demandPlan = supplyPlan.getDemandPlan();
        keyFigureProjection.calendario = calendario;

        carregaSelecaoPersistidaDeKeyFigures(keyFigureProjection, configuredViewProjection);
        atualizaProjectionComKeyFiguresSupplyPlanningApresentados(keyFigureProjection, configuredViewProjection);
        aplicaKeyFiguresSomenteLeituraDaSelecaoPersistida(keyFigureProjection, configuredViewProjection);

        for (KeyFigureInterface keyFigure : keyFigureProjection.keyFiguresApresentadosEOrdenados) {
            if (keyFigure instanceof KeyFigureStandard keyFigureStandard) {
                Constantes.TipoPlano tipoPlano = keyFigure instanceof KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanning
                        ? keyFigureStandardSupplyPlanning.getTipoPlano()
                        : Constantes.TipoPlano.PLANO_TRABALHO;

                switch (keyFigureStandard.getKeyFigureStandardEnum()) {
                    case DEMANDA_TOTAL:
                        atualizaKeyFigureProjectionSNPComDemandaTotal(keyFigureProjection, keyFigure, tipoPlano);
                        break;
                    case DEMANDA_DIRETA_TOTAL_SNP:
                        atualizaKeyFigureProjectionSNPComDemandaDiretaTotal(keyFigureProjection, keyFigure, tipoPlano);
                        break;
                    case DEMANDA_DIRETA_PLANO_DEMANDA_SNP:
                        atualizaKeyFigureProjectionSNPComDemandaDiretaPlanoDemanda(keyFigureProjection, keyFigure, tipoPlano);
                        break;
                    case DEMANDA_DIRETA_CARTEIRA_SNP:
                        materializaKeyFigureSupplyPlanningForaDoCatalogoCommunity(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano);
                        break;
                    case DEMANDA_INDIRETA_TOTAL:
                        atualizaKeyFigureProjectionSNPComDemandaIndiretaTotal(keyFigureProjection, keyFigure, tipoPlano);
                        break;
                    case ESTOQUE_SEGURANCA:
                        atualizaKeyFigureProjectionSNPComEstoque(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano,
                                (inventoryPlanLinha, tipoPlanoIterado) -> inventoryPlanLinha.getQuantidadeEstoqueSeguranca(tipoPlanoIterado));
                        break;
                    case ESTOQUE:
                        atualizaKeyFigureProjectionSNPComEstoque(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano,
                                (inventoryPlanLinha, tipoPlanoIterado) -> inventoryPlanLinha.getQuantidadeEstoqueProjetado(tipoPlanoIterado));
                        break;
                    case ESTOQUE_DIAS:
                        materializaKeyFigureSupplyPlanningForaDoCatalogoCommunity(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano);
                        break;
                    case WRITEOFF:
                        materializaKeyFigureSupplyPlanningForaDoCatalogoCommunity(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano);
                        break;
                    case PRODUCAO_FIRME:
                        materializaKeyFigureSupplyPlanningForaDoCatalogoCommunity(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano);
                        break;
                    case PRODUCAO_PLANEJADA:
                        atualizaKeyFigureProjectionSNPComProducao(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano,
                                (productionPlanLinha, tipoPlanoIterado) -> productionPlanLinha.getQuantidade(tipoPlanoIterado, Constantes.FirmePlanejado.PLANEJADO));
                        break;
                    case INBOUND_FIRME:
                        materializaKeyFigureSupplyPlanningForaDoCatalogoCommunity(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano);
                        break;
                    case INBOUND_ESTOQUE_EM_TRANSITO:
                        materializaKeyFigureSupplyPlanningForaDoCatalogoCommunity(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano);
                        break;
                    case INBOUND_PLANEJADO:
                        atualizaKeyFigureProjectionSNPComInbound(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano,
                                (distributionPlanItem, tipoPlanoIterado) -> distributionPlanItem.getQuantidade(Constantes.FirmePlanejado.PLANEJADO, tipoPlanoIterado));
                        break;
                    case OUTBOUND_PLANEJADO:
                        materializaKeyFigureSupplyPlanningForaDoCatalogoCommunity(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano);
                        break;
                    default:
                        materializaKeyFigureSupplyPlanningForaDoCatalogoCommunity(
                                keyFigureProjection,
                                keyFigure,
                                tipoPlano);
                }
            } else {
                materializaKeyFigureSupplyPlanningForaDoCatalogoCommunity(
                        keyFigureProjection,
                        keyFigure,
                        Constantes.TipoPlano.PLANO_TRABALHO);
            }


        }

        if (apagarCacheProjectionsAposInicializacao) limpaCacheKeyFigureProjection(keyFigureProjection);
        return keyFigureProjection;

    }


    /**
     * Metodo usado caso keyFigureProjection.keyFiguresApresentadosEOrdenados nao tenha sido preenchido.
     * Adiciona as key figures :
     * 1) standard Community (demanda direta total, historico, baseline, ajuste demanda)
     * @param keyFigureProjection
     */
    protected void atualizaProjectionComKeyFiguresDemandPlanningApresentados(KeyFigureProjection keyFigureProjection, ConfiguredViewProjection configuredViewProjection) {

        // Cenário-base legado, onde a própria visão especifica quais key figures.
        // A factory de ConfiguredView Community normaliza esta lista para vazia;
        // se algum fluxo antigo preencher a lista, validamos aqui antes de
        // carregar os dados para nao expor Uplift, New Materials ou custom KFs.
        if (configuredViewProjection.getKeyFiguresOrdenadasParaExibicao() != null && !configuredViewProjection.getKeyFiguresOrdenadasParaExibicao().isEmpty()) {
            configuredViewProjection.getKeyFiguresOrdenadasParaExibicao()
                    .forEach(keyFigure -> {
                        validaKeyFigureDemandPlanningBookCommunity(keyFigure);
                        keyFigureProjection.keyFiguresApresentadosEOrdenados.add(keyFigure);
                    });
        // Comportamento padrão, quando as KFs não são especificadas
        } else {
            // Conjunto padrao Community. Uplift, materiais novos e custom key
            // figures pertencem ao OpsFactor Enterprise.
            keyFigureProjection.keyFiguresApresentadosEOrdenados
                    .add(KeyFigureService.getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP));
            keyFigureProjection.keyFiguresApresentadosEOrdenados
                    .add(KeyFigureService.getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.HISTORICO_VENDAS));
            keyFigureProjection.keyFiguresApresentadosEOrdenados
                    .add(KeyFigureService.getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.BASELINE));
            keyFigureProjection.keyFiguresApresentadosEOrdenados
                    .add(KeyFigureService.getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.AJUSTE_DEMANDA));
        }

    }

    /**
     * Carrega a seleção persistida somente quando uma grade é aberta.
     *
     * <p>Lista vazia não representa uma grade vazia: preserva o catálogo
     * default. O repository é chamado uma única vez por Planning Book e a
     * resolução acontece inteiramente em memória, sem consulta por Key Figure
     * ou por célula.</p>
     */
    protected void carregaSelecaoPersistidaDeKeyFigures(
            KeyFigureProjection keyFigureProjection,
            ConfiguredViewProjection configuredViewProjection) {

        List<ConfiguredViewKeyFigure> configuredViewKeyFigures = configuredViewKeyFigureRepository
                .findAllByConfiguredViewIn(List.of(keyFigureProjection.getConfiguredViewProjection().getConfiguredView()));
        if (configuredViewKeyFigures.isEmpty()) {
            configuredViewProjection.setKeyFiguresOrdenadasParaExibicao(List.of());
            configuredViewProjection.setKeyFiguresConfiguradasPorId(Map.of());
            return;
        }

        Map<String, ConfiguredViewKeyFigure> configuredKeyFiguresById = new LinkedHashMap<>();
        for (ConfiguredViewKeyFigure configuredViewKeyFigure : configuredViewKeyFigures) {
            ConfiguredViewKeyFigure previous = configuredKeyFiguresById.put(
                    configuredViewKeyFigure.getKeyFigureId(), configuredViewKeyFigure);
            if (previous != null) {
                throw new IllegalStateException(
                        "Configured View key figure snapshot contains duplicate id: "
                                + configuredViewKeyFigure.getKeyFigureId());
            }
        }

        Map<String, KeyFigureInterface> resolvedKeyFiguresById = resolveKeyFiguresConfiguradas(
                keyFigureProjection.getConfiguredViewProjection().getConfiguredView().getTipoView(),
                configuredKeyFiguresById.keySet());
        if (resolvedKeyFiguresById.size() != configuredKeyFiguresById.size()) {
            throw new IllegalStateException("Configured View key figure resolution returned incomplete snapshot.");
        }

        Map<String, ConfiguredViewKeyFigure> configuredKeyFiguresByResolvedId = new LinkedHashMap<>();
        for (Map.Entry<String, KeyFigureInterface> resolvedKeyFigureEntry : resolvedKeyFiguresById.entrySet()) {
            configuredKeyFiguresByResolvedId.put(
                    resolvedKeyFigureEntry.getValue().getId(),
                    configuredKeyFiguresById.get(resolvedKeyFigureEntry.getKey()));
        }
        configuredViewProjection.setKeyFiguresConfiguradasPorId(configuredKeyFiguresByResolvedId);
        configuredViewProjection.setKeyFiguresOrdenadasParaExibicao(configuredViewKeyFigures.stream()
                .sorted(Comparator.comparingInt(ConfiguredViewKeyFigure::getPosition)
                        .thenComparing(ConfiguredViewKeyFigure::getKeyFigureId))
                .map(configuredViewKeyFigure -> resolvedKeyFiguresById.get(
                        configuredViewKeyFigure.getKeyFigureId()))
                .toList());

    }

    /**
     * Resolve ids persistidos em Key Figures standard Community. Overlays
     * Enterprise podem substituir o método para completar Custom Key Figures em uma única
     * consulta batch, mantendo a mesma tabela e DTO de seleção.
     */
    protected Map<String, KeyFigureInterface> resolveKeyFiguresConfiguradas(
            ConfiguredView.TipoView tipoView,
            Collection<String> keyFigureIds) {

        Map<String, KeyFigureInterface> keyFiguresById = new LinkedHashMap<>();
        for (String keyFigureId : keyFigureIds) {
            try {
                KeyFigureInterface keyFigure = ConfiguredView.TipoView.DEMANDPLANNINGBOOK.equals(tipoView)
                        ? KeyFigureService.getKeyFigureStandardDeKeyFigureStandardEnum(
                                MetodosUtilidade.getValorEnumDeJsonProperty(
                                        KeyFigureStandardEnum.class, keyFigureId))
                        : new KeyFigureStandardSupplyPlanning(keyFigureId);
                keyFiguresById.put(keyFigureId, keyFigure);
            } catch (IllegalArgumentException exception) {
                throw new RequiresEnterpriseVersionException(
                        "Planning Book key figure selection", exception);
            }
        }
        return keyFiguresById;

    }

    /**
     * Aplica o override de edição depois da normalização de KFs Supply, para
     * que a marca read-only recaia no mesmo objeto efetivamente exibido.
     */
    private void aplicaKeyFiguresSomenteLeituraDaSelecaoPersistida(
            KeyFigureProjection keyFigureProjection,
            ConfiguredViewProjection configuredViewProjection) {

        Map<String, ConfiguredViewKeyFigure> configuredKeyFiguresById =
                configuredViewProjection.getKeyFiguresConfiguradasPorId();
        for (KeyFigureInterface keyFigure : keyFigureProjection.getKeyFiguresApresentadosEOrdenados()) {
            ConfiguredViewKeyFigure configuredViewKeyFigure = configuredKeyFiguresById.get(keyFigure.getId());
            if (configuredViewKeyFigure != null && !configuredViewKeyFigure.getAllowChanges()) {
                keyFigureProjection.defineKeyFigureComoSomenteLeitura(keyFigure);
            }
        }

    }

    protected void validaKeyFigureDemandPlanningBookCommunity(KeyFigureInterface keyFigure) {

        if (!(keyFigure instanceof KeyFigureStandard keyFigureStandard)) {
            validaKeyFigureDemandPlanningForaDoCatalogoCommunity(keyFigure);
            return;
        }

        switch (keyFigureStandard.getKeyFigureStandardEnum()) {
            case DEMANDA_DIRETA_TOTAL_DP,
                    BASELINE,
                    AJUSTE_DEMANDA,
                    HISTORICO_VENDAS -> {
                // KFs padrao permitidas no Planning Book Community.
            }
            case DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL,
                    ITENS_NOVOS,
                    UPLIFT,
                    CARTEIRA,
                    VENDAS_GROSS,
                    VENDAS_NET ->
                    validaKeyFigureDemandPlanningForaDoCatalogoCommunity(keyFigure);
            default ->
                    validaKeyFigureDemandPlanningForaDoCatalogoCommunity(keyFigure);
        }

    }

    private void atualizaProjectionComKeyFiguresDemandPlanningTotalizadores(KeyFigureProjection keyFigureProjection, ConfiguredViewProjection configuredViewProjection) {
        keyFigureProjection.keyFiguresTotalizacaoDemanda = keyFigureService.getKeyFiguresQueCompoemDemandaDireta(configuredViewProjection);
    }

    private void atualizaProjectionComKeyFiguresSupplyPlanningApresentados(
            KeyFigureProjection keyFigureProjection,
            ConfiguredViewProjection configuredViewProjection) {

        List<KeyFigureInterface> keyFiguresOrdenadasParaExibicao =
                configuredViewProjection.getKeyFiguresOrdenadasParaExibicao();

        /*
         * A ConfiguredViewProjectionFactory Community normaliza a lista para
         * vazia. Ainda assim, callers diretos podem montar a projection sem
         * passar pela factory. Para Supply seguimos a mesma regra de Demand:
         * null ou vazio significam "usar KFs padrao Community"; lista
         * preenchida e tratada como configuracao explicita e validada item a
         * item antes de chegar ao front.
         */
        if (keyFiguresOrdenadasParaExibicao != null && !keyFiguresOrdenadasParaExibicao.isEmpty()) {
            keyFigureProjection.keyFiguresApresentadosEOrdenados
                    .addAll(keyFiguresOrdenadasParaExibicao
                            .stream()
                            .map(this::normalizaKeyFigureSupplyPlanning)
                            .peek(this::validaKeyFigureSupplyPlanningBookCommunity)
                            .toList());
            return;
        }

        atualizaProjectionComKeyFiguresPadraoSupplyPlanning(keyFigureProjection);
    }

    private KeyFigureInterface normalizaKeyFigureSupplyPlanning(KeyFigureInterface keyFigure) {
        if (!(keyFigure instanceof KeyFigureStandard keyFigureStandard)) {
            return keyFigure;
        }
        if (keyFigure instanceof KeyFigureStandardSupplyPlanning) {
            return keyFigure;
        }

        KeyFigureStandardEnum keyFigureStandardEnum = keyFigureStandard.getKeyFigureStandardEnum();
        if (keyFigureStandardEnum.equals(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP)) {
            return new KeyFigureStandardSupplyPlanning(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_SNP, Constantes.TipoPlano.PLANO_TRABALHO);
        }

        try {
            return new KeyFigureStandardSupplyPlanning(keyFigureStandardEnum, Constantes.TipoPlano.PLANO_TRABALHO);
        } catch (IllegalArgumentException exception) {
            return keyFigure;
        }
    }

    private void atualizaProjectionComKeyFiguresPadraoSupplyPlanning(KeyFigureProjection keyFigureProjection) {

        // Conjunto padrao Community. Ordens firmes, carteira, estoque em
        // transito e batch aging/writeoff sao transacionais/analiticos
        // Enterprise e permanecem fora da visualizacao padrao.
        keyFigureProjection.keyFiguresApresentadosEOrdenados
                .add(new KeyFigureStandardSupplyPlanning(KeyFigureStandardEnum.DEMANDA_TOTAL, Constantes.TipoPlano.PLANO_TRABALHO));
        keyFigureProjection.keyFiguresApresentadosEOrdenados
                .add(new KeyFigureStandardSupplyPlanning(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_SNP, Constantes.TipoPlano.PLANO_TRABALHO));
        keyFigureProjection.keyFiguresApresentadosEOrdenados
                .add(new KeyFigureStandardSupplyPlanning(KeyFigureStandardEnum.DEMANDA_DIRETA_PLANO_DEMANDA_SNP, Constantes.TipoPlano.PLANO_TRABALHO));
        keyFigureProjection.keyFiguresApresentadosEOrdenados
                .add(new KeyFigureStandardSupplyPlanning(KeyFigureStandardEnum.DEMANDA_INDIRETA_TOTAL, Constantes.TipoPlano.PLANO_TRABALHO));
        keyFigureProjection.keyFiguresApresentadosEOrdenados
                .add(new KeyFigureStandardSupplyPlanning(KeyFigureStandardEnum.ESTOQUE_SEGURANCA, Constantes.TipoPlano.PLANO_TRABALHO));
        keyFigureProjection.keyFiguresApresentadosEOrdenados
                .add(new KeyFigureStandardSupplyPlanning(KeyFigureStandardEnum.ESTOQUE, Constantes.TipoPlano.PLANO_TRABALHO));
        keyFigureProjection.keyFiguresApresentadosEOrdenados
                .add(new KeyFigureStandardSupplyPlanning(KeyFigureStandardEnum.PRODUCAO_PLANEJADA, Constantes.TipoPlano.PLANO_TRABALHO));
        keyFigureProjection.keyFiguresApresentadosEOrdenados
                .add(new KeyFigureStandardSupplyPlanning(KeyFigureStandardEnum.INBOUND_PLANEJADO, Constantes.TipoPlano.PLANO_TRABALHO));

    }

    /**
     * Valida a selecao explicita de uma Key Figure de Supply Planning Book.
     *
     * <p>O Community conserva a allowlist publica. O overlay Enterprise pode
     * reabrir somente KFs privadas ja materializaveis pela mesma projection
     * compartilhada, sem alterar o catalogo ou o default Community.</p>
     */
    protected void validaKeyFigureSupplyPlanningBookCommunity(KeyFigureInterface keyFigure) {

        if (!(keyFigure instanceof KeyFigureStandard keyFigureStandard)) {
            validaKeyFigureSupplyPlanningForaDoCatalogoCommunity(keyFigure);
            return;
        }

        /*
         * A selecao persistida chega a esta factory sem necessariamente passar
         * pela borda de save da ConfiguredView. Portanto a protecao do
         * catalogo Community tambem precisa existir no runtime: linhas legadas
         * ou injetadas nao podem reabrir Restricted/Unrestricted Plan apenas
         * porque o parser tipado de Supply sabe representa-las.
         */
        if (keyFigure instanceof KeyFigureStandardSupplyPlanning keyFigureSupplyPlanning
                && !Constantes.TipoPlano.PLANO_TRABALHO.equals(keyFigureSupplyPlanning.getTipoPlano())) {
            throw new RequiresEnterpriseVersionException("Supply Planning non-working plan key figure selection");
        }

        switch (keyFigureStandard.getKeyFigureStandardEnum()) {
            case DEMANDA_TOTAL,
                    DEMANDA_DIRETA_TOTAL_SNP,
                    DEMANDA_DIRETA_PLANO_DEMANDA_SNP,
                    DEMANDA_INDIRETA_TOTAL,
                    ESTOQUE_SEGURANCA,
                    ESTOQUE,
                    PRODUCAO_PLANEJADA,
                    INBOUND_PLANEJADO -> {
                // KFs operacionais publicaveis no Supply Planning Book Community.
            }
            case DEMANDA_DIRETA_CARTEIRA_SNP,
                    PRODUCAO_FIRME,
                    INBOUND_FIRME,
                    INBOUND_ESTOQUE_EM_TRANSITO,
                    OUTBOUND_PLANEJADO,
                    WRITEOFF,
                    ESTOQUE_DIAS ->
                    validaKeyFigureSupplyPlanningForaDoCatalogoCommunity(keyFigure);
            default ->
                    validaKeyFigureSupplyPlanningForaDoCatalogoCommunity(keyFigure);
        }

    }

    /**
     * Trata uma selecao de Demand Planning que nao pertence ao catalogo
     * Community. O overlay pode aceitar uma serie privada real; sem overlay a
     * recusamos em um unico ponto, sem catalogar suas variantes no nucleo.
     */
    protected void validaKeyFigureDemandPlanningForaDoCatalogoCommunity(KeyFigureInterface keyFigure) {

        throw new RequiresEnterpriseVersionException("Demand Planning key figure selection");

    }

    /**
     * Trata uma selecao de Supply Planning que nao pertence ao catalogo
     * Community. O metodo preserva a barreira de edicao sem fazer a factory
     * aberta conhecer cada serie privada que um overlay pode materializar.
     */
    protected void validaKeyFigureSupplyPlanningForaDoCatalogoCommunity(KeyFigureInterface keyFigure) {

        throw new RequiresEnterpriseVersionException("Supply Planning key figure selection");

    }

    private void atualizaCacheKeyFigureProjectionComPoliticaEstoquesProjection(KeyFigureProjection keyFigureProjection) {

        if (keyFigureProjection.politicaEstoquesProjectionCache == null) {
            SupplyPlan supplyPlan = keyFigureProjection.supplyPlan;
            ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
            ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();

            keyFigureProjection.politicaEstoquesProjectionCache = politicaEstoquesProjectionFactory.getPoliticaEstoquesProjection(
                    keyFigureProjection.calendario,
                    clusterEParametrosProjection,
                    supplyPlan.getPerfilExecucaoSupplyPlan());
        }


    }

    /**
     * Materializa uma unica fotografia batch de Supply Planning por grade.
     *
     * <p>O metodo e protegido para overlays Enterprise lerem apenas dados ja
     * anexados ao snapshot compartilhado, sem introduzir consultas por celula
     * ou novas projections para Key Figures privadas.</p>
     */
    protected void atualizaCacheKeyFigureProjectionComSupplyPlanningProjection(KeyFigureProjection keyFigureProjection) {

        if (keyFigureProjection.supplyPlanningProjectionCache == null) {
            ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = keyFigureProjection.supplyPlan.getPerfilExecucaoSupplyPlan();

            if (configuredViewProjection.getLocationsFiltradas().size() != 1) {
                throw getUnsupportedSupplyPlanningKeyFigureProjectionLocationScopeException(configuredViewProjection);
            }

            ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
            SupplyNetworkProjection supplyNetworkProjection = keyFigureProjection.supplyNetworkProjection;

            atualizaCacheKeyFigureProjectionComPoliticaEstoquesProjection(keyFigureProjection);

            Location location = configuredViewProjection.getLocationsFiltradas()
                    .iterator()
                    .next();

            keyFigureProjection.supplyPlanningProjectionCache = supplyPlanProjectionFactory.getSupplyPlanningProjectionCompleto(
                    keyFigureProjection.supplyPlan,
                    keyFigureProjection.supplyPlan.getPerfilExecucaoSupplyPlan(),
                    // considera o perfil raiz do supply plan Community.
                    location,
                    supplyNetworkProjection,
                    keyFigureProjection.politicaEstoquesProjectionCache,
                    MaterialProjectionFactory.getProjectionSetMateriais(
                            configuredViewProjection.getMateriaisFiltrados(),
                            clusterEParametrosProjection),
                    LocationProjectionFactory.getLocationProjectionCompleto(clusterEParametrosProjection));

            // se perfil execução indicar que inventory plan não é salvo, os estoques projetados deverão ser recalculados antes de se extrair
            // KFs de estoque projetado e estoque de segurança
            if (!perfilExecucaoSupplyPlan.getSalvaInventoryPlan()) { // estoques não salvos : precisam ser recalculados

                atualizaCacheKeyFigureProjectionComDemandPlanningProjection(keyFigureProjection);

                SupplyPlanning.atualizaEstoqueSeguranca(
                        keyFigureProjection.supplyPlanningProjectionCache,
                        Constantes.TipoPlano.PLANO_TRABALHO);
                SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                        keyFigureProjection.supplyPlanningProjectionCache,
                        Constantes.TipoPlano.PLANO_TRABALHO);

                if (!perfilExecucaoSupplyPlan.getPermiteBacklogDemanda()) {
                    SupplyPlanning.limitaEstoquesNegativosAZero(Constantes.TipoPlano.PLANO_TRABALHO, keyFigureProjection.supplyPlanningProjectionCache);
                }
            }
        }

    }

    /**
     * Supply Planning Book Community materializa uma projection por location.
     *
     * <p>Views agregadas por varias locations pertencem ao Enterprise. O erro
     * precisa acontecer antes de carregar `SupplyPlanningProjection`, pois esse
     * cache pressupoe location unica para estoque, producao e inbound.</p>
     */
    private RequiresEnterpriseVersionException getUnsupportedSupplyPlanningKeyFigureProjectionLocationScopeException(
            ConfiguredViewProjection configuredViewProjection) {

        return new RequiresEnterpriseVersionException(
                "Aggregated Supply Planning Book location views. KeyFigureProjectionFactory received "
                        + configuredViewProjection.getLocationsFiltradas().size()
                        + " locations before Supply Planning projection loading");

    }

    /**
     * Valida a projection de view antes de qualquer montagem de cache do
     * Planning Book.
     *
     * <p>A factory e chamada tanto pelo Community quanto por overlays
     * Enterprise. Falhar aqui deixa claro que o problema esta na construcao do
     * snapshot de view, e nao em uma rotina posterior de Key Figure.</p>
     */
    /**
     * Valida o calendario base antes de criar o BI material/location/periodo.
     *
     * <p>Sem calendario nao ha indice temporal seguro para a projection. Essa
     * falha deve aparecer na borda da factory, nao dentro do BI em memoria.</p>
     */
    /**
     * Valida o Demand Plan usado pelo Planning Book de demanda.
     */
    /**
     * Valida o Supply Plan usado pelo Planning Book de supply.
     */
    /**
     * Valida a location escalar do Supply Planning Book Community.
     *
     * <p>Views de supply agregadas por varias locations ja sao bloqueadas na
     * carga do cache. Este metodo cobre a entrada publica por location unica.</p>
     */
    /**
     * Retorna a entidade de view associada ao projection.
     *
     * <p>O Planning Book Community usa parametros da view para unidade de
     * medida, historico exibido e filtros. Uma view ausente indica snapshot
     * incompleto e deve falhar antes de consultar vendas ou planos.</p>
     */
    /**
     * Retorna a projection central de parametros/clusters obrigatoria para
     * montar as Key Figures.
     *
     * <p>Mesmo quando o Planning Book exibe apenas nivel material/location, a
     * factory ainda depende dos parametros globais para calendario, unidade de
     * medida padrao e interpretacao dos planos. Nao ha fallback Community para
     * esse snapshot.</p>
     */
    /**
     * Retorna a projection de unidades de medida usada para converter todas as
     * Key Figures quantitativas exibidas no Planning Book.
     */
    /**
     * Retorna a projection de malha usada pelas Key Figures de Supply Planning
     * e pelas conversoes indiretas de demanda/estoque.
     *
     * <p>A malha precisa carregar a mesma projection central de parametros. Se
     * ela chegar incompleta, o erro deve aparecer na borda da factory, antes de
     * popular BI em memoria ou recalcular estoques.</p>
     */
    private void atualizaCacheKeyFigureProjectionComSplitTemporalProjectionPorDfuCalendarioDPParaSNP(KeyFigureProjection keyFigureProjection) {

        if (keyFigureProjection.splitTemporalProjectionPorDfuCalendarioDPParaSNPCache == null) {
            keyFigureProjection.splitTemporalProjectionPorDfuCalendarioDPParaSNPCache = splitTemporalProjectionFactory.geraSplitTemporalProjectionPorDfu(
                    keyFigureProjection.demandPlan,
                    keyFigureProjection.supplyPlan);
        }

    }

    /**
     * Extrai a demanda direta total para o Supply Planning Book Community.
     *
     * <p>No Community a fonte futura e sempre o Demand Plan. Carteira, sell-in
     * e sales orders sao key figures Enterprise e sao bloqueadas antes da
     * materializacao do projection.</p>
     */
    private void atualizaKeyFigureProjectionSNPComDemandaDiretaTotal(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoPlano tipoPlano) {

        atualizaCacheKeyFigureProjectionComDemandPlanningProjection(keyFigureProjection);
        atualizaCacheKeyFigureProjectionComSplitTemporalProjectionPorDfuCalendarioDPParaSNP(keyFigureProjection);
        atualizaCacheKeyFigureProjectionComSupplyPlanningProjection(keyFigureProjection);

        SupplyPlanningProjection supplyPlanningProjection = keyFigureProjection.supplyPlanningProjectionCache;

        Location location = supplyPlanningProjection.getLocation();
        Calendario calendario = supplyPlanningProjection.getCalendario();
        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
        ParametrosGlobais parametrosGlobais = supplyPlanningProjection.getClusterEParametrosProjection().getParametrosGlobais();

        int posicaoPeriodoInicialDemandaDireta = calendario.getPosicaoPeriodoPresente();

        for (Produto material : supplyPlanningProjection.getMaterialProjection().getMateriaisAtivosEmLocation(location)) {
            for (int i = posicaoPeriodoInicialDemandaDireta; i < calendario.getNumeroPeriodosTotais(); i++) {
                double demandaDireta = SupplyPlanning.getDemandaDiretaConsideradaParaEstoqueProjetado(
                        supplyPlanningProjection,
                        i, material,
                        tipoPlano,
                        configuredViewProjection.getUnidadeMedidaView(parametrosGlobais));

                if (demandaDireta != 0.0) {
                    keyFigureProjection.biEmMemoriaDFUDataKeyFigure.addDadoAoBI(DFUDataKeyFigurePadrao.builder()
                            .location(location)
                            .produto(material)
                            .data(calendario.getUltimaDataHorarioPeriodo(i)) // se usa a data para não gerar problemas com calendario diferente no KeyFigureProjection
                            .keyFigure(keyFigure)
                            .valor(demandaDireta)
                            .build());
                }
            }
        }

    }

    private void atualizaKeyFigureProjectionSNPComDemandaDiretaPlanoDemanda(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoPlano tipoPlano) {

        atualizaCacheKeyFigureProjectionComSupplyPlanningProjection(keyFigureProjection);

        SupplyPlanningProjection supplyPlanningProjection = keyFigureProjection.supplyPlanningProjectionCache;
        Location location = supplyPlanningProjection.getLocation();
        Calendario calendario = supplyPlanningProjection.getCalendario();
        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
        ParametrosGlobais parametrosGlobais = supplyPlanningProjection.getClusterEParametrosProjection().getParametrosGlobais();

        for (Produto material : supplyPlanningProjection.getMaterialProjection().getMateriaisAtivosEmLocation(location)) {
            for (int i = calendario.getPosicaoPeriodoPresente(); i < calendario.getNumeroPeriodosTotais(); i++) {
                double demandaDiretaPlanoDemanda = SupplyPlanning.getDemandaDiretaPlanoDemandaConsideradaParaEstoqueProjetado(
                        supplyPlanningProjection,
                        i,
                        material,
                        tipoPlano,
                        configuredViewProjection.getUnidadeMedidaView(parametrosGlobais));

                if (demandaDiretaPlanoDemanda != 0.0) {
                    keyFigureProjection.addDadoDFUKeyFigurePadrao(
                            location,
                            material,
                            calendario.getUltimaDataHorarioPeriodo(i),
                            keyFigure,
                            demandaDiretaPlanoDemanda);
                }
            }
        }
    }

    private void atualizaKeyFigureProjectionSNPComDemandaTotal(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoPlano tipoPlano) {

        atualizaCacheKeyFigureProjectionComDemandPlanningProjection(keyFigureProjection);
        atualizaCacheKeyFigureProjectionComSplitTemporalProjectionPorDfuCalendarioDPParaSNP(keyFigureProjection);
        atualizaCacheKeyFigureProjectionComSupplyPlanningProjection(keyFigureProjection);

        DemandPlanningProjection demandPlanningProjection = keyFigureProjection.demandPlanningProjectionReferenciaCache;
        SupplyPlanningProjection supplyPlanningProjection = keyFigureProjection.supplyPlanningProjectionCache;
        SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu = keyFigureProjection.splitTemporalProjectionPorDfuCalendarioDPParaSNPCache;

        Location location = supplyPlanningProjection.getLocation();
        Calendario calendario = supplyPlanningProjection.getCalendario();
        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
        ParametrosGlobais parametrosGlobais = supplyPlanningProjection.getClusterEParametrosProjection().getParametrosGlobais();

        int posicaoPeriodoInicialDemandaDireta = calendario.getPosicaoPeriodoPresente();

        for (Produto material : supplyPlanningProjection.getMaterialProjection().getMateriaisAtivos()) {
            for (int i = posicaoPeriodoInicialDemandaDireta; i < calendario.getNumeroPeriodosTotais(); i++) {
                double demandaDiretaEIndireta = SupplyPlanning.getDemandaDiretaConsideradaEIndiretaParaProjecaoEstoque(
                        supplyPlanningProjection,
                        i, material,
                        tipoPlano,
                        configuredViewProjection.getUnidadeMedidaView(parametrosGlobais));

                if (demandaDiretaEIndireta != 0.0) {
                    keyFigureProjection.addDadoDFUKeyFigurePadrao(
                            location,
                            material,
                            calendario.getUltimaDataHorarioPeriodo(i), // se usa a data para não gerar problemas com calendario diferente no KeyFigureProjection
                            keyFigure,
                            demandaDiretaEIndireta);
                }
            }
        }

    }

    private void atualizaKeyFigureProjectionSNPComDemandaIndiretaTotal(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoPlano tipoPlano) {

        atualizaCacheKeyFigureProjectionComSupplyPlanningProjection(keyFigureProjection);

        SupplyPlanningProjection supplyPlanningProjection = keyFigureProjection.supplyPlanningProjectionCache;

        Location location = supplyPlanningProjection.getLocation();
        Calendario calendario = supplyPlanningProjection.getCalendario();
        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
        ParametrosGlobais parametrosGlobais = supplyPlanningProjection.getClusterEParametrosProjection().getParametrosGlobais();

        for (Produto material : supplyPlanningProjection.getMaterialProjection().getMateriaisAtivos()) {
            for (int i = calendario.getPosicaoPeriodoPresente(); i < calendario.getNumeroPeriodosTotais(); i++) {
                double demandaIndireta = SupplyPlanning.getDemandaIndireta(
                        supplyPlanningProjection,
                        i, material,
                        tipoPlano,
                        configuredViewProjection.getUnidadeMedidaView(parametrosGlobais));

                if (demandaIndireta != 0.0) {
                    keyFigureProjection.addDadoDFUKeyFigurePadrao(
                            location,
                            material,
                            calendario.getUltimaDataHorarioPeriodo(i), // se usa a data para não gerar problemas com calendario diferente no KeyFigureProjection
                            keyFigure,
                            demandaIndireta);
                }
            }
        }

    }

    protected void atualizaKeyFigureProjectionDPParaKfsStandardDp(
            KeyFigureProjection keyFigureProjection,
            KeyFigureStandard keyFigureStandard) {

        switch(keyFigureStandard.getKeyFigureStandardEnum()) {
            case BASELINE -> atualizaKeyFigureProjectionDPComDemandPlanItem(keyFigureProjection, keyFigureStandard, demandPlanItem -> demandPlanItem.getQuantidadeBaseline());
            case ITENS_NOVOS, UPLIFT, VENDAS_GROSS, VENDAS_NET, CARTEIRA ->
                    materializaKeyFigureDemandPlanningForaDoCatalogoCommunity(
                            keyFigureProjection,
                            keyFigureStandard);
            case AJUSTE_DEMANDA -> atualizaKeyFigureProjectionDPComDemandPlanItem(keyFigureProjection, keyFigureStandard, demandPlanItem -> demandPlanItem.getQuantidadeAjusteDemanda());
            // vendas para períodos passados e futuros
            case HISTORICO_VENDAS -> {
                ParametrosGlobais parametrosGlobais = keyFigureProjection.getSupplyNetworkProjection().getClusterEParametrosProjection().getParametrosGlobais();
                PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = keyFigureProjection.getDemandPlan().getPerfilExecucaoDemandPlan();
                atualizaKeyFigureProjectionDPComSalesEmPeriodosPassadosEFuturos(keyFigureProjection, keyFigureStandard, perfilExecucaoDemandPlan.getTipoDocumentoVenda(parametrosGlobais));
            }
            default -> materializaKeyFigureDemandPlanningForaDoCatalogoCommunity(
                    keyFigureProjection,
                    keyFigureStandard);
        }

    }

    /**
     * Carrega uma key figure exibida no Planning Book de Demand Planning.
     *
     * <p>O Community aceita somente KFs standard. O hook permite que o
     * Enterprise materialize suas linhas privadas sem expor o repository ou a
     * entidade de Custom Key Figure ao modulo aberto.</p>
     */
    protected void atualizaKeyFigureProjectionDemandPlanningComKeyFigure(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure) {

        if (!(keyFigure instanceof KeyFigureStandard keyFigureStandard)) {
            materializaKeyFigureDemandPlanningForaDoCatalogoCommunity(
                    keyFigureProjection,
                    keyFigure);
            return;
        }

        switch (keyFigureStandard.getKeyFigureStandardEnum()) {
            case BASELINE, AJUSTE_DEMANDA, HISTORICO_VENDAS ->
                    atualizaKeyFigureProjectionDPParaKfsStandardDp(
                            keyFigureProjection,
                            keyFigureStandard);
            case DEMANDA_DIRETA_TOTAL_DP -> {
                // Totalizadores sao calculados depois dos componentes.
            }
            case ITENS_NOVOS, UPLIFT, CARTEIRA, VENDAS_GROSS, VENDAS_NET,
                    DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL ->
                    materializaKeyFigureDemandPlanningForaDoCatalogoCommunity(
                            keyFigureProjection,
                            keyFigureStandard);
            default ->
                    materializaKeyFigureDemandPlanningForaDoCatalogoCommunity(
                            keyFigureProjection,
                            keyFigureStandard);
        }

    }

    protected void atualizaKeyFigureProjectionDPComDemandPlanItem(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Function<DemandPlanItem, Double> funcaoExtratoraDemandPlanItem) {

        UnidadeMedidaProjection unidadeMedidaProjection = keyFigureProjection.unidadeMedidaProjection;
        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;

        keyFigureProjection.demandPlanningProjectionReferenciaCache.getTodosDemandPlanItems().parallelStream().forEach(demandPlanItem -> {
            double valorDemandPlanItem = funcaoExtratoraDemandPlanItem.apply(demandPlanItem);

            if (Math.abs(valorDemandPlanItem) >= 0.00001) {
                keyFigureProjection.biEmMemoriaDFUDataKeyFigure.addDadoAoBI(DFUDataKeyFigurePadrao.builder()
                        .location(demandPlanItem.getLocation())
                        .produto(demandPlanItem.getProduto())
                        .data(demandPlanItem.getDataReferencia())
                        .keyFigure(keyFigure)
                        .valor((double) (unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                demandPlanItem.getProduto(),
                                demandPlanItem.getUnidadeMedida(parametrosGlobais),
                                configuredViewProjection.getUnidadeMedidaView(parametrosGlobais))
                                * valorDemandPlanItem))
                        .build());
            }

        });

    }

    /**
     * Recompõe uma série derivada a partir das Key Figures quantitativas já
     * materializadas no BI da projection.
     *
     * <p>O Community usa este método para {@code Direct Demand} visível. O
     * overlay Enterprise também pode reutilizá-lo para uma série interna, sem
     * acrescentar uma coluna à grade, quando uma Key Figure derivada depender
     * do total quantitativo oficial.</p>
     */
    protected void atualizaKeyFigureProjectionDPComKeyFigureSoma(
            KeyFigureProjection keyFigureProjection,
            Collection<KeyFigureInterface> keyFiguresInput,
            KeyFigureInterface keyFigureOutputSoma,
            @Nullable Integer periodoInicial,
            @Nullable Integer periodoFinal) {

        Calendario calendario = keyFigureProjection.calendario;

        // Indexa os dados de entrada antes de somar para evitar buscas repetidas no BI em memoria.
        Map<Location, Map<Produto, Map<LocalDateTime, List<DFUDataKeyFigurePadrao>>>> dadosIndexados = keyFigureProjection.getDadosKeyFigures(keyFiguresInput)
                .parallelStream()
                .filter(dadoKeyFigureInput -> {
                    if (periodoInicial != null || periodoFinal != null) {
                        int posicaoPeriodoDadoKeyFigureInput = calendario.getPosicaoPeriodo(dadoKeyFigureInput.getData());
                        if (periodoInicial != null && posicaoPeriodoDadoKeyFigureInput < periodoInicial) return false;
                        if (periodoFinal != null && posicaoPeriodoDadoKeyFigureInput > periodoFinal) return false;
                    }
                    return true;
                })
                .map(dadoKeyFigureInput -> (DFUDataKeyFigurePadrao) dadoKeyFigureInput)
                .collect(Collectors.groupingBy(
                        DFUDataKeyFigurePadrao::getLocation,
                        Collectors.groupingBy(
                                DFUDataKeyFigurePadrao::getProduto,
                                Collectors.groupingBy(
                                        DFUDataKeyFigurePadrao::getData,
                                        Collectors.toList()))));

        dadosIndexados.entrySet().parallelStream().forEach(subEntry1DadosIndexados -> {
            Location location = subEntry1DadosIndexados.getKey();

            List<DFUDataKeyFigureAbstract> dadosKeyFigureSomaFiltrados = keyFigureProjection.getDadosKeyFigure(keyFigureOutputSoma, null, location);
            Map<Produto, Map<LocalDateTime, DFUDataKeyFigurePadrao>> dadosKeyFigureSomaFiltradosIndexados1 =  dadosKeyFigureSomaFiltrados
                    .parallelStream()
                    .filter(dadoKeyFigureSoma -> {
                        if (periodoInicial != null || periodoFinal != null) {
                            int posicaoPeriodoDadoKeyFigureInput = calendario.getPosicaoPeriodo(dadoKeyFigureSoma.getData());
                            if (periodoInicial != null && posicaoPeriodoDadoKeyFigureInput < periodoInicial) return false;
                            if (periodoFinal != null && posicaoPeriodoDadoKeyFigureInput > periodoFinal) return false;
                        }
                        return true;
                    })
                    .map(dadoKeyFigureSoma -> (DFUDataKeyFigurePadrao) dadoKeyFigureSoma)
                    .collect(Collectors.groupingBy(
                            DFUDataKeyFigurePadrao::getProduto,
                            Collectors.toMap(
                                    DFUDataKeyFigurePadrao::getData,
                                    Function.identity())));

            subEntry1DadosIndexados.getValue().entrySet().parallelStream().forEach(subEntry2DadosIndexados -> {
                Produto produto = subEntry2DadosIndexados.getKey();

                Map<LocalDateTime, DFUDataKeyFigurePadrao> dadosKeyFigureSomaFiltradosIndexados2 = dadosKeyFigureSomaFiltradosIndexados1
                        .getOrDefault(produto, new HashMap<>());

                for (Map.Entry<LocalDateTime, List<DFUDataKeyFigurePadrao>> subEntry3DadosIndexados : subEntry2DadosIndexados.getValue().entrySet()) {
                    LocalDateTime data = subEntry3DadosIndexados.getKey();

                    DFUDataKeyFigurePadrao dadoKeyFigureOutputSoma = dadosKeyFigureSomaFiltradosIndexados2
                            .get(data);
                    if (dadoKeyFigureOutputSoma == null) {
                        int posicaoPeriodo = keyFigureProjection.getCalendario().getPosicaoPeriodo(data);
                        dadoKeyFigureOutputSoma = keyFigureProjection.addDadoDFUKeyFigurePadrao(
                                location,
                                produto,
                                posicaoPeriodo,
                                keyFigureOutputSoma,
                                0.0);
                    }

                    List<DFUDataKeyFigurePadrao> dadosKeyFiguresInput = subEntry3DadosIndexados.getValue();
                    double somaValoresKeyFiguresInput = dadosKeyFiguresInput
                            .stream()
                            .mapToDouble(DFUDataKeyFigurePadrao::getValor)
                            .sum();
                    dadoKeyFigureOutputSoma.setValor(somaValoresKeyFiguresInput);

                }
            });
        });

    }

    /**
     * Extrai e materializa uma serie de estoque usando a fotografia batch ja
     * carregada de Supply Planning e a conversao oficial da unidade da view.
     *
     * <p>O helper e protegido para que overlays privados reutilizem o mesmo
     * cache, filtro e BI em memoria sem consultas por celula.</p>
     */
    protected void atualizaKeyFigureProjectionSNPComEstoque(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoPlano tipoPlano,
            ToDoubleBiFunction<InventoryPlanLinha, Constantes.TipoPlano> funcaoExtratoraInventoryPlanLinha) {

        atualizaCacheKeyFigureProjectionComSupplyPlanningProjection(keyFigureProjection);

        SupplyPlanningProjection supplyPlanningProjection = keyFigureProjection.supplyPlanningProjectionCache;
        UnidadeMedidaProjection unidadeMedidaProjection = keyFigureProjection.unidadeMedidaProjection;
        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
        ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
        List<InventoryPlanLinha> inventoryPlanLinhaList = keyFigureProjection.supplyPlanningProjectionCache.getTodosInventoryPlanLinhas();

        inventoryPlanLinhaList.stream()
                .filter(x -> Math.abs(funcaoExtratoraInventoryPlanLinha.applyAsDouble(x, tipoPlano)) > 0.00001)
                .forEach(ThrowingConsumer.unchecked(inventoryPlanLinha -> {
                    double quantidadeEstoque = funcaoExtratoraInventoryPlanLinha.applyAsDouble(inventoryPlanLinha, tipoPlano);

                    if (quantidadeEstoque != 0.0) {
                        keyFigureProjection.biEmMemoriaDFUDataKeyFigure.addDadoAoBI(DFUDataKeyFigurePadrao.builder()
                                .location(supplyPlanningProjection.getLocation())
                                .produto(inventoryPlanLinha.getProduto())
                                .data(inventoryPlanLinha.getDataReferencia())
                                .keyFigure(keyFigure)
                                .valor((double) (unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                        inventoryPlanLinha.getProduto(),
                                        inventoryPlanLinha.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais()),
                                        configuredViewProjection.getUnidadeMedidaView(clusterEParametrosProjection.getParametrosGlobais()))
                                        * quantidadeEstoque))
                                .build());
                    }
                }));

    }

    /**
     * Materializa uma serie fora do catalogo Community. Sem extensao, a
     * recusa acontece neste ponto unico, sem uma ramificacao por capacidade.
     */
    protected void materializaKeyFigureDemandPlanningForaDoCatalogoCommunity(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure) {

        throw new RequiresEnterpriseVersionException("Demand Planning key figure selection");

    }

    /**
     * Materializa uma serie de Supply fora do catalogo Community. A extensao
     * pode reutilizar os helpers batch sem a factory enumerar series privadas.
     */
    protected void materializaKeyFigureSupplyPlanningForaDoCatalogoCommunity(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoPlano tipoPlano) {

        throw new RequiresEnterpriseVersionException("Supply Planning key figure selection");

    }

    private void atualizaCacheKeyFigureProjectionComDemandPlanningProjection(
            KeyFigureProjection keyFigureProjection) {

        // se houver cache, usá-lo
        if (keyFigureProjection.demandPlanningProjectionReferenciaCache == null) {
            // caso contrário, popular o cache e retorná-lo
            ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;

            keyFigureProjection.demandPlanningProjectionReferenciaCache = demandPlanProjectionFactory.getDemandPlanningProjectionCompleto(
                    keyFigureProjection.demandPlan,
                    configuredViewProjection.getDfuProjectionFiltrado(),
                    false);
        }

    }

    private void atualizaCacheKeyFigureProjectionComSalesProjection(KeyFigureProjection keyFigureProjection, Constantes.TipoDocumentoVenda tipoDocumentoVenda) {

        // se houver cache, usá-lo
        if (keyFigureProjection.salesProjectionCache == null) {
            // caso contrário, popular o cache e retorná-lo
            Calendario calendarioDemandPlan = keyFigureProjection.calendario;
            ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
            UnidadeMedidaProjection unidadeMedidaProjection = keyFigureProjection.unidadeMedidaProjection;
            ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
            // Se a configured view não especificar períodos históricos, não extrair nada da base de vendas.
            // O tipo de documento de vendas é definido pelo perfil de execução do Demand Plan
            // e chega a este método pelo argumento tipoDocumentoVenda.
            int numeroPeriodosHistoricosSales = configuredViewProjection.getConfiguredView().getNumeroPeriodosHistoricosDemandPlanningBook();
            if (numeroPeriodosHistoricosSales <= 0) {
                keyFigureProjection.salesProjectionCache = salesProjectionFactory.getSalesProjectionLocationMaterialDataVazio(
                        calendarioDemandPlan,
                        configuredViewProjection.getLocationsFiltradas(),
                        configuredViewProjection.getMateriaisFiltrados(),
                        unidadeMedidaProjection,
                        clusterEParametrosProjection);
                return;
            }

            LocalDateTime dataHorarioInicialCalendarioSales = calendarioDemandPlan.getDataHorarioInicialPresente();

            Calendario calendarioSales = Calendario.criaCalendarioDeDatas(
                    calendarioDemandPlan.getTamanhoBucket(),
                    Calendario.getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                            dataHorarioInicialCalendarioSales,
                            -numeroPeriodosHistoricosSales,
                            calendarioDemandPlan.getTamanhoBucket()),
                    dataHorarioInicialCalendarioSales,
                    calendarioDemandPlan.getDataHorarioFinalFutura());

            // A projection de vendas usada pelo Planning Book Community consolida vendas por dia.
            // Buckets menores que diario exigem outra projection de sales e permanecem fora deste contrato.
            keyFigureProjection.salesProjectionCache = salesProjectionFactory.getSalesProjectionLocationMaterialData(
                    tipoDocumentoVenda,
                    calendarioSales,
                    configuredViewProjection.getLocationsFiltradas(),
                    configuredViewProjection.getMateriaisFiltrados(),
                    unidadeMedidaProjection,
                    clusterEParametrosProjection,
                    clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal());
        }

    }

    private void atualizaKeyFigureProjectionDPComVendaEmPeriodosPassados(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoDocumentoVenda tipoDocumentoVenda) {

        atualizaCacheKeyFigureProjectionComSalesProjection(keyFigureProjection, tipoDocumentoVenda);

        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
        UnidadeMedidaProjection unidadeMedidaProjection = keyFigureProjection.unidadeMedidaProjection;
        ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        LocalDateTime dataHorarioInicialPresente = keyFigureProjection.calendario.getDataHorarioInicialPresente();

        keyFigureProjection.salesProjectionCache.getSetSalesConsolidado().stream()
                .filter(aggregatedByLocationMaterialUOMDate -> aggregatedByLocationMaterialUOMDate.getReferenceDate().isBefore(dataHorarioInicialPresente.toLocalDate()))
                .forEach(ThrowingConsumer.unchecked(aggregatedByLocationMaterialUOMDate -> {
                    double quantidadeSales = aggregatedByLocationMaterialUOMDate.getTotalQuantity();
                    if (Math.abs(quantidadeSales) >= 0.00001) {

                        // A projection de sales e diaria; por isso o ponto historico entra no inicio do dia.
                        // Granularidade intradiaria deve ser implementada em outra projection, sem alterar este contrato.
                        keyFigureProjection.biEmMemoriaDFUDataKeyFigure.addDadoAoBI(DFUDataKeyFigurePadrao.builder()
                                .location(aggregatedByLocationMaterialUOMDate.getLocation())
                                .produto(aggregatedByLocationMaterialUOMDate.getMaterial())
                                .data(aggregatedByLocationMaterialUOMDate.getReferenceDate()
                                        .atStartOfDay())
                                .keyFigure(keyFigure)
                                .valor(quantidadeSales
                                        * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                        aggregatedByLocationMaterialUOMDate.getMaterial(),
                                        aggregatedByLocationMaterialUOMDate.getUom(),
                                        configuredViewProjection.getUnidadeMedidaView(parametrosGlobais)))
                                .build());

                    }
        }));

    }

    private void atualizaKeyFigureProjectionDPComSalesEmPeriodosPassadosEFuturos(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoDocumentoVenda tipoDocumentoVenda) {

        atualizaCacheKeyFigureProjectionComSalesProjection(keyFigureProjection, tipoDocumentoVenda);

        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
        UnidadeMedidaProjection unidadeMedidaProjection = keyFigureProjection.unidadeMedidaProjection;
        ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        keyFigureProjection.salesProjectionCache.getSetSalesConsolidado().stream()
                .forEach(ThrowingConsumer.unchecked(aggregatedByLocationMaterialUOMDate -> {
                    double quantidadeSales = aggregatedByLocationMaterialUOMDate.getTotalQuantity();
                    if (Math.abs(quantidadeSales) >= 0.00001) {

                        // A projection de sales e diaria; por isso o ponto historico entra no inicio do dia.
                        // Granularidade intradiaria deve ser implementada em outra projection, sem alterar este contrato.
                        keyFigureProjection.biEmMemoriaDFUDataKeyFigure.addDadoAoBI(DFUDataKeyFigurePadrao.builder()
                                .location(aggregatedByLocationMaterialUOMDate.getLocation())
                                .produto(aggregatedByLocationMaterialUOMDate.getMaterial())
                                .data(aggregatedByLocationMaterialUOMDate.getReferenceDate()
                                        .atStartOfDay())
                                .keyFigure(keyFigure)
                                .valor(quantidadeSales
                                        * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                        aggregatedByLocationMaterialUOMDate.getMaterial(),
                                        aggregatedByLocationMaterialUOMDate.getUom(),
                                        configuredViewProjection.getUnidadeMedidaView(parametrosGlobais)))
                                .build());
                    }
        }));

    }

    protected void atualizaKeyFigureProjectionSNPComProducao(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoPlano tipoPlano,
            ToDoubleBiFunction<ProductionPlanLinha, Constantes.TipoPlano> funcaoExtratoraProducationPlanLinha) {

        atualizaCacheKeyFigureProjectionComSupplyPlanningProjection(keyFigureProjection);

        SupplyPlanningProjection supplyPlanningProjection = keyFigureProjection.supplyPlanningProjectionCache;
        UnidadeMedidaProjection unidadeMedidaProjection = keyFigureProjection.unidadeMedidaProjection;
        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
        ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
        List<ProductionPlanLinha> productionPlanLinhaList = supplyPlanningProjection.getTodosProductionPlanLinhasOutput();

        productionPlanLinhaList.stream()
                .filter(x -> Math.abs(funcaoExtratoraProducationPlanLinha.applyAsDouble(x, tipoPlano)) > 0.00001)
                .forEach(ThrowingConsumer.unchecked(productionPlanLinha -> {
                    double quantidadeProducao = funcaoExtratoraProducationPlanLinha.applyAsDouble(productionPlanLinha, tipoPlano);
                    keyFigureProjection.biEmMemoriaDFUDataKeyFigure.addDadoAoBI(DFUDataKeyFigurePadrao.builder()
                            .location(supplyPlanningProjection.getLocation())
                            .produto(productionPlanLinha.getMaterialOutput())
                            .data(productionPlanLinha.getDataReferencia())
                            .keyFigure(keyFigure)
                            .valor((double) (unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                    productionPlanLinha.getMaterialOutput(),
                                    productionPlanLinha.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais()),
                                    configuredViewProjection.getUnidadeMedidaView(clusterEParametrosProjection.getParametrosGlobais()))
                                    * quantidadeProducao))
                            .build());
                }));

    }

    /**
     * Materializa uma serie inbound pela fotografia batch de Supply Planning,
     * usando a data de recebimento e a conversao oficial da unidade da view.
     *
     * <p>O helper e protegido para que overlays privados possam reutilizar o
     * cache e o BI em memoria sem leitura por celula.</p>
     */
    protected void atualizaKeyFigureProjectionSNPComInbound(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoPlano tipoPlano,
            ToDoubleBiFunction<DistributionPlanItem, Constantes.TipoPlano> funcaoExtratoraDistributionPlanItem) {

        atualizaCacheKeyFigureProjectionComSupplyPlanningProjection(keyFigureProjection);

        SupplyPlanningProjection supplyPlanningProjection = keyFigureProjection.supplyPlanningProjectionCache;
        UnidadeMedidaProjection unidadeMedidaProjection = keyFigureProjection.unidadeMedidaProjection;
        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
        ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
        Collection<DistributionPlanItem> distributionPlanItemList = supplyPlanningProjection.getTodosDistributionPlanItemsInboundSet();

        distributionPlanItemList.stream()
                .filter(x -> Math.abs(funcaoExtratoraDistributionPlanItem.applyAsDouble(x, tipoPlano)) > 0.00001)
                .forEach(ThrowingConsumer.unchecked(distributionPlanItem -> {
                    double quantidadeInbound = funcaoExtratoraDistributionPlanItem.applyAsDouble(distributionPlanItem, tipoPlano);
                    keyFigureProjection.biEmMemoriaDFUDataKeyFigure.addDadoAoBI(DFUDataKeyFigurePadrao.builder()
                            .location(supplyPlanningProjection.getLocation())
                            .produto(distributionPlanItem.getProduto())
                            .data(distributionPlanItem.getDataRecebimento())
                            .keyFigure(keyFigure)
                            .valor((double) (unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                    distributionPlanItem.getProduto(),
                                    distributionPlanItem.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais()),
                                    configuredViewProjection.getUnidadeMedidaView(clusterEParametrosProjection.getParametrosGlobais()))
                                    * quantidadeInbound))
                            .build());
                }));

    }

    /**
     * Materializa uma serie outbound pela fotografia batch de Supply Planning,
     * usando a data de expedicao e a conversao oficial da unidade da view.
     *
     * <p>O helper permanece protegido porque a disponibilidade da KF e uma
     * decisao do overlay Enterprise, enquanto o cache e o BI em memoria seguem
     * sendo responsabilidade compartilhada.</p>
     */
    protected void atualizaKeyFigureProjectionSNPComOutbound(
            KeyFigureProjection keyFigureProjection,
            KeyFigureInterface keyFigure,
            Constantes.TipoPlano tipoPlano,
            ToDoubleBiFunction<DistributionPlanItem, Constantes.TipoPlano> funcaoExtratoraDistributionPlanItem) {

        atualizaCacheKeyFigureProjectionComSupplyPlanningProjection(keyFigureProjection);

        SupplyPlanningProjection supplyPlanningProjection = keyFigureProjection.supplyPlanningProjectionCache;
        UnidadeMedidaProjection unidadeMedidaProjection = keyFigureProjection.unidadeMedidaProjection;
        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.configuredViewProjection;
        ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
        Collection<DistributionPlanItem> distributionPlanItemList = supplyPlanningProjection.getTodosDistributionPlanItemsOutboundSet();

        distributionPlanItemList.stream()
                .filter(x -> Math.abs(funcaoExtratoraDistributionPlanItem.applyAsDouble(x, tipoPlano)) > 0.00001)
                .forEach(ThrowingConsumer.unchecked(distributionPlanItem -> {
                    double quantidadeOutbound = funcaoExtratoraDistributionPlanItem.applyAsDouble(distributionPlanItem, tipoPlano);
                    keyFigureProjection.biEmMemoriaDFUDataKeyFigure.addDadoAoBI(DFUDataKeyFigurePadrao.builder()
                            .location(supplyPlanningProjection.getLocation())
                            .produto(distributionPlanItem.getProduto())
                            .data(distributionPlanItem.getDataExpedicao())
                            .keyFigure(keyFigure)
                            .valor((double) (unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                    distributionPlanItem.getProduto(),
                                    distributionPlanItem.getUnidadeMedida(clusterEParametrosProjection.getParametrosGlobais()),
                                    configuredViewProjection.getUnidadeMedidaView(clusterEParametrosProjection.getParametrosGlobais()))
                                    * quantidadeOutbound))
                            .build());
                }));

    }

    /**
     * Libera as projections auxiliares depois que a projection de Key Figures
     * foi completamente montada para o Planning Book.
     */
    protected void limpaCacheKeyFigureProjection(KeyFigureProjection keyFigureProjection) {
        keyFigureProjection.salesProjectionCache = null;
        keyFigureProjection.supplyPlanningProjectionCache = null;
        keyFigureProjection.demandPlanningProjectionReferenciaCache = null;
        keyFigureProjection.splitTemporalProjectionPorDfuCalendarioDPParaSNPCache = null;
        keyFigureProjection.politicaEstoquesProjectionCache = null;
    }

}
