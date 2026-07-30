package com.opsfactor.community.capability.masterdata.network.location.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Contratos dos defaults estáticos de capacidade pertencentes à Location. */
class LocationLogisticsCapacityContractTest {

    @Test
    void staticLogisticsCapacityShouldKeepDefaultsAndConvertInboundAndOutboundBuckets() {

        Location location = new Location("LOC-CAPACITY");
        location.setCapacidadeInboundPadrao(100.0d);
        location.setCapacidadeOutboundPadrao(200.0d);
        location.setPeriodoIncidenciaCapacidadeInboundPadrao(Constantes.TamanhoBucket.MENSAL);
        location.setPeriodoIncidenciaCapacidadeOutboundPadrao(Constantes.TamanhoBucket.MENSAL);
        location.setCapacidadeInboundFinita(true);
        location.setCapacidadeOutboundFinita(true);

        Assertions.assertTrue(location.getCapacidadeInboundFinita());
        Assertions.assertTrue(location.getCapacidadeOutboundFinita());
        Assertions.assertEquals(100.0d, location.getCapacidadeInboundNoBucketTarget(Constantes.TamanhoBucket.MENSAL));
        Assertions.assertEquals(200.0d, location.getCapacidadeOutboundNoBucketTarget(Constantes.TamanhoBucket.MENSAL));

    }

    @Test
    void staticLogisticsCapacityShouldRejectInvalidValuesWhenRead() {

        Location location = new Location("LOC-CAPACITY");
        location.setCapacidadeArmazenagemPadrao(-1.0d);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                location::getCapacidadeArmazenagemPadrao);

        Assertions.assertTrue(exception.getMessage().contains("LOC-CAPACITY"));

    }

}
