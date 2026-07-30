package com.opsfactor.community.capability.demandplanning.facade;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningClusterLevelConfigurationDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningPreviaForecastRequestDTO;
import com.opsfactor.community.capability.cluster.facade.mapper.ClusterLocationsMapper;
import com.opsfactor.community.capability.cluster.facade.mapper.ClusterProdutosMapper;
import com.opsfactor.community.capability.demandplanning.configuration.facade.mapper.DemandPlanningConfigurationMapper;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.capability.cluster.repository.material.ClusterProdutosDemandPlanningRepository;
import com.opsfactor.community.capability.demandplanning.configuration.repository.ParametrosDemandPlanNivelClusterRepository;
import com.opsfactor.community.capability.demandplanning.configuration.repository.PerfilExecucaoDemandPlanRepository;
import com.opsfactor.community.capability.cluster.service.ClusterLocationService;
import com.opsfactor.community.capability.demandplanning.engine.DemandPlanning;
import com.opsfactor.community.capability.demandplanning.facade.dto.SimulatedDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.facade.mapper.DemandAnalysisMapper;
import com.opsfactor.community.capability.demandplanning.service.DemandPlanningService;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.NoResultException;
import java.util.*;

/**
 * Service de configuracao e simulacao Community de Demand Planning.
 *
 * <p>Esta classe alimenta a tela historicamente chamada de Demand Analysis,
 * mas sua responsabilidade no Community e limitada a editar parametros
 * cluster-level e simular forecast com o fluxo estatistico permitido. Demand
 * Accuracy, Auto-fit, support series, regression tree e pricing clusters
 * pertencem ao Enterprise e sao bloqueados nas bordas de mapper/service.</p>
 */
@Service
public class DemandSimulationFacade {

    /**
     * Service principal de Demand Planning usado para executar o mesmo workflow
     * estatistico Community da rodada real, reaproveitando sales/projections ja
     * materializadas pela simulacao.
     */
    @Autowired
    private DemandPlanningService demandPlanningService;

    /**
     * Service de master data usado para resolver o cluster de locations da
     * simulacao. O recorte Community permite somente clusters DP operacionais.
     */
    @Autowired
    private ClusterLocationService clusterLocationService;

    /**
     * Mapper do DTO de simulacao. No Community ele publica exclusivamente
     * series material/location e nao deve reintroduzir agregado MAPE, arvore,
     * support series ou diagnosticos Enterprise.
     */
    @Autowired
    private DemandAnalysisMapper demandAnalysisMapper;

    /**
     * Mapper dono da validacao de configuracao cluster-level Community. A
     * simulacao chama esse bean antes de repositories/factories para bloquear
     * payloads Enterprise cedo.
     */
    @Autowired
    private DemandPlanningConfigurationMapper demandPlanningConfigurationMapper;

    /**
     * Repository dos parametros cluster-level salvos/editados pela tela de
     * configuracao e simulacao de Demand Planning.
     */
    @Autowired
    private ParametrosDemandPlanNivelClusterRepository parametrosDemandPlanNivelClusterRepository;

    /**
     * Repository transicional do cluster de materiais. O tipo fisico ainda usa
     * `Produtos`, mas a borda publica e a variavel local falam em materiais.
     */
    @Autowired
    private ClusterProdutosDemandPlanningRepository clusterMateriaisDemandPlanningRepository;

    /**
     * Repository do perfil de execucao Demand Planning usado para bucket,
     * horizonte, documento historico e defaults operacionais Community.
     */
    @Autowired
    private PerfilExecucaoDemandPlanRepository perfilExecucaoDemandPlanRepository;

    /**
     * Factory da projection geral de parametros, clusters e master data usada
     * para resolver escopos material/location sem consultas N+1 durante a
     * simulacao.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Factory da projection de unidades/conversoes usada para normalizar o
     * historico de vendas na UOM Demand Planning configurada.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /**
     * Factory da projection de sales. No Community ela aceita apenas sell-out e
     * bloqueia sell-in/sales orders antes de tocar em repositories.
     */
    @Autowired
    private SalesProjectionFactory salesProjectionFactory;

    public DemandPlanningClusterLevelConfigurationDTO getDemandPlanningConfigurationDTO(
            String executionProfileId,
            Long locationClusterId,
            Long materialClusterId) {

        validaChavesConsultaDemandPlanningConfigurationCommunity(
                executionProfileId,
                locationClusterId,
                materialClusterId);

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                perfilExecucaoDemandPlanRepository.findById(executionProfileId).get();
        /*
         * A entidade fisica ainda se chama ClusterProdutosDemandPlanning, mas a
         * borda Community e o front novo trabalham com o conceito publico de
         * cluster de materiais.
         */
        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning =
                clusterMateriaisDemandPlanningRepository.findById(materialClusterId).get();
        ClusterLocations clusterLocations =
                clusterLocationService.getClusterLocation(locationClusterId).get();

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO = demandPlanningConfigurationMapper.getDemandPlanningConfigurationDtoFromEntities(
                getParametrosDemandPlanNivelClusterExistenteOuNovoCommunity(
                        perfilExecucaoDemandPlan,
                        clusterMateriaisDemandPlanning,
                        clusterLocations));

        return demandPlanningClusterLevelConfigurationDTO;

    }

    /**
     * Simula o forecast Community para a tela de configuracao cluster-level.
     *
     * <p>O metodo prepara historico + forecast com os parametros passados pelo
     * usuario. No Community nao ha tratamento real de stockout/outlier/evento;
     * a serie limpa retornada ao front e copia da venda historica observada.</p>
     *
     * <p>A resposta e o `SimulatedDemandPlanDTO`, sempre em material/location.
     * Forecast agregado, diagnosticos de auto-fit, support series e analytics
     * Enterprise nao participam deste DTO.</p>
     */
    public SimulatedDemandPlanDTO getSimulatedDemandPlanDTO(
            DemandPlanningPreviaForecastRequestDTO demandPlanningPreviaForecastRequestDTO) {

        if (demandPlanningPreviaForecastRequestDTO == null) {
            throw new IllegalArgumentException("Demand Planning simulation request is required");
        }

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO = demandPlanningPreviaForecastRequestDTO.demandPlanningConfiguration;

        /*
         * A simulacao usa a mesma configuracao editavel do cluster-level.
         * Parametros Enterprise devem falhar antes de buscar clusters,
         * projections ou historico de vendas.
         */
        demandPlanningConfigurationMapper.validaDemandPlanningClusterLevelConfigurationDTOCommunity(
                demandPlanningClusterLevelConfigurationDTO);

        validaModeloForecastPermitidoNaSimulacao(
                demandPlanningClusterLevelConfigurationDTO);

        if (demandPlanningPreviaForecastRequestDTO.referenceDate == null) {
            throw new IllegalArgumentException("Demand Planning simulation reference date is required");
        }
        demandPlanningConfigurationMapper.validaIdentidadeDemandPlanningClusterLevelConfigurationDTOCommunity(
                demandPlanningClusterLevelConfigurationDTO);

        /*
         * Mantemos o tipo JPA transicional, mas deste ponto em diante o nome da
         * variavel acompanha o contrato Community: material cluster.
         */
        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning =
                clusterMateriaisDemandPlanningRepository.findById(
                        demandPlanningClusterLevelConfigurationDTO.materialClusterId).get();
        ClusterLocations clusterLocations =
                clusterLocationService.getClusterLocation(
                        demandPlanningClusterLevelConfigurationDTO.locationClusterId).get();

        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        // cria-se um projection de parâmetros do demand planning a partir do DTO
        ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection = demandPlanningConfigurationMapper.getProjectionDeDto(
                demandPlanningClusterLevelConfigurationDTO);
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = parametrosDemandPlanNivelClusterProjection.getPerfilExecucaoDemandPlan();

        // A simulacao Community trabalha com historico em dias porque a projection de vendas compartilhada
        // e diaria. Granularidades menores exigem uma projection de sales especifica.
        Calendario calendario = DemandPlanning.getCalendarioDemandPlanComPeriodosPassadosEFuturos(
                parametrosDemandPlanNivelClusterProjection,
                perfilExecucaoDemandPlan,
                demandPlanningPreviaForecastRequestDTO.referenceDate.atStartOfDay());

        boolean usaHistoricoDemandaDeDfusInativos = parametrosDemandPlanNivelClusterProjection
                .getParametrosGeraisDemandPlanningProjection()
                .isDpUsaHistoricoDemandaInativos();

        Set<Produto> materiaisDoCluster = clusterEParametrosProjection.getMateriaisDeClusterMateriaisDemandPlanning(
                clusterMateriaisDemandPlanning,
                !usaHistoricoDemandaDeDfusInativos);
        Set<Location> locationsCluster = clusterEParametrosProjection.getLocationsDeClusterLocations(
                clusterLocations,
                !usaHistoricoDemandaDeDfusInativos);

        UnidadeMedida unidadeMedidaDP = parametrosDemandPlanNivelClusterProjection
                .getParametrosGeraisDemandPlanningProjection()
                .getUnidadeMedidaDP();
        SalesProjectionLocationMaterialData salesProjection = salesProjectionFactory.getSalesProjectionLocationMaterialData(
                        perfilExecucaoDemandPlan.getTipoDocumentoVenda(parametrosGlobais),
                        calendario,
                        locationsCluster, materiaisDoCluster,
                        unidadeMedidaProjection,
                        clusterEParametrosProjection,
                        unidadeMedidaDP);

        /*
         * A simulacao precisa da mesma sales projection para montar o DTO de
         * resposta e para gerar o forecast. Por isso chamamos o overload do
         * DemandPlanningService que recebe insumos ja materializados, evitando
         * uma segunda extracao de historico de vendas sem duplicar regras de
         * validacao Community.
         */
        LocationProjection locationProjection = LocationProjectionFactory
                .getProjectionClusterLocations(clusterLocations, clusterEParametrosProjection, !usaHistoricoDemandaDeDfusInativos);
        MaterialProjection materialProjection = MaterialProjectionFactory
                .getProjectionClusterMateriais(clusterMateriaisDemandPlanning, clusterEParametrosProjection, !usaHistoricoDemandaDeDfusInativos);

        // gera o forecast
        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao = demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                calendario,
                parametrosDemandPlanNivelClusterProjection,
                materialProjection,
                locationProjection,
                salesProjection,
                clusterEParametrosProjection,
                true);
        validaDemandPlanForecastProjectionsExecucaoSimulacaoCommunity(
                demandPlanForecastProjectionsExecucao);

        SimulatedDemandPlanDTO simulatedDemandPlanDTO = demandAnalysisMapper.demandPlanProjectionToDemandModelSetupDTO(
                demandPlanningClusterLevelConfigurationDTO,
                calendario,
                demandPlanForecastProjectionsExecucao,
                salesProjection);
        validaSimulatedDemandPlanDTOSimulacaoCommunity(simulatedDemandPlanDTO);
        // FIM CARGA HISTORICO E FORECAST POR MATERIAL/LOCATION ---------------------------------------

        // seta os clusters
        simulatedDemandPlanDTO.materialClusterDTO = ClusterProdutosMapper.convertComListaMateriaisERegrasAlocacaoDTO(
                clusterMateriaisDemandPlanning, clusterEParametrosProjection);
        simulatedDemandPlanDTO.clusterLocationsDTO = ClusterLocationsMapper.convertComListaLocationsERegrasAlocacaoDTO(
                clusterLocations, clusterEParametrosProjection);

        // lista com:
        // Calendario mensal : meses
        // Calendario semanal : semanas WW
        // Calendario diario : dia da semana (seg/ter...)
        simulatedDemandPlanDTO.setAgrupadoresPeriodoDesagregado(calendario.getListaAgrupadoresPeriodo(1));
        // lista com:
        // Calendario mensal : anos
        // Calendario semanal : anos
        // Calendario diario : semanas YYYYWW
        simulatedDemandPlanDTO.setAgrupadoresPeriodoAgregado(calendario.getListaAgrupadoresPeriodo(0));

        simulatedDemandPlanDTO.periodoInicioForecast = calendario.getUltimaDataPeriodo(calendario.getPosicaoPeriodoPresente());
        simulatedDemandPlanDTO.posicaoPeriodoInicioForecast = calendario.getPosicaoPeriodoPresente();
        simulatedDemandPlanDTO.posicaoPeriodoUltimaVenda = salesProjection.getUltimoPeriodoComSales()
                .orElse(calendario.getPosicaoPeriodoFinalPassado());
        simulatedDemandPlanDTO.periodoUltimaVenda = calendario.getUltimaDataPeriodo(simulatedDemandPlanDTO.posicaoPeriodoUltimaVenda);

        // Retorna DTO com todos os dados publicos do cluster material/location.
        return simulatedDemandPlanDTO;
    }

    /**
     * Mantem a fronteira de modelos da simulacao Community depois da
     * validacao estrutural compartilhada do mapper.
     *
     * <p>O overlay Enterprise reabre exclusivamente Budget as Forecast e
     * preserva este metodo para todos os demais modelos. O workflow de
     * simulacao continua unico e nao ganha dependencias Enterprise aqui.</p>
     */
    protected void validaModeloForecastPermitidoNaSimulacao(
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {

        if (Constantes.DPModeloEstatistico.BUDGET_DECOMPOSITION.equals(
                demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters.statisticalModel)) {
            throw new IllegalArgumentException(
                    "Budget as Forecast is not supported by Demand Planning forecast simulation.");
        }

    }

    /**
     * Valida a fotografia de projections calculada pelo service de Demand
     * Planning antes de entregar ao mapper da tela de simulacao.
     *
     * <p>Lista vazia e valida: significa que o recorte material/location nao
     * possui DFUs com historico/escopo para simular. Lista nula ou item nulo,
     * por outro lado, indica falha estrutural no service/projection upstream e
     * nao deve virar `NullPointerException` dentro do mapper, porque o front
     * trata a resposta como uma simulacao completa.</p>
     */
    private void validaDemandPlanForecastProjectionsExecucaoSimulacaoCommunity(
            List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao) {

        if (demandPlanForecastProjectionsExecucao == null) {
            throw new IllegalStateException(
                    "Demand Planning forecast simulation requires forecast projection result.");
        }

        for (int index = 0; index < demandPlanForecastProjectionsExecucao.size(); index++) {
            DemandPlanForecastProjection demandPlanForecastProjection =
                    demandPlanForecastProjectionsExecucao.get(index);
            if (demandPlanForecastProjection == null) {
                throw new IllegalStateException(
                        "Demand Planning forecast simulation projection at index "
                                + index
                                + " is required.");
            }
        }

    }

    /**
     * Valida o DTO base devolvido pelo mapper antes de enriquecer com clusters,
     * agrupadores de calendario e metadados de ultima venda.
     *
     * <p>O mapper e um bean independente e pode ser substituido no Enterprise.
     * Se ele devolver nulo, a simulacao Community deve falhar nesta borda com
     * mensagem de contrato, em vez de aceitar uma resposta parcial ou quebrar ao
     * preencher os campos seguintes.</p>
     */
    private void validaSimulatedDemandPlanDTOSimulacaoCommunity(
            SimulatedDemandPlanDTO simulatedDemandPlanDTO) {

        if (simulatedDemandPlanDTO == null) {
            throw new IllegalStateException(
                    "Demand Planning forecast simulation mapper result is required.");
        }

    }

    public void saveParametrosDemandPlanning(
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {

        /*
         * O save Community deve bloquear parametros Enterprise antes de buscar
         * projection, perfil ou parametros existentes. O mapper e o dono do
         * contrato de configuracao, entao a regra fica centralizada nele.
         */
        demandPlanningConfigurationMapper.validaDemandPlanningClusterLevelConfigurationDTOCommunity(
                demandPlanningClusterLevelConfigurationDTO);
        demandPlanningConfigurationMapper.validaIdentidadeDemandPlanningClusterLevelConfigurationDTOCommunity(
                demandPlanningClusterLevelConfigurationDTO);

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                perfilExecucaoDemandPlanRepository.findById(
                        demandPlanningClusterLevelConfigurationDTO.demandPlanExecutionProfileId).get();
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning = clusterEParametrosProjection.getClusterMateriaisDemandPlanningDeId(
                        demandPlanningClusterLevelConfigurationDTO.materialClusterId)
                .orElseThrow(() -> new NoResultException(
                        "Demand Planning Material Cluster "
                                + demandPlanningClusterLevelConfigurationDTO.materialClusterId
                                + " not found in parameter projection"));
        ClusterLocations clusterLocations = clusterEParametrosProjection.getClusterLocationsDeId(
                demandPlanningClusterLevelConfigurationDTO.locationClusterId);

        // extrai a versão atual dos parâmetros do banco de dados
        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster =
                getParametrosDemandPlanNivelClusterExistenteOuNovoCommunity(
                        perfilExecucaoDemandPlan,
                        clusterMateriaisDemandPlanning,
                        clusterLocations);

        // atualiza a entidade com os valores passados via DTO
        demandPlanningConfigurationMapper.atualizaEntidadeParametrosComDTO(
                parametrosDemandPlanNivelCluster,
                demandPlanningClusterLevelConfigurationDTO);
        /*
         * Salva a entidade com os parametros atualizados e valida a fotografia
         * retornada pelo repository. A configuracao cluster-level e chaveada
         * por perfil, cluster material e cluster location; se qualquer ponta da
         * chave voltar sem id, o cache/projection de Demand Planning ficaria
         * inconsistente para a proxima simulacao ou rodada.
         */
        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelClusterSalvos =
                parametrosDemandPlanNivelClusterRepository.save(parametrosDemandPlanNivelCluster);
        validaParametrosDemandPlanNivelClusterSalvosCommunity(parametrosDemandPlanNivelClusterSalvos);
        
        /*
         * Support series/regression time series sao Enterprise. O mapper
         * valida e rejeita qualquer tentativa de salvar selecao externa antes
         * deste ponto; Community persiste apenas os parametros cluster-level.
         */
        
    }

    /**
     * Valida a fotografia salva dos parametros cluster-level de Demand Planning.
     *
     * <p>Esta chave composta e o contrato minimo entre configuracao, simulacao,
     * projection de forecast e execucao real. Retorno nulo ou sem uma das tres
     * pontas funcionais indica falha de persistencia e deve ser detectado antes
     * de a tela assumir que a configuracao foi aplicada.</p>
     */
    private void validaParametrosDemandPlanNivelClusterSalvosCommunity(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelClusterSalvos) {

        if (parametrosDemandPlanNivelClusterSalvos == null) {
            throw new IllegalStateException(
                    "Saved Demand Planning cluster-level parameter snapshot is required.");
        }
        if (parametrosDemandPlanNivelClusterSalvos.getParametrosDemandPlanNivelClusterCompositeKey() == null) {
            throw new IllegalStateException(
                    "Saved Demand Planning cluster-level parameter key is required.");
        }
        if (parametrosDemandPlanNivelClusterSalvos.getPerfilExecucaoDemandPlan() == null
                || parametrosDemandPlanNivelClusterSalvos.getPerfilExecucaoDemandPlan().getId() == null
                || parametrosDemandPlanNivelClusterSalvos.getPerfilExecucaoDemandPlan().getId().isBlank()) {
            throw new IllegalStateException(
                    "Saved Demand Planning cluster-level execution profile id is required.");
        }
        if (parametrosDemandPlanNivelClusterSalvos.getClusterMateriaisDemandPlanning() == null
                || parametrosDemandPlanNivelClusterSalvos.getClusterMateriaisDemandPlanning().getId() == null) {
            throw new IllegalStateException(
                    "Saved Demand Planning cluster-level material cluster id is required.");
        }
        if (parametrosDemandPlanNivelClusterSalvos.getClusterLocations() == null
                || parametrosDemandPlanNivelClusterSalvos.getClusterLocations().getId() == null) {
            throw new IllegalStateException(
                    "Saved Demand Planning cluster-level location cluster id is required.");
        }

    }

    /**
     * Valida a projection estrutural usada pela simulacao e pelo save
     * cluster-level antes de qualquer lookup interno.
     *
     * <p>O cache de parametros e a fotografia base da configuracao
     * Community. Se ele nao existir ou vier sem parametros globais, o problema
     * e de bootstrap/snapshot, nao de id de cluster. Falhar aqui evita NPE
     * tardio em chamadas como calendario, sales projection ou mapper de
     * clusters.</p>
     */
    /**
     * Valida a projection de conversao de unidades usada pela simulacao antes
     * de entregar o snapshot para a factory de sales.
     *
     * <p>Mesmo quando a configuracao Community nao habilita recursos
     * Enterprise, o historico precisa ser convertido para a unidade de Demand
     * Planning configurada. A ausencia da projection ou dos parametros globais
     * indica fotografia estrutural incompleta e deve falhar antes do acesso a
     * vendas.</p>
     */
    /**
     * Valida a projection de vendas historicas materializada para a simulacao.
     *
     * <p>A mesma projection alimenta o forecast Community e o mapper da resposta
     * visual. Se a factory devolver `null`, isso e falha estrutural da borda de
     * dados historicos, nao ausencia de vendas. Snapshot sem vendas deve ser
     * representado por uma projection vazia, mantendo calendario e conversoes
     * disponiveis para o restante da simulacao.</p>
     */
    /**
     * Resolve o cluster de materiais dentro da projection de parametros usada
     * pelo save da configuracao cluster-level.
     *
     * <p>O DTO ja foi validado contra recursos Enterprise, entao a ausencia do
     * id informado significa referencia funcional invalida na tela de
     * configuracao, nao um `Optional.get()` transicional.</p>
     */

    /**
     * Busca os parametros cluster-level existentes ou cria a entidade nova.
     *
     * <p>`Optional.empty()` e o caso normal de primeiro cadastro do trio
     * perfil/material/location. Um `Optional` nulo do repository nao pode cair
     * nesse fallback, porque esconderia um contrato quebrado como criacao
     * funcional de configuracao.</p>
     */
    private ParametrosDemandPlanNivelCluster getParametrosDemandPlanNivelClusterExistenteOuNovoCommunity(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning,
            ClusterLocations clusterLocations) {

        Optional<ParametrosDemandPlanNivelCluster> optionalParametrosDemandPlanNivelCluster =
                parametrosDemandPlanNivelClusterRepository
                        .findByParametrosClusterProdutosDemandPlanningClusterLocationsCompositeKeyClusterProdutosDemandPlanningAndParametrosClusterProdutosDemandPlanningClusterLocationsCompositeKeyClusterLocations(
                                perfilExecucaoDemandPlan,
                                clusterMateriaisDemandPlanning,
                                clusterLocations);
        if (optionalParametrosDemandPlanNivelCluster == null) {
            throw new IllegalStateException(
                    "Demand Planning cluster-level parameter repository returned null Optional for Community profile "
                            + perfilExecucaoDemandPlan.getId()
                            + ", material cluster "
                            + clusterMateriaisDemandPlanning.getId()
                            + " and location cluster "
                            + clusterLocations.getId()
                            + ".");
        }

        return optionalParametrosDemandPlanNivelCluster.orElse(
                new ParametrosDemandPlanNivelCluster(
                        new ParametrosDemandPlanNivelCluster.ParametrosDemandPlanNivelClusterCompositeKey(
                                perfilExecucaoDemandPlan,
                                clusterMateriaisDemandPlanning,
                                clusterLocations)));

    }

    /**
     * Valida os identificadores recebidos pelo endpoint GET de configuracao.
     *
     * <p>Como essa chamada nao recebe o DTO completo, ela nao passa pelo mapper
     * de bloqueio Enterprise. Ainda assim, ids incompletos precisam falhar
     * antes de repositories para produzir erro funcional claro e evitar que
     * detalhes de implementacao do Spring Data/JPA virem contrato publico.</p>
     */
    private void validaChavesConsultaDemandPlanningConfigurationCommunity(
            String executionProfileId,
            Long locationClusterId,
            Long materialClusterId) {

        if (executionProfileId == null || executionProfileId.isBlank()) {
            throw new IllegalArgumentException(
                    "Demand Planning execution profile id is required");
        }
        if (locationClusterId == null) {
            throw new IllegalArgumentException("Demand Planning location cluster id is required");
        }
        if (materialClusterId == null) {
            throw new IllegalArgumentException("Demand Planning material cluster id is required");
        }

    }
    /**
     * Resolve o cluster de locations dentro da projection de parametros usada
     * pelo save da configuracao cluster-level.
     *
     * <p>O contrato historico retorna `null` quando o id nao existe. Esta borda
     * converte esse detalhe interno em erro funcional explicito para o front e
     * para os logs de configuracao Community.</p>
     */

}
