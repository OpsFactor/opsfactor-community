package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

/**
 * Contratos diretos da projection de demanda direta considerada.
 *
 * <p>A factory protege snapshots vindos do repository. Estes testes cobrem a
 * outra borda: chamadas diretas feitas por rotinas de Supply Planning, testes
 * ou overlays Enterprise precisam falhar antes de inserir chaves quebradas no
 * BI em memoria por material/location/periodo.</p>
 */
class DemandaDiretaConsideradaProjectionTest {

    @Test
    void constructorShouldRejectMissingSupplyPlanOrCalendarBeforeIndexCreation() {

        IllegalArgumentException missingSupplyPlanException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new DemandaDiretaConsideradaProjection(
                        null,
                        getCalendarioTeste(),
                        Mockito.mock(UnidadeMedidaProjection.class)));
        Assertions.assertEquals(
                "Direct demand considered projection requires supply plan and calendar.",
                missingSupplyPlanException.getMessage());

        IllegalArgumentException missingCalendarException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new DemandaDiretaConsideradaProjection(
                        new SupplyPlan(),
                        null,
                        Mockito.mock(UnidadeMedidaProjection.class)));
        Assertions.assertEquals(
                "Direct demand considered projection requires supply plan and calendar.",
                missingCalendarException.getMessage());

    }

    @Test
    void addDemandLineShouldRejectBrokenFunctionalKeyBeforeBiMutation() {

        DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection =
                getDemandaDiretaConsideradaProjection();

        IllegalArgumentException nullLineException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaProjection.addDemandPlanRestritoEIrrestritoLinha(null));
        Assertions.assertEquals(
                "Direct demand considered projection cannot index null line.",
                nullLineException.getMessage());

        IllegalArgumentException missingCompositeKeyException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaProjection.addDemandPlanRestritoEIrrestritoLinha(
                        new DemandaDiretaConsideradaLinha()));
        Assertions.assertEquals(
                "Direct demand considered projection requires line with supply plan, location, material and reference date before indexing.",
                missingCompositeKeyException.getMessage());

        IllegalArgumentException missingLocationIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaProjection.addDemandPlanRestritoEIrrestritoLinha(
                        getDemandaDiretaConsideradaLinha(
                                new Location(" "),
                                new Produto("MAT-01"),
                                LocalDateTime.of(2026, 1, 1, 0, 0))));
        Assertions.assertEquals(
                "Direct demand considered projection requires line location with id before indexing.",
                missingLocationIdException.getMessage());

        IllegalArgumentException missingMaterialIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaProjection.addDemandPlanRestritoEIrrestritoLinha(
                        getDemandaDiretaConsideradaLinha(
                                new Location("LOC-01"),
                                new Produto(" "),
                                LocalDateTime.of(2026, 1, 1, 0, 0))));
        Assertions.assertEquals(
                "Direct demand considered projection requires line material with id before indexing.",
                missingMaterialIdException.getMessage());

        Assertions.assertTrue(
                demandaDiretaConsideradaProjection.getAllDemandaDiretaConsideradaLinha().isEmpty());

    }

    @Test
    void addDemandLineShouldKeepFirstLineWhenSameMonthlyFunctionalKeyIsRepeated() {

        DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection =
                new DemandaDiretaConsideradaProjection(
                        new SupplyPlan(),
                        getCalendarioMensalTeste(),
                        Mockito.mock(UnidadeMedidaProjection.class));
        Location location = new Location("LOC-01");
        Produto material = new Produto("MAT-01");
        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinhaPrimeira =
                getDemandaDiretaConsideradaLinha(
                        location,
                        material,
                        LocalDateTime.of(2026, 1, 1, 0, 0));
        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinhaDuplicada =
                getDemandaDiretaConsideradaLinha(
                        location,
                        material,
                        LocalDateTime.of(2026, 1, 20, 0, 0));

        demandaDiretaConsideradaProjection.addDemandPlanRestritoEIrrestritoLinha(
                demandaDiretaConsideradaLinhaPrimeira);
        demandaDiretaConsideradaProjection.addDemandPlanRestritoEIrrestritoLinha(
                demandaDiretaConsideradaLinhaDuplicada);

        /*
         * A deduplicacao mensal e uma regra transicional intencional: se o
         * banco ainda tiver horarios diferentes no mesmo mes, a projection
         * mantem a primeira linha lida para nao dobrar a demanda no Supply Plan.
         */
        Assertions.assertEquals(
                1,
                demandaDiretaConsideradaProjection.getAllDemandaDiretaConsideradaLinha().size());
        Assertions.assertSame(
                demandaDiretaConsideradaLinhaPrimeira,
                demandaDiretaConsideradaProjection.getDemandaDiretaConsideradaLinha(
                                location,
                                material,
                                0)
                        .get());

    }

    private static DemandaDiretaConsideradaProjection getDemandaDiretaConsideradaProjection() {

        return new DemandaDiretaConsideradaProjection(
                new SupplyPlan(),
                getCalendarioTeste(),
                Mockito.mock(UnidadeMedidaProjection.class));

    }

    private static DemandaDiretaConsideradaLinha getDemandaDiretaConsideradaLinha(
            Location location,
            Produto material,
            LocalDateTime dataReferencia) {

        return new DemandaDiretaConsideradaLinha(
                new DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey(
                        new SupplyPlan(),
                        location,
                        material,
                        dataReferencia));

    }

    private static Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0));

    }

    private static Calendario getCalendarioMensalTeste() {

        return Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 3, 1, 0, 0));

    }

}
