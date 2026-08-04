package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.platform.bi.service.CommunityProductionOverviewProjectionLoader;
import com.opsfactor.community.platform.bi.service.CommunityProductionOverviewProjectionLoader.CommunityProductionOverviewProjectionContext;
import com.opsfactor.community.platform.bi.service.CommunitySupplyOverviewBaseFactory;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewSelectionDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunitySupplyOverviewBaseDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Garante que o service consome projections carregadas uma única vez. */
class CommunityProductionOverviewServiceTest {

    @Test
    void shouldUseOneProjectionContextAndPublishTheSharedBaseWhenNoEligibleLocationExists() {

        Calendario calendar = Mockito.mock(Calendario.class);
        List<LocalDateTime> expectedDates = List.of(LocalDateTime.of(2026, 7, 1, 0, 0));
        Mockito.when(calendar.getListaDatasHorarios()).thenReturn(expectedDates);
        SupplyNetworkProjection supplyNetworkProjection = Mockito.mock(SupplyNetworkProjection.class);
        CommunityProductionOverviewProjectionContext projectionContext = new CommunityProductionOverviewProjectionContext(
                null, calendar, null, null, null, supplyNetworkProjection, null, null, Set.of());
        CommunityProductionOverviewProjectionLoader projectionLoader = Mockito.mock(
                CommunityProductionOverviewProjectionLoader.class);
        CommunitySupplyOverviewBaseFactory baseFactory = Mockito.mock(CommunitySupplyOverviewBaseFactory.class);
        CommunityProductionOverviewSelectionDTO selection = new CommunityProductionOverviewSelectionDTO();
        Mockito.when(projectionLoader.load(selection)).thenReturn(projectionContext);
        CommunitySupplyOverviewBaseDTO base = new CommunitySupplyOverviewBaseDTO();
        base.finalDateTimeByPeriod.addAll(expectedDates);
        Mockito.when(baseFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(base);

        CommunityProductionOverviewService service = new CommunityProductionOverviewService();
        ReflectionTestUtils.setField(service, "productionOverviewProjectionLoader", projectionLoader);
        ReflectionTestUtils.setField(service, "supplyOverviewBaseFactory", baseFactory);

        CommunityProductionOverviewDTO response = service.getProductionOverview(selection);

        Assertions.assertEquals(expectedDates, response.finalDateTimeByPeriod);
        Assertions.assertTrue(response.capacityByProductionResource.isEmpty());
        Mockito.verify(projectionLoader).load(selection);
        Mockito.verify(baseFactory).create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

    }
}
