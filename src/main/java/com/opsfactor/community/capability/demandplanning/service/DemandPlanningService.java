package com.opsfactor.community.capability.demandplanning.service;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosGeraisDemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjectionSimples;
import com.opsfactor.community.capability.demandplanning.configuration.projection.factory.ParametrosDemandPlanningProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.capability.cluster.repository.material.ClusterProdutosDemandPlanningRepository;
import com.opsfactor.community.capability.demandplanning.configuration.repository.PerfilExecucaoDemandPlanRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanItemRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.HistoricoDemandPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.cluster.service.ClusterLocationService;
import com.opsfactor.community.capability.demandplanning.engine.DemandPlanning;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastStockoutContext;
import com.opsfactor.community.capability.demandplanning.forecast.service.DemandForecastWorkflowService;
import com.opsfactor.community.capability.demandplanning.service.spi.DemandPlanReferenceCopySpi;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.capability.demandplanning.forecast.configuration.DemandPlanningModelCatalog;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.NoResultException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service principal de Demand Planning Community.
 *
 * <p>Esta classe coordena apenas forecast estatistico Community, persistencia
 * de baseline/ajuste e operacoes de Planning Book em nivel material/location.
 * Pricing, listas de preco, auto-fit, regression tree, custom key figures,
 * uplift por evento/preco, sucessao de materiais e foundation models pertencem
 * ao OpsFactor Enterprise e devem entrar por services/overlays especificos.</p>
 */
@Slf4j
@Service
public class DemandPlanningService {

    /**
     * Fronteira da copia de plano de referencia. O bean Community rejeita a
     * capability; o Enterprise a reabre sem duplicar a geracao base.
     */
    @Autowired
    private DemandPlanReferenceCopySpi demandPlanReferenceCopySpi;

    /**
     * Inicia uma transacao curta somente para limpeza apos falha da geracao.
     *
     * <p>A execucao principal nao e transacional porque paraleliza clusters. O
     * catch, por outro lado, precisa de transacao real mesmo sendo chamada
     * interna: o template mantem o dispatch Enterprise de
     * {@link #deleteDemandPlan(Long)} e seus deletes privados antes do header.</p>
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Service de clusters de location usado para descobrir os clusters com
     * Demand Planning habilitado durante a geracao de uma versao.
     */
    @Autowired
    private ClusterLocationService clusterLocationService;

    /**
     * Workflow Spring que coordena engine estatistica e desagregacao. Esta
     * dependencia e o ponto natural para o Enterprise aplicar `@Primary` quando
     * trouxer foundation models, HTS/STL ou splits compostos.
     */
    @Autowired
    private DemandForecastWorkflowService demandForecastWorkflowService;

    /**
     * Repository das linhas do Demand Plan. Todas as escritas Community passam
     * por neutralizacao das KFs Enterprise antes de chegar neste bean.
     */
    @Autowired
    private DemandPlanItemRepository demandPlanItemRepository;

    /**
     * Repository do historico de linhas usado na limpeza de versoes antigas.
     */
    @Autowired
    private HistoricoDemandPlanItemRepository historicoDemandPlanItemRepository;

    /**
     * Repository transicional do cluster de materiais de Demand Planning.
     */
    @Autowired
    private ClusterProdutosDemandPlanningRepository clusterMateriaisDemandPlanningRepository;

    /**
     * Repository de Demand Plan usado para criar, consultar e remover versoes.
     */
    @Autowired
    private DemandPlanRepository demandPlanRepository;

    /**
     * Repository de Supply Plan usado para desalocar planos de supply
     * associados quando uma versao de demanda e removida.
     */
    @Autowired
    private SupplyPlanRepository supplyPlanRepository;

    /**
     * Repository do perfil de execucao usado para carregar bucket, horizonte e
     * documento historico antes da geracao.
     */
    @Autowired
    private PerfilExecucaoDemandPlanRepository perfilExecucaoDemandPlanRepository;

    /**
     * Factory de historico de vendas. No Community ela aceita apenas sell-out.
     */
    @Autowired
    private SalesProjectionFactory salesProjectionFactory;

    /**
     * Factory de projection de Demand Plan usada para salvar linhas geradas ou
     * recalculadas em memoria.
     */
    @Autowired
    private DemandPlanProjectionFactory demandPlanProjectionFactory;

    /**
     * Factory da projection global de parametros, clusters e master data.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Factory da projection de UOM/conversoes usada no forecast e persistencia
     * do plano.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /**
     * Factory de parametros Demand Planning por perfil/cluster.
     */
    @Autowired
    private ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory;

    /**
     * Usado apenas para updates em massa defensivos, como reset restrito
     * neutralizando KFs Enterprise transicionais.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Valida o subconjunto de forecast disponivel no OpsFactor Community.
     *
     * <p>A validacao fica no service porque ela precisa falhar antes de construir
     * projections, carregar historico de vendas ou chamar o workflow de forecast.
     * Isso evita executar rotinas Enterprise por acidente quando uma base legada
     * ou payload manual ainda expuser valores completos do contrato.</p>
     */
    protected void validaParametrosForecastCommunity(ParametrosForecastProjection parametrosForecastProjection) {

        if (parametrosForecastProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning forecast parameters are required");
        }
        if (parametrosForecastProjection.getDpModeloEstatistico() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning statistical forecast model is required");
        }
        if (parametrosForecastProjection.getDpModeloSplit() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning forecast split model is required");
        }

        if (!getDpModelosEstatisticosDisponiveis().contains(parametrosForecastProjection.getDpModeloEstatistico())) {
            throw new RequiresEnterpriseVersionException("Demand Planning Forecast Model " + parametrosForecastProjection.getDpModeloEstatistico());
        }

        if (!getDpModelosSplitDisponiveis().contains(parametrosForecastProjection.getDpModeloSplit())) {
            throw new RequiresEnterpriseVersionException("Demand Planning Split Model " + parametrosForecastProjection.getDpModeloSplit());
        }

    }

    /**
     * Modelos estatisticos disponiveis para esta implementacao do service.
     *
     * <p>O Community retorna somente os modelos abertos. O Enterprise pode
     * sobrepor este hook em um service `@Primary`, liberando modelos estatisticos
     * privados ja migrados sem copiar o fluxo principal de Demand Planning.</p>
     */
    protected Set<Constantes.DPModeloEstatistico> getDpModelosEstatisticosDisponiveis() {

        return DemandPlanningModelCatalog.getDpModelosEstatisticosCommunity();

    }

    /**
     * Modelos de split disponiveis para esta implementacao do service.
     *
     * <p>O Community retorna somente Historical Sales. O Enterprise pode
     * sobrepor este hook para liberar splits ja migrados sem copiar o fluxo
     * principal de Demand Planning nem transformar a decisao em flag de edicao.</p>
     */
    protected Set<Constantes.DPModeloSplit> getDpModelosSplitDisponiveis() {

        return DemandPlanningModelCatalog.getDpModelosSplitCommunity();

    }

    /**
     * Valida configuracoes de perfil que podem ser carregadas diretamente do
     * banco antes da construcao das projections. No Community o perfil fisico
     * nao mapeia MAPE, auto-fit nem arvore de regressao; a validacao restante
     * protege apenas contra bases legadas com documento historico diferente de
     * sell-out.
     */
    protected void validaPerfilExecucaoDemandPlanDisponivel(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            ParametrosGlobais parametrosGlobais) {

        if (perfilExecucaoDemandPlan == null) {
            throw new IllegalArgumentException(
                    "Demand Planning execution profile is required");
        }
        if (parametrosGlobais == null) {
            throw new IllegalArgumentException(
                    "Demand Planning global parameters are required");
        }

        Constantes.TipoDocumentoVenda tipoDocumentoVenda = perfilExecucaoDemandPlan.getTipoDocumentoVenda(parametrosGlobais);
        if (!getTiposDocumentoHistoricoDisponiveis().contains(tipoDocumentoVenda)) {
            throw new RequiresEnterpriseVersionException("Sell-in and sales orders as historical sales source");
        }

    }

    /**
     * Tipos de documento historico disponiveis para esta implementacao.
     *
     * <p>O Community aceita apenas sell-out. O Enterprise sobrescreve este hook
     * para liberar sell-in quando a factory/projection privada esta no
     * classpath, preservando o mesmo fluxo principal de execucao.</p>
     */
    protected Set<Constantes.TipoDocumentoVenda> getTiposDocumentoHistoricoDisponiveis() {

        return DemandPlanningModelCatalog.getTiposDocumentoHistoricoCommunity();

    }

    /**
     * Valida a fotografia estrutural compartilhada da rodada de Demand Planning.
     *
     * <p>Essa projection vem de cache/factory no fluxo normal e pode ser
     * recebida diretamente em simulações/testes. Se estiver ausente, a falha
     * deve acontecer antes de ler parametros globais, clusters ou projections
     * derivadas. `ParametrosGlobais` tambem e parte obrigatoria dessa
     * fotografia, pois documento historico, calendario e defaults de UOM
     * dependem dele logo em seguida.</p>
     */
    /**
     * Resolve a projection de conversao de unidades consumida pela execucao de
     * Demand Planning.
     *
     * <p>O snapshot de UOM e carregado de cache/factory antes da extraction de
     * sales e antes de arredondamentos. Quando a factory retorna nulo, o erro e
     * de montagem do runtime/projection e deve falhar aqui, nao dentro da
     * projection de vendas ou da rotina de arredondamento.</p>
     */
    /**
     * Valida a data inicial funcional da rodada antes de carregar perfil,
     * clusters ou projections.
     *
     * <p>A data inicial define o calendario persistido no `DemandPlan`. Sem ela,
     * a chamada nao representa uma execucao vazia: representa payload incompleto
     * do controller/task. Falhar aqui evita criar versao parcial ou depender de
     * `NullPointerException` ao logar ou calcular a primeira data do bucket.</p>
     */
    /**
     * Valida a fotografia de clusters de location retornada pelo service de
     * master data antes do filtro de clusters planejaveis.
     *
     * <p>Lista vazia significa que nao ha clusters a executar. Lista nula, item
     * nulo ou cluster sem id, por outro lado, indica quebra da borda de
     * extracao/cache e deve falhar antes de paralelizar a rodada ou criar um
     * Demand Plan sem escopo confiavel.</p>
     */
    private List<ClusterLocations> getClusterLocationsDemandPlanningParaExecucaoCommunity(
            List<ClusterLocations> clusterLocationList) {

        if (clusterLocationList == null) {
            throw new IllegalArgumentException(
                    "Demand Planning location cluster collection is required for Demand Planning execution.");
        }

        int indiceClusterLocations = 0;
        for (ClusterLocations clusterLocations : clusterLocationList) {
            if (clusterLocations == null) {
                throw new IllegalArgumentException(
                        "Demand Planning location cluster at index "
                                + indiceClusterLocations
                                + " is required for Demand Planning execution.");
            }
            if (clusterLocations.getId() == null) {
                throw new IllegalArgumentException(
                        "Demand Planning location cluster at index "
                                + indiceClusterLocations
                                + " must have an id for Demand Planning execution.");
            }
            indiceClusterLocations++;
        }

        return clusterLocationList;

    }

    /**
     * Valida a fotografia de clusters de materiais antes da ordenacao por
     * prioridade.
     *
     * <p>O tipo fisico ainda se chama `ClusterProdutosDemandPlanning`, mas o
     * contrato publico Community e cluster de materiais. Snapshot nulo, item
     * nulo ou cluster sem id deve falhar antes do sort e antes de gerar qualquer
     * linha parcial do plano.</p>
     */
    private List<ClusterProdutosDemandPlanning> getClusterMateriaisDemandPlanningParaExecucaoCommunity(
            List<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningList) {

        if (clusterMateriaisDemandPlanningList == null) {
            throw new IllegalArgumentException(
                    "Demand Planning material cluster collection is required for Demand Planning execution.");
        }

        int indiceClusterMateriaisDemandPlanning = 0;
        for (ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning : clusterMateriaisDemandPlanningList) {
            if (clusterMateriaisDemandPlanning == null) {
                throw new IllegalArgumentException(
                        "Demand Planning material cluster at index "
                                + indiceClusterMateriaisDemandPlanning
                                + " is required for Demand Planning execution.");
            }
            if (clusterMateriaisDemandPlanning.getId() == null) {
                throw new IllegalArgumentException(
                        "Demand Planning material cluster at index "
                                + indiceClusterMateriaisDemandPlanning
                                + " must have an id for Demand Planning execution.");
            }
            indiceClusterMateriaisDemandPlanning++;
        }

        return clusterMateriaisDemandPlanningList;

    }

    /**
     * Valida a projection consolidada de parametros de Demand Planning carregada
     * para o perfil.
     *
     * <p>Essa projection contem o mapa de combinacoes cluster location/material.
     * Se a factory/cache retornar nulo, a execucao nao deve seguir ate calendario
     * maximo, loops paralelos ou sales projection, pois o service nao tem como
     * decidir quais combinacoes executam plano de demanda.</p>
     */
    /**
     * Valida a configuracao efetiva da combinacao cluster location/material antes
     * de ler parametros gerais ou criar calendario especifico.
     *
     * <p>Uma combinacao ausente na projection de parametros e erro de
     * materializacao da configuracao. O Community deve falhar com contexto
     * funcional em vez de estourar NPE dentro do loop paralelo, onde a causa fica
     * mais dificil de rastrear.</p>
     */
    /**
     * Reference Plan e copia a partir de outro Demand Plan sao capacidades
     * Enterprise.
     *
     * <p>A validacao roda antes de criar qualquer plano novo no Community. Isso
     * evita que payloads manuais, legados ou transicionais com campos
     * Enterprise iniciem uma rodada que depois precisaria ser revertida.</p>
     */
    private void validaReferencePlanExecucaoCommunity(
            Long demandPlanIdReferenciaCopiaDados,
            boolean copiaApenasNoHorizonteCongelado) {

        demandPlanReferenceCopySpi.validateReferencePlanRequest(
                demandPlanIdReferenciaCopiaDados,
                copiaApenasNoHorizonteCongelado);

    }

    /**
     * Entrada usada pelo controller quando a tela envia o periodo em formato textual.
     *
     * <p>O metodo traduz o periodo para a primeira data do bucket configurado no
     * perfil e reutiliza a entrada principal de execucao. A validacao de
     * Reference Plan roda antes da consulta ao perfil para falhar cedo em
     * payloads Enterprise enviados ao Community.</p>
     */
    public Long executaDemandPlanning(
            String perfilExecucaoDemandPlanId,
            String dataReferenciaComoString,
            String descricao,
            Long demandPlanIdReferenciaCopiaDados,
            boolean copiaApenasNoHorizonteCongelado,
            String userId) {

        validaReferencePlanExecucaoCommunity(demandPlanIdReferenciaCopiaDados, copiaApenasNoHorizonteCongelado);

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                perfilExecucaoDemandPlanRepository.findById(perfilExecucaoDemandPlanId)
                        .orElseThrow(() -> new NoResultException(
                                "No Demand Plan execution profiled found with id = "
                                        + perfilExecucaoDemandPlanId));

        LocalDateTime dataInicioPlano = Calendario.getPrimeiraDataFromDescricaoPeriodo(
                dataReferenciaComoString,
                perfilExecucaoDemandPlan.getTamanhoBucket());

        return executaDemandPlanning(
                perfilExecucaoDemandPlanId,
                dataInicioPlano,
                descricao,
                demandPlanIdReferenciaCopiaDados,
                copiaApenasNoHorizonteCongelado,
                userId);

    }

    /**
     * Cria um novo Demand Plan para o periodo selecionado e para os clusters
     * habilitados no perfil. No Community a geracao persiste apenas baseline e
     * ajustes operacionais de demanda; Uplift, New Materials e efeitos de preco
     * sao neutralizados ou bloqueados nas bordas de configuracao.
     */
    public Long executaDemandPlanning(
            String perfilExecucaoDemandPlanId,
            LocalDateTime dataInicioPlano,
            String descricao,
            Long demandPlanIdReferenciaCopiaDados,
            boolean copiaApenasNoHorizonteCongelado,
            String userId) {

        validaReferencePlanExecucaoCommunity(demandPlanIdReferenciaCopiaDados, copiaApenasNoHorizonteCongelado);

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                perfilExecucaoDemandPlanRepository.findById(perfilExecucaoDemandPlanId)
                        .orElseThrow(() -> new NoResultException(
                                "No Demand Plan execution profiled found with id = "
                                        + perfilExecucaoDemandPlanId));

        log.info("Iniciando a execucao do demand planning para a data " + dataInicioPlano + " e descricao " + descricao);
        List<ClusterLocations> clusterLocationList =
                getClusterLocationsDemandPlanningParaExecucaoCommunity(
                        clusterLocationService.getClusterLocationList(false))
                .stream()
                .filter(x -> x.getParametrosClusterLocations().getPlanejaDP())
                .collect(Collectors.toList());
        /*
         * Tipo fisico transicional: ClusterProdutosDemandPlanning. Conceito
         * publico Community: clusters de materiais usados para Demand Planning.
         */
        List<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningList =
                getClusterMateriaisDemandPlanningParaExecucaoCommunity(
                        clusterMateriaisDemandPlanningRepository.findAll());
        // Comecamos rodando os clusters materiais de menor prioridade.
        clusterMateriaisDemandPlanningList.sort(Comparator.comparingInt(ClusterProdutosDemandPlanning::getPrioridade).reversed());

        // Projection de unidade medida usada para converter tudo para a unidade
        // alvo da combinacao cluster material / cluster location definida nos parametros DP.
        UnidadeMedidaProjection unidadeMedidaProjection =
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        ClusterEParametrosProjection clusterEParametrosProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        validaPerfilExecucaoDemandPlanDisponivel(perfilExecucaoDemandPlan, parametrosGlobais);
        ParametrosDemandPlanProjection parametrosDemandPlanProjection =
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(perfilExecucaoDemandPlan);

        if (descricao == null || descricao.isBlank()) {
            descricao = "Demand Plan " + String.valueOf(Calendario.getDescricaoIntegerPeriodo(LocalDateTime.now(), perfilExecucaoDemandPlan.getTamanhoBucket()));
        }

        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setPerfilExecucaoDemandPlan(perfilExecucaoDemandPlan);
        demandPlan.setDescricao(descricao);
        demandPlan.setHorarioGeracao(LocalDateTime.now());
        demandPlan.setUsuarioGeradorPlano((userId != null) ? userId : "System");
        demandPlan.setTamanhoBucket(perfilExecucaoDemandPlan.getTamanhoBucket());
        // ao salvar no banco converte a data para a 1a data do período (diario, semanal ou mensal)
        demandPlan.setDataInicioPlano(Calendario.getPrimeiraDataHorarioPeriodo(dataInicioPlano, perfilExecucaoDemandPlan.getTamanhoBucket()));
        aplicaJanelaEdicaoPlanningBookCommunity(demandPlan, perfilExecucaoDemandPlan);

        // Calendario maximo entre todas as combinacoes cluster material / cluster location.
        Calendario calendarioHistoricoMaximo = demandPlan.getCalendarioDoDemandPlanComHistoricoMaximo(parametrosDemandPlanProjection);
        demandPlan.setDataFimPlano(calendarioHistoricoMaximo.getDataHorarioFinal());

        demandPlan = validaDemandPlanSalvoInicialCommunity(
                demandPlanRepository.saveAndFlush(demandPlan));

        // envolvendo em try/catch para remover o plano caso ele seja novo e tenha ocorrido um erro de execução
        try {

            /*
             * O header ja possui identificador persistido, portanto este e o
             * unico ponto seguro para um overlay Enterprise substituir sua
             * fotografia de preco. O hook precisa anteceder qualquer forecast
             * e permanecer dentro deste try: se a copia privada falhar, o
             * cleanup remove tambem o header que acabou de ser salvo.
             */
            atualizaSnapshotPrecoDemandPlanAposSalvarHeader(demandPlan);

            DemandPlan demandPlanParaLambda = demandPlan;

            clusterLocationList.parallelStream().forEach(clusterLocations -> {

                log.info("Gerando o Demand Plan para ClusterLocations " + clusterLocations.getDescricao());
                if (clusterLocations.getParametrosClusterLocations().getPlanejaDP()) {

                    clusterMateriaisDemandPlanningList.parallelStream().forEach(clusterMateriaisDemandPlanning -> {

                        ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection =
                                parametrosDemandPlanProjection.getParametrosDemandPlanNivelClusterProjection(
                                                clusterLocations,
                                                clusterMateriaisDemandPlanning);
                        ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection =
                                parametrosDemandPlanNivelClusterProjection
                                        .getParametrosGeraisDemandPlanningProjection();

                        if (!parametrosGeraisDemandPlanningProjection.executaPlanoDemanda) return;

                        // Calendario com historico determinado pela combinacao cluster material / cluster location.
                        Calendario calendario = demandPlanParaLambda.getCalendarioDoDemandPlan(
                                parametrosDemandPlanProjection,
                                clusterMateriaisDemandPlanning,
                                clusterLocations);

                        // GERA O FORECAST
                        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao = geraDemandPlanForecastProjectionsExecucaoComForecast(
                                calendario,
                                parametrosDemandPlanNivelClusterProjection,
                                false); // evita preencher o horizonte futuro com historico bruto na SalesProjection.

                        List<DemandPlanItem> demandPlansLinhaClusterLocations =
                                getDemandPlanItemsGeradasParaPersistenciaCommunity(
                                        DemandPlanning.geraDemandPlanItemListDeDemandPlanForecastProjectionsExecucao(
                                                demandPlanParaLambda,
                                                demandPlanForecastProjectionsExecucao,
                                                calendario,
                                                parametrosGeraisDemandPlanningProjection,
                                                clusterEParametrosProjection));

                        // ARREDONDA PARA UNIDADE DE VENDA
                        if (demandPlansLinhaClusterLocations.size() > 0) {
                            if (parametrosGeraisDemandPlanningProjection.isArredondaParaUnidadeVenda()) {
                                log.info("Arredondando plano para unidades de vendas");
                                DemandPlanning.arredondaDemandPlanItemsParaUnidadeVenda(
                                        demandPlansLinhaClusterLocations,
                                        Constantes.ModificacaoAgregadaPlano.PROPORCIONAL_OU_BASELINE,
                                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                                        clusterEParametrosProjection,
                                        unidadeMedidaProjection);
                            }
                            log.info("Salvando demand plan e demand plan linha para Cluster Materiais " + clusterMateriaisDemandPlanning.getDescricao() +
                                    " e Cluster Locations " + clusterLocations.getDescricao());
                            /*
                             * A rotina Community gera apenas Baseline e Demand Adjustment, mas a
                             * entidade ainda possui KFs transicionais Enterprise. Neutralizamos
                             * imediatamente antes do save para que arredondamentos ou fluxos futuros
                             * nao reintroduzam New Materials/Uplift por acidente.
                             */
                            demandPlansLinhaClusterLocations.forEach(this::neutralizaKeyFiguresEnterpriseDemandPlanItemCommunity);
                            List<DemandPlanItem> demandPlanItemsSalvas =
                                    demandPlanItemRepository.saveAll(demandPlansLinhaClusterLocations);
                            validaDemandPlanItemsSalvasCommunity(
                                    demandPlanItemsSalvas,
                                    demandPlansLinhaClusterLocations.size());
                        }

                        log.info("Plano gerado e salvo para Cluster Materiais " + clusterMateriaisDemandPlanning.getDescricao()
                                + " e Cluster Locations " + clusterLocations.getDescricao());
                    });
                }
            });
            demandPlanItemRepository.flush();
            historicoDemandPlanItemRepository.flush();

            executaPosProcessamentoDemandPlanGerado(
                    demandPlan,
                    clusterEParametrosProjection);

            if (demandPlanIdReferenciaCopiaDados != null) {
                demandPlanReferenceCopySpi.copyReferencePlan(
                        demandPlan,
                        demandPlanIdReferenciaCopiaDados,
                        copiaApenasNoHorizonteCongelado,
                        parametrosDemandPlanProjection,
                        clusterEParametrosProjection,
                        unidadeMedidaProjection);
            }

            // Sucessao de materiais, like modeling de novos materiais,
            // espelhamento de locations e custom key figures pertencem ao
            // OpsFactor Enterprise. No Community, a execucao termina no plano
            // padrao salvo acima, sem criar series auxiliares Enterprise.

            // Pricing, elasticidade e uplift baseado em preco pertencem ao OpsFactor Enterprise.
            // No Community, a geracao do plano de demanda termina sem criar PricePlan nem alterar
            // o forecast por efeito de preco.
            demandPlan = demandPlanRepository.customFindByIdComPerfilExecucao(demandPlan.getId()).get();

            log.info("Executando reset do plano restrito (restrito = irrestrito)");
            resetPlanoRestrito(demandPlan.getId());

        /*
         * Se a geracao falhar, removemos o plano recem-criado para nao deixar
         * um Demand Plan parcial visivel no Planning Book. A execucao Community
         * nao propaga checked exceptions nesta borda: engines, projections e
         * repositories falham como erros funcionais/runtime. StackOverflowError
         * continua coberto porque forecasts legados recursivos podem estourar
         * antes de o workflow conseguir devolver uma excecao de negocio.
         */
        } catch (RuntimeException | StackOverflowError e) {
            log.error("Erro ao gerar Demand Plan Community {}", demandPlan.getId(), e);
            try {
                demandPlanRepository.flush();
                Long generatedDemandPlanId = demandPlan.getId();
                transactionTemplate.executeWithoutResult(
                        transactionStatus -> deleteDemandPlan(generatedDemandPlanId));
                demandPlanRepository.flush();
            // garante que o erro original seja lançado, ao invés
            // de eventual erro na remoção do demand plan do banco de dados
            } catch (RuntimeException e2) {
                // Cleanup pos-falha usa apenas repositories/deletes runtime.
                // Preservamos sempre a excecao original da geracao do plano.
                log.error("Demand Plan Community {} nao pode ser removido apos erro de execucao", demandPlan.getId(), e2);
                throw e;
            }
            throw e;
        }

        return demandPlan.getId();

    }

    /**
     * Materializa a janela editavel do Planning Book no Demand Plan gerado.
     *
     * <p>O execution profile guarda offsets de periodo relativos ao inicio do
     * plano. A entidade `DemandPlan`, por sua vez, guarda datas absolutas para
     * que consultas do Planning Book nao precisem reprocessar a configuracao do
     * perfil. Se a configuracao estiver incompleta ou invertida, mantemos a
     * janela nula e o getter da entidade aplica seu default historico.</p>
     */
    private void aplicaJanelaEdicaoPlanningBookCommunity(
            DemandPlan demandPlan,
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan) {

        if (!perfilExecucaoDemandPlan.getRestringePeriodosEdicaoPlano()
                || perfilExecucaoDemandPlan.getPeriodoInicialEdicaoPlano() == null
                || perfilExecucaoDemandPlan.getPeriodoFinalEdicaoPlano() == null
                || perfilExecucaoDemandPlan.getPeriodoFinalEdicaoPlano() < perfilExecucaoDemandPlan.getPeriodoInicialEdicaoPlano()) {
            return;
        }

        demandPlan.setDataInicioEdicao(Calendario.getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                demandPlan.getDataInicioPlano(),
                perfilExecucaoDemandPlan.getPeriodoInicialEdicaoPlano() - 1,
                perfilExecucaoDemandPlan.getTamanhoBucket())
                .toLocalDate());
        demandPlan.setDataFimEdicao(Calendario.getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                        demandPlan.getDataInicioPlano(),
                        perfilExecucaoDemandPlan.getPeriodoFinalEdicaoPlano() - 1,
                        perfilExecucaoDemandPlan.getTamanhoBucket())
                .toLocalDate());

    }

    /**
     * Valida o cabecalho salvo de um Demand Plan novo antes de gerar linhas.
     *
     * <p>O `saveAndFlush` inicial materializa o id usado por todas as linhas de
     * Demand Planning. Como a geracao por cluster roda em paralelo, um snapshot
     * nulo ou incompleto precisa falhar aqui, ainda em trecho sequencial, antes
     * de chamar factories, workflow estatistico ou persistencia em lote com um
     * plano que nao pode ser rastreado.</p>
     */
    private DemandPlan validaDemandPlanSalvoInicialCommunity(
            DemandPlan demandPlanSalvo) {

        if (demandPlanSalvo == null) {
            throw new IllegalStateException(
                    "Saved Demand Plan snapshot is required before Community Demand Planning execution.");
        }

        if (demandPlanSalvo.getId() == null) {
            throw new IllegalStateException(
                    "Saved Demand Plan snapshot has no id before Community Demand Planning execution.");
        }

        if (demandPlanSalvo.getPerfilExecucaoDemandPlan() == null) {
            throw new IllegalStateException(
                    "Saved Demand Plan snapshot has no execution profile before Community Demand Planning execution.");
        }

        if (demandPlanSalvo.getHorarioGeracaoCadastrado() == null) {
            throw new IllegalStateException(
                    "Saved Demand Plan snapshot has no generation timestamp before Community Demand Planning execution.");
        }

        if (demandPlanSalvo.getDataInicioPlano() == null) {
            throw new IllegalStateException(
                    "Saved Demand Plan snapshot has no plan start date before Community Demand Planning execution.");
        }

        if (demandPlanSalvo.getDataFimPlano() == null) {
            throw new IllegalStateException(
                    "Saved Demand Plan snapshot has no plan end date before Community Demand Planning execution.");
        }

        if (demandPlanSalvo.getTamanhoBucketCadastrado() == null) {
            throw new IllegalStateException(
                    "Saved Demand Plan snapshot has no bucket size before Community Demand Planning execution.");
        }

        return demandPlanSalvo;

    }

    /**
     * Hook protegido para pos-processamentos Enterprise depois da geracao base.
     *
     * <p>O Community nao altera o plano apos salvar as linhas de baseline e
     * ajuste. O metodo existe apenas para overlays `@Primary`, como o
     * Enterprise, aplicarem capabilities privadas ja migradas sem duplicar todo
     * o fluxo de criacao do Demand Plan. O hook roda antes do reset do plano
     * restrito; por isso qualquer overlay que grave KFs privadas tambem deve
     * sobrescrever `resetPlanoRestrito(...)` quando essas KFs precisarem ser
     * copiadas para as colunas atendidas.</p>
     */
    protected void executaPosProcessamentoDemandPlanGerado(
            DemandPlan demandPlan,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        /*
         * No-op intencional: sucessao de materiais, New Materials, location
         * mirroring, custom KFs, pricing e uplift pertencem ao Enterprise.
         */

    }

    /**
     * Atualiza a fotografia privada de precos imediatamente apos persistir o
     * header do Demand Plan.
     *
     * <p>Pricing nao pertence ao Community; por isso o default e no-op. O
     * Enterprise sobrepoe o hook para substituir as sete familias privadas de
     * preco antes de construir projections ou iniciar o forecast.</p>
     *
     * @param demandPlan header ja salvo, com id obrigatorio.
     */
    protected void atualizaSnapshotPrecoDemandPlanAposSalvarHeader(
            DemandPlan demandPlan) {

        // No-op intencional: snapshots de preco sao capability Enterprise.

    }

    /**
     * Persiste o cabecalho de uma versao derivada sem executar novo forecast.
     *
     * <p>Derivacoes como Trend pertencem a overlays, mas precisam passar pelo
     * mesmo ponto de lifecycle usado pela geracao normal: o header ja possui
     * ID antes de o Enterprise criar seu snapshot privado. O caller continua
     * dono das linhas e do cleanup completo quando a derivacao falhar.</p>
     */
    protected DemandPlan saveDerivedDemandPlanHeader(DemandPlan demandPlan) {

        DemandPlan savedDemandPlan = validaDemandPlanSalvoInicialCommunity(
                demandPlanRepository.saveAndFlush(demandPlan));
        atualizaSnapshotPrecoDemandPlanAposSalvarHeader(savedDemandPlan);
        return savedDemandPlan;

    }

    @Transactional
    public void deleteDemandPlan(Long demandPlanId) {


        // desassocia demand plan dos respectivos supply plans
        List<SupplyPlan> supplyPlansAssociadosAoDemandPlan =
                getSupplyPlansAssociadosAoDemandPlanParaExclusaoCommunity(
                        supplyPlanRepository.findByDemandPlanId(demandPlanId));
        for (SupplyPlan supplyPlanAssociadoAoDemandPlan : supplyPlansAssociadosAoDemandPlan) {
            supplyPlanAssociadoAoDemandPlan.setDemandPlan(null);
        }
        List<SupplyPlan> supplyPlansAssociadosAoDemandPlanSalvos =
                supplyPlanRepository.saveAll(supplyPlansAssociadosAoDemandPlan);
        validaSupplyPlansAssociadosSalvosParaExclusaoCommunity(
                supplyPlansAssociadosAoDemandPlanSalvos,
                supplyPlansAssociadosAoDemandPlan.size());

        // remove dados históricos
        historicoDemandPlanItemRepository.deleteByKeyDemandPlanId(demandPlanId);

        // remove demand plan linhas
        demandPlanItemRepository.removeByKeyDemandPlanId(demandPlanId);
        // desassocia demand plan dos campos demandPlanCopiadoNoHorizonteCongelado de outros demand plans
        List<DemandPlan> demandPlansAssociadosAoDemandPlan =
                getDemandPlansAssociadosAoDemandPlanParaExclusaoCommunity(
                        demandPlanRepository.findByDemandPlanCopiadoNoHorizonteCongeladoId(demandPlanId));
        for (DemandPlan demandPlanAssociadoAoDemandPlan : demandPlansAssociadosAoDemandPlan) {
            demandPlanAssociadoAoDemandPlan.setDemandPlanCopiadoNoHorizonteCongelado(null);
        }
        List<DemandPlan> demandPlansAssociadosAoDemandPlanSalvos =
                demandPlanRepository.saveAll(demandPlansAssociadosAoDemandPlan);
        validaDemandPlansAssociadosSalvosParaExclusaoCommunity(
                demandPlansAssociadosAoDemandPlanSalvos,
                demandPlansAssociadosAoDemandPlan.size());

        // remove demand plan
        demandPlanRepository.deleteById(demandPlanId);
    }

    /**
     * Valida os Supply Plans associados antes da exclusao do Demand Plan.
     *
     * <p>Lista vazia e caso normal: o Demand Plan pode nao estar ligado a
     * nenhum Supply Plan. Lista nula ou item nulo indicam quebra de contrato do
     * repository/projection de exclusao e devem falhar antes de deletar linhas,
     * historico ou remover parcialmente a versao.</p>
     */
    private List<SupplyPlan> getSupplyPlansAssociadosAoDemandPlanParaExclusaoCommunity(
            List<SupplyPlan> supplyPlansAssociadosAoDemandPlan) {

        if (supplyPlansAssociadosAoDemandPlan == null) {
            throw new IllegalArgumentException(
                    "Associated Supply Plan collection is required for Community Demand Plan deletion.");
        }

        int indiceSupplyPlanAssociadoAoDemandPlan = 0;
        for (SupplyPlan supplyPlanAssociadoAoDemandPlan : supplyPlansAssociadosAoDemandPlan) {
            if (supplyPlanAssociadoAoDemandPlan == null) {
                throw new IllegalArgumentException(
                        "Associated Supply Plan at index "
                                + indiceSupplyPlanAssociadoAoDemandPlan
                                + " is required for Community Demand Plan deletion.");
            }
            indiceSupplyPlanAssociadoAoDemandPlan++;
        }

        return supplyPlansAssociadosAoDemandPlan;

    }

    /**
     * Valida outros Demand Plans que copiam a versao removida no horizonte
     * congelado.
     *
     * <p>Essa associacao e apenas uma referencia interna entre versoes
     * Community. Validamos a fotografia retornada pelo repository antes de
     * limpar a referencia e salvar em lote, mantendo a exclusao deterministica e
     * com erro funcional para snapshots quebrados.</p>
     */
    private List<DemandPlan> getDemandPlansAssociadosAoDemandPlanParaExclusaoCommunity(
            List<DemandPlan> demandPlansAssociadosAoDemandPlan) {

        if (demandPlansAssociadosAoDemandPlan == null) {
            throw new IllegalArgumentException(
                    "Associated Demand Plan collection is required for Community Demand Plan deletion.");
        }

        int indiceDemandPlanAssociadoAoDemandPlan = 0;
        for (DemandPlan demandPlanAssociadoAoDemandPlan : demandPlansAssociadosAoDemandPlan) {
            if (demandPlanAssociadoAoDemandPlan == null) {
                throw new IllegalArgumentException(
                        "Associated Demand Plan at index "
                                + indiceDemandPlanAssociadoAoDemandPlan
                                + " is required for Community Demand Plan deletion.");
            }
            indiceDemandPlanAssociadoAoDemandPlan++;
        }

        return demandPlansAssociadosAoDemandPlan;

    }

    /**
     * Valida o retorno salvo dos Supply Plans que foram desassociados do Demand
     * Plan removido.
     *
     * <p>A exclusao do Demand Plan continua depois deste save. Retorno nulo ou
     * item nulo indicaria que a desassociacao nao ficou auditavel e deve falhar
     * antes de apagar historico, linhas e a versao de demanda.</p>
     */
    private void validaSupplyPlansAssociadosSalvosParaExclusaoCommunity(
            List<SupplyPlan> supplyPlansAssociadosAoDemandPlanSalvos) {

        validaSupplyPlansAssociadosSalvosParaExclusaoCommunity(
                supplyPlansAssociadosAoDemandPlanSalvos,
                null);

    }

    /**
     * Valida o retorno salvo dos Supply Plans desassociados contra o lote
     * original da exclusao.
     *
     * <p>Quando a quantidade esperada e informada, a remocao do Demand Plan so
     * pode continuar se todas as associacoes carregadas tambem voltarem do
     * `saveAll(...)`. Retorno parcial deixaria Supply Plans apontando para um
     * Demand Plan prestes a ser apagado.</p>
     */
    private void validaSupplyPlansAssociadosSalvosParaExclusaoCommunity(
            List<SupplyPlan> supplyPlansAssociadosAoDemandPlanSalvos,
            Integer numeroSupplyPlansAssociadosAoDemandPlanEsperado) {

        if (supplyPlansAssociadosAoDemandPlanSalvos == null) {
            throw new IllegalArgumentException(
                    "Saved associated Supply Plan collection is required for Community Demand Plan deletion.");
        }

        if (numeroSupplyPlansAssociadosAoDemandPlanEsperado != null
                && supplyPlansAssociadosAoDemandPlanSalvos.size() != numeroSupplyPlansAssociadosAoDemandPlanEsperado) {
            throw new IllegalArgumentException(
                    "Saved associated Supply Plan collection size "
                            + supplyPlansAssociadosAoDemandPlanSalvos.size()
                            + " does not match expected size "
                            + numeroSupplyPlansAssociadosAoDemandPlanEsperado
                            + " for Community Demand Plan deletion.");
        }

        int indiceSupplyPlanAssociadoAoDemandPlan = 0;
        for (SupplyPlan supplyPlanAssociadoAoDemandPlan : supplyPlansAssociadosAoDemandPlanSalvos) {
            if (supplyPlanAssociadoAoDemandPlan == null) {
                throw new IllegalArgumentException(
                        "Saved associated Supply Plan at index "
                                + indiceSupplyPlanAssociadoAoDemandPlan
                                + " is required for Community Demand Plan deletion.");
            }
            indiceSupplyPlanAssociadoAoDemandPlan++;
        }

    }

    /**
     * Valida o retorno salvo dos Demand Plans que copiavam a versao removida no
     * horizonte congelado.
     *
     * <p>Esse save limpa uma referencia interna entre versoes Community. Se o
     * repository devolver snapshot quebrado, a remocao final do plano deve ser
     * interrompida para nao mascarar estado parcial.</p>
     */
    private void validaDemandPlansAssociadosSalvosParaExclusaoCommunity(
            List<DemandPlan> demandPlansAssociadosAoDemandPlanSalvos) {

        validaDemandPlansAssociadosSalvosParaExclusaoCommunity(
                demandPlansAssociadosAoDemandPlanSalvos,
                null);

    }

    /**
     * Valida o retorno salvo dos Demand Plans associados contra o lote original
     * da exclusao.
     *
     * <p>Este save remove referencias de horizonte congelado para a versao que
     * sera apagada. Se o repository devolver menos itens que recebeu, a exclusao
     * final deve falhar antes de deixar outras versoes apontando para um plano
     * removido.</p>
     */
    private void validaDemandPlansAssociadosSalvosParaExclusaoCommunity(
            List<DemandPlan> demandPlansAssociadosAoDemandPlanSalvos,
            Integer numeroDemandPlansAssociadosAoDemandPlanEsperado) {

        if (demandPlansAssociadosAoDemandPlanSalvos == null) {
            throw new IllegalArgumentException(
                    "Saved associated Demand Plan collection is required for Community Demand Plan deletion.");
        }

        if (numeroDemandPlansAssociadosAoDemandPlanEsperado != null
                && demandPlansAssociadosAoDemandPlanSalvos.size() != numeroDemandPlansAssociadosAoDemandPlanEsperado) {
            throw new IllegalArgumentException(
                    "Saved associated Demand Plan collection size "
                            + demandPlansAssociadosAoDemandPlanSalvos.size()
                            + " does not match expected size "
                            + numeroDemandPlansAssociadosAoDemandPlanEsperado
                            + " for Community Demand Plan deletion.");
        }

        int indiceDemandPlanAssociadoAoDemandPlan = 0;
        for (DemandPlan demandPlanAssociadoAoDemandPlan : demandPlansAssociadosAoDemandPlanSalvos) {
            if (demandPlanAssociadoAoDemandPlan == null) {
                throw new IllegalArgumentException(
                        "Saved associated Demand Plan at index "
                                + indiceDemandPlanAssociadoAoDemandPlan
                                + " is required for Community Demand Plan deletion.");
            }
            indiceDemandPlanAssociadoAoDemandPlan++;
        }

    }

    /**
     * Porta principal de execucao do forecast Community para um cluster/perfil.
     *
     * <p>Este metodo valida o perfil Community, carrega a projection de vendas
     * observadas, rejeita execution profiles Enterprise como arvore/auto-fit e
     * encaminha os insumos para as etapas seguintes. Mantemos a assinatura
     * completa porque ela deixa claro quais objetos cada fase consome e evita
     * um contexto opaco de execucao.</p>
     */
    public List<? extends DemandPlanForecastProjection> geraDemandPlanForecastProjectionsExecucaoComForecast(
            Calendario calendario,
            ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection,
            boolean preencheHorizonteForecastComDemandaHistorica) {

        ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection =
                parametrosDemandPlanNivelClusterProjection.getParametrosGeraisDemandPlanningProjection();

        if (!parametrosGeraisDemandPlanningProjection.executaPlanoDemanda) return new ArrayList<>();

        ClusterEParametrosProjection clusterEParametrosProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        ClusterLocations clusterLocations = parametrosDemandPlanNivelClusterProjection.getClusterLocations();
        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning = parametrosDemandPlanNivelClusterProjection.getClusterMateriaisDemandPlanning();
        boolean consideraDfusInativos = parametrosGeraisDemandPlanningProjection.isDpUsaHistoricoDemandaInativos();
        UnidadeMedida unidadeMedidaPadraoDp = parametrosGeraisDemandPlanningProjection.getUnidadeMedidaDP();


        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = parametrosDemandPlanNivelClusterProjection.getPerfilExecucaoDemandPlan();
        validaPerfilExecucaoDemandPlanDisponivel(perfilExecucaoDemandPlan, parametrosGlobais);

        ParametrosForecastProjection parametrosForecastProjection =
                resolveParametrosForecastProjectionExecucao(
                        parametrosDemandPlanNivelClusterProjection);
        /*
         * A validacao de modelo/split vem antes da extraction de sales para
         * bases legadas com configuracao Enterprise falharem barato, sem
         * consultar historico transacional que a execucao Community nao vai
         * utilizar.
         */
        validaParametrosForecastCommunity(parametrosForecastProjection);

        UnidadeMedidaProjection unidadeMedidaProjection =
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();

        SalesProjectionLocationMaterialData salesProjection = salesProjectionFactory.getSalesProjectionLocationMaterialData(
                perfilExecucaoDemandPlan.getTipoDocumentoVenda(parametrosGlobais),
                calendario,
                clusterEParametrosProjection.getLocationsDeClusterLocations(clusterLocations, !consideraDfusInativos),
                clusterEParametrosProjection.getMateriaisDeClusterMateriaisDemandPlanning(clusterMateriaisDemandPlanning, !consideraDfusInativos),
                unidadeMedidaProjection,
                clusterEParametrosProjection,
                unidadeMedidaPadraoDp);

        LocationProjection locationProjection = LocationProjectionFactory
                .getProjectionClusterLocations(clusterLocations, clusterEParametrosProjection, !consideraDfusInativos);
        MaterialProjection materialProjection = MaterialProjectionFactory
                .getProjectionClusterMateriais(clusterMateriaisDemandPlanning, clusterEParametrosProjection, !consideraDfusInativos);

        return geraDemandPlanForecastProjectionsExecucaoComForecast(
                calendario,
                parametrosDemandPlanNivelClusterProjection,
                materialProjection,
                locationProjection,
                salesProjection,
                clusterEParametrosProjection,
                preencheHorizonteForecastComDemandaHistorica);

    }

    /**
     * Executa o forecast Community usando projections ja materializadas.
     *
     * <p>Este overload existe para fluxos como simulacao, que precisam manter a
     * mesma `SalesProjectionLocationMaterialData` para montar o DTO de resposta e
     * para gerar o forecast. Assim evitamos extrair historico de vendas duas
     * vezes sem abrir mao das validacoes Community de perfil, modelo e split.</p>
     */
    public List<? extends DemandPlanForecastProjection> geraDemandPlanForecastProjectionsExecucaoComForecast(
            Calendario calendario,
            ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            SalesProjectionLocationMaterialData salesProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            boolean preencheHorizonteForecastComDemandaHistorica) {

        ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection =
                parametrosDemandPlanNivelClusterProjection.getParametrosGeraisDemandPlanningProjection();

        if (!parametrosGeraisDemandPlanningProjection.executaPlanoDemanda) return new ArrayList<>();


        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = parametrosDemandPlanNivelClusterProjection.getPerfilExecucaoDemandPlan();
        validaPerfilExecucaoDemandPlanDisponivel(perfilExecucaoDemandPlan, parametrosGlobais);

        ParametrosForecastProjection parametrosForecastProjection =
                resolveParametrosForecastProjectionExecucao(
                        parametrosDemandPlanNivelClusterProjection);
        validaParametrosForecastCommunity(parametrosForecastProjection);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucaoComForecast =
                geraDemandPlanForecastProjectionsExecucaoComParametrosResolvidos(
                        calendario,
                        parametrosDemandPlanNivelClusterProjection,
                        materialProjection,
                        locationProjection,
                        salesProjection,
                        parametrosGeraisDemandPlanningProjection,
                        parametrosForecastProjection,
                        clusterEParametrosProjection,
                        preencheHorizonteForecastComDemandaHistorica);

        /*
         * Uplift por evento e sua desagregacao sao Enterprise. No Community o
         * forecast produzido contem apenas baseline e ajustes via Planning Book.
         * A redistribuicao abaixo preserva totais entre materiais ativos e zera
         * series que nao devem vazar para persistencia/simulacao Community.
         */
        DemandPlanning.redistribuiForecastBaselineTrendSeasonalEntreMateriaisAtivosCommunity(
                demandPlanForecastProjectionsExecucaoComForecast,
                calendario,
                clusterEParametrosProjection,
                parametrosDemandPlanNivelClusterProjection.getParametrosGeraisDemandPlanningProjection());

        return demandPlanForecastProjectionsExecucaoComForecast;

    }

    /**
     * Executa a etapa estatística após a resolução do parâmetro do cluster.
     *
     * <p>O Community mantém um único parâmetro para toda a unidade e delega ao
     * overload existente. Um overlay Enterprise pode sobrescrever este hook
     * para particionar as folhas por vencedor privado, sem repetir validações,
     * extração de vendas ou o pós-processamento público do fluxo.</p>
     */
    protected List<? extends DemandPlanForecastProjection>
            geraDemandPlanForecastProjectionsExecucaoComParametrosResolvidos(
                    Calendario calendario,
                    ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection,
                    MaterialProjection materialProjection,
                    LocationProjection locationProjection,
                    SalesProjectionLocationMaterialData salesProjection,
                    ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
                    ParametrosForecastProjection parametrosForecastProjection,
                    ClusterEParametrosProjection clusterEParametrosProjection,
                    boolean preencheHorizonteForecastComDemandaHistorica) {

        return geraDemandPlanForecastProjectionsExecucaoComForecast(
                calendario,
                materialProjection,
                locationProjection,
                salesProjection,
                parametrosGeraisDemandPlanningProjection,
                parametrosForecastProjection,
                clusterEParametrosProjection,
                preencheHorizonteForecastComDemandaHistorica);

    }

    /**
     * Resolve os parametros estatisticos que valem para a unidade de forecast
     * cluster material/location ja materializada.
     *
     * <p>No Community, somente a projection simples e permitida e conserva a
     * configuracao manual do cluster. O hook existe para que uma edicao privada
     * entregue uma projection por cluster ja enriquecida por uma selecao em
     * lote, sem duplicar a leitura de vendas nem alterar a entidade de perfil
     * compartilhada.</p>
     *
     * @param parametrosDemandPlanNivelClusterProjection unidade efetiva de
     *                                                    configuracao do
     *                                                    cluster
     * @return parametros estatisticos a executar para toda a unidade
     */
    protected ParametrosForecastProjection resolveParametrosForecastProjectionExecucao(
            ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection) {

        if (parametrosDemandPlanNivelClusterProjection
                instanceof ParametrosDemandPlanNivelClusterProjectionSimples) {
            return ((ParametrosDemandPlanNivelClusterProjectionSimples)
                    parametrosDemandPlanNivelClusterProjection)
                    .getParametrosForecastProjection();
        }

        throw new RequiresEnterpriseVersionException(
                "Demand Planning Auto-fit / Regression Tree Execution Profile");

    }

    /**
     * Cria as series material/location de forecast a partir da projection de
     * vendas observadas e encaminha a lista para o workflow estatistico.
     *
     * <p>No Community, `salesProjection` contem apenas sell-out. Sell-in,
     * pedidos e eventos/campanhas nao entram neste objeto; quando o Enterprise
     * sobrepor a factory de vendas, o contrato desta etapa deve continuar
     * recebendo uma projection neutra de vendas, ja validada pela borda
     * Enterprise.</p>
     */
    public List<? extends DemandPlanForecastProjection> geraDemandPlanForecastProjectionsExecucaoComForecast(
            Calendario calendario,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            SalesProjectionLocationMaterialData salesProjection,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
            ParametrosForecastProjection parametrosForecastProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            boolean preencheHorizonteForecastComDemandaHistorica) {

        if (parametrosGeraisDemandPlanningProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning general parameters are required");
        }

        validaInputsProjectionAwareForecastCommunity(
                calendario,
                materialProjection,
                locationProjection,
                salesProjection,
                parametrosGeraisDemandPlanningProjection);

        UnidadeMedida unidadeMedidaPadraoDp = parametrosGeraisDemandPlanningProjection.getUnidadeMedidaDP();
        boolean consideraDfusInativos = parametrosGeraisDemandPlanningProjection.isDpUsaHistoricoDemandaInativos();

        preparaHistoricoForecastDemandPlanning(
                salesProjection,
                parametrosGeraisDemandPlanningProjection,
                parametrosForecastProjection,
                clusterEParametrosProjection);

        List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationList = DemandPlanning.geraDemandPlanForecastProjectionMaterialLocationListComDemandaHistoricaPopuladaCommunity(
                calendario,
                locationProjection,
                materialProjection,
                unidadeMedidaPadraoDp,
                consideraDfusInativos,
                salesProjection,
                clusterEParametrosProjection,
                preencheHorizonteForecastComDemandaHistorica);

        demandPlanForecastProjectionMaterialLocationList = preparaFolhasAntesDoWorkflowForecast(
                demandPlanForecastProjectionMaterialLocationList,
                calendario,
                materialProjection,
                locationProjection,
                parametrosGeraisDemandPlanningProjection,
                parametrosForecastProjection,
                clusterEParametrosProjection,
                preencheHorizonteForecastComDemandaHistorica);

        return geraDemandPlanForecastProjectionsExecucaoComForecast(
                demandPlanForecastProjectionMaterialLocationList,
                calendario,
                materialProjection,
                locationProjection,
                parametrosGeraisDemandPlanningProjection,
                parametrosForecastProjection,
                clusterEParametrosProjection,
                preencheHorizonteForecastComDemandaHistorica);

    }

    /**
     * Porta deliberada entre a factory Community de folhas e o workflow de
     * forecast. O Community nao interpreta fontes privadas de baseline; uma
     * edicao Enterprise pode sobrescrever a porta para materializar as folhas
     * antes de qualquer agregacao ou engine estatistica.
     */
    protected List<DemandPlanForecastProjectionMaterialLocation>
            preparaFolhasAntesDoWorkflowForecast(
                    List<DemandPlanForecastProjectionMaterialLocation> forecastLeaves,
                    Calendario calendario,
                    MaterialProjection materialProjection,
                    LocationProjection locationProjection,
                    ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
                    ParametrosForecastProjection parametrosForecastProjection,
                    ClusterEParametrosProjection clusterEParametrosProjection,
                    boolean preencheHorizonteForecastComDemandaHistorica) {

        if (Constantes.DPModeloEstatistico.BUDGET_DECOMPOSITION.equals(
                parametrosForecastProjection.getDpModeloEstatistico())) {
            throw new RequiresEnterpriseVersionException("Budget as Forecast");
        }

        return forecastLeaves;

    }

    /**
     * Valida os snapshots obrigatorios quando o caller entrega projections ja
     * materializadas para o forecast.
     *
     * <p>Este overload e usado por execucao e simulacao para reaproveitar uma
     * `SalesProjectionLocationMaterialData` ja carregada. Por isso a falha
     * precisa acontecer nesta borda, antes de chamar rotinas estaticas que
     * acessam mapas internos de sales/material/location e acabariam gerando
     * `NullPointerException` sem contexto funcional.</p>
     */
    private void validaInputsProjectionAwareForecastCommunity(
            Calendario calendario,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            SalesProjectionLocationMaterialData salesProjection,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection) {

        if (calendario == null) {
            throw new IllegalArgumentException(
                    "Demand Planning calendar is required for Demand Planning forecast projection generation.");
        }
        if (materialProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning material projection is required for Demand Planning forecast projection generation.");
        }
        if (locationProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning location projection is required for Demand Planning forecast projection generation.");
        }
        if (salesProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning sales projection is required for Demand Planning forecast projection generation.");
        }
        if (parametrosGeraisDemandPlanningProjection.getParametrosAgregacaoForecast() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning forecast aggregation parameters are required for Demand Planning forecast projection generation.");
        }
        if (parametrosGeraisDemandPlanningProjection.getUnidadeMedidaDP() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning default UOM is required for Demand Planning forecast projection generation.");
        }

    }

    /**
     * Hook de preparo da projection historica antes da criacao das series de forecast.
     *
     * <p>No Community esta etapa e propositalmente vazia: a venda observada
     * usada pelo forecast e exatamente a projection sell-out carregada pela
     * factory aberta. O Enterprise pode sobrescrever este ponto para aplicar
     * enriquecimentos privados que precisam acontecer antes da agregacao das
     * series material/location, sem colocar essas dependencias dentro do
     * Community nem contaminar a factory compartilhada usada por outros dominios.</p>
     *
     * @param salesProjection projection de vendas historicas ja carregada para
     *                        o perfil/cluster em execucao
     * @param parametrosGeraisDemandPlanningProjection parametros gerais ja
     *                                                 normalizados para a edicao
     * @param parametrosForecastProjection parametros do modelo/split da rodada
     * @param clusterEParametrosProjection fotografia de parametros e master data
     *                                     usada pela execucao do forecast
     */
    protected void preparaHistoricoForecastDemandPlanning(
            SalesProjectionLocationMaterialData salesProjection,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
            ParametrosForecastProjection parametrosForecastProjection,
            ClusterEParametrosProjection clusterEParametrosProjection) {

    }

    /**
     * Monta as unidades de execucao e delega forecast/desagregacao ao workflow Spring.
     *
     * <p>A sequencia e propositalmente explicita: validar parametros, agregar
     * conforme `DPNivelAgregacao` material/location e entregar cada unidade ao
     * `DemandForecastWorkflowService`. Quando material e location estao em
     * `BOTTOM_UP`, a propria serie material/location e a unidade de execucao e
     * o workflow nao executa desagregacao.</p>
     *
     * <p>Processors, engines e desagregacoes devem permanecer stateless, sem
     * guardar estado entre clusters, preservando a execucao paralela atual.</p>
     */
    public List<? extends DemandPlanForecastProjection> geraDemandPlanForecastProjectionsExecucaoComForecast(
            List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationList,
            Calendario calendario,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
            ParametrosForecastProjection parametrosForecastProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            boolean preencheHorizonteForecastComDemandaHistorica) {

        validaParametrosForecastCommunity(parametrosForecastProjection);
        validaInputsListaForecastCommunity(
                demandPlanForecastProjectionMaterialLocationList,
                calendario,
                parametrosGeraisDemandPlanningProjection);

        DemandForecastStockoutContext demandForecastStockoutContext =
                criaDemandForecastStockoutContext(
                        calendario,
                        parametrosGeraisDemandPlanningProjection,
                        parametrosForecastProjection,
                        clusterEParametrosProjection,
                        demandPlanForecastProjectionMaterialLocationList);

        return geraDemandPlanForecastProjectionsExecucaoComForecastEContextoStockout(
                demandPlanForecastProjectionMaterialLocationList,
                calendario,
                materialProjection,
                locationProjection,
                parametrosGeraisDemandPlanningProjection,
                parametrosForecastProjection,
                clusterEParametrosProjection,
                preencheHorizonteForecastComDemandaHistorica,
                demandForecastStockoutContext);

    }

    /**
     * Executa a montagem e o workflow de forecast para um conjunto de folhas
     * que compartilha os mesmos parâmetros estatísticos e contexto de stockout.
     *
     * <p>O caminho público Community constrói o contexto uma única vez e chega
     * aqui imediatamente. O hook protegido permite que um overlay particione as
     * folhas por parâmetros privados sem recriar a fotografia de estoque para
     * cada grupo. Community não o sobrescreve e, portanto, conserva exatamente
     * o comportamento anterior.</p>
     */
    protected List<? extends DemandPlanForecastProjection>
            geraDemandPlanForecastProjectionsExecucaoComForecastEContextoStockout(
                    List<DemandPlanForecastProjectionMaterialLocation>
                            demandPlanForecastProjectionMaterialLocationList,
                    Calendario calendario,
                    MaterialProjection materialProjection,
                    LocationProjection locationProjection,
                    ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
                    ParametrosForecastProjection parametrosForecastProjection,
                    ClusterEParametrosProjection clusterEParametrosProjection,
                    boolean preencheHorizonteForecastComDemandaHistorica,
                    DemandForecastStockoutContext demandForecastStockoutContext) {

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanning.geraDemandPlanForecastProjectionsExecucaoComDemandaHistoricaPopuladaCommunity(
                        demandPlanForecastProjectionMaterialLocationList,
                        calendario,
                        materialProjection,
                        locationProjection,
                        parametrosGeraisDemandPlanningProjection,
                        preencheHorizonteForecastComDemandaHistorica);

        for (DemandPlanForecastProjection demandPlanForecastProjection : demandPlanForecastProjectionsExecucao) {

            /*
             * Community executa apenas estatisticos basicos e split por
             * historico de vendas. Budget Decomposition, HTS, Forecast
             * Proportion, Chronos e uplift por eventos sao bloqueados na
             * validacao acima e devem ser reintroduzidos pelo Enterprise.
             */
            demandForecastWorkflowService.executaForecastEDesagregacao(
                    calendario,
                    parametrosForecastProjection,
                    parametrosGeraisDemandPlanningProjection.getParametrosAgregacaoForecast(),
                    demandPlanForecastProjection,
                    clusterEParametrosProjection,
                    demandForecastStockoutContext);

        }

        return demandPlanForecastProjectionsExecucao;

    }

    /**
     * Cria a fotografia opcional de estoque usada pelo tratamento de stockout.
     *
     * <p>O Community devolve {@code null}: sua etapa de stockout permanece
     * neutra e nao deve carregar estoque apenas para copiar vendas. O Enterprise
     * sobrescreve este hook e materializa uma unica projection para todas as
     * folhas da rodada, evitando N+1 dentro do workflow paralelo.</p>
     */
    protected DemandForecastStockoutContext criaDemandForecastStockoutContext(
            Calendario calendario,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
            ParametrosForecastProjection parametrosForecastProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationList) {

        return null;

    }

    /**
     * Valida a lista de series material/location antes da agregacao de forecast.
     *
     * <p>Lista vazia e snapshot valido: significa que o cluster nao possui DFUs
     * a executar. Lista nula, item nulo, calendario ausente ou parametros de
     * agregacao ausentes indicam quebra da factory/caller e devem falhar antes
     * da rotina de agregacao, mantendo claro que a unidade de execucao ainda
     * nem foi montada.</p>
     */
    private void validaInputsListaForecastCommunity(
            List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationList,
            Calendario calendario,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection) {

        if (parametrosGeraisDemandPlanningProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning general parameters are required");
        }
        if (parametrosGeraisDemandPlanningProjection.getParametrosAgregacaoForecast() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning forecast aggregation parameters are required for Demand Planning forecast projection generation.");
        }
        if (calendario == null) {
            throw new IllegalArgumentException(
                    "Demand Planning calendar is required for Demand Planning forecast projection generation.");
        }
        if (demandPlanForecastProjectionMaterialLocationList == null) {
            throw new IllegalArgumentException(
                    "Demand Planning material/location forecast projection collection is required for forecast projection generation.");
        }

        int indiceDemandPlanForecastProjectionMaterialLocation = 0;
        for (DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation : demandPlanForecastProjectionMaterialLocationList) {
            if (demandPlanForecastProjectionMaterialLocation == null) {
                throw new IllegalArgumentException(
                        "Demand Planning material/location forecast projection at index "
                                + indiceDemandPlanForecastProjectionMaterialLocation
                                + " is required for forecast projection generation.");
            }
            indiceDemandPlanForecastProjectionMaterialLocation++;
        }

    }

    @Transactional
    public void resetPlanoRestrito(Long demandPlanId) {


        /*
         * Community só possui Baseline e Demand Adjustment. O reset do plano
         * restrito tambem funciona como barreira defensiva contra dados legados
         * de Uplift/New Materials eventualmente presentes na tabela
         * compartilhada.
         */
        jdbcTemplate.update("update demand_plan_item SET quantidade_baseline_atendida = quantidade_baseline, "
                + "quantidade_itens_novos = 0,"
                + "quantidade_uplift = 0,"
                + "quantidade_itens_novos_atendida = 0,"
                + "quantidade_uplift_atendida = 0,"
                + "quantidade_ajuste_demanda_atendida = quantidade_ajuste_demanda "
                + "where demand_plan_id = ?",  demandPlanId);

    }

    /**
     * Salva linhas de Demand Plan Community para todos os periodos.
     *
     * <p>O Community nao gera nem propaga New Materials/Uplift. A entidade ainda
     * contem esses campos por transicao de modelo, entao a borda de persistencia
     * zera explicitamente as KFs Enterprise antes de decidir se a linha deve ser
     * salva.</p>
     *
     * @param demandPlanningProjection projection de Demand Planning ja populada
     */
    public void saveDemandPlanDePlanningProjection(DemandPlanningProjection demandPlanningProjection) {

        validaDemandPlanningProjectionParaPersistenciaCommunity(demandPlanningProjection);

        Collection<DemandPlanItem> demandPlanItemsProjection =
                getDemandPlanItemsProjectionParaPersistenciaCommunity(
                        demandPlanningProjection.getTodosDemandPlanItems());
        List<DemandPlanItem> demandPlanItems = demandPlanItemsProjection.stream()
                .map(this::neutralizaKeyFiguresEnterpriseDemandPlanItemCommunity)
                .filter(x -> Math.abs(x.getQuantidadeBaselineAtendida()) > 0.000001 ||
                        Math.abs(x.getQuantidadeAjusteDemandaAtendida()) > 0.000001 ||
                        Math.abs(x.getQuantidadeBaseline()) > 0.00001 ||
                        Math.abs(x.getQuantidadeAjusteDemanda()) > 0.00001)
                .toList();
        validaDemandPlanItemsParaPersistenciaCommunity(demandPlanItems);
        List<DemandPlanItem> demandPlanItemsSalvas =
                demandPlanItemRepository.saveAll(demandPlanItems);
        validaDemandPlanItemsSalvasCommunity(
                demandPlanItemsSalvas,
                demandPlanItems.size());
    }

    /**
     * Salva linhas de Demand Plan Community para um unico periodo.
     *
     * <p>Aplica a mesma neutralizacao defensiva das KFs Enterprise usada no
     * salvamento completo, mantendo a persistencia Community restrita a Baseline
     * e Demand Adjustment.</p>
     *
     * @param demandPlanningProjection projection de Demand Planning ja populada
     * @param posicaoPeriodo periodo de calendario a ser persistido
     */
    public void saveDemandPlanDePlanningProjection(DemandPlanningProjection demandPlanningProjection,
            int posicaoPeriodo) {

        validaDemandPlanningProjectionParaPersistenciaCommunity(demandPlanningProjection);

        Collection<DemandPlanItem> demandPlanItemsProjection =
                getDemandPlanItemsProjectionParaPersistenciaCommunity(
                        demandPlanningProjection.getDemandPlanItems(posicaoPeriodo));
        List<DemandPlanItem> demandPlanItems = demandPlanItemsProjection.stream()
                .map(this::neutralizaKeyFiguresEnterpriseDemandPlanItemCommunity)
                .filter(x -> Math.abs(x.getQuantidadeBaselineAtendida()) > 0.00001 ||
                        Math.abs(x.getQuantidadeAjusteDemandaAtendida()) > 0.00001 ||
                        Math.abs(x.getQuantidadeBaseline()) > 0.00001 ||
                        Math.abs(x.getQuantidadeAjusteDemanda()) > 0.00001)
                .toList();
        validaDemandPlanItemsParaPersistenciaCommunity(demandPlanItems);
        List<DemandPlanItem> demandPlanItemsSalvas =
                demandPlanItemRepository.saveAll(demandPlanItems);
        validaDemandPlanItemsSalvasCommunity(
                demandPlanItemsSalvas,
                demandPlanItems.size());
    }

    /**
     * Valida o conjunto de linhas entregue pela projection antes do stream de
     * persistencia.
     *
     * <p>Uma lista vazia e um snapshot valido sem linhas a salvar. Ja colecao
     * nula ou item nulo indicam projection quebrada ou uso incorreto do service:
     * nesses casos falhamos antes de neutralizar KFs Enterprise ou chamar o
     * repository.</p>
     */
    private Collection<DemandPlanItem> getDemandPlanItemsProjectionParaPersistenciaCommunity(
            Collection<DemandPlanItem> demandPlanItemsProjection) {

        if (demandPlanItemsProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Plan line collection is required to save Demand Plan lines.");
        }

        int indiceDemandPlanItem = 0;
        for (DemandPlanItem demandPlanItem : demandPlanItemsProjection) {
            if (demandPlanItem == null) {
                throw new IllegalArgumentException(
                        "Demand Plan line at index "
                                + indiceDemandPlanItem
                                + " is required to save Demand Plan lines.");
            }
            indiceDemandPlanItem++;
        }

        return demandPlanItemsProjection;

    }

    /**
     * Valida linhas filtradas antes do `saveAll` de Demand Plan Community.
     *
     * <p>A projection oficial ja indexa linhas por periodo/location/material,
     * mas esta borda recebe uma colecao materializada e deve preservar a
     * cardinalidade ate uma validacao explicita. Assim uma projection quebrada,
     * mock transicional ou subtipo de teste nao consegue esconder duplicidades
     * por `Set` antes da persistencia.</p>
     */
    private void validaDemandPlanItemsParaPersistenciaCommunity(
            Collection<DemandPlanItem> demandPlanItemsParaPersistencia) {

        Set<String> chavesDemandPlanItem = new HashSet<>();
        int indiceDemandPlanItem = 0;
        for (DemandPlanItem demandPlanItem : demandPlanItemsParaPersistencia) {
            if (hasDemandPlanItemKeyIncompletaCommunity(demandPlanItem)) {
                throw new IllegalArgumentException(
                        "Demand Plan line at index "
                                + indiceDemandPlanItem
                                + " has an incomplete Community Demand Planning key for persistence.");
            }
            String chaveDemandPlanItem =
                    getMensagemChaveDemandPlanItemCommunity(demandPlanItem);
            if (!chavesDemandPlanItem.add(chaveDemandPlanItem)) {
                throw new IllegalArgumentException(
                        "Demand Plan line at index "
                                + indiceDemandPlanItem
                                + " has duplicated Community Demand Planning key for persistence: "
                                + chaveDemandPlanItem
                                + ".");
            }
            indiceDemandPlanItem++;
        }

    }

    private boolean hasDemandPlanItemKeyIncompletaCommunity(
            DemandPlanItem demandPlanItem) {

        return demandPlanItem.getKey() == null ||
                demandPlanItem.getDemandPlan() == null ||
                demandPlanItem.getLocation() == null ||
                demandPlanItem.getProduto() == null ||
                demandPlanItem.getDataReferencia() == null;

    }

    private String getMensagemChaveDemandPlanItemCommunity(
            DemandPlanItem demandPlanItem) {

        return "demandPlanId="
                + demandPlanItem.getDemandPlan().getId()
                + ", location="
                + demandPlanItem.getLocation().getId()
                + ", material="
                + demandPlanItem.getProduto().getId()
                + ", referenceDate="
                + demandPlanItem.getDataReferencia();

    }

    /**
     * Valida linhas geradas pela rotina estatistica antes do save inicial.
     *
     * <p>A geracao Community pode produzir zero linhas para um cluster sem DFUs
     * ativas, e isso deve continuar sendo apenas no-op. Ja uma lista nula ou
     * item nulo indica quebra da materializacao estatistica/DFU split; nesse
     * caso falhamos antes de chamar `size`, neutralizar KFs Enterprise ou salvar
     * um lote parcial.</p>
     */
    private List<DemandPlanItem> getDemandPlanItemsGeradasParaPersistenciaCommunity(
            List<DemandPlanItem> demandPlanItemsGeradas) {

        if (demandPlanItemsGeradas == null) {
            throw new IllegalArgumentException(
                    "Generated Demand Plan line collection is required for Community Demand Planning persistence.");
        }

        int indiceDemandPlanItemGerada = 0;
        for (DemandPlanItem demandPlanItemGerada : demandPlanItemsGeradas) {
            if (demandPlanItemGerada == null) {
                throw new IllegalArgumentException(
                        "Generated Demand Plan line at index "
                                + indiceDemandPlanItemGerada
                                + " is required for Community Demand Planning persistence.");
            }
            indiceDemandPlanItemGerada++;
        }

        return demandPlanItemsGeradas;

    }

    /**
     * Valida o retorno salvo das linhas de Demand Plan Community.
     *
     * <p>Esta rotina e usada tanto pela geracao estatistica quanto pelos saves
     * de projection. Como as linhas sao chaves material/location/periodo, o
     * retorno salvo precisa preservar a chave completa para que o snapshot
     * gerado possa ser auditado e reprocessado por Planning Book/Supply.</p>
     */
    private void validaDemandPlanItemsSalvasCommunity(
            Collection<DemandPlanItem> demandPlanItemsSalvas) {

        validaDemandPlanItemsSalvasCommunity(
                demandPlanItemsSalvas,
                null);

    }

    /**
     * Valida o retorno salvo das linhas de Demand Plan Community contra o lote
     * enviado ao repository.
     *
     * <p>Quando `numeroDemandPlanItemsEsperado` e informado, o repository
     * precisa devolver a mesma quantidade de linhas que recebeu. Isso impede que
     * uma persistencia parcial siga adiante como se o lote inteiro tivesse sido
     * materializado. O overload sem quantidade fica preservado para validacoes
     * estruturais/reflexivas que nao tem acesso ao lote original.</p>
     */
    private void validaDemandPlanItemsSalvasCommunity(
            Collection<DemandPlanItem> demandPlanItemsSalvas,
            Integer numeroDemandPlanItemsEsperado) {

        if (demandPlanItemsSalvas == null) {
            throw new IllegalArgumentException(
                    "Saved Demand Plan line collection is required for Community Demand Planning persistence.");
        }

        if (numeroDemandPlanItemsEsperado != null
                && demandPlanItemsSalvas.size() != numeroDemandPlanItemsEsperado) {
            throw new IllegalArgumentException(
                    "Saved Demand Plan line collection size "
                            + demandPlanItemsSalvas.size()
                            + " does not match expected size "
                            + numeroDemandPlanItemsEsperado
                            + " for Community Demand Planning persistence.");
        }

        int indiceDemandPlanItemSalva = 0;
        for (DemandPlanItem demandPlanItemSalva : demandPlanItemsSalvas) {
            if (demandPlanItemSalva == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Plan line at index "
                                + indiceDemandPlanItemSalva
                                + " is required for Community Demand Planning persistence.");
            }
            if (demandPlanItemSalva.getKey() == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Plan line at index "
                                + indiceDemandPlanItemSalva
                                + " must have a primary key.");
            }
            if (demandPlanItemSalva.getDemandPlan() == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Plan line at index "
                                + indiceDemandPlanItemSalva
                                + " must have a Demand Plan.");
            }
            if (demandPlanItemSalva.getLocation() == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Plan line at index "
                                + indiceDemandPlanItemSalva
                                + " must have a location.");
            }
            if (demandPlanItemSalva.getProduto() == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Plan line at index "
                                + indiceDemandPlanItemSalva
                                + " must have a material.");
            }
            if (demandPlanItemSalva.getDataReferencia() == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Plan line at index "
                                + indiceDemandPlanItemSalva
                                + " must have a reference date.");
            }
            indiceDemandPlanItemSalva++;
        }

    }

    /**
     * Valida a projection de Demand Planning antes de qualquer persistencia.
     *
     * <p>A projection e o snapshot em memoria que concentra linhas por
     * material/location/periodo ja calculadas ou editadas. Salvar sem ela nao
     * tem semantica Community: seria uma chamada invalida ao service, nao uma
     * execucao vazia. A falha explicita evita `NullPointerException` durante
     * leitura das linhas e garante que nenhum `saveAll` seja acionado para um
     * payload incompleto.</p>
     */
    private void validaDemandPlanningProjectionParaPersistenciaCommunity(
            DemandPlanningProjection demandPlanningProjection) {

        if (demandPlanningProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning projection is required to save Demand Plan lines.");
        }

    }

    /**
     * Neutraliza KFs de Demand Planning que existem apenas no Enterprise.
     *
     * <p>Este metodo fica junto da persistencia para evitar que qualquer fluxo
     * transicional ou dado carregado de referencia consiga gravar New Materials ou
     * Uplift no Community. O retorno do proprio objeto permite uso direto em
     * streams sem esconder que ha mutacao defensiva da entidade.</p>
     */
    protected DemandPlanItem neutralizaKeyFiguresEnterpriseDemandPlanItemCommunity(DemandPlanItem demandPlanItem) {

        demandPlanItem.setQuantidadeItensNovos(0.0);
        demandPlanItem.setQuantidadeItensNovosAtendida(0.0);
        demandPlanItem.setQuantidadeUplift(0.0);
        demandPlanItem.setQuantidadeUpliftAtendida(0.0);
        return demandPlanItem;

    }

    /**
     * Carrega um Demand Plan por id para fachadas que precisam do perfil de
     * execucao junto da entidade.
     *
     * <p>Este metodo ainda e publico porque a fachada de Planning Book reaproveita
     * a regra central de carregamento. Id ausente ou plano inexistente sao erros
     * funcionais da requisicao e devem falhar antes de qualquer projection,
     * mapper ou ajuste de linhas.</p>
     */
    public DemandPlan getDemandPlanDeId(Long id) {


        Optional<DemandPlan> demandPlanOptional =
                demandPlanRepository.customFindByIdComPerfilExecucao(id);
        if (demandPlanOptional == null) {
            throw new IllegalStateException(
                    "Demand Plan repository returned null Optional while loading Demand Plan id "
                            + id
                            + ".");
        }

        return demandPlanOptional.orElseThrow(() -> new NoResultException("Demand Plan " + id + " not found"));

    }

    /**
     * Valida ids numericos obrigatorios antes de acessar repositories.
     *
     * <p>Demand Planning possui rotas chamadas por task, controller e cleanup
     * defensivo de erro. Id ausente e problema de contrato da chamada, nao uma
     * ausencia de registro no banco, portanto falhamos antes de qualquer query
     * ou operacao de delecao em lote.</p>
     */
    /**
     * Valida ids textuais obrigatorios da configuracao Demand Planning.
     *
     * <p>O perfil de execucao define horizonte, bucket, documento historico e
     * parametros de forecast. Blank deve ser tratado como ausencia do parametro
     * antes de carregar perfil, vendas ou projections de forecast.</p>
     */

}
