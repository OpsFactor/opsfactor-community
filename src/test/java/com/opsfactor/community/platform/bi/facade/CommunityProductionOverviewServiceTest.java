package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.platform.bi.service.CommunityProductionOverviewSnapshotFactory;
import com.opsfactor.community.platform.bi.service.CommunityProductionOverviewSnapshotFactory.CommunityProductionOverviewSnapshot;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewSelectionDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Garante que o service consome uma única fotografia e não reinicializa projections por location. */
class CommunityProductionOverviewServiceTest {

    @Test
    void shouldUseOneSnapshotAndPublishItsCalendarWhenNoEligibleLocationExists() {

        Calendario calendar = Mockito.mock(Calendario.class);
        List<LocalDateTime> expectedDates = List.of(LocalDateTime.of(2026, 7, 1, 0, 0));
        Mockito.when(calendar.getListaDatasHorarios()).thenReturn(expectedDates);
        CommunityProductionOverviewSnapshot snapshot = new CommunityProductionOverviewSnapshot(
                null, calendar, null, null, null, null, null, null, Set.of());
        CommunityProductionOverviewSnapshotFactory snapshotFactory = Mockito.mock(
                CommunityProductionOverviewSnapshotFactory.class);
        CommunityProductionOverviewSelectionDTO selection = new CommunityProductionOverviewSelectionDTO();
        Mockito.when(snapshotFactory.createSnapshot(selection)).thenReturn(snapshot);

        CommunityProductionOverviewService service = new CommunityProductionOverviewService();
        ReflectionTestUtils.setField(service, "productionOverviewSnapshotFactory", snapshotFactory);

        CommunityProductionOverviewDTO response = service.getProductionOverview(selection);

        Assertions.assertEquals(expectedDates, response.finalDateTimeByPeriod);
        Assertions.assertTrue(response.capacityByProductionResource.isEmpty());
        Mockito.verify(snapshotFactory).createSnapshot(selection);
        Mockito.verifyNoMoreInteractions(snapshotFactory);

    }
}
