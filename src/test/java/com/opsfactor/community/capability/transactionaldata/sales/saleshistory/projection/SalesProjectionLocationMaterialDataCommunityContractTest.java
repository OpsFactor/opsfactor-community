package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDateImpl;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMImpl;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDateImpl;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Contratos Community da projection neutra de vendas historicas.
 *
 * <p>A projection pode ser alimentada por sell-out no Community ou por overlays
 * Enterprise no futuro, mas qualquer consulta por calendario externo precisa
 * usar o mesmo bucket da extracao para evitar conversao temporal implicita.</p>
 */
class SalesProjectionLocationMaterialDataCommunityContractTest {

    @Test
    void builderShouldCreateUsableEmptySalesSnapshot() {

        SalesProjectionLocationMaterialData salesProjectionLocationMaterialData =
                SalesProjectionLocationMaterialData.builder().build();

        /*
         * Snapshot vazio e um resultado valido de extracao: significa que nao
         * houve vendas no recorte carregado. O mapa interno nao pode nascer
         * nulo pelo builder, porque consumidores de forecast e Planning Book
         * iteram essas colecoes sem voltar para factories.
         */
        Assertions.assertTrue(salesProjectionLocationMaterialData.getDFUsComSales().isEmpty());
        Assertions.assertTrue(salesProjectionLocationMaterialData.getMateriaisComSales().isEmpty());
        Assertions.assertTrue(salesProjectionLocationMaterialData.getLocationsComSales().isEmpty());
        Assertions.assertTrue(salesProjectionLocationMaterialData.getSetSalesConsolidado().isEmpty());

    }

    @Test
    void allSalesProjectionBuildersShouldCreateUsableEmptyIndexes() {

        Produto material = new Produto("MAT");
        Location location = new Location("LOC");
        LocalDate referenceDate = LocalDate.of(2026, 7, 5);

        /*
         * As projections podem nascer vazias em simulacoes, filtros sem vendas
         * ou factories que detectam escopo operacional vazio. O builder nao
         * pode deixar mapas internos nulos, porque consumers leem os snapshots
         * diretamente, sem passar novamente pelas factories.
         */
        Assertions.assertTrue(
                SalesProjectionMaterial.builder()
                        .build()
                        .getMateriaisComSales()
                        .isEmpty());
        Assertions.assertTrue(
                SalesProjectionMaterialData.builder()
                        .build()
                        .getSetSalesAgregado(
                                material,
                                referenceDate)
                        .isEmpty());
        Assertions.assertTrue(
                SalesProjectionLocationMaterial.builder()
                        .build()
                        .getSetSalesConsolidado(
                                material,
                                location)
                        .isEmpty());

    }

    @Test
    void addSalesAgregadoShouldRejectIncompleteLocationMaterialDateAggregateBeforeIndexMutation() {

        SalesProjectionLocationMaterialData salesProjectionLocationMaterialData =
                SalesProjectionLocationMaterialData.builder().build();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> salesProjectionLocationMaterialData.addSalesAgregado(
                        AggregatedByLocationMaterialUOMDateImpl.builder()
                                .material(new Produto("MAT"))
                                .location(null)
                                .uom(new UnidadeMedida("UN"))
                                .referenceDate(LocalDate.of(2026, 7, 5))
                                .totalQuantity(10.0)
                                .build()));

        Assertions.assertEquals(
                "Sales projection aggregate location is required for location-material-date sales projection.",
                illegalArgumentException.getMessage());
        Assertions.assertTrue(
                salesProjectionLocationMaterialData.getSetSalesConsolidado().isEmpty());

    }

    @Test
    void addSalesAgregadoShouldRejectAggregateWithMaterialOrLocationWithoutIdBeforeIndexMutation() {

        SalesProjectionLocationMaterialData salesProjectionLocationMaterialData =
                SalesProjectionLocationMaterialData.builder().build();

        IllegalArgumentException materialIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> salesProjectionLocationMaterialData.addSalesAgregado(
                        AggregatedByLocationMaterialUOMDateImpl.builder()
                                .material(new Produto(" "))
                                .location(new Location("LOC"))
                                .uom(new UnidadeMedida("UN"))
                                .referenceDate(LocalDate.of(2026, 7, 5))
                                .totalQuantity(10.0)
                                .build()));
        IllegalArgumentException locationIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> salesProjectionLocationMaterialData.addSalesAgregado(
                        AggregatedByLocationMaterialUOMDateImpl.builder()
                                .material(new Produto("MAT"))
                                .location(new Location(" "))
                                .uom(new UnidadeMedida("UN"))
                                .referenceDate(LocalDate.of(2026, 7, 5))
                                .totalQuantity(10.0)
                                .build()));

        Assertions.assertEquals(
                "Sales projection aggregate material id is required for location-material-date sales projection.",
                materialIdException.getMessage());
        Assertions.assertEquals(
                "Sales projection aggregate location id is required for location-material-date sales projection.",
                locationIdException.getMessage());
        Assertions.assertTrue(
                salesProjectionLocationMaterialData.getSetSalesConsolidado().isEmpty());

    }

    @Test
    void addSalesAgregadoShouldRejectInvalidMaterialDateQuantityBeforeIndexMutation() {

        SalesProjectionMaterialData salesProjectionMaterialData =
                SalesProjectionMaterialData.builder().build();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> salesProjectionMaterialData.addSalesAgregado(
                        AggregatedByMaterialUOMDateImpl.builder()
                                .material(new Produto("MAT"))
                                .uom(new UnidadeMedida("UN"))
                                .referenceDate(LocalDate.of(2026, 7, 5))
                                .totalQuantity(Double.NaN)
                                .build()));

        Assertions.assertEquals(
                "Sales projection aggregate quantity must be finite for material-date sales projection.",
                illegalArgumentException.getMessage());
        Assertions.assertTrue(
                salesProjectionMaterialData.getSetSalesAgregado(
                                new Produto("MAT"),
                                LocalDate.of(2026, 7, 5))
                        .isEmpty());

    }

    @Test
    void addSalesAgregadoShouldRejectMissingLocationMaterialUomBeforeIndexMutation() {

        SalesProjectionLocationMaterial salesProjectionLocationMaterial =
                SalesProjectionLocationMaterial.builder().build();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> salesProjectionLocationMaterial.addSalesAgregado(
                        AggregatedByLocationMaterialUOMImpl.builder()
                                .material(new Produto("MAT"))
                                .location(new Location("LOC"))
                                .uom(null)
                                .totalQuantity(10.0)
                                .build()));

        Assertions.assertEquals(
                "Sales projection aggregate UOM is required for location-material sales projection.",
                illegalArgumentException.getMessage());
        Assertions.assertTrue(
                salesProjectionLocationMaterial
                        .getSetSalesConsolidado(
                                new Produto("MAT"),
                                new Location("LOC"))
                        .isEmpty());

    }

    @Test
    void getQuantidadeSalesShouldRejectQueryCalendarWithDifferentBucketBeforeReadingData() {

        Calendario calendarioDiario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 6, 24, 0, 0),
                0,
                0,
                1,
                0);
        Calendario calendarioSemanal = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.SEMANAL,
                LocalDateTime.of(2026, 6, 24, 0, 0),
                0,
                0,
                1,
                0);
        SalesProjectionLocationMaterialData salesProjectionLocationMaterialData =
                SalesProjectionLocationMaterialData.builder()
                        .calendario(calendarioDiario)
                        .build();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> salesProjectionLocationMaterialData.getQuantidadeSales(
                        new Produto("MAT"),
                        new Location("LOC"),
                        calendarioSemanal,
                        0,
                        null));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "SalesProjectionLocationMaterialData requires the query calendar bucket to match the projection calendar bucket"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("query bucket=SEMANAL"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("projection bucket=DIARIO"));

    }

}
