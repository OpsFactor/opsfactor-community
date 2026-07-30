package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.platform.bi.facade.dto.CommunityMaterialFlowsDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

class CommunityMaterialFlowsServiceTest {

    @Test
    void shouldAggregateUnconstrainedPlannedAndFirmFlowsIntoLegacySquareMatrix() {

        DistributionPlanItemRepository repository = Mockito.mock(DistributionPlanItemRepository.class);
        CommunityMaterialFlowsService service = new CommunityMaterialFlowsService();
        ReflectionTestUtils.setField(service, "distributionPlanItemRepository", repository);

        Location firstLocation = location("LOC-A");
        Location secondLocation = location("LOC-B");
        Location thirdLocation = location("LOC-C");
        DistributionPlanItem firstOutboundLine = distributionPlanItem(
                firstLocation, secondLocation, 4.0d, 1.0d);
        DistributionPlanItem secondOutboundLine = distributionPlanItem(
                firstLocation, secondLocation, 2.0d, 3.0d);
        DistributionPlanItem secondLocationLine = distributionPlanItem(
                secondLocation, thirdLocation, 2.0d, 0.0d);
        DistributionPlanItem zeroFlowLine = distributionPlanItem(
                thirdLocation, firstLocation, 0.0d, 0.0d);
        Mockito.when(repository.customFindBySupplyPlanId(72L)).thenReturn(List.of(
                firstOutboundLine,
                secondOutboundLine,
                secondLocationLine,
                zeroFlowLine));

        CommunityMaterialFlowsDTO result = service.getMaterialFlows(72L);

        Assertions.assertEquals(List.of("LOC-A", "LOC-B", "LOC-C"),
                result.locationAndColorList.stream().map(location -> location.location()).toList());
        Assertions.assertEquals(List.of("#b2182b", "#d6604d", "#f4a582"),
                result.locationAndColorList.stream().map(location -> location.color()).toList());
        Assertions.assertEquals(List.of(
                        List.of(0.0d, 10.0d, 0.0d),
                        List.of(0.0d, 0.0d, 2.0d),
                        List.of(0.0d, 0.0d, 0.0d)),
                result.flowData);
        Mockito.verify(repository).customFindBySupplyPlanId(72L);
        Mockito.verifyNoMoreInteractions(repository);

    }

    @Test
    void shouldReturnEmptyLegacyCollectionsWhenSupplyPlanHasNoDistributionLines() {

        DistributionPlanItemRepository repository = Mockito.mock(DistributionPlanItemRepository.class);
        CommunityMaterialFlowsService service = new CommunityMaterialFlowsService();
        ReflectionTestUtils.setField(service, "distributionPlanItemRepository", repository);
        Mockito.when(repository.customFindBySupplyPlanId(9L)).thenReturn(List.of());

        CommunityMaterialFlowsDTO result = service.getMaterialFlows(9L);

        Assertions.assertTrue(result.locationAndColorList.isEmpty());
        Assertions.assertTrue(result.flowData.isEmpty());
        Mockito.verify(repository).customFindBySupplyPlanId(9L);
        Mockito.verifyNoMoreInteractions(repository);

    }

    private Location location(String locationId) {

        Location location = Mockito.mock(Location.class);
        Mockito.when(location.getId()).thenReturn(locationId);
        return location;

    }

    private DistributionPlanItem distributionPlanItem(
            Location originLocation,
            Location destinationLocation,
            double unrestrictedPlannedQuantity,
            double unrestrictedFirmQuantity) {

        DistributionPlanItem distributionPlanItem = Mockito.mock(DistributionPlanItem.class);
        Mockito.when(distributionPlanItem.getLocationOrigem()).thenReturn(originLocation);
        Mockito.when(distributionPlanItem.getLocationDestino()).thenReturn(destinationLocation);
        Mockito.when(distributionPlanItem.getQuantidadeOrdemPlanejadaIrrestrita())
                .thenReturn(unrestrictedPlannedQuantity);
        Mockito.when(distributionPlanItem.getQuantidadeOrdemFirmeIrrestrita())
                .thenReturn(unrestrictedFirmQuantity);
        return distributionPlanItem;

    }
}
