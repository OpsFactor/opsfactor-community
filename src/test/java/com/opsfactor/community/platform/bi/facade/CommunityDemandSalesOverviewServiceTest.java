package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.service.UnidadeMedidaService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityDemandSalesOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityDemandSalesOverviewSelectionDTO;
import com.opsfactor.community.capability.demandplanning.service.DemandPlanningService;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class CommunityDemandSalesOverviewServiceTest {

    @Test
    void shouldLoadSalesAndUnconstrainedPlanThroughOfficialBatchProjections() {

        UnidadeMedidaService unidadeMedidaService = Mockito.mock(UnidadeMedidaService.class);
        ClusterEParametrosProjectionFactory clusterFactory = Mockito.mock(ClusterEParametrosProjectionFactory.class);
        UnidadeMedidaProjectionFactory unitOfMeasureProjectionFactory =
                Mockito.mock(UnidadeMedidaProjectionFactory.class);
        SalesProjectionFactory salesProjectionFactory = Mockito.mock(SalesProjectionFactory.class);
        DemandPlanningService demandPlanningService = Mockito.mock(DemandPlanningService.class);
        DemandPlanProjectionFactory demandPlanProjectionFactory = Mockito.mock(DemandPlanProjectionFactory.class);

        CommunityDemandSalesOverviewService service = new CommunityDemandSalesOverviewService();
        ReflectionTestUtils.setField(service, "unidadeMedidaService", unidadeMedidaService);
        ReflectionTestUtils.setField(service, "clusterEParametrosProjectionFactory", clusterFactory);
        ReflectionTestUtils.setField(service, "unidadeMedidaProjectionFactory", unitOfMeasureProjectionFactory);
        ReflectionTestUtils.setField(service, "salesProjectionFactory", salesProjectionFactory);
        ReflectionTestUtils.setField(service, "demandPlanningService", demandPlanningService);
        ReflectionTestUtils.setField(service, "demandPlanProjectionFactory", demandPlanProjectionFactory);

        DemandPlan demandPlan = Mockito.mock(DemandPlan.class);
        UnidadeMedida unitOfMeasure = Mockito.mock(UnidadeMedida.class);
        UnidadeMedidaProjection unitOfMeasureProjection = Mockito.mock(UnidadeMedidaProjection.class);
        ClusterEParametrosProjection clusterProjection = Mockito.mock(ClusterEParametrosProjection.class);
        Location location = Mockito.mock(Location.class);
        Produto material = Mockito.mock(Produto.class);
        Mockito.when(location.getId()).thenReturn("LOC-1");
        Mockito.when(material.getId()).thenReturn("MAT-1");
        Mockito.when(clusterProjection.getLocationsAtivas()).thenReturn(Set.of(location));
        Mockito.when(clusterProjection.getMateriaisAtivos()).thenReturn(Set.of(material));
        Mockito.when(clusterProjection.getLocations(true)).thenReturn(Set.of(location));
        Mockito.when(clusterProjection.getMateriais(true)).thenReturn(Set.of(material));
        Mockito.when(clusterProjection.getMateriaisAtivosEmLocation(location)).thenReturn(Set.of(material));
        Mockito.when(clusterProjection.isDfuAtiva(material, location)).thenReturn(true);
        CaracteristicaProduto materialCharacteristic = Mockito.mock(CaracteristicaProduto.class);
        CaracteristicaLocation locationCharacteristic = Mockito.mock(CaracteristicaLocation.class);
        Mockito.when(clusterProjection.getCaracteristicaProdutoMap())
                .thenReturn(Map.of("PRODUCT_FAMILY", materialCharacteristic));
        Mockito.when(clusterProjection.getCaracteristicaLocationMap())
                .thenReturn(Map.of("CUSTOMER_REGION", locationCharacteristic));
        Mockito.when(materialCharacteristic.findValorCaracteristicaDeProduto(material))
                .thenReturn(Optional.of("Paper"));
        Mockito.when(locationCharacteristic.findValorCaracteristicaDeLocation(location))
                .thenReturn(Optional.of("South"));

        Calendario demandPlanCalendar = Calendario.criaCalendarioPeriodosFuturosDeDatas(
                com.opsfactor.community.platform.utility.Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2027, 1, 31, 23, 59));
        DemandPlanningProjection demandPlanningProjection = Mockito.mock(DemandPlanningProjection.class);
        Mockito.when(demandPlanningProjection.getCalendario()).thenReturn(demandPlanCalendar);
        DemandPlanItem demandPlanItem = Mockito.mock(DemandPlanItem.class);
        LocalDateTime demandReferenceDate = LocalDateTime.of(2026, 2, 28, 23, 59, 59);
        Mockito.when(demandPlanItem.getLocation()).thenReturn(location);
        Mockito.when(demandPlanItem.getProduto()).thenReturn(material);
        Mockito.when(demandPlanItem.getDataReferencia()).thenReturn(demandReferenceDate);
        Mockito.when(demandPlanningProjection.getValorDemandPlanItem(
                demandPlanItem,
                Constantes.TipoDemanda.TOTAL,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                unitOfMeasure))
                .thenReturn(30.0d);
        Mockito.when(demandPlanningProjection.getTodosDemandPlanItems()).thenReturn(Set.of(demandPlanItem));

        SalesProjectionLocationMaterialData salesProjection = Mockito.mock(SalesProjectionLocationMaterialData.class);
        AggregatedByLocationMaterialUOMDate aggregatedSales = Mockito.mock(AggregatedByLocationMaterialUOMDate.class);
        LocalDate salesReferenceDate = LocalDate.of(2026, 1, 31);
        Mockito.when(aggregatedSales.getLocation()).thenReturn(location);
        Mockito.when(aggregatedSales.getMaterial()).thenReturn(material);
        Mockito.when(aggregatedSales.getReferenceDate()).thenReturn(salesReferenceDate);
        Mockito.when(salesProjection.getSetSalesConsolidado()).thenReturn(Set.of(aggregatedSales));
        Calendario salesCalendar = Calendario.criaCalendarioPeriodosFuturosDeDatas(
                com.opsfactor.community.platform.utility.Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2025, 12, 1, 0, 0),
                LocalDateTime.of(2027, 1, 31, 23, 59));
        Mockito.when(salesProjection.getCalendario()).thenReturn(salesCalendar);
        Mockito.when(salesProjection.getQuantidadeSales(material, location, salesReferenceDate, unitOfMeasure))
                .thenReturn(12.0d);

        Mockito.when(demandPlanningService.getDemandPlanDeId(10L)).thenReturn(demandPlan);
        Mockito.when(unidadeMedidaService.getUnidadeMedida("PC")).thenReturn(unitOfMeasure);
        Mockito.when(clusterFactory.getParametrosProjectionCompletoDeCache()).thenReturn(clusterProjection);
        Mockito.when(unitOfMeasureProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache())
                .thenReturn(unitOfMeasureProjection);
        Mockito.when(demandPlanProjectionFactory.getDemandPlanningProjectionCompleto(
                eq(demandPlan), any(), eq(false))).thenReturn(demandPlanningProjection);
        Mockito.when(salesProjectionFactory.getSalesProjectionLocationMaterialData(
                any(), any(), any(), any(), eq(unitOfMeasureProjection), eq(clusterProjection), any()))
                .thenReturn(salesProjection);
        CommunityDemandSalesOverviewDTO result = service.getDemandSalesOverview(
                new CommunityDemandSalesOverviewSelectionDTO(
                        10L, null, "PC", 2, List.of(), List.of(), Map.of(), Map.of()));

        Assertions.assertEquals(2, result.data().size());
        Assertions.assertEquals(12.0d, result.data().getFirst().historicalSales());
        Assertions.assertEquals(30.0d, result.data().getLast().unconstrainedPlan());
        Assertions.assertEquals("Paper", result.data().getFirst().valuesByMaterialCharacteristicId().get("PRODUCT_FAMILY"));
        Assertions.assertEquals("South", result.data().getFirst().valuesByLocationCharacteristicId().get("CUSTOMER_REGION"));
        ArgumentCaptor<FiltroDFUProjection> dfuCaptor =
                ArgumentCaptor.forClass(FiltroDFUProjection.class);
        Mockito.verify(demandPlanProjectionFactory).getDemandPlanningProjectionCompleto(
                eq(demandPlan), dfuCaptor.capture(), eq(false));
        Assertions.assertEquals(1, dfuCaptor.getValue().getDFUs().size());
        Mockito.verify(demandPlanningProjection).getValorDemandPlanItem(
                demandPlanItem,
                Constantes.TipoDemanda.TOTAL,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                unitOfMeasure);
    }

    @Test
    void shouldLoadOnlyHistoricalSalesWhenNoDemandPlanIsSelected() {

        UnidadeMedidaService unidadeMedidaService = Mockito.mock(UnidadeMedidaService.class);
        ClusterEParametrosProjectionFactory clusterFactory = Mockito.mock(ClusterEParametrosProjectionFactory.class);
        UnidadeMedidaProjectionFactory unitOfMeasureProjectionFactory =
                Mockito.mock(UnidadeMedidaProjectionFactory.class);
        SalesProjectionFactory salesProjectionFactory = Mockito.mock(SalesProjectionFactory.class);
        DemandPlanningService demandPlanningService = Mockito.mock(DemandPlanningService.class);
        DemandPlanProjectionFactory demandPlanProjectionFactory = Mockito.mock(DemandPlanProjectionFactory.class);

        CommunityDemandSalesOverviewService service = new CommunityDemandSalesOverviewService();
        ReflectionTestUtils.setField(service, "unidadeMedidaService", unidadeMedidaService);
        ReflectionTestUtils.setField(service, "clusterEParametrosProjectionFactory", clusterFactory);
        ReflectionTestUtils.setField(service, "unidadeMedidaProjectionFactory", unitOfMeasureProjectionFactory);
        ReflectionTestUtils.setField(service, "salesProjectionFactory", salesProjectionFactory);
        ReflectionTestUtils.setField(service, "demandPlanningService", demandPlanningService);
        ReflectionTestUtils.setField(service, "demandPlanProjectionFactory", demandPlanProjectionFactory);

        UnidadeMedida unitOfMeasure = Mockito.mock(UnidadeMedida.class);
        UnidadeMedidaProjection unitOfMeasureProjection = Mockito.mock(UnidadeMedidaProjection.class);
        ClusterEParametrosProjection clusterProjection = Mockito.mock(ClusterEParametrosProjection.class);
        Location location = Mockito.mock(Location.class);
        Produto material = Mockito.mock(Produto.class);
        Mockito.when(location.getId()).thenReturn("LOC-1");
        Mockito.when(material.getId()).thenReturn("MAT-1");
        Mockito.when(clusterProjection.getLocations(true)).thenReturn(Set.of(location));
        Mockito.when(clusterProjection.getMateriais(true)).thenReturn(Set.of(material));
        Mockito.when(clusterProjection.getMateriaisAtivosEmLocation(location)).thenReturn(Set.of(material));
        Mockito.when(clusterProjection.getCaracteristicaProdutoMap()).thenReturn(Map.of());
        Mockito.when(clusterProjection.getCaracteristicaLocationMap()).thenReturn(Map.of());

        SalesProjectionLocationMaterialData salesProjection = Mockito.mock(SalesProjectionLocationMaterialData.class);
        Mockito.when(salesProjection.getSetSalesConsolidado()).thenReturn(Set.of());
        LocalDateTime currentDateTime = LocalDateTime.now();
        Mockito.when(salesProjection.getCalendario()).thenReturn(
                Calendario.criaCalendarioPeriodosFuturosDeDatas(
                        Constantes.TamanhoBucket.MENSAL,
                        currentDateTime.minusMonths(1),
                        currentDateTime));

        Mockito.when(unidadeMedidaService.getUnidadeMedida("PC")).thenReturn(unitOfMeasure);
        Mockito.when(clusterFactory.getParametrosProjectionCompletoDeCache()).thenReturn(clusterProjection);
        Mockito.when(unitOfMeasureProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache())
                .thenReturn(unitOfMeasureProjection);
        Mockito.when(salesProjectionFactory.getSalesProjectionLocationMaterialData(
                eq(Constantes.TipoDocumentoVenda.SELLOUT),
                any(),
                any(),
                any(),
                eq(unitOfMeasureProjection),
                eq(clusterProjection),
                any()))
                .thenReturn(salesProjection);

        CommunityDemandSalesOverviewDTO result = service.getDemandSalesOverview(
                new CommunityDemandSalesOverviewSelectionDTO(
                        null,
                        Constantes.TipoDocumentoVenda.SELLOUT,
                        "PC",
                        1,
                        List.of(),
                        List.of(),
                        Map.of(),
                        Map.of()));

        Assertions.assertFalse(result.periods().isEmpty());
        Assertions.assertTrue(result.data().isEmpty());
        Mockito.verifyNoInteractions(demandPlanningService, demandPlanProjectionFactory);
        Mockito.verify(salesProjectionFactory).getSalesProjectionLocationMaterialData(
                eq(Constantes.TipoDocumentoVenda.SELLOUT),
                any(),
                any(),
                any(),
                eq(unitOfMeasureProjection),
                eq(clusterProjection),
                any());

    }

    @Test
    void shouldRejectMissingUnitOfMeasureBeforeOpeningDataSources() {

        CommunityDemandSalesOverviewService service = new CommunityDemandSalesOverviewService();

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.getDemandSalesOverview(
                        new CommunityDemandSalesOverviewSelectionDTO(
                                10L, null, " ", null, null, null, null, null)));

        Assertions.assertEquals("Demand Sales Overview unit of measure id is required.", exception.getMessage());

    }

    @Test
    void shouldBuildExplicitDfuScopeFromLocationActiveMaterialsWithoutCartesianScan() {

        ClusterEParametrosProjection clusterProjection = Mockito.mock(ClusterEParametrosProjection.class);
        Location firstLocation = Mockito.mock(Location.class);
        Location secondLocation = Mockito.mock(Location.class);
        Produto firstMaterial = Mockito.mock(Produto.class);
        Produto secondMaterial = Mockito.mock(Produto.class);

        Mockito.when(firstLocation.getId()).thenReturn("LOC-1");
        Mockito.when(secondLocation.getId()).thenReturn("LOC-2");
        Mockito.when(firstMaterial.getId()).thenReturn("MAT-1");
        Mockito.when(secondMaterial.getId()).thenReturn("MAT-2");
        Mockito.when(clusterProjection.getMateriaisAtivosEmLocation(firstLocation)).thenReturn(Set.of(firstMaterial));
        Mockito.when(clusterProjection.getMateriaisAtivosEmLocation(secondLocation)).thenReturn(Set.of(secondMaterial));
        Mockito.when(clusterProjection.getLocations(true)).thenReturn(Set.of(firstLocation, secondLocation));
        Mockito.when(clusterProjection.getMateriais(true)).thenReturn(Set.of(firstMaterial, secondMaterial));
        Mockito.when(clusterProjection.getLocationPersistida("LOC-1")).thenReturn(firstLocation);
        Mockito.when(clusterProjection.getLocationPersistida("LOC-2")).thenReturn(secondLocation);
        Mockito.when(clusterProjection.getMaterialPersistido("MAT-1")).thenReturn(firstMaterial);
        Mockito.when(clusterProjection.getMaterialPersistido("MAT-2")).thenReturn(secondMaterial);

        CommunityDemandSalesOverviewService service = new CommunityDemandSalesOverviewService();
        ReflectionTestUtils.setField(service, "clusterEParametrosProjectionFactory", Mockito.mock(ClusterEParametrosProjectionFactory.class));

        @SuppressWarnings("unchecked")
        FiltroDFUProjection dfuProjection =
                (FiltroDFUProjection) ReflectionTestUtils.invokeMethod(
                        service,
                        "getActiveDfuProjection",
                        new CommunityDemandSalesOverviewSelectionDTO(
                                10L,
                                null,
                                "PC",
                                null,
                                List.of("MAT-1", "MAT-2"),
                                List.of("LOC-1", "LOC-2"),
                                Map.of(),
                                Map.of()),
                        clusterProjection);

        Assertions.assertTrue(dfuProjection.contemCombinacaoLocationMaterial(firstLocation, firstMaterial));
        Assertions.assertTrue(dfuProjection.contemCombinacaoLocationMaterial(secondLocation, secondMaterial));
        Assertions.assertFalse(dfuProjection.contemCombinacaoLocationMaterial(firstLocation, secondMaterial));
        Assertions.assertFalse(dfuProjection.contemCombinacaoLocationMaterial(secondLocation, firstMaterial));
        Mockito.verify(clusterProjection, Mockito.never()).isDfuAtiva(Mockito.any(), Mockito.any());
    }
}
