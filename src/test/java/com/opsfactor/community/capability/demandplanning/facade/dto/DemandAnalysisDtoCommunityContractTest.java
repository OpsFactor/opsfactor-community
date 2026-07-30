package com.opsfactor.community.capability.demandplanning.facade.dto;

import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanItemDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrato de shape dos DTOs de Demand Analysis expostos no Community.
 *
 * <p>A simulacao Community trabalha somente no nivel material/location. Campos
 * de agregado MAPE, arvore, caracteristicas, campanhas, support series,
 * forecast semi-agregado ou diagnosticos de auto-fit pertencem ao Enterprise e
 * nao devem reaparecer no payload sem uma decisao explicita.</p>
 */
public class DemandAnalysisDtoCommunityContractTest {

    @Test
    public void simulatedDemandPlanDtoShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "periodos",
                        "agrupadoresPeriodoDesagregado",
                        "agrupadoresPeriodoAgregado",
                        "periodoInicioForecast",
                        "posicaoPeriodoInicioForecast",
                        "periodoUltimaVenda",
                        "posicaoPeriodoUltimaVenda",
                        "materialClusterDTO",
                        "clusterLocationsDTO",
                        "materialLocationData"),
                getDeclaredFieldNames(SimulatedDemandPlanDTO.class));

    }

    @Test
    public void simulatedDemandPlanMaterialLocationDtoShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "locationId",
                        "materialId",
                        "historicalSales",
                        "residual",
                        "absoluteResidual",
                        "baselineForecast",
                        "historicalSalesAfterStockoutTreatment",
                        "historicalSalesAfterOutlierTreatment",
                        "trend",
                        "seasonal",
                        "lowerBound",
                        "upperBound"),
                getDeclaredFieldNames(SimulatedDemandPlanMaterialLocationDTO.class));

    }

    @Test
    public void demandPlanItemDtoShouldExposeMaterialLocationContract() {

        Assertions.assertEquals(
                Set.of(
                        "locationId",
                        "materialId",
                        "referenceDate",
                        "uomId",
                        "baselineQtyUnconstrained",
                        "demandAdjustmentQtyUnconstrained",
                        "totalQtyUnconstrained",
                        "baselineQtyConstrained",
                        "demandAdjustmentQtyConstrained",
                        "totalQtyConstrained"),
                getDeclaredFieldNames(DemandPlanItemDTO.class));

    }

    private static Set<String> getDeclaredFieldNames(Class<?> dtoClass) {

        return Arrays.stream(dtoClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

    }

}
