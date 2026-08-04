package com.opsfactor.community.platform.bi.service;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Contrato de UOM obrigatória antes de montar qualquer projection agregada. */
class CommunityProductionOverviewProjectionLoaderTest {

    @Test
    void shouldFailExplicitlyWhenTheQuantityUnitOfMeasureIsMissing() {

        UnidadeMedidaProjection unitOfMeasureProjection = Mockito.mock(UnidadeMedidaProjection.class);
        Mockito.when(unitOfMeasureProjection.getUnidadeMedidaFromId(null)).thenReturn(null);

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> CommunityProductionOverviewProjectionLoader.getRequiredQuantityUnitOfMeasure(
                        unitOfMeasureProjection, null));

        Assertions.assertEquals("uomId is required for Production Overview", exception.getMessage());

    }

    @Test
    void shouldReturnTheSelectedQuantityUnitOfMeasureWithoutFallback() {

        UnidadeMedidaProjection unitOfMeasureProjection = Mockito.mock(UnidadeMedidaProjection.class);
        UnidadeMedida expectedUnitOfMeasure = Mockito.mock(UnidadeMedida.class);
        Mockito.when(unitOfMeasureProjection.getUnidadeMedidaFromId("EA"))
                .thenReturn(expectedUnitOfMeasure);

        Assertions.assertSame(expectedUnitOfMeasure,
                CommunityProductionOverviewProjectionLoader.getRequiredQuantityUnitOfMeasure(
                        unitOfMeasureProjection, "EA"));

    }
}
