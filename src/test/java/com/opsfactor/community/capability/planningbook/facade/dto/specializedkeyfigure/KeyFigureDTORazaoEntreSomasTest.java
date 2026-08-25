package com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure;

import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureRelacaoEntreValores;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyFigureDTORazaoEntreSomasTest {

    @Test
    void aggregatesAveragePriceAsRatioOfMonetaryAndQuantitySums() {

        Calendario calendario = Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0));
        LocalDateTime data = calendario.getUltimaDataHorarioPeriodo(0);
        KeyFigureDTORazaoEntreSomas primeiraFolha = criaPrecoMedio(
                calendario,
                data,
                100.0d,
                10.0d);
        KeyFigureDTORazaoEntreSomas segundaFolha = criaPrecoMedio(
                calendario,
                data,
                30.0d,
                2.0d);

        KeyFigureDTORazaoEntreSomas precoMedioAgregado =
                new KeyFigureDTORazaoEntreSomas("Gross Average Price", EditMode.NOEDIT);
        precoMedioAgregado.incorporaValoresDeKeyFigure(primeiraFolha);
        precoMedioAgregado.incorporaValoresDeKeyFigure(segundaFolha);

        assertEquals(
                130.0d / 12.0d,
                precoMedioAgregado.getValues().get(data.toString()),
                0.00001d);

    }

    private KeyFigureDTORazaoEntreSomas criaPrecoMedio(
            Calendario calendario,
            LocalDateTime data,
            double valorMonetario,
            double quantidade) {

        KeyFigureDTORazaoEntreSomas precoMedio =
                new KeyFigureDTORazaoEntreSomas("Gross Average Price", EditMode.NOEDIT);
        precoMedio.importaDadosDFUDataKeyFigure(
                calendario,
                List.of(DFUDataKeyFigureRelacaoEntreValores.builder()
                        .data(data)
                        .numeratorValue(valorMonetario)
                        .denominatorValue(quantidade)
                        .build()));
        return precoMedio;

    }
}
