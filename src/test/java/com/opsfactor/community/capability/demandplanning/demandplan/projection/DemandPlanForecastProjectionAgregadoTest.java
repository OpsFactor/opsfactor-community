package com.opsfactor.community.capability.demandplanning.demandplan.projection;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contratos estruturais da projection agregada de forecast.
 *
 * <p>A lista de filhos e mutavel para permitir montagem incremental pela
 * factory. Estes testes protegem a classe dona da hierarquia contra snapshots
 * corrompidos que poderiam vazar como NPE em consumers de Demand Planning,
 * Chronos ou desagregacao.</p>
 */
class DemandPlanForecastProjectionAgregadoTest {

    @Test
    void forecastProjectionShouldRejectMissingCalendarWhenInitializingArrays() {

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanForecastProjectionMaterialLocation.inicializaArrays(null, false));

        Assertions.assertEquals(
                "Demand Plan forecast projection requires calendar.",
                illegalArgumentException.getMessage());

    }

    @Test
    void forecastProjectionShouldRejectMissingCalendarWhenConstructed() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new DemandPlanForecastProjectionMaterialLocation(
                        null,
                        new UnidadeMedida("UN"),
                        null,
                        null,
                        false));

        Assertions.assertEquals(
                "Demand Plan forecast projection requires calendar.",
                illegalArgumentException.getMessage());

    }

    @Test
    void forecastProjectionShouldRejectMissingUnitOfMeasureWhenConstructed() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new DemandPlanForecastProjectionAgregado(
                        getCalendarioTeste(),
                        null,
                        false));

        Assertions.assertEquals(
                "Demand Plan forecast projection requires unit of measure.",
                illegalArgumentException.getMessage());

    }

    @Test
    void materialLocationProjectionShouldRejectMissingLocationWhenConstructed() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new DemandPlanForecastProjectionMaterialLocation(
                        getCalendarioTeste(),
                        new UnidadeMedida("UN"),
                        null,
                        new Produto("MAT"),
                        false));

        Assertions.assertEquals(
                "Demand Plan forecast material/location projection requires location.",
                illegalArgumentException.getMessage());

    }

    @Test
    void materialLocationProjectionShouldRejectMissingMaterialWhenConstructed() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new DemandPlanForecastProjectionMaterialLocation(
                        getCalendarioTeste(),
                        new UnidadeMedida("UN"),
                        new Location("LOC"),
                        null,
                        false));

        Assertions.assertEquals(
                "Demand Plan forecast material/location projection requires material.",
                illegalArgumentException.getMessage());

    }

    @Test
    void materialLocationProjectionShouldRejectUninitializedLeafWhenListingItself() {

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                demandPlanForecastProjectionMaterialLocation::getDemandPlanForecastProjectionMaterialLocationList);

        Assertions.assertEquals(
                "Demand Plan forecast material/location projection requires location.",
                illegalArgumentException.getMessage());

    }

    private static DemandPlanForecastProjectionAgregado getDemandPlanForecastProjectionAgregado() {

        return new DemandPlanForecastProjectionAgregado(
                getCalendarioTeste(),
                new UnidadeMedida("UN"),
                false);

    }

    private static DemandPlanForecastProjectionMaterialLocation getDemandPlanForecastProjectionMaterialLocation() {

        return new DemandPlanForecastProjectionMaterialLocation(
                getCalendarioTeste(),
                new UnidadeMedida("UN"),
                new Location("LOC"),
                new Produto("MAT"),
                false);

    }

    private static Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);

    }

}
