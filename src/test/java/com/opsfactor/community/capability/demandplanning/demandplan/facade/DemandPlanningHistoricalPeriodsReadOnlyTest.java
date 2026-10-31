package com.opsfactor.community.capability.demandplanning.demandplan.facade;

import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Historical display values never become Planning Book write commands. */
class DemandPlanningHistoricalPeriodsReadOnlyTest {

    private final DemandPlanningFacade facade = new DemandPlanningFacade();

    @Test
    void rejectsHistoryEvenWhenTheRequestedForecastEditComesFirst() {

        Calendario calendario = calendarioMensal();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> facade.validaPeriodosEditaveisPlanningBook(
                        List.of(cell(LocalDate.of(2020, 10, 31)), cell(LocalDate.of(2020, 9, 30))), calendario));
        assertEquals("Historical sales periods are read-only in the Demand Planning Book.", error.getMessage());

    }

    @Test
    void allowsForecastByThePlanCalendarEvenWhenItsDatesAreBeforeToday() {

        // Fixed old dates ensure the guard cannot accidentally use LocalDate.now().
        assertDoesNotThrow(() -> facade.validaPeriodosEditaveisPlanningBook(
                List.of(cell(LocalDate.of(2020, 10, 1)), cell(LocalDate.of(2020, 10, 31)),
                        cell(LocalDate.of(2020, 11, 30))), calendarioMensal()));

    }

    @Test
    void usesTheEffectiveCalendarBoundaryInsteadOfRebuildingAMonth() {

        Calendario calendario = Mockito.mock(Calendario.class);
        Mockito.when(calendario.getPosicaoPeriodoPresente()).thenReturn(3);
        Mockito.when(calendario.getPrimeiraDataHorarioPeriodo(3))
                .thenReturn(LocalDateTime.of(2020, 10, 15, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> facade.validaPeriodosEditaveisPlanningBook(
                List.of(cell(LocalDate.of(2020, 10, 14))), calendario));
        assertDoesNotThrow(() -> facade.validaPeriodosEditaveisPlanningBook(
                List.of(cell(LocalDate.of(2020, 10, 15))), calendario));

    }

    @Test
    void rejectsAMissingPeriodExplicitly() {

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> facade.validaPeriodosEditaveisPlanningBook(List.of(new SelectedPlanningBookCellDTO()), calendarioMensal()));
        assertEquals("Planning Book selected cell period is required.", error.getMessage());

    }

    private static Calendario calendarioMensal() {

        return CalendarioSimples.criaCalendarioDeOffsetsPeriodos(
                Constantes.TamanhoBucket.MENSAL, LocalDateTime.of(2020, 10, 1, 0, 0), 0, 2, 3, 0);

    }

    private static SelectedPlanningBookCellDTO cell(LocalDate period) {

        SelectedPlanningBookCellDTO selectedCell = new SelectedPlanningBookCellDTO();
        selectedCell.period = period;
        return selectedCell;

    }
}
