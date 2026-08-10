package com.opsfactor.community.capability.configuration.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewRepository;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewKeyFigureRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.demandplanning.planningbook.domain.DemandPlanningPlanningBookCatalog;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewCaracteristicaDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewKeyFigureDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.capability.configuration.facade.mapper.ConfiguredViewAutoMapper;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.capability.supplyplanning.planningbook.domain.SupplyPlanningPlanningBookCatalog;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valida regras de persistencia de views de usuario sem depender do contexto
 * Spring completo nem do banco local.
 */
public class ConfiguredViewFacadeTest {

    @Test
    public void saveConfiguredViewDTOShouldNotDeclareLegacyCheckedNotFoundException() throws Exception {

        Method saveConfiguredViewDTOMethod = ConfiguredViewFacade.class.getDeclaredMethod(
                "saveConfiguredViewDTO",
                ConfiguredViewDTO.class,
                String.class,
                boolean.class);

        /*
         * O salvamento de view Community valida payload e normaliza campos
         * Enterprise antes do repository. Ausencia de view existente e tratada
         * como criacao, portanto nao deve vazar NotFoundException checked do
         * legado/Javassist na assinatura publica do service.
         */
        Assertions.assertEquals(0, saveConfiguredViewDTOMethod.getExceptionTypes().length);

    }

    @Test
    public void configuredViewDtoShouldExposeOnlyKnownSharedFields() {

        /*
         * ConfiguredViewDTO e compartilhado com o front Enterprise. Novos
         * campos neste DTO precisam ser classificados explicitamente para nao
         * reabrir workflow, agrupamentos, filtros DFU, caracteristicas ou KFs
         * customizadas no Community por acidente.
         */
        Assertions.assertEquals(
                Set.of(
                        "userId",
                        "viewName",
                        "viewType",
                        "directDemandUpdateKeyFigure",
                        "materialCharacteristicDetailList",
                        "locationCharacteristicDetailList",
                        "materialLocationCharacteristicDetailList",
                        "showMaterialLevel",
                        "showLocationLevel",
                        "unitOfMeasure",
                        "numberHistoricalSalesPeriodsDemandPlanningBook",
                        "keyFigureList",
                        "autoSubmitChanges",
                        "allowInputFrozenHorizon",
                        "showHistoricalAverage",
                        "showDiscontinuedMaterials",
                        "showAverageHistoricalSales",
                        "showDfusWithoutHistoricalSalesOverHistoricalPeriod",
                        "demandPlanWorkflowId",
                        "demandPlanWorkflowStageId",
                        "materialIdFilterList",
                        "locationIdFilterList"),
                getDeclaredFieldNames(ConfiguredViewDTO.class));

    }

    @Test
    public void configuredViewDtoShouldRejectHistoricalPayloadFieldNames() {

        for (String historicalFieldName : List.of(
                "showDiscontinuedProducts",
                "productLocationCharacteristicDetailList")) {
            Assertions.assertThrows(
                    JsonProcessingException.class,
                    () -> new ObjectMapper().readValue(
                            "{\"" + historicalFieldName + "\":true}",
                            ConfiguredViewDTO.class));
        }

    }

    @Test
    public void configuredViewDtoShouldRoundTripCanonicalMaterialFields() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ConfiguredViewDTO configuredViewDTO = objectMapper.readValue(
                """
                {
                  "showDiscontinuedMaterials": true,
                  "materialLocationCharacteristicDetailList": [
                    { "characteristicId": "CHANNEL" }
                  ]
                }
                """,
                ConfiguredViewDTO.class);

        Assertions.assertTrue(configuredViewDTO.showDiscontinuedMaterials);
        Assertions.assertEquals(
                1,
                configuredViewDTO.materialLocationCharacteristicDetailList.size());

        String serializedJson = objectMapper.writeValueAsString(configuredViewDTO);
        Assertions.assertTrue(serializedJson.contains("\"showDiscontinuedMaterials\""));
        Assertions.assertTrue(serializedJson.contains("\"materialLocationCharacteristicDetailList\""));

    }

    @Test
    public void configuredViewAuxiliaryDtosShouldExposeOnlyKnownSharedFields() {

        Assertions.assertEquals(
                Set.of(
                        "viewName",
                        "planId",
                        "referencePlanId",
                        "locationId",
                        "locationIdList",
                        "materialAggregationLevelId",
                        "locationAggregationLevelId"),
                getDeclaredFieldNames(ConfiguredViewSelectionDTO.class));
        Assertions.assertEquals(
                Set.of(
                        "characteristicId",
                        "characteristicDescription",
                        "aggregationType",
                        "columnPosition",
                        "filteredValues"),
                getDeclaredFieldNames(ConfiguredViewCaracteristicaDTO.class));
        Assertions.assertEquals(
                Set.of(
                        "keyFigure",
                        "allowChanges",
                        "position",
                        "userId",
                        "viewName",
                        "viewType"),
                getDeclaredFieldNames(ConfiguredViewKeyFigureDTO.class));

    }

    @Test
    public void createConfiguredViewDemandPlanningBookShouldRejectInvalidNameBeforeRepository() {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();

        /*
         * Nenhum repository e injetado. Nome invalido precisa falhar como erro
         * de payload antes de qualquer leitura ou gravacao da view persistida.
         */
        assertInvalidViewName(() -> configuredViewFrontService.createConfiguredViewDTODemandPlanningBook(
                "DEBUG",
                ""));
        assertInvalidViewName(() -> configuredViewFrontService.createConfiguredViewDTODemandPlanningBook(
                "DEBUG",
                "X".repeat(101)));

    }

    @Test
    public void createConfiguredViewSupplyPlanningBookShouldRejectInvalidNameBeforeRepository() {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();

        /*
         * Supply e Demand compartilham a mesma politica Community de nome de
         * view. O teste protege a duplicacao historica de validacao nessa borda.
         */
        assertInvalidViewName(() -> configuredViewFrontService.createConfiguredViewSupplyPlanningBook(
                "DEBUG",
                ""));
        assertInvalidViewName(() -> configuredViewFrontService.createConfiguredViewSupplyPlanningBook(
                "DEBUG",
                "X".repeat(101)));

    }

    @Test
    public void createConfiguredViewDemandPlanningBookShouldRejectNullSavedSnapshotAfterRepository() {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();
        ConfiguredViewRepositoryStub configuredViewRepositoryStub = new ConfiguredViewRepositoryStub();
        configuredViewRepositoryStub.saveReturnsNull = true;
        setPrivateField(
                configuredViewFrontService,
                "configuredViewRepository",
                configuredViewRepositoryStub.getRepository());

        /*
         * A criacao retorna void, mas o front assume que a view passa a existir.
         * Repository que devolve snapshot nulo deve falhar antes de a chamada
         * ser considerada concluida.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> configuredViewFrontService.createConfiguredViewDTODemandPlanningBook(
                        "DEBUG",
                        "Default Demand View"));

        Assertions.assertEquals(
                "Saved Configured View snapshot is required after Community view persistence.",
                illegalStateException.getMessage());

    }

    @Test
    public void getConfiguredViewDTOListDemandPlanningBookShouldRejectBrokenMapperSnapshotBeforeReturning() {

        ConfiguredViewRepositoryStub configuredViewRepositoryStub =
                new ConfiguredViewRepositoryStub();
        configuredViewRepositoryStub.configuredViewListByUserAndType = List.of(
                configuredView(
                        "DEBUG",
                        "Default Demand View",
                        ConfiguredView.TipoView.DEMANDPLANNINGBOOK));

        ConfiguredViewFacade configuredViewFrontServiceComListaDTONula =
                configuredViewFrontServiceParaListagem(
                        configuredViewRepositoryStub,
                        null);

        /*
         * A entidade carregada ja foi validada antes do mapper. Mesmo assim a
         * fotografia DTO devolvida para a SPA precisa ser completa, pois a tela
         * passa a trabalhar apenas com esse contrato apos a listagem.
         */
        IllegalStateException listaDTONulaException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> configuredViewFrontServiceComListaDTONula.getConfiguredViewDTOListDemandPlanningBook("DEBUG"));
        Assertions.assertEquals(
                "Configured View DTO list item 0 is required for Community view listing.",
                listaDTONulaException.getMessage());

        ConfiguredViewFacade configuredViewFrontServiceComItemDTONulo =
                configuredViewFrontServiceParaListagem(
                        configuredViewRepositoryStub,
                        Arrays.asList((ConfiguredViewDTO) null));

        IllegalStateException itemDTONuloException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> configuredViewFrontServiceComItemDTONulo.getConfiguredViewDTOListDemandPlanningBook("DEBUG"));
        Assertions.assertEquals(
                "Configured View DTO list item 0 is required for Community view listing.",
                itemDTONuloException.getMessage());

        ConfiguredViewDTO configuredViewDTOAgregado =
                configuredViewDTOListagem(
                        "DEBUG",
                        "Default Demand View",
                        ConfiguredView.TipoView.DEMANDPLANNINGBOOK);
        configuredViewDTOAgregado.showMaterialLevel = false;
        ConfiguredViewFacade configuredViewFrontServiceComViewAgregada =
                configuredViewFrontServiceParaListagem(
                        configuredViewRepositoryStub,
                        List.of(configuredViewDTOAgregado));

        IllegalStateException viewAgregadaException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> configuredViewFrontServiceComViewAgregada.getConfiguredViewDTOListDemandPlanningBook("DEBUG"));
        Assertions.assertEquals(
                "Configured View DTO list item 0 material level must be enabled in Community view listing.",
                viewAgregadaException.getMessage());

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectMissingIdentityAndPersistenceFieldsBeforeRepository() {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();

        /*
         * Nenhum repository e injetado: a borda precisa transformar payloads
         * sem chave funcional em erro de contrato antes de qualquer leitura ou
         * gravacao. A unidade de medida e validada depois das travas Enterprise,
         * mas ainda antes do repository quando a view Community pode ser salva.
         */
        assertInvalidConfiguredViewPayload(() -> configuredViewFrontService.saveConfiguredViewDTO(
                null,
                "DEBUG",
                false));

        ConfiguredViewDTO configuredViewDTOWithoutUser = getBaseConfiguredViewDTO();
        configuredViewDTOWithoutUser.userId = null;
        assertInvalidConfiguredViewPayload(() -> configuredViewFrontService.saveConfiguredViewDTO(
                configuredViewDTOWithoutUser,
                "DEBUG",
                false));

        ConfiguredViewDTO configuredViewDTOWithoutViewName = getBaseConfiguredViewDTO();
        configuredViewDTOWithoutViewName.viewName = " ";
        assertInvalidConfiguredViewPayload(() -> configuredViewFrontService.saveConfiguredViewDTO(
                configuredViewDTOWithoutViewName,
                "DEBUG",
                false));

        ConfiguredViewDTO configuredViewDTOWithoutViewType = getBaseConfiguredViewDTO();
        configuredViewDTOWithoutViewType.viewType = null;
        assertInvalidConfiguredViewPayload(() -> configuredViewFrontService.saveConfiguredViewDTO(
                configuredViewDTOWithoutViewType,
                "DEBUG",
                false));

        ConfiguredViewDTO configuredViewDTOWithoutUnitOfMeasure = getBaseConfiguredViewDTO();
        configuredViewDTOWithoutUnitOfMeasure.unitOfMeasure = null;
        assertInvalidConfiguredViewPayload(() -> configuredViewFrontService.saveConfiguredViewDTO(
                configuredViewDTOWithoutUnitOfMeasure,
                "DEBUG",
                false));

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectNegativeHistoricalPeriodCountBeforeRepository() {

        ConfiguredViewFacade configuredViewFrontService =
                new ConfiguredViewFacade();
        ConfiguredViewDTO configuredViewDTO = getBaseConfiguredViewDTO();
        configuredViewDTO.numberHistoricalSalesPeriodsDemandPlanningBook = -1;

        /*
         * Zero significa nao exibir periodos historicos adicionais no Planning
         * Book. Negativo nao tem semantica funcional e deve falhar antes de
         * qualquer repository, em vez de ser truncado para zero pelo getter da
         * entidade.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewFrontService.saveConfiguredViewDTO(
                        configuredViewDTO,
                        "DEBUG",
                        false));

        Assertions.assertEquals(
                "Configured View historical sales period count must be zero or positive.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectBrokenSavedSnapshotAfterRepository() throws Exception {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();
        ConfiguredViewRepositoryStub configuredViewRepositoryStub = new ConfiguredViewRepositoryStub();
        configuredViewRepositoryStub.savedEntityReturnedBySave = new ConfiguredView();
        setPrivateField(
                configuredViewFrontService,
                "configuredViewRepository",
                configuredViewRepositoryStub.getRepository());
        setPrivateField(
                configuredViewFrontService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository());
        setPrivateField(
                configuredViewFrontService,
                "configuredViewKeyFigureRepository",
                configuredViewKeyFigureRepository());

        ConfiguredViewDTO configuredViewDTO = getBaseConfiguredViewDTO();

        /*
         * A view pre-save possui chave, mas o repository/stub devolveu outra
         * entidade sem chave. O service deve validar a fotografia salva, nao
         * apenas o objeto montado antes da persistencia.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> configuredViewFrontService.saveConfiguredViewDTO(
                        configuredViewDTO,
                        "DEBUG",
                        false));

        Assertions.assertEquals(
                "Saved Configured View key is required after Community view persistence.",
                illegalStateException.getMessage());

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectNullNestedEntriesBeforeRepository() throws Exception {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();
        Map<String, Object> invalidNestedEntryValuesByName = new LinkedHashMap<>();
        invalidNestedEntryValuesByName.put("keyFigureList", Arrays.asList((ConfiguredViewKeyFigureDTO) null));
        invalidNestedEntryValuesByName.put("materialCharacteristicDetailList", Arrays.asList((ConfiguredViewCaracteristicaDTO) null));
        invalidNestedEntryValuesByName.put("locationCharacteristicDetailList", Arrays.asList((ConfiguredViewCaracteristicaDTO) null));
        invalidNestedEntryValuesByName.put("materialLocationCharacteristicDetailList", Arrays.asList((ConfiguredViewCaracteristicaDTO) null));

        /*
         * Listas nulas sao tratadas como ausencia de configuracao; itens nulos
         * dentro de uma lista sao payload corrompido e precisam falhar antes de
         * qualquer interacao com repositories.
         */
        for (Map.Entry<String, Object> invalidNestedEntry : invalidNestedEntryValuesByName.entrySet()) {
            ConfiguredViewDTO configuredViewDTO = getBaseConfiguredViewDTO();
            Field field = ConfiguredViewDTO.class.getDeclaredField(invalidNestedEntry.getKey());
            field.setAccessible(true);
            field.set(configuredViewDTO, invalidNestedEntry.getValue());

            assertInvalidConfiguredViewPayload(() -> configuredViewFrontService.saveConfiguredViewDTO(
                    configuredViewDTO,
                    "DEBUG",
                    false));
        }

    }

    @Test
    public void removeConfiguredViewShouldRejectMissingIdentityBeforeRepository() {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();

        /*
         * Remocao so precisa da chave da view. Payload nulo ou sem um dos
         * elementos da chave nao pode chegar ao metodo derivado de DELETE JPA.
         */
        assertInvalidConfiguredViewPayload(() -> configuredViewFrontService.removeConfiguredView(null));

        ConfiguredViewDTO configuredViewDTOWithoutUser = getBaseConfiguredViewDTO();
        configuredViewDTOWithoutUser.userId = "";
        assertInvalidConfiguredViewPayload(() -> configuredViewFrontService.removeConfiguredView(
                configuredViewDTOWithoutUser));

        ConfiguredViewDTO configuredViewDTOWithoutViewName = getBaseConfiguredViewDTO();
        configuredViewDTOWithoutViewName.viewName = null;
        assertInvalidConfiguredViewPayload(() -> configuredViewFrontService.removeConfiguredView(
                configuredViewDTOWithoutViewName));

        ConfiguredViewDTO configuredViewDTOWithoutViewType = getBaseConfiguredViewDTO();
        configuredViewDTOWithoutViewType.viewType = null;
        assertInvalidConfiguredViewPayload(() -> configuredViewFrontService.removeConfiguredView(
                configuredViewDTOWithoutViewType));

    }

    @Test
    public void saveConfiguredViewDTOShouldNormalizeCommunityPlanningBookView() throws Exception {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();
        ConfiguredViewRepositoryStub configuredViewRepositoryStub = new ConfiguredViewRepositoryStub();
        setPrivateField(
                configuredViewFrontService,
                "configuredViewRepository",
                configuredViewRepositoryStub.getRepository());
        setPrivateField(
                configuredViewFrontService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository());
        setPrivateField(
                configuredViewFrontService,
                "configuredViewKeyFigureRepository",
                configuredViewKeyFigureRepository());

        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = "DEBUG";
        configuredViewDTO.viewName = "B2B - Canal - SKU";
        configuredViewDTO.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;
        configuredViewDTO.unitOfMeasure = "UN";
        configuredViewDTO.showMaterialLevel = true;
        configuredViewDTO.showLocationLevel = true;
        configuredViewDTO.directDemandUpdateKeyFigure = "Demand Adjustment";
        configuredViewDTO.materialCharacteristicDetailList = List.of();
        configuredViewDTO.locationCharacteristicDetailList = List.of();
        configuredViewDTO.materialLocationCharacteristicDetailList = List.of();
        configuredViewDTO.keyFigureList = List.of(
                keyFigureDTO("Direct Demand", true, 1),
                keyFigureDTO("Baseline", false, 2),
                keyFigureDTO("Demand Adjustment", true, 3));

        configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false);

        Assertions.assertTrue(configuredViewRepositoryStub.savedEntity.getExibeMateriais());
        Assertions.assertTrue(configuredViewRepositoryStub.savedEntity.getExibeLocations());
        Assertions.assertEquals("Demand Adjustment", configuredViewRepositoryStub.savedEntity.getKeyFigureAjustesDemandaDiretaTotal());

    }

    @Test
    public void saveConfiguredViewDTOShouldAcceptDefaultDemandPlanningBookKeyFigures() throws Exception {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();
        ConfiguredViewRepositoryStub configuredViewRepositoryStub = new ConfiguredViewRepositoryStub();
        setPrivateField(
                configuredViewFrontService,
                "configuredViewRepository",
                configuredViewRepositoryStub.getRepository());
        setPrivateField(
                configuredViewFrontService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository());
        setPrivateField(
                configuredViewFrontService,
                "configuredViewKeyFigureRepository",
                configuredViewKeyFigureRepository());

        ConfiguredViewDTO configuredViewDTO = getBaseConfiguredViewDTO();
        configuredViewDTO.unitOfMeasure = "UN";
        configuredViewDTO.keyFigureList = DemandPlanningPlanningBookCatalog
                .getKeyFiguresVisiveisDemandPlanningBookCommunity()
                .stream()
                .map(keyFigure -> keyFigureDTO(keyFigure, false, 1))
                .toList();

        configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false);

        /*
         * A lista publicada em runtime-info precisa continuar aceita pela borda
         * de salvamento de views. O Community ignora a selecao livre ao persistir,
         * mas payloads vindos da SPA podem reenviar as KFs padrao visiveis.
         */
        Assertions.assertNotNull(configuredViewRepositoryStub.savedEntity);

    }

    @Test
    public void saveConfiguredViewDTOShouldAcceptDefaultDemandPlanningBookKeyFigureEnumNames() throws Exception {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();
        ConfiguredViewRepositoryStub configuredViewRepositoryStub = new ConfiguredViewRepositoryStub();
        setPrivateField(
                configuredViewFrontService,
                "configuredViewRepository",
                configuredViewRepositoryStub.getRepository());
        setPrivateField(
                configuredViewFrontService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository());
        setPrivateField(
                configuredViewFrontService,
                "configuredViewKeyFigureRepository",
                configuredViewKeyFigureRepository());

        ConfiguredViewDTO configuredViewDTO = getBaseConfiguredViewDTO();
        configuredViewDTO.unitOfMeasure = "UN";
        configuredViewDTO.keyFigureList = DemandPlanningPlanningBookCatalog
                .getKeyFiguresVisiveisDemandPlanningBookCommunity()
                .stream()
                .map(keyFigure -> MetodosUtilidade.getValorEnumDeJsonProperty(
                        KeyFigureStandardEnum.class,
                        keyFigure).name())
                .map(keyFigure -> keyFigureDTO(keyFigure, false, 1))
                .toList();

        configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false);

        /*
         * A SPA nova deve enviar labels publicos, mas bancos/scripts legados
         * ainda podem trafegar `KeyFigureStandardEnum.name()`. A allowlist de
         * view precisa continuar derivada do catalogo visual sem quebrar essa
         * compatibilidade de entrada.
         */
        Assertions.assertNotNull(configuredViewRepositoryStub.savedEntity);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectAggregatedPlanningBookView() {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();

        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = "DEBUG";
        configuredViewDTO.viewName = "Aggregated View";
        configuredViewDTO.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;
        configuredViewDTO.showMaterialLevel = false;
        configuredViewDTO.showLocationLevel = true;

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false));

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectEnterpriseDemandPlanningKeyFiguresBeforeRepository() {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();

        for (String enterpriseDemandPlanningKeyFigure : List.of(
                "New Products",
                "Uplift",
                "Client Orders",
                "Comparison Plan",
                "Direct Demand / Working Day",
                "Direct Demand - Sales Orders")) {

            ConfiguredViewDTO configuredViewDTO = getBaseConfiguredViewDTO();
            configuredViewDTO.keyFigureList = List.of(
                    keyFigureDTO(enterpriseDemandPlanningKeyFigure, false, 1));

            /*
             * Nenhum repository e injetado neste teste: a validacao deve falhar
             * na allowlist Community de KFs de Demand Planning antes de tentar
             * ler ou salvar a view persistida.
             */
            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false),
                    "Expected Enterprise guard for Demand Planning KF " + enterpriseDemandPlanningKeyFigure);

        }

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectSupplyCustomerOrdersKeyFigure() {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();

        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = "DEBUG";
        configuredViewDTO.viewName = "SNP";
        configuredViewDTO.viewType = ConfiguredView.TipoView.SUPPLYPLANNINGBOOK;
        configuredViewDTO.keyFigureList = List.of(
                keyFigureDTO("Direct Demand - Sales Orders", false, 1));

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false));

    }

    @Test
    public void saveConfiguredViewDTOShouldAcceptDefaultSupplyPlanningBookKeyFigures() throws Exception {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();
        ConfiguredViewRepositoryStub configuredViewRepositoryStub = new ConfiguredViewRepositoryStub();
        setPrivateField(
                configuredViewFrontService,
                "configuredViewRepository",
                configuredViewRepositoryStub.getRepository());
        setPrivateField(
                configuredViewFrontService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository());
        setPrivateField(
                configuredViewFrontService,
                "configuredViewKeyFigureRepository",
                configuredViewKeyFigureRepository());

        ConfiguredViewDTO configuredViewDTO = getBaseConfiguredViewDTO();
        configuredViewDTO.viewType = ConfiguredView.TipoView.SUPPLYPLANNINGBOOK;
        configuredViewDTO.keyFigureList = SupplyPlanningPlanningBookCatalog
                .getKeyFiguresVisiveisSupplyPlanningBookCommunity()
                .stream()
                .map(keyFigure -> keyFigureDTO(keyFigure, false, 1))
                .toList();

        configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false);

        /*
         * O RuntimeInfo publica ids tecnicos de Supply com `-Working Plan`.
         * Estes mesmos ids precisam continuar aceitos na borda de views para
         * que a SPA possa reenviar a configuracao padrao sem conhecer classes
         * de projection ou reconstruir labels manualmente.
         */
        Assertions.assertNotNull(configuredViewRepositoryStub.savedEntity);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectSupplyFirmInboundOrdersKeyFigure() {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();

        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = "DEBUG";
        configuredViewDTO.viewName = "SNP";
        configuredViewDTO.viewType = ConfiguredView.TipoView.SUPPLYPLANNINGBOOK;
        configuredViewDTO.keyFigureList = List.of(
                keyFigureDTO("Inbound Orders", false, 1));

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false));

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectMaterialLocationDfuFilters() {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();

        ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO = new ConfiguredViewCaracteristicaDTO();
        configuredViewCaracteristicaDTO.characteristicId = "CHANNEL";
        configuredViewCaracteristicaDTO.filteredValues = List.of("RETAIL");

        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = "DEBUG";
        configuredViewDTO.viewName = "DFU Filter";
        configuredViewDTO.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;
        configuredViewDTO.materialLocationCharacteristicDetailList = List.of(configuredViewCaracteristicaDTO);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false));

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectEveryEnterpriseValueDependentFieldBeforeRepository() throws Exception {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();
        Map<String, Object> enterpriseFieldValuesByName = new LinkedHashMap<>();
        enterpriseFieldValuesByName.put("directDemandUpdateKeyFigure", "Enterprise Custom KF");
        enterpriseFieldValuesByName.put("materialCharacteristicDetailList", List.of(characteristicDTO()));
        enterpriseFieldValuesByName.put("locationCharacteristicDetailList", List.of(characteristicDTO()));
        enterpriseFieldValuesByName.put("materialLocationCharacteristicDetailList", List.of(characteristicDTO()));
        enterpriseFieldValuesByName.put("showMaterialLevel", false);
        enterpriseFieldValuesByName.put("showLocationLevel", false);
        enterpriseFieldValuesByName.put("keyFigureList", List.of(
                keyFigureDTO("Direct Demand - Sales Orders", false, 1)));
        enterpriseFieldValuesByName.put("demandPlanWorkflowId", "WF_01");
        enterpriseFieldValuesByName.put("demandPlanWorkflowStageId", "WF_STAGE_01");

        /*
         * Nenhum repository e injetado de proposito: todos estes campos devem
         * falhar na validacao Community antes de qualquer leitura/gravação da
         * view persistida.
         */
        for (Map.Entry<String, Object> enterpriseFieldValueEntry : enterpriseFieldValuesByName.entrySet()) {
            ConfiguredViewDTO configuredViewDTO = getBaseConfiguredViewDTO();
            Field field = ConfiguredViewDTO.class.getDeclaredField(enterpriseFieldValueEntry.getKey());
            field.setAccessible(true);
            field.set(configuredViewDTO, enterpriseFieldValueEntry.getValue());

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> configuredViewFrontService.saveConfiguredViewDTO(configuredViewDTO, "DEBUG", false),
                    "Expected Enterprise guard for ConfiguredViewDTO." + enterpriseFieldValueEntry.getKey());
        }

    }

    private static ConfiguredViewDTO getBaseConfiguredViewDTO() {

        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = "DEBUG";
        configuredViewDTO.viewName = "Community View";
        configuredViewDTO.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;
        configuredViewDTO.unitOfMeasure = "UN";
        configuredViewDTO.showMaterialLevel = true;
        configuredViewDTO.showLocationLevel = true;

        return configuredViewDTO;

    }

    private static ConfiguredView configuredView(
            String userId,
            String viewName,
            ConfiguredView.TipoView tipoView) {

        return new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                userId,
                viewName,
                tipoView));

    }

    private static ConfiguredViewDTO configuredViewDTOListagem(
            String userId,
            String viewName,
            ConfiguredView.TipoView tipoView) {

        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = userId;
        configuredViewDTO.viewName = viewName;
        configuredViewDTO.viewType = tipoView;
        configuredViewDTO.unitOfMeasure = "UN";
        configuredViewDTO.showMaterialLevel = true;
        configuredViewDTO.showLocationLevel = true;
        configuredViewDTO.materialCharacteristicDetailList = List.of();
        configuredViewDTO.locationCharacteristicDetailList = List.of();
        configuredViewDTO.materialLocationCharacteristicDetailList = List.of();
        configuredViewDTO.keyFigureList = List.of();

        return configuredViewDTO;

    }

    private static ConfiguredViewFacade configuredViewFrontServiceParaListagem(
            ConfiguredViewRepositoryStub configuredViewRepositoryStub,
            List<ConfiguredViewDTO> configuredViewDTOList) {

        ConfiguredViewFacade configuredViewFrontService =
                new ConfiguredViewFacade();
        setPrivateField(
                configuredViewFrontService,
                "configuredViewRepository",
                configuredViewRepositoryStub.getRepository());
        setPrivateField(
                configuredViewFrontService,
                "configuredViewAutoMapper",
                configuredViewAutoMapper(configuredViewDTOList));
        setPrivateField(
                configuredViewFrontService,
                "parametrosGlobaisService",
                new TestParametrosGlobaisService());
        setPrivateField(
                configuredViewFrontService,
                "configuredViewKeyFigureRepository",
                configuredViewKeyFigureRepository());

        return configuredViewFrontService;

    }

    private static ConfiguredViewCaracteristicaDTO characteristicDTO() {

        ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO = new ConfiguredViewCaracteristicaDTO();
        configuredViewCaracteristicaDTO.characteristicId = "CHANNEL";
        configuredViewCaracteristicaDTO.filteredValues = List.of("RETAIL");

        return configuredViewCaracteristicaDTO;

    }

    private static ConfiguredViewKeyFigureDTO keyFigureDTO(String keyFigure, Boolean allowChanges, Integer position) {

        ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO = new ConfiguredViewKeyFigureDTO();
        configuredViewKeyFigureDTO.keyFigure = keyFigure;
        configuredViewKeyFigureDTO.allowChanges = allowChanges;
        configuredViewKeyFigureDTO.position = position;

        return configuredViewKeyFigureDTO;

    }

    private static Set<String> getDeclaredFieldNames(Class<?> dtoClass) {

        return Arrays.stream(dtoClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

    }

    private static void assertInvalidViewName(Runnable viewCreationRunnable) {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                viewCreationRunnable::run);

        Assertions.assertEquals(
                "View name must be between 1 and 100 characters long.",
                illegalArgumentException.getMessage());

    }

    private static void assertInvalidConfiguredViewPayload(ConfiguredViewOperation configuredViewOperation) {

        Assertions.assertThrows(
                IllegalArgumentException.class,
                configuredViewOperation::run);

    }

    private static UnidadeMedidaRepository unidadeMedidaRepository() {

        return unidadeMedidaRepositoryRetornando(Optional.of(new UnidadeMedida("UN")));

    }

    private static UnidadeMedidaRepository unidadeMedidaRepositoryRetornando(
            Optional<UnidadeMedida> unidadeMedidaOptional) {

        return (UnidadeMedidaRepository) Proxy.newProxyInstance(
                UnidadeMedidaRepository.class.getClassLoader(),
                new Class[]{UnidadeMedidaRepository.class},
                new UnidadeMedidaRepositoryStub(unidadeMedidaOptional));

    }

    private static ConfiguredViewKeyFigureRepository configuredViewKeyFigureRepository() {

        return (ConfiguredViewKeyFigureRepository) Proxy.newProxyInstance(
                ConfiguredViewKeyFigureRepository.class.getClassLoader(),
                new Class[]{ConfiguredViewKeyFigureRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAllByConfiguredViewIn" -> List.of();
                    case "deleteAll", "saveAll", "deleteAllByConfiguredView" -> null;
                    case "toString" -> "ConfiguredViewKeyFigureRepositoryStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(
                            "Metodo nao suportado no stub: " + method.getName());
                });

    }

    private static ConfiguredViewAutoMapper configuredViewAutoMapper(
            List<ConfiguredViewDTO> configuredViewDTOList) {

        return (ConfiguredViewAutoMapper) Proxy.newProxyInstance(
                ConfiguredViewAutoMapper.class.getClassLoader(),
                new Class[]{ConfiguredViewAutoMapper.class},
                new ConfiguredViewAutoMapperStub(configuredViewDTOList));

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) {

        ReflectionTestUtils.setField(
                target,
                fieldName,
                value);

    }

    private static class ConfiguredViewRepositoryStub implements InvocationHandler {

        private ConfiguredView existingEntity;
        private ConfiguredView savedEntity;
        private ConfiguredView savedEntityReturnedBySave;
        private List<ConfiguredView> configuredViewListByUserAndType = List.of();
        private boolean saveReturnsNull;
        private boolean findReturnsNullOptional;
        private boolean findListReturnsNull;

        private ConfiguredViewRepository getRepository() {

            return (ConfiguredViewRepository) Proxy.newProxyInstance(
                    ConfiguredViewRepository.class.getClassLoader(),
                    new Class[]{ConfiguredViewRepository.class},
                    this);

        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {

            return switch (method.getName()) {
                case "findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView" ->
                        findReturnsNullOptional ? null : Optional.ofNullable(existingEntity);
                case "findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyTipoView" ->
                        findListReturnsNull ? null : configuredViewListByUserAndType;
                case "save" -> {
                    savedEntity = (ConfiguredView) args[0];
                    existingEntity = savedEntity;
                    yield saveReturnsNull ? null
                            : savedEntityReturnedBySave != null ? savedEntityReturnedBySave : savedEntity;
                }
                case "toString" -> "ConfiguredViewRepositoryStub";
                case "hashCode" -> System.identityHashCode(this);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Metodo nao suportado no stub: " + method.getName());
            };

        }

    }

    private static class UnidadeMedidaRepositoryStub implements InvocationHandler {

        private final Optional<UnidadeMedida> unidadeMedidaOptional;

        private UnidadeMedidaRepositoryStub(Optional<UnidadeMedida> unidadeMedidaOptional) {

            this.unidadeMedidaOptional = unidadeMedidaOptional;

        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {

            return switch (method.getName()) {
                case "findById" -> unidadeMedidaOptional;
                case "toString" -> "UnidadeMedidaRepositoryStub";
                case "hashCode" -> System.identityHashCode(this);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Metodo nao suportado no stub: " + method.getName());
            };

        }

    }

    private static class ConfiguredViewAutoMapperStub implements InvocationHandler {

        private final List<ConfiguredViewDTO> configuredViewDTOList;

        private ConfiguredViewAutoMapperStub(List<ConfiguredViewDTO> configuredViewDTOList) {

            this.configuredViewDTOList = configuredViewDTOList;

        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {

            return switch (method.getName()) {
                case "converteComKeyFigures" -> configuredViewDTOList == null
                        ? null
                        : configuredViewDTOList.getFirst();
                case "toString" -> "ConfiguredViewAutoMapperStub";
                case "hashCode" -> System.identityHashCode(this);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Metodo nao suportado no stub: " + method.getName());
            };

        }

    }

    private static class TestParametrosGlobaisService extends ParametrosGlobaisService {

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return new ParametrosGlobais();

        }

    }

    @FunctionalInterface
    private interface ConfiguredViewOperation {

        void run() throws Exception;

    }

}
