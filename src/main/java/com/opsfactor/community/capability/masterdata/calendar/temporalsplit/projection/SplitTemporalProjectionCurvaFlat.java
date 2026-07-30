package com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection;

import com.opsfactor.community.platform.calendar.Calendario;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Year;

/**
 * Curva temporal flat usada como padrao no Community.
 *
 * <p>Cada dia recebe a mesma participacao dentro do ano correspondente. A
 * distribuicao para buckets menores que dia divide essa participacao de forma
 * uniforme nas fracoes do dia.</p>
 */
@Getter
public class SplitTemporalProjectionCurvaFlat extends SplitTemporalProjectionCurva {

    // CONSTRUTOR
    public SplitTemporalProjectionCurvaFlat(
            Calendario calendarioOrigem,
            Calendario calendarioTarget) {
        super(calendarioOrigem, calendarioTarget);
        inicializaSplitTemporalProjectionCurva();
    }

    @Override
    protected void atualizaParticipacaoDataNoAno() {

        LocalDate dataInicialCalendarioOrigem = calendarioOrigem.getDataHorarioInicial().toLocalDate().plusDays(0);
        LocalDate dataFinalCalendarioOrigem = calendarioOrigem.getDataHorarioFinal().toLocalDate().plusDays(0);

        LocalDate dataInicialCalendarioTarget = calendarioTarget.getDataHorarioInicial().toLocalDate().plusDays(0);
        LocalDate dataFinalCalendarioTarget = calendarioTarget.getDataHorarioFinal().toLocalDate().plusDays(0);

        LocalDate dataAtual = Calendario.getMinData(dataInicialCalendarioOrigem, dataInicialCalendarioTarget);
        LocalDate dataFinal = Calendario.getMaxData(dataFinalCalendarioOrigem, dataFinalCalendarioTarget);

        while (dataAtual.isBefore(dataFinal.plusDays(1))) {

            Year ano = Year.of(dataAtual.getYear());
            int numeroDiasAno = ano.length();

            double participacaoDiaNoAno = 1.0 / numeroDiasAno;

            getParticipacaoDataNoAno().put(dataAtual, participacaoDiaNoAno);

            dataAtual = dataAtual.plusDays(1);
        }

    }


}
