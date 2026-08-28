package com.opsfactor.community.capability.supplyplanning.supplyplan.facade;

import com.google.common.collect.ImmutableMap;
import com.opsfactor.community.capability.planningbook.facade.dto.CellDetailsDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionCompleto;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardSupplyPlanning;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionFactory;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionPorDfu;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.platform.bi.AgregacaoDFU;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.capability.configuration.facade.ConfiguredViewService;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.platform.exception.SupplyPlanException;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.mapper.SupplyPlanAutoMapper;
import com.opsfactor.community.capability.supplyplanning.supplyplan.service.SupplyPlanningModificacoesService;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanExecutionProfileMaterialScopeSpi;
import com.opsfactor.community.capability.planningbook.facade.PlanningBookService;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanPeriodDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanSelectDTO;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjectionFactory;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.ReferenciaPeriodo;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import org.apache.commons.compress.utils.Sets;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.NoResultException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fachada Community usada pelo front-end de Supply Planning.
 *
 * <p>Esta classe monta DTOs de selecao, Planning Book e detalhes operacionais
 * do plano heuristico. A colaboracao aberta fica limitada a material/location,
 * KFs padrao e dados transacionais do proprio Supply Plan. Otimizador,
 * process chain, upload de ajustes, funcao objetivo, custos, fleets,
 * line scheduling e Constraint Tracker pertencem ao OpsFactor Enterprise e
 * devem permanecer fora desta fachada.</p>
 */

@Service
public class SupplyPlanFacade {

    /**
     * Service generico de montagem do Planning Book. Recebe projections de KFs
     * ja filtradas para o recorte Community.
     */
    @Autowired
    private PlanningBookService planningBookService;

    /**
     * Service de User Views usado para resolver filtros e selecoes
     * material/location.
     */
    @Autowired
    private ConfiguredViewService configuredViewService;

    /**
     * Service principal de Supply Planning, usado para listas e acesso ao plano
     * heuristico salvo.
     */
    @Autowired
    private SupplyPlanService supplyPlanService;

    /** Optional Enterprise scope used by a profile-backed Supply Planning Book. */
    @Autowired(required = false)
    private SupplyPlanExecutionProfileMaterialScopeSpi supplyPlanExecutionProfileMaterialScope;

    /**
     * Service de persistencia de modificacoes manuais do Planning Book Supply.
     */
    @Autowired
    private SupplyPlanningModificacoesService supplyPlanningModificacoesService;

    /**
     * Service de parametros globais usado para interpretar defaults de calendario
     * e exibicao.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Factory da malha operacional usada para detalhes e validacoes de Supply
     * Planning Book.
     */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /**
     * Factory da projection de User View filtrada.
     */
    @Autowired
    private ConfiguredViewProjectionFactory configuredViewProjectionFactory;

    /**
     * Factory de UOM/conversoes usada em detalhes e ajustes.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory conversaoUnidadeMedidaProjectionFactory;

    /**
     * Factory da projection de Demand Plan usada para calcular demanda direta
     * considerada em detalhes de Supply Planning.
     */
    @Autowired
    private DemandPlanProjectionFactory demandPlanProjectionFactory;

    /**
     * Factory da projection de Supply Plan usada para Planning Book e detalhes.
     */
    @Autowired
    private SupplyPlanProjectionFactory supplyPlanProjectionFactory;

    /**
     * Factory da projection global de parametros, clusters e master data.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Factory de split temporal operacional. Curvas configuraveis Enterprise
     * continuam bloqueadas no perfil.
     */
    @Autowired
    private SplitTemporalProjectionFactory splitTemporalProjectionFactory;

    /**
     * Factory de safety stock operacional. Otimizacao de politica de estoques
     * continua fora do Community.
     */
    @Autowired
    private PoliticaEstoquesProjectionFactory politicaEstoquesProjectionFactory;

    /**
     * Factory das Key Figures publicadas no Planning Book Supply Community.
     */
    @Autowired
    private KeyFigureProjectionFactory keyFigureProjectionFactory;

    /**
     * Repository de Supply Plan usado para carregar versoes e metadados do
     * plano.
     */
    @Autowired
    private SupplyPlanRepository supplyPlanRepository;

    /**
     * Repository de locations usado nos seletores operacionais.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Mapper de entidade Supply Plan para DTOs do front.
     */
    @Autowired
    private SupplyPlanAutoMapper supplyPlanAutoMapper;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Retorna um DTO com os valores dos drop-downs de seleção do Supply Planning
     * Todas as locations onde parametrosLocation.planejaSupply = true são listadas
     * @return
     */
    public SupplyPlanSelectDTO getSupplyPlanningSelectDTO() {

        // extrai objetos que serão incorporados ao DTO
        List<Location> locationListCarregada = locationRepository.customFindAllWithParametros();
        validaLocationListCarregadaParaSeletorSupplyPlanningCommunity(locationListCarregada);
        List<Location> locationList = locationListCarregada.stream()
                .filter(x -> x.getPlanejaSupply())
                .collect(Collectors.toList());

        List<SupplyPlan> supplyPlanList = supplyPlanRepository.customFindAllForSelector();
        validaSupplyPlanListCarregadaParaSeletorSupplyPlanningCommunity(supplyPlanList);

        // incorpora objetos no DTO
        SupplyPlanSelectDTO supplyPlanningSelectDTO = new SupplyPlanSelectDTO();
        supplyPlanningSelectDTO.addLocationList(locationList);
        supplyPlanningSelectDTO.addSupplyPlanList(supplyPlanList);

        return supplyPlanningSelectDTO;

    }

    /**
     * Valida locations carregadas para o seletor legado de Supply Planning.
     *
     * <p>Lista vazia e valida. Lista nula, item nulo ou location sem id indica
     * quebra de snapshot antes do filtro `planejaSupply` e antes da ordenacao
     * do DTO por id.</p>
     */
    private void validaLocationListCarregadaParaSeletorSupplyPlanningCommunity(
            List<Location> locationList) {

        if (locationList == null) {
            throw new IllegalStateException(
                    "Location repository returned null list for Supply Planning selector.");
        }

        for (int index = 0; index < locationList.size(); index++) {
            Location location = locationList.get(index);
            if (location == null) {
                throw new IllegalStateException(
                        "Location repository returned null item at index "
                                + index
                                + " for Supply Planning selector.");
            }
            if (location.getId() == null || location.getId().isBlank()) {
                throw new IllegalStateException(
                        "Location repository returned item without id at index "
                                + index
                                + " for Supply Planning selector.");
            }
        }

    }

    /**
     * Valida Supply Plans antes de montar as versoes do seletor.
     *
     * <p>O DTO de versao navega por Demand Plan, perfil de execucao e malha.
     * Falhar aqui deixa a quebra de snapshot explicita e evita NPE tardio dentro
     * do construtor de `VersaoSupplyPlanDTO`.</p>
     */
    private void validaSupplyPlanListCarregadaParaSeletorSupplyPlanningCommunity(
            List<SupplyPlan> supplyPlanList) {

        if (supplyPlanList == null) {
            throw new IllegalStateException(
                    "Supply Plan repository returned null list for Supply Planning selector.");
        }

        for (int index = 0; index < supplyPlanList.size(); index++) {
            SupplyPlan supplyPlan = supplyPlanList.get(index);
            if (supplyPlan == null) {
                throw new IllegalStateException(
                        "Supply Plan repository returned null item at index "
                                + index
                                + " for Supply Planning selector.");
            }
            if (supplyPlan.getId() == null) {
                throw new IllegalStateException(
                        "Supply Plan repository returned item without id at index "
                                + index
                                + " for Supply Planning selector.");
            }
            if (supplyPlan.getDemandPlan() == null || supplyPlan.getDemandPlan().getId() == null) {
                throw new IllegalStateException(
                        "Supply Plan repository returned item without Demand Plan id at index "
                                + index
                                + " for Supply Planning selector.");
            }
            if (supplyPlan.getPerfilExecucaoSupplyPlan() == null
                    || supplyPlan.getPerfilExecucaoSupplyPlan().getId() == null
                    || supplyPlan.getPerfilExecucaoSupplyPlan().getId().isBlank()) {
                throw new IllegalStateException(
                        "Supply Plan repository returned item without execution profile id at index "
                                + index
                                + " for Supply Planning selector.");
            }
            if (supplyPlan.getVersaoMalha() == null
                    || supplyPlan.getVersaoMalha().getId() == null
                    || supplyPlan.getVersaoMalha().getId().isBlank()) {
                throw new IllegalStateException(
                        "Supply Plan repository returned item without supply network version id at index "
                                + index
                                + " for Supply Planning selector.");
            }
        }

    }

    /*
     * Planning Book e seletores de Supply Planning Community.
     *
     * Os metodos abaixo alimentam o front compartilhado com versoes de Supply
     * Plan, periodos operacionais e grids em material/location. Qualquer
     * colaboracao agregada, custom KF ou ajuste dependente de processo
     * otimizado precisa ser bloqueado antes de tocar nas projections.
     */

    /**
     * Método usado para se extrair lista supply plans a serem exibidas como seleção
     * nos planning books de supply/production planning, na tela de data download (SNP)
     * e na tela de plano consolidado
     * @return
     */
    public List<SupplyPlanDTO> getSupplyPlanDTOList() {
        List<SupplyPlanDTO> supplyPlanDTOList =
                supplyPlanAutoMapper.converteLista(supplyPlanService.getSupplyPlanList());
        validaSupplyPlanDTOListCarregadaParaListagemCommunity(supplyPlanDTOList);
        return supplyPlanDTOList;
    }

    /**
     * Valida o resultado resumido do mapper antes de devolver a listagem.
     *
     * <p>`SupplyPlanService#getSupplyPlanList()` ja valida a fotografia de
     * entidades. Esta guarda cobre a segunda fronteira: regressao do mapper nao
     * pode devolver lista nula, item nulo ou DTO sem `supplyPlanId` para o
     * front compartilhado.</p>
     */
    private void validaSupplyPlanDTOListCarregadaParaListagemCommunity(
            List<SupplyPlanDTO> supplyPlanDTOList) {

        if (supplyPlanDTOList == null) {
            throw new IllegalStateException(
                    "Supply Plan mapper returned null DTO list for Supply Planning DTO listing.");
        }

        for (int index = 0; index < supplyPlanDTOList.size(); index++) {
            SupplyPlanDTO supplyPlanDTO = supplyPlanDTOList.get(index);
            if (supplyPlanDTO == null) {
                throw new IllegalStateException(
                        "Supply Plan mapper returned null DTO at index "
                                + index
                                + " for Supply Planning DTO listing.");
            }
            if (supplyPlanDTO.supplyPlanId == null) {
                throw new IllegalStateException(
                        "Supply Plan mapper returned DTO without supplyPlanId at index "
                                + index
                                + " for Supply Planning DTO listing.");
            }
        }

    }

    /**
     * Retorna os períodos futuros do calendário operacional de um Supply Plan.
     * O range retornado deve ser usado pelo backend para filtrar linhas de resultado,
     * pois a data gravada pode ser o início ou o fim do bucket dependendo da origem do dado.
     */
    public List<SupplyPlanPeriodDTO> getSupplyPlanPeriodDTOList(Long supplyPlanId) {

        SupplyPlan supplyPlan = supplyPlanRepository.findById(supplyPlanId).get();
        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        Calendario calendario = supplyPlan.getCalendarioDoSupplyPlan(parametrosGlobais);
        List<SupplyPlanPeriodDTO> supplyPlanPeriodDTOList = new ArrayList<>();

        for (int periodIndex = calendario.getPosicaoPeriodoPresente();
             periodIndex <= calendario.getPosicaoPeriodoFinalFuturo();
             periodIndex++) {

            supplyPlanPeriodDTOList.add(new SupplyPlanPeriodDTO(
                    periodIndex,
                    calendario.getDescricaoPeriodoDePosicaoPeriodo(periodIndex),
                    calendario.getTamanhoBucket(),
                    calendario.getPrimeiraDataHorarioPeriodo(periodIndex),
                    calendario.getPrimeiraDataHorarioPeriodo(periodIndex),
                    calendario.getUltimaDataHorarioPeriodo(periodIndex)));

        }

        return supplyPlanPeriodDTOList;

    }

    /**
     * Valida parametros globais consumidos diretamente pela fachada.
     *
     * <p>Nas demais rotas o snapshot de parametros pode chegar dentro de
     * projections. A lista de periodos usa o service diretamente, entao precisa
     * da mesma falha explicita antes de montar calendario.</p>
     */
    /**
     * Carrega um Supply Plan obrigatorio para operacoes de front.
     *
     * <p>As telas Community de Supply Planning sempre dependem de uma versao
     * de plano materializada: periodos, Planning Book, detalhes e ajustes nao
     * possuem comportamento de no-op quando o id informado nao existe. O
     * contexto da chamada e incluido na mensagem para facilitar diagnostico de
     * payloads manuais, links antigos ou estado stale da SPA.</p>
     */
    

    /**
     * Valida a identidade do Supply Plan carregado para operacoes de front.
     *
     * <p>O id solicitado e parte do contrato da chamada. Um repository que
     * devolve snapshot sem id ou com id divergente deve falhar antes de
     * calendario, Planning Book, detalhes ou projections.</p>
     */
    /**
     * Recupera uma location obrigatoria para operacoes do Planning Book Supply.
     *
     * <p>A location selecionada define a grade material/location do Community.
     * Se ela nao existir, a chamada falha como erro funcional de Supply
     * Planning, sem expor excecao checked do legado.</p>
     */
    

    /**
     * Valida a identidade da Location carregada para o Planning Book Supply.
     */
    /**
     * Resolve a projection central de parametros/master data exigida pela
     * fachada Community de Supply Planning.
     *
     * <p>O Planning Book e seus detalhes usam esta fotografia para filtrar
     * material/location, calcular calendario, resolver unidade de medida da
     * view e montar projections derivadas. Projection ou parametros globais
     * ausentes indicam falha de montagem do snapshot, nao ausencia de feature
     * Enterprise; por isso a chamada falha aqui, antes de view/projection
     * downstream.</p>
     */
    /**
     * Resolve a projection de conversao de unidades usada por detalhes e
     * ajustes do Planning Book.
     *
     * <p>Mesmo no Community, quantidades podem ser exibidas/editadas em uma UOM
     * de view. A projection precisa existir e carregar parametros globais para
     * que conversoes e unidades padrao falhem com mensagem funcional, nao como
     * NPE dentro de BOM, roteiro ou projection de plano.</p>
     */
    /**
     * Resolve a projection de malha operacional usada pelas telas Community.
     *
     * <p>A malha alimenta ressuprimento, detalhes inbound/producao e
     * `SupplyPlanningProjection`. Ela tambem carrega snapshots de parametros e
     * UOM usados por metodos internos da projection; validar tudo na fronteira
     * evita que uma falha de cache apareca como erro tecnico em stream ou
     * recursao de detalhe.</p>
     */
    /**
     * Resolve a projection operacional de politica de estoques.
     *
     * <p>Safety stock e inventory plan operacional fazem parte do Community.
     * Projection nula e falha de montagem do snapshot fisico e deve parar
     * antes de updates/detalhes do Planning Book.</p>
     */
    public PlanningBookDTO getPlanningBookDTO(ConfiguredViewSelectionDTO configuredViewSelectionDTO, String userId) {

        Long supplyPlanId = validaConfiguredViewSelectionSupplyPlanningBookCommunity(
                configuredViewSelectionDTO);

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        Location location = locationRepository.findById(configuredViewSelectionDTO.locationId).get();

        ConfiguredViewProjection configuredViewProjection = configuredViewProjectionFactory.getConfiguredViewProjection(
                userId,
                configuredViewSelectionDTO.viewName,
                ConfiguredView.TipoView.SUPPLYPLANNINGBOOK,
                clusterEParametrosProjection,
                (materialIterado, locationIterada) -> locationIterada.equals(location));

        validaConfiguredViewProjectionSupplyPlanningBookCommunity(
                configuredViewProjection,
                location,
                configuredViewSelectionDTO);

        SupplyPlan supplyPlan = supplyPlanRepository.findById(supplyPlanId).get();

        KeyFigureProjection keyFigureProjection = keyFigureProjectionFactory.getKeyFigureProjectionDeSupplyPlan(
                supplyPlan,
                location,
                configuredViewProjection,
                true);

        return planningBookService.getPlanningBookDTO(keyFigureProjection);

    }

    /**
     * Valida o payload de abertura do Supply Planning Book antes de qualquer
     * cache, repository ou projection.
     *
     * <p>O Community precisa de plano, view e location explicitos para montar
     * a grade material/location do plano heuristico. Payloads incompletos nao
     * devem chegar a `Long.valueOf(...)`, `findById(...)` ou factories de
     * projection, porque isso mascara o erro funcional como falha tecnica.</p>
     */
    private Long validaConfiguredViewSelectionSupplyPlanningBookCommunity(
            ConfiguredViewSelectionDTO configuredViewSelectionDTO) {

        if (configuredViewSelectionDTO == null) {
            throw new IllegalArgumentException("Supply Planning Book view selection is required");
        }

        if (configuredViewSelectionDTO.planId == null || configuredViewSelectionDTO.planId.isBlank()) {
            throw new IllegalArgumentException("Supply Planning Book plan id is required");
        }

        if (configuredViewSelectionDTO.viewName == null || configuredViewSelectionDTO.viewName.isBlank()) {
            throw new IllegalArgumentException("Supply Planning Book view name is required");
        }

        if (configuredViewSelectionDTO.locationId == null || configuredViewSelectionDTO.locationId.isBlank()) {
            throw new IllegalArgumentException("Supply Planning Book location id is required");
        }

        try {
            return Long.valueOf(configuredViewSelectionDTO.planId);
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException(
                    "Supply Planning Book plan id must be numeric: " + configuredViewSelectionDTO.planId,
                    numberFormatException);
        }

    }

    /**
     * Valida o escopo material/location resolvido para o Planning Book Supply
     * antes de montar KeyFigureProjection.
     *
     * <p>No Community o Planning Book nao aceita agrupamento, ajuste agregado
     * ou fallback para escopo vazio. Se a User View nao contiver a location
     * selecionada ou nao produzir nenhum material, seguir adiante montaria uma
     * grade vazia e esconderia um erro de configuracao da view. Por isso a
     * validacao falha imediatamente, ainda antes de carregar o Supply Plan e
     * antes de instanciar projections de KFs.</p>
     */
    private void validaConfiguredViewProjectionSupplyPlanningBookCommunity(
            ConfiguredViewProjection configuredViewProjection,
            Location location,
            ConfiguredViewSelectionDTO configuredViewSelectionDTO) {

        if (configuredViewProjection == null) {
            throw new IllegalArgumentException(
                    "Supply Planning Book opening requires configured view projection");
        }

        /*
         * A projection de User View encapsula o escopo material/location
         * efetivamente publicado para o Planning Book Community. Validamos o
         * snapshot antes dos getters derivados porque eles delegam para
         * FiltroDFUProjection; se a factory retornar uma projection parcial,
         * isso deve aparecer como contrato quebrado da view, nao como NPE.
         */
        if (configuredViewProjection.getDfuProjectionFiltrado() == null) {
            throw new IllegalArgumentException(
                    "Supply Planning Book opening requires DFU projection in configured view projection");
        }
        if (configuredViewProjection.getLocationsFiltradas() == null) {
            throw new IllegalArgumentException(
                    "Supply Planning Book opening requires filtered locations in configured view projection");
        }
        if (configuredViewProjection.getMateriaisFiltrados() == null) {
            throw new IllegalArgumentException(
                    "Supply Planning Book opening requires filtered materials in configured view projection");
        }

        validaLocationAcessivelSupplyPlanningBookCommunity(
                configuredViewProjection,
                location,
                configuredViewSelectionDTO.locationId,
                configuredViewSelectionDTO.viewName,
                "Supply Planning Book opening");

        if (configuredViewProjection.getMateriaisFiltrados().isEmpty()) {
            throw new SupplyPlanException(
                    "No materials conform to the filters. Please review the filters in the Admin -> User Data View menu");
        }

    }

    /**
     * Resolve o nome da view para mensagens funcionais do Planning Book.
     *
     * <p>A projection normal deve carregar a entidade `ConfiguredView`, mas
     * testes, payloads stale ou snapshots incompletos podem chegar sem ela. A
     * validacao de acesso por location nao deve mascarar esse caso com NPE:
     * usamos o nome solicitado no DTO como fallback diagnostico.</p>
     */
    private String getNomeViewSupplyPlanningBookCommunity(
            ConfiguredViewProjection configuredViewProjection,
            ConfiguredViewSelectionDTO configuredViewSelectionDTO) {

        return getNomeViewSupplyPlanningBookCommunity(
                configuredViewProjection,
                configuredViewSelectionDTO.viewName);

    }

    /**
     * Resolve o nome da view para caminhos que nao usam
     * {@link ConfiguredViewSelectionDTO}.
     *
     * <p>Os endpoints de ajuste e detalhe recebem DTOs diferentes, mas a
     * mensagem funcional deve continuar estavel: usar o nome da entidade quando
     * a projection estiver completa e cair para o nome solicitado quando um
     * snapshot de teste, payload stale ou projection parcial nao trouxer
     * `ConfiguredView`.</p>
     */
    private String getNomeViewSupplyPlanningBookCommunity(
            ConfiguredViewProjection configuredViewProjection,
            String viewNameFallback) {

        if (configuredViewProjection.getConfiguredView() != null
                && configuredViewProjection.getConfiguredView().getNomeView() != null) {
            return configuredViewProjection.getConfiguredView().getNomeView();
        }

        return viewNameFallback;

    }

    /**
     * Valida que a location selecionada pertence ao escopo da User View.
     *
     * <p>Location fora da view e erro funcional de selecao/acesso do Planning
     * Book, nao erro fatal da JVM. Por isso o metodo devolve
     * {@link IllegalArgumentException} com mensagem estavel para o front e para
     * testes, mantendo falhas estruturais da projection separadas de payloads
     * apenas inacessiveis.</p>
     */
    private void validaLocationAcessivelSupplyPlanningBookCommunity(
            ConfiguredViewProjection configuredViewProjection,
            Location location,
            String locationId,
            String viewNameFallback,
            String contexto) {

        if (configuredViewProjection == null) {
            throw new IllegalArgumentException(
                    contexto + " requires configured view projection");
        }
        if (configuredViewProjection.getLocationsFiltradas() == null) {
            throw new IllegalArgumentException(
                    contexto + " requires filtered locations in configured view projection");
        }

        if (!configuredViewProjection.getLocationsFiltradas().contains(location)) {
            throw new IllegalArgumentException(
                    "Location " + locationId
                            + " not accessible for view "
                            + getNomeViewSupplyPlanningBookCommunity(
                                    configuredViewProjection,
                                    viewNameFallback));
        }

    }

    /**
     * Método para processar qualquer tipo de ajuste realizado em um planning book supply/produção
     * Qualquer linha é processada através deste método : informação da linha
     * em selectedPlanningBookCellDTO.keyFigure : ESTOQUE, INBOUND_PLANEJADO ou PRODUCAO_PLANEJADA
     *
     * <p>Falhas de UOM, calendario incompatível ou payload fora do recorte
     * Community sao runtime funcionais. A assinatura fica sem checked exception
     * para que controllers traduzam apenas o limite HTTP, sem capturar
     * `Exception` generico legado.</p>
     *
     * @return Planning Book atualizado apos persistencia e recarga das
     * projections Community.
     */
    public PlanningBookDTO modificaSupplyPlan(List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs, String userId) {

        KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanning = validaSelecaoModificacaoSupplyPlanningBookCommunity(
                selectedPlanningBookCellDTOs);
        SelectedPlanningBookCellDTO selectedPlanningBookCellDTO = selectedPlanningBookCellDTOs.get(0);

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        SupplyPlan supplyPlan = supplyPlanRepository.findById(selectedPlanningBookCellDTO.planId).get();
        VersaoMalha versaoMalha = supplyPlan.getVersaoMalha();

        // gera view projection, incluindo todas as locations possíveis para a view
        // ao se gerar o planning book DTO se deverão filtrar somente dados da location selecionada

        ConfiguredViewProjection configuredViewProjectionComValoresAAtualizar = configuredViewService.getConfiguredViewProjectionComDTOSelecaoCelulas(
                userId,
                selectedPlanningBookCellDTO.viewName,
                ConfiguredView.TipoView.SUPPLYPLANNINGBOOK,
                supplyPlan.getTamanhoBucket(),
                selectedPlanningBookCellDTOs,
                clusterEParametrosProjection,
                null);

        String locationId = selectedPlanningBookCellDTO.locationId;
        Location location = locationRepository.findById(locationId).get();
        /*
         * Cria copias independentes porque o conjunto editavel sera filtrado
         * por ressuprimento logo abaixo, enquanto a projection ainda precisa
         * carregar o escopo DFU original da selecao.
         */
        Set<Produto> materiaisProjectionAAtualizar = new HashSet<>(configuredViewProjectionComValoresAAtualizar.getMateriaisAAtualizar());
        Set<Produto> materiaisAAtualizar = new HashSet<>(materiaisProjectionAAtualizar);
        Set<Produto> materiaisView = new HashSet(configuredViewProjectionComValoresAAtualizar.getMateriaisFiltrados());

        validaLocationAcessivelSupplyPlanningBookCommunity(
                configuredViewProjectionComValoresAAtualizar,
                location,
                locationId,
                selectedPlanningBookCellDTO.viewName,
                "Supply Planning Book adjustment");
        // Usa o perfil de execucao raiz do Supply Plan Community.
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlan.getPerfilExecucaoSupplyPlan();
        MaterialProjection materialProjection = getMaterialProjectionComEscopoEnterprise(
                perfilExecucaoSupplyPlan,
                clusterEParametrosProjection,
                MaterialProjectionFactory.getMaterialProjectionDePerfilExecucaoSupplyPlan(
                        perfilExecucaoSupplyPlan,
                        clusterEParametrosProjection));
        LocationProjection locationProjection = LocationProjectionFactory.getLocationProjectionDePerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan, clusterEParametrosProjection);

        /*
         * A selecao da view e independente do perfil de execucao. Antes de
         * reconstruir DFUs ou modificar linhas, intersectamos cada conjunto
         * derivado com o mesmo escopo efetivo que chega ao motor Supply.
         */
        Set<Produto> materiaisEscopoEfetivo = materialProjection.getMaterialSet();
        materiaisProjectionAAtualizar.retainAll(materiaisEscopoEfetivo);
        materiaisAAtualizar.retainAll(materiaisEscopoEfetivo);
        materiaisView.retainAll(materiaisEscopoEfetivo);

        // O refresh do planning book de Supply usa somente a location destino
        // e os materiais ainda elegiveis pelo perfil. Origins inbound seguem
        // nas detail lines do snapshot reconstruido.
        configuredViewProjectionComValoresAAtualizar.getDfuProjectionFiltrado().aplicaFiltroDFUs(
                AgregacaoDFU.getDFUListDeProdutoCartesianoLocationMaterial(
                        Sets.newHashSet(location),
                        materiaisView,
                        clusterEParametrosProjection));

        DemandPlan demandPlan = supplyPlan.getDemandPlan();

        PoliticaEstoquesProjection politicaEstoquesProjection = politicaEstoquesProjectionFactory.getPoliticaEstoquesProjection(
                        supplyPlan.getCalendarioDoSupplyPlan(clusterEParametrosProjection.getParametrosGlobais()),
                        clusterEParametrosProjection,
                        perfilExecucaoSupplyPlan);

        UnidadeMedidaProjection conversaoUnidadeMedidaProjection = conversaoUnidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();

        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();

        // evita que materiais sem possibilidade de ressuprimento sejam modificados

        // Community nao possui line scheduling nem roteiros paralelos; o Planning
        // Book deve filtrar e exibir apenas a cadeia prioritaria do heuristico.
        boolean consideraVersoesProducaoParalelas = false;
        materiaisAAtualizar = materiaisAAtualizar.stream()
                .filter(material -> !supplyNetworkProjection.getTipoRessuprimento(
                        versaoMalha,
                        location,
                        material,
                        supplyPlan.getDataInicioPlano(),
                        consideraVersoesProducaoParalelas,
                        locationProjection.getLocationsAtivasOuNuloSeLocationProjectionCompleto(),
                        materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                        .equals(Constantes.SNPOrigemReabastecimento.SEM_RESSUPRIMENTO))
                .collect(Collectors.toSet());

        UnidadeMedida unidadeMedidaView = configuredViewProjectionComValoresAAtualizar.getUnidadeMedidaView(parametrosGlobais);

        SupplyPlanningProjection supplyPlanningProjection = supplyPlanProjectionFactory.getSupplyPlanningProjectionCompleto(
                supplyPlan,
                perfilExecucaoSupplyPlan, // considera o perfil raiz do supply plan Community
                location,
                supplyNetworkProjection,
                politicaEstoquesProjection,
                MaterialProjectionFactory.getProjectionSetMateriais(materiaisProjectionAAtualizar, clusterEParametrosProjection),
                locationProjection);

        if (!materiaisAAtualizar.isEmpty()) {
            supplyPlanningModificacoesService.modificaSupplyPlan(
                    keyFigureStandardSupplyPlanning.getTipoPlano(),
                    keyFigureStandardSupplyPlanning.getKeyFigureStandardEnum(),
                    selectedPlanningBookCellDTO.newValue,
                    selectedPlanningBookCellDTO.oldValue,
                    unidadeMedidaView,
                    supplyPlanningProjection,
                    supplyPlanningProjection.getCalendario().getPosicaoPeriodo(selectedPlanningBookCellDTO.period),
                    materiaisAAtualizar);

        }

        // Apos a persistencia, a resposta e reconstruida por uma KeyFigureProjection nova para refletir
        // exatamente o estado salvo. Ha custo extra de montagem das projections, mas evita devolver ao
        // front uma visao parcialmente atualizada do Planning Book.
        KeyFigureProjection keyFigureProjection = keyFigureProjectionFactory.getKeyFigureProjectionDeSupplyPlan(
                supplyPlan,
                location,
                configuredViewProjectionComValoresAAtualizar,
                true);

        return planningBookService.getPlanningBookDTO(keyFigureProjection);

    }

    /**
     * Carrega os detalhes de uma celula do Supply Planning Book Community.
     *
     * <p>O controller nao deve instanciar `KeyFigureStandardSupplyPlanning`
     * diretamente, porque essa classe conhece apenas o subconjunto publicavel
     * no Community e lanca `IllegalArgumentException` para KFs Enterprise. A
     * fronteira funcional fica neste service para que payloads Enterprise
     * conhecidos retornem `RequiresEnterpriseVersionException` de forma
     * consistente com os demais bloqueios da edicao aberta.</p>
     */
    public CellDetailsDTO getDetalhesSupplyPlanningBook(
            SelectedPlanningBookCellDTO selectedPlanningBookCellDTO,
            String userId) throws UnitOfMeasureConversionException {

        if (selectedPlanningBookCellDTO == null) {
            throw new IllegalArgumentException("Supply Planning Book cell details selection is required");
        }
        if (selectedPlanningBookCellDTO.keyFigure == null || selectedPlanningBookCellDTO.keyFigure.isBlank()) {
            throw new IllegalArgumentException("Supply Planning Book cell details key figure is required");
        }

        KeyFigureStandardEnum keyFigureStandardEnum =
                resolveKeyFigureStandardEnumDetalheSupplyPlanningBookCommunity(
                        selectedPlanningBookCellDTO.keyFigure);
        

        if (keyFigureStandardEnum.equals(KeyFigureStandardEnum.DEMANDA_INDIRETA_TOTAL)) {
            return getDetalhesDemandaIndiretaSupplyPlanningBook(
                    selectedPlanningBookCellDTO,
                    userId);
        }

        return getDetalhesCelulaSupplyPlanningBook(
                selectedPlanningBookCellDTO,
                userId);

    }

    /**
     * Extrai a lista ag-grid a ser apresentada quando se clica com botão direito em linha 'production' ou 'inbound' no planning book
     * @return
     */
    public CellDetailsDTO getDetalhesDemandaIndiretaSupplyPlanningBook(SelectedPlanningBookCellDTO selectedPlanningBookCellDTO, String userId) throws UnitOfMeasureConversionException {

        if (selectedPlanningBookCellDTO == null) {
            throw new IllegalArgumentException("Supply Planning Book cell details selection is required");
        }
        if (selectedPlanningBookCellDTO.keyFigure == null || selectedPlanningBookCellDTO.keyFigure.isBlank()) {
            throw new IllegalArgumentException("Supply Planning Book cell details key figure is required");
        }

        KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanning =
                getKeyFigureStandardSupplyPlanningBookCommunity(selectedPlanningBookCellDTO.keyFigure);
        if (!keyFigureStandardSupplyPlanning.getKeyFigureStandardEnum().equals(KeyFigureStandardEnum.DEMANDA_INDIRETA_TOTAL)) {
            throw getUnsupportedIndirectDemandDetailsKeyFigureException(keyFigureStandardSupplyPlanning);
        }
        

        Constantes.TipoPlano tipoPlano = keyFigureStandardSupplyPlanning.getTipoPlano();

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        SupplyPlan supplyPlan = supplyPlanRepository.findById(selectedPlanningBookCellDTO.planId).get();

        // gera view projection, incluindo todas as locations possíveis para a view
        // ao se gerar o planning book DTO se deverão filtrar somente dados da location selecionada
        ConfiguredViewProjection configuredViewProjectionComAgrupamentosAAtualizar = configuredViewService.getConfiguredViewProjectionComDTOSelecaoCelulas(
                userId,
                selectedPlanningBookCellDTO.viewName,
                ConfiguredView.TipoView.SUPPLYPLANNINGBOOK,
                supplyPlan.getTamanhoBucket(),
                selectedPlanningBookCellDTO, clusterEParametrosProjection);

        String locationId = selectedPlanningBookCellDTO.locationId;
        Location location = locationRepository.findById(locationId).get();
        // Location fora da view deve falhar como selecao funcional invalida.
        validaLocationAcessivelSupplyPlanningBookCommunity(
                configuredViewProjectionComAgrupamentosAAtualizar,
                location,
                locationId,
                selectedPlanningBookCellDTO.viewName,
                "Supply Planning indirect demand details");

        Set<Produto> materiaisFiltrados = configuredViewProjectionComAgrupamentosAAtualizar.getMateriaisAAtualizar();

        // Usa o perfil de execucao raiz do Supply Plan Community.
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlan.getPerfilExecucaoSupplyPlan();
        LocationProjectionCompleto locationProjectionCompleto = LocationProjectionFactory.getLocationProjectionCompleto(clusterEParametrosProjection);

        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        UnidadeMedida unidadeMedidaView = configuredViewProjectionComAgrupamentosAAtualizar.getUnidadeMedidaView(parametrosGlobais);
        UnidadeMedidaProjection unidadeMedidaProjection = conversaoUnidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();

        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();

        SupplyPlanningProjection supplyPlanningProjection = supplyPlanProjectionFactory.getSupplyPlanningProjectionVazio(
                supplyPlan,
                perfilExecucaoSupplyPlan, // considera o perfil raiz do supply plan Community
                location,
                supplyNetworkProjection,
                null,
                MaterialProjectionFactory.getProjectionSetMateriais(materiaisFiltrados, clusterEParametrosProjection),
                locationProjectionCompleto);
        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComDistributionPlanOutbound(supplyPlanningProjection);
        supplyPlanProjectionFactory.populaSupplyPlanningProjectionComProductionPlanInput(supplyPlanningProjection);

        Calendario calendario = supplyPlanningProjection.getCalendario();
        int posicaoPeriodo = calendario.getPosicaoPeriodo(
                selectedPlanningBookCellDTO.period);

        List<Map<String,Object>> columnDefs = new ArrayList<>();
        columnDefs.add(ImmutableMap.of("headerName", "Material Id", "field", "Material Id", "width", 120, "editable", false));
        columnDefs.add(ImmutableMap.of("headerName", "Destination Location Id", "field", "Destination Location Id", "width", 120, "editable", false));
        columnDefs.add(ImmutableMap.of("headerName", "Delivery Date", "field", "Delivery Date", "width", 90, "editable", false));
        columnDefs.add(ImmutableMap.of("headerName", "Production Version Id", "field", "Production Version Id", "width", 120, "editable", false));
        columnDefs.add(ImmutableMap.of("headerName", "Routing Id", "field", "Routing Id", "width", 120, "editable", false));
        columnDefs.add(ImmutableMap.of("headerName", "Bill of Materials Id", "field", "Bill of Materials Id", "width", 120, "editable", false));
        columnDefs.add(ImmutableMap.of("headerName", "UOM", "field", "UOM", "width", 50, "editable", false));
        columnDefs.add(ImmutableMap.of("headerName", "Planned Quantity", "field", "Planned Quantity", "width", 90, "editable", false));

        CellDetailsDTO cellDetailsDTO = new CellDetailsDTO();
        cellDetailsDTO.locationId = location.getId();
        cellDetailsDTO.planId = supplyPlan.getId();
        cellDetailsDTO.viewName = selectedPlanningBookCellDTO.viewName;
        cellDetailsDTO.keyFigure = keyFigureStandardSupplyPlanning.getKeyFigureStandardEnum();
        cellDetailsDTO.tipoPlano = tipoPlano;
        cellDetailsDTO.period = selectedPlanningBookCellDTO.period;
        cellDetailsDTO.columnDefs = columnDefs;

        for (Produto material : materiaisFiltrados) {

            for (DistributionPlanItem distributionPlanItemDemandaIndireta : supplyPlanningProjection.getDistributionPlanItemOutboundQueueParaLocationsInternas(
                    ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material)) {

                Map<String,Object> detalhes = new HashMap<>();

                detalhes.put("Material Id", material.getId());
                detalhes.put("Destination Location Id", distributionPlanItemDemandaIndireta.getLocationDestino().getId());
                detalhes.put("Delivery Date", distributionPlanItemDemandaIndireta.getDataRecebimento());
                detalhes.put("Production Version Id", null);
                detalhes.put("Routing Id", null);
                detalhes.put("Bill of Materials Id", null);
                detalhes.put("UOM", unidadeMedidaView.getId());
                detalhes.put("Planned Quantity", distributionPlanItemDemandaIndireta.getQuantidade(Constantes.FirmePlanejado.PLANEJADO, tipoPlano));

                cellDetailsDTO.detailLines.add(detalhes);

            }

            for (ProductionPlanLinha productionPlanLinha : supplyPlanningProjection.getProductionPlanLinhaInput(posicaoPeriodo, material)) {

                Map<String,Object> detalhes = new HashMap<>();

                detalhes.put("Material Id", material.getId());
                detalhes.put("Destination Location Id", null);
                detalhes.put("Delivery Date", null);
                detalhes.put("Production Version Id", productionPlanLinha.getVersaoProducaoCadastrada().isVersaoProducaoInexistente() ? null : productionPlanLinha.getVersaoProducaoCadastrada().getId());
                detalhes.put("Routing Id", productionPlanLinha.getRoteiro().getId());
                detalhes.put("Bill of Materials Id", productionPlanLinha.getListaTecnica().getId());
                detalhes.put("UOM", unidadeMedidaView.getId());
                detalhes.put("Planned Quantity", productionPlanLinha.getQuantidadeMaterialInputConsumido(material, Constantes.FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedidaProjection, clusterEParametrosProjection, unidadeMedidaView));

                cellDetailsDTO.detailLines.add(detalhes);

            }

        }

        return cellDetailsDTO;

    }

    /**
     * Extrai a lista ag-grid a ser apresentada quando se clica com botão direito em linha 'production' ou 'inbound' no planning book
     * @return
     */
    public CellDetailsDTO getDetalhesCelulaSupplyPlanningBook(SelectedPlanningBookCellDTO selectedPlanningBookCellDTO, String userId) throws UnitOfMeasureConversionException {

        

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        SupplyPlan supplyPlan = supplyPlanRepository.findById(selectedPlanningBookCellDTO.planId).get();
        VersaoMalha versaoMalha = supplyPlan.getVersaoMalha();

        // gera view projection, incluindo todas as locations possíveis para a view
        // ao se gerar o planning book DTO se deverão filtrar somente dados da location selecionada
        ConfiguredViewProjection configuredViewProjectionComAgrupamentosAAtualizar = configuredViewService.getConfiguredViewProjectionComDTOSelecaoCelulas(
                userId,
                selectedPlanningBookCellDTO.viewName,
                ConfiguredView.TipoView.SUPPLYPLANNINGBOOK,
                supplyPlan.getTamanhoBucket(),
                selectedPlanningBookCellDTO, clusterEParametrosProjection);

        String locationId = selectedPlanningBookCellDTO.locationId;
        String materialId = selectedPlanningBookCellDTO.getMaterialId();
        Location location = locationRepository.findById(locationId).get();

        // verifica se material faz parte do configured view. caso negativo lança exceção
        Set<Produto> materiais = configuredViewProjectionComAgrupamentosAAtualizar.getMateriaisAAtualizar();
        Produto material = materiais.stream()
                .filter(x -> x.getId().equals(materialId))
                .findFirst().orElseThrow(() -> new NoResultException("Selected material not found for user view " + selectedPlanningBookCellDTO.viewName));
        validaLocationAcessivelSupplyPlanningBookCommunity(
                configuredViewProjectionComAgrupamentosAAtualizar,
                location,
                locationId,
                selectedPlanningBookCellDTO.viewName,
                "Supply Planning cell details");

        // Usa o perfil de execucao raiz do Supply Plan Community.
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlan.getPerfilExecucaoSupplyPlan();
        LocationProjectionCompleto locationProjectionCompleto = LocationProjectionFactory.getLocationProjectionCompleto(clusterEParametrosProjection);

        PoliticaEstoquesProjection politicaEstoquesProjection = politicaEstoquesProjectionFactory.getPoliticaEstoquesProjection(
                        supplyPlan.getCalendarioDoSupplyPlan(clusterEParametrosProjection.getParametrosGlobais()),
                        clusterEParametrosProjection,
                        perfilExecucaoSupplyPlan);

        UnidadeMedidaProjection conversaoUnidadeMedidaProjection = conversaoUnidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();

        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();

        UnidadeMedida unidadeMedidaView = configuredViewProjectionComAgrupamentosAAtualizar.getUnidadeMedidaView(parametrosGlobais);

        SupplyPlanningProjection supplyPlanningProjection = supplyPlanProjectionFactory.getSupplyPlanningProjectionCompleto(
                supplyPlan,
                perfilExecucaoSupplyPlan, // considera o perfil raiz do supply plan Community
                location,
                supplyNetworkProjection,
                politicaEstoquesProjection,
                MaterialProjectionFactory.getProjectionUnicoMaterial(material, clusterEParametrosProjection),
                locationProjectionCompleto);

        Calendario calendario = supplyPlanningProjection.getCalendario();
        int posicaoPeriodoModificada = calendario.getPosicaoPeriodo(
                selectedPlanningBookCellDTO.period);

        KeyFigureStandardEnum keyFigureStandardEnum =
                resolveKeyFigureStandardEnumDetalheSupplyPlanningBookCommunity(
                        selectedPlanningBookCellDTO.keyFigure);
        validaKeyFigureDetalheSupplyPlanningBookCommunity(keyFigureStandardEnum);
        Constantes.TipoPlano tipoPlano = resolveTipoPlanoDetalheSupplyPlanningBookCommunity(
                selectedPlanningBookCellDTO.keyFigure);

        CellDetailsDTO cellDetailsDTO = new CellDetailsDTO();
        cellDetailsDTO.locationId = location.getId();
        cellDetailsDTO.materialId = material.getId();
        cellDetailsDTO.planId = supplyPlan.getId();
        cellDetailsDTO.viewName = selectedPlanningBookCellDTO.viewName;
        cellDetailsDTO.keyFigure = keyFigureStandardEnum;
        cellDetailsDTO.tipoPlano = tipoPlano;
        cellDetailsDTO.period = selectedPlanningBookCellDTO.period;

        if (keyFigureStandardEnum.equals(KeyFigureStandardEnum.PRODUCAO_PLANEJADA)
                || keyFigureStandardEnum.equals(KeyFigureStandardEnum.PRODUCAO_FIRME)) {

            boolean producaoFirme = keyFigureStandardEnum.equals(KeyFigureStandardEnum.PRODUCAO_FIRME);
            boolean producaoFirmeSomenteLeitura = producaoFirme
                    && isKeyFigureDetalheSupplyPlanningBookReadOnlyEnterprise(keyFigureStandardEnum);
            Constantes.FirmePlanejado firmePlanejado = producaoFirme
                    ? Constantes.FirmePlanejado.ORDEM
                    : Constantes.FirmePlanejado.PLANEJADO;

            cellDetailsDTO.columnDefs.add(ImmutableMap.of("headerName", "Production Version Id", "field", "Production Version Id", "width", 100, "editable", false));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of("headerName", "Priority", "field", "Priority", "width", 80, "editable", false));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of("headerName", "Routing Ids", "field", "Routing Ids", "width", 100, "editable", false));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of("headerName", "Production Resources", "field", "Production Resources", "width", 100, "editable", false));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of("headerName", "BOM Ids", "field", "BOM Ids", "width", 80, "editable", false));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of(
                    "headerName", "Quantity",
                    "field", "Quantity",
                    "width", 80,
                    "editable", !producaoFirmeSomenteLeitura));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of(
                    "headerName", "UOM",
                    "field", "UOM",
                    "width", 80,
                    "editable", !producaoFirmeSomenteLeitura));

            int maximoRecursosProdutivosEmRoteiro = 0;

            // Detalhes produtivos Community sempre olham a versao prioritaria.
            boolean consideraVersoesProducaoParalelas = false;
            List<VersaoProducao> versoesProducao = supplyNetworkProjection.getVersoesProducaoViaveisOrdenadasPorPrioridade(
                    location, material, consideraVersoesProducaoParalelas, null);

            for (VersaoProducao versaoProducao : versoesProducao) {

                for (Triplet<Roteiro,ListaTecnica,Double> detalheComponenteVersaoProducao : versaoProducao.getDetalhePorVersaoProducao(
                        conversaoUnidadeMedidaProjection, material, unidadeMedidaView, posicaoPeriodoModificada)) {

                    Map<String,Object> detalhes = new HashMap<>();

                    String routingIds = versaoProducao.getRoteiros().stream()
                            .map(x -> (x.getId() == null) ? "No Prod Version" : x.getId())
                            .reduce("", (subtotal, novo) -> (subtotal.equals("")) ? novo : subtotal + "," + novo);
                    String productionResourcesIds = versaoProducao.getRoteiros().stream()
                            .flatMap(x -> x.getOperacaoRoteiroListOrdenadaPorPosicaoAsc().stream())
                            .map(x -> x.getRecursoProdutivo().getId())
                            .reduce("", (subtotal, novo) -> (subtotal.equals("")) ? novo : subtotal + "," + novo);
                    String bomIds = versaoProducao.getListasTecnicas().stream()
                            .map(x -> x.getId())
                            .reduce("", (subtotal, novo) -> (subtotal.equals("")) ? novo : subtotal + "," + novo);

                    detalhes.put("Production Version Id", versaoProducao.getId());
                    detalhes.put("Priority", versaoProducao.getPrioridadeCadastrada());
                    detalhes.put("Routing Ids", routingIds);
                    detalhes.put("Production Resources", productionResourcesIds);
                    detalhes.put("BOM Ids", bomIds);

                    detalhes.put("UOM", unidadeMedidaView.getId());
                    detalhes.put("Quantity", supplyPlanningProjection.getQuantidadeProductionPlan(
                            posicaoPeriodoModificada, material, versaoProducao,
                            tipoPlano,
                            firmePlanejado,
                            unidadeMedidaView));

                    cellDetailsDTO.detailLines.add(detalhes);

                }

            }

        } else if (keyFigureStandardEnum.equals(KeyFigureStandardEnum.INBOUND_PLANEJADO)
                || keyFigureStandardEnum.equals(KeyFigureStandardEnum.INBOUND_FIRME)) {

            boolean inboundFirme = keyFigureStandardEnum.equals(KeyFigureStandardEnum.INBOUND_FIRME);
            boolean inboundFirmeSomenteLeitura = inboundFirme
                    && isKeyFigureDetalheSupplyPlanningBookReadOnlyEnterprise(keyFigureStandardEnum);
            Constantes.FirmePlanejado firmePlanejado = inboundFirme
                    ? Constantes.FirmePlanejado.ORDEM
                    : Constantes.FirmePlanejado.PLANEJADO;

            cellDetailsDTO.columnDefs.add(ImmutableMap.of("headerName", "Origin Location Id", "field", "Origin Location Id", "width", 120, "editable", false));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of("headerName", "Priority", "field", "Priority", "width", 80, "editable", false));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of("headerName", "Lead Time (days)", "field", "Lead Time (days)", "width", 120, "editable", false));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of("headerName", "Viable Delivery (lead time)?", "field", "Viable Delivery (lead time)?", "width", 140, "editable", false));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of("headerName", "UOM", "field", "UOM", "width", 50, "editable", false));
            cellDetailsDTO.columnDefs.add(ImmutableMap.of(
                    "headerName", "Quantity",
                    "field", "Quantity",
                    "width", 100,
                    "editable", !inboundFirmeSomenteLeitura));

            for (LinhaTransporte linhaTransporteInbound : supplyNetworkProjection
                    .getLinhaTransporteInboundViavelSetParaLocationMaterial(
                            versaoMalha, location, material, supplyPlan.getDataInicioPlano(), null)
                    .stream()
                    .sorted(Comparator.comparing(x -> x.getPrioridade()))
                    .collect(Collectors.toList())) {

                Map<String,Object> detalhes = new HashMap<>();

                int leadTimeDias = supplyNetworkProjection
                .getLeadTimeDiasEntreOrigemDestinoParaMaterial(
                        versaoMalha,
                        linhaTransporteInbound.getLocationOrigem(),
                        linhaTransporteInbound.getLocationDestino(),
                        material,
                        supplyPlan.getDataInicioPlano())
                .orElseThrow(() -> new SupplyPlanException(
                        "Lead time not found in Supply Planning inbound detail line from origin "
                                + linhaTransporteInbound.getLocationOrigem().getId()
                                + " to destination "
                                + linhaTransporteInbound.getLocationDestino().getId()
                                + " for material " + material.getId() + "."));

                detalhes.put("Origin Location Id", linhaTransporteInbound.getLocationOrigem().getId());
                detalhes.put("Priority", linhaTransporteInbound.getPrioridade());
                detalhes.put("Lead Time (days)", leadTimeDias);
                detalhes.put("Viable Delivery (lead time)?", calendario.getPosicaoPeriodoAposOffsetDoFimPeriodoReferencia(
                        calendario.getPosicaoPeriodoPresente() - 1, leadTimeDias, TamanhoBucket.DIARIO) <= posicaoPeriodoModificada);
                detalhes.put("UOM", unidadeMedidaView.getId());
                detalhes.put("Quantity", supplyPlanningProjection.getQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, posicaoPeriodoModificada, material, linhaTransporteInbound.getLocationOrigem(), firmePlanejado, tipoPlano, unidadeMedidaView));

                cellDetailsDTO.detailLines.add(detalhes);

            }

        }

        /*
         * O Community termina aqui suas KFs publicas. O hook permite que o
         * overlay Enterprise detalhe uma KF privada a partir da mesma
         * SupplyPlanningProjection ja construida para a celula, sem reabrir
         * o parser/publicacao Community nem consultar uma fonte adicional.
         */
        materializaDetalhesKeyFigureSupplyPlanningBookEnterprise(
                cellDetailsDTO,
                keyFigureStandardEnum,
                supplyPlanningProjection,
                location,
                material,
                posicaoPeriodoModificada,
                unidadeMedidaView,
                tipoPlano);

        return cellDetailsDTO;

    }

    /**
     * Valida o shape minimo para consulta de detalhe do Supply Planning Book.
     *
     * <p>Detalhes sao somente leitura, mas ainda precisam de plano, view,
     * location, periodo e linha material/location. A location pode vir no campo
     * dedicado do Supply Book e por isso e normalizada para o mapa antes da
     * mesma validacao material/location usada nos updates.</p>
     */
    

    /**
     * Método invocado ao se editar quantidades detalhadas no planning book de supply
     *
     * <p>As validacoes Community de key figure e escopo acontecem dentro do
     * metodo antes de montar projections pesadas. Falhas funcionais usam
     * excecoes runtime especificas de dominio, alinhadas com o restante do
     * recorte Community.</p>
     *
     * @param cellDetailsDTO
     * @return
     */
    public PlanningBookDTO modificaDetalhesSupplyPlan(CellDetailsDTO cellDetailsDTO, String userId) {

        if (cellDetailsDTO == null) {
            throw new IllegalArgumentException("Supply Planning Book cell detail update payload is required");
        }
        if (cellDetailsDTO.keyFigure == null) {
            throw new IllegalArgumentException("Supply Planning Book cell detail update key figure is required");
        }

        /*
         * Edicao detalhada Community existe somente para KFs operacionais do
         * Planning Book. KFs Enterprise devem falhar antes de carregar location,
         * view, supply network ou projections.
         */
        validaKeyFigureModificacaoSupplyPlanningBookCommunity(cellDetailsDTO.keyFigure);

        String locationId = cellDetailsDTO.locationId;
        String materialId = cellDetailsDTO.materialId;
        Location location = locationRepository.findById(locationId).get();

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        // gera view projection, incluindo todas as locations possíveis para a view
        // ao se gerar o planning book DTO se deverão filtrar somente dados da location selecionada
        ConfiguredViewProjection configuredViewProjectionFiltrado = configuredViewProjectionFactory.getConfiguredViewProjection(
                userId,
                cellDetailsDTO.viewName,
                ConfiguredView.TipoView.SUPPLYPLANNINGBOOK,
                clusterEParametrosProjection);

        Set<Produto> materiaisView = configuredViewProjectionFiltrado.getMateriaisFiltrados();
        Produto material = materiaisView.stream()
                .filter(x -> x.getId().equals(materialId))
                .findFirst().orElseThrow(() -> new NoResultException("Selected material not found for user view " + cellDetailsDTO.viewName));
        validaLocationAcessivelSupplyPlanningBookCommunity(
                configuredViewProjectionFiltrado,
                location,
                locationId,
                cellDetailsDTO.viewName,
                "Supply Planning detailed adjustment");
        // O refresh do planning book de supply exige apenas uma location filtrada.
        // Restringe a view à location editada, preservando somente DFUs ativos dessa location.
        configuredViewProjectionFiltrado.getDfuProjectionFiltrado().aplicaFiltroDFUs(
                AgregacaoDFU.getDFUListDeProdutoCartesianoLocationMaterial(
                        Sets.newHashSet(location),
                        materiaisView,
                        clusterEParametrosProjection));

        SupplyPlan supplyPlan = supplyPlanRepository.findById(cellDetailsDTO.planId).get();
        VersaoMalha versaoMalha = supplyPlan.getVersaoMalha();
        // Usa o perfil de execucao raiz do Supply Plan Community.
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlan.getPerfilExecucaoSupplyPlan();
        LocationProjection locationProjection = LocationProjectionFactory.getLocationProjectionDePerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan, clusterEParametrosProjection);
        MaterialProjection materialProjectionMateriaisView = MaterialProjectionFactory.getProjectionSetMateriais(materiaisView, clusterEParametrosProjection);

        DemandPlan demandPlan = supplyPlan.getDemandPlan();

        PoliticaEstoquesProjection politicaEstoquesProjection = politicaEstoquesProjectionFactory.getPoliticaEstoquesProjection(
                        supplyPlan.getCalendarioDoSupplyPlan(clusterEParametrosProjection.getParametrosGlobais()),
                        clusterEParametrosProjection,
                        perfilExecucaoSupplyPlan);

        UnidadeMedidaProjection conversaoUnidadeMedidaProjection = conversaoUnidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();

        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();

        UnidadeMedida unidadeMedidaView = configuredViewProjectionFiltrado.getUnidadeMedidaView(parametrosGlobais);

        SupplyPlanningProjection supplyPlanningProjection = supplyPlanProjectionFactory.getSupplyPlanningProjectionCompleto(
                supplyPlan,
                perfilExecucaoSupplyPlan, // considera o perfil raiz do supply plan Community
                location,
                supplyNetworkProjection,
                politicaEstoquesProjection,
                materialProjectionMateriaisView,
                locationProjection); // precisa ser para todos materiais da view e apenas material filtrado por conta de limitação de como o DTO é gerado mais adiante. se for apenas filtrados ele zera os demais materais do GrupoDTO

        DemandPlanningProjection demandPlanningProjection = demandPlanProjectionFactory.getDemandPlanningProjectionCompleto(
                demandPlan,
                location,
                materiaisView/*Sets.newHashSet(material)*/, // precisa ser para todos materiais da view e apenas material filtrado por conta de limitação de como o DTO é gerado mais adiante. se for apenas filtrados ele zera os demais materais do GrupoDTO
                false); // Community nao aplica demand catch-up por historico de vendas passado.

        SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu = splitTemporalProjectionFactory.geraSplitTemporalProjectionPorDfu(
                demandPlanningProjection.getDemandPlan(),
                supplyPlan);

        Calendario calendario = supplyPlanningProjection.getCalendario();
        int posicaoPeriodoModificacao = calendario.getPosicaoPeriodo(cellDetailsDTO.period);
        Constantes.TipoPlano tipoPlano = Optional.ofNullable(cellDetailsDTO.tipoPlano).orElse(Constantes.TipoPlano.PLANO_TRABALHO);

        // Esta service front ainda materializa os detalhes editaveis do Planning Book porque a rotina
        // precisa interpretar diretamente as linhas enviadas pelo grid. O save final continua centralizado
        // no SupplyPlanService para preservar a persistencia padrao do plano.
        if (cellDetailsDTO.keyFigure.equals(KeyFigureStandardEnum.PRODUCAO_PLANEJADA)) {
            for (Map<String,Object> linhaDetalhe : cellDetailsDTO.detailLines) {

                String versaoProducaoId = (String) linhaDetalhe.get("Production Version Id");

                VersaoProducao versaoProducao;
                // Salvamento de detalhe produtivo Community nunca seleciona
                // versoes paralelas; esse comportamento pertence ao Enterprise.
                boolean consideraVersoesProducaoParalelas = false;
                if (versaoProducaoId.equals("No Prod Version")) {

                    Roteiro roteiro = supplyNetworkProjection.getRoteirosViaveis(location, material).stream()
                            .filter(roteiroCandidato -> roteiroCandidato.getId().equals(
                                    (String) linhaDetalhe.get("Routing Id")))
                            .findAny()
                            .get();
                    ListaTecnica listaTecnica = supplyNetworkProjection.getListasTecnicasViaveis(location, material, null).stream()
                            .filter(listaTecnicaCandidata -> listaTecnicaCandidata.getId().equals(
                                    (String) linhaDetalhe.get("BOM Id")))
                            .findAny()
                            .get();

                    versaoProducao = supplyNetworkProjection
                .getVersoesProducaoViaveisOrdenadasPorPrioridade(
                        location,
                        material,
                        consideraVersoesProducaoParalelas,
                        null)
                .stream()
                            .filter(versaoProducaoCandidata -> versaoProducaoCandidata.getId() == null
                                    && versaoProducaoCandidata.getRoteiros().contains(roteiro)
                                    && versaoProducaoCandidata.getListasTecnicas().contains(listaTecnica))
                            .findAny()
                            .get();

                } else {
                    versaoProducao = supplyNetworkProjection
                .getVersoesProducaoViaveisOrdenadasPorPrioridade(
                        location,
                        material,
                        consideraVersoesProducaoParalelas,
                        null)
                .stream()
                            .filter(versaoProducaoCandidata -> versaoProducaoCandidata.getId().equals(
                                    (String) linhaDetalhe.get("Production Version Id")))
                            .findAny()
                            .get();
                }

                Object quantityObj = linhaDetalhe.get("Quantity");
                double quantity;
                if (quantityObj instanceof String) {
                    quantity = Double.valueOf((String) quantityObj);
                } else if (quantityObj instanceof Number) {
                    quantity = ((Number) quantityObj).doubleValue();
                } else {
                    quantity = 0;
                }

                supplyPlanningProjection.setQuantidadeProductionPlan(
                        posicaoPeriodoModificacao,
                        material,
                        versaoProducao,
                        Math.max(0, quantity),
                        tipoPlano,
                        Constantes.FirmePlanejado.PLANEJADO,
                        unidadeMedidaView);
            }
        } else if (cellDetailsDTO.keyFigure.equals(KeyFigureStandardEnum.INBOUND_PLANEJADO)) {
            for (Map<String,Object> linhaDetalhe : cellDetailsDTO.detailLines) {

                Location locationOrigem = locationRepository.findById((String) linhaDetalhe.get("Origin Location Id")).get();

                Object quantityObj = linhaDetalhe.get("Quantity");
                double quantity;
                if (quantityObj instanceof String) {
                    quantity = Double.valueOf((String) quantityObj);
                } else if (quantityObj instanceof Number) {
                    quantity = ((Number) quantityObj).doubleValue();
                } else {
                    quantity = 0;
                }

                supplyPlanningProjection.setQuantidadeDistributionPlanInbound(
                        ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL,
                        posicaoPeriodoModificacao,
                        material,
                        locationOrigem,
                        Math.max(0, quantity),
                        unidadeMedidaView,
                        Constantes.FirmePlanejado.PLANEJADO,
                        tipoPlano);

            }
        }

        SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(supplyPlanningProjection, tipoPlano);
        SupplyPlanning.atualizaEstoqueSeguranca(supplyPlanningProjection, tipoPlano);
        if (!perfilExecucaoSupplyPlan.getPermiteBacklogDemanda()) {
            SupplyPlanning.limitaEstoquesNegativosAZero(tipoPlano, supplyPlanningProjection);
        }

        supplyPlanService.saveDistributionPlanInboundDePlanningProjection(supplyPlanningProjection, ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, posicaoPeriodoModificacao, true);
        supplyPlanService.saveProductionPlanOutputDePlanningProjection(supplyPlanningProjection, posicaoPeriodoModificacao, true);
        supplyPlanService.saveInventoryPlanDePlanningProjection(supplyPlanningProjection, true);

        // atualiza o inventory plan das DFUs dependentes (material/location de origem ou insumos de produção)
        Set<DFU> dfusDependentes = SupplyPlanning.getDfusDependentes(
                AgregacaoDFU.getDFUListDeProdutoCartesianoLocationMaterial(Sets.newHashSet(location), Sets.newHashSet(material), clusterEParametrosProjection),
                versaoMalha,
                supplyPlanningProjection.getSupplyPlan().getDataInicioPlano(),
                supplyPlanningProjection.getSupplyNetworkProjection(),
                true, true);
        supplyPlanningModificacoesService.atualizaESalvaInventoryPlanLinhasDeDFUs(
                tipoPlano,
                dfusDependentes,
                supplyPlanningProjection.getSupplyPlan(),
                perfilExecucaoSupplyPlan,
                supplyPlanningProjection.getSupplyNetworkProjection(),
                politicaEstoquesProjection,
                true);

        // Apos salvar os detalhes, a resposta e reconstruida a partir de uma KeyFigureProjection nova.
        // Isso mantem o retorno do Planning Book alinhado ao estado persistido, mesmo com custo extra.
        KeyFigureProjection keyFigureProjection = keyFigureProjectionFactory.getKeyFigureProjectionDeSupplyPlan(
                supplyPlan,
                location,
                configuredViewProjectionFiltrado,
                true);

        return planningBookService.getPlanningBookDTO(keyFigureProjection);

    }

    /**
     * Valida o payload de salvamento de detalhe antes de carregar view, plano,
     * malha ou projections.
     *
     * <p>O Community nao permite ajuste detalhado agregado. O detalhe pode vir
     * sem linhas quando o usuario zera uma lista operacional, mas a lista nao
     * pode ser nula porque a rotina interpreta esse campo como snapshot do grid
     * de detalhes enviado pelo front.</p>
     */
    /**
     * Resolve o roteiro de uma linha de detalhe produtivo.
     *
     * <p>As linhas de detalhe sao geradas pelo proprio backend a partir da
     * malha viavel Community. Se o payload volta com routing inexistente, a
     * tela provavelmente esta stale ou houve alteracao manual da chamada; nesse
     * caso a edicao deve falhar antes de gravar production plan inconsistente.</p>
     */
    /**
     * Resolve a lista tecnica de uma linha de detalhe produtivo.
     */
    /**
     * Resolve a versao de producao sintetica usada quando o detalhe produtivo
     * representa roteiro/lista tecnica sem id cadastrado de versao.
     */
    /**
     * Resolve a versao de producao cadastrada de uma linha de detalhe produtivo.
     */
    /**
     * Resolve a location origem de uma linha de detalhe inbound.
     */
    

    /**
     * Resolve o lead time usado para exibir viabilidade de uma linha inbound.
     *
     * <p>A propria lista de inbound vem de linhas viaveis da malha. Ausencia
     * de lead time nesse ponto indica inconsistencia entre linha, material e
     * data de referencia; retornar zero mascararia prazo incorreto no Planning
     * Book.</p>
     */
    private IllegalArgumentException getUnsupportedIndirectDemandDetailsKeyFigureException(
            KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanning) {

        return new IllegalArgumentException(
                "SupplyPlanFrontService can load indirect demand details only for "
                        + KeyFigureStandardEnum.DEMANDA_INDIRETA_TOTAL
                        + "; received "
                        + keyFigureStandardSupplyPlanning.getKeyFigureStandardEnum()
                        + " in "
                        + keyFigureStandardSupplyPlanning.getTipoPlano()
                        + ". Other Supply Planning Book cells must use the standard cell-details flow.");

    }

    /**
     * Valida a lista enviada pelo endpoint de update do Supply Planning Book.
     *
     * <p>A assinatura recebe lista porque o DTO e compartilhado com Demand
     * Planning e porque o front pode enviar mais de uma linha material/location
     * para a mesma operacao. A implementacao Community de Supply, no entanto,
     * aplica uma unica key figure, periodo e valor de ajuste por chamada. Por
     * isso payloads manuais que misturem KFs, periodos ou valores precisam
     * falhar antes da projection e antes de qualquer persistencia.</p>
     *
     * <p>A validacao percorre todas as celulas tambem para bloquear KFs
     * Enterprise em qualquer item da lista. Sem isso, uma payload com primeira
     * celula Community e segunda celula Enterprise poderia passar pela leitura
     * do primeiro item e chegar a uma projection inconsistente.</p>
     *
     * <p>A mesma validacao normaliza a location explicita do Supply Book para
     * o mapa compartilhado de dimensoes e exige material/location em todas as
     * celulas. Assim o service pode entregar a lista inteira ao
     * ConfiguredViewService sem reabrir ajuste agregado nem ignorar silenciosamente
     * celulas depois da primeira.</p>
     */
    private KeyFigureStandardSupplyPlanning validaSelecaoModificacaoSupplyPlanningBookCommunity(
            List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs) {

        if (selectedPlanningBookCellDTOs == null || selectedPlanningBookCellDTOs.isEmpty()) {
            throw new IllegalArgumentException("Supply Planning Book update requires at least one selected cell");
        }

        for (int indiceSelectedPlanningBookCellDTO = 0;
             indiceSelectedPlanningBookCellDTO < selectedPlanningBookCellDTOs.size();
             indiceSelectedPlanningBookCellDTO++) {
            if (selectedPlanningBookCellDTOs.get(indiceSelectedPlanningBookCellDTO) == null) {
                throw new IllegalArgumentException(
                        "Supply Planning Book update cannot contain null selected cell at index "
                                + indiceSelectedPlanningBookCellDTO);
            }
        }

        if (selectedPlanningBookCellDTOs.stream()
                .anyMatch(selectedPlanningBookCellDTO -> selectedPlanningBookCellDTO.referencePlanId != null)) {
            throw new RequiresEnterpriseVersionException("Supply Planning reference plan");
        }

        SelectedPlanningBookCellDTO selectedPlanningBookCellDTOReferencia = selectedPlanningBookCellDTOs.get(0);
        normalizaLocationDescriptionColsSupplyPlanningBookCommunity(selectedPlanningBookCellDTOReferencia);
        validaEscopoMaterialLocationAjusteSupplyPlanningBookCommunity(selectedPlanningBookCellDTOReferencia);

        KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanningReferencia =
                getKeyFigureStandardSupplyPlanningBookCommunity(selectedPlanningBookCellDTOReferencia.keyFigure);

        for (SelectedPlanningBookCellDTO selectedPlanningBookCellDTO : selectedPlanningBookCellDTOs) {
            normalizaLocationDescriptionColsSupplyPlanningBookCommunity(selectedPlanningBookCellDTO);
            validaEscopoMaterialLocationAjusteSupplyPlanningBookCommunity(selectedPlanningBookCellDTO);

            KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanning =
                    getKeyFigureStandardSupplyPlanningBookCommunity(selectedPlanningBookCellDTO.keyFigure);

            if (!keyFigureStandardSupplyPlanningReferencia.equals(keyFigureStandardSupplyPlanning)) {
                throw new IllegalArgumentException("Supply Planning Book update requires the same key figure and plan type for all selected cells");
            }
            if (!Objects.equals(selectedPlanningBookCellDTOReferencia.planId, selectedPlanningBookCellDTO.planId)) {
                throw new IllegalArgumentException("Supply Planning Book update requires the same plan for all selected cells");
            }
            if (!Objects.equals(selectedPlanningBookCellDTOReferencia.viewName, selectedPlanningBookCellDTO.viewName)) {
                throw new IllegalArgumentException("Supply Planning Book update requires the same view for all selected cells");
            }
            if (!Objects.equals(selectedPlanningBookCellDTOReferencia.locationId, selectedPlanningBookCellDTO.locationId)) {
                throw new IllegalArgumentException("Supply Planning Book update requires the same location for all selected cells");
            }
            if (!Objects.equals(selectedPlanningBookCellDTOReferencia.period, selectedPlanningBookCellDTO.period)) {
                throw new IllegalArgumentException("Supply Planning Book update requires the same period for all selected cells");
            }
            if (!Objects.equals(selectedPlanningBookCellDTOReferencia.newValue, selectedPlanningBookCellDTO.newValue)) {
                throw new IllegalArgumentException("Supply Planning Book update requires the same new value for all selected cells");
            }
            if (!Objects.equals(selectedPlanningBookCellDTOReferencia.oldValue, selectedPlanningBookCellDTO.oldValue)) {
                throw new IllegalArgumentException("Supply Planning Book update requires the same old value for all selected cells");
            }
        }

        validaKeyFigureEditavelSupplyPlanningBookCommunity(keyFigureStandardSupplyPlanningReferencia);

        return keyFigureStandardSupplyPlanningReferencia;

    }

    /**
     * Bloqueia ajuste agregado antes da criacao de projections.
     *
     * <p>Supply Planning Book Community recebe a location tambem no campo
     * dedicado `locationId`, por isso a chamada deve passar por
     * {@link #normalizaLocationDescriptionColsSupplyPlanningBookCommunity(SelectedPlanningBookCellDTO)}
     * antes desta validacao. O material, no entanto, precisa estar no mapa de
     * dimensoes da linha; ausencia ou valor vazio indicam uma linha agregada
     * que pertence ao Enterprise.</p>
     */
    private void validaEscopoMaterialLocationAjusteSupplyPlanningBookCommunity(
            SelectedPlanningBookCellDTO selectedPlanningBookCellDTO) {

        if (!selectedPlanningBookCellDTO.isMaterialLevel()
                || selectedPlanningBookCellDTO.getMaterialId() == null
                || selectedPlanningBookCellDTO.getMaterialId().isBlank()
                || !selectedPlanningBookCellDTO.isLocationLevel()
                || selectedPlanningBookCellDTO.getLocationId() == null
                || selectedPlanningBookCellDTO.getLocationId().isBlank()) {
            throw new RequiresEnterpriseVersionException("Planning Book aggregated adjustments");
        }

    }

    /**
     * Valida o shape minimo de cada celula antes de buscar plano, location,
     * view ou projections de Supply Planning.
     *
     * <p>A operacao Community aplica uma alteracao homogenea por chamada. Esses
     * campos sao usados para identificar plano, view, location, periodo, key
     * figure e valor do ajuste; se algum deles estiver ausente, seguir adiante
     * apenas deslocaria o erro para repository, calendario ou persistencia.</p>
     */
    /**
     * Mantem o contrato historico do Supply Planning Book em que a location da
     * tela vem no campo explicito `locationId`, enquanto `ConfiguredViewService`
     * espera a dimensao material/location dentro dos mapas usados tambem pelo
     * Demand Planning Book.
     *
     * <p>Payloads de front compartilhado podem enviar `locationDescriptionCols`
     * nulo quando a location ja esta no campo dedicado. Nesse caso o Community
     * materializa um mapa minimo antes de delegar para a validacao comum; mapas
     * existentes continuam sendo preservados e apenas recebem a location
     * selecionada como filtro obrigatorio.</p>
     */
    private void normalizaLocationDescriptionColsSupplyPlanningBookCommunity(
            SelectedPlanningBookCellDTO selectedPlanningBookCellDTO) {

        if (selectedPlanningBookCellDTO.locationDescriptionCols == null) {
            selectedPlanningBookCellDTO.locationDescriptionCols = new HashMap<>();
        }

        selectedPlanningBookCellDTO.locationDescriptionCols.put(
                "locationId",
                selectedPlanningBookCellDTO.locationId);

    }

    /**
     * Decodifica a key figure do Supply Planning Book preservando o erro
     * funcional correto para capacidades Enterprise conhecidas.
     *
     * <p>`KeyFigureStandardSupplyPlanning` aceita somente o subconjunto
     * publicavel no Community e lanca `IllegalArgumentException` para o resto.
     * Antes de instanciar essa classe, verificamos se o identificador recebido
     * corresponde a uma KF Enterprise conhecida para devolver
     * `RequiresEnterpriseVersionException`, que e o contrato usado pelo front
     * compartilhado para marcar e bloquear features Enterprise.</p>
     */
    /**
     * Resolve a identidade de uma KF usada somente na leitura de detalhes de
     * celula. O Community mantem a representacao tipada publica; overlays
     * privados podem reabrir uma identidade standard ja materializavel sem
     * alterar o parser geral da Configured View.
     */
    protected KeyFigureStandardEnum resolveKeyFigureStandardEnumDetalheSupplyPlanningBookCommunity(
            String keyFigureId) {

        return getKeyFigureStandardSupplyPlanningBookCommunity(keyFigureId).getKeyFigureStandardEnum();

    }

    /**
     * Resolve o plano da KF de detalhe. A implementacao Community aceita
     * apenas identificadores tipados pelo seu catalogo publico.
     */
    protected Constantes.TipoPlano resolveTipoPlanoDetalheSupplyPlanningBookCommunity(
            String keyFigureId) {

        return getKeyFigureStandardSupplyPlanningBookCommunity(keyFigureId).getTipoPlano();

    }

    /**
     * Gate separado para detalhes read-only. O Enterprise pode liberar uma
     * serie privada apenas para leitura sem reabrir o endpoint de modificacao.
     */
    protected void validaKeyFigureDetalheSupplyPlanningBookCommunity(
            KeyFigureStandardEnum keyFigureStandardEnum) {

        validaKeyFigureSupplyPlanningBookCommunity(keyFigureStandardEnum);

    }

    /**
     * Gate separado para modificacoes de detalhe. O default conserva o bloqueio
     * Community inclusive se um overlay reabrir a mesma KF para leitura.
     */
    protected void validaKeyFigureModificacaoSupplyPlanningBookCommunity(
            KeyFigureStandardEnum keyFigureStandardEnum) {

        validaKeyFigureSupplyPlanningBookCommunity(keyFigureStandardEnum);

    }

    /**
     * Mantem detalhes Enterprise indisponiveis para edicao no Community. O
     * overlay pode marcar uma KF privada como read-only depois de liberar sua
     * consulta, sem alterar o endpoint de modificacao.
     */
    protected boolean isKeyFigureDetalheSupplyPlanningBookReadOnlyEnterprise(
            KeyFigureStandardEnum keyFigureStandardEnum) {

        return false;

    }

    /**
     * Ponto de extensao para detalhes Enterprise que dependem exclusivamente
     * do snapshot de Supply Planning ja montado para a celula.
     *
     * <p>O default Community nao adiciona linhas. A extensao nao deve buscar
     * entidades, repositories ou projections adicionais por celula; ela
     * recebe a location, material, periodo e UOM ja validados pelo fluxo
     * compartilhado.</p>
     */
    protected void materializaDetalhesKeyFigureSupplyPlanningBookEnterprise(
            CellDetailsDTO cellDetailsDTO,
            KeyFigureStandardEnum keyFigureStandardEnum,
            SupplyPlanningProjection supplyPlanningProjection,
            Location location,
            Produto material,
            int posicaoPeriodo,
            UnidadeMedida unidadeMedidaView,
            Constantes.TipoPlano tipoPlano) {

        // Sem detalhes privados no Community.

    }

    private KeyFigureStandardSupplyPlanning getKeyFigureStandardSupplyPlanningBookCommunity(String keyFigureId) {

        getOptionalKeyFigureStandardEnumSupplyPlanningBook(keyFigureId)
                .ifPresent(this::validaKeyFigureSupplyPlanningBookCommunity);

        return new KeyFigureStandardSupplyPlanning(keyFigureId);

    }

    private Optional<KeyFigureStandardEnum> getOptionalKeyFigureStandardEnumSupplyPlanningBook(String keyFigureId) {

        if (keyFigureId == null) {
            return Optional.empty();
        }

        String keyFigureIdSemTipoPlano = removeSufixoTipoPlanoSupplyPlanningBook(keyFigureId);
        return Arrays.stream(KeyFigureStandardEnum.values())
                .filter(keyFigureStandardEnum ->
                        keyFigureStandardEnum.name().equals(keyFigureIdSemTipoPlano)
                                || MetodosUtilidade.getValorJsonPropertyDeEnum(keyFigureStandardEnum).equals(keyFigureIdSemTipoPlano))
                .findFirst();

    }

    private String removeSufixoTipoPlanoSupplyPlanningBook(String keyFigureId) {

        for (Constantes.TipoPlano tipoPlano : Constantes.TipoPlano.values()) {
            String descricaoTipoPlano = MetodosUtilidade.getValorJsonPropertyDeEnum(tipoPlano);
            String sufixoId = "-" + descricaoTipoPlano;
            if (keyFigureId.endsWith(sufixoId)) {
                return keyFigureId.substring(0, keyFigureId.length() - sufixoId.length());
            }

            String sufixoDescricao = " (" + descricaoTipoPlano + ")";
            if (keyFigureId.endsWith(sufixoDescricao)) {
                return keyFigureId.substring(0, keyFigureId.length() - sufixoDescricao.length());
            }
        }

        return keyFigureId;

    }

    private void validaKeyFigureSupplyPlanningBookCommunity(KeyFigureStandardEnum keyFigureStandardEnum) {

        if (keyFigureStandardEnum == null) {
            throw new IllegalArgumentException("Supply Planning Book key figure must be provided");
        }

        switch (keyFigureStandardEnum) {
            case DEMANDA_DIRETA_CARTEIRA_SNP ->
                    throw new RequiresEnterpriseVersionException("Supply Planning customer orders key figure");
            case PRODUCAO_FIRME ->
                    throw new RequiresEnterpriseVersionException("Supply Planning production orders key figure");
            case INBOUND_FIRME ->
                    throw new RequiresEnterpriseVersionException("Supply Planning inbound orders key figure");
            case INBOUND_ESTOQUE_EM_TRANSITO ->
                    throw new RequiresEnterpriseVersionException("Supply Planning in-transit inventory key figure");
            case WRITEOFF ->
                    throw new RequiresEnterpriseVersionException("Supply Planning batch aging/writeoff key figure");
        }

    }

    /**
     * Keeps a profile-backed Planning Book aligned with the material scope
     * passed to the actual Supply execution while retaining Community fallback.
     */
    private MaterialProjection getMaterialProjectionComEscopoEnterprise(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            ClusterEParametrosProjection clusterEParametrosProjection,
            MaterialProjection materialProjectionCommunity) {

        if (supplyPlanExecutionProfileMaterialScope == null) {
            return materialProjectionCommunity;
        }

        return supplyPlanExecutionProfileMaterialScope
                .getMaterialProjectionParaExecucaoSupplyPlan(
                        perfilExecucaoSupplyPlan,
                        clusterEParametrosProjection,
                        materialProjectionCommunity);

    }

    /**
     * Garante que a alteracao manual atinja somente o subconjunto editavel do
     * Supply Planning Book Community.
     *
     * <p>`KeyFigureStandardSupplyPlanning` tambem representa KFs de leitura e
     * variantes tecnicas de plano usadas pela projection. Para atualizacao, no
     * entanto, o contrato aberto permite apenas KFs editaveis do `Working Plan`,
     * as mesmas publicadas pelo RuntimeInfo. Validar aqui evita montar
     * projections pesadas ou chamar persistencia quando o payload manual tenta
     * alterar plano restrito/irrestrito ou uma linha calculada.</p>
     */
    private void validaKeyFigureEditavelSupplyPlanningBookCommunity(
            KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanning) {

        if (!keyFigureStandardSupplyPlanning.getPadraoPermiteEdicao()) {
            throw new IllegalArgumentException(
                    "Supply Planning Book key figure "
                            + keyFigureStandardSupplyPlanning.getId()
                            + " is not editable in Community");
        }

    }
}
