package com.opsfactor.community.capability.demandplanning.configuration.facade.mapper;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningClusterLevelConfigurationDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningForecastParametersDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningGeneralParametersDTO;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.factory.ParametrosDemandPlanningProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.configuration.repository.PerfilExecucaoDemandPlanRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.demandplanning.forecast.configuration.DemandPlanningModelCatalog;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Set;

/**
 * Mapper Community da configuracao cluster-level de Demand Planning.
 *
 * <p>Esta classe e a borda de escrita/leitura usada pelo front de configuracao
 * para transformar DTOs em entidades/projections e entidades em DTOs. O DTO
 * compartilhado ainda contem campos transicionais Enterprise para tolerar
 * payloads legados, mas esta implementacao publica apenas o contrato Community:
 * configuracao simples por cluster material/location, modelos estatisticos
 * abertos, split Historical Sales e defaults neutros para recursos privados.</p>
 *
 * <p>Qualquer abertura futura de auto-fit, support series, MAPE, foundation
 * models, stockout/outlier reais ou uplift deve acontecer em overlay Enterprise
 * `@Primary`, junto com runtime/factories/testes reais. O Community deve
 * continuar falhando cedo antes de acessar repositories quando receber payload
 * Enterprise.</p>
 */
@Service
public class DemandPlanningConfigurationMapper {

    /**
     * Projection de parametros e clusters usada para montar DTOs de leitura e
     * resolver clusters na criacao de entidades transicionais.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Factory que materializa a projection simples Community depois que o DTO
     * ja foi validado contra recursos Enterprise.
     */
    @Autowired
    private ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory;

    /**
     * Repository usado somente quando o DTO informa UOM explicita. UOM nula
     * significa herdar a unidade global e nao deve disparar lookup.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Repository do perfil de execucao Demand Planning. Chamadas publicas deste
     * mapper validam o payload Community antes de consultar o perfil.
     */
    @Autowired
    private PerfilExecucaoDemandPlanRepository perfilExecucaoDemandPlanRepository;

    public DemandPlanningClusterLevelConfigurationDTO getDemandPlanningConfigurationDtoFromEntities(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster) {

        validaParametrosDemandPlanNivelClusterParaLeitura(parametrosDemandPlanNivelCluster);

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO = new DemandPlanningClusterLevelConfigurationDTO();

        demandPlanningClusterLevelConfigurationDTO.demandPlanExecutionProfileId = parametrosDemandPlanNivelCluster.getPerfilExecucaoDemandPlan().getId();
        demandPlanningClusterLevelConfigurationDTO.locationClusterId = parametrosDemandPlanNivelCluster.getClusterLocations().getId();
        demandPlanningClusterLevelConfigurationDTO.materialClusterId = parametrosDemandPlanNivelCluster.getClusterMateriaisDemandPlanning().getId();

        // seta parametros gerais (somente nivel cluster loc / cluster mat)
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters = getDemandPlanningGeneralParametersDTOFromEntities(
                parametrosDemandPlanNivelCluster);
        // seta parametros de forecast configurados para o nivel cluster
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters = getDemandPlanningForecastParametersDTOFromEntities(
                parametrosDemandPlanNivelCluster);

        return demandPlanningClusterLevelConfigurationDTO;

    }

    /**
     * Valida a entidade lida do banco antes de montar o DTO de configuracao.
     *
     * <p>O metodo de leitura usa o embedded-id para expor perfil, cluster de
     * location e cluster de material ao front. Quando a entidade esta
     * incompleta, falhar aqui produz erro funcional de snapshot/cadastro em vez
     * de `NullPointerException` ao encadear getters dentro do mapper.</p>
     */
    private void validaParametrosDemandPlanNivelClusterParaLeitura(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster) {

        if (parametrosDemandPlanNivelCluster == null) {
            throw new IllegalArgumentException(
                    "Demand Planning cluster-level parameters are required");
        }
        if (parametrosDemandPlanNivelCluster.getParametrosDemandPlanNivelClusterCompositeKey() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning cluster-level parameter key is required");
        }
        if (parametrosDemandPlanNivelCluster.getPerfilExecucaoDemandPlan() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning execution profile is required for cluster-level configuration");
        }
        if (parametrosDemandPlanNivelCluster.getPerfilExecucaoDemandPlan().getId() == null
                || parametrosDemandPlanNivelCluster.getPerfilExecucaoDemandPlan().getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Demand Planning execution profile id is required for cluster-level configuration");
        }
        if (parametrosDemandPlanNivelCluster.getClusterLocations() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning location cluster is required for cluster-level configuration");
        }
        if (parametrosDemandPlanNivelCluster.getClusterLocations().getId() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning location cluster id is required for cluster-level configuration");
        }
        if (parametrosDemandPlanNivelCluster.getClusterMateriaisDemandPlanning() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning material cluster is required for cluster-level configuration");
        }
        if (parametrosDemandPlanNivelCluster.getClusterMateriaisDemandPlanning().getId() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning material cluster id is required for cluster-level configuration");
        }

    }

    private DemandPlanningGeneralParametersDTO getDemandPlanningGeneralParametersDTOFromEntities(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster) {

        ClusterEParametrosProjection clusterEParametrosProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        ParametrosGlobais parametrosGlobais =
                clusterEParametrosProjection.getParametrosGlobais();

        DemandPlanningGeneralParametersDTO demandPlanningGeneralParametersDTO = new DemandPlanningGeneralParametersDTO();
        demandPlanningGeneralParametersDTO.executeDemandPlan = (parametrosDemandPlanNivelCluster.getExecutaDp() == null) ?
                true :
                parametrosDemandPlanNivelCluster.getExecutaDp();
        demandPlanningGeneralParametersDTO.uomId = (parametrosDemandPlanNivelCluster.getUnidadeMedidaPadraoDP() == null) ?
                clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal().getId()
                : parametrosDemandPlanNivelCluster.getUnidadeMedidaPadraoDP().getId();
        // O arredondamento pela UOM de venda e uma capacidade Pro. A borda
        // Community nunca reexpoe um valor legado persistido.
        demandPlanningGeneralParametersDTO.roundToSalesUnit = false;
        demandPlanningGeneralParametersDTO.considerHistoricalSalesOfInactiveDfus = (parametrosDemandPlanNivelCluster.getDpUsaHistoricoDemandaInativos() == null) ?
                true
                : parametrosDemandPlanNivelCluster.getDpUsaHistoricoDemandaInativos();
        /*
         * O override por cluster e opcional. Quando nao houver valor local,
         * a configuracao deve herdar exatamente o getter efetivo do aggregate
         * global, inclusive o default historico para registros antigos. A flag
         * de historico de DFUs inativas tem semantica distinta e nao participa
         * deste fallback.
         */
        demandPlanningGeneralParametersDTO.generateForecastForDiscontinuedMaterials = (parametrosDemandPlanNivelCluster.getDpGeraForecastParaDescontinuados() == null) ?
                parametrosGlobais.getDpGeraForecastParaDescontinuados()
                : parametrosDemandPlanNivelCluster.getDpGeraForecastParaDescontinuados();
        // A edicao Community executa sempre Top-Down nas duas dimensoes. Os
        // seletores de estrategia sao reabertos somente pelo overlay Pro.
        demandPlanningGeneralParametersDTO.materialAggregationType = Constantes.DPNivelAgregacao.TOP_DOWN;
        demandPlanningGeneralParametersDTO.locationAggregationType = Constantes.DPNivelAgregacao.TOP_DOWN;
        /*
         * Budget as Forecast e bases financeiras pertencem ao Enterprise.
         * O DTO Community conserva o campo apenas para tolerar payloads
         * transicionais; na leitura ele sempre volta neutro.
         */
        demandPlanningGeneralParametersDTO.budgetId = null;
        /*
         * Tratamento especifico de materiais novos pertence ao Enterprise. O
         * campo permanece no DTO apenas para rejeicao defensiva de payloads
         * legados, mas a leitura Community sempre devolve 0.
         */
        demandPlanningGeneralParametersDTO.daysAsNewMaterial = 0;
        demandPlanningGeneralParametersDTO.daysSalesHistory = (parametrosDemandPlanNivelCluster.getDiasHistoricosForecastEstatistico() == null) ?
                parametrosGlobais.getDiasHistoricosForecastEstatistico()
                : parametrosDemandPlanNivelCluster.getDiasHistoricosForecastEstatistico();
        /*
         * Support series/regression time series pertencem ao Enterprise. Mesmo
         * quando bancos Enterprise tiverem associacoes persistidas, o DTO
         * Community nao materializa nem expõe esses seletores.
         */
        demandPlanningGeneralParametersDTO.regressionTimeSeries = new ArrayList<>();
        /*
         * Regressores internos baseados em STL/trend target e dias uteis
         * pertencem ao Enterprise. O Community devolve sempre defaults neutros
         * para nao sugerir que a configuracao esta disponivel nesta edicao.
         */
        demandPlanningGeneralParametersDTO.considerTargetTrendGrowthYoy = false;
        demandPlanningGeneralParametersDTO.numberOfDaysCurrentLevelAsAverageOfHistoricalStl = 365;
        demandPlanningGeneralParametersDTO.targetGrowthYoy = 0.0;
        demandPlanningGeneralParametersDTO.includeWorkingDaysRegressor = false;
        demandPlanningGeneralParametersDTO.useExecutionProfileAutofitModel = false;


        return demandPlanningGeneralParametersDTO;

    }

    private DemandPlanningForecastParametersDTO getDemandPlanningForecastParametersDTOFromEntities(
            ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract) {

        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO = new DemandPlanningForecastParametersDTO();

        Constantes.DPModeloEstatistico dpModeloEstatisticoCadastrado =
                parametrosModeloEstatisticoAbstract.getDpModeloEstatistico();
        demandPlanningForecastParametersDTO.statisticalModel =
                dpModeloEstatisticoCadastrado == null
                        || !DemandPlanningModelCatalog.isDpModeloEstatisticoCommunity(dpModeloEstatisticoCadastrado) ?
                        Constantes.DPModeloEstatistico.MM
                        : dpModeloEstatisticoCadastrado;
        demandPlanningForecastParametersDTO.daysMovingAverageModel = (parametrosModeloEstatisticoAbstract.getDiasMediaMovelDp() == null) ?
                120
                : parametrosModeloEstatisticoAbstract.getDiasMediaMovelDp();
        // Stockout, limpeza historica e uplift sao capacidades Enterprise. O
        // Community mantem os campos apenas para payload legado/transicional e
        // devolve defaults neutros sem mapear colunas JPA.
        demandPlanningForecastParametersDTO.considerStockoutData = false;
        demandPlanningForecastParametersDTO.daysSmoothingModel = 365;
        demandPlanningForecastParametersDTO.enableUpperPercentileSmoothing = false;
        demandPlanningForecastParametersDTO.smoothingUpperPercentile = 85.0;
        demandPlanningForecastParametersDTO.enableLowerPercentileSmoothing = false;
        demandPlanningForecastParametersDTO.smoothingLowerPercentile = 15.0;
        demandPlanningForecastParametersDTO.smoothingModel = Constantes.DPModeloNormalizacao.DESATIVADO;
        demandPlanningForecastParametersDTO.upliftModel = Constantes.DPModeloUplift.DESATIVADO;
        /*
         * Mesmo que a base transicional ainda traga HTS/STL ou outro split
         * Enterprise salvo, a leitura Community devolve sempre Historical
         * Sales. Reexpor o valor legado poderia fazer clientes transicionais
         * reenviar uma configuracao que esta bloqueada nesta edicao.
         */
        demandPlanningForecastParametersDTO.splitModel = Constantes.DPModeloSplit.HISTORICAL_SALES;
        demandPlanningForecastParametersDTO.daysTopDownSplit = (parametrosModeloEstatisticoAbstract.getNumeroDiasSplitTopDown() == null) ?
                120
                : parametrosModeloEstatisticoAbstract.getNumeroDiasSplitTopDown();
        demandPlanningForecastParametersDTO.alpha = (parametrosModeloEstatisticoAbstract.getAlfa() == null) ?
                null
                : parametrosModeloEstatisticoAbstract.getAlfa();
        demandPlanningForecastParametersDTO.beta = (parametrosModeloEstatisticoAbstract.getBeta() == null) ?
                null
                : parametrosModeloEstatisticoAbstract.getBeta();
        demandPlanningForecastParametersDTO.gamma = (parametrosModeloEstatisticoAbstract.getGama() == null) ?
                null
                : parametrosModeloEstatisticoAbstract.getGama();
        // Campos Prophet/Chronos existem apenas para compatibilidade defensiva
        // com payloads Enterprise/legados. Community nao persiste esses
        // parametros e sempre devolve defaults neutros.
        demandPlanningForecastParametersDTO.prophetAutoSeasonalityPriorScale = true;
        demandPlanningForecastParametersDTO.prophetSeasonalityPriorScale = 10.0;
        demandPlanningForecastParametersDTO.prophetAutoChangepointPriorScale = true;
        demandPlanningForecastParametersDTO.prophetChangepointPriorScale = 0.05;
        demandPlanningForecastParametersDTO.prophetAutoYearlyFourierOrder = true;
        demandPlanningForecastParametersDTO.prophetYearlyFourierOrder = 10;
        demandPlanningForecastParametersDTO.chronosForceAggregatedForecast = false;

        return demandPlanningForecastParametersDTO;

    }


    public ParametrosDemandPlanNivelClusterProjection getProjectionDeDto(
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {

        /*
         * Falha cedo para configuracoes Enterprise antes de carregar perfil,
         * clusters, UOM ou factory de projection. Isso mantém a borda Community
         * defensiva para chamadas manuais e evita consultas desnecessarias.
         */
        validaDemandPlanningClusterLevelConfigurationDTOCommunity(demandPlanningClusterLevelConfigurationDTO);
        validaIdentidadeDemandPlanningClusterLevelConfigurationDTOCommunity(demandPlanningClusterLevelConfigurationDTO);

        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster = getNovaEntidadeParametrosDeDto(demandPlanningClusterLevelConfigurationDTO);

        // Auto-fit e arvore de regressao sao capacidades Enterprise. O mapper
        // Community monta apenas a projection simples em nivel de cluster.
        return parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanNivelClusterProjection(
                parametrosDemandPlanNivelCluster);

    }

    /**
     * Valida o contrato editavel da configuracao cluster-level de Demand
     * Planning Community.
     *
     * <p>O front Community recebe listas vazias/defaults neutros para campos
     * Enterprise, mas services front tambem chamam este metodo antes de qualquer
     * repository/factory para bloquear payloads manuais ou DTOs antigos tentando
     * salvar support series, auto-fit, budget, material novo, regressores,
     * stockout/outlier/uplift, foundation models ou split diferente de
     * Historical Sales.</p>
     */
    public void validaDemandPlanningClusterLevelConfigurationDTOCommunity(
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {

        if (demandPlanningClusterLevelConfigurationDTO == null) {
            throw new IllegalArgumentException(
                    "Demand Planning cluster-level configuration DTO is required");
        }
        if (demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters == null) {
            throw new IllegalArgumentException(
                    "Demand Planning general parameters are required");
        }
        if (demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters == null) {
            throw new IllegalArgumentException(
                    "Demand Planning forecast parameters are required");
        }

        if (demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.regressionTimeSeries != null
                && !demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.regressionTimeSeries.isEmpty()) {
            throw new RequiresEnterpriseVersionException("Demand Planning support series");
        }
        if (Boolean.TRUE.equals(demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.useExecutionProfileAutofitModel)) {
            throw new RequiresEnterpriseVersionException("Demand Planning default auto-fit configuration");
        }
        if (Boolean.TRUE.equals(demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.roundToSalesUnit)) {
            throw new RequiresEnterpriseVersionException("Demand Planning round forecast to sales UOM");
        }
        if ((demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.materialAggregationType != null
                && !Constantes.DPNivelAgregacao.TOP_DOWN.equals(
                demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.materialAggregationType))
                || (demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.locationAggregationType != null
                && !Constantes.DPNivelAgregacao.TOP_DOWN.equals(
                demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.locationAggregationType))) {
            throw new RequiresEnterpriseVersionException("Demand Planning aggregation strategy");
        }
        if (demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.budgetId != null) {
            throw new RequiresEnterpriseVersionException("Demand Planning Budget as Forecast");
        }
        if (demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.daysAsNewMaterial != null
                && demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.daysAsNewMaterial > 0) {
            throw new RequiresEnterpriseVersionException("Demand Planning New Material Treatment");
        }
        validaJanelaHistoricaPositiva(
                demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.daysSalesHistory,
                "Demand Planning statistical forecast historical window must be positive");
        if (Boolean.TRUE.equals(demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.considerTargetTrendGrowthYoy)
                || (demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.numberOfDaysCurrentLevelAsAverageOfHistoricalStl != null
                && !Integer.valueOf(365).equals(demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.numberOfDaysCurrentLevelAsAverageOfHistoricalStl))
                || (demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.targetGrowthYoy != null
                && Double.compare(demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.targetGrowthYoy, 0.0) != 0)) {
            throw new RequiresEnterpriseVersionException("Demand Planning ARIMA/STL target trend regressor");
        }
        if (Boolean.TRUE.equals(demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.includeWorkingDaysRegressor)) {
            throw new RequiresEnterpriseVersionException("Demand Planning working days regressor");
        }
        validaForecastParametersEnterpriseCommunity(demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters);

    }

    /**
     * Valida as chaves funcionais obrigatorias da configuracao cluster-level.
     *
     * <p>Este metodo fica separado da validacao Enterprise para permitir que
     * services preservem a ordem de erro correta: primeiro bloqueiam campos
     * privados, depois acusam payload Community incompleto antes de qualquer
     * repository/factory. Essa separacao evita que um payload Chronos/HTS sem
     * ids seja reportado como erro de formulario incompleto em vez de erro de
     * edicao.</p>
     */
    public void validaIdentidadeDemandPlanningClusterLevelConfigurationDTOCommunity(
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {

        if (demandPlanningClusterLevelConfigurationDTO == null) {
            throw new IllegalArgumentException(
                    "Demand Planning cluster-level configuration DTO is required");
        }
        if (demandPlanningClusterLevelConfigurationDTO.demandPlanExecutionProfileId == null
                || demandPlanningClusterLevelConfigurationDTO.demandPlanExecutionProfileId.isBlank()) {
            throw new IllegalArgumentException(
                    "Demand Planning execution profile id is required");
        }
        if (demandPlanningClusterLevelConfigurationDTO.locationClusterId == null) {
            throw new IllegalArgumentException(
                    "Demand Planning location cluster id is required");
        }
        if (demandPlanningClusterLevelConfigurationDTO.materialClusterId == null) {
            throw new IllegalArgumentException(
                    "Demand Planning material cluster id is required");
        }

    }

    /**
     * Bloqueia a persistencia de parametros de forecast Enterprise no
     * Community.
     *
     * <p>Os enums/campos continuam no DTO para compatibilidade com o front
     * compartilhado e com payloads legados. A regra Community, porem, deve
     * falhar antes de qualquer escrita, evitando que um perfil salvo localmente
     * só exploda mais tarde durante a execucao do plano.</p>
     */
    private void validaForecastParametersEnterpriseCommunity(
            DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO) {

        /*
         * Modelo e split nulos significam payload quebrado, nao escolha de
         * default. O front Community deve enviar explicitamente a alternativa
         * selecionada para que a entidade persistida e o workflow de execucao
         * compartilhem o mesmo contrato.
         */
        if (demandPlanningForecastParametersDTO.statisticalModel == null) {
            throw new IllegalArgumentException(
                    "Demand Planning statistical forecast model is required");
        }
        Constantes.DPModeloEstatistico dpModeloEstatistico =
                demandPlanningForecastParametersDTO.statisticalModel;
        if (!DemandPlanningModelCatalog.isDpModeloEstatisticoCommunity(dpModeloEstatistico)) {
            throw new RequiresEnterpriseVersionException("Demand Planning Forecast Model " + dpModeloEstatistico);
        }

        if (demandPlanningForecastParametersDTO.splitModel == null) {
            throw new IllegalArgumentException(
                    "Demand Planning forecast split model is required");
        }
        Constantes.DPModeloSplit dpModeloSplit =
                demandPlanningForecastParametersDTO.splitModel;
        if (!getDpModelosSplitDisponiveis().contains(dpModeloSplit)) {
            throw new RequiresEnterpriseVersionException("Demand Planning Split Model " + dpModeloSplit);
        }
        validaJanelaHistoricaPositiva(
                demandPlanningForecastParametersDTO.daysTopDownSplit,
                "Demand Planning Historical Sales split reference window must be positive");
        validaJanelaHistoricaPositiva(
                demandPlanningForecastParametersDTO.daysMovingAverageModel,
                "Demand Planning Moving Average historical window must be positive");

        Constantes.DPModeloUplift dpModeloUplift = (demandPlanningForecastParametersDTO.upliftModel == null) ?
                Constantes.DPModeloUplift.DESATIVADO
                : demandPlanningForecastParametersDTO.upliftModel;
        if (!Constantes.DPModeloUplift.DESATIVADO.equals(dpModeloUplift)) {
            throw new RequiresEnterpriseVersionException("Demand Planning Event Uplift");
        }

        if (Boolean.TRUE.equals(demandPlanningForecastParametersDTO.considerStockoutData)) {
            throw new RequiresEnterpriseVersionException("Demand Planning Stockout Treatment");
        }

        Constantes.DPModeloNormalizacao dpModeloNormalizacao = (demandPlanningForecastParametersDTO.smoothingModel == null) ?
                Constantes.DPModeloNormalizacao.DESATIVADO
                : demandPlanningForecastParametersDTO.smoothingModel;
        if (!Constantes.DPModeloNormalizacao.DESATIVADO.equals(dpModeloNormalizacao)) {
            throw new RequiresEnterpriseVersionException("Demand Planning Outlier/Event History Cleaning");
        }
        if ((demandPlanningForecastParametersDTO.daysSmoothingModel != null
                && !Integer.valueOf(365).equals(demandPlanningForecastParametersDTO.daysSmoothingModel))
                || Boolean.TRUE.equals(demandPlanningForecastParametersDTO.enableUpperPercentileSmoothing)
                || (demandPlanningForecastParametersDTO.smoothingUpperPercentile != null
                && Double.compare(demandPlanningForecastParametersDTO.smoothingUpperPercentile, 85.0) != 0)
                || Boolean.TRUE.equals(demandPlanningForecastParametersDTO.enableLowerPercentileSmoothing)
                || (demandPlanningForecastParametersDTO.smoothingLowerPercentile != null
                && Double.compare(demandPlanningForecastParametersDTO.smoothingLowerPercentile, 15.0) != 0)) {
            throw new RequiresEnterpriseVersionException("Demand Planning Outlier/Event History Cleaning");
        }

        if (Boolean.FALSE.equals(demandPlanningForecastParametersDTO.prophetAutoSeasonalityPriorScale)
                || (demandPlanningForecastParametersDTO.prophetSeasonalityPriorScale != null
                && Double.compare(demandPlanningForecastParametersDTO.prophetSeasonalityPriorScale, 10.0) != 0)
                || Boolean.FALSE.equals(demandPlanningForecastParametersDTO.prophetAutoChangepointPriorScale)
                || (demandPlanningForecastParametersDTO.prophetChangepointPriorScale != null
                && Double.compare(demandPlanningForecastParametersDTO.prophetChangepointPriorScale, 0.05) != 0)
                || Boolean.FALSE.equals(demandPlanningForecastParametersDTO.prophetAutoYearlyFourierOrder)
                || (demandPlanningForecastParametersDTO.prophetYearlyFourierOrder != null
                && !Integer.valueOf(10).equals(demandPlanningForecastParametersDTO.prophetYearlyFourierOrder))) {
            throw new RequiresEnterpriseVersionException("Demand Planning Prophet Parameters");
        }

        if (Boolean.TRUE.equals(demandPlanningForecastParametersDTO.chronosForceAggregatedForecast)) {
            throw new RequiresEnterpriseVersionException("Demand Planning Chronos Parameters");
        }

    }

    private void validaJanelaHistoricaPositiva(
            Integer janelaHistorica,
            String mensagemErro) {

        /*
         * Nulo e interpretado como pedido explicito de default publico no
         * metodo de copia para entidade. Valor preenchido menor ou igual a zero
         * e configuracao invalida e nao deve ser persistido para falhar apenas
         * durante a rodada estatistica.
         */
        if (janelaHistorica != null && janelaHistorica <= 0) {
            throw new IllegalArgumentException(mensagemErro);
        }

    }

    /**
     * Modelos de split aceitos por esta implementacao de mapper.
     *
     * <p>O Community retorna somente Historical Sales. Overlays Enterprise
     * podem ampliar a lista para splits ja migrados, mantendo no mesmo metodo
     * herdado os demais bloqueios de stockout, outlier, uplift, support series
     * e parametros privados ainda nao disponiveis.</p>
     */
    protected Set<Constantes.DPModeloSplit> getDpModelosSplitDisponiveis() {

        return DemandPlanningModelCatalog.getDpModelosSplitCommunity();

    }

    /**
     * Cria uma entidade nao persistida com os valores do DTO.
     *
     * <p>No Community a configuracao de Demand Planning e sempre materializada
     * no nivel cluster material/location. Configuracoes por arvore, leaf/node ou
     * nivel MAPE pertencem ao Enterprise e nao participam deste mapper.</p>
     *
     * @param demandPlanningClusterLevelConfigurationDTO configuracao editada no
     *                                                   front Community
     * @return entidade transicional pronta para virar projection ou persistencia
     */
    public ParametrosDemandPlanNivelCluster getNovaEntidadeParametrosDeDto(
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {

        /*
         * Este metodo tambem e publico e pode ser usado sem passar por
         * getProjectionDeDto. Portanto ele repete a validacao antes de qualquer
         * acesso a repositories/factories.
         */
        validaDemandPlanningClusterLevelConfigurationDTOCommunity(demandPlanningClusterLevelConfigurationDTO);
        validaIdentidadeDemandPlanningClusterLevelConfigurationDTOCommunity(demandPlanningClusterLevelConfigurationDTO);

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = perfilExecucaoDemandPlanRepository
                .findById(demandPlanningClusterLevelConfigurationDTO.demandPlanExecutionProfileId)
                .get();
        ClusterEParametrosProjection clusterEParametrosProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        ClusterLocations clusterLocations = clusterEParametrosProjection.getClusterLocationsDeId(
                demandPlanningClusterLevelConfigurationDTO.locationClusterId);
        ClusterMateriais clusterMateriaisDemandPlanning = clusterEParametrosProjection
                .getClusterMateriaisDemandPlanningDeId(
                        demandPlanningClusterLevelConfigurationDTO.materialClusterId)
                .get();
        
        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster = new ParametrosDemandPlanNivelCluster(
                new ParametrosDemandPlanNivelCluster.ParametrosDemandPlanNivelClusterCompositeKey(
                        perfilExecucaoDemandPlan,
                        clusterMateriaisDemandPlanning,
                        clusterLocations));

        atualizaEntidadeParametrosComDTO(parametrosDemandPlanNivelCluster, demandPlanningClusterLevelConfigurationDTO);

        return parametrosDemandPlanNivelCluster;
        
    }

    /**
     * Atualiza uma entidade de configuração com os valores trazidos no DTO.
     *
     * <p>Esta borda grava apenas campos Community. Campos Enterprise presentes
     * no DTO compartilhado sao validados antes da escrita e, se vierem ativos,
     * falham com erro de edicao antes de qualquer consulta em repository.</p>
     *
     * @param parametrosDemandPlanNivelCluster entidade cluster-level que sera
     *                                         preenchida com a configuracao
     *                                         aceita no Community
     * @param demandPlanningClusterLevelConfigurationDTO payload recebido da tela
     *                                                   de configuracao de
     *                                                   Demand Planning
     * @return a propria entidade recebida, ja atualizada para persistencia
     */
    public ParametrosDemandPlanNivelCluster atualizaEntidadeParametrosComDTO(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster,
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {

        validaDemandPlanningClusterLevelConfigurationDTOCommunity(demandPlanningClusterLevelConfigurationDTO);

        DemandPlanningGeneralParametersDTO demandPlanningGeneralParametersDTO =
                demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters;
        /*
         * O value object de agregacao e o dono do fallback top-down. Usar o
         * mesmo ponto aqui e no workflow evita persistir nulos que depois seriam
         * interpretados de outro modo por factories/engines.
         */
        ParametrosAgregacaoForecast parametrosAgregacaoForecast =
                new ParametrosAgregacaoForecast(
                        demandPlanningGeneralParametersDTO.locationAggregationType,
                        demandPlanningGeneralParametersDTO.materialAggregationType);

        parametrosDemandPlanNivelCluster.setExecutaDp(demandPlanningGeneralParametersDTO.executeDemandPlan);
        parametrosDemandPlanNivelCluster.setLocationAggregationType(parametrosAgregacaoForecast.getLocationAggregationType());
        parametrosDemandPlanNivelCluster.setMaterialAggregationType(parametrosAgregacaoForecast.getMaterialAggregationType());
        parametrosDemandPlanNivelCluster.setDiasHistoricosForecastEstatistico(demandPlanningGeneralParametersDTO.daysSalesHistory);
        parametrosDemandPlanNivelCluster.setDpUsaHistoricoDemandaInativos(demandPlanningGeneralParametersDTO.considerHistoricalSalesOfInactiveDfus);
        parametrosDemandPlanNivelCluster.setDpGeraForecastParaDescontinuados(demandPlanningGeneralParametersDTO.generateForecastForDiscontinuedMaterials);
        /*
         * UOM nula significa usar a unidade global, mesma regra usada na leitura
         * da configuracao. Evitamos consultar repository com chave nula para que
         * payloads parciais falhem apenas nas validacoes funcionais reais.
         */
        parametrosDemandPlanNivelCluster.setUnidadeMedidaPadraoDP(
                getUnidadeMedidaPadraoDpOuNull(demandPlanningGeneralParametersDTO));
        parametrosDemandPlanNivelCluster.setArredondaParaUnidadeVenda(false);
        /*
         * A escrita dos parametros estatisticos fica isolada para deixar
         * evidente que apenas campos Community chegam na entidade. Stockout,
         * smoothing, uplift, Prophet/Chronos e demais parametros Enterprise ja
         * foram bloqueados por validaDemandPlanningClusterLevelConfigurationDTOCommunity(...).
         */
        setParametrosModeloEstatisticoEmEntidade(
                parametrosDemandPlanNivelCluster,
                demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters);

        return parametrosDemandPlanNivelCluster;

    }

    /**
     * Resolve a UOM configurada no DTO.
     *
     * <p>UOM nula significa herdar a unidade global do ambiente. UOM informada,
     * porem, precisa existir: cair silenciosamente para a global esconderia erro
     * de cadastro ou payload.</p>
     */
    @Nullable
    private UnidadeMedida getUnidadeMedidaPadraoDpOuNull(
            DemandPlanningGeneralParametersDTO demandPlanningGeneralParametersDTO) {

        if (demandPlanningGeneralParametersDTO.uomId == null) {
            return null;
        }

        return unidadeMedidaRepository
                .findById(demandPlanningGeneralParametersDTO.uomId)
                .get();

    }

    /**
     * Copia para a entidade apenas os parametros estatisticos persistidos pelo
     * Community.
     *
     * <p>O DTO compartilhado possui varios campos Enterprise transicionais, mas
     * esta entidade Community ainda carrega somente modelo estatistico, split
     * Historical Sales, janela do split e parametros alpha/beta/gamma. O metodo
     * deve permanecer pequeno e explicito para que qualquer novo campo persistido
     * seja uma decisao visivel de fronteira.</p>
     */
    private void setParametrosModeloEstatisticoEmEntidade(
            ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract,
            DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO) {

        /*
         * Modelo estatistico e split ja foram validados como obrigatorios em
         * validaForecastParametersEnterpriseCommunity(...). Mantemos a copia
         * direta aqui para que qualquer chamada publica que tente persistir
         * payload incompleto falhe antes de chegar neste ponto.
         */
        parametrosModeloEstatisticoAbstract.setDpModeloSplit(demandPlanningForecastParametersDTO.splitModel);
        parametrosModeloEstatisticoAbstract.setNumeroDiasSplitTopDown(
                demandPlanningForecastParametersDTO.daysTopDownSplit == null ?
                        120 :
                        demandPlanningForecastParametersDTO.daysTopDownSplit);
        parametrosModeloEstatisticoAbstract.setModeloUplift(
                demandPlanningForecastParametersDTO.upliftModel == null ?
                        Constantes.DPModeloUplift.DESATIVADO :
                        demandPlanningForecastParametersDTO.upliftModel);
        parametrosModeloEstatisticoAbstract.setDpModeloEstatistico(demandPlanningForecastParametersDTO.statisticalModel);
        parametrosModeloEstatisticoAbstract.setDiasMediaMovelDp(
                demandPlanningForecastParametersDTO.daysMovingAverageModel == null ?
                        120 :
                        demandPlanningForecastParametersDTO.daysMovingAverageModel);
        parametrosModeloEstatisticoAbstract.setAlfa(demandPlanningForecastParametersDTO.alpha);
        parametrosModeloEstatisticoAbstract.setBeta(demandPlanningForecastParametersDTO.beta);
        parametrosModeloEstatisticoAbstract.setGama(demandPlanningForecastParametersDTO.gamma);
        parametrosModeloEstatisticoAbstract.setConsideraDadosEstoque(
                demandPlanningForecastParametersDTO.considerStockoutData);
        parametrosModeloEstatisticoAbstract.setModeloNormalizacao(
                demandPlanningForecastParametersDTO.smoothingModel == null ?
                        Constantes.DPModeloNormalizacao.DESATIVADO :
                        demandPlanningForecastParametersDTO.smoothingModel);
        parametrosModeloEstatisticoAbstract.setDiasHistoricosNormalizacao(
                demandPlanningForecastParametersDTO.daysSmoothingModel == null ?
                        Constantes.DP_PADRAO_DIAS_NORMALIZACAO :
                        demandPlanningForecastParametersDTO.daysSmoothingModel);
        parametrosModeloEstatisticoAbstract.setHabilitaLimpezaHistoricoPercentilSuperior(
                demandPlanningForecastParametersDTO.enableUpperPercentileSmoothing);
        parametrosModeloEstatisticoAbstract.setPercentilSuperiorLimpezaHistorico(
                demandPlanningForecastParametersDTO.smoothingUpperPercentile == null ?
                        null :
                        demandPlanningForecastParametersDTO.smoothingUpperPercentile / 100.0d);
        parametrosModeloEstatisticoAbstract.setHabilitaLimpezaHistoricoPercentilInferior(
                demandPlanningForecastParametersDTO.enableLowerPercentileSmoothing);
        parametrosModeloEstatisticoAbstract.setPercentilInferiorLimpezaHistorico(
                demandPlanningForecastParametersDTO.smoothingLowerPercentile == null ?
                        null :
                        demandPlanningForecastParametersDTO.smoothingLowerPercentile / 100.0d);
        parametrosModeloEstatisticoAbstract.setProphetAutoSeasonalityPriorScale(
                demandPlanningForecastParametersDTO.prophetAutoSeasonalityPriorScale);
        parametrosModeloEstatisticoAbstract.setProphetSeasonalityPriorScale(
                demandPlanningForecastParametersDTO.prophetSeasonalityPriorScale);
        parametrosModeloEstatisticoAbstract.setProphetAutoChangepointPriorScale(
                demandPlanningForecastParametersDTO.prophetAutoChangepointPriorScale);
        parametrosModeloEstatisticoAbstract.setProphetChangepointPriorScale(
                demandPlanningForecastParametersDTO.prophetChangepointPriorScale);
        parametrosModeloEstatisticoAbstract.setProphetAutoYearlyFourierOrder(
                demandPlanningForecastParametersDTO.prophetAutoYearlyFourierOrder);
        parametrosModeloEstatisticoAbstract.setProphetYearlyFourierOrder(
                demandPlanningForecastParametersDTO.prophetYearlyFourierOrder);
        parametrosModeloEstatisticoAbstract.setChronosForcaForecastAgregado(
                demandPlanningForecastParametersDTO.chronosForceAggregatedForecast);

    }

}
