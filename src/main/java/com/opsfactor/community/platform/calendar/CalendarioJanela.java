package com.opsfactor.community.platform.calendar;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Visão interna de horizonte/presente, não um novo tipo de perfil. Delega lookup
 * à grade original para não substituir por bucket-base a parte da receita que
 * fica além do horizonte de uma location. A opção passada desloca as posições
 * para manter passado em 0 e presente em 1, como os consumidores legados.
 */
final class CalendarioJanela implements Calendario {

    private final Calendario calendario;
    private final int numeroPeriodosFuturos;
    private final int deslocamento;

    CalendarioJanela(Calendario calendario, int numeroPeriodosFuturos, boolean incluirPeriodoPassado) {

        if (numeroPeriodosFuturos <= 0 || numeroPeriodosFuturos > calendario.getNumeroPeriodosFuturos()) {
            throw new IllegalArgumentException("Horizonte da janela deve estar entre 1 e o horizonte do calendário");
        }
        this.calendario = calendario;
        this.numeroPeriodosFuturos = numeroPeriodosFuturos;
        this.deslocamento = incluirPeriodoPassado && calendario.getNumeroPeriodosPassadosTotal() == 0 ? 1 : 0;

    }

    @Override
    public PeriodoCalendario getPeriodo(int posicaoPeriodo) {

        PeriodoCalendario periodo = calendario.getPeriodo(posicaoPeriodo - deslocamento);
        return new PeriodoCalendario(posicaoPeriodo, periodo.dataHorarioInicial(),
                periodo.dataHorarioFinal(), periodo.tamanhoBucketNominal());

    }

    @Override
    public Integer getPosicaoPeriodo(LocalDateTime dataHorario) {

        return calendario.getPosicaoPeriodo(dataHorario) + deslocamento;

    }

    @Override
    public Integer getPosicaoPeriodoInicial() {

        return calendario.getPosicaoPeriodoInicial();

    }

    @Override
    public Integer getPosicaoPeriodoPresente() {

        return calendario.getPosicaoPeriodoPresente() + deslocamento;

    }

    @Override
    public Integer getPosicaoPeriodoFinalFuturo() {

        return getPosicaoPeriodoPresente() + numeroPeriodosFuturos - 1;

    }

    @Override
    public Integer getNumeroPeriodosPassadosAdicional() {

        return calendario.getNumeroPeriodosPassadosAdicional();

    }

    @Override
    public Integer getNumeroPeriodosPassados() {

        return calendario.getNumeroPeriodosPassados() + deslocamento;

    }

    @Override
    public Integer getNumeroPeriodosFuturos() {

        return numeroPeriodosFuturos;

    }

    @Override
    public Integer getNumeroPeriodosFuturosAdicional() {

        return 0;

    }

    @Override
    public LocalDateTime getDataHorarioReferencia() {

        return calendario.getDataHorarioReferencia();

    }

    /** Mantém a aritmética simples se a visão apenas limita/reindexa calendário uniforme. */
    @Override
    public double converteDiasParaPeriodosCalendario(double numeroDias, int posicaoPeriodoReferencia) {

        return calendario.converteDiasParaPeriodosCalendario(numeroDias, posicaoPeriodoReferencia - deslocamento);

    }

    @Override
    public double getNumeroPeriodosNoAno(int posicaoPeriodo) {

        return calendario.getNumeroPeriodosNoAno(posicaoPeriodo - deslocamento);

    }

    /** Recorta faixas existentes; o passado opcional é uma faixa-base independente. */
    @Override
    public List<IntervaloExtracaoCalendario> getIntervalosExtracao() {

        List<IntervaloExtracaoCalendario> intervalos = new ArrayList<>();
        if (deslocamento != 0) {
            PeriodoCalendario passado = getPeriodo(0);
            intervalos.add(new IntervaloExtracaoCalendario(passado.dataHorarioInicial(),
                    passado.dataHorarioFinal(), passado.tamanhoBucketNominal()));
        }
        for (IntervaloExtracaoCalendario intervalo : calendario.getIntervalosExtracao()) {
            LocalDateTime inicio = intervalo.dataHorarioInicial().isAfter(getDataHorarioInicial())
                    ? intervalo.dataHorarioInicial() : getDataHorarioInicial();
            LocalDateTime fim = intervalo.dataHorarioFinal().isBefore(getDataHorarioFinal())
                    ? intervalo.dataHorarioFinal() : getDataHorarioFinal();
            if (!inicio.isAfter(fim)) {
                intervalos.add(new IntervaloExtracaoCalendario(inicio, fim, intervalo.tamanhoBucket()));
            }
        }
        return List.copyOf(intervalos);

    }
}
