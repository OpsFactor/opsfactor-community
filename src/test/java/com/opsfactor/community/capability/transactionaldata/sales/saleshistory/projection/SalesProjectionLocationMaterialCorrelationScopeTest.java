package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDateImpl;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Regressao da borda correlacionada de SalesProjectionFactory.
 *
 * <p>O double captura a chamada da query de envelope. Assim a prova nao
 * depende de provider JPA: confirma que ha uma unica leitura e que pares
 * cruzados retornados por {@code location IN (...) AND material IN (...)} nao
 * sobrevivem na projection final.</p>
 */
public class SalesProjectionLocationMaterialCorrelationScopeTest {

    @Test
    public void shouldUseOneEnvelopeReadAndDiscardCrossedLocationMaterialPairs() {

        Location locationNorth = new Location("LOC_NORTH");
        Location locationSouth = new Location("LOC_SOUTH");
        Produto materialAlpha = new Produto("MAT_ALPHA");
        Produto materialBeta = new Produto("MAT_BETA");

        Map<Location, Set<Produto>> materiaisPorLocation = new LinkedHashMap<>();
        materiaisPorLocation.put(locationNorth, Set.of(materialAlpha));
        materiaisPorLocation.put(locationSouth, Set.of(materialBeta));
        LocationMaterialCorrelationScope scope =
                LocationMaterialCorrelationScope.of(materiaisPorLocation);

        CapturingSalesProjectionFactory salesProjectionFactory =
                new CapturingSalesProjectionFactory(
                        getProjectionWithAllEnvelopePairs(
                                locationNorth,
                                locationSouth,
                                materialAlpha,
                                materialBeta));

        SalesProjectionLocationMaterialData correlatedProjection =
                salesProjectionFactory.getSalesProjectionLocationMaterialData(
                        Constantes.TipoDocumentoVenda.SELLOUT,
                        null,
                        scope,
                        null,
                        null,
                        null);

        Assertions.assertEquals(1, salesProjectionFactory.getEnvelopeReadCount());
        Assertions.assertEquals(
                Set.of(locationNorth, locationSouth),
                salesProjectionFactory.getLocationsPassedToEnvelopeRead());
        Assertions.assertEquals(
                Set.of(materialAlpha, materialBeta),
                salesProjectionFactory.getMaterialsPassedToEnvelopeRead());
        Assertions.assertEquals(2, correlatedProjection.getSetSalesConsolidado().size());
        Assertions.assertEquals(
                1,
                correlatedProjection.getSetSalesConsolidado(materialAlpha, locationNorth).size());
        Assertions.assertEquals(
                1,
                correlatedProjection.getSetSalesConsolidado(materialBeta, locationSouth).size());
        Assertions.assertTrue(
                correlatedProjection.getSetSalesConsolidado(materialBeta, locationNorth).isEmpty());
        Assertions.assertTrue(
                correlatedProjection.getSetSalesConsolidado(materialAlpha, locationSouth).isEmpty());

    }

    @Test
    public void shouldMapExplicitDfuPairsWithoutListingOnlyActiveDfus() {

        Location locationNorth = new Location("LOC_NORTH");
        Location locationSouth = new Location("LOC_SOUTH");
        Produto materialAlpha = new Produto("MAT_ALPHA");
        Produto materialBeta = new Produto("MAT_BETA");
        FiltroDFUProjection explicitDfuScope = new FiltroDFUProjection(
                Set.of(
                        new DFU(materialAlpha, locationNorth),
                        new DFU(materialBeta, locationSouth)),
                null);

        LocationMaterialCorrelationScope correlationScope =
                LocationMaterialCorrelationScope.fromDfuScope(explicitDfuScope);

        Assertions.assertTrue(correlationScope.contains(locationNorth, materialAlpha));
        Assertions.assertTrue(correlationScope.contains(locationSouth, materialBeta));
        Assertions.assertFalse(correlationScope.contains(locationNorth, materialBeta));
        Assertions.assertFalse(correlationScope.contains(locationSouth, materialAlpha));

    }

    private static SalesProjectionLocationMaterialData getProjectionWithAllEnvelopePairs(
            Location locationNorth,
            Location locationSouth,
            Produto materialAlpha,
            Produto materialBeta) {

        UnidadeMedida unidadeMedida = new UnidadeMedida("EA");
        SalesProjectionLocationMaterialData salesProjection =
                SalesProjectionLocationMaterialData.builder()
                        .mapaVendasAgregadasPorPeriodo(new LinkedHashMap<>())
                        .build();

        for (Location location : Set.of(locationNorth, locationSouth)) {
            for (Produto material : Set.of(materialAlpha, materialBeta)) {
                salesProjection.addSalesAgregado(
                        AggregatedByLocationMaterialUOMDateImpl.builder()
                                .location(location)
                                .material(material)
                                .uom(unidadeMedida)
                                .referenceDate(LocalDate.of(2026, 1, 1))
                                .totalQuantity(1.0d)
                                .build());
            }
        }

        return salesProjection;

    }

    private static class CapturingSalesProjectionFactory extends SalesProjectionFactory {

        private final SalesProjectionLocationMaterialData envelopeSalesProjection;
        private int envelopeReadCount;
        private Set<Location> locationsPassedToEnvelopeRead;
        private Set<Produto> materialsPassedToEnvelopeRead;

        private CapturingSalesProjectionFactory(
                SalesProjectionLocationMaterialData envelopeSalesProjection) {

            this.envelopeSalesProjection = envelopeSalesProjection;

        }

        @Override
        public SalesProjectionLocationMaterialData getSalesProjectionLocationMaterialData(
                Constantes.TipoDocumentoVenda tipoDocumentoVenda,
                Calendario calendario,
                Set<Location> locations,
                Set<Produto> produtos,
                UnidadeMedidaProjection unidadeMedidaProjection,
                ClusterEParametrosProjection clusterEParametrosProjection,
                UnidadeMedida unidadePadrao) {

            envelopeReadCount++;
            locationsPassedToEnvelopeRead = locations;
            materialsPassedToEnvelopeRead = produtos;
            return envelopeSalesProjection;

        }

        private int getEnvelopeReadCount() {

            return envelopeReadCount;

        }

        private Set<Location> getLocationsPassedToEnvelopeRead() {

            return locationsPassedToEnvelopeRead;

        }

        private Set<Produto> getMaterialsPassedToEnvelopeRead() {

            return materialsPassedToEnvelopeRead;

        }

    }

}
