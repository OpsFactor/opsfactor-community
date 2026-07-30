package com.opsfactor.community.capability.supplyplanning.distributionplan.facade;

import com.opsfactor.community.capability.supplyplanning.distributionplan.facade.dto.DeploymentOperationalInboundUpdateDTO;
import com.opsfactor.community.capability.supplyplanning.distributionplan.facade.dto.DeploymentOperationalLineDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Contract tests for the deliberately small Community deployment vertical.
 */
class DeploymentOperationalFacadeCommunityContractTest {

    @Test
    void updateCommandShouldRejectNegativePlannedInboundBeforeProjectionOrPersistence() throws Exception {

        DeploymentOperationalFacade deploymentOperationalFrontService =
                new DeploymentOperationalFacade();
        DeploymentOperationalInboundUpdateDTO deploymentOperationalInboundUpdateDTO =
                new DeploymentOperationalInboundUpdateDTO(10L, "ORIGIN", "DESTINATION", "MATERIAL", -0.01d);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> invokeValidateUpdateCommand(
                        deploymentOperationalFrontService,
                        deploymentOperationalInboundUpdateDTO));

        Assertions.assertEquals(
                "Community deployment planned inbound quantity must be greater than or equal to zero.",
                illegalArgumentException.getMessage());

    }

    @Test
    void updateShouldUseTypedTransactionalWorkingPlanInboundCommand() throws Exception {

        Method updateMethod = DeploymentOperationalFacade.class.getDeclaredMethod(
                "updatePlannedInbound",
                DeploymentOperationalInboundUpdateDTO.class);

        Assertions.assertNotNull(updateMethod.getAnnotation(Transactional.class));
        Assertions.assertEquals(DeploymentOperationalLineDTO.class, updateMethod.getReturnType());
        Assertions.assertEquals(DeploymentOperationalInboundUpdateDTO.class, updateMethod.getParameterTypes()[0]);

    }

    @Test
    void publicDtosShouldExposeOnlyOperationalTransferFields() {

        Assertions.assertEquals(
                List.of(
                        "supplyPlanId",
                        "originLocationId",
                        "destinationLocationId",
                        "materialId",
                        "materialDescription",
                        "currentPeriodEndDate",
                        "leadTimeDays",
                        "expectedReceiptDate",
                        "unitOfMeasureId",
                        "plannedInboundQuantity"),
                List.of(DeploymentOperationalLineDTO.class.getRecordComponents())
                        .stream()
                        .map(component -> component.getName())
                        .toList());
        Assertions.assertEquals(
                List.of(
                        "supplyPlanId",
                        "originLocationId",
                        "destinationLocationId",
                        "materialId",
                        "plannedInboundQuantity"),
                List.of(DeploymentOperationalInboundUpdateDTO.class.getRecordComponents())
                        .stream()
                        .map(component -> component.getName())
                        .toList());

    }

    @Test
    void updateShouldDeriveOldValueAndDelegateOnlyPlannedWorkingInboundToExistingModificationService() throws Exception {

        String source = Files.readString(
                Path.of("src/main/java/com/opsfactor/community/capability/supplyplanning/distributionplan/facade/DeploymentOperationalFacade.java"),
                StandardCharsets.UTF_8);

        Assertions.assertTrue(source.contains("double currentPlannedInboundQuantity = getPlannedInboundQuantity("));
        Assertions.assertTrue(source.contains("KeyFigureStandardEnum.INBOUND_PLANEJADO"));
        Assertions.assertTrue(source.contains("Constantes.TipoPlano.PLANO_TRABALHO"));
        Assertions.assertTrue(source.contains("Constantes.FirmePlanejado.PLANEJADO"));
        Assertions.assertTrue(source.contains("Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE"));
        Assertions.assertFalse(source.contains("ESTOQUE_DIAS"));
        Assertions.assertFalse(source.contains("FirmePlanejado.ORDEM"));

    }

    private void invokeValidateUpdateCommand(
            DeploymentOperationalFacade deploymentOperationalFrontService,
            DeploymentOperationalInboundUpdateDTO deploymentOperationalInboundUpdateDTO) throws Exception {

        Method validateMethod = DeploymentOperationalFacade.class.getDeclaredMethod(
                "validateUpdateCommand",
                DeploymentOperationalInboundUpdateDTO.class);
        validateMethod.setAccessible(true);
        try {
            validateMethod.invoke(deploymentOperationalFrontService, deploymentOperationalInboundUpdateDTO);
        } catch (InvocationTargetException invocationTargetException) {
            if (invocationTargetException.getCause() instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw invocationTargetException;
        }

    }
}
