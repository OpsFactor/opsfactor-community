package com.opsfactor.community.platform.calendar;

import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

/**
 * Contrato temporal compartilhado por calendários uniformes e mistos.
 *
 * <p>As posições identificam janelas reais, não uma unidade de tempo constante.
 * Consultas fora do horizonte são permitidas e não materializam novos períodos.
 * Frequência estatística, formatter global e bucket global continuam exclusivos
 * de CalendarioSimples. Implementações antigas de métodos permanecem nele para
 * preservar exatamente sua aritmética; os defaults abaixo usam interseções reais.</p>
 */
public interface Calendario {

    /** Janela da posição, inclusive para uma extrapolação pontual fora da grade. */
    PeriodoCalendario getPeriodo(int posicaoPeriodo);

    /** Traduz uma data em posição global, sem ampliar o horizonte ativo. */
    Integer getPosicaoPeriodo(LocalDateTime dataHorario);

    Integer getPosicaoPeriodoInicial();
    Integer getPosicaoPeriodoPresente();
    Integer getPosicaoPeriodoFinalFuturo();
    Integer getNumeroPeriodosPassadosAdicional();
    Integer getNumeroPeriodosPassados();
    Integer getNumeroPeriodosFuturos();
    Integer getNumeroPeriodosFuturosAdicional();
    LocalDateTime getDataHorarioReferencia();

    /** Uma faixa por item configurado; itens adjacentes iguais não são unidos. */
    default List<IntervaloExtracaoCalendario> getIntervalosExtracao() {

        return List.of(new IntervaloExtracaoCalendario(getDataHorarioInicial(), getDataHorarioFinal(),
                getTamanhoBucket(getPosicaoPeriodoInicial())));

    }

    default TamanhoBucket getTamanhoBucket(int posicaoPeriodo) {

        return getPeriodo(posicaoPeriodo).tamanhoBucketNominal();

    }

    /** Visão local: conserva a receita inteira para lookup, mas não planeja além do limite. */
    default Calendario comHorizonteFuturo(int numeroPeriodosFuturos) {

        return new CalendarioJanela(this, numeroPeriodosFuturos, false);

    }

    /** Acrescenta uma posição passada e desloca o presente, seguindo a indexação legada. */
    default Calendario comPeriodoPassado() {

        return new CalendarioJanela(this, getNumeroPeriodosFuturos(), true);

    }

    default Integer getPosicaoPeriodoInicialPassadoAdicional() {

        return getNumeroPeriodosPassadosAdicional() == 0 ? null : getPosicaoPeriodoInicial();

    }

    default Integer getPosicaoPeriodoFinalPassadoAdicional() {

        return getNumeroPeriodosPassadosAdicional() == 0 ? null
                : getPosicaoPeriodoInicial() + getNumeroPeriodosPassadosAdicional() - 1;

    }

    default Integer getPosicaoPeriodoInicialPassado() {

        return getNumeroPeriodosPassados() == 0 ? null
                : getPosicaoPeriodoPresente() - getNumeroPeriodosPassados();

    }

    default Integer getPosicaoPeriodoFinalPassado() {

        return getNumeroPeriodosPassados() == 0 ? null : getPosicaoPeriodoPresente() - 1;

    }

    default Integer getPosicaoPeriodoInicialFuturo() {

        return getPosicaoPeriodoPresente();

    }

    default Integer getPosicaoPeriodoInicialFuturoAdicional() {

        return getNumeroPeriodosFuturosAdicional() == 0 ? null : getPosicaoPeriodoFinalFuturo() + 1;

    }

    default Integer getPosicaoPeriodoFinalFuturoAdicional() {

        return getNumeroPeriodosFuturosAdicional() == 0 ? null
                : getPosicaoPeriodoFinalFuturo() + getNumeroPeriodosFuturosAdicional();

    }

    default LocalDateTime getDataHorarioInicial() {

        return getPrimeiraDataHorarioPeriodo(getPosicaoPeriodoInicial());

    }

    default LocalDateTime getDataHorarioFinal() {

        return getUltimaDataHorarioPeriodo(getPosicaoPeriodoInicial() + getNumeroPeriodosTotais() - 1);

    }

    default LocalDateTime getDataHorarioInicialPassadaAdicional() {

        Integer posicaoPeriodo = getPosicaoPeriodoInicialPassadoAdicional();
        return posicaoPeriodo == null ? null : getPrimeiraDataHorarioPeriodo(posicaoPeriodo);

    }

    default LocalDateTime getDataHorarioFinalPassadaAdicional() {

        Integer posicaoPeriodo = getPosicaoPeriodoFinalPassadoAdicional();
        return posicaoPeriodo == null ? null : getUltimaDataHorarioPeriodo(posicaoPeriodo);

    }

    default LocalDateTime getDataHorarioInicialPassada() {

        Integer posicaoPeriodo = getPosicaoPeriodoInicialPassado();
        return posicaoPeriodo == null ? null : getPrimeiraDataHorarioPeriodo(posicaoPeriodo);

    }

    default LocalDateTime getDataHorarioFinalPassada() {

        Integer posicaoPeriodo = getPosicaoPeriodoFinalPassado();
        return posicaoPeriodo == null ? null : getUltimaDataHorarioPeriodo(posicaoPeriodo);

    }

    default LocalDateTime getDataHorarioInicialFutura() {

        Integer posicaoPeriodo = getPosicaoPeriodoInicialFuturo();
        return posicaoPeriodo == null ? null : getPrimeiraDataHorarioPeriodo(posicaoPeriodo);

    }

    default LocalDateTime getDataHorarioFinalFutura() {

        Integer posicaoPeriodo = getPosicaoPeriodoFinalFuturo();
        return posicaoPeriodo == null ? null : getUltimaDataHorarioPeriodo(posicaoPeriodo);

    }

    default LocalDateTime getDataHorarioInicialFuturaAdicional() {

        Integer posicaoPeriodo = getPosicaoPeriodoInicialFuturoAdicional();
        return posicaoPeriodo == null ? null : getPrimeiraDataHorarioPeriodo(posicaoPeriodo);

    }

    default LocalDateTime getDataHorarioFinalFuturaAdicional() {

        Integer posicaoPeriodo = getPosicaoPeriodoFinalFuturoAdicional();
        return posicaoPeriodo == null ? null : getUltimaDataHorarioPeriodo(posicaoPeriodo);

    }

    default LocalDateTime getDataHorarioInicialPresente() {

        return getPrimeiraDataHorarioPeriodo(getPosicaoPeriodoPresente());

    }

    default LocalDateTime getDataHorarioFinalPresente() {

        return getUltimaDataHorarioPeriodo(getPosicaoPeriodoPresente());

    }

    default int getNumeroPeriodosPassadosTotal() {

        return getNumeroPeriodosPassadosAdicional() + getNumeroPeriodosPassados();

    }

    default int getNumeroPeriodosTotais() {

        return getNumeroPeriodosPassadosTotal() + getNumeroPeriodosFuturos() + getNumeroPeriodosFuturosAdicional();

    }

    default List<Integer> getListaPosicoesPeriodo() {

        return IntStream.range(getPosicaoPeriodoInicial(), getPosicaoPeriodoInicial() + getNumeroPeriodosTotais())
                .boxed().toList();

    }

    default List<PeriodoCalendario> getPeriodos() {

        return getListaPosicoesPeriodo().stream().map(this::getPeriodo).toList();

    }

    default List<LocalDateTime> getListaDatasHorarios() {

        return getListaPosicoesPeriodo().stream().map(this::getPrimeiraDataHorarioPeriodo).toList();

    }

    default Map<LocalDateTime, Integer> getMapaDatasHorarios() {

        Map<LocalDateTime, Integer> mapaDatasHorarios = new LinkedHashMap<>();
        getListaPosicoesPeriodo().forEach(posicao ->
                mapaDatasHorarios.put(getPrimeiraDataHorarioPeriodo(posicao), posicao));
        return Collections.unmodifiableMap(mapaDatasHorarios);

    }

    default List<LocalDateTime> getListDataHorariosFinaisPorPeriodo() {

        return getListaPosicoesPeriodo().stream().map(this::getUltimaDataHorarioPeriodo).toList();

    }

    default LocalDateTime getPrimeiraDataHorarioPeriodo(int posicaoPeriodo) {

        return getPeriodo(posicaoPeriodo).dataHorarioInicial();

    }

    default LocalDateTime getUltimaDataHorarioPeriodo(int posicaoPeriodo) {

        return getPeriodo(posicaoPeriodo).dataHorarioFinal();

    }

    default LocalDateTime getUltimoSegundoPeriodo(int posicaoPeriodo) {

        return getUltimaDataHorarioPeriodo(posicaoPeriodo);

    }

    default Integer getPosicaoPeriodo(LocalDate data) {

        return getPosicaoPeriodo(data.atStartOfDay());

    }

    default LocalDate getPrimeiraDataPeriodo(int posicaoPeriodo) {

        return getPrimeiraDataHorarioPeriodo(posicaoPeriodo).toLocalDate();

    }

    default LocalDate getUltimaDataPeriodo(int posicaoPeriodo) {

        return getUltimaDataHorarioPeriodo(posicaoPeriodo).toLocalDate();

    }

    default LocalDateTime getPrimeiraDataHorarioPeriodo(LocalDateTime dataReferencia) {

        return getPrimeiraDataHorarioPeriodo(getPosicaoPeriodo(dataReferencia));

    }

    default LocalDateTime getUltimaDataHorarioPeriodo(LocalDateTime dataReferencia) {

        return getUltimaDataHorarioPeriodo(getPosicaoPeriodo(dataReferencia));

    }

    default LocalDateTime getUltimaDataHorarioPeriodo(LocalDate dataReferencia) {

        return getUltimaDataHorarioPeriodo(getPosicaoPeriodo(dataReferencia));

    }

    default LocalDateTime getPrimeiroHorarioDiaInicialPeriodo(int posicaoPeriodo) {

        return getPrimeiraDataPeriodo(posicaoPeriodo).atStartOfDay();

    }

    default LocalDateTime getPrimeiroHorarioDiaFinalPeriodo(int posicaoPeriodo) {

        return getUltimaDataPeriodo(posicaoPeriodo).atStartOfDay();

    }

    default boolean contemData(LocalDateTime dataHorario) {

        return !dataHorario.isBefore(getDataHorarioInicial())
                && dataHorario.isBefore(getDataHorarioFinal().plusSeconds(1));

    }

    default boolean contemData(LocalDate data) {

        return contemData(data.atStartOfDay());

    }

    default boolean contemDataNoPeriodo(LocalDateTime dataHorario, int posicaoPeriodo) {

        return getPeriodo(posicaoPeriodo).contem(dataHorario);

    }

    default int getNumeroPeriodosEntreDatas(LocalDate dataInicial, LocalDate dataFinal) {

        return getPosicaoPeriodo(dataFinal) - getPosicaoPeriodo(dataInicial) + 1;

    }

    default double getNumeroDiasNoPeriodo(int posicaoPeriodo) {

        return getPeriodo(posicaoPeriodo).numeroDias();

    }

    default double getNumeroHorasNoPeriodo(int posicaoPeriodo) {

        return getNumeroDiasNoPeriodo(posicaoPeriodo) * 24d;

    }

    default double getNumeroDiasEntrePeriodos(int posicaoPeriodoInicial, int posicaoPeriodoFinal) {

        return IntStream.rangeClosed(posicaoPeriodoInicial, posicaoPeriodoFinal)
                .mapToDouble(this::getNumeroDiasNoPeriodo).sum();

    }

    default double getNumeroDiasEmPeriodosFuturos() {

        return getNumeroDiasEntrePeriodos(getPosicaoPeriodoPresente(), getPosicaoPeriodoFinalFuturo());

    }

    default double getNumeroDiasEmPeriodosPassados() {

        return getNumeroDiasEntrePeriodos(getPosicaoPeriodoInicial(), getPosicaoPeriodoPresente() - 1);

    }

    /**
     * Conta frações de janelas a partir do INÍCIO da posição de referência.
     * Ao cruzar diário/semanal/mensal, usa a duração de cada janela atravessada.
     * Valores negativos percorrem os períodos anteriores, sem alterar a grade.
     */
    default double converteDiasParaPeriodosCalendario(double numeroDias, int posicaoPeriodoReferencia) {

        if (!Double.isFinite(numeroDias)) {
            throw new IllegalArgumentException("Quantidade de dias deve ser finita");
        }
        double diasRestantes = Math.abs(numeroDias);
        int direcao = numeroDias < 0 ? -1 : 1;
        int posicaoPeriodo = numeroDias < 0 ? posicaoPeriodoReferencia - 1 : posicaoPeriodoReferencia;
        double quantidadePeriodos = 0d;
        while (diasRestantes > 0d) {
            double diasPeriodo = getNumeroDiasNoPeriodo(posicaoPeriodo);
            double diasConsiderados = Math.min(diasRestantes, diasPeriodo);
            quantidadePeriodos += diasConsiderados / diasPeriodo;
            diasRestantes -= diasConsiderados;
            posicaoPeriodo += direcao;
        }
        return direcao * quantidadePeriodos;

    }

    /** Fator nominal histórico; uma semana técnica de 3 dias recebe 3/7 da taxa semanal. */
    default double getNumeroPeriodosNoAno(int posicaoPeriodo) {

        TamanhoBucket tamanhoBucket = getTamanhoBucket(posicaoPeriodo);
        double numeroPeriodosNominal = switch (tamanhoBucket) {
            case ANUAL -> 1d;
            case MENSAL -> 12d;
            case SEMANAL -> 52.1775d;
            case DIARIO -> 365.25d;
            case HORARIO -> 365.25d * 24d;
            default -> throw new UnsupportedOperationException("Fator financeiro não implementado para " + tamanhoBucket);
        };
        LocalDateTime inicioNominal = CalendarioSimples.getPrimeiraDataHorarioPeriodo(
                getPrimeiraDataHorarioPeriodo(posicaoPeriodo), tamanhoBucket);
        LocalDateTime fimNominal = CalendarioSimples.getUltimaDataHorarioPeriodo(inicioNominal, tamanhoBucket);
        double diasNominais = (Duration.between(inicioNominal, fimNominal).getSeconds() + 1d) / 86400d;
        return numeroPeriodosNominal * diasNominais / getNumeroDiasNoPeriodo(posicaoPeriodo);

    }

    default int getPosicaoPeriodoAposOffsetDoInicioPeriodoReferencia(int periodoReferencia,
            int offsetPeriodos, TamanhoBucket tamanhoBucketOffset) {

        LocalDateTime dataReferencia = getPrimeiraDataHorarioPeriodo(periodoReferencia);
        return getPosicaoPeriodo(CalendarioSimples.getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                dataReferencia, offsetPeriodos, tamanhoBucketOffset));

    }

    default int getPosicaoPeriodoAposOffsetDoFimPeriodoReferencia(int periodoReferencia,
            int offsetPeriodos, TamanhoBucket tamanhoBucketOffset) {

        LocalDateTime dataReferencia = getUltimaDataHorarioPeriodo(periodoReferencia);
        return getPosicaoPeriodo(CalendarioSimples.getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                dataReferencia, offsetPeriodos, tamanhoBucketOffset));

    }

    default int getPosicaoPeriodoDePosicaoPeriodoOutroCalendario(Calendario outroCalendario,
            int posicaoPeriodoOutroCalendario) {

        return getPosicaoPeriodo(outroCalendario.getUltimaDataHorarioPeriodo(posicaoPeriodoOutroCalendario));

    }

    default int getUltimoPeriodoFuturoEmHorizontePeriodos(int horizontePeriodos, TamanhoBucket tamanhoBucket) {

        return getPosicaoPeriodoAposOffsetDoFimPeriodoReferencia(
                getPosicaoPeriodoPresente() - 1, horizontePeriodos, tamanhoBucket);

    }

    default String getDescricaoPeriodoDePosicaoPeriodo(int posicaoPeriodo) {

        return CalendarioSimples.getDescricaoPeriodoDeLocalDateTime(
                getUltimaDataHorarioPeriodo(posicaoPeriodo), getTamanhoBucket(posicaoPeriodo));

    }

    default int getDescricaoIntegerPeriodo(int posicaoPeriodo) {

        return CalendarioSimples.getDescricaoIntegerPeriodo(
                getPrimeiraDataHorarioPeriodo(posicaoPeriodo), getTamanhoBucket(posicaoPeriodo));

    }

    /** Mesma apresentação legada, avaliada pelo bucket nominal da posição. */
    default List<Integer> getAgregadorPeriodo(int posicaoPeriodo) {

        LocalDateTime dataReferencia = getPrimeiraDataHorarioPeriodo(posicaoPeriodo);
        return switch (getTamanhoBucket(posicaoPeriodo)) {
            case ANUAL -> List.of(dataReferencia.getYear());
            case MENSAL -> List.of(dataReferencia.getYear(), dataReferencia.getMonthValue());
            case SEMANAL -> List.of(dataReferencia.getYear(), CalendarioSimples.getSemanaWW(dataReferencia));
            case DIARIO -> List.of(CalendarioSimples.getSemanaYYYYWW(dataReferencia),
                    dataReferencia.getDayOfWeek().getValue());
            case TURNO -> List.of(CalendarioSimples.getDataYYYYMMDD(dataReferencia.toLocalDate()),
                    Math.floorDiv(dataReferencia.getHour(), 8) + 1);
            case HORARIO -> List.of(CalendarioSimples.getDataYYYYMMDD(dataReferencia.toLocalDate()), dataReferencia.getHour());
            default -> throw new UnsupportedOperationException("Agrupamento não implementado para " + getTamanhoBucket(posicaoPeriodo));
        };

    }

    default List<Integer> getListaAgrupadoresPeriodo(int posicaoAgregacao) {

        return getListaPosicoesPeriodo().stream().map(posicao -> getAgregadorPeriodo(posicao).get(posicaoAgregacao)).toList();

    }

    default List<Integer> getListaAgrupadoresPeriodoSemDuplicatasOrdenado(int posicaoAgregacao) {

        return getListaAgrupadoresPeriodo(posicaoAgregacao).stream().distinct().sorted().toList();

    }

    /**
     * Soma a fração de cada bucket de referência interceptado pela janela real.
     * Ex.: semana técnica 01–03 num mês de 30 dias representa 3/30 de um mês.
     */
    default double getNumeroPeriodosNoBucketReferencia(int posicaoPeriodo, TamanhoBucket tamanhoBucketReferencia) {

        return consolidaDadosNoCalendario(posicaoPeriodo, tamanhoBucketReferencia, dataHorario -> 1d);

    }

    /**
     * Consolida valores de buckets originais usando interseção inclusiva.
     * O denominador é sempre a janela INTEGRAL do dado: 300 num mês de 30 dias,
     * consultado do dia 16 ao 30, resulta 150, nunca renormalização para 300.
     */
    default double consolidaDadosNoCalendario(int posicaoPeriodoCalendario, TamanhoBucket tamanhoBucketDados,
            ToDoubleFunction<LocalDateTime> valorPorDataHorarioDados) {

        LocalDateTime inicioPeriodo = getPrimeiraDataHorarioPeriodo(posicaoPeriodoCalendario);
        LocalDateTime fimPeriodo = getUltimaDataHorarioPeriodo(posicaoPeriodoCalendario);
        LocalDateTime inicioDados = CalendarioSimples.getPrimeiraDataHorarioPeriodo(inicioPeriodo, tamanhoBucketDados);
        double valorAcumulado = 0d;
        while (!inicioDados.isAfter(fimPeriodo)) {
            LocalDateTime fimDados = CalendarioSimples.getUltimaDataHorarioPeriodo(inicioDados, tamanhoBucketDados);
            LocalDateTime inicioIntersecao = inicioDados.isAfter(inicioPeriodo) ? inicioDados : inicioPeriodo;
            LocalDateTime fimIntersecao = fimDados.isBefore(fimPeriodo) ? fimDados : fimPeriodo;
            double segundosIntersecao = Duration.between(inicioIntersecao, fimIntersecao).getSeconds() + 1d;
            double segundosDados = Duration.between(inicioDados, fimDados).getSeconds() + 1d;
            valorAcumulado += valorPorDataHorarioDados.applyAsDouble(inicioDados) * segundosIntersecao / segundosDados;
            inicioDados = fimDados.plusSeconds(1);
        }
        return valorAcumulado;

    }

    default double getPercentualDoPeriodoCobertoPeloRange(int posicaoPeriodo,
            LocalDateTime dataHorarioInicialReferencia, LocalDateTime dataHorarioFinalReferencia) {

        PeriodoCalendario periodo = getPeriodo(posicaoPeriodo);
        LocalDateTime inicio = periodo.dataHorarioInicial().isAfter(dataHorarioInicialReferencia)
                ? periodo.dataHorarioInicial() : dataHorarioInicialReferencia;
        LocalDateTime fim = periodo.dataHorarioFinal().isBefore(dataHorarioFinalReferencia)
                ? periodo.dataHorarioFinal() : dataHorarioFinalReferencia;
        if (inicio.isAfter(fim)) {
            return 0d;
        }
        return (Duration.between(inicio, fim).getSeconds() + 1d) / (periodo.numeroDias() * 86400d);

    }

    default Map<Integer, Double> getNumeroHorasPorPeriodoDeRangeDatasHorarios(
            LocalDateTime dataHorarioInicial, LocalDateTime dataHorarioFinal) {

        Map<Integer, Double> horasPorPeriodo = new LinkedHashMap<>();
        for (int posicao = getPosicaoPeriodo(dataHorarioInicial); posicao <= getPosicaoPeriodo(dataHorarioFinal); posicao++) {
            double horas = getPercentualDoPeriodoCobertoPeloRange(posicao, dataHorarioInicial, dataHorarioFinal)
                    * getNumeroHorasNoPeriodo(posicao);
            if (horas > 0d) {
                horasPorPeriodo.put(posicao, horas);
            }
        }
        return horasPorPeriodo;

    }

    default List<LocalDate> getDatasEmPeriodo(int posicaoPeriodo) {

        return getPrimeiraDataPeriodo(posicaoPeriodo).datesUntil(getUltimaDataPeriodo(posicaoPeriodo).plusDays(1)).toList();

    }
}
