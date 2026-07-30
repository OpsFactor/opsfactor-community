package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.PerfilExecucaoDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.mapper.PerfilExecucaoDemandPlanAutoMapper;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.demandplanning.configuration.repository.PerfilExecucaoDemandPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.service.UnidadeMedidaService;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Garante que o perfil Community aceite apenas sell-out como documento
 * historico de demanda. Sell-in e sales orders pertencem ao Enterprise e
 * precisam falhar antes da persistencia.
 */
public class PerfilExecucaoDemandPlanFacadeTest {

    private static final Set<String> COMMUNITY_ACCEPTED_FIELD_NAMES = Set.of(
            "id",
            "description",
            "bucketSize",
            "planningHorizonInPeriods",
            "constrainPlanEditPeriods",
            "initialPlanEditPeriod",
            "finalPlanEditPeriod",
            "defaultDemandPlanningUomId");

    @Test
    public void getPerfilExecucaoDemandPlanDTOSetShouldRejectBrokenMapperSnapshotBeforeReturning() {

        PerfilExecucaoDemandPlanFacade serviceComDTOListNula =
                criaPerfilExecucaoDemandPlanFrontServiceParaListagem(null);

        IllegalStateException nullDTOListException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDTOListNula::getPerfilExecucaoDemandPlanDTOSet);
        Assertions.assertEquals(
                "Demand Planning execution profile DTO listing requires mapper result.",
                nullDTOListException.getMessage());

        List<PerfilExecucaoDemandPlanDTO> perfilExecucaoDemandPlanDTOListComItemNulo =
                new java.util.ArrayList<>();
        perfilExecucaoDemandPlanDTOListComItemNulo.add(null);
        PerfilExecucaoDemandPlanFacade serviceComDTOItemNulo =
                criaPerfilExecucaoDemandPlanFrontServiceParaListagem(
                        perfilExecucaoDemandPlanDTOListComItemNulo);

        IllegalStateException nullDTOItemException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDTOItemNulo::getPerfilExecucaoDemandPlanDTOSet);
        Assertions.assertEquals(
                "Demand Planning execution profile DTO at index 0 is required in list snapshot.",
                nullDTOItemException.getMessage());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTOSemId =
                criaPerfilExecucaoDemandPlanDTOListagemCommunity(null);
        PerfilExecucaoDemandPlanFacade serviceComDTOSemId =
                criaPerfilExecucaoDemandPlanFrontServiceParaListagem(
                        List.of(perfilExecucaoDemandPlanDTOSemId));

        IllegalStateException missingDTOIdException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDTOSemId::getPerfilExecucaoDemandPlanDTOSet);
        Assertions.assertEquals(
                "Demand Planning execution profile DTO at index 0 has no id in list snapshot.",
                missingDTOIdException.getMessage());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTOComSellIn =
                criaPerfilExecucaoDemandPlanDTOListagemCommunity("PERFIL_PADRAO");
        perfilExecucaoDemandPlanDTOComSellIn.historicalSalesDocumentType =
                Constantes.TipoDocumentoVenda.SELLIN;
        PerfilExecucaoDemandPlanFacade serviceComSellIn =
                criaPerfilExecucaoDemandPlanFrontServiceParaListagem(
                        List.of(perfilExecucaoDemandPlanDTOComSellIn));

        IllegalStateException sellInDTOException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComSellIn::getPerfilExecucaoDemandPlanDTOSet);
        Assertions.assertEquals(
                "Demand Planning execution profile DTO at index 0 must use sell-out historical sales in Community.",
                sellInDTOException.getMessage());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTOComAutoFit =
                criaPerfilExecucaoDemandPlanDTOListagemCommunity("PERFIL_PADRAO");
        perfilExecucaoDemandPlanDTOComAutoFit.defaultAutoTunedDemandPlanConfigurationId = 10L;
        PerfilExecucaoDemandPlanFacade serviceComAutoFit =
                criaPerfilExecucaoDemandPlanFrontServiceParaListagem(
                        List.of(perfilExecucaoDemandPlanDTOComAutoFit));

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                serviceComAutoFit::getPerfilExecucaoDemandPlanDTOSet);

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectMissingPayload() {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService =
                new PerfilExecucaoDemandPlanFacade();

        IllegalArgumentException missingPayloadException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(null));
        Assertions.assertEquals(
                "Demand Planning execution profile DTO is required.",
                missingPayloadException.getMessage());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = " ";

        IllegalArgumentException missingIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(
                        perfilExecucaoDemandPlanDTO));
        Assertions.assertEquals(
                "Demand Planning execution profile id is required.",
                missingIdException.getMessage());

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectIdLongerThanPersistedColumnBeforeRepository() {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService =
                new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO =
                criaPerfilExecucaoDemandPlanDTOCommunityMinimoParaTeste();
        perfilExecucaoDemandPlanDTO.id = "P".repeat(51);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(
                        perfilExecucaoDemandPlanDTO));

        Assertions.assertEquals(
                "Demand Planning execution profile id must be at most 50 characters long.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectNonPositivePlanningHorizonBeforeRepository() {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService =
                new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO =
                criaPerfilExecucaoDemandPlanDTOCommunityMinimoParaTeste();
        perfilExecucaoDemandPlanDTO.planningHorizonInPeriods = 0;

        /*
         * O horizonte e campo Community real. Valor preenchido invalido deve
         * falhar como erro funcional do payload, antes de repository, UOM ou
         * qualquer validacao de capability Enterprise.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(
                        perfilExecucaoDemandPlanDTO));

        Assertions.assertEquals(
                "Demand Planning execution profile planning horizon must be positive.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectEnterpriseHistoricalSalesDocumentType() {
        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService = new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub = new PerfilExecucaoDemandPlanRepositoryStub();
        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlanPersistido = new PerfilExecucaoDemandPlan("PERFIL_PADRAO");
        perfilExecucaoDemandPlanPersistido.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);
        perfilExecucaoDemandPlanRepositoryStub.existingEntity = perfilExecucaoDemandPlanPersistido;

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = "PERFIL_PADRAO";
        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType = Constantes.TipoDocumentoVenda.SELLIN;

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(perfilExecucaoDemandPlanDTO));
        Assertions.assertNull(perfilExecucaoDemandPlanRepositoryStub.savedEntity);
    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectSalesOrdersHistoricalSalesDocumentType() {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService = new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub = new PerfilExecucaoDemandPlanRepositoryStub();
        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlanPersistido = new PerfilExecucaoDemandPlan("PERFIL_PADRAO");
        perfilExecucaoDemandPlanPersistido.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);
        perfilExecucaoDemandPlanRepositoryStub.existingEntity = perfilExecucaoDemandPlanPersistido;

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = "PERFIL_PADRAO";
        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType = Constantes.TipoDocumentoVenda.PEDIDO;

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(perfilExecucaoDemandPlanDTO));
        Assertions.assertNull(perfilExecucaoDemandPlanRepositoryStub.savedEntity);

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectEveryNonCommunityFieldBeforeRepositories() throws Exception {

        /*
         * O perfil Demand Community aceita apenas os campos da allowlist acima.
         * `historicalSalesDocumentType` fica fora da allowlist simples porque
         * ele tem validacao por valor: `SELLOUT` e Community, `SELLIN` e
         * `PEDIDO` sao Enterprise.
         *
         * Todo o restante existe no DTO para desserializar payloads legados ou
         * transicionais e precisa falhar antes de repository, mapper ou servicos
         * auxiliares.
         */
        for (Field field : PerfilExecucaoDemandPlanDTO.class.getDeclaredFields()) {
            if (field.isSynthetic() || COMMUNITY_ACCEPTED_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            Object enterpriseFieldValue = getEnterpriseFieldValue(field);
            Assertions.assertNotNull(
                    enterpriseFieldValue,
                    "Campo sem valor de teste Enterprise configurado: " + field.getName());

            PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService =
                    new PerfilExecucaoDemandPlanFacade();
            PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
            perfilExecucaoDemandPlanDTO.id = "PERFIL_PADRAO";
            field.setAccessible(true);
            field.set(perfilExecucaoDemandPlanDTO, enterpriseFieldValue);

            RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(
                            perfilExecucaoDemandPlanDTO),
                    "Campo Enterprise aceito silenciosamente no Community: " + field.getName());

            Assertions.assertTrue(
                    requiresEnterpriseVersionException.getMessage().startsWith(
                            RequiresEnterpriseVersionException.ERROR_CODE));
        }

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldPersistSelloutAsCommunityHistoricalSalesDocumentType() {
        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService = new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub = new PerfilExecucaoDemandPlanRepositoryStub();
        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlanPersistido = new PerfilExecucaoDemandPlan("PERFIL_PADRAO");
        perfilExecucaoDemandPlanPersistido.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);
        perfilExecucaoDemandPlanRepositoryStub.existingEntity = perfilExecucaoDemandPlanPersistido;

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = "PERFIL_PADRAO";
        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType = Constantes.TipoDocumentoVenda.SELLOUT;

        perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(perfilExecucaoDemandPlanDTO);

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);

        Assertions.assertEquals(
                Constantes.TipoDocumentoVenda.SELLOUT,
                perfilExecucaoDemandPlanRepositoryStub.savedEntity.getTipoDocumentoVenda(parametrosGlobais));
    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectNullSavedSnapshotAfterRepository() {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService =
                new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub =
                new PerfilExecucaoDemandPlanRepositoryStub();
        perfilExecucaoDemandPlanRepositoryStub.saveReturnsNull = true;
        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO =
                criaPerfilExecucaoDemandPlanDTOCommunityMinimoParaTeste();

        /*
         * A borda Community nao retorna DTO, mas deve garantir que o repository
         * devolveu um snapshot minimamente integro antes de considerar o save
         * concluido para o front.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(
                        perfilExecucaoDemandPlanDTO));

        Assertions.assertEquals(
                "Saved Demand Planning execution profile snapshot is required after Community profile persistence.",
                illegalStateException.getMessage());

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectBrokenSavedSnapshotAfterRepository() {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService =
                new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub =
                new PerfilExecucaoDemandPlanRepositoryStub();
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlanSalvoSemId =
                new PerfilExecucaoDemandPlan();
        perfilExecucaoDemandPlanSalvoSemId.setId(" ");
        perfilExecucaoDemandPlanRepositoryStub.savedEntityReturnedBySave =
                perfilExecucaoDemandPlanSalvoSemId;
        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO =
                criaPerfilExecucaoDemandPlanDTOCommunityMinimoParaTeste();

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(
                        perfilExecucaoDemandPlanDTO));

        Assertions.assertEquals(
                "Saved Demand Planning execution profile id is required after Community profile persistence.",
                illegalStateException.getMessage());

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectNullRepositoryOptionalBeforeCreatingEntity() {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService =
                new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub =
                new PerfilExecucaoDemandPlanRepositoryStub();
        perfilExecucaoDemandPlanRepositoryStub.findByIdReturnsNullOptional = true;
        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = "PERFIL_PADRAO";
        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType = Constantes.TipoDocumentoVenda.SELLOUT;

        IllegalStateException nullOptionalException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(
                        perfilExecucaoDemandPlanDTO));
        Assertions.assertEquals(
                "Demand Planning execution profile repository returned null Optional for profile PERFIL_PADRAO.",
                nullOptionalException.getMessage());
        Assertions.assertNull(perfilExecucaoDemandPlanRepositoryStub.savedEntity);

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectMapeAggregationLevels() {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService = new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub = new PerfilExecucaoDemandPlanRepositoryStub();
        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = "PERFIL_PADRAO";
        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType = Constantes.TipoDocumentoVenda.SELLOUT;
        perfilExecucaoDemandPlanDTO.mapeMaterialAggregationLevelId = "MAPE_MATERIAL";

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(perfilExecucaoDemandPlanDTO));
        Assertions.assertNull(perfilExecucaoDemandPlanRepositoryStub.savedEntity);

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectMapeLocationAggregationLevel() {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService = new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub = new PerfilExecucaoDemandPlanRepositoryStub();
        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = "PERFIL_PADRAO";
        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType = Constantes.TipoDocumentoVenda.SELLOUT;
        perfilExecucaoDemandPlanDTO.mapeLocationAggregationLevelId = "MAPE_LOCATION";

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(perfilExecucaoDemandPlanDTO));
        Assertions.assertNull(perfilExecucaoDemandPlanRepositoryStub.savedEntity);

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectAutoFitConfiguration() {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService = new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub = new PerfilExecucaoDemandPlanRepositoryStub();
        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = "PERFIL_PADRAO";
        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType = Constantes.TipoDocumentoVenda.SELLOUT;
        perfilExecucaoDemandPlanDTO.defaultAutoTunedDemandPlanConfigurationId = 10L;

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(perfilExecucaoDemandPlanDTO));
        Assertions.assertNull(perfilExecucaoDemandPlanRepositoryStub.savedEntity);

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectAutoFitExecutionParameters() {

        /*
         * Auto-fit execution e um bloco inteiro Enterprise. O teste percorre
         * cada campo do DTO para garantir que nenhum deles escape da validacao
         * apenas por estar visualmente escondido no front Community.
         */
        for (Consumer<PerfilExecucaoDemandPlanDTO> perfilExecucaoDemandPlanDTOConsumer : List.<Consumer<PerfilExecucaoDemandPlanDTO>>of(
                perfilExecucaoDemandPlanDTO -> perfilExecucaoDemandPlanDTO.autofitModelType = "BEST_MODEL",
                perfilExecucaoDemandPlanDTO -> perfilExecucaoDemandPlanDTO.modelAutofitObjectiveFunction = "MAPE",
                perfilExecucaoDemandPlanDTO -> perfilExecucaoDemandPlanDTO.modelAutofitNumberOfPeriodsForAccuracyEvaluation = 6,
                perfilExecucaoDemandPlanDTO -> perfilExecucaoDemandPlanDTO.modelAutofitEvaluationLagInPeriods = 1)) {
            assertSaveRejectsEnterpriseParameter(perfilExecucaoDemandPlanDTOConsumer);
        }

    }

    @Test
    public void savePerfilExecucaoDemandPlanDTOShouldRejectRegressionTreeParameters() {

        /*
         * A arvore de regressao faz parte da infraestrutura Enterprise de
         * configuracao/auto-fit. No Community, qualquer campo preenchido deve
         * falhar antes de criar ou atualizar o perfil.
         */
        for (Consumer<PerfilExecucaoDemandPlanDTO> perfilExecucaoDemandPlanDTOConsumer : List.<Consumer<PerfilExecucaoDemandPlanDTO>>of(
                perfilExecucaoDemandPlanDTO -> perfilExecucaoDemandPlanDTO.regressionTreeObjectiveFunction = "MAPE",
                perfilExecucaoDemandPlanDTO -> perfilExecucaoDemandPlanDTO.numberOfDimensionsUsedForCandidateSplits = 3,
                perfilExecucaoDemandPlanDTO -> perfilExecucaoDemandPlanDTO.numberOfCandidateSplitsByDimension = 4,
                perfilExecucaoDemandPlanDTO -> perfilExecucaoDemandPlanDTO.maxDepthAfterLastConfirmedSplit = 2,
                perfilExecucaoDemandPlanDTO -> perfilExecucaoDemandPlanDTO.minimumPercentErrorReductionForNewSplits = 0.05,
                perfilExecucaoDemandPlanDTO -> perfilExecucaoDemandPlanDTO.numberOfPeriodsForRegressionTreePruning = 12)) {
            assertSaveRejectsEnterpriseParameter(perfilExecucaoDemandPlanDTOConsumer);
        }

    }

    private static void assertSaveRejectsEnterpriseParameter(
            Consumer<PerfilExecucaoDemandPlanDTO> perfilExecucaoDemandPlanDTOConsumer) {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService = new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub = new PerfilExecucaoDemandPlanRepositoryStub();
        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = "PERFIL_PADRAO";
        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType = Constantes.TipoDocumentoVenda.SELLOUT;
        perfilExecucaoDemandPlanDTOConsumer.accept(perfilExecucaoDemandPlanDTO);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(perfilExecucaoDemandPlanDTO));
        Assertions.assertNull(perfilExecucaoDemandPlanRepositoryStub.savedEntity);

    }

    private static PerfilExecucaoDemandPlanDTO criaPerfilExecucaoDemandPlanDTOCommunityMinimoParaTeste() {

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = "PERFIL_PADRAO";
        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType = Constantes.TipoDocumentoVenda.SELLOUT;
        return perfilExecucaoDemandPlanDTO;

    }

    private static PerfilExecucaoDemandPlanFacade criaPerfilExecucaoDemandPlanFrontServiceParaListagem(
            List<PerfilExecucaoDemandPlanDTO> perfilExecucaoDemandPlanDTOList) {

        PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService =
                new PerfilExecucaoDemandPlanFacade();
        PerfilExecucaoDemandPlanRepositoryStub perfilExecucaoDemandPlanRepositoryStub =
                new PerfilExecucaoDemandPlanRepositoryStub();
        perfilExecucaoDemandPlanRepositoryStub.customFindAllResult =
                List.of(new PerfilExecucaoDemandPlan("PERFIL_PADRAO"));

        setPerfilExecucaoDemandPlanRepository(
                perfilExecucaoDemandPlanFrontService,
                perfilExecucaoDemandPlanRepositoryStub.getRepository());
        setParametrosGlobaisService(
                perfilExecucaoDemandPlanFrontService,
                new ParametrosGlobaisServiceStub(new ParametrosGlobais()));
        setPerfilExecucaoDemandPlanAutoMapper(
                perfilExecucaoDemandPlanFrontService,
                getPerfilExecucaoDemandPlanAutoMapperComLista(perfilExecucaoDemandPlanDTOList));
        return perfilExecucaoDemandPlanFrontService;

    }

    private static PerfilExecucaoDemandPlanDTO criaPerfilExecucaoDemandPlanDTOListagemCommunity(String id) {

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO = new PerfilExecucaoDemandPlanDTO();
        perfilExecucaoDemandPlanDTO.id = id;
        perfilExecucaoDemandPlanDTO.historicalSalesDocumentType = Constantes.TipoDocumentoVenda.SELLOUT;
        return perfilExecucaoDemandPlanDTO;

    }

    private static void setPerfilExecucaoDemandPlanRepository(
            PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService,
            PerfilExecucaoDemandPlanRepository perfilExecucaoDemandPlanRepository) {

        try {
            Field perfilExecucaoDemandPlanRepositoryField = PerfilExecucaoDemandPlanFacade.class.getDeclaredField(
                    "perfilExecucaoDemandPlanRepository");
            perfilExecucaoDemandPlanRepositoryField.setAccessible(true);
            perfilExecucaoDemandPlanRepositoryField.set(
                    perfilExecucaoDemandPlanFrontService,
                    perfilExecucaoDemandPlanRepository);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nao foi possivel injetar o stub de repository no teste", e);
        }

    }

    private static void setPerfilExecucaoDemandPlanAutoMapper(
            PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService,
            PerfilExecucaoDemandPlanAutoMapper perfilExecucaoDemandPlanAutoMapper) {

        try {
            Field perfilExecucaoDemandPlanAutoMapperField = PerfilExecucaoDemandPlanFacade.class.getDeclaredField(
                    "perfilExecucaoDemandPlanAutoMapper");
            perfilExecucaoDemandPlanAutoMapperField.setAccessible(true);
            perfilExecucaoDemandPlanAutoMapperField.set(
                    perfilExecucaoDemandPlanFrontService,
                    perfilExecucaoDemandPlanAutoMapper);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nao foi possivel injetar o stub de mapper no teste", e);
        }

    }

    private static void setParametrosGlobaisService(
            PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService,
            ParametrosGlobaisService parametrosGlobaisService) {

        try {
            Field parametrosGlobaisServiceField = PerfilExecucaoDemandPlanFacade.class.getDeclaredField(
                    "parametrosGlobaisService");
            parametrosGlobaisServiceField.setAccessible(true);
            parametrosGlobaisServiceField.set(
                    perfilExecucaoDemandPlanFrontService,
                    parametrosGlobaisService);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nao foi possivel injetar o stub de parametros globais no teste", e);
        }

    }

    private static void setUnidadeMedidaService(
            PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService,
            UnidadeMedidaService unidadeMedidaService) {

        try {
            Field unidadeMedidaServiceField = PerfilExecucaoDemandPlanFacade.class.getDeclaredField(
                    "unidadeMedidaService");
            unidadeMedidaServiceField.setAccessible(true);
            unidadeMedidaServiceField.set(
                    perfilExecucaoDemandPlanFrontService,
                    unidadeMedidaService);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nao foi possivel injetar o stub de unidade de medida no teste", e);
        }

    }

    private static Object getEnterpriseFieldValue(Field field) {

        Class<?> fieldType = field.getType();
        if (String.class.equals(fieldType)) {
            return "enterprise-value";
        }
        if (Integer.class.equals(fieldType)) {
            return 1;
        }
        if (Long.class.equals(fieldType)) {
            return 1L;
        }
        if (Double.class.equals(fieldType)) {
            return 1.0d;
        }
        if (Constantes.TipoDocumentoVenda.class.equals(fieldType)) {
            return Constantes.TipoDocumentoVenda.PEDIDO;
        }
        return null;

    }

    private static PerfilExecucaoDemandPlanAutoMapper getPerfilExecucaoDemandPlanAutoMapperComLista(
            List<PerfilExecucaoDemandPlanDTO> perfilExecucaoDemandPlanDTOList) {

        return (PerfilExecucaoDemandPlanAutoMapper) Proxy.newProxyInstance(
                PerfilExecucaoDemandPlanAutoMapper.class.getClassLoader(),
                new Class[]{PerfilExecucaoDemandPlanAutoMapper.class},
                (proxy, method, args) -> {

                    if (method.getDeclaringClass().equals(Object.class)) {
                        return switch (method.getName()) {
                            case "toString" -> "PerfilExecucaoDemandPlanAutoMapper com lista para teste Community";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new AssertionError("Metodo Object inesperado: " + method.getName());
                        };
                    }
                    if ("converteListaEntidadesParaDtoList".equals(method.getName())) {
                        return perfilExecucaoDemandPlanDTOList;
                    }

                    throw new AssertionError("Metodo inesperado no teste: " + method.getName());

                });

    }

    /**
     * Stub minimo de repositório para validar apenas o comportamento de save/findById
     * sem depender de Mockito legado incompatível com Java 21 neste módulo.
     */
    private static class PerfilExecucaoDemandPlanRepositoryStub implements InvocationHandler {

        private PerfilExecucaoDemandPlan existingEntity;
        private PerfilExecucaoDemandPlan savedEntity;
        private PerfilExecucaoDemandPlan savedEntityReturnedBySave;
        private List<PerfilExecucaoDemandPlan> customFindAllResult;
        private boolean customFindAllReturnsNull;
        private boolean findByIdReturnsNullOptional;
        private boolean saveReturnsNull;

        private PerfilExecucaoDemandPlanRepository getRepository() {
            return (PerfilExecucaoDemandPlanRepository) Proxy.newProxyInstance(
                    PerfilExecucaoDemandPlanRepository.class.getClassLoader(),
                    new Class[]{PerfilExecucaoDemandPlanRepository.class},
                    this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findById", "customFindById" -> findByIdReturnsNullOptional ?
                        null
                        : Optional.ofNullable(existingEntity);
                case "customFindAll" -> customFindAllReturnsNull ?
                        null
                        : customFindAllResult != null ?
                        customFindAllResult
                        : existingEntity == null ? List.of() : List.of(existingEntity);
                case "save" -> {
                    savedEntity = (PerfilExecucaoDemandPlan) args[0];
                    existingEntity = savedEntity;
                    yield saveReturnsNull ? null
                            : savedEntityReturnedBySave != null ? savedEntityReturnedBySave : savedEntity;
                }
                case "toString" -> "PerfilExecucaoDemandPlanRepositoryStub";
                case "hashCode" -> System.identityHashCode(this);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Metodo nao suportado no stub: " + method.getName());
            };
        }
    }

    private static class ParametrosGlobaisServiceStub extends ParametrosGlobaisService {

        private final ParametrosGlobais parametrosGlobais;

        private ParametrosGlobaisServiceStub(ParametrosGlobais parametrosGlobais) {
            this.parametrosGlobais = parametrosGlobais;
        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {
            return parametrosGlobais;
        }
    }

    private static class UnidadeMedidaServiceStub extends UnidadeMedidaService {

        private final UnidadeMedida unidadeMedida;

        private UnidadeMedidaServiceStub(UnidadeMedida unidadeMedida) {
            this.unidadeMedida = unidadeMedida;
        }

        @Override
        public UnidadeMedida getUnidadeMedidaDeId(String id) {
            return unidadeMedida;
        }
    }
}
