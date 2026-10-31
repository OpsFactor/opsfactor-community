package com.opsfactor.community.capability.planningbook.facade;

import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.planningbook.facade.dto.ColumnDefDTO;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The wire metadata must prohibit history editing even for editable forecast rows. */
class PlanningBookHistoricalColumnsReadOnlyTest {

    @Test
    void publishesHistoryAndAverageAsReadOnlyWhilePreservingForecastColumns() {

        ConfiguredView view = Mockito.mock(ConfiguredView.class);
        Mockito.when(view.getExibeVendaMediaHistorica()).thenReturn(true);

        ConfiguredViewProjection projection = Mockito.mock(ConfiguredViewProjection.class);
        Mockito.when(projection.getConfiguredView()).thenReturn(view);
        CalendarioSimples calendar = CalendarioSimples.criaCalendarioDeOffsetsPeriodos(
                Constantes.TamanhoBucket.MENSAL, LocalDateTime.of(2020, 10, 1, 0, 0), 0, 2, 3, 0);
        List<ColumnDefDTO> columns = ReflectionTestUtils.invokeMethod(
                new PlanningBookService(), "getColumnDefDTOList", calendar, projection);

        List<ColumnDefDTO> history = columns.stream()
                .filter(column -> "pastPeriods".equals(column.cellClass)).toList();
        assertEquals(3, history.size(), "Two historical months and their computed average");
        assertTrue(history.stream().allMatch(column -> Boolean.FALSE.equals(column.enableCellEdit)));
        assertTrue(history.stream().anyMatch(column -> "Average Historical Sales".equals(column.field)));
        List<ColumnDefDTO> forecast = columns.stream().filter(column -> Boolean.TRUE.equals(column.dataColumn)
                && !"pastPeriods".equals(column.cellClass)).toList();
        assertEquals(3, forecast.size());
        assertTrue(forecast.stream().allMatch(column -> Boolean.TRUE.equals(column.enableCellEdit)));

    }
}
