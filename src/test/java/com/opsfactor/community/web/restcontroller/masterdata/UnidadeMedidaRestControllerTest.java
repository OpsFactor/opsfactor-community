package com.opsfactor.community.web.restcontroller.masterdata;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.UnidadeMedidaFacade;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto.UnidadeConversaoFaltanteDTO;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Contrato HTTP do diagnostico Community de lacunas de UOM para Supply
 * Planning.
 */
public class UnidadeMedidaRestControllerTest {

    @Test
    public void supplyPlanningUomGapEndpointShouldAdaptLegacyPeriodAndDelegateToCommunityService()
            throws Exception {

        UnidadeMedidaRestController unidadeMedidaRestController = new UnidadeMedidaRestController();
        CapturingUnidadeMedidaFrontService unidadeMedidaFrontService =
                new CapturingUnidadeMedidaFrontService();
        injectUnidadeMedidaFrontService(unidadeMedidaRestController, unidadeMedidaFrontService);

        ResponseEntity<Set<UnidadeConversaoFaltanteDTO>> responseEntity =
                unidadeMedidaRestController.getGapsDeConversaoEntreUnidadesParaSNP(
                        "202607",
                        "Monthly",
                        "NETWORK",
                        "PROFILE",
                        42L);

        Assertions.assertEquals(200, responseEntity.getStatusCode().value());
        Assertions.assertSame(unidadeMedidaFrontService.result, responseEntity.getBody());
        Assertions.assertEquals(
                LocalDateTime.of(2026, 7, 1, 0, 0),
                unidadeMedidaFrontService.referenceDate);
        Assertions.assertEquals(TamanhoBucket.MENSAL, unidadeMedidaFrontService.bucketSize);
        Assertions.assertEquals("NETWORK", unidadeMedidaFrontService.supplyNetworkVersionId);
        Assertions.assertEquals("PROFILE", unidadeMedidaFrontService.supplyPlanningExecutionProfileId);
        Assertions.assertEquals(42L, unidadeMedidaFrontService.demandPlanVersionId);

    }

    @Test
    public void demandPlanningUomGapEndpointShouldDelegateTheLegacyTextArgumentsWithoutTransformation()
            throws Exception {

        UnidadeMedidaRestController unidadeMedidaRestController = new UnidadeMedidaRestController();
        CapturingUnidadeMedidaFrontService unidadeMedidaFrontService =
                new CapturingUnidadeMedidaFrontService();
        injectUnidadeMedidaFrontService(unidadeMedidaRestController, unidadeMedidaFrontService);

        ResponseEntity<Set<UnidadeConversaoFaltanteDTO>> responseEntity =
                unidadeMedidaRestController.getGapsDeConversaoEntreUnidadesParaDP(
                        "DP_PROFILE",
                        "2026-07-19T00:00:00");

        Assertions.assertEquals(200, responseEntity.getStatusCode().value());
        Assertions.assertSame(unidadeMedidaFrontService.result, responseEntity.getBody());
        Assertions.assertEquals("DP_PROFILE", unidadeMedidaFrontService.demandPlanningExecutionProfileId);
        Assertions.assertEquals("2026-07-19T00:00:00", unidadeMedidaFrontService.demandPlanningReferenceDate);

    }

    @Test
    public void demandPlanningUomGapEndpointShouldPreserveLegacyAuthenticationOnlyAuthorization()
            throws Exception {

        Method endpointMethod = UnidadeMedidaRestController.class.getDeclaredMethod(
                "getGapsDeConversaoEntreUnidadesParaDP",
                String.class,
                String.class);

        Assertions.assertNull(
                endpointMethod.getAnnotation(Secured.class),
                "O endpoint legado deve continuar dependendo apenas da cadeia autenticada de api/secured.");

    }

    @Test
    public void deploymentUomGapEndpointShouldDelegateSupplyPlanIdWithoutTransformation()
            throws Exception {

        UnidadeMedidaRestController unidadeMedidaRestController = new UnidadeMedidaRestController();
        CapturingUnidadeMedidaFrontService unidadeMedidaFrontService =
                new CapturingUnidadeMedidaFrontService();
        injectUnidadeMedidaFrontService(unidadeMedidaRestController, unidadeMedidaFrontService);

        ResponseEntity<Set<UnidadeConversaoFaltanteDTO>> responseEntity =
                unidadeMedidaRestController.getGapsDeConversaoEntreUnidadesParaDeployment(73L);

        Assertions.assertEquals(200, responseEntity.getStatusCode().value());
        Assertions.assertSame(unidadeMedidaFrontService.result, responseEntity.getBody());
        Assertions.assertEquals(73L, unidadeMedidaFrontService.deploymentSupplyPlanId);

    }

    @Test
    public void deploymentUomGapEndpointShouldPreserveLegacyAuthenticationOnlyAuthorization()
            throws Exception {

        Method endpointMethod = UnidadeMedidaRestController.class.getDeclaredMethod(
                "getGapsDeConversaoEntreUnidadesParaDeployment",
                Long.class);

        Assertions.assertNull(
                endpointMethod.getAnnotation(Secured.class),
                "O endpoint legado deve continuar dependendo apenas da cadeia autenticada de api/secured.");

    }

    @Test
    public void supplyPlanningUomGapEndpointShouldPreserveLegacyAuthenticationOnlyAuthorization()
            throws Exception {

        Method endpointMethod = UnidadeMedidaRestController.class.getDeclaredMethod(
                "getGapsDeConversaoEntreUnidadesParaSNP",
                String.class,
                String.class,
                String.class,
                String.class,
                Long.class);

        Assertions.assertNull(
                endpointMethod.getAnnotation(Secured.class),
                "O endpoint legado deve continuar dependendo apenas da cadeia autenticada de api/secured.");

    }

    private static void injectUnidadeMedidaFrontService(
            UnidadeMedidaRestController unidadeMedidaRestController,
            UnidadeMedidaFacade unidadeMedidaFrontService) throws Exception {

        Field field = UnidadeMedidaRestController.class.getDeclaredField("unidadeConversaoFrontService");
        field.setAccessible(true);
        field.set(unidadeMedidaRestController, unidadeMedidaFrontService);

    }

    private static class CapturingUnidadeMedidaFrontService extends UnidadeMedidaFacade {

        private final Set<UnidadeConversaoFaltanteDTO> result = Set.of();
        private LocalDateTime referenceDate;
        private TamanhoBucket bucketSize;
        private String supplyNetworkVersionId;
        private String supplyPlanningExecutionProfileId;
        private Long demandPlanVersionId;
        private String demandPlanningExecutionProfileId;
        private String demandPlanningReferenceDate;
        private Long deploymentSupplyPlanId;

        @Override
        public Set<UnidadeConversaoFaltanteDTO> getUnidadeConversaoFaltanteSNPListDTO(
                LocalDateTime referenceDate,
                TamanhoBucket bucketSize,
                String supplyNetworkVersionId,
                String supplyPlanningExecutionProfileId,
                Long demandPlanVersionId) {

            this.referenceDate = referenceDate;
            this.bucketSize = bucketSize;
            this.supplyNetworkVersionId = supplyNetworkVersionId;
            this.supplyPlanningExecutionProfileId = supplyPlanningExecutionProfileId;
            this.demandPlanVersionId = demandPlanVersionId;
            return result;

        }

        @Override
        public Set<UnidadeConversaoFaltanteDTO> getUnidadeConversaoFaltanteDPListDTO(
                String demandPlanningExecutionProfileId,
                String demandPlanningReferenceDate) {

            this.demandPlanningExecutionProfileId = demandPlanningExecutionProfileId;
            this.demandPlanningReferenceDate = demandPlanningReferenceDate;
            return result;

        }

        @Override
        public Set<UnidadeConversaoFaltanteDTO> getUnidadeConversaoFaltanteDeploymentListDTO(
                Long supplyPlanId) {

            this.deploymentSupplyPlanId = supplyPlanId;
            return result;

        }

    }

}
