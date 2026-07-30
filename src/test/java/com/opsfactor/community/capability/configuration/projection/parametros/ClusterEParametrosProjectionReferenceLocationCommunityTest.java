package com.opsfactor.community.capability.configuration.projection.parametros;

import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.utility.Constantes;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Garante que o snapshot de parametros resolve a location de referencia uma
 * unica vez, antes dos calculos em lote.
 */
class ClusterEParametrosProjectionReferenceLocationCommunityTest {

    @Test
    void projectionShouldReadParametersFromTheDirectReferenceOnly() {

        Produto material = new Produto("MAT-01");
        Location requestedLocation = new Location("LOC-REQUESTED");
        Location referenceLocation = new Location("LOC-REFERENCE");
        Location chainedReferenceLocation = new Location("LOC-CHAINED");
        requestedLocation.setReferenceLocationForProductLocationParameters(referenceLocation);
        referenceLocation.setReferenceLocationForProductLocationParameters(chainedReferenceLocation);

        ParametrosProdutoLocation referenceParameters = getParameters(
                material,
                referenceLocation,
                Constantes.StatusProduto.NOVO);
        ParametrosProdutoLocation chainedReferenceParameters = getParameters(
                material,
                chainedReferenceLocation,
                Constantes.StatusProduto.DESCONTINUADO);

        ClusterEParametrosProjection clusterEParametrosProjection =
                new ClusterEParametrosProjection();
        clusterEParametrosProjection.locationMap = Map.of(
                requestedLocation.getId(), requestedLocation,
                referenceLocation.getId(), referenceLocation,
                chainedReferenceLocation.getId(), chainedReferenceLocation);
        clusterEParametrosProjection.locationForProductLocationParametersMap = Map.of(
                requestedLocation, referenceLocation,
                referenceLocation, chainedReferenceLocation,
                chainedReferenceLocation, chainedReferenceLocation);
        clusterEParametrosProjection.mapaParametrosProdutoLocation = Map.of(
                referenceLocation, Map.of(material, referenceParameters),
                chainedReferenceLocation, Map.of(material, chainedReferenceParameters));

        Assertions.assertEquals(
                Constantes.StatusProduto.NOVO,
                clusterEParametrosProjection.getStatusProduto(
                        material,
                        requestedLocation,
                        LocalDateTime.of(2026, 1, 1, 0, 0)),
                "O snapshot deve usar o destino pre-resolvido do unico salto.");

    }

    private ParametrosProdutoLocation getParameters(
            Produto material,
            Location location,
            Constantes.StatusProduto statusProduto) {

        ParametrosProdutoLocation parametrosProdutoLocation =
                new ParametrosProdutoLocation(
                        new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(
                                material,
                                location));
        parametrosProdutoLocation.setEstagioCicloVida(statusProduto);

        return parametrosProdutoLocation;

    }
}
