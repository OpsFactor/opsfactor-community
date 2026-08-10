package com.opsfactor.community.capability.supplyplanning.supplyplan.integration.service;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.integration.dto.FulfilledDemandIntegrationDataDto;
import com.opsfactor.community.platform.bi.facade.CommunityProductionOverviewService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunitySupplyOverviewBaseDTO.DirectAndIndirectDemandDTO;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Testes do recorte fisico e read-only de Fulfilled Demand Community.
 */
public class FulfilledDemandIntegrationServiceTest {

    @Test
    public void filteredExportShouldUseProductionOverviewMetDemandSeries() {

        LocalDateTime referenceDate = LocalDateTime.of(2026, 8, 16, 23, 59, 59);
        CommunityProductionOverviewDTO productionOverview = new CommunityProductionOverviewDTO();
        productionOverview.finalDateTimeByPeriod.add(referenceDate);
        DirectAndIndirectDemandDTO demandSeries = new DirectAndIndirectDemandDTO(
                "PAPER_MILL",
                java.util.Map.of("materialId", "FG_COPY_A4"),
                "MT",
                1);
        demandSeries.unconstrainedDirectDemand[0] = 120F;
        demandSeries.constrainedDirectDemand[0] = 100F;
        productionOverview.directAndIndirectDemandByLocationAndMaterialGrouping.add(demandSeries);

        CommunityProductionOverviewService productionOverviewService =
                Mockito.mock(CommunityProductionOverviewService.class);
        Mockito.when(productionOverviewService.getProductionOverview(Mockito.any()))
                .thenReturn(productionOverview);
        LocationRepository locationRepository = Mockito.mock(LocationRepository.class);
        ProdutoRepository produtoRepository = Mockito.mock(ProdutoRepository.class);
        Location location = new Location("PAPER_MILL");
        location.setDescricao("Paper Mill");
        Produto material = new Produto("FG_COPY_A4");
        material.setDescricao("Copy Paper A4");
        Mockito.when(locationRepository.findAllById(Mockito.any())).thenReturn(List.of(location));
        Mockito.when(produtoRepository.findAllById(Mockito.any())).thenReturn(List.of(material));
        FulfilledDemandIntegrationService service = getService(
                productionOverviewService,
                locationRepository,
                produtoRepository);

        List<FulfilledDemandIntegrationDataDto> result =
                service.getFulfilledDemandDtoList(2L, "MT");

        Assertions.assertEquals(1, result.size());
        FulfilledDemandIntegrationDataDto fulfilledDemand = result.get(0);
        Assertions.assertEquals(2L, fulfilledDemand.getSupplyPlanId());
        Assertions.assertEquals("PAPER_MILL", fulfilledDemand.getLocationId());
        Assertions.assertEquals("Paper Mill", fulfilledDemand.getLocationDescription());
        Assertions.assertEquals("FG_COPY_A4", fulfilledDemand.getMaterialId());
        Assertions.assertEquals("Copy Paper A4", fulfilledDemand.getMaterialDescription());
        Assertions.assertEquals(referenceDate, fulfilledDemand.getReferenceDate());
        Assertions.assertEquals("MT", fulfilledDemand.getUnitOfMeasureId());
        Assertions.assertEquals(120D, fulfilledDemand.getUnconstrainedDemand());
        Assertions.assertEquals(100D, fulfilledDemand.getFulfilledDemand());
        Assertions.assertEquals(20D, fulfilledDemand.getUnmetDemand());
        Assertions.assertEquals(100D / 120D, fulfilledDemand.getFulfillmentRate());

        ArgumentCaptor<com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewSelectionDTO>
                selectionCaptor = ArgumentCaptor.forClass(
                        com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewSelectionDTO.class);
        Mockito.verify(productionOverviewService).getProductionOverview(selectionCaptor.capture());
        Assertions.assertEquals(2L, selectionCaptor.getValue().supplyPlanId);
        Assertions.assertEquals("MT", selectionCaptor.getValue().uomId);

        List<List<Object>> file = service.getFile(2L, "MT");
        Assertions.assertEquals("Fulfilled Demand", file.get(0).get(8));
        Assertions.assertEquals(100D, file.get(1).get(8));

    }

    @Test
    public void exportShouldRejectFulfilledQuantityAboveUnconstrainedDemand() {

        CommunityProductionOverviewDTO productionOverview = new CommunityProductionOverviewDTO();
        productionOverview.finalDateTimeByPeriod.add(LocalDateTime.of(2026, 8, 16, 23, 59, 59));
        DirectAndIndirectDemandDTO demandSeries = new DirectAndIndirectDemandDTO(
                "PAPER_MILL",
                java.util.Map.of("materialId", "FG_COPY_A4"),
                "MT",
                1);
        demandSeries.unconstrainedDirectDemand[0] = 100F;
        demandSeries.constrainedDirectDemand[0] = 120F;
        productionOverview.directAndIndirectDemandByLocationAndMaterialGrouping.add(demandSeries);
        CommunityProductionOverviewService productionOverviewService =
                Mockito.mock(CommunityProductionOverviewService.class);
        Mockito.when(productionOverviewService.getProductionOverview(Mockito.any()))
                .thenReturn(productionOverview);
        FulfilledDemandIntegrationService service = getService(
                productionOverviewService,
                Mockito.mock(LocationRepository.class),
                Mockito.mock(ProdutoRepository.class));

        DataUploadException error = Assertions.assertThrows(
                DataUploadException.class,
                () -> service.getFulfilledDemandDtoList(2L, "MT"));

        Assertions.assertTrue(error.getMessage().contains("exceeds unconstrained demand"));

    }

    private FulfilledDemandIntegrationService getService(
            CommunityProductionOverviewService productionOverviewService,
            LocationRepository locationRepository,
            ProdutoRepository produtoRepository) {

        FulfilledDemandIntegrationService service = new FulfilledDemandIntegrationService();
        ReflectionTestUtils.setField(
                service,
                "communityProductionOverviewService",
                productionOverviewService);
        ReflectionTestUtils.setField(service, "locationRepository", locationRepository);
        ReflectionTestUtils.setField(service, "produtoRepository", produtoRepository);
        return service;

    }

}
