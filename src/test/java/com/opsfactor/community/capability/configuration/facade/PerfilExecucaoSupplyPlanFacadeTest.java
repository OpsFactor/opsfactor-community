package com.opsfactor.community.capability.configuration.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.supplyplanning.configuration.facade.dto.PerfilExecucaoSupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.configuration.facade.mapper.PerfilExecucaoSupplyPlanAutoMapper;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesRepository;
import com.opsfactor.community.capability.supplyplanning.configuration.repository.PerfilExecucaoSupplyPlanRepository;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Valida o contrato Community do perfil de execucao de Supply Planning.
 *
 * Os metodos testados sao privados porque a persistencia completa depende de
 * mapper/repository. A reflexao mantem o teste concentrado nas travas que
 * protegem o backend contra payloads Enterprise do front compartilhado.
 */
public class PerfilExecucaoSupplyPlanFacadeTest {

    private static final Set<String> COMMUNITY_ACCEPTED_FIELD_NAMES = Set.of(
            "id",
            "description",
            "generatePlannedInboundOrders",
            "generatePlannedProductionOrders",
            "generatePlannedInboundOrdersWhenProductionIsViable",
            "alwaysUseDrp",
            "planHorizonInDays",
            "automaticallyRunConstrainedPlan",
            "roundRequisitionsByMoqAndLotSize",
            "roundRequisitionsByMoqAndLotSizeForAllExpeditionPeriods",
            "expeditionPeriodsToRoundRequisitionsByMoqAndLotSize",
            "roundProductionByMoqAndLotSize",
            "roundProductionByMoqAndLotSizeForAllPeriods",
            "periodsToRoundProductionByMoqAndLotSize",
            "considerInitialStock",
            "saveInventoryPlan",
            "targetStockModel",
            "generateUnconstrainedPlan",
            "ignoreProductionConstraintsForUnconstrainedPlan",
            "considerProductionConstraints",
            "heuristicUnconstrainedPlanCapacityLeveling",
            "consolidateClientDemand",
            "demandConsolidationMode",
            "generateProfitLoss",
            "allowSalesProfitLossBomRetroaction",
            "directDemandFairShare",
            "planTypeForWorkVersion",
            "inventoryPolicyIdSet");

    @Test
    public void serviceShouldUseExplicitAutowiredBeanFields() throws Exception {

        /*
         * O usuario pediu que beans em services fiquem marcados com @Autowired
         * no proprio campo para distinguir dependencias Spring de atributos
         * comuns. Este contrato cobre a borda Community do perfil Supply.
         */
        for (String beanFieldName : List.of(
                "perfilExecucaoSupplyPlanRepository",
                "perfilExecucaoSupplyPlanAutoMapper",
                "politicaEstoquesRepository")) {
            Field beanField = PerfilExecucaoSupplyPlanFacade.class.getDeclaredField(beanFieldName);
            Autowired autowired = beanField.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    "Bean sem @Autowired explicito: " + beanFieldName);
            Assertions.assertTrue(
                    autowired.required(),
                    "Bean obrigatorio marcado como opcional sem necessidade: " + beanFieldName);
        }

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldRejectMissingPayloadBeforeRepositories() {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                new PerfilExecucaoSupplyPlanFacade();

        IllegalArgumentException missingPayloadException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(null));
        Assertions.assertEquals(
                "Supply Planning execution profile DTO is required.",
                missingPayloadException.getMessage());

        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setId("");

        IllegalArgumentException missingIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(
                        perfilExecucaoSupplyPlanDTO));
        Assertions.assertEquals(
                "Supply Planning execution profile id is required.",
                missingIdException.getMessage());

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldRejectNonPositiveOperationalIntegersBeforeRepositories() {

        assertSaveRejectsInvalidOperationalInteger(
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setPlanHorizonInDays(0),
                "Supply Planning execution profile plan horizon must be positive.");
        assertSaveRejectsInvalidOperationalInteger(
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO
                        .setExpeditionPeriodsToRoundRequisitionsByMoqAndLotSize(0),
                "Supply Planning requisition rounding expedition window must be positive.");
        assertSaveRejectsInvalidOperationalInteger(
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO
                        .setPeriodsToRoundProductionByMoqAndLotSize(0),
                "Supply Planning production rounding window must be positive.");

    }

    @Test
    public void getPerfilExecucaoSupplyPlanDTOSetShouldRejectBrokenSnapshotsBeforeMapper()
            throws Exception {

        PerfilExecucaoSupplyPlanFacade serviceComListaNula =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(null);

        IllegalStateException nullSetException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComListaNula::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile list snapshot is required.",
                nullSetException.getMessage());

        List<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanListComItemNulo =
                new ArrayList<>();
        perfilExecucaoSupplyPlanListComItemNulo.add(null);
        PerfilExecucaoSupplyPlanFacade serviceComItemNulo =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
                        perfilExecucaoSupplyPlanListComItemNulo);

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComItemNulo::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile at index 0 is required in list snapshot.",
                nullItemException.getMessage());

        PerfilExecucaoSupplyPlanFacade serviceComPerfilSemId =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
                        List.of(new PerfilExecucaoSupplyPlan()));

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComPerfilSemId::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile at index 0 has no id in list snapshot.",
                missingIdException.getMessage());

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanDuplicadoA =
                criaPerfilExecucaoSupplyPlanParaTeste("supply-profile");
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanDuplicadoB =
                criaPerfilExecucaoSupplyPlanParaTeste("supply-profile");
        PerfilExecucaoSupplyPlanFacade serviceComPerfilDuplicado =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
                        List.of(
                                perfilExecucaoSupplyPlanDuplicadoA,
                                perfilExecucaoSupplyPlanDuplicadoB));

        IllegalStateException duplicatedIdException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComPerfilDuplicado::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile list snapshot has duplicated id supply-profile.",
                duplicatedIdException.getMessage());

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanOtimizador =
                criaPerfilExecucaoSupplyPlanParaTeste("supply-profile");
        perfilExecucaoSupplyPlanOtimizador.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        PerfilExecucaoSupplyPlanFacade serviceComPerfilEnterprise =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
                        List.of(perfilExecucaoSupplyPlanOtimizador));

        IllegalStateException enterpriseModeException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComPerfilEnterprise::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile at index 0 must be heuristic in Community.",
                enterpriseModeException.getMessage());

    }

    @Test
    public void getPerfilExecucaoSupplyPlanDTOSetShouldRejectBrokenMapperSnapshotsBeforeReturning()
            throws Exception {

        List<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanList =
                List.of(criaPerfilExecucaoSupplyPlanParaTeste("supply-profile"));

        PerfilExecucaoSupplyPlanFacade serviceComDTOSetNulo =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
                        perfilExecucaoSupplyPlanList,
                        null);

        IllegalStateException nullDTOSetException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDTOSetNulo::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile DTO list snapshot is required.",
                nullDTOSetException.getMessage());

        List<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOListComItemNulo =
                new ArrayList<>();
        perfilExecucaoSupplyPlanDTOListComItemNulo.add(null);
        PerfilExecucaoSupplyPlanFacade serviceComDTOItemNulo =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
                        perfilExecucaoSupplyPlanList,
                        perfilExecucaoSupplyPlanDTOListComItemNulo);

        IllegalStateException nullDTOItemException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDTOItemNulo::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile DTO at index 0 is required in list snapshot.",
                nullDTOItemException.getMessage());

        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTOSemId =
                criaPerfilExecucaoSupplyPlanDTOListagemParaTeste(null);
        PerfilExecucaoSupplyPlanFacade serviceComDTOSemId =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
                        perfilExecucaoSupplyPlanList,
                        List.of(perfilExecucaoSupplyPlanDTOSemId));

        IllegalStateException missingDTOIdException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDTOSemId::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile DTO at index 0 has no id in list snapshot.",
                missingDTOIdException.getMessage());

        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTODuplicadoA =
                criaPerfilExecucaoSupplyPlanDTOListagemParaTeste("supply-profile");
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTODuplicadoB =
                criaPerfilExecucaoSupplyPlanDTOListagemParaTeste("supply-profile");
        PerfilExecucaoSupplyPlanFacade serviceComDTODuplicado =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
                        perfilExecucaoSupplyPlanList,
                        List.of(
                                perfilExecucaoSupplyPlanDTODuplicadoA,
                                perfilExecucaoSupplyPlanDTODuplicadoB));

        IllegalStateException duplicatedDTOIdException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDTODuplicado::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile DTO list snapshot has duplicated id supply-profile.",
                duplicatedDTOIdException.getMessage());

        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTOOptimizer =
                criaPerfilExecucaoSupplyPlanDTOListagemParaTeste("supply-profile");
        perfilExecucaoSupplyPlanDTOOptimizer.setExecutionModel(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        PerfilExecucaoSupplyPlanFacade serviceComDTOEnterprise =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
                        perfilExecucaoSupplyPlanList,
                        List.of(perfilExecucaoSupplyPlanDTOOptimizer));

        IllegalStateException enterpriseDTOException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDTOEnterprise::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile DTO at index 0 must be heuristic in Community.",
                enterpriseDTOException.getMessage());

        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTOSemPoliticas =
                criaPerfilExecucaoSupplyPlanDTOListagemParaTeste("supply-profile");
        perfilExecucaoSupplyPlanDTOSemPoliticas.setInventoryPolicyIdSet(null);
        PerfilExecucaoSupplyPlanFacade serviceComDTOSemPoliticas =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
                        perfilExecucaoSupplyPlanList,
                        List.of(perfilExecucaoSupplyPlanDTOSemPoliticas));

        IllegalStateException missingInventoryPolicySetException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDTOSemPoliticas::getPerfilExecucaoSupplyPlanDTOSet);
        Assertions.assertEquals(
                "Supply Planning execution profile DTO at index 0 must expose an inventory policy id set in Community.",
                missingInventoryPolicySetException.getMessage());

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldRejectEnterprisePayloadBeforeRepositories() {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setId("supply-profile");
        perfilExecucaoSupplyPlanDTO.setExecutionModel(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);

        /*
         * Nao injetamos mapper/repositories de proposito: payload Enterprise deve
         * falhar na borda Community antes de qualquer acesso a banco ou
         * materializacao de entidade.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(perfilExecucaoSupplyPlanDTO));

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldRejectNullInventoryPolicySetBeforeRepositories() {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setId("supply-profile");
        perfilExecucaoSupplyPlanDTO.setInventoryPolicyIdSet(null);

        /*
         * Lista vazia e snapshot valido para nao associar politicas ao perfil.
         * Nulo nao tem semantica Community e precisa falhar antes de acessar
         * repositories, mantendo erro diagnostico em payload manual/incompleto.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(
                        perfilExecucaoSupplyPlanDTO));

        Assertions.assertEquals(
                "Supply Planning inventory policy id set must be provided",
                illegalArgumentException.getMessage());

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldRejectMissingInventoryPolicyBeforeMapper() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                new PerfilExecucaoSupplyPlanFacade();
        AtomicInteger findAllCallCount = new AtomicInteger(0);
        PoliticaEstoquesRepository politicaEstoquesRepository =
                createPoliticaEstoquesRepositoryProxy(findAllCallCount);
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setId("supply-profile");
        perfilExecucaoSupplyPlanDTO.setInventoryPolicyIdSet(Set.of("missing-policy"));

        /*
         * Politica de estoque operacional existe no Community, mas id
         * inexistente indica referencia a cadastro ausente. O erro precisa
         * acontecer antes do mapper para nao materializar entidade parcial nem
         * depender da criacao tardia da chave composta JPA.
         */
        setPrivateField(
                perfilExecucaoSupplyPlanFrontService,
                "politicaEstoquesRepository",
                politicaEstoquesRepository);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(
                        perfilExecucaoSupplyPlanDTO));

        Assertions.assertEquals(
                "Supply Planning inventory policy not found: missing-policy",
                illegalArgumentException.getMessage());
        Assertions.assertEquals(
                1,
                findAllCallCount.get());

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldRejectBlankInventoryPolicyBeforeMapper() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                new PerfilExecucaoSupplyPlanFacade();
        AtomicInteger findAllCallCount = new AtomicInteger(0);
        PoliticaEstoquesRepository politicaEstoquesRepository =
                createPoliticaEstoquesRepositoryProxy(findAllCallCount);
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setId("supply-profile");
        perfilExecucaoSupplyPlanDTO.setInventoryPolicyIdSet(Set.of(" "));

        /*
         * Id vazio dentro de uma lista tecnicamente presente tambem nao tem
         * semantica Community. A validacao deve falhar antes do mapper, com
         * mensagem de request invalido, nao como ausencia de feature Enterprise.
         */
        setPrivateField(
                perfilExecucaoSupplyPlanFrontService,
                "politicaEstoquesRepository",
                politicaEstoquesRepository);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(
                        perfilExecucaoSupplyPlanDTO));

        Assertions.assertEquals(
                "Supply Planning inventory policy id is required.",
                illegalArgumentException.getMessage());
        Assertions.assertEquals(
                1,
                findAllCallCount.get());

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldRejectBrokenInventoryPolicySnapshotBeforeMapper() throws Exception {

        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO =
                criaPerfilExecucaoSupplyPlanDTOCommunityMinimoParaTeste();

        PerfilExecucaoSupplyPlanFacade serviceComListaNula =
                new PerfilExecucaoSupplyPlanFacade();
        setPrivateField(
                serviceComListaNula,
                "politicaEstoquesRepository",
                createPoliticaEstoquesRepositoryProxy(new AtomicInteger(0), null));

        IllegalStateException listaNulaException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComListaNula.savePerfilExecucaoSupplyPlanDTO(perfilExecucaoSupplyPlanDTO));

        Assertions.assertEquals(
                "Supply Planning inventory policy repository returned null list.",
                listaNulaException.getMessage());

        List<PoliticaEstoques> politicaEstoquesListComItemNulo = new ArrayList<>();
        politicaEstoquesListComItemNulo.add(null);
        PerfilExecucaoSupplyPlanFacade serviceComItemNulo =
                new PerfilExecucaoSupplyPlanFacade();
        setPrivateField(
                serviceComItemNulo,
                "politicaEstoquesRepository",
                createPoliticaEstoquesRepositoryProxy(
                        new AtomicInteger(0),
                        politicaEstoquesListComItemNulo));

        IllegalStateException itemNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComItemNulo.savePerfilExecucaoSupplyPlanDTO(perfilExecucaoSupplyPlanDTO));

        Assertions.assertEquals(
                "Supply Planning inventory policy repository returned null item at index 0.",
                itemNuloException.getMessage());

        PerfilExecucaoSupplyPlanFacade serviceComPolicySemId =
                new PerfilExecucaoSupplyPlanFacade();
        setPrivateField(
                serviceComPolicySemId,
                "politicaEstoquesRepository",
                createPoliticaEstoquesRepositoryProxy(
                        new AtomicInteger(0),
                        List.of(new PoliticaEstoques())));

        IllegalStateException policySemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComPolicySemId.savePerfilExecucaoSupplyPlanDTO(perfilExecucaoSupplyPlanDTO));

        Assertions.assertEquals(
                "Supply Planning inventory policy repository returned item without id at index 0.",
                policySemIdException.getMessage());

        PoliticaEstoques politicaEstoquesUm = new PoliticaEstoques();
        politicaEstoquesUm.setId("INV_POLICY_01");
        PoliticaEstoques politicaEstoquesDois = new PoliticaEstoques();
        politicaEstoquesDois.setId("INV_POLICY_01");
        PerfilExecucaoSupplyPlanFacade serviceComPolicyDuplicada =
                new PerfilExecucaoSupplyPlanFacade();
        setPrivateField(
                serviceComPolicyDuplicada,
                "politicaEstoquesRepository",
                createPoliticaEstoquesRepositoryProxy(
                        new AtomicInteger(0),
                        List.of(politicaEstoquesUm, politicaEstoquesDois)));

        IllegalStateException policyDuplicadaException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComPolicyDuplicada.savePerfilExecucaoSupplyPlanDTO(perfilExecucaoSupplyPlanDTO));

        Assertions.assertEquals(
                "Supply Planning inventory policy repository returned duplicate id INV_POLICY_01.",
                policyDuplicadaException.getMessage());

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldRejectNullSavedSnapshotAfterRepository() throws Exception {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanMapeado =
                criaPerfilExecucaoSupplyPlanParaTeste("supply-profile");
        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                criaPerfilExecucaoSupplyPlanFrontServiceParaSaveSnapshotTest(
                        perfilExecucaoSupplyPlanMapeado,
                        null);
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO =
                criaPerfilExecucaoSupplyPlanDTOCommunityMinimoParaTeste();

        /*
         * O mapper/repository foram injetados apenas para atravessar o caminho
         * real de save. Retorno nulo do repository precisa falhar aqui, antes
         * de a tela considerar o perfil persistido.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(
                        perfilExecucaoSupplyPlanDTO));

        Assertions.assertEquals(
                "Saved Supply Planning execution profile snapshot is required after Community profile persistence.",
                illegalStateException.getMessage());

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldRejectBrokenSavedSnapshotAfterRepository() throws Exception {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanMapeado =
                criaPerfilExecucaoSupplyPlanParaTeste("supply-profile");
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanSalvoSemId =
                criaPerfilExecucaoSupplyPlanParaTeste(" ");
        PerfilExecucaoSupplyPlanFacade serviceComSnapshotSemId =
                criaPerfilExecucaoSupplyPlanFrontServiceParaSaveSnapshotTest(
                        perfilExecucaoSupplyPlanMapeado,
                        perfilExecucaoSupplyPlanSalvoSemId);
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO =
                criaPerfilExecucaoSupplyPlanDTOCommunityMinimoParaTeste();

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComSnapshotSemId.savePerfilExecucaoSupplyPlanDTO(
                        perfilExecucaoSupplyPlanDTO));
        Assertions.assertEquals(
                "Saved Supply Planning execution profile id is required after Community profile persistence.",
                missingIdException.getMessage());

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanSalvoComOptimizer =
                criaPerfilExecucaoSupplyPlanParaTeste("supply-profile");
        perfilExecucaoSupplyPlanSalvoComOptimizer.setModoExecucao(
                PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        PerfilExecucaoSupplyPlanFacade serviceComModoEnterprise =
                criaPerfilExecucaoSupplyPlanFrontServiceParaSaveSnapshotTest(
                        perfilExecucaoSupplyPlanMapeado,
                        perfilExecucaoSupplyPlanSalvoComOptimizer);

        /*
         * Mesmo que o payload tenha passado pelas travas Community, um snapshot
         * salvo com modo Enterprise reintroduz otimizador/process chain na
         * edicao errada e deve ser tratado como persistencia quebrada.
         */
        IllegalStateException executionModeException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComModoEnterprise.savePerfilExecucaoSupplyPlanDTO(
                        perfilExecucaoSupplyPlanDTO));
        Assertions.assertEquals(
                "Saved Supply Planning execution profile must remain heuristic in Community.",
                executionModeException.getMessage());

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldRejectEveryNonCommunityFieldBeforeRepositories() throws Exception {

        /*
         * O DTO e compartilhado com o front Enterprise e possui muitos campos
         * que o Community precisa desserializar para bloquear. Este teste evita
         * regressao silenciosa: qualquer campo fora da allowlist Community deve
         * falhar individualmente antes de mapper/repository.
         */
        for (Field field : PerfilExecucaoSupplyPlanDTO.class.getDeclaredFields()) {
            if (field.isSynthetic() || COMMUNITY_ACCEPTED_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            Object enterpriseFieldValue = getEnterpriseFieldValue(field);
            Assertions.assertNotNull(
                    enterpriseFieldValue,
                    "Campo sem valor de teste Enterprise configurado: " + field.getName());

            PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                    new PerfilExecucaoSupplyPlanFacade();
            PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
            perfilExecucaoSupplyPlanDTO.setId("supply-profile");
            field.setAccessible(true);
            field.set(perfilExecucaoSupplyPlanDTO, enterpriseFieldValue);

            RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(
                            perfilExecucaoSupplyPlanDTO),
                    "Campo Enterprise aceito silenciosamente no Community: " + field.getName());

            Assertions.assertTrue(
                    requiresEnterpriseVersionException.getMessage().startsWith(
                            RequiresEnterpriseVersionException.ERROR_CODE));
        }

    }

    @Test
    public void savePerfilExecucaoSupplyPlanDTOShouldPersistHeuristicCapacityLeveling() throws Exception {

        /*
         * O nivelamento pertence ao heuristico Community. O teste percorre a
         * borda completa de save com repositorios e mapper controlados para
         * provar que o flag nao e mais tratado como capability Enterprise.
         */
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan =
                criaPerfilExecucaoSupplyPlanParaTeste("supply-profile");
        perfilExecucaoSupplyPlan.setHeuristicUnconstrainedPlanCapacityLeveling(true);
        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                criaPerfilExecucaoSupplyPlanFrontServiceParaSaveSnapshotTest(
                        perfilExecucaoSupplyPlan,
                        perfilExecucaoSupplyPlan);
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO =
                criaPerfilExecucaoSupplyPlanDTOCommunityMinimoParaTeste();
        perfilExecucaoSupplyPlanDTO.setHeuristicUnconstrainedPlanCapacityLeveling(true);

        perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(perfilExecucaoSupplyPlanDTO);

        Assertions.assertTrue(perfilExecucaoSupplyPlan.getHeuristicUnconstrainedPlanCapacityLeveling());

    }

    @Test
    public void validaModoExecucaoCommunityShouldAcceptHeuristicExecution() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setExecutionModel(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

        invokeValidation(
                perfilExecucaoSupplyPlanFrontService,
                "validaModoExecucaoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaModoExecucaoCommunityShouldRejectOptimizerExecution() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setExecutionModel(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaModoExecucaoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaModoExecucaoCommunityShouldRejectProcessChainExecution() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setExecutionModel(PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaModoExecucaoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldAcceptDemandPlanOnlySource() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setCustomerOrdersAndForecastReconciliationModelForProjectedInventory(
                PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta.SOMENTE_FORECAST);
        perfilExecucaoSupplyPlanDTO.setCustomerOrdersAndForecastReconciliationModelForSafetyStock(
                PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta.SOMENTE_FORECAST);

        invokeValidation(
                perfilExecucaoSupplyPlanFrontService,
                "validaPedidosTransacionaisCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldRejectClientOrdersSource() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setCustomerOrdersAndForecastReconciliationModelForProjectedInventory(
                PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta.PLANO_DEMANDA_MAIS_CARTEIRA);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaPedidosTransacionaisCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldRejectDemandCatchUp() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setEnableDemandCatchUpFromPastSellout(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaPedidosTransacionaisCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldRejectSellInOrders() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setConsiderSellinOrdersBacklog(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaPedidosTransacionaisCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldRejectAllTransactionalOrderFlagsAndMakeToOrderOverrides() throws Exception {

        /*
         * O Community usa apenas Demand Plan como demanda futura. Cada flag
         * transacional ou override MTO abaixo deve falhar isoladamente antes
         * de qualquer persistencia de perfil.
         */
        for (Consumer<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOConsumer : List.<Consumer<PerfilExecucaoSupplyPlanDTO>>of(
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderSelloutOrdersBacklog(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderSelloutOrdersFuture(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderSellinOrdersFuture(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderTransferOrdersBacklog(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderTransferOrdersFuture(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderPurchaseOrdersBacklog(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderPurchaseOrdersFuture(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderProductionOrdersBacklog(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderProductionOrdersFuture(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setAllowBacklogCarryOver(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setForceMakeToOrderModel(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderForecastForMto(false),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setConsiderUnmetClientOrderImpact(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setCustomerOrderHorizonInDays(30),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setCustomerOrderMetDemandImpactCoefficient(1.0d),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setCustomerOrdersAndForecastReconciliationModelForSafetyStock(
                        PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta.PLANO_DEMANDA_MAIS_CARTEIRA))) {
            assertValidationRejectsEnterpriseParameter(
                    "validaPedidosTransacionaisCommunity",
                    perfilExecucaoSupplyPlanDTOConsumer);
        }

    }

    @Test
    public void validaFrotasEOtimizadorInteligenciaArtificialCommunityShouldRejectFleetAllocation() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setAllocateTransfersInFleets(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaFrotasEOtimizadorInteligenciaArtificialCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaFrotasEOtimizadorInteligenciaArtificialCommunityShouldRejectAiOptimizer() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setAiOptimizer(PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial.SNP);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaFrotasEOtimizadorInteligenciaArtificialCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectLineSequencing() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setEnableLineSequencing(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaParametrosModeloOtimizadoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectGreenfieldBrownfield() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setEnableGreenfieldBrownfield(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaParametrosModeloOtimizadoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCurvasSplitTemporalCommunityShouldRejectConfiguredCurves() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setTemporalSplitCurveIdSet(Set.of("temporal-split-curve"));

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCurvasSplitTemporalCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaGreenfieldCommunityShouldRejectLocationActivationBudget() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setConsiderBudgetForGreenfieldLocationActivation(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaGreenfieldCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaGreenfieldCommunityShouldRejectLocationActivationBudgetValue() throws Exception {

        assertValidationRejectsEnterpriseParameter(
                "validaGreenfieldCommunity",
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setGreenfieldLocationActivationBudget(1000.0d));

    }

    @Test
    public void validaFiltroMateriaisCommunityShouldRejectMaterialFilter() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setMaterialFilterId("material-filter");

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaFiltroMateriaisCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void perfilExecucaoSupplyPlanDTOShouldRejectHistoricalPayloadFieldNames() {

        for (String historicalFieldName : List.of(
                "productFilterId",
                "increaseMetDemandImpactInEarlierPeriods",
                "maximumPercentageIncreaseMetDemandImpactAtFirstPeriod",
                "metDemandTemporalImpactDecayModel",
                "metDemandTemporalImpactExponentialDecayFactor",
                "metDemandTemporalImpactMinimumMultiplier",
                "considerSelloutOrdersNonBacklog",
                "considerSellinOrdersNonBacklog",
                "considerTransferOrdersNonBacklog",
                "considerPurchaseOrdersNonBacklog",
                "considerProductionOrdersNonBacklog")) {
            Assertions.assertThrows(
                    JsonProcessingException.class,
                    () -> new ObjectMapper().readValue(
                            "{\"" + historicalFieldName + "\":true}",
                            PerfilExecucaoSupplyPlanDTO.class));
        }

    }

    @Test
    public void perfilExecucaoSupplyPlanDTOShouldRoundTripCanonicalPayloadFieldNames() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = objectMapper.readValue(
                """
                {
                  "materialFilterId": "material-filter",
                  "increaseObjectiveFunctionImpactInEarlierPeriods": true,
                  "considerSelloutOrdersFuture": true
                }
                """,
                PerfilExecucaoSupplyPlanDTO.class);

        Assertions.assertEquals("material-filter", perfilExecucaoSupplyPlanDTO.getMaterialFilterId());
        Assertions.assertTrue(perfilExecucaoSupplyPlanDTO.getIncreaseObjectiveFunctionImpactInEarlierPeriods());
        Assertions.assertTrue(perfilExecucaoSupplyPlanDTO.getConsiderSelloutOrdersFuture());

        String serializedJson = objectMapper.writeValueAsString(perfilExecucaoSupplyPlanDTO);
        Assertions.assertTrue(serializedJson.contains("\"materialFilterId\""));
        Assertions.assertTrue(serializedJson.contains("\"increaseObjectiveFunctionImpactInEarlierPeriods\""));
        Assertions.assertTrue(serializedJson.contains("\"considerSelloutOrdersFuture\""));

    }

    @Test
    public void validaCapacidadesEConstraintsCommunityShouldAcceptTotalHoursPerDay() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setProductiveCapacityType(PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.HORAS_POR_DIA);

        invokeValidation(
                perfilExecucaoSupplyPlanFrontService,
                "validaCapacidadesEConstraintsCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCapacidadesEConstraintsCommunityShouldRejectShiftAllocation() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setProductiveCapacityType(PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.ALOCACAO_TURNOS);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCapacidadesEConstraintsCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCapacidadesEConstraintsCommunityShouldRejectLogisticsCapacityLevel() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setLogisticsCapacityLevel(PerfilExecucaoSupplyPlan.TipoCapacidadeLogistica.NIVEL_LOCATION_DATA);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCapacidadesEConstraintsCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCapacidadesEConstraintsCommunityShouldRejectStockAtClients() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setAllowStockAtClients(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCapacidadesEConstraintsCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCapacidadesEConstraintsCommunityShouldRejectStorageConstraints() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setConsiderStorageConstraints(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCapacidadesEConstraintsCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCapacidadesEConstraintsCommunityShouldRejectInboundConstraints() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setConsiderInboundConstraints(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCapacidadesEConstraintsCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCapacidadesEConstraintsCommunityShouldRejectOutboundConstraints() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setConsiderOutboundConstraints(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCapacidadesEConstraintsCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCapacidadesEConstraintsCommunityShouldRejectStockAtTransshipmentPoints() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setAllowStockAtTransshipmentPoints(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCapacidadesEConstraintsCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCapacidadesEConstraintsCommunityShouldRejectProductionScheduling() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setGenerateProductionScheduling(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCapacidadesEConstraintsCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCapacidadesEConstraintsCommunityShouldRejectUnconstrainedLogisticsConstraintOverrides() throws Exception {

        for (Consumer<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOConsumer : List.<Consumer<PerfilExecucaoSupplyPlanDTO>>of(
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setIgnoreStorageConstraintsForUnconstrainedPlan(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setIgnoreInboundConstraintsForUnconstrainedPlan(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setIgnoreOutboundConstraintsForUnconstrainedPlan(true))) {
            assertValidationRejectsEnterpriseParameter(
                    "validaCapacidadesEConstraintsCommunity",
                    perfilExecucaoSupplyPlanDTOConsumer);
        }

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectProfitAndLossGeneration() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setGeneratePL(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaParametrosModeloOtimizadoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectTaxApportionment() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setTaxApportionmentModel(
                PerfilExecucaoSupplyPlan.ModoApuracaoImpostos.APURACAO_ICMS);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaParametrosModeloOtimizadoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectCostModel() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setConsiderSupplierPrices(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaParametrosModeloOtimizadoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectOptimizerDiagnosticsAndSoftTargetParameters() throws Exception {

        for (Consumer<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOConsumer : List.<Consumer<PerfilExecucaoSupplyPlanDTO>>of(
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setOptimizationModelType("MIP"),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setSaveOptimizerVariablesAndConstraints(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setSaveConstraintBacktracking(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setCustomerDemandPrioritizationModelId("customer-priority"),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setSafetyStockPrioritizationModelId("safety-stock-priority"),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setObjectiveFunctionTemporalImpactDecayModel(
                        PerfilExecucaoSupplyPlan.ModeloDecaimentoImpactoTemporal.EXPONENCIAL),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setMaximumOptimizerExecutionTime(60L),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setSoftTargetMaximumPercentPenalty(0.1d),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setSoftTargetDeviationAmplitudeAsTargetPercent(0.2d),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setSoftTargetDeviationLinearizationNumberSegments(5))) {
            assertValidationRejectsEnterpriseParameter(
                    "validaParametrosModeloOtimizadoCommunity",
                    perfilExecucaoSupplyPlanDTOConsumer);
        }

    }

    @Test
    public void validaCurvasCustoLogisticoCommunityShouldRejectConfiguredCurves() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setLogisticsCostCurvesId(10L);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCurvasCustoLogisticoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaCurvasCustoLogisticoCommunityShouldRejectLocationCostCurveApplication() throws Exception {

        assertValidationRejectsEnterpriseParameter(
                "validaCurvasCustoLogisticoCommunity",
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setApplyLocationCostCurves(true));

    }

    @Test
    public void validaCurvasCustoLogisticoCommunityShouldRejectFreightCostCurveApplication() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setApplyFreightCostCurves(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaCurvasCustoLogisticoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectLineScheduling() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setGenerateDetailedPlan(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaParametrosModeloOtimizadoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectObjectiveFunctionParameters() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setDemandPlanMetDemandImpactCoefficient(1.0d);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaParametrosModeloOtimizadoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectNonFiniteObjectiveFunctionParameters() throws Exception {

        /*
         * Parametros numericos Enterprise aceitam apenas ausencia ou zero
         * tecnico neutro no payload Community. `NaN` nao representa campo vazio:
         * se for aceito, a borda publica pode deixar um payload quebrado chegar
         * ao mapper/repository sem sinalizar a feature Enterprise correspondente.
         */
        assertValidationRejectsEnterpriseParameter(
                "validaParametrosModeloOtimizadoCommunity",
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setDemandPlanMetDemandImpactCoefficient(Double.NaN));

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectLeadTimeOptimization() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setIgnoreLeadTimeConstraintsForUnconstrainedPlan(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaParametrosModeloOtimizadoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectSafetyStockFairShare() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setSafetyStockFairShare(true);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaParametrosModeloOtimizadoCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    @Test
    public void validaParametrosModeloOtimizadoCommunityShouldRejectUnmetDemandPenaltyParameters() throws Exception {

        for (Consumer<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOConsumer : List.<Consumer<PerfilExecucaoSupplyPlanDTO>>of(
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setPenalizeUnmetDemand(true),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setUnmetDemandPenalizationAsFractionOfGrossSales(0.05d),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setUnmetDemandPenalizationAsUnitImpact(10.0d),
                perfilExecucaoSupplyPlanDTO -> perfilExecucaoSupplyPlanDTO.setUnmetDemandPenalizationAsUnitImpactUomId("EA"))) {
            assertValidationRejectsEnterpriseParameter(
                    "validaParametrosModeloOtimizadoCommunity",
                    perfilExecucaoSupplyPlanDTOConsumer);
        }

    }

    @Test
    public void normalizaFairShareCommunityShouldForceDirectDemandFairShare() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setAplicaFairShareDemandaDireta(false);

        invokeNormalization(
                perfilExecucaoSupplyPlanFrontService,
                "normalizaFairShareCommunity",
                perfilExecucaoSupplyPlan);

        Assertions.assertTrue(perfilExecucaoSupplyPlan.getAplicaFairShareDemandaDireta());

    }

    @Test
    public void validaPerfilLocationLevelCommunityShouldRejectPartialLocationExecution() throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setExecuteSupplyPlanForAllLocations(false);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                "validaPerfilLocationLevelCommunity",
                perfilExecucaoSupplyPlanDTO);

    }

    private static void assertValidationRejectsEnterpriseParameter(
            String methodName,
            Consumer<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOConsumer) throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService = new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTOConsumer.accept(perfilExecucaoSupplyPlanDTO);

        assertRequiresEnterpriseVersionException(
                perfilExecucaoSupplyPlanFrontService,
                methodName,
                perfilExecucaoSupplyPlanDTO);

    }

    private static void assertRequiresEnterpriseVersionException(
            PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService,
            String methodName,
            PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) throws Exception {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidation(
                        perfilExecucaoSupplyPlanFrontService,
                        methodName,
                        perfilExecucaoSupplyPlanDTO));
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    private static void invokeValidation(
            PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService,
            String methodName,
            PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) throws Exception {

        Method validationMethod = PerfilExecucaoSupplyPlanFacade.class.getDeclaredMethod(
                methodName,
                PerfilExecucaoSupplyPlanDTO.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                perfilExecucaoSupplyPlanFrontService,
                perfilExecucaoSupplyPlanDTO);

    }

    private static void invokeNormalization(
            PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService,
            String methodName,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) throws Exception {

        Method normalizationMethod = PerfilExecucaoSupplyPlanFacade.class.getDeclaredMethod(
                methodName,
                PerfilExecucaoSupplyPlan.class);
        normalizationMethod.setAccessible(true);
        normalizationMethod.invoke(
                perfilExecucaoSupplyPlanFrontService,
                perfilExecucaoSupplyPlan);

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static PoliticaEstoquesRepository createPoliticaEstoquesRepositoryProxy(
            AtomicInteger findAllCallCount) {

        return createPoliticaEstoquesRepositoryProxy(
                findAllCallCount,
                List.<PoliticaEstoques>of());

    }

    private static PoliticaEstoquesRepository createPoliticaEstoquesRepositoryProxy(
            AtomicInteger findAllCallCount,
            List<PoliticaEstoques> politicaEstoquesList) {

        return (PoliticaEstoquesRepository) Proxy.newProxyInstance(
                PoliticaEstoquesRepository.class.getClassLoader(),
                new Class<?>[]{PoliticaEstoquesRepository.class},
                (proxy, method, args) -> {

                    if (method.getDeclaringClass().equals(Object.class)) {
                        return switch (method.getName()) {
                            case "toString" -> "PoliticaEstoquesRepositoryProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new AssertionError("Metodo Object inesperado: " + method.getName());
                        };
                    }
                    if ("findAll".equals(method.getName()) && method.getParameterCount() == 0) {
                        findAllCallCount.incrementAndGet();
                        return politicaEstoquesList;
                    }

                    throw new AssertionError("Metodo inesperado no teste: " + method.getName());

                });

    }

    private static PerfilExecucaoSupplyPlanFacade criaPerfilExecucaoSupplyPlanFrontServiceParaSaveSnapshotTest(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanMapeado,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanSalvo) throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                new PerfilExecucaoSupplyPlanFacade();
        AtomicInteger findAllCallCount = new AtomicInteger(0);

        setPrivateField(
                perfilExecucaoSupplyPlanFrontService,
                "politicaEstoquesRepository",
                createPoliticaEstoquesRepositoryProxy(findAllCallCount));
        setPrivateField(
                perfilExecucaoSupplyPlanFrontService,
                "perfilExecucaoSupplyPlanAutoMapper",
                createPerfilExecucaoSupplyPlanAutoMapperProxy(perfilExecucaoSupplyPlanMapeado));
        setPrivateField(
                perfilExecucaoSupplyPlanFrontService,
                "perfilExecucaoSupplyPlanRepository",
                createPerfilExecucaoSupplyPlanRepositoryProxy(perfilExecucaoSupplyPlanSalvo));

        return perfilExecucaoSupplyPlanFrontService;

    }

    private static PerfilExecucaoSupplyPlanFacade criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
            List<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanList) throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                new PerfilExecucaoSupplyPlanFacade();
        setPrivateField(
                perfilExecucaoSupplyPlanFrontService,
                "perfilExecucaoSupplyPlanRepository",
                createPerfilExecucaoSupplyPlanRepositoryListProxy(perfilExecucaoSupplyPlanList));
        return perfilExecucaoSupplyPlanFrontService;

    }

    private static PerfilExecucaoSupplyPlanFacade criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(
            List<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanList,
            List<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOList) throws Exception {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                criaPerfilExecucaoSupplyPlanFrontServiceParaListagem(perfilExecucaoSupplyPlanList);
        setPrivateField(
                perfilExecucaoSupplyPlanFrontService,
                "perfilExecucaoSupplyPlanAutoMapper",
                createPerfilExecucaoSupplyPlanAutoMapperListProxy(perfilExecucaoSupplyPlanDTOList));
        return perfilExecucaoSupplyPlanFrontService;

    }

    private static PerfilExecucaoSupplyPlanRepository createPerfilExecucaoSupplyPlanRepositoryListProxy(
            List<PerfilExecucaoSupplyPlan> perfilExecucaoSupplyPlanList) {

        return (PerfilExecucaoSupplyPlanRepository) Proxy.newProxyInstance(
                PerfilExecucaoSupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{PerfilExecucaoSupplyPlanRepository.class},
                (proxy, method, args) -> {

                    if (method.getDeclaringClass().equals(Object.class)) {
                        return switch (method.getName()) {
                            case "toString" -> "PerfilExecucaoSupplyPlanRepositoryListProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new AssertionError("Metodo Object inesperado: " + method.getName());
                        };
                    }
                    if ("customFindAll".equals(method.getName()) && method.getParameterCount() == 0) {
                        return perfilExecucaoSupplyPlanList;
                    }

                    throw new AssertionError("Metodo inesperado no teste: " + method.getName());

                });

    }

    private static PerfilExecucaoSupplyPlanRepository createPerfilExecucaoSupplyPlanRepositoryProxy(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanSalvo) {

        return (PerfilExecucaoSupplyPlanRepository) Proxy.newProxyInstance(
                PerfilExecucaoSupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{PerfilExecucaoSupplyPlanRepository.class},
                (proxy, method, args) -> {

                    if (method.getDeclaringClass().equals(Object.class)) {
                        return switch (method.getName()) {
                            case "toString" -> "PerfilExecucaoSupplyPlanRepositoryProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new AssertionError("Metodo Object inesperado: " + method.getName());
                        };
                    }
                    if ("save".equals(method.getName()) && method.getParameterCount() == 1) {
                        return perfilExecucaoSupplyPlanSalvo;
                    }

                    throw new AssertionError("Metodo inesperado no teste: " + method.getName());

                });

    }

    private static PerfilExecucaoSupplyPlanAutoMapper createPerfilExecucaoSupplyPlanAutoMapperProxy(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanMapeado) {

        return (PerfilExecucaoSupplyPlanAutoMapper) Proxy.newProxyInstance(
                PerfilExecucaoSupplyPlanAutoMapper.class.getClassLoader(),
                new Class<?>[]{PerfilExecucaoSupplyPlanAutoMapper.class},
                (proxy, method, args) -> {

                    if (method.getDeclaringClass().equals(Object.class)) {
                        return switch (method.getName()) {
                            case "toString" -> "PerfilExecucaoSupplyPlanAutoMapperProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new AssertionError("Metodo Object inesperado: " + method.getName());
                        };
                    }
                    if ("converte".equals(method.getName())
                            && method.getParameterCount() == 1
                            && args[0] instanceof PerfilExecucaoSupplyPlanDTO) {
                        return perfilExecucaoSupplyPlanMapeado;
                    }

                    throw new AssertionError("Metodo inesperado no teste: " + method.getName());

                });

    }

    private static PerfilExecucaoSupplyPlanAutoMapper createPerfilExecucaoSupplyPlanAutoMapperListProxy(
            List<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOList) {

        return (PerfilExecucaoSupplyPlanAutoMapper) Proxy.newProxyInstance(
                PerfilExecucaoSupplyPlanAutoMapper.class.getClassLoader(),
                new Class<?>[]{PerfilExecucaoSupplyPlanAutoMapper.class},
                (proxy, method, args) -> {

                    if (method.getDeclaringClass().equals(Object.class)) {
                        return switch (method.getName()) {
                            case "toString" -> "PerfilExecucaoSupplyPlanAutoMapperListProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new AssertionError("Metodo Object inesperado: " + method.getName());
                        };
                    }
                    if ("converteListaEntidadesParaDTOs".equals(method.getName())
                            && method.getParameterCount() == 1
                            && args[0] instanceof List) {
                        return perfilExecucaoSupplyPlanDTOList;
                    }

                    throw new AssertionError("Metodo inesperado no teste: " + method.getName());

                });

    }

    private static PerfilExecucaoSupplyPlanDTO criaPerfilExecucaoSupplyPlanDTOCommunityMinimoParaTeste() {

        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setId("supply-profile");
        perfilExecucaoSupplyPlanDTO.setInventoryPolicyIdSet(Set.of());
        return perfilExecucaoSupplyPlanDTO;

    }

    private static void assertSaveRejectsInvalidOperationalInteger(
            Consumer<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOConsumer,
            String expectedMessage) {

        PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService =
                new PerfilExecucaoSupplyPlanFacade();
        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO =
                criaPerfilExecucaoSupplyPlanDTOCommunityMinimoParaTeste();
        perfilExecucaoSupplyPlanDTOConsumer.accept(perfilExecucaoSupplyPlanDTO);

        /*
         * O erro e de configuracao operacional Community, nao de capability
         * Enterprise. Por isso deve falhar antes de mapper, repository de
         * politica de estoque ou qualquer normalizacao do DTO.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(
                        perfilExecucaoSupplyPlanDTO));

        Assertions.assertEquals(
                expectedMessage,
                illegalArgumentException.getMessage());

    }

    private static PerfilExecucaoSupplyPlanDTO criaPerfilExecucaoSupplyPlanDTOListagemParaTeste(String id) {

        PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO = new PerfilExecucaoSupplyPlanDTO();
        perfilExecucaoSupplyPlanDTO.setId(id);
        perfilExecucaoSupplyPlanDTO.setExecutionModel(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);
        perfilExecucaoSupplyPlanDTO.setInventoryPolicyIdSet(Set.of());
        return perfilExecucaoSupplyPlanDTO;

    }

    private static PerfilExecucaoSupplyPlan criaPerfilExecucaoSupplyPlanParaTeste(String id) {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setId(id);
        return perfilExecucaoSupplyPlan;

    }

    private static Object getEnterpriseFieldValue(Field field) {

        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        if ("considerForecastForMto".equals(fieldName)
                || "executeSupplyPlanForAllLocations".equals(fieldName)) {
            return false;
        }
        if (Boolean.class.equals(fieldType)) {
            return true;
        }
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
        if (Set.class.equals(fieldType)) {
            return Set.of("enterprise-value");
        }

        return switch (fieldName) {
            case "executionModel" -> PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR;
            case "aiOptimizer" -> PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial.SNP;
            case "customerOrdersAndForecastReconciliationModelForProjectedInventory",
                    "customerOrdersAndForecastReconciliationModelForSafetyStock" ->
                    PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta.PLANO_DEMANDA_MAIS_CARTEIRA;
            case "objectiveFunctionTemporalImpactDecayModel" ->
                    PerfilExecucaoSupplyPlan.ModeloDecaimentoImpactoTemporal.EXPONENCIAL;
            case "salesMeasure" -> Constantes.TipoQuantidadeValor.GROSS;
            case "taxApportionmentModel" -> PerfilExecucaoSupplyPlan.ModoApuracaoImpostos.APURACAO_ICMS;
            case "productiveCapacityType" -> PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.QUANTIDADE_POR_UOM;
            case "logisticsCapacityLevel" -> PerfilExecucaoSupplyPlan.TipoCapacidadeLogistica.NIVEL_LOCATION_DATA;
            case "detailedPlanBucketSize" -> Constantes.TamanhoBucket.HORARIO;
            default -> null;
        };

    }

}
