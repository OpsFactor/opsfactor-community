package com.opsfactor.community.capability.demandplanning.demandplan.facade;

import com.opsfactor.community.capability.demandplanning.planningbook.service.PlanningBookDemandAdjustmentContext;
import com.opsfactor.community.capability.planningbook.domain.AjusteCelulaPlanningBook;
import com.opsfactor.community.capability.planningbook.domain.PlanningBookDfuScope;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.platform.exception.AccessDeniedException;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.factory.ParametrosDemandPlanningProjectionFactory;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterial;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanItemRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.planningbook.keyfigure.service.KeyFigureService;
import com.opsfactor.community.capability.demandplanning.engine.DemandPlanning;
import com.opsfactor.community.capability.configuration.facade.ConfiguredViewService;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.platform.exception.DemandPlanException;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.mapper.DemandPlanAutoMapper;
import com.opsfactor.community.capability.demandplanning.service.DemandPlanningService;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanItemDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanPeriodDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanSelectDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.VersaoDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.web.spi.CommunityDemandPlanningComparisonPlanSpi;
import com.opsfactor.community.capability.demandplanning.web.spi.DemandPlanningComparisonPlanSpi;
import com.opsfactor.community.capability.planningbook.facade.PlanningBookService;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjectionFactory;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.IncompatibleCalendarException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Quartet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servico de fachada usado pelo front-end de Demand Planning Community.
 *
 * <p>O Planning Book aberto preserva o ajuste de folha e o ajuste agregado
 * das Key Figures standard Community. O mesmo método compartilhado calcula o
 * delta e distribui a alteração entre as DFUs selecionadas. Recursos como
 * reference plan, Custom Key Figures, agrupamentos configuráveis e
 * características continuam pertencendo ao OpsFactor Enterprise.</p>
 */
@Slf4j
@Component
public class DemandPlanningFacade {

    /**
     * Mapper de Demand Plan para DTOs do front. Usado para listagens e detalhe
     * operacional sem expor campos Enterprise de demanda.
     */
    @Autowired
    private DemandPlanAutoMapper demandPlanAutoMapper;

    /**
     * Service principal de Demand Planning para buscar planos e reutilizar
     * regras de dominio ja centralizadas.
     */
    @Autowired
    private DemandPlanningService demandPlanningService;

    /**
     * Service de parametros globais usado para montar calendario de exibicao e
     * interpretar defaults transicionais da entidade.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Service de Key Figures. A fachada Community filtra os retornos deste bean
     * para aceitar apenas KFs padrao liberadas no Planning Book.
     */
    @Autowired
    private KeyFigureService keyFigureService;

    /**
     * Service generico de montagem do Planning Book. Recebe uma projection ja
     * filtrada pelo recorte Community material/location.
     */
    @Autowired
    private PlanningBookService planningBookService;

    /**
     * Service de User Views. No Community ele deve entregar somente views sem
     * agrupamentos por caracteristica, KFs customizadas ou reference plan.
     */
    @Autowired
    private ConfiguredViewService configuredViewService;

    /**
     * Repository de Demand Plan usado para listagens e carregamento das versoes
     * selecionadas pelo usuario.
     */
    @Autowired
    private DemandPlanRepository demandPlanRepository;

    /**
     * Repository das linhas de Demand Plan usado apenas ao persistir ajustes do
     * Planning Book material/location.
     */
    @Autowired
    private DemandPlanItemRepository demandPlanItemRepository;

    /**
     * Repository de locations usado para montar os filtros de selecao de planos.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Factory da projection de User View aplicada ao Planning Book.
     */
    @Autowired
    private ConfiguredViewProjectionFactory configuredViewProjectionFactory;

    /**
     * Factory de projection de UOM/conversoes usada por Planning Book,
     * filtro de faturamento historico e persistencia de ajustes.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory conversaoUnidadeMedidaProjectionFactory;

    /**
     * Factory da projection geral de parametros, clusters e master data.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Factory da projection de Demand Plan usada para aplicar ajustes em
     * memoria antes de persistir as linhas alteradas.
     */
    @Autowired
    private DemandPlanProjectionFactory demandPlanProjectionFactory;

    /**
     * Factory de historico sell-out usada apenas para o filtro opcional de DFUs
     * com faturamento historico na view.
     */
    @Autowired
    private SalesProjectionFactory salesProjectionFactory;

    /**
     * Factory da projection de Key Figures do Demand Plan, ja filtrada por view
     * e recorte Community.
     */
    @Autowired
    private KeyFigureProjectionFactory keyFigureProjectionFactory;

    /**
     * SPI que conserva o bloqueio Community e permite ao Enterprise montar a
     * leitura de Comparison Plan sem duplicar a fachada do Planning Book.
     *
     * <p>O fallback local preserva o contrato tambem em testes unitarios que
     * instanciam a fachada sem o container Spring. No runtime, o Enterprise
     * `@Primary` substitui essa instancia pelo bean injetado.</p>
     */
    @Autowired
    private DemandPlanningComparisonPlanSpi demandPlanningComparisonPlanSpi =
            new CommunityDemandPlanningComparisonPlanSpi();

    /**
     * Factory de parametros Demand Planning usada para carregar configuracao do
     * perfil durante ajustes do Planning Book.
     */
    @Autowired
    private ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory;

    /**
     * Retorna um DTO com os valores dos drop-downs de seleção do Demand Planning
     * Todas as locations que constem em ao menos 1 demandPlanItem são apresentadas
     * @return DemandPlanningSelectDTO
     */
    public DemandPlanSelectDTO getDemandPlanningSelectDTO() {

        List<Location> locationListCarregada = locationRepository.findAll();
        validaLocationListCarregadaParaSeletorDemandPlanningCommunity(locationListCarregada);

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        // extrai objetos que serão incorporados ao DTO
        List<Location> demandPlanLocations = locationListCarregada.stream()
                .filter(x -> clusterEParametrosProjection.getClusterLocationsDeLocation(x).getParametrosClusterLocations().getPlanejaDP())
                .collect(Collectors.toList());

        List<ClusterLocations> demandPlanClusterLocations = demandPlanLocations.stream()
                .map(location -> clusterEParametrosProjection.getClusterLocationsDeLocation(location))
                .distinct()
                .collect(Collectors.toList());
        /*
         * O seletor tambem serializa o perfil de execucao de cada versao.
         * Carrega-o na mesma consulta para impedir uma consulta lazy por
         * Demand Plan quando a base possui muitas versoes historicas.
         */
        List<DemandPlan> demandPlanList = demandPlanRepository.customFindAllComPerfilExecucao();
        validaDemandPlanListCarregadaParaListagemCommunity(
                demandPlanList,
                "Demand Planning selector");
        
        // incorpora objetos no DTO
        DemandPlanSelectDTO demandPlanningSelectDTO = new DemandPlanSelectDTO();
        demandPlanningSelectDTO.addClusterLocationsList(demandPlanClusterLocations, clusterEParametrosProjection);
        demandPlanningSelectDTO.addDemandPlanList(demandPlanList);
        
        return demandPlanningSelectDTO;
    }

    /**
     * Valida as locations carregadas para o seletor de Demand Planning.
     *
     * <p>Lista vazia e valida para base nova. Item nulo ou location sem id nao
     * pode chegar ao `ClusterEParametrosProjection`, que usa a identidade da
     * location para resolver o cluster de exibicao do seletor.</p>
     */
    private void validaLocationListCarregadaParaSeletorDemandPlanningCommunity(
            List<Location> locationListCarregada) {

        if (locationListCarregada == null) {
            throw new IllegalStateException("Demand Planning selector location list snapshot is required.");
        }

        for (int index = 0; index < locationListCarregada.size(); index++) {
            Location location = locationListCarregada.get(index);

            if (location == null) {
                throw new IllegalStateException(
                        "Demand Planning selector location at index " + index + " is required.");
            }
            if (location.getId() == null || location.getId().isBlank()) {
                throw new IllegalStateException(
                        "Demand Planning selector location at index " + index + " has no id.");
            }
        }

    }

    /**
     * Monta o calendario consumido pela projection de Demand Plan exibida no
     * front.
     *
     * @return
     */
    public Calendario getCalendarioDemandPlanExibicaoFront(DemandPlan demandPlan) {
        if (demandPlan == null) {
            throw new DemandPlanException("Demand Plan is required for Demand Planning calendar display.");
        }

        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        
        return Calendario.criaCalendarioDeOffsetsPeriodos(
                demandPlan.getTamanhoBucket(), 
                demandPlan.getDataInicioPlano(),
                0, 
                parametrosGlobais.getPeriodosHistoricosTelaDP(), 
                parametrosGlobais.getHorizonteForecastDias(), 
                0);
    }


    /*
     * Consulta e serializacao de versoes de Demand Plan.
     *
     * Estes metodos alimentam seletores e detalhes operacionais do front
     * Community. Comparacao com reference plan, Change Log e auditorias
     * avancadas permanecem fora desta fachada.
     */
    
    /**
     * Método usado para gerar lista de demand plans para diversas telas no front
     * @return 
     */
    public List<VersaoDemandPlanDTO> getListaVersaoDemandPlanDTO() {
        List<VersaoDemandPlanDTO> listaVersaoDemandPlanDTO = new ArrayList<>();
        /*
         * VersaoDemandPlanDTO le o perfil de execucao. O fetch explicito
         * preserva o contrato do seletor sem criar N+1 na listagem.
         */
        List<DemandPlan> demandPlanList = demandPlanRepository.customFindAllComPerfilExecucao();
        validaDemandPlanListCarregadaParaListagemCommunity(
                demandPlanList,
                "Demand Planning version listing");

        for (DemandPlan demandPlan : demandPlanList) {
            VersaoDemandPlanDTO versaoDemandPlanDTO = new VersaoDemandPlanDTO(demandPlan);
            listaVersaoDemandPlanDTO.add(versaoDemandPlanDTO);
        }
        
        listaVersaoDemandPlanDTO.sort(Comparator.comparing(VersaoDemandPlanDTO::getHorarioGeracao).reversed());

        return listaVersaoDemandPlanDTO;
    }
    
    public List<DemandPlanDTO> getDemandPlanDTOList() {
        List<DemandPlan> demandPlanList = demandPlanRepository.customFindAllComPerfilExecucao();
        validaDemandPlanListCarregadaParaListagemCommunity(
                demandPlanList,
                "Demand Planning DTO listing");
        List<DemandPlanDTO> demandPlanDTOList = demandPlanAutoMapper.converteListaSemLinhas(demandPlanList);
        validaDemandPlanDTOListCarregadaParaListagemCommunity(demandPlanDTOList);
        return demandPlanDTOList.stream()
                .sorted(Comparator.comparingLong(x -> ((DemandPlanDTO) x).demandPlanId).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Valida a lista de Demand Plans carregada para seletores/listagens.
     *
     * <p>Lista vazia e um estado funcional valido. Lista nula, item nulo ou
     * plano sem identidade indicam quebra do snapshot do repository e precisam
     * falhar antes de montar DTOs que acessam perfil, periodo e ids. O perfil
     * tambem e obrigatorio porque a listagem exibe o execution profile usado na
     * geracao da versao.</p>
     */
    private void validaDemandPlanListCarregadaParaListagemCommunity(
            List<DemandPlan> demandPlanList,
            String contexto) {

        if (demandPlanList == null) {
            throw new IllegalStateException(
                    "Demand Plan repository returned null list for "
                            + contexto
                            + ".");
        }

        for (int index = 0; index < demandPlanList.size(); index++) {
            DemandPlan demandPlan = demandPlanList.get(index);
            if (demandPlan == null) {
                throw new IllegalStateException(
                        "Demand Plan repository returned null item at index "
                                + index
                                + " for "
                                + contexto
                                + ".");
            }
            if (demandPlan.getId() == null) {
                throw new IllegalStateException(
                        "Demand Plan repository returned item without id at index "
                                + index
                                + " for "
                                + contexto
                                + ".");
            }
            if (demandPlan.getPerfilExecucaoDemandPlan() == null
                    || demandPlan.getPerfilExecucaoDemandPlan().getId() == null
                    || demandPlan.getPerfilExecucaoDemandPlan().getId().isBlank()) {
                throw new IllegalStateException(
                        "Demand Plan repository returned item without execution profile id at index "
                                + index
                                + " for "
                                + contexto
                                + ".");
            }
        }

    }

    /**
     * Valida DTOs resumidos gerados pelo mapper antes da ordenacao.
     *
     * <p>O mapper continua dono da conversao entidade -> DTO. Esta guarda
     * protege apenas o contrato da borda front: toda linha resumida precisa ter
     * id de Demand Plan para ordenacao deterministica e selecao posterior.</p>
     */
    private void validaDemandPlanDTOListCarregadaParaListagemCommunity(
            List<DemandPlanDTO> demandPlanDTOList) {

        if (demandPlanDTOList == null) {
            throw new IllegalStateException(
                    "Demand Plan mapper returned null DTO list for Demand Planning DTO listing.");
        }

        for (int index = 0; index < demandPlanDTOList.size(); index++) {
            DemandPlanDTO demandPlanDTO = demandPlanDTOList.get(index);
            if (demandPlanDTO == null) {
                throw new IllegalStateException(
                        "Demand Plan mapper returned null DTO at index "
                                + index
                                + " for Demand Planning DTO listing.");
            }
            if (demandPlanDTO.demandPlanId == null) {
                throw new IllegalStateException(
                        "Demand Plan mapper returned DTO without demandPlanId at index "
                                + index
                                + " for Demand Planning DTO listing.");
            }
        }

    }

    public DemandPlanDTO getDemandPlanDTO(Long demandPlanId) {
        DemandPlan demandPlan =
                demandPlanRepository.customFindByIdComPerfilExecucao(demandPlanId).get();

        /*
         * O mapper completo das linhas precisa de ParametrosGlobais para
         * resolver a UOM fallback de cada DemandPlanItem. Passar esse contexto
         * explicitamente impede o MapStruct de gerar DTOs de linha vazios para
         * a lista detalhada do Planning Book.
         */
        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        DemandPlanDTO demandPlanDTO = demandPlanAutoMapper.converte(
                demandPlan,
                parametrosGlobais);
        validaDemandPlanDTODetalhadoCarregadoCommunity(
                demandPlanDTO,
                demandPlan.getId());
        demandPlan = null; // libera memória
        
        // LIMPA OS CAMPOS = 0 PARA QUE NÃO SEJAM EXPORTADOS NO JSON
        for (DemandPlanItemDTO demandPlanItemDTO : demandPlanDTO.demandPlanDetail) {
            if (isZero(demandPlanItemDTO.baselineQtyConstrained)) demandPlanItemDTO.baselineQtyConstrained = null;
            if (isZero(demandPlanItemDTO.baselineQtyUnconstrained)) demandPlanItemDTO.baselineQtyUnconstrained = null;
            if (isZero(demandPlanItemDTO.demandAdjustmentQtyConstrained)) demandPlanItemDTO.demandAdjustmentQtyConstrained = null;
            if (isZero(demandPlanItemDTO.demandAdjustmentQtyUnconstrained)) demandPlanItemDTO.demandAdjustmentQtyUnconstrained = null;
            if (isZero(demandPlanItemDTO.totalQtyConstrained)) demandPlanItemDTO.totalQtyConstrained = null;
            if (isZero(demandPlanItemDTO.totalQtyUnconstrained)) demandPlanItemDTO.totalQtyUnconstrained = null;
        }
        return demandPlanDTO;
    }

    /**
     * Lista os períodos do snapshot persistido do Demand Plan.
     *
     * <p>Não consulta linhas, projections nem o relógio atual: o cabeçalho do
     * plano é a única fonte do calendário. Um snapshot incompleto deve falhar
     * em vez de devolver referências temporais variáveis.</p>
     */
    public List<DemandPlanPeriodDTO> getDemandPlanPeriodDTOList(Long demandPlanId) {

        DemandPlan demandPlan =
                demandPlanRepository.customFindByIdComPerfilExecucao(demandPlanId).get();
        LocalDateTime startDateTime = demandPlan.getDataInicioPlano();
        LocalDateTime endDateTime = demandPlan.getDataFimPlano();
        Constantes.TamanhoBucket bucketSize = demandPlan.getTamanhoBucketCadastrado();
        validaCabecalhoCalendarioDemandPlan(demandPlanId, startDateTime, endDateTime, bucketSize);

        Calendario calendario = Calendario.criaCalendarioDeDatas(
                bucketSize,
                startDateTime,
                startDateTime,
                endDateTime);
        List<DemandPlanPeriodDTO> demandPlanPeriodDTOList = new ArrayList<>();
        for (int periodIndex = calendario.getPosicaoPeriodoPresente();
             periodIndex <= calendario.getPosicaoPeriodoFinalFuturo();
             periodIndex++) {

            LocalDateTime periodStartDateTime = calendario.getPrimeiraDataHorarioPeriodo(periodIndex);
            demandPlanPeriodDTOList.add(new DemandPlanPeriodDTO(
                    periodIndex,
                    calendario.getDescricaoPeriodoDePosicaoPeriodo(periodIndex),
                    calendario.getTamanhoBucket(),
                    periodStartDateTime,
                    periodStartDateTime,
                    calendario.getUltimaDataHorarioPeriodo(periodIndex)));
        }
        return demandPlanPeriodDTOList;

    }

    /** Valida o cabeçalho antes de materializar o calendário determinístico. */
    private void validaCabecalhoCalendarioDemandPlan(
            Long demandPlanId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Constantes.TamanhoBucket bucketSize) {

        if (bucketSize == null) {
            throw new DemandPlanException("Demand Plan " + demandPlanId
                    + " has no bucket size for Demand Planning period list.");
        }
        if (startDateTime == null || endDateTime == null) {
            throw new DemandPlanException("Demand Plan " + demandPlanId
                    + " requires persisted start and end dates for Demand Planning period list.");
        }
        if (endDateTime.isBefore(startDateTime)) {
            throw new DemandPlanException("Demand Plan " + demandPlanId
                    + " has end date before start date for Demand Planning period list.");
        }
        if (!startDateTime.equals(Calendario.getPrimeiraDataHorarioPeriodo(startDateTime, bucketSize))) {
            throw new DemandPlanException("Demand Plan " + demandPlanId
                    + " has start date outside the bucket boundary for Demand Planning period list.");
        }
        if (!endDateTime.equals(Calendario.getUltimaDataHorarioPeriodo(endDateTime, bucketSize))) {
            throw new DemandPlanException("Demand Plan " + demandPlanId
                    + " has end date outside the bucket boundary for Demand Planning period list.");
        }

    }

    /**
     * Valida o DTO detalhado gerado pelo mapper antes de limpar zeros das
     * linhas.
     *
     * <p>Listagens usam `DemandPlanDTO` sem linhas, mas o detalhe sempre deve
     * trazer uma fotografia material/location, ainda que vazia. DTO nulo, id
     * ausente/divergente ou item nulo no detalhe indicam regressao do mapper e
     * devem falhar antes da iteracao que ajusta a serializacao JSON.</p>
     */
    private void validaDemandPlanDTODetalhadoCarregadoCommunity(
            DemandPlanDTO demandPlanDTO,
            Long demandPlanIdEsperado) {

        if (demandPlanIdEsperado == null) {
            throw new IllegalStateException(
                    "Demand Plan repository returned item without id for Demand Planning detail.");
        }
        if (demandPlanDTO == null) {
            throw new IllegalStateException(
                    "Demand Plan mapper returned null DTO for Demand Planning detail.");
        }
        if (demandPlanDTO.demandPlanId == null) {
            throw new IllegalStateException(
                    "Demand Plan mapper returned DTO without demandPlanId for Demand Planning detail.");
        }
        if (!demandPlanIdEsperado.equals(demandPlanDTO.demandPlanId)) {
            throw new IllegalStateException(
                    "Demand Plan mapper returned DTO with mismatched demandPlanId for Demand Planning detail.");
        }
        if (demandPlanDTO.demandPlanDetail == null) {
            throw new IllegalStateException(
                    "Demand Plan mapper returned null detail list for Demand Planning detail.");
        }

        for (int index = 0; index < demandPlanDTO.demandPlanDetail.size(); index++) {
            if (demandPlanDTO.demandPlanDetail.get(index) == null) {
                throw new IllegalStateException(
                        "Demand Plan mapper returned null detail line at index "
                                + index
                                + " for Demand Planning detail.");
            }
        }

    }

    /**
     * Indica se a quantidade deve ser omitida do JSON detalhado.
     */
    private boolean isZero(Double value) {

        return value != null && value == 0;

    }

    /**
     * Carrega o Demand Plan solicitado pela tela de detalhes.
     *
     * <p>Ausencia do plano e erro funcional da requisicao, nao excecao generica.
     * Falhar aqui, antes do mapper, produz mensagem clara para API/log e evita
     * que o front receba uma resposta vazia que pareca plano sem linhas.</p>
     */

    /**
     * Valida o snapshot de Demand Plan carregado para detalhes e ajustes.
     *
     * <p>As rotas de detalhe usam repository e a abertura do Planning Book usa
     * o service principal. Em ambos os casos, se o item vier nulo, sem id, com
     * id divergente ou sem perfil, a fachada deve falhar antes de calendario,
     * mapper, projections ou janela de edicao.</p>
     */
    /**
     * Valida os parametros globais antes de usá-los como contexto de calendario,
     * mapper ou unidade de medida.
     *
     * <p>Parametros globais ausentes indicam bootstrap/snapshot estrutural
     * incompleto. A fachada Community deve falhar aqui, com contexto funcional,
     * antes de deixar o erro aparecer como NPE em MapStruct, calendario ou
     * calculo de Planning Book.</p>
     */
    /**
     * Valida a projection estrutural de parametros, clusters e master data
     * usada pelas bordas de Demand Planning do front.
     *
     * <p>A projection pode estar vazia por erro de cache/bootstrap, e esse
     * cenario deve ser separado de uma view sem DFUs ou de um plano inexistente.
     * `ParametrosGlobais` tambem e parte obrigatoria dessa fotografia, pois
     * calendario, UOM e filtros dependem dele logo depois.</p>
     */
    /**
     * Valida a projection de unidades e conversoes usada pelo Planning Book.
     *
     * <p>Folhas e seleções agregadas podem estar na UOM configurada na view.
     * Por isso a projection de UOM precisa estar materializada antes de montar
     * filtros de sales, Key Figures ou persistir ajustes.</p>
     */
        
    
    
    
    
    
    
    /*
     * Planning Book de Demand Planning Community.
     *
     * A colaboracao e sempre material/location, com KFs padrao e sem
     * reference plan. As validacoes abaixo existem para proteger tambem
     * payloads manuais ou transicionais enviados ao backend.
     */
    public PlanningBookDTO getPlanningBookDTO(ConfiguredViewSelectionDTO configuredViewSelectionDTO, String userId) {

        PlanningBookDemandPlanningContext planningBookDemandPlanningContext =
                preparaContextoPlanningBookDemandPlanning(
                        configuredViewSelectionDTO,
                        userId);

        KeyFigureProjection keyFigureProjection = demandPlanningComparisonPlanSpi.getKeyFigureProjection(
                configuredViewSelectionDTO,
                planningBookDemandPlanningContext.demandPlan(),
                planningBookDemandPlanningContext.configuredViewProjection(),
                keyFigureProjectionFactory);

        return montaPlanningBookDTOParaLeitura(
                keyFigureProjection,
                planningBookDemandPlanningContext.configuredViewProjection(),
                configuredViewSelectionDTO);

    }

    /**
     * Reconstroi o recorte efetivo de uma abertura de Planning Book sem
     * materializar key figures ou alterar dados.
     *
     * <p>Overlays Enterprise usam esta leitura para validar uma selecao de pai
     * contra a mesma view, plano e filtro adicional de faturamento historico
     * usados pela grade. O metodo nao devolve escopo de escrita nem habilita
     * ajuste agregado no Community.</p>
     */
    public ConfiguredViewProjection getConfiguredViewProjectionPlanningBook(
            ConfiguredViewSelectionDTO configuredViewSelectionDTO,
            String userId) {

        return preparaContextoPlanningBookDemandPlanning(
                configuredViewSelectionDTO,
                userId).configuredViewProjection();

    }

    /**
     * Centraliza o preparo imutavel comum entre a abertura da grade e leituras
     * Enterprise que precisam validar seu recorte contra a mesma view.
     */
    private PlanningBookDemandPlanningContext preparaContextoPlanningBookDemandPlanning(
            ConfiguredViewSelectionDTO configuredViewSelectionDTO,
            String userId) {

        Long demandPlanId = validaConfiguredViewSelectionDemandPlanningBookCommunity(
                configuredViewSelectionDTO);
        
        DemandPlan demandPlan = demandPlanningService.getDemandPlanDeId(demandPlanId);
        
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        UnidadeMedidaProjection conversaoUnidadeMedidaProjection = conversaoUnidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        
        ConfiguredViewProjection configuredViewProjection = getConfiguredViewProjectionDemandPlanning(
                userId,
                configuredViewSelectionDTO.viewName,
                demandPlan,
                clusterEParametrosProjection,
                conversaoUnidadeMedidaProjection);
        
        validaConfiguredViewProjectionDemandPlanningBookCommunity(configuredViewProjection);

        return new PlanningBookDemandPlanningContext(
                demandPlan,
                configuredViewProjection);

    }

    /**
     * Dados read-only preparados uma unica vez para uma abertura de Planning
     * Book e para preflights Enterprise associados a ela.
     */
    private record PlanningBookDemandPlanningContext(
            DemandPlan demandPlan,
            ConfiguredViewProjection configuredViewProjection) {
    }

    /**
     * Valida o payload de abertura do Demand Planning Book antes de qualquer
     * cache, repository ou projection.
     *
     * <p>`referencePlanId` e comparacao entre planos sao Enterprise. Campos
     * estruturais vazios indicam payload manual/incompleto da SPA e devem
     * falhar como contrato invalido, nao como `NumberFormatException` ou NPE
     * em collaborator nao inicializado.</p>
     */
    private Long validaConfiguredViewSelectionDemandPlanningBookCommunity(
            ConfiguredViewSelectionDTO configuredViewSelectionDTO) {

        if (configuredViewSelectionDTO == null) {
            throw new IllegalArgumentException("Demand Planning Book view selection is required");
        }

        /*
         * Reference Plan e comparacao entre planos sao Enterprise. A consulta do
         * Planning Book Community deve falhar aqui, antes de carregar plano,
         * view, projections ou key figures.
         */
        demandPlanningComparisonPlanSpi.validateReferencePlanRequest(
                configuredViewSelectionDTO.referencePlanId);
        validaNiveisAgrupamentoPlanningBookCommunity(configuredViewSelectionDTO);

        if (configuredViewSelectionDTO.planId == null || configuredViewSelectionDTO.planId.isBlank()) {
            throw new IllegalArgumentException("Demand Planning Book plan id is required");
        }

        if (configuredViewSelectionDTO.viewName == null || configuredViewSelectionDTO.viewName.isBlank()) {
            throw new IllegalArgumentException("Demand Planning Book view name is required");
        }

        try {
            return Long.valueOf(configuredViewSelectionDTO.planId);
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException(
                    "Demand Planning Book plan id must be numeric: " + configuredViewSelectionDTO.planId,
                    numberFormatException);
        }

    }

    /**
     * Preserva a fronteira Community para os niveis de agregacao usados apenas
     * na leitura Enterprise do Planning Book.
     *
     * <p>Os ids chegam no payload de abertura como selecao transitoria: nao
     * sao gravados na view e nao participam do fluxo de ajuste. O overlay
     * Enterprise reabre somente o par completo para reagrupar o DTO final.</p>
     */
    protected void validaNiveisAgrupamentoPlanningBookCommunity(
            ConfiguredViewSelectionDTO configuredViewSelectionDTO) {

        if (configuredViewSelectionDTO.materialAggregationLevelId != null
                || configuredViewSelectionDTO.locationAggregationLevelId != null) {
            throw new RequiresEnterpriseVersionException(
                    "Demand Planning Planning Book aggregation levels");
        }

    }

    /**
     * Materializa a resposta de abertura do Planning Book.
     *
     * <p>O Community devolve a grade plana material/location. O hook recebe a
     * mesma projection ja montada para que o Enterprise possa somente
     * reorganizar suas folhas em grupos read-only, sem duplicar a montagem de
     * plano, calendario, filtros, key figures ou comparacao de planos.</p>
     */
    protected PlanningBookDTO montaPlanningBookDTOParaLeitura(
            KeyFigureProjection keyFigureProjection,
            ConfiguredViewProjection configuredViewProjection,
            ConfiguredViewSelectionDTO configuredViewSelectionDTO) {

        return planningBookService.getPlanningBookDTO(keyFigureProjection);

    }

    /**
     * Monta o retorno da escrita com a mesma projection ja recarregada.
     * Community devolve a grade folha; overlays podem somente reorganizar o
     * DTO, sem reconstruir projection ou consultar dados por celula.
     */
    protected PlanningBookDTO montaPlanningBookDTOAposAtualizacao(
            KeyFigureProjection keyFigureProjection,
            ConfiguredViewProjection configuredViewProjection,
            List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs) {

        return planningBookService.getPlanningBookDTO(keyFigureProjection);

    }

    /**
     * Garante que a view de Demand Planning ainda possui ao menos um material
     * e uma location depois de todos os filtros Community serem aplicados.
     *
     * <p>Projection nula ou sem DFU projection e erro estrutural da factory ou
     * cache. Escopo vazio, por outro lado, e erro funcional da configuracao da
     * view. Separar os dois casos evita NPE e preserva mensagem amigavel quando
     * o usuario realmente filtrou tudo.</p>
     */
    private void validaConfiguredViewProjectionDemandPlanningBookCommunity(
            ConfiguredViewProjection configuredViewProjection) {

        if (configuredViewProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning Book display requires configured view projection");
        }
        if (configuredViewProjection.getDfuProjectionFiltrado() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning Book display requires DFU projection in configured view projection");
        }
        if (configuredViewProjection.getLocationsFiltradas().isEmpty()) {
            throw new DemandPlanException("No locations conform to the filters. Please review the filters in the Admin -> User Data View menu");
        }

        if (configuredViewProjection.getMateriaisFiltrados().isEmpty()) {
            throw new DemandPlanException("No materials conform to the filters. Please review the filters in the Admin -> User Data View menu");
        }

    }

    public PlanningBookDTO atualizaDemandPlan(List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs, String userId)
            throws UnitOfMeasureConversionException, IncompatibleCalendarException {

        return atualizaDemandPlanInterno(
                selectedPlanningBookCellDTOs,
                userId,
                false);

    }

    /**
     * Atualiza um lote de Planning Book de forma atomica para consumidores
     * internos que precisam rejeitar o arquivo inteiro diante da primeira
     * inconsistencia funcional.
     *
     * <p>Este nao e um endpoint nem altera o contrato tolerante da UI. Antes
     * de persistir a primeira celula, a fachada monta as mesmas projections do
     * fluxo normal e percorre toda a lista para validar escopo, key figure,
     * permissao e horizonte congelado. A segunda passada aplica os ajustes sem
     * capturar excecoes por celula; qualquer erro faz a excecao escapar e a
     * transacao reverte todas as escritas feitas no lote.</p>
     *
     * @param selectedPlanningBookCellDTOs celulas de uma unica view e plano
     * @param userId usuario responsavel pelo lote interno
     * @return Planning Book recarregado depois da gravacao atomica
     */
    @Transactional(rollbackFor = Exception.class)
    public PlanningBookDTO atualizaDemandPlanEmLoteEstrito(
            List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs,
            String userId) throws UnitOfMeasureConversionException, IncompatibleCalendarException {

        return atualizaDemandPlanInterno(
                selectedPlanningBookCellDTOs,
                userId,
                true);

    }

    /**
     * Implementacao compartilhada pelos contratos tolerante da UI e estrito
     * dos consumidores internos.
     *
     * <p>O booleano fica privado para que a distincao de contrato seja sempre
     * expressa pelos dois metodos publicos. Overlays Enterprise podem aplicar
     * seus gates antes de delegar para o metodo estrito sem criar um endpoint
     * Community adicional.</p>
     */
    private PlanningBookDTO atualizaDemandPlanInterno(
            List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs,
            String userId,
            boolean processamentoEstrito)
            throws UnitOfMeasureConversionException, IncompatibleCalendarException {
        
        log.trace("Modificação feita Planning Book DP por usuário " + userId + " ----------------");

        validaReferencePlanPlanningBookCommunity(selectedPlanningBookCellDTOs);

        DemandPlan demandPlan = demandPlanRepository.customFindByIdComPerfilExecucao(
                selectedPlanningBookCellDTOs.get(0).planId).get();

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        UnidadeMedidaProjection unidadeMedidaProjection = conversaoUnidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();

        if (LocalDate.now().isAfter(demandPlan.getDataFimEdicao()) || LocalDate.now().isBefore(demandPlan.getDataInicioEdicao())) {
            throw new AccessDeniedException("A janela para ajustes no plano de demanda se encerrou (habilitada do dia " + demandPlan.getDataInicioEdicao().toString() + " ao " + demandPlan.getDataFimEdicao().toString()+ ").");
        }

        ParametrosDemandPlanProjection parametrosDemandPlanProjection =
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjectionDeCache(
                                demandPlan.getPerfilExecucaoDemandPlan());

        ConfiguredView configuredView = configuredViewService.getConfiguredView(
                userId,
                selectedPlanningBookCellDTOs.get(0).viewName,
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK);

        ConfiguredViewProjection configuredViewProjectionSemFiltroFaturamentoHistorico = configuredViewProjectionFactory.getConfiguredViewProjection(
                configuredView,
                clusterEParametrosProjection);

        // O filtro de faturamento historico deve respeitar primeiro os filtros da propria
        // view para evitar uma consolidacao de sellout em toda a malha ativa.
        BiFunction<Produto,Location,Boolean> filtroFaturamentoHistorico = getFiltroFaturamentoHistorico(
                configuredView,
                demandPlan,
                configuredViewProjectionSemFiltroFaturamentoHistorico.getLocationsFiltradas(),
                configuredViewProjectionSemFiltroFaturamentoHistorico.getMateriaisFiltrados(),
                clusterEParametrosProjection,
                unidadeMedidaProjection);

        ConfiguredViewProjection configuredViewProjectionComSelecaoAAtualizar = configuredViewService.getConfiguredViewProjectionComDTOSelecaoCelulas(
                userId,
                selectedPlanningBookCellDTOs.get(0).viewName,
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK,
                demandPlan.getTamanhoBucket(),
                selectedPlanningBookCellDTOs,
                clusterEParametrosProjection,
                filtroFaturamentoHistorico);

        DemandPlanningProjection demandPlanningProjection = demandPlanProjectionFactory.getDemandPlanningProjectionCompleto(
                demandPlan,
                configuredViewProjectionComSelecaoAAtualizar.getDfuProjectionFiltrado(),
                unidadeMedidaProjection,
                clusterEParametrosProjection,
                parametrosDemandPlanProjection,
                false);

        PlanningBookDemandAdjustmentContext adjustmentContext = preparaContextoAjustePlanningBookDemand(
                configuredViewProjectionComSelecaoAAtualizar,
                demandPlanningProjection);
        preValidaLoteAjustePlanningBookDemand(
                configuredViewProjectionComSelecaoAAtualizar,
                demandPlanningProjection,
                adjustmentContext);

        if (processamentoEstrito) {
            validaLoteEstritoAjustePlanningBookDemand(
                    configuredViewProjectionComSelecaoAAtualizar,
                    demandPlanningProjection,
                    adjustmentContext);
        }

        // Realiza as atualizações para cada célula selecionada no Planning Book.
        // O escopo pode representar uma folha ou uma seleção agregada de DFUs.
        
        // Cada DetalheSelecao foi criado a partir de um SelectedPlanningBookCellDTO.
        for (AjusteCelulaPlanningBook ajusteCelulaPlanningBook
                : configuredViewProjectionComSelecaoAAtualizar.getDetalhesSelecaoAAtualizar()) {

            if (processamentoEstrito) {
                // A primitiva interna nao registra erro por celula: a excecao
                // precisa escapar para que a transacao reverta o lote inteiro.
                atualizaDemandPlanningProjection(
                        ajusteCelulaPlanningBook,
                        configuredViewProjectionComSelecaoAAtualizar,
                        demandPlanningProjection,
                        adjustmentContext);
                continue;
            }

            try {
                // Atualiza o escopo de folha ou agregado aprovado para o Community.
                atualizaDemandPlanningProjection(
                        ajusteCelulaPlanningBook,
                        configuredViewProjectionComSelecaoAAtualizar,
                        demandPlanningProjection,
                        adjustmentContext);
            } catch (RuntimeException e) {
                // Uma celula invalida nao deve impedir a atualizacao das demais
                // celulas da mesma chamada do Planning Book. Erros funcionais
                // ficam associados ao detalhe de selecao para retorno a SPA.
                log.error("Erro ao atualizar celula do Demand Planning Book Community", e);
                configuredViewProjectionComSelecaoAAtualizar.getErroAtualizacaoPorDetalheSelecao().put(ajusteCelulaPlanningBook, e.getMessage());
            }
            
        }

        finalizaContextoAjustePlanningBookDemand(adjustmentContext);
        
        // Custom Key Figures e propagação por lista técnica são recursos Enterprise.
        // No Community, a validação de cada célula bloqueia inputs em custom KFs
        // antes de qualquer persistência, então não há pós-processamento adicional.


        // gera o key figure projection após as alterações no plano e retorna DTOs
        // Recria a projection apos a persistencia para devolver ao Planning Book uma visao consistente
        // com o estado salvo. A reutilizacao da projection de entrada pode ser avaliada depois, mas nao
        // deve alterar o contrato funcional Community.
        KeyFigureProjection keyFigureProjection = keyFigureProjectionFactory.getKeyFigureProjectionDeDemandPlan(
                demandPlan,
                configuredViewProjectionComSelecaoAAtualizar,
                false);

        return montaPlanningBookDTOAposAtualizacao(
                keyFigureProjection,
                configuredViewProjectionComSelecaoAAtualizar,
                selectedPlanningBookCellDTOs);

    }

    /**
     * Valida todas as celulas depois de a fotografia oficial do lote estar
     * pronta, mas antes de a primeira chamada de persistencia.
     *
     * <p>O metodo deliberadamente nao despacha nem calcula o delta final de
     * cada key figure: essas etapas podem depender dos ajustes anteriores da
     * mesma lista. Ele antecipa todas as guardas que independem dessa ordem e
     * deixa a segunda passada protegida pela transacao estrita.</p>
     */
    protected void validaLoteEstritoAjustePlanningBookDemand(
            ConfiguredViewProjection configuredViewProjection,
            DemandPlanningProjection demandPlanningProjection,
            PlanningBookDemandAdjustmentContext adjustmentContext) {

        for (AjusteCelulaPlanningBook ajusteCelulaPlanningBook
                : configuredViewProjection.getDetalhesSelecaoAAtualizar()) {
            KeyFigureInterface keyFigureAjustada =
                    validaAjusteDemandPlanningProjectionAntesDaPersistencia(
                    ajusteCelulaPlanningBook,
                    configuredViewProjection,
                    demandPlanningProjection,
                    adjustmentContext);
            validaDespachoAjusteDiretoPlanningBookAntesDaPersistencia(
                    ajusteCelulaPlanningBook,
                    keyFigureAjustada,
                    configuredViewProjection,
                    demandPlanningProjection,
                    adjustmentContext);
        }

    }

    /**
     * Permite que overlays validem invariantes de lote depois de montar as
     * projections oficiais e antes do loop Community que trata erros por célula.
     */
    protected void preValidaLoteAjustePlanningBookDemand(
            ConfiguredViewProjection configuredViewProjection,
            DemandPlanningProjection demandPlanningProjection,
            PlanningBookDemandAdjustmentContext adjustmentContext) {

        // Community nao possui validacao adicional de lote.

    }

    /**
     * Valida a projection de parametros de Demand Planning antes de montar a
     * projection editavel do Planning Book.
     *
     * <p>O ajuste Community ja sabe qual plano e view serao usados, mas ainda
     * depende da fotografia de parametros do perfil para construir a
     * `DemandPlanningProjection`. Se a factory/cache retornar nulo, a chamada
     * deve falhar aqui, antes de `ConfiguredView`, `DemandPlanProjectionFactory`
     * ou persistencia parcial de ajustes.</p>
     */
    /**
     * Reference Plan e comparacao entre planos pertencem ao Enterprise.
     *
     * <p>Payloads legados/transicionais podem manter `referencePlanId` em cada
     * celula selecionada. O Community precisa validar a lista completa antes de
     * carregar projections ou persistir ajustes, porque payloads manuais podem
     * misturar celulas sem referencia no primeiro item e com referencia nos
     * itens seguintes. A mesma borda tambem valida lista nula ou vazia para
     * impedir falhas tecnicas no acesso posterior a primeira celula.</p>
     */
    private void validaReferencePlanPlanningBookCommunity(
            List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs) {

        if (selectedPlanningBookCellDTOs == null) {
            throw new IllegalArgumentException("Demand Planning Planning Book adjustment cells are required");
        }

        if (selectedPlanningBookCellDTOs.isEmpty()) {
            throw new IllegalArgumentException("At least one Demand Planning Planning Book adjustment cell is required.");
        }

        int indiceSelectedPlanningBookCellDTO = 0;
        for (SelectedPlanningBookCellDTO selectedPlanningBookCellDTO : selectedPlanningBookCellDTOs) {
            if (selectedPlanningBookCellDTO == null) {
                throw new IllegalArgumentException(
                        "Demand Planning Planning Book adjustment cells cannot contain null value at index "
                                + indiceSelectedPlanningBookCellDTO
                                + ".");
            }
            indiceSelectedPlanningBookCellDTO++;
        }

        if (selectedPlanningBookCellDTOs.stream()
                .anyMatch(selectedPlanningBookCellDTO -> selectedPlanningBookCellDTO.referencePlanId != null)) {
            throw new RequiresEnterpriseVersionException("Demand Planning reference plan");
        }

        SelectedPlanningBookCellDTO selectedPlanningBookCellDTOReferencia = selectedPlanningBookCellDTOs.get(0);

        for (SelectedPlanningBookCellDTO selectedPlanningBookCellDTO : selectedPlanningBookCellDTOs) {

            if (!Objects.equals(selectedPlanningBookCellDTOReferencia.planId, selectedPlanningBookCellDTO.planId)) {
                throw new IllegalArgumentException("Demand Planning Planning Book adjustment requires the same plan for all selected cells.");
            }

            if (!Objects.equals(selectedPlanningBookCellDTOReferencia.viewName, selectedPlanningBookCellDTO.viewName)) {
                throw new IllegalArgumentException("Demand Planning Planning Book adjustment requires the same view for all selected cells.");
            }
        }

    }

    /**
     * Valida o shape minimo de cada celula antes de qualquer carregamento de
     * plano, view ou projection.
     *
     * <p>Demand Planning Book permite atualizar varias celulas por chamada,
     * mas todas precisam pertencer ao mesmo plano e a mesma view. Periodo, key
     * figure, UOM e valores sao parte do ajuste atomico; deixar algum deles
     * nulo empurraria o erro para calendario/conversao/persistencia.</p>
     */
        protected PlanningBookDemandAdjustmentContext preparaContextoAjustePlanningBookDemand(
            ConfiguredViewProjection configuredViewProjection,
            DemandPlanningProjection demandPlanningProjection) {

        PlanningBookDemandAdjustmentContext adjustmentContext = new PlanningBookDemandAdjustmentContext();
        adjustmentContext.setPlanningBookViewName(configuredViewProjection.getConfiguredView().getNomeView());
        return adjustmentContext;

    }

    /**
     * Finaliza o lote depois que cada celula recebeu sua validacao individual.
     *
     * <p>O Community nao acumula escrita privada. O hook permite que o
     * Enterprise execute um unico {@code saveAll} para linhas privadas ja
     * pre-carregadas no contexto, sem criar estado de requisicao no bean
     * singleton.</p>
     */
    protected void finalizaContextoAjustePlanningBookDemand(
            PlanningBookDemandAdjustmentContext adjustmentContext) {

        // Community nao possui linhas privadas de Custom Key Figure.

    }

    /**
     * Resolve uma key figure do lote. O fallback Community preserva o catalogo
     * standard; overlays podem oferecer uma fotografia batch de Custom Key Figures.
     */
    protected KeyFigureInterface resolveKeyFigureAjustePlanningBookDemand(
            String keyFigureId,
            PlanningBookDemandAdjustmentContext adjustmentContext) {

        KeyFigureInterface keyFigure = adjustmentContext == null
                ? null
                : adjustmentContext.getKeyFigureOrNull(keyFigureId);
        return keyFigure == null ? keyFigureService.getKeyFigureDeId(keyFigureId) : keyFigure;

    }

    protected void atualizaDemandPlanningProjection(
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook,
            ConfiguredViewProjection configuredViewProjection,
            DemandPlanningProjection demandPlanningProjection,
            PlanningBookDemandAdjustmentContext adjustmentContext) {

        KeyFigureInterface keyFigureAjustada =
                validaAjusteDemandPlanningProjectionAntesDaPersistencia(
                        ajusteCelulaPlanningBook,
                        configuredViewProjection,
                        demandPlanningProjection,
                        adjustmentContext);

        ConfiguredView configuredView = configuredViewProjection.getConfiguredView();

        /*
         * O dispatcher e executado somente depois de validar a celula,
         * permissao, horizonte congelado e o escopo material/location. O
         * Community nao possui workflow e devolve false; o Enterprise pode
         * desviar Direct Demand para a KF de ajuste da etapa sem duplicar o
         * loop, a captura de erros por celula ou a validacao comum.
         */
        if (despachaAjusteDiretoDemandPlanningBook(
                ajusteCelulaPlanningBook,
                keyFigureAjustada,
                configuredViewProjection,
                demandPlanningProjection,
                adjustmentContext)) {
            return;
        }

        atualizaDemandPlanEmKeyFigure(
                ajusteCelulaPlanningBook,
                demandPlanningProjection,
                configuredView.getUserId(),
                adjustmentContext);

    }

    /**
     * Executa as guardas de uma celula que nao dependem da ordem de aplicacao
     * do lote nem mutam a projection de demanda.
     *
     * <p>O modo estrito usa esta mesma rotina numa primeira passada por toda a
     * lista. O fluxo tolerante tambem a usa imediatamente antes de aplicar a
     * celula, preservando as mensagens e o comportamento existentes da UI.</p>
     *
     * @return key figure ja resolvida e aprovada para o dispatcher
     */
    protected KeyFigureInterface validaAjusteDemandPlanningProjectionAntesDaPersistencia(
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook,
            ConfiguredViewProjection configuredViewProjection,
            DemandPlanningProjection demandPlanningProjection,
            PlanningBookDemandAdjustmentContext adjustmentContext) {

        ClusterEParametrosProjection clusterEParametrosProjection = demandPlanningProjection.getClusterEParametrosProjection();
        ConfiguredView configuredView = configuredViewProjection.getConfiguredView();
        Boolean permiteAlteracaoHorizonteCongelado = configuredView.getPermiteAlteracaoHorizonteCongelado();

        PlanningBookDfuScope planningBookDfuScope = ajusteCelulaPlanningBook.getPlanningBookDfuScope();
        KeyFigureInterface keyFigureAjustada = resolveKeyFigureAjustePlanningBookDemand(
                ajusteCelulaPlanningBook.getKeyFigureId(),
                adjustmentContext);
        validaKeyFigureAjustePlanningBookCommunity(keyFigureAjustada);
        validaPermissaoKeyFigureAjustePlanningBookDemand(
                keyFigureAjustada,
                configuredViewProjection,
                adjustmentContext);
        
        // checa se ajuste foi feito dentro do horizonte congelado
        Calendario calendarioDemandPlan = demandPlanningProjection.getCalendario();
        if (!permiteAlteracaoHorizonteCongelado && !DemandPlanning.verificaSeAjusteDentroHorizonteCongelado(
                ajusteCelulaPlanningBook.getDataHorarioReferencia().toLocalDate(),
                planningBookDfuScope.getLocations(),
                planningBookDfuScope.getMateriais(),
                calendarioDemandPlan,
                clusterEParametrosProjection)) throw new AccessDeniedException("The adjustment was made inside the frozen horizon");

        // Permissão por key figure configurada na User View é Enterprise.
        // O Community usa apenas o modo padrão de edição de cada KF standard.
        validaFiltroDFUProjectionPlanningBookDemand(
                planningBookDfuScope.getNovoDFUProjectionInterseccaoComProjectionFiltro(
                        demandPlanningProjection.getFiltroDfuProjection(),
                        clusterEParametrosProjection));

        return keyFigureAjustada;

    }

    /**
     * Hook sem escrita para guards adicionais do dispatcher de Direct Demand.
     *
     * <p>O Community nao possui dispatcher privado. O Enterprise o usa para
     * antecipar no modo estrito as precondicoes de workflow e Custom Key
     * Figure que, no caminho tolerante, continuam sendo verificadas durante o
     * despacho da celula.</p>
     */
    protected void validaDespachoAjusteDiretoPlanningBookAntesDaPersistencia(
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook,
            KeyFigureInterface keyFigureAjustada,
            ConfiguredViewProjection configuredViewProjection,
            DemandPlanningProjection demandPlanningProjection,
            PlanningBookDemandAdjustmentContext adjustmentContext) {

        // Community nao possui condicoes privadas de dispatcher.

    }

    /**
     * Guarda de permissao especifica da key figure. No Community a lista de
     * KFs e fixa e o comportamento padrao ja e suficiente; overlays privados
     * devem validar qualquer permissao persistida antes da escrita.
     */
    protected void validaPermissaoKeyFigureAjustePlanningBookDemand(
            KeyFigureInterface keyFigureAjustada,
            ConfiguredViewProjection configuredViewProjection,
            PlanningBookDemandAdjustmentContext adjustmentContext) {

        // Community nao possui permissao por key figure configurada.

    }

    /**
     * Permite que um overlay trate uma celula totalizadora depois que a borda
     * comum ja validou payload, escopo DFU, horizonte congelado e permissao.
     *
     * <p>O Community nao tem etapa de workflow nem destino privado para
     * {@code Direct Demand}; por isso preserva a persistencia standard. O
     * retorno {@code true} informa que o overlay concluiu a escrita material/
     * location e impede que o caminho comum aplique um segundo ajuste.</p>
     */
    protected boolean despachaAjusteDiretoDemandPlanningBook(
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook,
            KeyFigureInterface keyFigureAjustada,
            ConfiguredViewProjection configuredViewProjection,
            DemandPlanningProjection demandPlanningProjection,
            PlanningBookDemandAdjustmentContext adjustmentContext) {

        return false;

    }

    protected void validaKeyFigureAjustePlanningBookCommunity(KeyFigureInterface keyFigureAjustada) {

        // O Community aceita ajustes apenas nas key figures standard.
        // Qualquer outra implementacao indica configuracao customizada do
        // Planning Book, que pertence ao OpsFactor Enterprise.
        if (!(keyFigureAjustada instanceof KeyFigureStandard)) throw new RequiresEnterpriseVersionException(
                "Demand Planning custom key figure adjustment");

        KeyFigureStandard keyFigureAjustadaStandard = (KeyFigureStandard) keyFigureAjustada;
        KeyFigureStandardEnum keyFigureStandardEnum = keyFigureAjustadaStandard.getKeyFigureStandardEnum();
        if (permiteAjusteKeyFigureStandardPlanningBookDemandPlanning(keyFigureStandardEnum)) return;

        switch (keyFigureStandardEnum) {
            case ITENS_NOVOS ->
                    throw new RequiresEnterpriseVersionException("Demand Planning new materials key figure adjustment");
            case UPLIFT ->
                    throw new RequiresEnterpriseVersionException("Demand Planning uplift key figure adjustment");
            case CARTEIRA ->
                    throw new RequiresEnterpriseVersionException("Demand Planning customer orders key figure adjustment");
            default ->
                    throw new RequiresEnterpriseVersionException("Demand Planning key figure adjustment " + keyFigureStandardEnum);
        }

    }

    /**
     * Define quais KFs standard podem ser selecionadas como alvo de ajuste no
     * Planning Book de demanda desta edicao.
     *
     * <p>O Community libera apenas a colaboracao aberta: `Baseline`, `Demand
     * Adjustment` e `Direct Demand` como linha totalizadora. Overlays Enterprise
     * podem reabrir KFs privadas ja migradas sem duplicar a validacao de
     * payload, congelamento, escopo DFU e persistencia da fachada base.</p>
     */
    protected boolean permiteAjusteKeyFigureStandardPlanningBookDemandPlanning(
            KeyFigureStandardEnum keyFigureStandardEnum) {

        return switch (keyFigureStandardEnum) {
            case BASELINE, AJUSTE_DEMANDA, DEMANDA_DIRETA_TOTAL_DP -> true;
            default -> false;
        };

    }

    protected void validaKeyFiguresBaseTotalizacaoPlanningBookCommunity(
            Collection<KeyFigureInterface> keyFiguresBaseTotalizacao,
            String contextoTotalizacao) {

        if (keyFiguresBaseTotalizacao == null || keyFiguresBaseTotalizacao.isEmpty()) {
            throw new IllegalArgumentException(contextoTotalizacao + " deve informar ao menos uma key figure base");
        }

        /*
         * As listas de totalizacao e split sao pontos tecnicos internos do
         * Planning Book. No Community elas devem conter somente Baseline e
         * Demand Adjustment. Direct Demand e uma KF totalizadora de UI, enquanto
         * New Materials, Uplift, Customer Orders e custom KFs pertencem ao
         * Enterprise e nao podem entrar no calculo de proporcao. O enum ainda
         * carrega o rótulo transicional New Products para payloads legados.
         *
         * O metodo e protected porque o Enterprise pode reabrir uma KF base ja
         * migrada, como New Products, sem duplicar todo o fluxo de ajuste do
         * Planning Book Community.
         */
        for (KeyFigureInterface keyFigureBaseTotalizacao : keyFiguresBaseTotalizacao) {
            validaKeyFigureBaseTotalizacaoPlanningBookCommunity(
                    keyFigureBaseTotalizacao,
                    contextoTotalizacao);
        }

    }

    protected void validaKeyFigureBaseTotalizacaoPlanningBookCommunity(
            KeyFigureInterface keyFigureBaseTotalizacao,
            String contextoTotalizacao) {

        if (!(keyFigureBaseTotalizacao instanceof KeyFigureStandard)) {
            throw new RequiresEnterpriseVersionException(contextoTotalizacao + " custom key figure");
        }

        KeyFigureStandard keyFigureBaseTotalizacaoStandard = (KeyFigureStandard) keyFigureBaseTotalizacao;
        switch (keyFigureBaseTotalizacaoStandard.getKeyFigureStandardEnum()) {
            case BASELINE, AJUSTE_DEMANDA -> {
                // KFs base permitidas para totalizar e distribuir ajustes Community.
            }
            case ITENS_NOVOS ->
                    throw new RequiresEnterpriseVersionException(contextoTotalizacao + " new materials key figure");
            case UPLIFT ->
                    throw new RequiresEnterpriseVersionException(contextoTotalizacao + " uplift key figure");
            case CARTEIRA ->
                    throw new RequiresEnterpriseVersionException(contextoTotalizacao + " customer orders key figure");
            default -> throw new IllegalArgumentException(contextoTotalizacao
                    + " aceita apenas Baseline e Demand Adjustment, nao "
                    + keyFigureBaseTotalizacaoStandard.getKeyFigureStandardEnum());
        }

    }

    public void atualizaDemandPlanEmKeyFigure(
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook,
            DemandPlanningProjection demandPlanningProjection,
            String userId) {

        atualizaDemandPlanEmKeyFigure(
                ajusteCelulaPlanningBook,
                demandPlanningProjection,
                userId,
                new PlanningBookDemandAdjustmentContext());

    }

    /**
     * Executa o ajuste usando a fotografia de key figures preparada para o
     * lote. A assinatura protegida evita lookup individual de KFs privadas em
     * overlays Enterprise, sem abrir esse catalogo para o Community.
     */
    protected void atualizaDemandPlanEmKeyFigure(
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook,
            DemandPlanningProjection demandPlanningProjection,
            String userId,
            PlanningBookDemandAdjustmentContext adjustmentContext) {

        KeyFigureInterface keyFigureQueReceberaAjuste = keyFigureService.getKeyFigureAjusteDemandaCommunity(
                resolveKeyFigureAjustePlanningBookDemand(
                        ajusteCelulaPlanningBook.getKeyFigureId(),
                        adjustmentContext));
        List<KeyFigureInterface> keyFiguresQueTotalizamDemandaDireta = keyFigureService.getKeyFiguresDpQueCompoemDemandaDireta();

        atualizaDemandPlanEmKeyFigure(
                keyFigureQueReceberaAjuste,
                ajusteCelulaPlanningBook,
                keyFiguresQueTotalizamDemandaDireta,
                demandPlanningProjection,
                userId,
                adjustmentContext);

    }

    /**
     *
     * @param keyFigureQueReceberaAjustesNaDemandaDiretaTotal apenas usado se keyFigureInputAjuste for standard do tipo DEMANDA_DIRETA_TOTAL_DP
     * @param ajusteCelulaPlanningBook informações gerais do ajuste : valor anterior/atual, data, agrupamento caracteristicas, unidade de medida, key figure
     * @param keyFiguresQueTotalizamDemandaDireta
     * @param demandPlanningProjection
     * @param userId
     */
    public void atualizaDemandPlanEmKeyFigure(
            KeyFigureInterface keyFigureQueReceberaAjustesNaDemandaDiretaTotal,
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook,
            List<KeyFigureInterface> keyFiguresQueTotalizamDemandaDireta,
            DemandPlanningProjection demandPlanningProjection,
            String userId) {

        atualizaDemandPlanEmKeyFigure(
                keyFigureQueReceberaAjustesNaDemandaDiretaTotal,
                ajusteCelulaPlanningBook,
                keyFiguresQueTotalizamDemandaDireta,
                demandPlanningProjection,
                userId,
                null);

    }

    /** Variante interna que conserva o contexto da view para extensões privadas. */
    protected void atualizaDemandPlanEmKeyFigure(
            KeyFigureInterface keyFigureQueReceberaAjustesNaDemandaDiretaTotal,
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook,
            List<KeyFigureInterface> keyFiguresQueTotalizamDemandaDireta,
            DemandPlanningProjection demandPlanningProjection,
            String userId,
            @Nullable PlanningBookDemandAdjustmentContext adjustmentContext) {

        KeyFigureInterface keyFigureInputAjuste = keyFigureService.getKeyFigureDeId(ajusteCelulaPlanningBook.getKeyFigureId());
        ClusterEParametrosProjection clusterEParametrosProjection = demandPlanningProjection.getClusterEParametrosProjection();

        validaKeyFigureAjustePlanningBookCommunity(keyFigureInputAjuste);

        UnidadeMedida unidadeMedidaAjuste = demandPlanningProjection.getUnidadeMedidaProjection()
                .getUnidadeMedidaFromId(ajusteCelulaPlanningBook.getUomId());

        PlanningBookDfuScope planningBookDfuScope = ajusteCelulaPlanningBook.getPlanningBookDfuScope();
        FiltroDFUProjection filtroDFUProjection = planningBookDfuScope.getNovoDFUProjectionInterseccaoComProjectionFiltro(
                demandPlanningProjection.getFiltroDfuProjection(),
                clusterEParametrosProjection);

        if (!(keyFigureInputAjuste instanceof KeyFigureStandard)) {
            throw new RequiresEnterpriseVersionException("Demand Planning custom key figure adjustment");
        } else {
            KeyFigureStandard keyFigureInputAjusteStandard = (KeyFigureStandard) keyFigureInputAjuste;
            switch (keyFigureInputAjusteStandard.getKeyFigureStandardEnum()) {
                case DEMANDA_DIRETA_TOTAL_DP -> {
                    atualizaDemandPlanEmKeyFigure(
                        keyFiguresQueTotalizamDemandaDireta, // ajuste relativo à soma das KFs Community
                        keyFigureQueReceberaAjustesNaDemandaDiretaTotal,
                        keyFiguresQueTotalizamDemandaDireta, // split seguirá proporção do valor total das KFs Community
                        ajusteCelulaPlanningBook.getDataHorarioReferencia(),
                        ajusteCelulaPlanningBook.getValorNovo(),
                        unidadeMedidaAjuste,
                        demandPlanningProjection,
                        filtroDFUProjection,
                        userId,
                        planningBookDfuScope.getDescricaoEscopo(),
                        ajusteCelulaPlanningBook,
                        adjustmentContext == null ? null : adjustmentContext.getPlanningBookViewName());
                }
                default -> atualizaDemandPlanEmKeyFigure(
                        List.of(keyFigureInputAjuste), // ajuste restrito à KF indicada
                        keyFigureInputAjuste,
                        keyFiguresQueTotalizamDemandaDireta, // split seguirá proporção do valor total das KFs Community
                        ajusteCelulaPlanningBook.getDataHorarioReferencia(),
                        ajusteCelulaPlanningBook.getValorNovo(),
                        unidadeMedidaAjuste,
                        demandPlanningProjection,
                        filtroDFUProjection,
                        userId,
                        planningBookDfuScope.getDescricaoEscopo(),
                        ajusteCelulaPlanningBook,
                        adjustmentContext == null ? null : adjustmentContext.getPlanningBookViewName());
            }
        }

    }

    public void atualizaDemandPlanEmKeyFigure(
            Collection<KeyFigureInterface> keyFiguresOndeFoiAplicadoAjusteTotal, // no Community, Direct Demand totaliza BASELINE + AJUSTE_DEMANDA
            KeyFigureInterface keyFigureQueReceberaAjusteNoTotal, // ex. o delta do ajuste deverá recair sobre AJUSTE_DEMANDA
            Collection<KeyFigureInterface> keyFiguresReferenciaSplitEntreDfus, // tipicamente igual a keyFiguresOndeFoiAplicadoAjusteTotal
            LocalDateTime dataHorarioReferencia,
            double valorNovo,
            UnidadeMedida unidadeMedidaValor,
            DemandPlanningProjection demandPlanningProjection,
            FiltroDFUProjection filtroDFUProjection,
            String userId,
            String descricaoNivelAjustePlano // ex. descricao do agrupamento de características
    ) {

        atualizaDemandPlanEmKeyFigure(
                keyFiguresOndeFoiAplicadoAjusteTotal,
                keyFigureQueReceberaAjusteNoTotal,
                keyFiguresReferenciaSplitEntreDfus,
                dataHorarioReferencia,
                valorNovo,
                unidadeMedidaValor,
                demandPlanningProjection,
                filtroDFUProjection,
                userId,
                descricaoNivelAjustePlano,
                null,
                null);

    }

    /**
     * Variante interna que preserva a célula original quando a alteração
     * física decorre de uma totalização, conversão ou rateio do Planning Book.
     */
    protected void atualizaDemandPlanEmKeyFigure(
            Collection<KeyFigureInterface> keyFiguresOndeFoiAplicadoAjusteTotal,
            KeyFigureInterface keyFigureQueReceberaAjusteNoTotal,
            Collection<KeyFigureInterface> keyFiguresReferenciaSplitEntreDfus,
            LocalDateTime dataHorarioReferencia,
            double valorNovo,
            UnidadeMedida unidadeMedidaValor,
            DemandPlanningProjection demandPlanningProjection,
            FiltroDFUProjection filtroDFUProjection,
            String userId,
            String descricaoNivelAjustePlano,
            @Nullable AjusteCelulaPlanningBook ajusteCelulaPlanningBookOriginal,
            @Nullable String planningBookViewName) {

        validaKeyFiguresBaseTotalizacaoPlanningBookCommunity(
                keyFiguresOndeFoiAplicadoAjusteTotal,
                "Demand Planning totalized key figures");
        validaKeyFigureAjustePlanningBookCommunity(keyFigureQueReceberaAjusteNoTotal);
        validaKeyFiguresBaseTotalizacaoPlanningBookCommunity(
                keyFiguresReferenciaSplitEntreDfus,
                "Demand Planning split reference key figures");
        validaFiltroDFUProjectionPlanningBookDemand(filtroDFUProjection);

        ClusterEParametrosProjection clusterEParametrosProjection = demandPlanningProjection.getClusterEParametrosProjection();

        Integer posicaoPeriodo = demandPlanningProjection.getCalendario().getPosicaoPeriodo(dataHorarioReferencia);

        double valorAtualKeyFiguresOndeFoiAplicadoAjusteTotal = demandPlanningProjection.getValorTotalKeyFigures(
                keyFiguresOndeFoiAplicadoAjusteTotal,
                posicaoPeriodo,
                filtroDFUProjection,
                unidadeMedidaValor);

        validaAjusteAgregadoSemReferenciaPlanningBook(
                valorNovo,
                valorAtualKeyFiguresOndeFoiAplicadoAjusteTotal,
                filtroDFUProjection,
                clusterEParametrosProjection);

        double quantidadeTotalEmKeyFiguresTotalizacao = demandPlanningProjection.getValorTotalKeyFigures(
                keyFiguresOndeFoiAplicadoAjusteTotal,
                posicaoPeriodo,
                filtroDFUProjection,
                unidadeMedidaValor);
        double quantidadeAtualEmKeyFigureQueReceberaAjuste = demandPlanningProjection.getValorTotalKeyFigure(
                keyFigureQueReceberaAjusteNoTotal,
                posicaoPeriodo,
                filtroDFUProjection,
                unidadeMedidaValor);

        // valores a serem calculados : qual o novo valor a ser aplicado e em qual das key figures do planning book
        double quantidadeNovaEmKeyFigureQueReceberaAjuste = Math.max(0, valorNovo) - quantidadeTotalEmKeyFiguresTotalizacao + quantidadeAtualEmKeyFigureQueReceberaAjuste;

        // se ajuste for 0, ignorar
        if (Math.abs(quantidadeAtualEmKeyFigureQueReceberaAjuste - quantidadeNovaEmKeyFigureQueReceberaAjuste) < 0.00001) return;

        // Community persiste somente as Key Figures standard do seu recorte.
        if (keyFigureQueReceberaAjusteNoTotal instanceof KeyFigureStandard) {
            atualizaDemandPlanEmKeyFigureStandardIndividual(
                    (KeyFigureStandard) keyFigureQueReceberaAjusteNoTotal,
                    dataHorarioReferencia,
                    quantidadeAtualEmKeyFigureQueReceberaAjuste,
                    quantidadeNovaEmKeyFigureQueReceberaAjuste,
                    unidadeMedidaValor,
                    keyFiguresReferenciaSplitEntreDfus,
                    demandPlanningProjection,
                    filtroDFUProjection,
                    userId,
                    descricaoNivelAjustePlano,
                    ajusteCelulaPlanningBookOriginal,
                    planningBookViewName);
        } else {
            // Ajuste total em key figures customizadas e recurso Enterprise.
            throw new RequiresEnterpriseVersionException("Demand Planning custom key figure adjustment");
        }

    }

    /**
     * Garante que o escopo resolvido possui ao menos uma DFU antes de calcular
     * ou distribuir o ajuste do Planning Book.
     *
     * <p>Uma ou várias DFUs seguem o mesmo contrato. A distribuição agregada
     * permanece no {@link DemandPlanningProjection}, como no legado; esta
     * validação existe apenas para impedir divisão por zero e mensagens
     * indiretas quando a seleção não intersecta a fotografia carregada.</p>
     */
    protected void validaFiltroDFUProjectionPlanningBookDemand(
            FiltroDFUProjection filtroDFUProjection) {

        if (filtroDFUProjection == null || filtroDFUProjection.getNumeroDFUs() == 0) {
            throw new IllegalArgumentException(
                    "Demand Planning Planning Book adjustment requires at least one filtered DFU.");
        }

    }

    /**
     * Preserva a regra legada para iniciar um valor positivo em uma seleção
     * agregada cuja série de referência ainda está zerada.
     *
     * <p>Para cada dimensão com mais de um elemento, o parâmetro global
     * correspondente decide explicitamente se a divisão igualitária pode criar
     * a distribuição inicial.</p>
     */
    protected void validaAjusteAgregadoSemReferenciaPlanningBook(
            double valorNovo,
            double valorAtualReferencia,
            FiltroDFUProjection filtroDFUProjection,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (valorNovo <= 0.0000001 || valorAtualReferencia > 0.0000001) {
            return;
        }

        boolean nivelMaterial = filtroDFUProjection.getMateriais().size() == 1;
        boolean nivelLocation = filtroDFUProjection.getLocations().size() == 1;
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        if ((!nivelMaterial && !parametrosGlobais.getPermiteAjusteAgregadoSemBaselineProduto())
                || (!nivelLocation && !parametrosGlobais.getPermiteAjusteAgregadoSemBaselineLocation())) {
            throw new DemandPlanException(
                    "Ajustes em linhas onde plano de demanda =0 (Direct Demand = 0) devem ser feitos no nível material/location");
        }

    }

    /**
     * Persiste o alvo físico de uma Key Figure standard nas DFUs do escopo.
     *
     * <p>O fluxo normal chega aqui com um valor digitado e ja normalizado pelo
     * totalizador publico. Um overlay pode tambem entregar um alvo derivado de
     * uma totalizacao previamente validada, inclusive negativo para uma key
     * figure de ajuste: nesse caso o valor nao deve ser novamente limitado a
     * zero, pois o limite pertence ao total de Direct Demand, nao ao seu
     * componente fisico.</p>
     */
    protected void atualizaDemandPlanEmKeyFigureStandardIndividual(
            KeyFigureStandard keyFigureStandardInputAjuste,
            LocalDateTime dataHorarioReferencia,
            double valorAntigo,
            double novoValor,
            UnidadeMedida unidadeMedidaValor,
            Collection<KeyFigureInterface> keyFiguresReferenciaSplitEntreDfus,
            DemandPlanningProjection demandPlanningProjection,
            FiltroDFUProjection filtroDFUProjection,
            String userId,
            String descricaoNivelAjustePlano, // ex. descricao do agrupamento de características
            @Nullable AjusteCelulaPlanningBook ajusteCelulaPlanningBookOriginal,
            @Nullable String planningBookViewName
    ) {

        int posicaoPeriodo = demandPlanningProjection.getCalendario().getPosicaoPeriodo(dataHorarioReferencia);

        KeyFigureStandardEnum keyFigureStandardEnum = keyFigureStandardInputAjuste.getKeyFigureStandardEnum();

        if (permitePersistenciaDiretaKeyFigureStandardPlanningBookDemandPlanning(keyFigureStandardEnum)) {
            // efetua os ajustes no projection e retorna lista de demand plan linhas afetados
            Queue<Quartet<DemandPlanItem,Constantes.TipoDemanda,Double,Double>> demandPlanItemsAjustados = demandPlanningProjection.setValorDemandPlanItemEmKeyFigureStandardDp(
                    posicaoPeriodo,
                    filtroDFUProjection,
                    keyFigureStandardEnum.getTipoDemanda(),
                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                    keyFiguresReferenciaSplitEntreDfus,
                    novoValor,
                    unidadeMedidaValor);

            if (!demandPlanItemsAjustados.isEmpty()) {
                // salva os resultados dos demand plan linhas
                List<DemandPlanItem> demandPlanItemsParaPersistencia =
                        materializaDemandPlanItemsCommunityParaPersistencia(demandPlanItemsAjustados);
                List<DemandPlanItem> demandPlanItemsSalvas =
                        demandPlanItemRepository.saveAll(demandPlanItemsParaPersistencia);
                validaDemandPlanItemsSalvasPlanningBookCommunity(
                        demandPlanItemsSalvas,
                        demandPlanItemsParaPersistencia.size());
                registraAlteracoesFisicasPlanningBookDemandAposPersistencia(
                        keyFigureStandardInputAjuste,
                        dataHorarioReferencia,
                        unidadeMedidaValor,
                        demandPlanItemsAjustados,
                        demandPlanningProjection,
                        userId,
                        descricaoNivelAjustePlano,
                        ajusteCelulaPlanningBookOriginal,
                        planningBookViewName);
            }
            return;
        }

        switch (keyFigureStandardEnum) {
            case ITENS_NOVOS:
                throw new RequiresEnterpriseVersionException("Demand Planning new materials key figure adjustment");
            case UPLIFT:
                throw new RequiresEnterpriseVersionException("Demand Planning uplift key figure adjustment");
            // não irá processar key figures DEMANDA DIRETA por exemplo
            default:
                throw new IllegalArgumentException("Not possible for key figure of type "
                        + MetodosUtilidade.getValorJsonPropertyDeEnum(keyFigureStandardEnum));
        }
    }

    /**
     * Define quais KFs standard podem ser persistidas diretamente pela rotina
     * compartilhada de ajuste material/location.
     *
     * <p>`Direct Demand` e editavel na tela, mas nao e coluna fisica: o fluxo
     * transforma o ajuste total em uma das KFs base antes de chegar aqui. Por
     * isso o Community persiste diretamente apenas `Baseline` e `Demand
     * Adjustment`. O Enterprise pode reabrir `New Products` quando a projection
     * privada correspondente estiver no classpath.</p>
     */
    protected boolean permitePersistenciaDiretaKeyFigureStandardPlanningBookDemandPlanning(
            KeyFigureStandardEnum keyFigureStandardEnum) {

        return switch (keyFigureStandardEnum) {
            case BASELINE, AJUSTE_DEMANDA -> true;
            default -> false;
        };

    }

    /**
     * Notifica um overlay sobre as linhas físicas standard já persistidas pelo
     * Planning Book.
     *
     * <p>O Community não mantém auditoria privada por DFU. A extensão recebe
     * a própria fila retornada pela projection, que conserva uma ocorrência
     * por folha após rateio, sem recalcular distribuição nem reler linhas do
     * plano.</p>
     */
    protected void registraAlteracoesFisicasPlanningBookDemandAposPersistencia(
            KeyFigureStandard keyFigureFisicaAfetada,
            LocalDateTime dataHorarioReferencia,
            UnidadeMedida unidadeMedidaPlanningBook,
            Collection<Quartet<DemandPlanItem,Constantes.TipoDemanda,Double,Double>> demandPlanItemsAjustados,
            DemandPlanningProjection demandPlanningProjection,
            String userId,
            String descricaoLinhaPlanningBook,
            @Nullable AjusteCelulaPlanningBook ajusteCelulaPlanningBookOriginal,
            @Nullable String planningBookViewName) {

        // Community não persiste Change Log físico por DFU.

    }

    /**
     * Materializa as linhas alteradas pelo Planning Book antes de persistir no
     * contrato Community.
     *
     * <p>O front compartilhado nunca deve conseguir salvar Uplift ou New
     * Materials no Community. Mesmo que uma projection transicional tenha sido
     * carregada com essas colunas preenchidas, o save do Planning Book zera as
     * KFs Enterprise imediatamente antes do repository.</p>
     */
    private List<DemandPlanItem> materializaDemandPlanItemsCommunityParaPersistencia(
            Collection<Quartet<DemandPlanItem,Constantes.TipoDemanda,Double,Double>> demandPlanItemsAjustados) {

        validaDemandPlanItemsAjustadosCommunity(demandPlanItemsAjustados);

        List<DemandPlanItem> demandPlanItemsParaPersistencia = materializaListaParaPersistencia(
                demandPlanItemsAjustados,
                Quartet::getValue0);

        for (DemandPlanItem demandPlanItem : demandPlanItemsParaPersistencia) {
            neutralizaKeyFiguresEnterpriseDemandPlanItemCommunity(demandPlanItem);
        }

        return demandPlanItemsParaPersistencia;

    }

    /**
     * Valida o snapshot de ajustes produzido pela projection antes de copiar a
     * lista para persistencia.
     *
     * <p>Fila vazia e fluxo valido sem linha alterada. Colecao nula, quartet
     * nulo ou `DemandPlanItem` nulo indicam inconsistencia na projection ou no
     * caller do Planning Book e devem falhar antes de neutralizar KFs Enterprise
     * ou chamar repository.</p>
     */
    private void validaDemandPlanItemsAjustadosCommunity(
            Collection<Quartet<DemandPlanItem,Constantes.TipoDemanda,Double,Double>> demandPlanItemsAjustados) {

        if (demandPlanItemsAjustados == null) {
            throw new IllegalArgumentException(
                    "Demand Planning Planning Book adjusted line collection is required for Community persistence.");
        }

        int indiceDemandPlanItemAjustado = 0;
        for (Quartet<DemandPlanItem,Constantes.TipoDemanda,Double,Double> demandPlanItemAjustado : demandPlanItemsAjustados) {
            if (demandPlanItemAjustado == null) {
                throw new IllegalArgumentException(
                        "Demand Planning Planning Book adjusted line at index "
                                + indiceDemandPlanItemAjustado
                                + " is required for Community persistence.");
            }
            if (demandPlanItemAjustado.getValue0() == null) {
                throw new IllegalArgumentException(
                        "Demand Plan line at adjusted index "
                                + indiceDemandPlanItemAjustado
                                + " is required for Community Planning Book persistence.");
            }
            indiceDemandPlanItemAjustado++;
        }

    }

    /**
     * Valida o retorno salvo das linhas ajustadas no Planning Book Community.
     *
     * <p>A tela não recebe as entidades salvas de volta, mas um retorno nulo ou
     * quebrado do repository indicaria falha na escrita da colaboração
     * material/location. Falhar aqui evita que o front receba sucesso enquanto
     * o snapshot persistido não pode ser auditado pelas chaves da linha.</p>
     */
    private void validaDemandPlanItemsSalvasPlanningBookCommunity(
            List<DemandPlanItem> demandPlanItemsSalvas,
            int numeroDemandPlanItemsEsperado) {

        if (demandPlanItemsSalvas == null) {
            throw new IllegalArgumentException(
                    "Saved Demand Planning Planning Book line collection is required for Community persistence.");
        }
        if (demandPlanItemsSalvas.size() != numeroDemandPlanItemsEsperado) {
            throw new IllegalArgumentException(
                    "Saved Demand Planning Planning Book line collection size "
                            + demandPlanItemsSalvas.size()
                            + " differs from expected size "
                            + numeroDemandPlanItemsEsperado
                            + ".");
        }

        int indiceDemandPlanItemSalva = 0;
        for (DemandPlanItem demandPlanItem : demandPlanItemsSalvas) {
            if (demandPlanItem == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Planning Planning Book line at index "
                                + indiceDemandPlanItemSalva
                                + " is required for Community persistence.");
            }
            if (demandPlanItem.getKey() == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Planning Planning Book line at index "
                                + indiceDemandPlanItemSalva
                                + " must have a primary key.");
            }
            if (demandPlanItem.getDemandPlan() == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Planning Planning Book line at index "
                                + indiceDemandPlanItemSalva
                                + " must have a Demand Plan.");
            }
            if (demandPlanItem.getLocation() == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Planning Planning Book line at index "
                                + indiceDemandPlanItemSalva
                                + " must have a location.");
            }
            if (demandPlanItem.getProduto() == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Planning Planning Book line at index "
                                + indiceDemandPlanItemSalva
                                + " must have a material.");
            }
            if (demandPlanItem.getDataReferencia() == null) {
                throw new IllegalArgumentException(
                        "Saved Demand Planning Planning Book line at index "
                                + indiceDemandPlanItemSalva
                                + " must have a reference date.");
            }
            indiceDemandPlanItemSalva++;
        }

    }

    /**
     * Zera KFs que pertencem ao Enterprise antes de qualquer save de Planning
     * Book Community.
     *
     * <p>O metodo e protected para overlays privados preservarem apenas KFs ja
     * migradas. O Community permanece conservador e neutraliza todas as colunas
     * privadas mantidas no schema compartilhado.</p>
     */
    protected void neutralizaKeyFiguresEnterpriseDemandPlanItemCommunity(DemandPlanItem demandPlanItem) {

        demandPlanItem.setQuantidadeItensNovos(0.0);
        demandPlanItem.setQuantidadeItensNovosAtendida(0.0);
        demandPlanItem.setQuantidadeUplift(0.0);
        demandPlanItem.setQuantidadeUpliftAtendida(0.0);

    }

    /**
     * Materializa explicitamente a lista que será persistida a partir das filas de ajustes.
     * Os fluxos de Demand Planning montam essas filas em `ConcurrentLinkedQueue` abastecidas por `parallel()`,
     * e o `stream().toList()` passou a falhar em runtime ao confiar em um tamanho fixo do spliterator.
     * A cópia imperativa mantém o conteúdo final estável sem alterar a semântica do ajuste.
     */
    private <T, R> List<R> materializaListaParaPersistencia(
            Collection<T> elementosAjustados,
            Function<T, R> mapperElementoPersistido) {

        List<R> elementosParaPersistencia = new ArrayList<>(elementosAjustados.size());

        for (T elementoAjustado : elementosAjustados) {
            elementosParaPersistencia.add(mapperElementoPersistido.apply(elementoAjustado));
        }

        return elementosParaPersistencia;
    }

    /**
     * Monta a ConfiguredViewProjection do Demand Planning garantindo que o filtro
     * opcional de faturamento historico seja calculado apenas sobre o recorte ja
     * filtrado da propria view.
     *
     * Sem esse passo intermediario, visoes pequenas de colaboracao acabam disparando
     * uma consolidacao de sellout sobre toda a malha ativa para depois descartar quase
     * tudo em memoria.
     */
    private ConfiguredViewProjection getConfiguredViewProjectionDemandPlanning(
            String userId,
            String viewName,
            DemandPlan demandPlan,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ConfiguredView configuredView = configuredViewService.getConfiguredView(
                userId,
                viewName,
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK);

        ConfiguredViewProjection configuredViewProjectionSemFiltroFaturamentoHistorico = configuredViewProjectionFactory.getConfiguredViewProjection(
                configuredView,
                clusterEParametrosProjection);

        BiFunction<Produto,Location,Boolean> filtroFaturamentoHistorico = getFiltroFaturamentoHistorico(
                configuredView,
                demandPlan,
                configuredViewProjectionSemFiltroFaturamentoHistorico.getLocationsFiltradas(),
                configuredViewProjectionSemFiltroFaturamentoHistorico.getMateriaisFiltrados(),
                clusterEParametrosProjection,
                unidadeMedidaProjection);

        if (filtroFaturamentoHistorico == null) {
            return configuredViewProjectionSemFiltroFaturamentoHistorico;
        }

        return configuredViewProjectionFactory.getConfiguredViewProjection(
                configuredView,
                clusterEParametrosProjection,
                filtroFaturamentoHistorico);

    }

    /**
     * Quando a view esconde DFUs sem faturamento historico, a consulta de sellout
     * usada nesse filtro deve considerar somente locations e materiais ja filtrados
     * pela propria ConfiguredView.
     */
    private @Nullable BiFunction<Produto,Location,Boolean> getFiltroFaturamentoHistorico(
            ConfiguredView configuredView,
            DemandPlan demandPlan,
            Set<Location> locationsFiltradas,
            Set<Produto> materiaisFiltrados,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) {
        
        BiFunction<Produto,Location,Boolean> filtroFaturamentoHistorico = null;
        if (!configuredView.getExibeDfusSemFaturamentoNoHorizonteHistorico() && configuredView.getNumeroPeriodosHistoricosDemandPlanningBook() > 0) {
            
            ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = demandPlan.getPerfilExecucaoDemandPlan();
            
            Calendario calendarioHistoricoVendas = Calendario.criaCalendarioDeOffsetsPeriodos(
                    demandPlan.getTamanhoBucket(), demandPlan.getDataInicioPlano(), 
                    0, configuredView.getNumeroPeriodosHistoricosDemandPlanningBook(), 0, 0);
            
            UnidadeMedida unidadeMedidaVisao = configuredView.getUnidadeMedidaView(parametrosGlobais);
            SalesProjectionLocationMaterial salesProjectionMaterialLocation = salesProjectionFactory.getSalesProjectionMaterialLocation(
                    perfilExecucaoDemandPlan.getTipoDocumentoVenda(parametrosGlobais),
                    calendarioHistoricoVendas, 
                    locationsFiltradas,
                    materiaisFiltrados,
                    unidadeMedidaProjection, 
                    clusterEParametrosProjection, 
                    configuredView.getUnidadeMedidaView(parametrosGlobais));
            
            filtroFaturamentoHistorico = (material, location) -> salesProjectionMaterialLocation.getQuantidadeSales(material, location, unidadeMedidaVisao) > 0;
            
        }
        
        return filtroFaturamentoHistorico;
        
    }
    
}
