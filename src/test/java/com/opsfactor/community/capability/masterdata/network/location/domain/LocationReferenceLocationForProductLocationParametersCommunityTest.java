package com.opsfactor.community.capability.masterdata.network.location.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato Community da location de referencia para parametros de
 * material/location.
 *
 * <p>A associacao pertence ao aggregate compartilhado e substitui apenas a
 * location de leitura dos parametros. Ela e deliberadamente unidirecional:
 * carregar a location de referencia nao deve carregar dependentes.</p>
 */
class LocationReferenceLocationForProductLocationParametersCommunityTest {

    @Test
    void referenceLocationShouldBeLazyUnidirectionalManyToOne() throws NoSuchFieldException {

        Field referenceLocationField = Location.class.getDeclaredField(
                "referenceLocationForProductLocationParameters");
        ManyToOne manyToOne = referenceLocationField.getAnnotation(ManyToOne.class);

        Assertions.assertEquals(Location.class, referenceLocationField.getType());
        Assertions.assertNotNull(manyToOne);
        Assertions.assertEquals(FetchType.LAZY, manyToOne.fetch());

    }

    @Test
    void materialShouldUseOnlyTheDirectReferenceLocationParameters() {

        Produto material = new Produto("MAT-01");
        Location requestedLocation = new Location("LOC-REQUESTED");
        Location referenceLocation = new Location("LOC-REFERENCE");
        Location chainedReferenceLocation = new Location("LOC-CHAINED");
        requestedLocation.setReferenceLocationForProductLocationParameters(referenceLocation);
        referenceLocation.setReferenceLocationForProductLocationParameters(chainedReferenceLocation);

        ParametrosProdutoLocation referenceParameters = getParameters(material, referenceLocation);
        ParametrosProdutoLocation chainedReferenceParameters = getParameters(
                material,
                chainedReferenceLocation);
        referenceLocation.setMapaParametrosProdutoLocation(Map.of(material, referenceParameters));
        chainedReferenceLocation.setMapaParametrosProdutoLocation(
                Map.of(material, chainedReferenceParameters));

        Assertions.assertSame(
                referenceParameters,
                material.getParametrosProdutoLocation(requestedLocation),
                "A leitura pontual deve usar somente a referencia direta, sem seguir cadeia.");

    }

    private ParametrosProdutoLocation getParameters(Produto material, Location location) {

        return new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(
                        material,
                        location));

    }
}
