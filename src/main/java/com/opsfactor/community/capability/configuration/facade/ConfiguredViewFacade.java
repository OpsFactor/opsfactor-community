package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.configuration.user.domain.*;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView.ConfiguredViewCompositeKey;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView.TipoView;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardSupplyPlanning;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewRepository;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewKeyFigureRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewCaracteristicaDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewKeyFigureDTO;
import com.opsfactor.community.capability.configuration.facade.mapper.ConfiguredViewAutoMapper;
import com.opsfactor.community.capability.demandplanning.planningbook.domain.DemandPlanningPlanningBookCatalog;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Set;

/**
 * Service de fachada para configuracao de User Views dos Planning Books.
 *
 * <p>No Community, uma view pode salvar preferencias operacionais simples
 * (nome, unidade de medida, auto-submit, horizonte congelado, exibicao de
 * historico e materiais descontinuados), mas nao pode reabrir capacidades
 * Enterprise: workflow de colaboracao, agrupamento por caracteristicas,
 * filtros DFU persistidos, selecao livre de key figures ou ocultacao de nivel
 * material/location.</p>
 *
 * <p>As validacoes desta classe sao intencionalmente anteriores ao acesso aos
 * repositories. Assim payloads legados, chamadas manuais de API ou campos
 * transicionais enviados pelo front falham com `RequiresEnterpriseVersionException`
 * antes de qualquer leitura ou gravacao de view.</p>
 */
@Service
public class ConfiguredViewFacade {

    /** Atributos de material que o runtime Community consegue avaliar sem catálogo privado. */
    public static final Set<String> MATERIAL_FILTER_CHARACTERISTICS = Set.of(
            "MATERIAL_STATUS",
            "MATERIAL_ACTIVE");

    /** Atributos de location presentes no próprio aggregate Community. */
    public static final Set<String> LOCATION_FILTER_CHARACTERISTICS = Set.of(
            "LOCATION_TYPE",
            "LOCATION_ACTIVE",
            "COUNTRY",
            "STATE",
            "CITY");

    /**
     * Repository da entidade de view persistida.
     *
     * <p>O acesso ocorre apenas depois das validacoes Community, para impedir
     * que campos Enterprise transicionais alterem ou criem views parcialmente
     * configuradas.</p>
     */
    @Autowired
    private ConfiguredViewRepository configuredViewRepository;

    /**
     * Armazena seleção/ordem/edição como filhos unidirecionais da view. A
     * entidade ConfiguredView não mantém coleção inversa para não transformar
     * todo carregamento de view em candidato a lazy collection/N+1.
     */
    @Autowired
    private ConfiguredViewKeyFigureRepository configuredViewKeyFigureRepository;

    /**
     * Mapper usado somente para leitura de views ja normalizadas.
     */
    @Autowired
    private ConfiguredViewAutoMapper configuredViewAutoMapper;

    /**
     * Parametros globais necessarios para resolver defaults de unidade e
     * exibicao na conversao para DTO.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Repository de unidade de medida da view. O Community ainda permite
     * escolher a unidade de exibicao, mesmo sem agrupamentos Enterprise.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /** Repositories usados em duas consultas batch para validar filtros por ID. */
    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private LocationRepository locationRepository;

    /**
     * Lista as views de Demand Planning Book do usuario solicitante.
     */
    public List<ConfiguredViewDTO> getConfiguredViewDTOListDemandPlanningBook(String userIdSolicitante) {

        List<ConfiguredView> configuredViewList =
                configuredViewRepository.findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyTipoView(
                        userIdSolicitante,
                        ConfiguredView.TipoView.DEMANDPLANNINGBOOK);

        return converteConfiguredViewDTOListCommunity(
                configuredViewList,
                userIdSolicitante,
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK);

    }

    /**
     * Cria uma view vazia de Demand Planning Book.
     *
     * <p>A view nasce sem filtros, grupos ou key figures customizadas. O front
     * Community deve configurar apenas preferencias permitidas por
     * {@link #saveConfiguredViewDTO(ConfiguredViewDTO, String, boolean)}.</p>
     */
    public void createConfiguredViewDTODemandPlanningBook(String userId, String viewName) {

        validaNomeViewCommunity(viewName);

        if (configuredViewRepository.findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
                        userId,
                        viewName,
                        ConfiguredView.TipoView.DEMANDPLANNINGBOOK).isPresent()) return;
        salvaConfiguredViewCommunity(
                new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                        userId,
                        viewName,
                        ConfiguredView.TipoView.DEMANDPLANNINGBOOK)));
    }

    /**
     * Valida o nome informado ao criar uma User View Community.
     *
     * <p>Nome invalido e erro de payload, nao erro tecnico nem recurso
     * Enterprise. A validacao precisa ocorrer antes do repository para que
     * chamadas manuais de API falhem com mensagem funcional e sem leitura
     * desnecessaria de banco.</p>
     */
    private void validaNomeViewCommunity(String viewName) {

        if (viewName == null || viewName.length() > 100 || viewName.length() <= 0) {
            throw new IllegalArgumentException("View name must be between 1 and 100 characters long.");
        }

    }

    /**
     * Salva uma view Community depois de normalizar o contrato aberto.
     *
     * <p>Mesmo quando o DTO contem campos compartilhados com o Enterprise, a
     * entidade persistida Community fica sempre em nivel material/location,
     * sem workflow, sem selecao de KF de destino e sem listas de caracteristicas
     * ou filtros DFU persistidos. Essa normalizacao evita que configuracoes
     * parciais sobrevivam no banco e sejam interpretadas no futuro como
     * capabilities Enterprise ativas.</p>
     */
    @Transactional
    public void saveConfiguredViewDTO(
            ConfiguredViewDTO configuredViewDTO,
            String userId,
            boolean usuarioPodeModificarVisaoOutrosUsuarios) {

        validaConfiguredViewDTOCommunity(configuredViewDTO);
        validaCamposPersistenciaConfiguredViewDTOCommunity(configuredViewDTO);
        validaFiltrosIdConfiguredViewDTOCommunity(configuredViewDTO);

        if (userId.equals(configuredViewDTO.userId) || usuarioPodeModificarVisaoOutrosUsuarios) {

            Optional<ConfiguredView> configuredViewOptional =
                    configuredViewRepository.findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
                            configuredViewDTO.userId,
                            configuredViewDTO.viewName,
                            configuredViewDTO.viewType);

            ConfiguredView configuredView = configuredViewOptional
                    .orElse(new ConfiguredView(new ConfiguredViewCompositeKey(
                            configuredViewDTO.userId,
                            configuredViewDTO.viewName, configuredViewDTO.viewType)));

            Optional<UnidadeMedida> unidadeMedidaOptional =
                    unidadeMedidaRepository.findById(configuredViewDTO.unitOfMeasure);

            /*
             * UOM inexistente preserva o fallback historico para UN. Optional
             * nulo, entretanto, e repository quebrado e nao deve ser tratado
             * como unidade ausente.
             */
            if (unidadeMedidaOptional == null) {
                throw new IllegalStateException(
                        "Unit of Measure repository returned null Optional for configured view UOM "
                                + configuredViewDTO.unitOfMeasure
                                + ".");
            }

            UnidadeMedida unidadeMedidaView = unidadeMedidaOptional.orElse(new UnidadeMedida("UN"));
            configuredView.setUnidadeMedidaView(unidadeMedidaView);

            // Community sempre trabalha no Planning Book em nivel material/location.
            // Os toggles de nivel agregado existem apenas no OpsFactor Enterprise.
            configuredView.setExibeMateriais(true);
            configuredView.setExibeLocations(true);
            configuredView.setSubmissaoAutomaticaAlteracoes(configuredViewDTO.autoSubmitChanges);
            configuredView.setPermiteAlteracaoHorizonteCongelado(configuredViewDTO.allowInputFrozenHorizon);
            configuredView.setExibeVendaMediaHistorica(configuredViewDTO.showHistoricalAverage);
            configuredView.setExibeMateriaisDescontinuados(configuredViewDTO.showDiscontinuedMaterials);
            configuredView.setExibeVendaMediaHistorica(configuredViewDTO.showAverageHistoricalSales);
            configuredView.setExibeDfusSemFaturamentoNoHorizonteHistorico(configuredViewDTO.showDfusWithoutHistoricalSalesOverHistoricalPeriod);

            /*
             * O hook e o unico ponto de extensao para campos compartilhados
             * cujo significado pertence ao Enterprise. A implementacao base
             * neutraliza os identificadores para que chamadas Community nunca
             * preservem workflow por acidente; o overlay privado persiste o
             * par ja reconciliado pelo seu repository.
             */
            aplicaCamposEnterpriseConfiguredView(configuredView, configuredViewDTO);

            // Community nao permite workflow de colaboracao nem escolha da KF de destino.
            // A linha Direct Demand sempre propaga ajustes para a KF padrao Demand Adjustment.
            configuredView.setKeyFigureAjustesDemandaDiretaTotal(getKeyFigureAjusteDemandaDiretaTotalCommunity());

            configuredView.setNumeroPeriodosHistoricosDemandPlanningBook(configuredViewDTO.numberHistoricalSalesPeriodsDemandPlanningBook);

            /*
             * Filtros simples por chave funcional pertencem à Community. Eles
             * não criam agrupamentos nem dependem das características
             * dinâmicas do overlay Enterprise.
             */
            configuredView.setMaterialIdFilterSet(
                    new LinkedHashSet<>(getIdFilterList(configuredViewDTO.materialIdFilterList)));
            configuredView.setLocationIdFilterSet(
                    new LinkedHashSet<>(getIdFilterList(configuredViewDTO.locationIdFilterList)));
            configuredView.setMaterialCharacteristicFilterSet(
                    toCharacteristicFilterSet(configuredViewDTO.materialCharacteristicDetailList));
            configuredView.setLocationCharacteristicFilterSet(
                    toCharacteristicFilterSet(configuredViewDTO.locationCharacteristicDetailList));

            ConfiguredView configuredViewSalva = salvaConfiguredViewCommunity(configuredView);
            sincronizaConfiguredViewKeyFigures(
                    configuredViewSalva,
                    getConfiguredViewKeyFigureDTOList(configuredViewDTO.keyFigureList));
        }

    }

    /**
     * Persiste a view e valida o snapshot salvo antes de devolver sucesso para
     * a tela.
     *
     * <p>Os caminhos de criacao e atualizacao retornam `void`, mas a SPA passa
     * a tratar a view como existente depois da resposta. Por isso a borda
     * Community precisa diferenciar sucesso real de repository/stub quebrado
     * que devolveu entidade nula ou sem chave funcional.</p>
     */
    private ConfiguredView salvaConfiguredViewCommunity(ConfiguredView configuredView) {

        ConfiguredView configuredViewSalva = configuredViewRepository.save(configuredView);
        validaConfiguredViewSalvaCommunity(configuredViewSalva);
        return configuredViewSalva;

    }

    /**
     * Sincroniza a fotografia completa das preferências de Key Figure sem
     * depender de coleção inversa ou cascade na ConfiguredView.
     *
     * <p>A coleção recebida já passou pelos gates da edição. Ainda assim,
     * duplicidade de id não pode ser reduzida silenciosamente: duas entradas
     * para a mesma chave não possuem semântica de merge e falham antes de
     * qualquer escrita.</p>
     */
    private void sincronizaConfiguredViewKeyFigures(
            ConfiguredView configuredView,
            List<ConfiguredViewKeyFigureDTO> configuredViewKeyFigureDTOList) {

        Map<String, ConfiguredViewKeyFigureDTO> dtoByKeyFigureId = new LinkedHashMap<>();
        for (ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO : configuredViewKeyFigureDTOList) {
            if (configuredViewKeyFigureDTO.keyFigure == null || configuredViewKeyFigureDTO.keyFigure.isBlank()) {
                throw new IllegalArgumentException("Configured View key figure id is required.");
            }
            if (dtoByKeyFigureId.put(configuredViewKeyFigureDTO.keyFigure, configuredViewKeyFigureDTO) != null) {
                throw new IllegalArgumentException(
                        "Configured View key figure id must be unique: " + configuredViewKeyFigureDTO.keyFigure);
            }
        }

        List<ConfiguredViewKeyFigure> keyFiguresToSave = new java.util.ArrayList<>();
        int position = 1;
        for (ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO : configuredViewKeyFigureDTOList) {
            ConfiguredViewKeyFigure configuredViewKeyFigure = new ConfiguredViewKeyFigure(
                    new ConfiguredViewKeyFigure.Key(configuredView, configuredViewKeyFigureDTO.keyFigure));
            /* A ordem recebida é o contrato público; posição enviada é legado. */
            configuredViewKeyFigure.setPosition(position++);
            configuredViewKeyFigure.setAllowChanges(
                    resolveAllowChangesConfiguredViewKeyFigure(
                            configuredView.getTipoView(),
                            configuredViewKeyFigureDTO.keyFigure,
                            configuredViewKeyFigureDTO.allowChanges));
            keyFiguresToSave.add(configuredViewKeyFigure);
        }

        /*
         * Primeiro slice: substituição integral da fotografia. Não há merge
         * por posição, nem update seletivo que possa manter linha removida.
         */
        configuredViewKeyFigureRepository.deleteAllByConfiguredView(configuredView);
        if (!keyFiguresToSave.isEmpty()) {
            configuredViewKeyFigureRepository.saveAll(keyFiguresToSave);
        }

    }

    /**
     * Resolve a permissao efetiva antes da fotografia de Key Figures ser
     * persistida.
     *
     * <p>O payload continua podendo omitir {@code allowChanges} por
     * compatibilidade, mas uma KF derivada ou de leitura obrigatoria nunca
     * pode voltar a ser editavel por um request manual. Overlays de edicao
     * podem acrescentar outras travas sem duplicar a sincronizacao inteira.</p>
     */
    protected boolean resolveAllowChangesConfiguredViewKeyFigure(
            ConfiguredView.TipoView tipoView,
            String keyFigureId,
            Boolean requestedAllowChanges) {

        if (ConfiguredView.TipoView.DEMANDPLANNINGBOOK.equals(tipoView)
                && ("Baseline".equals(keyFigureId)
                || KeyFigureStandardEnum.BASELINE.name().equals(keyFigureId))) {
            return false;
        }

        return requestedAllowChanges == null || requestedAllowChanges;

    }

    /**
     * Valida a chave da view salva.
     *
     * <p>As preferencias operacionais da view podem usar defaults, mas a chave
     * user/view/type e obrigatoria em todos os caminhos. Sem ela, leituras
     * posteriores por usuario e tipo de Planning Book passam a depender de
     * estado indeterminado.</p>
     */
    private void validaConfiguredViewSalvaCommunity(ConfiguredView configuredViewSalva) {

        if (configuredViewSalva == null) {
            throw new IllegalStateException(
                    "Saved Configured View snapshot is required after Community view persistence.");
        }

        ConfiguredViewCompositeKey configuredViewCompositeKey =
                configuredViewSalva.getConfiguredViewCompositeKey();
        if (configuredViewCompositeKey == null) {
            throw new IllegalStateException(
                    "Saved Configured View key is required after Community view persistence.");
        }
        if (isBlank(configuredViewCompositeKey.getUserId())) {
            throw new IllegalStateException(
                    "Saved Configured View user id is required after Community view persistence.");
        }
        if (isBlank(configuredViewCompositeKey.getNomeView())) {
            throw new IllegalStateException(
                    "Saved Configured View name is required after Community view persistence.");
        }
        if (configuredViewCompositeKey.getTipoView() == null) {
            throw new IllegalStateException(
                    "Saved Configured View type is required after Community view persistence.");
        }

    }

    /**
     * Centraliza as travas Community da tela de User Views. A tela nova deve
     * bloquear visualmente estes controles, mas a borda do backend tambem
     * precisa falhar de forma explicita para payloads legados ou chamadas API
     * feitas manualmente.
     */
    protected void validaConfiguredViewDTOCommunity(ConfiguredViewDTO configuredViewDTO) {

        validaIdentidadeConfiguredViewDTOCommunity(configuredViewDTO);
        validaWorkflowDemandPlanningCommunity(configuredViewDTO);
        validaNivelDetalhePlanningBookCommunity(configuredViewDTO);
        validaKeyFigureAjusteDemandaDiretaTotalCommunity(configuredViewDTO);
        validaKeyFiguresPlanningBookCommunity(configuredViewDTO);
        validaCaracteristicasPlanningBookCommunity(
                configuredViewDTO.materialCharacteristicDetailList,
                MATERIAL_FILTER_CHARACTERISTICS,
                "Material characteristic presentation and grouping");
        validaCaracteristicasPlanningBookCommunity(
                configuredViewDTO.locationCharacteristicDetailList,
                LOCATION_FILTER_CHARACTERISTICS,
                "Location characteristic presentation and grouping");
        validaFiltrosDfuPlanningBookCommunity(configuredViewDTO);

    }

    /**
     * Valida a identidade minima da view antes de qualquer gate Community.
     *
     * <p>Estes campos fazem parte da chave logica da view e sao necessarios
     * tanto para salvar como para remover. Campos operacionais de persistencia,
     * como unidade de medida, sao validados separadamente depois das travas
     * Enterprise para que um payload que tente usar feature Enterprise continue
     * recebendo `RequiresEnterpriseVersionException` como erro principal.</p>
     */
    private void validaIdentidadeConfiguredViewDTOCommunity(ConfiguredViewDTO configuredViewDTO) {

        if (configuredViewDTO == null) {
            throw new IllegalArgumentException("Configured View payload is required.");
        }
        if (isBlank(configuredViewDTO.userId)) {
            throw new IllegalArgumentException("Configured View user id is required.");
        }
        if (isBlank(configuredViewDTO.viewName)) {
            throw new IllegalArgumentException("Configured View name is required.");
        }
        if (configuredViewDTO.viewType == null) {
            throw new IllegalArgumentException("Configured View type is required.");
        }

    }

    /**
     * Valida campos usados apenas quando a view realmente sera persistida.
     *
     * <p>A unidade de medida permanece editavel no Community; por isso ela
     * precisa vir explicita quando a chamada nao foi bloqueada por nenhum gate
     * Enterprise. Sem esta validacao o repository de unidade receberia id nulo
     * e a borda falharia como erro tecnico.</p>
     */
    private void validaCamposPersistenciaConfiguredViewDTOCommunity(ConfiguredViewDTO configuredViewDTO) {

        if (isBlank(configuredViewDTO.unitOfMeasure)) {
            throw new IllegalArgumentException("Configured View unit of measure is required.");
        }
        /*
         * Zero e uma configuracao valida para nao exibir historico adicional
         * no Planning Book. Valor negativo nao tem semantica funcional e nao
         * deve ser salvo para depois ser truncado por
         * ConfiguredView#getNumeroPeriodosHistoricosDemandPlanningBook().
         */
        if (configuredViewDTO.numberHistoricalSalesPeriodsDemandPlanningBook != null
                && configuredViewDTO.numberHistoricalSalesPeriodsDemandPlanningBook < 0) {
            throw new IllegalArgumentException(
                    "Configured View historical sales period count must be zero or positive.");
        }

    }

    /**
     * Valida em batch as chaves funcionais usadas pelos filtros Community.
     *
     * <p>São no máximo duas consultas, independentemente da quantidade de IDs.
     * Duplicidades, valores vazios ou cadastros inexistentes falham antes de
     * qualquer escrita da view.</p>
     */
    private void validaFiltrosIdConfiguredViewDTOCommunity(ConfiguredViewDTO configuredViewDTO) {

        List<String> materialIds = getIdFilterList(configuredViewDTO.materialIdFilterList);
        List<String> locationIds = getIdFilterList(configuredViewDTO.locationIdFilterList);
        validaIdFilterList(materialIds, "material");
        validaIdFilterList(locationIds, "location");

        Set<String> materialIdsEncontrados = materialIds.isEmpty()
                ? Set.of()
                : produtoRepository.findAllById(materialIds).stream()
                        .map(Produto::getId)
                        .collect(Collectors.toSet());
        Set<String> locationIdsEncontrados = locationIds.isEmpty()
                ? Set.of()
                : locationRepository.findAllById(locationIds).stream()
                        .map(Location::getId)
                        .collect(Collectors.toSet());

        validaTodosIdsEncontrados(materialIds, materialIdsEncontrados, "material");
        validaTodosIdsEncontrados(locationIds, locationIdsEncontrados, "location");

    }

    /** Valida forma e unicidade sem consultar uma entidade por vez. */
    private void validaIdFilterList(List<String> ids, String dimensionName) {

        Set<String> uniqueIds = new LinkedHashSet<>();
        for (String id : ids) {
            if (isBlank(id)) {
                throw new IllegalArgumentException(
                        "Configured View " + dimensionName + " filter id is required.");
            }
            if (!uniqueIds.add(id)) {
                throw new IllegalArgumentException(
                        "Configured View " + dimensionName + " filter id must be unique: " + id);
            }
        }

    }

    /** Diferencia cadastro inexistente de um filtro vazio intencional. */
    private void validaTodosIdsEncontrados(
            List<String> requestedIds,
            Set<String> foundIds,
            String dimensionName) {

        Set<String> missingIds = new LinkedHashSet<>(requestedIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Configured View " + dimensionName + " filter contains unknown ids: "
                            + String.join(", ", missingIds));
        }

    }

    /** Normaliza somente ausência de lista; valores inválidos são validados acima. */
    private List<String> getIdFilterList(List<String> idFilterList) {

        return idFilterList == null ? List.of() : idFilterList;

    }

    private void validaNivelDetalhePlanningBookCommunity(ConfiguredViewDTO configuredViewDTO) {

        /*
         * Community sempre materializa Planning Books em nivel material/location.
         * Ocultar estes niveis no front significa pedir uma visao agregada, que
         * depende da infraestrutura Enterprise de agrupamento, selecao de KFs e
         * persistencia de ajustes em nivel agregado.
         */
        if (Boolean.FALSE.equals(configuredViewDTO.showMaterialLevel)) {
            throw new RequiresEnterpriseVersionException("Planning Book material-level aggregation");
        }
        if (Boolean.FALSE.equals(configuredViewDTO.showLocationLevel)) {
            throw new RequiresEnterpriseVersionException("Planning Book location-level aggregation");
        }

    }

    /**
     * Guarda extensivel do workflow de Demand Planning.
     *
     * <p>O contrato Community continua recusando qualquer workflow. O
     * Enterprise substitui somente este gate e a aplicacao escalar, mantendo
     * todo o fluxo comum de save, autorizacao, validacoes e sincronizacao de
     * Key Figures.</p>
     */
    protected void validaWorkflowDemandPlanningCommunity(ConfiguredViewDTO configuredViewDTO) {

        if (configuredViewDTO.demandPlanWorkflowId != null || configuredViewDTO.demandPlanWorkflowStageId != null) {
            throw new RequiresEnterpriseVersionException("Demand Planning workflow stage");
        }

    }

    /**
     * Aplica campos compartilhados que o Community nao interpreta.
     *
     * <p>O default limpa os dois ids para manter o runtime Community neutro
     * inclusive em chamadas internas que criem ou reutilizem uma entidade sem
     * passar pela guarda de DTO. O overlay Enterprise deve validar o par antes
     * de sobrescrever este comportamento.</p>
     */
    protected void aplicaCamposEnterpriseConfiguredView(
            ConfiguredView configuredView,
            ConfiguredViewDTO configuredViewDTO) {

        configuredView.setDemandPlanWorkflowId(null);
        configuredView.setDemandPlanWorkflowStageId(null);

    }

    private void validaKeyFigureAjusteDemandaDiretaTotalCommunity(ConfiguredViewDTO configuredViewDTO) {

        if (configuredViewDTO.directDemandUpdateKeyFigure == null) return;

        Set<String> valoresPermitidos = Set.of(
                KeyFigureStandardEnum.AJUSTE_DEMANDA.name(),
                getKeyFigureAjusteDemandaDiretaTotalCommunity());
        if (!valoresPermitidos.contains(configuredViewDTO.directDemandUpdateKeyFigure)) {
            throw new RequiresEnterpriseVersionException("Custom direct demand adjustment key figure");
        }

    }

    protected void validaKeyFiguresPlanningBookCommunity(ConfiguredViewDTO configuredViewDTO) {

        List<ConfiguredViewKeyFigureDTO> configuredViewKeyFigureDTOList =
                getConfiguredViewKeyFigureDTOList(configuredViewDTO.keyFigureList);
        Set<String> keyFigureIds = new LinkedHashSet<>();
        for (ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO : configuredViewKeyFigureDTOList) {
            if (configuredViewKeyFigureDTO == null) {
                throw new IllegalArgumentException("Configured View key figure entry is required.");
            }
            if (isBlank(configuredViewKeyFigureDTO.keyFigure)) {
                throw new IllegalArgumentException("Configured View key figure id is required.");
            }
            if (!keyFigureIds.add(configuredViewKeyFigureDTO.keyFigure)) {
                throw new IllegalArgumentException(
                        "Configured View key figure id must be unique: " + configuredViewKeyFigureDTO.keyFigure);
            }

            /*
             * Community permite somente KFs standard do book. IDs Enterprise
             * falham antes de qualquer repository; o overlay privado reabre
             * esse ponto apenas para Custom Key Figures que conseguir resolver em lote.
             */
            if (TipoView.SUPPLYPLANNINGBOOK.equals(configuredViewDTO.viewType)) {
                validaKeyFigurePlanningBookSupplyCommunity(configuredViewKeyFigureDTO);
            } else {
                validaKeyFigurePlanningBookDemandCommunity(configuredViewKeyFigureDTO);
            }
        }

        if (TipoView.DEMANDPLANNINGBOOK.equals(configuredViewDTO.viewType)) {
            /*
             * A seleção livre é Pro, porém versões anteriores do Community
             * reenviavam subconjuntos ou o catálogo público inteiro. Depois de
             * validar que não há KFs privadas, converte qualquer fotografia
             * antiga para o conjunto canônico em vez de quebrar a API.
             */
            configuredViewDTO.keyFigureList =
                    getKeyFiguresPredefinidasDemandPlanningBookCommunity();
        }

    }

    /**
     * Valida uma key figure explicitamente enviada para a view de Demand
     * Planning Book.
     *
     * <p>No Community esta lista continua restrita ao catalogo aberto. O metodo
     * e protegido para que o overlay Enterprise possa reabrir somente as KFs
     * privadas ja migradas, sem duplicar todo o fluxo de validacao e
     * persistencia da view.</p>
     */
    protected void validaKeyFigurePlanningBookDemandCommunity(ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO) {

        if (!getKeyFiguresPlanningBookDemandCommunity().contains(configuredViewKeyFigureDTO.keyFigure)) {
            throw new RequiresEnterpriseVersionException("Planning Book key figure selection");
        }

    }

    /**
     * Valida uma Key Figure explicitamente enviada para Supply Planning Book.
     *
     * <p>O Community aceita apenas o contrato publico do plano de trabalho. O
     * overlay Enterprise pode reabrir uma KF privada ja suportada no runtime,
     * sem ampliar o catalogo ou os defaults compartilhados.</p>
     */
    protected void validaKeyFigurePlanningBookSupplyCommunity(ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO) {

        try {
            KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanning =
                    new KeyFigureStandardSupplyPlanning(configuredViewKeyFigureDTO.keyFigure);
            if (!Constantes.TipoPlano.PLANO_TRABALHO.equals(keyFigureStandardSupplyPlanning.getTipoPlano())) {
                throw new RequiresEnterpriseVersionException("Planning Book key figure selection");
            }
        } catch (IllegalArgumentException illegalArgumentException) {
            /*
             * Views antigas ou payloads manuais podem trazer key figures que o
             * Community nao conhece. A mensagem publica continua sendo a
             * fronteira Enterprise; a causa preserva o valor tecnico que nao
             * foi reconhecido pelo parser de key figure.
             */
            throw new RequiresEnterpriseVersionException(
                    "Planning Book key figure selection",
                    illegalArgumentException);
        }

    }

    private void validaCaracteristicasPlanningBookCommunity(
            List<ConfiguredViewCaracteristicaDTO> configuredViewCaracteristicaDTOList,
            Set<String> allowedCharacteristicIds,
            String featureName) {

        Set<String> characteristicIds = new LinkedHashSet<>();
        for (ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO : getConfiguredViewCaracteristicaDTOList(configuredViewCaracteristicaDTOList)) {
            if (configuredViewCaracteristicaDTO == null) {
                throw new IllegalArgumentException("Configured View characteristic entry is required.");
            }
            /*
             * Community aceita filtros sobre atributos públicos, mas não usa a
             * mesma linha para definir apresentação, posição ou agrupamento.
             */
            if (configuredViewCaracteristicaDTO.aggregationType != null
                    || configuredViewCaracteristicaDTO.columnPosition != null) {
                throw new RequiresEnterpriseVersionException(featureName);
            }
            if (isBlank(configuredViewCaracteristicaDTO.characteristicId)) {
                throw new IllegalArgumentException(
                        "Configured View characteristic filter id is required.");
            }
            if (!allowedCharacteristicIds.contains(configuredViewCaracteristicaDTO.characteristicId)) {
                throw new RequiresEnterpriseVersionException(featureName);
            }
            if (!characteristicIds.add(configuredViewCaracteristicaDTO.characteristicId)) {
                throw new IllegalArgumentException(
                        "Configured View characteristic filter id must be unique: "
                                + configuredViewCaracteristicaDTO.characteristicId);
            }
            validaCharacteristicFilterValues(configuredViewCaracteristicaDTO);
        }

    }

    /** Valida valores sem consultar uma linha de master data por opção. */
    private void validaCharacteristicFilterValues(
            ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO) {

        Set<String> uniqueValues = new LinkedHashSet<>();
        for (String filteredValue : getFilteredValues(configuredViewCaracteristicaDTO)) {
            if (isBlank(filteredValue)) {
                throw new IllegalArgumentException(
                        "Configured View characteristic filter value is required.");
            }
            if (!uniqueValues.add(filteredValue)) {
                throw new IllegalArgumentException(
                        "Configured View characteristic filter value must be unique: "
                                + filteredValue);
            }
        }

    }

    /** Converte somente os valores efetivamente selecionados para o snapshot relacional. */
    private Set<ConfiguredViewCharacteristicFilter> toCharacteristicFilterSet(
            List<ConfiguredViewCaracteristicaDTO> configuredViewCaracteristicaDTOList) {

        Set<ConfiguredViewCharacteristicFilter> configuredViewCharacteristicFilters =
                new LinkedHashSet<>();
        for (ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO :
                getConfiguredViewCaracteristicaDTOList(configuredViewCaracteristicaDTOList)) {
            for (String filteredValue : getFilteredValues(configuredViewCaracteristicaDTO)) {
                configuredViewCharacteristicFilters.add(new ConfiguredViewCharacteristicFilter(
                        configuredViewCaracteristicaDTO.characteristicId,
                        filteredValue));
            }
        }
        return configuredViewCharacteristicFilters;

    }

    private void validaFiltrosDfuPlanningBookCommunity(ConfiguredViewDTO configuredViewDTO) {

        for (ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO : getConfiguredViewCaracteristicaDTOList(configuredViewDTO.materialLocationCharacteristicDetailList)) {
            if (configuredViewCaracteristicaDTO == null) {
                throw new IllegalArgumentException("Configured View material-location characteristic entry is required.");
            }
            /*
             * Diferente de material/location, caracteristica material-location
             * nao tem nenhum papel Community. Qualquer configuracao real aqui
             * indica tentativa de usar filtros DFU persistidos, que pertencem
             * ao Enterprise.
             */
            if (temConfiguracaoCaracteristica(configuredViewCaracteristicaDTO)) {
                throw new RequiresEnterpriseVersionException("Material-location DFU filters");
            }
        }

    }

    private boolean temConfiguracaoCaracteristica(
            ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO) {

        return configuredViewCaracteristicaDTO.characteristicId != null
                || configuredViewCaracteristicaDTO.characteristicDescription != null
                || configuredViewCaracteristicaDTO.aggregationType != null
                || configuredViewCaracteristicaDTO.columnPosition != null
                || !getFilteredValues(configuredViewCaracteristicaDTO).isEmpty();

    }

    private List<ConfiguredViewCaracteristicaDTO> getConfiguredViewCaracteristicaDTOList(
            List<ConfiguredViewCaracteristicaDTO> configuredViewCaracteristicaDTOList) {

        return (configuredViewCaracteristicaDTOList == null) ? List.of() : configuredViewCaracteristicaDTOList;

    }

    private List<ConfiguredViewKeyFigureDTO> getConfiguredViewKeyFigureDTOList(
            List<ConfiguredViewKeyFigureDTO> configuredViewKeyFigureDTOList) {

        return (configuredViewKeyFigureDTOList == null) ? List.of() : configuredViewKeyFigureDTOList;

    }

    private List<String> getFilteredValues(ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO) {

        return (configuredViewCaracteristicaDTO.filteredValues == null) ? List.of() : configuredViewCaracteristicaDTO.filteredValues;

    }

    private Set<String> getKeyFiguresPlanningBookDemandCommunity() {

        Set<String> keyFiguresPlanningBookDemandCommunity = new LinkedHashSet<>();
        for (String keyFigurePlanningBookDemandCommunity :
                DemandPlanningPlanningBookCatalog.getKeyFiguresVisiveisDemandPlanningBookCommunity()) {
            KeyFigureStandardEnum keyFigureStandardEnum =
                    MetodosUtilidade.getValorEnumDeJsonProperty(
                            KeyFigureStandardEnum.class,
                            keyFigurePlanningBookDemandCommunity);

            /*
             * O RuntimeInfo publica o label publico que a SPA nova usa. O name()
             * tecnico permanece aceito apenas para payloads legados ou testes
             * que ainda trafeguem o enum serializado como identificador bruto.
             */
            keyFiguresPlanningBookDemandCommunity.add(keyFigurePlanningBookDemandCommunity);
            keyFiguresPlanningBookDemandCommunity.add(keyFigureStandardEnum.name());
        }

        return keyFiguresPlanningBookDemandCommunity;

    }

    private String getKeyFigureAjusteDemandaDiretaTotalCommunity() {

        return MetodosUtilidade.getValorJsonPropertyDeEnum(KeyFigureStandardEnum.AJUSTE_DEMANDA);

    }


    public List<ConfiguredViewDTO> getConfiguredViewDTOListSupplyPlanningBook(String userIdSolicitante) {

        List<ConfiguredView> configuredViewList =
                configuredViewRepository.findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyTipoView(
                        userIdSolicitante,
                        TipoView.SUPPLYPLANNINGBOOK);

        return converteConfiguredViewDTOListCommunity(
                configuredViewList,
                userIdSolicitante,
                TipoView.SUPPLYPLANNINGBOOK);

    }

    public List<ConfiguredViewDTO> getConfiguredViewDTOListSupplyPlanningBook(String userIdSolicitante, String userIdAExtrair, boolean usuarioPodeExtrairVisaoOutrosUsuarios) {


        if (userIdSolicitante.equals(userIdAExtrair) || usuarioPodeExtrairVisaoOutrosUsuarios) {
            List<ConfiguredView> configuredViewList =
                    configuredViewRepository.findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyTipoView(
                            userIdAExtrair,
                            TipoView.SUPPLYPLANNINGBOOK);

            return converteConfiguredViewDTOListCommunity(
                    configuredViewList,
                    userIdAExtrair,
                    TipoView.SUPPLYPLANNINGBOOK);
        } else {
            return List.of();
        }
    }

    public void createConfiguredViewSupplyPlanningBook(String userId, String viewName) {

        validaNomeViewCommunity(viewName);

        if (configuredViewRepository.findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
                        userId,
                        viewName,
                        ConfiguredView.TipoView.SUPPLYPLANNINGBOOK).isPresent()) return;
        salvaConfiguredViewCommunity(
                new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                        userId,
                        viewName,
                        ConfiguredView.TipoView.SUPPLYPLANNINGBOOK)));
    }

    @Transactional
    public void removeConfiguredView(ConfiguredViewDTO configuredViewDTO) {

        /*
         * Remocao usa a mesma chave logica do salvamento, mas nao exige unidade
         * de medida nem preferencias operacionais. Validar antes do repository
         * evita DELETE derivado com parametros nulos vindo de chamadas manuais.
         */
        validaIdentidadeConfiguredViewDTOCommunity(configuredViewDTO);

        /*
         * A ConfiguredView nao possui colecao inversa/cascade de Key Figures.
         * Buscamos a entidade persistida para remover suas filhas em bulk antes
         * do DELETE do pai, preservando a integridade da FK sem N+1.
         */
        configuredViewRepository.findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
                        configuredViewDTO.userId,
                        configuredViewDTO.viewName,
                        configuredViewDTO.viewType)
                .ifPresent(configuredView -> {
                    configuredViewKeyFigureRepository.deleteAllByConfiguredView(configuredView);
                    /*
                     * EntityManager.remove também limpa as duas
                     * ElementCollections de filtros por ID. Um DELETE bulk do
                     * pai deixaria essa limpeza fora do ciclo de vida JPA.
                     */
                    configuredViewRepository.delete(configuredView);
                });

    }

    private boolean isBlank(String value) {

        return value == null || value.isBlank();

    }

    /**
     * Converte as views persistidas para DTO e valida o contrato publicado
     * para a SPA antes de devolver a listagem.
     *
     * <p>A validacao pos-mapper complementa a validacao do snapshot JPA: a
     * entidade pode estar correta, mas um mapper ou overlay futuro quebrado
     * poderia devolver DTO sem chave, sem listas inicializadas ou com controles
     * Enterprise reabertos. Como o front Community consome essa resposta como
     * configuracao operacional da tela, a falha precisa acontecer aqui e nao
     * depois em uma interacao do usuario.</p>
     */
    private List<ConfiguredViewDTO> converteConfiguredViewDTOListCommunity(
            List<ConfiguredView> configuredViewList,
            String expectedUserId,
            TipoView expectedTipoView) {

        List<ConfiguredViewKeyFigure> configuredViewKeyFigures = configuredViewList.isEmpty()
                ? List.of()
                : configuredViewKeyFigureRepository.findAllByConfiguredViewIn(configuredViewList);
        Map<ConfiguredView, List<ConfiguredViewKeyFigure>> keyFiguresByConfiguredView =
                configuredViewKeyFigures.stream().collect(Collectors.groupingBy(
                        configuredViewKeyFigure -> configuredViewKeyFigure.getKey().getConfiguredView()));
        /*
         * Parametros globais pertencem a fotografia unica de abertura da
         * grade. Carrega-los fora do stream evita uma consulta/lookup por view
         * na listagem de preferencias do usuario.
         */
        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        List<ConfiguredViewDTO> configuredViewDTOList = configuredViewList.stream()
                .map(configuredView -> configuredViewAutoMapper.converteComKeyFigures(
                        configuredView,
                        parametrosGlobais,
                        keyFiguresByConfiguredView.getOrDefault(configuredView, List.of())))
                .collect(Collectors.toList());

        /*
         * Views anteriores à seleção persistida usam o conjunto Community
         * predefinido. O fallback ocorre uma vez por DTO e não grava o banco
         * durante uma leitura administrativa.
         */
        configuredViewDTOList.stream()
                .filter(Objects::nonNull)
                .filter(configuredViewDTO -> TipoView.DEMANDPLANNINGBOOK.equals(configuredViewDTO.viewType))
                .forEach(configuredViewDTO -> configuredViewDTO.keyFigureList =
                        getKeyFiguresPredefinidasDemandPlanningBookCommunity());

        validaConfiguredViewDTOListSnapshotCommunity(
                configuredViewDTOList,
                expectedUserId,
                expectedTipoView);

        return configuredViewDTOList;

    }

    /** Cria uma nova fotografia mutável do conjunto fixo para cada view. */
    private List<ConfiguredViewKeyFigureDTO> getKeyFiguresPredefinidasDemandPlanningBookCommunity() {

        List<ConfiguredViewKeyFigureDTO> configuredViewKeyFigureDTOList =
                new java.util.ArrayList<>();
        for (String keyFigureId : List.of(
                "Direct Demand",
                "Baseline",
                "Demand Adjustment")) {
            ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO =
                    new ConfiguredViewKeyFigureDTO();
            configuredViewKeyFigureDTO.keyFigure = keyFigureId;
            configuredViewKeyFigureDTO.allowChanges = !"Baseline".equals(keyFigureId);
            configuredViewKeyFigureDTO.position = configuredViewKeyFigureDTOList.size() + 1;
            configuredViewKeyFigureDTOList.add(configuredViewKeyFigureDTO);
        }
        return configuredViewKeyFigureDTOList;

    }

    /**
     * Busca views por usuario/tipo e valida a fotografia antes do mapper.
     *
     * <p>Lista vazia representa usuario sem views e continua valida. Lista
     * nula, item nulo ou chave divergente indicam repository quebrado ou query
     * desalinhada e devem falhar antes de o mapper montar DTO parcial para a
     * SPA.</p>
     */
    /**
     * Busca a view pela chave funcional diferenciando ausencia real de quebra
     * de contrato do repository.
     */
    /**
     * Valida snapshots de views lidos para listagens do front.
     */
    private void validaConfiguredViewListSnapshotCommunity(
            List<ConfiguredView> configuredViewList,
            String expectedUserId,
            TipoView expectedTipoView) {

        if (configuredViewList == null) {
            throw new IllegalStateException(
                    "Configured View list snapshot is required for user "
                            + expectedUserId
                            + " and type "
                            + expectedTipoView
                            + ".");
        }

        for (int configuredViewIndex = 0; configuredViewIndex < configuredViewList.size(); configuredViewIndex++) {
            ConfiguredView configuredView = configuredViewList.get(configuredViewIndex);
            if (configuredView == null) {
                throw new IllegalStateException(
                        "Configured View list item "
                                + configuredViewIndex
                                + " is required for user "
                                + expectedUserId
                                + " and type "
                                + expectedTipoView
                                + ".");
            }

            ConfiguredViewCompositeKey configuredViewCompositeKey =
                    configuredView.getConfiguredViewCompositeKey();
            if (configuredViewCompositeKey == null) {
                throw new IllegalStateException(
                        "Configured View list item "
                                + configuredViewIndex
                                + " key is required for Community view listing.");
            }
            if (!expectedUserId.equals(configuredViewCompositeKey.getUserId())) {
                throw new IllegalStateException(
                        "Configured View list item "
                                + configuredViewIndex
                                + " user id must match requested Community user.");
            }
            if (isBlank(configuredViewCompositeKey.getNomeView())) {
                throw new IllegalStateException(
                        "Configured View list item "
                                + configuredViewIndex
                                + " name is required for Community view listing.");
            }
            if (!expectedTipoView.equals(configuredViewCompositeKey.getTipoView())) {
                throw new IllegalStateException(
                        "Configured View list item "
                                + configuredViewIndex
                                + " type must match requested Community Planning Book.");
            }
        }

    }

    /**
     * Valida a fotografia DTO devolvida pelo mapper de listagem.
     *
     * <p>O Community pode listar zero views, mas uma lista retornada precisa
     * ser uma fotografia completa: chave funcional preservada, unidade de
     * medida resolvida, nivel material/location explicitamente habilitado e
     * listas transicionais inicializadas. Caracteristicas, filtros DFU,
     * workflow e configuracao livre de key figures seguem bloqueados no
     * mapper, assim como no salvamento.</p>
     */
    private void validaConfiguredViewDTOListSnapshotCommunity(
            List<ConfiguredViewDTO> configuredViewDTOList,
            String expectedUserId,
            TipoView expectedTipoView) {

        if (configuredViewDTOList == null) {
            throw new IllegalStateException(
                    "Configured View DTO list snapshot is required for user "
                            + expectedUserId
                            + " and type "
                            + expectedTipoView
                            + ".");
        }

        for (int configuredViewDTOIndex = 0; configuredViewDTOIndex < configuredViewDTOList.size(); configuredViewDTOIndex++) {
            ConfiguredViewDTO configuredViewDTO = configuredViewDTOList.get(configuredViewDTOIndex);
            if (configuredViewDTO == null) {
                throw new IllegalStateException(
                        "Configured View DTO list item "
                                + configuredViewDTOIndex
                                + " is required for Community view listing.");
            }

            /* Ausência no mapper/overlay antigo equivale ao filtro vazio. */
            configuredViewDTO.materialIdFilterList =
                    getIdFilterList(configuredViewDTO.materialIdFilterList);
            configuredViewDTO.locationIdFilterList =
                    getIdFilterList(configuredViewDTO.locationIdFilterList);

            validaIdentidadeConfiguredViewDTOCommunity(configuredViewDTO);
            if (!expectedUserId.equals(configuredViewDTO.userId)) {
                throw new IllegalStateException(
                        "Configured View DTO list item "
                                + configuredViewDTOIndex
                                + " user id must match requested Community user.");
            }
            if (!expectedTipoView.equals(configuredViewDTO.viewType)) {
                throw new IllegalStateException(
                        "Configured View DTO list item "
                                + configuredViewDTOIndex
                                + " type must match requested Community Planning Book.");
            }
            if (isBlank(configuredViewDTO.unitOfMeasure)) {
                throw new IllegalStateException(
                        "Configured View DTO list item "
                                + configuredViewDTOIndex
                                + " unit of measure is required for Community view listing.");
            }
            if (!Boolean.TRUE.equals(configuredViewDTO.showMaterialLevel)) {
                throw new IllegalStateException(
                        "Configured View DTO list item "
                                + configuredViewDTOIndex
                                + " material level must be enabled in Community view listing.");
            }
            if (!Boolean.TRUE.equals(configuredViewDTO.showLocationLevel)) {
                throw new IllegalStateException(
                        "Configured View DTO list item "
                                + configuredViewDTOIndex
                                + " location level must be enabled in Community view listing.");
            }

            validaListaMapperConfiguredViewVaziaCommunity(
                    configuredViewDTO.materialLocationCharacteristicDetailList,
                    configuredViewDTOIndex,
                    "material-location characteristic");
            /*
             * Diferentemente das demais listas Enterprise, a selecao de Key
             * Figures agora e uma preferencia compartilhada da view. O
             * Community aceita apenas seu catalogo publico; o overlay privado
             * reaproveita a mesma validacao para resolver Custom Key Figures em lote.
             */
            validaKeyFiguresPlanningBookCommunity(configuredViewDTO);
            validaConfiguredViewDTOCommunity(configuredViewDTO);
            if (configuredViewDTO.materialIdFilterList == null) {
                throw new IllegalStateException(
                        "Configured View DTO list item "
                                + configuredViewDTOIndex
                                + " material id filter list is required for Community view listing.");
            }
            if (configuredViewDTO.locationIdFilterList == null) {
                throw new IllegalStateException(
                        "Configured View DTO list item "
                                + configuredViewDTOIndex
                                + " location id filter list is required for Community view listing.");
            }
            validaIdFilterList(configuredViewDTO.materialIdFilterList, "material");
            validaIdFilterList(configuredViewDTO.locationIdFilterList, "location");
        }

    }

    /**
     * Valida listas que o mapper Community deve publicar inicializadas e sem
     * configuracao Enterprise embutida.
     */
    private void validaListaMapperConfiguredViewVaziaCommunity(
            List<?> configuredViewDTOFieldList,
            int configuredViewDTOIndex,
            String fieldDescription) {

        if (configuredViewDTOFieldList == null) {
            throw new IllegalStateException(
                    "Configured View DTO list item "
                            + configuredViewDTOIndex
                            + " "
                            + fieldDescription
                            + " list is required for Community view listing.");
        }
        if (!configuredViewDTOFieldList.isEmpty()) {
            throw new IllegalStateException(
                    "Configured View DTO list item "
                            + configuredViewDTOIndex
                            + " "
                            + fieldDescription
                            + " list must be empty in Community view listing.");
        }

    }
    /**
     * Falha para textos obrigatorios nulos ou em branco antes de repository.
     */

}
