package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contratos do indice mutavel de primeira/ultima venda historica.
 */
class FirstLastSalesProjectionTest {

    @Test
    void constructorShouldRejectMissingCalendar() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FirstLastSalesProjection(null));

        Assertions.assertEquals(
                "First/last sales projection calendar is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    void addMaterialLocationEntryShouldRejectInvertedDates() {

        FirstLastSalesProjection firstLastSalesProjection = criaFirstLastSalesProjection();
        FirstLastRow firstLastRow = new FirstLastRow(
                new Produto("MAT"),
                new Location("LOC"),
                LocalDateTime.of(2026, 1, 3, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> firstLastSalesProjection.addFirstLastByMaterialLocation(firstLastRow));

        Assertions.assertEquals(
                "First/last material/location entry returned last date 2026-01-01T00:00 before first date 2026-01-03T00:00.",
                illegalArgumentException.getMessage());

    }

    @Test
    void addMaterialLocationEntryShouldRejectDuplicatedFunctionalKey() {

        FirstLastSalesProjection firstLastSalesProjection = criaFirstLastSalesProjection();
        FirstLastRow firstLastRow = criaFirstLastRowValida();
        firstLastSalesProjection.addFirstLastByMaterialLocation(firstLastRow);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> firstLastSalesProjection.addFirstLastByMaterialLocation(firstLastRow));

        Assertions.assertEquals(
                "First/last sales projection already contains material/location entry material MAT / location LOC.",
                illegalStateException.getMessage());

    }

    @Test
    void addMaterialAndLocationEntriesShouldRejectDuplicatedFunctionalKeys() {

        FirstLastSalesProjection firstLastSalesProjection = criaFirstLastSalesProjection();
        FirstLastRow firstLastRow = criaFirstLastRowValida();
        firstLastSalesProjection.addFirstLastByMaterial(firstLastRow);
        firstLastSalesProjection.addFirstLastByLocation(firstLastRow);

        IllegalStateException materialException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> firstLastSalesProjection.addFirstLastByMaterial(firstLastRow));
        IllegalStateException locationException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> firstLastSalesProjection.addFirstLastByLocation(firstLastRow));

        Assertions.assertEquals(
                "First/last sales projection already contains material entry material MAT.",
                materialException.getMessage());
        Assertions.assertEquals(
                "First/last sales projection already contains location entry location LOC.",
                locationException.getMessage());

    }

    @Test
    void addLocationEntryShouldRejectLocationWithoutId() {

        FirstLastSalesProjection firstLastSalesProjection = criaFirstLastSalesProjection();
        FirstLastRow firstLastRow = new FirstLastRow(
                new Produto("MAT"),
                new Location(" "),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> firstLastSalesProjection.addFirstLastByLocation(firstLastRow));

        Assertions.assertEquals(
                "First/last location entry requires location with id.",
                illegalArgumentException.getMessage());

    }

    @Test
    void lookupMethodsShouldRejectBrokenFunctionalKeys() {

        FirstLastSalesProjection firstLastSalesProjection = criaFirstLastSalesProjection();

        IllegalArgumentException materialLocationException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> firstLastSalesProjection.getFirstLastByMaterialLocation(
                        new Location("LOC"),
                        new Produto(" ")));
        IllegalArgumentException materialException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> firstLastSalesProjection.getFirstLastByMaterial(null));
        IllegalArgumentException locationException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> firstLastSalesProjection.getFirstLastByLocation(new Location(" ")));

        Assertions.assertEquals(
                "First/last material/location lookup requires material with id.",
                materialLocationException.getMessage());
        Assertions.assertEquals(
                "First/last material lookup requires material with id.",
                materialException.getMessage());
        Assertions.assertEquals(
                "First/last location lookup requires location with id.",
                locationException.getMessage());

    }

    private FirstLastSalesProjection criaFirstLastSalesProjection() {

        return new FirstLastSalesProjection(Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0)));

    }

    private FirstLastRow criaFirstLastRowValida() {

        return new FirstLastRow(
                new Produto("MAT"),
                new Location("LOC"),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0));

    }

    private static class FirstLastRow implements
            FirstLastByMaterialLocation,
            FirstLastByLocation,
            FirstLastByMaterial {

        private final Produto material;

        private final Location location;

        private final LocalDateTime firstDateTime;

        private final LocalDateTime lastDateTime;

        private FirstLastRow(
                Produto material,
                Location location,
                LocalDateTime firstDateTime,
                LocalDateTime lastDateTime) {

            this.material = material;
            this.location = location;
            this.firstDateTime = firstDateTime;
            this.lastDateTime = lastDateTime;

        }

        @Override
        public Produto getMaterial() {

            return material;

        }

        @Override
        public Location getLocation() {

            return location;

        }

        @Override
        public LocalDateTime getFirstDateTime() {

            return firstDateTime;

        }

        @Override
        public LocalDateTime getLastDateTime() {

            return lastDateTime;

        }

        @Override
        public Double getTotalQuantity() {

            return 1.0D;

        }

        @Override
        public UnidadeMedida getUom() {

            return null;

        }

    }

}
