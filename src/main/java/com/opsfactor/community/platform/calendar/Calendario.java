package com.opsfactor.community.platform.calendar;

import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import lombok.Getter;
import org.threeten.extra.YearWeek;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

@Getter
public class Calendario {

    //|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //|        SemanasPassadasAdicionais      |       SemanasPassadas        |     SemanaPresente       |       SemanasFuturas        |      SemanasFuturasAdicionais        |
    //|   <-NumeroSemanasPassadasAdicional->  |  <-NumeroSemanasPassadas->   |                          |  <-NumeroSemanasFuturas->   |  <-NumeroSemanasFuturasAdicional->   |
    //| posicao/semanaFinalPassadaAdicional-> | posicao/semanaFinalPassada-> | posicao/semanaPresente-> | posicao/semanaFinalFutura-> | posicao/semanaFinalFuturaAdicional-> |
    //|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    
    TamanhoBucket tamanhoBucket;

    LocalDateTime dataHorarioReferencia;

    Integer posicaoPeriodoInicial = 0;
    Integer posicaoPeriodoPresente; // primeiro período futuro
    
    Integer posicaoPeriodoInicialPassadoAdicional;
    Integer posicaoPeriodoFinalPassadoAdicional;
    
    Integer posicaoPeriodoInicialPassado;
    Integer posicaoPeriodoFinalPassado;
    
    Integer posicaoPeriodoInicialFuturo;
    Integer posicaoPeriodoFinalFuturo;
    
    Integer posicaoPeriodoInicialFuturoAdicional;
    Integer posicaoPeriodoFinalFuturoAdicional;

    Integer numeroPeriodosPassadosAdicional = 0;
    Integer numeroPeriodosPassados = 0;
    Integer numeroPeriodosFuturos = 0; // periodo presente é um período futuro
    Integer numeroPeriodosFuturosAdicional = 0; // mesmo que numeroPeriodosFuturos : pode ser usado por exemplo para calcular a política estoques no último período do calendário
    
    
    LocalDateTime dataHorarioInicial; // semana 0, a primeira semana considerada no calendário
    LocalDateTime dataHorarioFinal; // semanaFinalFutura + max(leadtime) + max(seguranca) + max(frequenciaemissao) - 1
    
    LocalDateTime dataHorarioInicialPassadaAdicional; // usado apenas em simulações de estoques: período antes do passado para cálculo forecast
    LocalDateTime dataHorarioFinalPassadaAdicional; // usado apenas em simulações de estoques: período antes do passado para cálculo forecast
    LocalDateTime dataHorarioInicialPassada; // usado apenas em simulações de estoques: período antes do passado para cálculo forecast
    LocalDateTime dataHorarioFinalPassada; // usado apenas em simulações de estoques: período antes do passado para cálculo forecast
    LocalDateTime dataHorarioInicialPresente; // última semana antes da semanaPresente
    LocalDateTime dataHorarioFinalPresente; // última semana antes da semanaPresente
    LocalDateTime dataHorarioInicialFutura; // última semana do horizonte de planejamento
    LocalDateTime dataHorarioFinalFutura; // última semana do horizonte de planejamento
    LocalDateTime dataHorarioInicialFuturaAdicional; // semanaFinalFutura + max(leadtime) + max(seguranca) + max(frequenciaemissao) - 1
    LocalDateTime dataHorarioFinalFuturaAdicional; // semanaFinalFutura + max(leadtime) + max(seguranca) + max(frequenciaemissao) - 1
    
    
    // key(LocalDateTime) -> posição
    Map<LocalDateTime, Integer> mapaDatasHorarios = new HashMap<>();
    // posição -> LocalDateTime
    List<LocalDateTime> listaDatasHorarios = new ArrayList<>();    

    public static Calendario criaCalendarioDeOffsetsDias(
            Constantes.TamanhoBucket tamanhoBucket, 
            LocalDateTime dataHorarioReferencia, 
            int offsetDiasAdicionalPassado, int offsetDiasPassado,
            int offsetDiasFuturo, int offsetDiasAdicionalFuturo) {
        
        LocalDateTime dataInicialArredondada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                dataHorarioReferencia.minusDays(offsetDiasAdicionalPassado + offsetDiasPassado), 0, tamanhoBucket);
        LocalDateTime dataInicialPassadaArredondada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                dataHorarioReferencia.minusDays(offsetDiasPassado), 0, tamanhoBucket);
        LocalDateTime dataFinalFuturaArredondada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                dataHorarioReferencia.plusDays(offsetDiasFuturo), 0, tamanhoBucket);
        LocalDateTime dataFinalFuturaAdicionalArredondada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                dataHorarioReferencia.plusDays(offsetDiasFuturo + offsetDiasAdicionalFuturo), 0, tamanhoBucket);

        int offsetPassadoAdicional = - getOffsetPeriodosEntreDataHorarios(dataInicialPassadaArredondada, dataInicialArredondada, tamanhoBucket);
        int offsetPassado = - getOffsetPeriodosEntreDataHorarios(dataHorarioReferencia, dataInicialPassadaArredondada, tamanhoBucket);
        int offsetFuturo = getOffsetPeriodosEntreDataHorarios(dataHorarioReferencia, dataFinalFuturaArredondada, tamanhoBucket);
        int offsetFuturoAdicional = getOffsetPeriodosEntreDataHorarios(dataFinalFuturaArredondada, dataFinalFuturaAdicionalArredondada, tamanhoBucket);
        
        return criaCalendarioDeOffsetsPeriodos(tamanhoBucket, dataHorarioReferencia, 
                offsetPassadoAdicional, offsetPassado, offsetFuturo, offsetFuturoAdicional);
        
    }
    
    /**
     * Cria um calendário entre datas inicial e final. 
     * O calendário considera 100% das datas como futuras
     * @param tamanhoBucket
     * @param dataInicial
     * @param dataFinal
     * @return 
     */
    public static Calendario criaCalendarioPeriodosFuturosDeDatas(Constantes.TamanhoBucket tamanhoBucket,
                                                                  LocalDateTime dataInicial, LocalDateTime dataFinal) {
        
        if (dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException("Data inicial " + dataInicial + " precisa ser igual ou anterior à dataFinal " + dataFinal);
        }
        
        LocalDateTime dataInicialArredondada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataInicial, 0, tamanhoBucket);
        LocalDateTime dataFinalFuturaArredondada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataFinal, 0, tamanhoBucket);
                
        int offsetFuturo = getOffsetPeriodosEntreDataHorarios(dataInicial, dataFinal, tamanhoBucket) + 1;
        
        return criaCalendarioDeOffsetsPeriodos(tamanhoBucket, dataInicial, 0, 0, offsetFuturo, 0);
        
    }

    private static Calendario criaCalendarioVazio(Constantes.TamanhoBucket tamanhoBucket, LocalDateTime dataReferencia) {
        
        if (tamanhoBucket == null) throw new IllegalArgumentException("Bucket size is null");
        
        Calendario calendario = new Calendario();
        calendario.tamanhoBucket = tamanhoBucket;
        calendario.dataHorarioReferencia = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataReferencia, 0, tamanhoBucket);
        calendario.dataHorarioInicialPresente = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataReferencia, 0, tamanhoBucket);
        
        return calendario;
    }
    
    public static Calendario criaCalendarioDeDatas(Constantes.TamanhoBucket tamanhoBucket, 
            LocalDateTime dataInicial, LocalDateTime dataInicialPresente, LocalDateTime dataFinal) {
        
        if (dataInicial.isAfter(dataInicialPresente) || dataInicialPresente.isAfter(dataFinal)) {
            throw new IllegalArgumentException("Data inicial " + dataInicial + " data inicial presente " + dataInicialPresente + " dataFinal " + dataFinal
                    + " precisam ser sequenciais");
        }
        
        LocalDateTime dataInicialArredondada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataInicial, 0, tamanhoBucket);
        LocalDateTime dataInicialPresenteArredondada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataInicialPresente, 0, tamanhoBucket);
        LocalDateTime dataFinalFuturaArredondada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataFinal, 0, tamanhoBucket);
                
        int offsetPassado = - getOffsetPeriodosEntreDataHorarios(dataInicialPresente, dataInicial, tamanhoBucket);
        int offsetFuturo = getOffsetPeriodosEntreDataHorarios(dataInicialPresente, dataFinal, tamanhoBucket);
        
        return criaCalendarioDeOffsetsPeriodos(tamanhoBucket, dataInicialPresente, 0, offsetPassado, offsetFuturo + 1, 0);
        
    }

    public static Calendario criaCalendarioPeriodosPassadosDeDatas(Constantes.TamanhoBucket tamanhoBucket,
                                                   LocalDateTime dataInicialPassada, LocalDateTime dataFinalPassada) {

        // data inicial futuro = 1 período à frente da data final passada
        LocalDateTime dataInicialFuturo = Calendario.getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataFinalPassada, 1, tamanhoBucket);

        return criaCalendarioDeDatas(tamanhoBucket, dataInicialPassada, dataInicialFuturo, dataInicialFuturo);

    }

    /**
     * Retorna um número (positivo ou negativo) de períodos entre duas datas
     * Caso elas pertençam ao mesmo período retorna 0
     * @param dataHorarioReferencia
     * @param dataHorarioComparacao retorno positivo se estiver à frente de dataHorarioReferencia e vice-versa
     * @param tamanhoBucket
     * @return 
     */
    public static int getOffsetPeriodosEntreDataHorarios(LocalDateTime dataHorarioReferencia, LocalDateTime dataHorarioComparacao, TamanhoBucket tamanhoBucket) {
        
        LocalDateTime dataHorarioReferenciaArredondado = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataHorarioReferencia, 0, tamanhoBucket);
        LocalDateTime dataHorarioComparacaoArredondado = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataHorarioComparacao, 0, tamanhoBucket);
        
        int posicaoPeriodo = 0;
        LocalDateTime dataIterada = dataHorarioReferenciaArredondado.plusDays(0);
        
        // dataCompacacao < dataReferencia
        while (dataIterada.isAfter(dataHorarioComparacaoArredondado)) {
            dataIterada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataHorarioReferencia, posicaoPeriodo, tamanhoBucket);
            if (dataIterada.isEqual(dataHorarioComparacaoArredondado)) {
                return posicaoPeriodo;
            }
            posicaoPeriodo--;
        }
        // dataCompacacao > dataReferencia
        while (dataIterada.isBefore(dataHorarioComparacaoArredondado)) {
            dataIterada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataHorarioReferencia, posicaoPeriodo, tamanhoBucket);
            if (dataIterada.isEqual(dataHorarioComparacaoArredondado)) {
                return posicaoPeriodo;
            }
            posicaoPeriodo++;
        }

        return 0;
        
    } 
    
    /**
     * Método principal para criação de calendários
     * @param tamanhoBucket
     * @param dataHorarioReferencia
     * @param offsetPeriodosAdicionalPassado
     * @param offsetPeriodosPassado
     * @param offsetPeriodosFuturo
     * @param offsetPeriodosAdicionalFuturo
     * @return 
     */
    public static Calendario criaCalendarioDeOffsetsPeriodos(
            Constantes.TamanhoBucket tamanhoBucket, LocalDateTime dataHorarioReferencia,
            int offsetPeriodosAdicionalPassado, int offsetPeriodosPassado, 
            int offsetPeriodosFuturo, int offsetPeriodosAdicionalFuturo) {
                
        if (offsetPeriodosPassado < 0 || offsetPeriodosAdicionalPassado < 0 || offsetPeriodosFuturo < 0 || offsetPeriodosAdicionalFuturo < 0) {
            throw new IllegalArgumentException("Um dos offsets passados é negativo");
        }
        if (offsetPeriodosAdicionalPassado > 0 && offsetPeriodosPassado == 0) {
            throw new IllegalArgumentException("offsetPeriodosAdicionalPassado > 0 e offsetPeriodosPassado = 0");
        }
        if (offsetPeriodosAdicionalFuturo > 0 && offsetPeriodosFuturo == 0) {
            throw new IllegalArgumentException("offsetPeriodosAdicionalPassado > 0 e offsetPeriodosPassado = 0");
        }
        if (offsetPeriodosPassado == 0) offsetPeriodosPassado = offsetPeriodosAdicionalPassado;
        
        Calendario calendario = criaCalendarioVazio(tamanhoBucket, dataHorarioReferencia);
        calendario.tamanhoBucket = tamanhoBucket;
        
        calendario.posicaoPeriodoPresente = offsetPeriodosAdicionalPassado + offsetPeriodosPassado;
        
        calendario.dataHorarioInicial = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                dataHorarioReferencia, -offsetPeriodosPassado -offsetPeriodosAdicionalPassado , tamanhoBucket);
        calendario.dataHorarioFinal = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                dataHorarioReferencia, +offsetPeriodosFuturo +offsetPeriodosAdicionalFuturo , tamanhoBucket).minusSeconds(1);
                        
        for (int i=0; i < offsetPeriodosAdicionalPassado; i++) {
            LocalDateTime dataIterada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                    calendario.getDataHorarioInicial(), i, tamanhoBucket);
            
            calendario.mapaDatasHorarios.put(dataIterada, i);
            calendario.listaDatasHorarios.add(dataIterada);
            
            // atualiza data inicial do offsetPeriodosAdicionalPassado
            if (calendario.dataHorarioInicialPassadaAdicional == null) {
                calendario.posicaoPeriodoInicialPassadoAdicional = 0;
                calendario.dataHorarioInicialPassadaAdicional = dataIterada;
            }
            // atualiza data final do offsetPeriodosAdicionalPassado
            if (i == offsetPeriodosAdicionalPassado - 1) {
                calendario.posicaoPeriodoFinalPassadoAdicional = i;
                calendario.dataHorarioFinalPassadaAdicional = getUltimaDataHorarioPeriodo(dataIterada, tamanhoBucket);;
            }
            
            calendario.numeroPeriodosPassadosAdicional++;
            
        }
        for (int i=offsetPeriodosAdicionalPassado; i < offsetPeriodosAdicionalPassado + offsetPeriodosPassado; i++) {
            LocalDateTime dataIterada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                    calendario.getDataHorarioInicial(), i, tamanhoBucket);
            
            calendario.mapaDatasHorarios.put(dataIterada, i);
            calendario.listaDatasHorarios.add(dataIterada);
            
            // atualiza data inicial do offsetPeriodosPassado
            if (calendario.dataHorarioInicialPassada == null) {
                calendario.posicaoPeriodoInicialPassado = i;
                calendario.dataHorarioInicialPassada = dataIterada;
            }
            // atualiza data final do offsetPeriodosPassado
            if (i == offsetPeriodosAdicionalPassado + offsetPeriodosPassado - 1) {
                calendario.posicaoPeriodoFinalPassado = i;
                calendario.dataHorarioFinalPassada = getUltimaDataHorarioPeriodo(dataIterada, tamanhoBucket);;
            }
            
            calendario.numeroPeriodosPassados++;
            
        }
        for (int i=offsetPeriodosAdicionalPassado + offsetPeriodosPassado; i < offsetPeriodosAdicionalPassado + offsetPeriodosPassado + offsetPeriodosFuturo; i++) {
            LocalDateTime dataIterada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                    calendario.getDataHorarioInicial(), +i, tamanhoBucket);
            
            calendario.mapaDatasHorarios.put(dataIterada, i);
            calendario.listaDatasHorarios.add(dataIterada);
            
            // atualiza data inicial do offsetPeriodosFuturo
            if (calendario.dataHorarioInicialFutura == null) {
                calendario.posicaoPeriodoInicialFuturo = i;
                calendario.dataHorarioInicialFutura = dataIterada;
            }
            // atualiza data final do offsetPeriodosFuturo
            if (i == offsetPeriodosAdicionalPassado + offsetPeriodosPassado + offsetPeriodosFuturo - 1) {
                calendario.posicaoPeriodoFinalFuturo = i;
                calendario.dataHorarioFinalFutura = getUltimaDataHorarioPeriodo(dataIterada, tamanhoBucket);
            }
            
            calendario.numeroPeriodosFuturos++;
            
        }
        for (int i=offsetPeriodosAdicionalPassado + offsetPeriodosPassado + offsetPeriodosFuturo; i < offsetPeriodosAdicionalPassado + offsetPeriodosPassado + offsetPeriodosFuturo + offsetPeriodosAdicionalFuturo; i++) {
            
            LocalDateTime dataIterada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                    calendario.getDataHorarioInicial(), i, tamanhoBucket);
            
            calendario.mapaDatasHorarios.put(dataIterada, i);
            calendario.listaDatasHorarios.add(dataIterada);
            
            // atualiza data inicial do offsetPeriodosFuturoAdicional
            if (calendario.dataHorarioInicialFuturaAdicional == null) {
                calendario.posicaoPeriodoInicialFuturoAdicional = i;
                calendario.dataHorarioInicialFuturaAdicional = dataIterada;
            }
            // atualiza data final do offsetPeriodosFuturoAdicional
            if (i == offsetPeriodosAdicionalPassado + offsetPeriodosPassado + offsetPeriodosFuturo + offsetPeriodosAdicionalFuturo - 1) {
                calendario.posicaoPeriodoFinalFuturoAdicional = i;
                calendario.dataHorarioFinalFuturaAdicional = getUltimaDataHorarioPeriodo(dataIterada, tamanhoBucket);;
            }
            
            calendario.numeroPeriodosFuturosAdicional++;
            
        }
        
        return calendario;
    }

    public double getNumeroDiasNoPeriodo(int posicaoPeriodo) {
        switch (tamanhoBucket) {
            case ANUAL:
                Year ano = Year.of(getPrimeiraDataHorarioPeriodo(posicaoPeriodo).getYear());
                return (ano.isLeap()) ? 366 : 365;
            // conversões que dependem da posicaoPeriodo:
            case MENSAL : return getPrimeiraDataHorarioPeriodo(posicaoPeriodo).plusMonths(1).minusDays(1).getDayOfMonth();
            // conversões padrão (independem da posicaoPeriodo) :
            default : return getNumeroMedioDiasPorPeriodo(tamanhoBucket);
        }
    }

    public double getNumeroHorasNoPeriodo(int posicaoPeriodo) {
        return getNumeroDiasNoPeriodo(posicaoPeriodo) * 24;
    }

    public double getNumeroDiasEntrePeriodos(int posicaoPeriodoInicial, int posicaoPeriodoFinal) {
        double numeroDiasTotais = 0;
        for (int i=posicaoPeriodoInicial; i<=posicaoPeriodoFinal; i++) {
            numeroDiasTotais += getNumeroDiasNoPeriodo(i);
        }
        return numeroDiasTotais;
    }
    
    public double getNumeroDiasEmPeriodosFuturos() {
        double quantidadeDias = 0f;
        for (int i = getPosicaoPeriodoPresente(); i <= getPosicaoPeriodoFinalFuturo(); i++) {
            quantidadeDias += getNumeroDiasNoPeriodo(i);
        }
        return quantidadeDias;
    }

    public double getNumeroDiasEmPeriodosPassados() {
        double quantidadeDias = 0f;
        for (int i = 0; i <= getPosicaoPeriodoFinalPassado(); i++) {
            quantidadeDias += getNumeroDiasNoPeriodo(i);
        }
        return quantidadeDias;
    }

    /**
     * @return numeroSemanasPassados + numeroSemanasPassadosAdicional
     */
    public int getNumeroPeriodosPassadosTotal() {
        return getNumeroPeriodosPassados() + getNumeroPeriodosPassadosAdicional();
    }

    public int getNumeroPeriodosTotais() {
        return getNumeroPeriodosPassadosAdicional() + getNumeroPeriodosPassados() + getNumeroPeriodosFuturos() + getNumeroPeriodosFuturosAdicional();
    }

    /**
     * Para um determinado numero de dias retorna o numero de dias/semanas/meses
     * correspondente dependendo do calendário
     *
     * @param numeroDias
     * @return número de dias/semanas/meses correspondente
     */
    public double converteDiasParaPeriodosCalendario(double numeroDias) {
        return numeroDias / getNumeroMedioDiasPorPeriodo(tamanhoBucket);
    }

    public static double converteDiasParaPeriodosCalendario(double numeroDias, Constantes.TamanhoBucket tamanhoBucket) {
        return numeroDias / getNumeroMedioDiasPorPeriodo(tamanhoBucket);
    }
    
    public static double getNumeroMedioDiasPorPeriodo(TamanhoBucket tamanhoBucket) {

        if (tamanhoBucket == null) {
            throw getUnsupportedTamanhoBucketException(
                    "Calendario.getNumeroMedioDiasPorPeriodo",
                    null);
        }
        
        switch (tamanhoBucket) {           
            case SEGUNDO : return 1.0 / (60 * 60 * 24);
            case MINUTO : return 1.0 / (60 * 24);
            case SEXTO_HORA : return 1.0 / (60 * 24 / 6.0);
            case QUARTO_HORA : return 1.0 / (60 * 24 / 4.0);
            case MEIA_HORA : return 1.0 / (60 * 24 / 2.0);
            case HORARIO : return 1.0 / 24.0;
            case TURNO : return 1.0 / 3.0;
            case DIARIO : return 1;
            case SEMANAL : return 7;
            case MENSAL : return 30.436875;
            case ANUAL: return 365.25;
            default : throw getUnsupportedTamanhoBucketException(
                    "Calendario.getNumeroMedioDiasPorPeriodo",
                    tamanhoBucket);
        }
        
    }

    /**
     * Exemplo : Bucket Origem diario -> Bucket Destino Semanal. Retorno = # dias por semana (7) / # dias por dia (1) = 7
     * Exemplo : Bucket Origem Semanal -> Bucket Destino Mensal. Retorno = # dias por mês (30.436875) / # dias por semana (7) = 4.348125
     * @param tamanhoBucketOrigem
     * @param tamanhoBucketDestino
     * @return
     */
    public static double getNumeroMedioPeriodosBucketOrigemNoBucketDestino(TamanhoBucket tamanhoBucketOrigem, TamanhoBucket tamanhoBucketDestino) {
        return getNumeroMedioDiasPorPeriodo(tamanhoBucketDestino) / getNumeroMedioDiasPorPeriodo(tamanhoBucketOrigem);
    }

    public LocalDate getPrimeiraDataPeriodo(int posicaoPeriodo) {
        return getPrimeiraDataHorarioPeriodo(posicaoPeriodo).toLocalDate();
    }
    public LocalDate getUltimaDataPeriodo(int posicaoPeriodo) {
        return getUltimoSegundoPeriodo(posicaoPeriodo).toLocalDate();
    }

    /**
     * Retorna o número de períodos entre duas datas.
     * Ex1 : Calendário mensal com dataInicial = 01/01/2030 e dataFinal = 15/01/2030 . Retorna 1 período
     * Ex2 : Calendário mensal com dataInicial = 15/01/2030 e dataFinal = 05/02/2030 . Retorna 2 períodos
     * @param dataInicial
     * @param dataFinal
     * @return
     */
    public int getNumeroPeriodosEntreDatas(LocalDate dataInicial, LocalDate dataFinal) {
        int posicaoPeriodoInicial = getPosicaoPeriodo(dataInicial);
        int posicaoPeriodoFinal = getPosicaoPeriodo(dataFinal);

        return posicaoPeriodoFinal - posicaoPeriodoInicial + 1;
    }

    /**
     * Indica se a data faz parte do período contemplado no calendário
     *
     * @param data
     * @return
     */
    public boolean contemData(LocalDateTime data) {
        LocalDateTime dataArredondadaParaInicioBucket = getPrimeiraDataHorarioPeriodoCalendarioComOffset(data, 0, getTamanhoBucket());
        
        if ((dataArredondadaParaInicioBucket.isEqual(getDataHorarioInicial()) || dataArredondadaParaInicioBucket.isAfter(getDataHorarioInicial()))
                && (dataArredondadaParaInicioBucket.isEqual(getDataHorarioFinal()) || dataArredondadaParaInicioBucket.isBefore(getDataHorarioFinal()))) {
            return true;
        }
        return false;
    }
    
    public boolean contemDataNoPeriodo(LocalDateTime data, int posicaoPeriodo) {
        
        LocalDateTime dataHorarioInicialPeriodo = getPrimeiraDataHorarioPeriodo(posicaoPeriodo);
        LocalDateTime dataHorarioFinalPeriodo = getUltimaDataHorarioPeriodo(posicaoPeriodo);
        
        return data.isAfter(dataHorarioInicialPeriodo.minusSeconds(1)) && data.isBefore(dataHorarioFinalPeriodo.plusSeconds(1));
        
    }
    
    /**
     * Indica se a data faz parte do período contemplado no calendário
     *
     * @param data
     * @return
     */
    public boolean contemData(LocalDate data) {
        return contemData(data.atStartOfDay());
    }

    /**
     * Retorna a posição do calendário correspondente à data
     *
     * @param data
     * @return int com posição período. também traz posições período fora do calendário
     */
    public Integer getPosicaoPeriodo(LocalDateTime data) {
        LocalDateTime dataArredondada = getPrimeiraDataHorarioPeriodoCalendarioComOffset(data, 0, tamanhoBucket);
        return mapaDatasHorarios.getOrDefault(dataArredondada, getOffsetPeriodosEntreDataHorarios(getDataHorarioInicial(), data, tamanhoBucket));
    }
    public Integer getPosicaoPeriodo(LocalDate data) {
        return getPosicaoPeriodo(data.atStartOfDay());
    }

    public LocalDateTime getPrimeiraDataHorarioPeriodo(int posicaoPeriodo) {
        if (posicaoPeriodo >= 0 && posicaoPeriodo < listaDatasHorarios.size()) {
            return listaDatasHorarios.get(posicaoPeriodo);
        }
        return getPrimeiraDataHorarioPeriodoCalendarioComOffset(getDataHorarioInicial(), posicaoPeriodo, getTamanhoBucket());
    }
    public LocalDateTime getPrimeiraDataHorarioPeriodo(LocalDateTime dataReferencia) {
        return getPrimeiraDataHorarioPeriodo(dataReferencia, getTamanhoBucket());
    }
    public static LocalDateTime getPrimeiraDataHorarioPeriodo(LocalDateTime dataReferencia, TamanhoBucket tamanhoBucket) {
        return getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataReferencia, 0, tamanhoBucket);
    }
    
    public LocalDateTime getUltimaDataHorarioPeriodo(int posicaoPeriodo) {
        if (posicaoPeriodo >= 0 && posicaoPeriodo < listaDatasHorarios.size() - 1) {
            return listaDatasHorarios.get(posicaoPeriodo + 1).minusSeconds(1);
        }
        return getPrimeiraDataHorarioPeriodoCalendarioComOffset(getDataHorarioInicial(), posicaoPeriodo + 1, getTamanhoBucket()).minusSeconds(1);    
    }
    public LocalDateTime getUltimaDataHorarioPeriodo(LocalDateTime dataReferencia) {
        return getUltimaDataHorarioPeriodo(dataReferencia, getTamanhoBucket());
    }
    public LocalDateTime getUltimaDataHorarioPeriodo(LocalDate dataReferencia) {
        return getUltimaDataHorarioPeriodo(dataReferencia.atStartOfDay(), getTamanhoBucket());
    }
    public static LocalDateTime getUltimaDataHorarioPeriodo(LocalDateTime dataReferencia, TamanhoBucket tamanhoBucket) {
        return getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataReferencia, 1, tamanhoBucket).minusSeconds(1);
    }
    public static LocalDateTime getUltimaDataHorarioPeriodo(LocalDate dataReferencia, TamanhoBucket tamanhoBucket) {
        return getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataReferencia.atStartOfDay(), 1, tamanhoBucket).minusSeconds(1);
    }
    
    /**
     * Retorna o número de períodos que contempla um ciclo de sazonalidade
     * completo
     *
     * @return
     */
    public double getFrequenciaCalendario() {

        if (tamanhoBucket == null) {
            throw getUnsupportedTamanhoBucketException(
                    "Calendario.getFrequenciaCalendario",
                    null);
        }
        
        switch (tamanhoBucket) {
            case SEGUNDO: return 60;
            case MINUTO: return 60;
            case SEXTO_HORA: return 6;
            case QUARTO_HORA: return 4;
            case MEIA_HORA: return 2;
            case HORARIO: return 24;
            case TURNO: return 3;
            case DIARIO: return 7;
            case SEMANAL: return 52;
            case MENSAL: return 12;
            case ANUAL: return 1;
            default: throw getUnsupportedTamanhoBucketException(
                    "Calendario.getFrequenciaCalendario",
                    tamanhoBucket);
        }
        
    }

    /**
     * Calendário mensal : retorna lista com 0) o ano da posição período 1) o mês MM
     * Calendário semanal : retorna lista com 0) o ano YYYY 1) a semana WW
     * Calendário diário : retorna lista com 0) a semana YYYYWW 1) o dia da semana
     * @param posicaoPeriodo
     * @return 
     */
    public List<Integer> getAgregadorPeriodo(int posicaoPeriodo) {
        LocalDateTime primeiraDataPeriodo = getPrimeiraDataHorarioPeriodo(posicaoPeriodo);
        List<Integer> agregadorPeriodo = new ArrayList<>();
        switch (getTamanhoBucket()) {
            case ANUAL:
                agregadorPeriodo.add(primeiraDataPeriodo.getYear());
                break;
            case MENSAL:
                agregadorPeriodo.add(primeiraDataPeriodo.getYear());
                agregadorPeriodo.add(primeiraDataPeriodo.getMonthValue());
                break;
            case SEMANAL:
                agregadorPeriodo.add(primeiraDataPeriodo.getYear());
                agregadorPeriodo.add(getSemanaWW(primeiraDataPeriodo));
                break;
            case DIARIO:
                agregadorPeriodo.add(getSemanaYYYYWW(primeiraDataPeriodo));
                agregadorPeriodo.add(primeiraDataPeriodo.getDayOfWeek().getValue());
                break;
            case TURNO:
                agregadorPeriodo.add(getDataYYYYMMDD(primeiraDataPeriodo.toLocalDate()));
                agregadorPeriodo.add(Math.floorDiv(primeiraDataPeriodo.getHour(), 8) + 1); // turno, de 1 a 3
                break;
            case HORARIO:
                agregadorPeriodo.add(getDataYYYYMMDD(primeiraDataPeriodo.toLocalDate()));
                agregadorPeriodo.add(primeiraDataPeriodo.getHour());
                break;
            case MEIA_HORA:
            case QUARTO_HORA:
            case SEXTO_HORA:
            case MINUTO:
            case SEGUNDO:
                throw getUnsupportedTamanhoBucketException(
                        "Calendario.getAgregadorPeriodo",
                        getTamanhoBucket());
            default:
                throw getUnsupportedTamanhoBucketException(
                        "Calendario.getAgregadorPeriodo",
                        getTamanhoBucket());
        }
        return agregadorPeriodo;
    }

    /**
     * Padroniza mensagens de contrato de calendario para buckets nulos,
     * adicionados futuramente ao enum ou ainda sem suporte naquela operacao.
     */
    private static IllegalArgumentException getUnsupportedTamanhoBucketException(
            String operacao,
            TamanhoBucket tamanhoBucket) {

        return new IllegalArgumentException(
                operacao + " does not support calendar bucket "
                        + (tamanhoBucket == null ? "null" : tamanhoBucket.name())
                        + ". Review the calendar granularity before executing planning calculations.");

    }
    
    /**
     * Retorna lista com o 2o nível dos agrupadores período
     * Calendário mensal : lista com meses, sem duplicatas
     * Calendário semanal : lista com número das semanas, sem duplicatas
     * Calendário diário : lista com dias da semana, sem duplicatas
     * @return
     */
    public List<Integer> getListaAgrupadoresPeriodo(int posicaoAgregacao) {
        List<Integer> listaAgrupadoresPeriodo = new ArrayList<>();
        for (int i=0; i<getNumeroPeriodosTotais(); i++) {
            int agrupadorPeriodo = getAgregadorPeriodo(i).get(posicaoAgregacao);
            listaAgrupadoresPeriodo.add(agrupadorPeriodo);
        }
        return listaAgrupadoresPeriodo;
    }
    public List<Integer> getListaAgrupadoresPeriodoSemDuplicatasOrdenado(int posicaoAgregacao) {
        return getListaAgrupadoresPeriodo(posicaoAgregacao).stream().distinct().sorted().collect(Collectors.toList());
    }
        
    public int getDescricaoIntegerPeriodo(int posicaoPeriodo) {
        return getDescricaoIntegerPeriodo(getPrimeiraDataHorarioPeriodo(posicaoPeriodo), getTamanhoBucket());
    }
    
    public static int getDescricaoIntegerPeriodo(LocalDateTime dataReferencia, Constantes.TamanhoBucket tamanhoBucket) {
        switch (tamanhoBucket) {
            case DIARIO:
                return getDataYYYYMMDD(dataReferencia);
            case SEMANAL:
                return getSemanaYYYYWW(dataReferencia);
            case MENSAL:
                return getMesYYYYMM(dataReferencia);
            case ANUAL:
                return dataReferencia.getYear();
            default:
                return getDataYYYYMMDD(dataReferencia);
        }
    }

    public static String getDescricaoPeriodo(LocalDateTime dataReferencia, Constantes.TamanhoBucket tamanhoBucket) {
        switch (tamanhoBucket) {
            case ANUAL:
                return String.valueOf(dataReferencia.getYear());
            case MENSAL:
                return String.valueOf(getMesYYYYMM(dataReferencia));
            case SEMANAL:
                return String.valueOf(getSemanaYYYYWW(dataReferencia));
            case DIARIO:
                return dataReferencia.toString();
        }
        return null;
    }
    
    public List<LocalDateTime> getListDataHorariosFinaisPorPeriodo() {
        
        return listaDatasHorarios.stream()
                .map(x -> getUltimaDataHorarioPeriodo(x))
                .collect(Collectors.toList());
        
    }
    
    public static LocalDateTime getPrimeiraDataFromDescricaoPeriodo(String descricaoPeriodo, Constantes.TamanhoBucket tamanhoBucket) {
        switch (tamanhoBucket) {
            case ANUAL:
                return LocalDate.of(Integer.valueOf(descricaoPeriodo), 1, 1).atStartOfDay();
            case MENSAL:
                return LocalDate.of(
                        Integer.valueOf(descricaoPeriodo.substring(0, 4)), 
                        Integer.valueOf(descricaoPeriodo.substring(4,6)), 1).atStartOfDay();
            case SEMANAL:
                LocalDate dataInicioBusca = LocalDate.of(
                        Integer.valueOf(descricaoPeriodo.substring(0, 4)), 
                        1, 1).minusDays(14); // semana pode começar ano anterior
                 LocalDate dataFimBusca = LocalDate.of(
                        Integer.valueOf(descricaoPeriodo.substring(0, 4)), 
                        12, 31).plusDays(14); // semana pode terminar proximo ano
                LocalDate dataAtual = dataInicioBusca.plusDays(0); // data atual é cópia da data inicio busca
                
                while (dataAtual.isBefore(dataFimBusca)) {
                    int semanaAtual = getSemanaYYYYWW(dataAtual);
                    if (semanaAtual == Integer.valueOf(descricaoPeriodo)) {
                        TemporalField temporalField = WeekFields.ISO.dayOfWeek();
                        // retorna 1o dia da semana
                        return dataAtual.with(temporalField, temporalField.range().getMinimum()).atStartOfDay();
                    }
                    dataAtual = dataAtual.plusWeeks(1);
                }
            case DIARIO:
                return stringToLocalDate(descricaoPeriodo).atStartOfDay();
            case TURNO:
                return LocalDateTime.parse(descricaoPeriodo);
            case HORARIO:
                return LocalDateTime.parse(descricaoPeriodo);
        }
        return null;
    }
    
    /**
     * Retorna semana atual no formato YYYYWW
     *
     * @param date
     * @return
     */
    public static int getSemanaYYYYWW(LocalDate date) {
        int weekNumber = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        int weekYear = date.get(WeekFields.ISO.weekBasedYear());
        return weekYear * 100 + weekNumber;
    }
    /**
     * Retorna semana atual no formato YYYYWW
     *
     * @param date
     * @return
     */
    public static int getSemanaYYYYWW(LocalDateTime date) {
        int weekNumber = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        int weekYear = date.get(WeekFields.ISO.weekBasedYear());
        return weekYear * 100 + weekNumber;
    }
    
    /**
     * Retorna semana atual no formato WW (1 a 53)
     *
     * @param date
     * @return
     */
    public static int getSemanaWW(LocalDate date) {
        int weekNumber = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        return weekNumber;
    }
    /**
     * Retorna semana atual no formato WW (1 a 53)
     *
     * @param date
     * @return
     */
    public static int getSemanaWW(LocalDateTime date) {
        int weekNumber = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        return weekNumber;
    }

    /**
     * Retorna o mês atual no formato YYYYMM
     *
     * @param date
     * @return
     */
    public static int getMesYYYYMM(LocalDateTime date) {
        int mesNumber = date.getMonthValue();
        int mesYear = date.getYear();
        return mesYear * 100 + mesNumber;
    }
    /**
     * Retorna o mês atual no formato YYYYMM
     *
     * @param date
     * @return
     */
    public static int getMesYYYYMM(LocalDate date) {
        int mesNumber = date.getMonthValue();
        int mesYear = date.getYear();
        return mesYear * 100 + mesNumber;
    }
    
    public static int getDataYYYYMMDD(LocalDate date) {
        int mesNumber = date.getMonthValue();
        int mesYear = date.getYear();
        int dia = date.getDayOfMonth();
        return mesYear * 10000 + mesNumber * 100 + dia;
    }
    public static int getDataYYYYMMDD(LocalDateTime date) {
        int mesNumber = date.getMonthValue();
        int mesYear = date.getYear();
        int dia = date.getDayOfMonth();
        return mesYear * 10000 + mesNumber * 100 + dia;
    }

    /**
     * Retorna ultima data (iso) de acordo com a semana
     * Referencia : https://dzone.com/articles/deeper-look-java-8-date-and
     * DESCONSIDERAR PARA LOCALDATETIME : NESSE CASO SE QUER O 1o HORARIO DA SEMANA AO INVES DO ULTIMO DIA!
     * @param semana ano e semana
     * @return ultimo dia ISO da samana
     */
    public static LocalDate getUltimoDiaSemana(Integer semana) {
        String dateStr = String.valueOf(semana);
        Integer weekNumber = Integer.parseInt(dateStr.substring(4, dateStr.length()));//2017'52'
        Integer yearNumber = Integer.parseInt(dateStr.substring(0, 4));//'2017'50
        YearWeek yearWeek = YearWeek.of(yearNumber, weekNumber);
        return yearWeek.atDay(DayOfWeek.SUNDAY);
    }
    
    /**
     * Retorna primeiro horário data (iso) de acordo com a semana
     * Referencia : https://dzone.com/articles/deeper-look-java-8-date-and
     *
     * @param semana ano e semana
     * @return primeiro dia/horário ISO da samana
     */
    public static LocalDateTime getPrimeiroDiaHorarioSemana(Integer semana) {
        String dateStr = String.valueOf(semana);
        Integer weekNumber = Integer.parseInt(dateStr.substring(4, dateStr.length()));//2017'52'
        Integer yearNumber = Integer.parseInt(dateStr.substring(0, 4));//'2017'50
        YearWeek yearWeek = YearWeek.of(yearNumber, weekNumber);
        return yearWeek.atDay(DayOfWeek.MONDAY).atStartOfDay();
    }

    public static LocalDate getUltimoDiaMes(Integer mes) {
        String dateStr = String.valueOf(mes);
        //convertendo o numero do mês em int
        String monthNumber = dateStr.substring(4, dateStr.length());//2017'52'
        String yearNumber = dateStr.substring(0, 4);//'2017'50
        YearMonth yearMonth = YearMonth.of(Integer.valueOf(yearNumber), Integer.valueOf(monthNumber));
        return yearMonth.atEndOfMonth();
    }
    
    public static LocalDateTime getPrimeiroDiaHorarioMes(Integer mes) {
        String dateStr = String.valueOf(mes);
        //convertendo o numero do mês em int
        String monthNumber = dateStr.substring(4, dateStr.length());//2017'52'
        String yearNumber = dateStr.substring(0, 4);//'2017'50
        YearMonth yearMonth = YearMonth.of(Integer.valueOf(yearNumber), Integer.valueOf(monthNumber));
        return yearMonth.atDay(1).atStartOfDay();
    }


    /**
     * Converte Date em LocalDate
     *
     * @param date
     * @return
     */
    public static LocalDate dateToLocalDate(Date date) {
        return LocalDate.from(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()));
    }

    public static LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static LocalDate getUltimoDiaSemana(LocalDate data) {
        return data.with(DayOfWeek.SUNDAY); // semana ISO
    }
    public static LocalDate getPrimeiroDiaSemana(LocalDate data) {
        return data.with(DayOfWeek.MONDAY); // semana ISO
    }
    
    public static LocalDateTime getPrimeiraDataHorarioPeriodoCalendarioComOffset(LocalDateTime data, int offsetPeriodos, 
            TamanhoBucket tamanhoBucket) {
        switch (tamanhoBucket) {
            case MEIA_HORA:
                int minuto = data.getMinute();
                int minutoArredondadoParaMeiaHoraBaixo = Math.floorDiv(minuto, 30);
                return data.withMinute(minutoArredondadoParaMeiaHoraBaixo).plusMinutes(offsetPeriodos * 30).withSecond(0).withNano(0);
            case HORARIO:
                return data.plusHours(offsetPeriodos).withMinute(0).withSecond(0).withNano(0);
            case TURNO:
                int hora = data.getHour();
                int horaArredondadaParaBaixo = Math.floorDiv(hora, 8) * 8;
                return data.withHour(horaArredondadaParaBaixo).withMinute(0).withSecond(0).withNano(0).plusHours(offsetPeriodos * 8);
            case DIARIO:
                return data.plusDays(offsetPeriodos).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case SEMANAL:
                int weekNumber = data.get(WeekFields.ISO.weekOfWeekBasedYear());
                int year = data.get(WeekFields.ISO.weekBasedYear());
                YearWeek yearWeek = YearWeek.of(year, weekNumber);
                return yearWeek.atDay(DayOfWeek.MONDAY).atStartOfDay().plusWeeks(offsetPeriodos);
            case MENSAL:
                return data.plusMonths(offsetPeriodos).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case ANUAL:
                return data.plusYears(offsetPeriodos).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            default:
                return data.plusDays(offsetPeriodos).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
    }

    public static LocalDateTime getUltimaDataHorarioPeriodoCalendarioComOffset(LocalDateTime data, int offsetPeriodos,
            TamanhoBucket tamanhoBucket) {
        switch (tamanhoBucket) {
            case MEIA_HORA:
                int minuto = data.getMinute();
                int minutoArredondadoParaMeiaHoraBaixo = Math.floorDiv(minuto, 30);
                return data.withMinute(minutoArredondadoParaMeiaHoraBaixo).plusMinutes(offsetPeriodos * 30).withSecond(59).withNano(0);
            case HORARIO:
                return data.plusHours(offsetPeriodos).withMinute(59).withSecond(59).withNano(0);
            case TURNO:
                int hora = data.getHour();
                int horaArredondadaParaBaixo = Math.floorDiv(hora, 8) * 8;
                return data.withHour(horaArredondadaParaBaixo).withMinute(59).withSecond(59).withNano(0).plusHours(offsetPeriodos * 8);
            case DIARIO:
                return data.plusDays(offsetPeriodos).withHour(23).withMinute(59).withSecond(59).withNano(0);
            case SEMANAL:
                int weekNumber = data.get(WeekFields.ISO.weekOfWeekBasedYear());
                YearWeek yearWeek = YearWeek.of(data.getYear(), weekNumber);
                return yearWeek.atDay(DayOfWeek.MONDAY).atStartOfDay().plusWeeks(offsetPeriodos).withHour(23).withMinute(59).withSecond(59).withNano(0);
            case MENSAL:
                return data.plusMonths(offsetPeriodos).plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).minusSeconds(1);
            case ANUAL:
                return data.plusMonths(offsetPeriodos).plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).minusSeconds(1);
            default:
                return data.plusDays(offsetPeriodos).withHour(23).withMinute(59).withSecond(59).withNano(0);
        }
    }

    public int getPosicaoPeriodoAposOffsetDoInicioPeriodoReferencia(int periodoReferencia, int offsetPeriodos, TamanhoBucket tamanhoBucketOffset) {
        
        LocalDateTime inicioPeriodoAposReferencia = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                getPrimeiraDataHorarioPeriodo(periodoReferencia),
                offsetPeriodos, tamanhoBucketOffset);
        
        return getPosicaoPeriodo(inicioPeriodoAposReferencia);
        
    }
    public int getPosicaoPeriodoAposOffsetDoFimPeriodoReferencia(int periodoReferencia, int offsetPeriodos, TamanhoBucket tamanhoBucketOffset) {
        
        LocalDateTime inicioPeriodoAposReferencia = getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                getPrimeiraDataHorarioPeriodo(periodoReferencia + 1).minusNanos(1),
                offsetPeriodos, tamanhoBucketOffset);
        
        return getPosicaoPeriodo(inicioPeriodoAposReferencia);
        
    }
    
    public LocalDateTime getUltimoSegundoPeriodo(int posicaoPeriodo) {
        return getPrimeiraDataHorarioPeriodo(posicaoPeriodo + 1).minusSeconds(1);
    }
    
    public static LocalDateTime getUltimoSegundoData(LocalDate data) {
        return data.plusDays(1).atStartOfDay().minusSeconds(1);
    }
    
    /**
     * Traz a menor de duas datas
     *
     * @param data1
     * @param data2
     * @return
     */
    public static LocalDate getMinData(LocalDate data1, LocalDate data2) {
        if (data1 == null || data2 == null) {
            return null;
        } else {
            return (data1.isAfter(data2) ? data2 : data1);
        }
    }
    /**
     * Traz a menor de duas datas
     *
     * @param data1
     * @param data2
     * @return
     */
    public static LocalDateTime getMinDataHorario(LocalDateTime data1, LocalDateTime data2) {
        if (data1 == null || data2 == null) {
            return null;
        } else {
            return (data1.isAfter(data2) ? data2 : data1);
        }
    }

    /**
     * Traz a maior de duas datas
     *
     * @param data1
     * @param data2
     * @return
     */
    public static LocalDate getMaxData(LocalDate data1, LocalDate data2) {
        if (data1 == null || data2 == null) {
            return null;
        } else {
            return (data1.isAfter(data2) ? data1 : data2);
        }
    }
    /**
     * Traz a maior de duas datas
     *
     * @param data1
     * @param data2
     * @return
     */
    public static LocalDateTime getMaxDataHorario(LocalDateTime data1, LocalDateTime data2) {
        if (data1 == null || data2 == null) {
            return null;
        } else {
            return (data1.isAfter(data2) ? data1 : data2);
        }
    }

    /**
     * Calcula o número de dias ENTRE duas datas 
     * Exemplo : entre 04/10 e 05/10 o método retorna 1
     * Caso uma das datas seja nula retorna 0.
     *
     * @param dataInicial
     * @param dataFinal
     * @return
     */
    public static int calculaNumeroDiasEntreDatas(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial == null || dataFinal == null) {
            return 0;
        }
        return (int) DAYS.between(dataInicial, dataFinal);
    }
    /**
     * Calcula o número de dias (apenas parte inteira) ENTRE duas datas 
     * Exemplo : entre 04/10 e 05/10 o método retorna 1
     * Caso uma das datas seja nula retorna 0.
     *
     * @param dataInicial
     * @param dataFinal
     * @return
     */
    public static int calculaNumeroDiasEntreDataHorarios(LocalDateTime dataInicial, LocalDateTime dataFinal) {
        if (dataInicial == null || dataFinal == null) {
            return 0;
        }
        return (int) DAYS.between(dataInicial, dataFinal);
    }

    /**
     * Calcula o número de semanas entre 2 semanas input Ex: 201720 a 201720 : 0
     * 201720 a 201722 : 2 201721 a 201718 : -3
     *
     * @param semanaInicial YYYYWW
     * @param semanaFinal YYYYWW
     * @return
     */
    public static int calculaNumeroSemanasEntreSemanas(int semanaInicial, int semanaFinal) {
        LocalDate dtInicial = getUltimoDiaSemana(semanaInicial);
        LocalDate dtFinal = getUltimoDiaSemana(semanaFinal);

        return (int) (calculaNumeroDiasEntreDatas(dtInicial, dtFinal) / 7);
    }

    public static DateTimeFormatter getPatternHorario() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    }
    
    public static DateTimeFormatter getPatternDate() {
        return DateTimeFormatter.ISO_LOCAL_DATE;
    }

    public static DateTimeFormatter getPatternMonthYear() {
        return DateTimeFormatter.ofPattern("yyyyMM");
    }

    public static boolean verificaSeLocalDate(String data) {

        if (data == null) {
            return false;
        }

        try {
            stringToLocalDate(data);
            return true;
        } catch (DateTimeParseException dateTimeParseException) {
            /*
             * Este metodo e apenas um predicado de formato. Ausencia de valor e
             * tratada acima; aqui so convertemos falha de parse em false.
             */
            return false;
        }

    }

    public static LocalDate stringToLocalDate(String data) {
        
        data = data.trim();
        
        try {
            return LocalDate.parse(data);
        } catch (DateTimeParseException e1) {
            try {
                return stringToLocalDateTime(data).toLocalDate();
            } catch (DateTimeParseException e2) {
                try {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm");
                    return LocalDateTime.parse(data, dtf).toLocalDate();
                } catch (DateTimeParseException e3) {
                    try {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss");
                        return LocalDateTime.parse(data, dtf).toLocalDate();
                    } catch (DateTimeParseException e4) {
                        try {
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            return LocalDate.parse(data, dtf);
                        } catch (DateTimeParseException e5) {
                            try {
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");
                                return LocalDateTime.parse(data, dtf).toLocalDate();
                            } catch (DateTimeParseException e6) {
                                try {
                                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
                                    return LocalDateTime.parse(data, dtf).toLocalDate();
                                } catch (DateTimeParseException e7) {
                                    try {
                                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                                        return LocalDate.parse(data, dtf);
                                    } catch (DateTimeParseException e8) {
                                        /*
                                         * Todas as tentativas publicas de data falharam. Preservar a
                                         * ultima falha mantem o detalhe tecnico do formatter sem mudar
                                         * a mensagem funcional ja exposta pelos chamadores.
                                         */
                                        throw new DateTimeParseException("Incompatible date format : " + data, data, 0, e8);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }        
    }
    
    public static LocalDateTime stringToLocalDateTime(String data) {
        
        data = data.trim();
        
        try {
            return LocalDateTime.parse(data);
        } catch (DateTimeParseException e1) {
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm");
                return LocalDateTime.parse(data, dtf);
            } catch (DateTimeParseException e2) {
                try {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss");
                    return LocalDateTime.parse(data, dtf);
                } catch (DateTimeParseException e3) {
                    try {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        return LocalDate.parse(data, dtf).atStartOfDay();
                    } catch (DateTimeParseException e4) {
                        try {
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");
                            return LocalDateTime.parse(data, dtf);
                        } catch (DateTimeParseException e5) {
                            try {
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
                                return LocalDateTime.parse(data, dtf);
                            } catch (DateTimeParseException e6) {
                                try {DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                                    return LocalDate.parse(data, dtf).atStartOfDay();
                                } catch (DateTimeParseException e7) {
                                    /*
                                     * Todas as tentativas publicas de data/hora falharam. Preservar a
                                     * ultima falha mantem o detalhe tecnico do formatter sem mudar
                                     * a mensagem funcional ja exposta pelos chamadores.
                                     */
                                    throw new DateTimeParseException("Incompatible date format : " + data, data, 0, e7);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    public static LocalTime stringToLocalTime(String data) {

        data = data.trim();

        try {
            return LocalTime.parse(data);
        } catch (DateTimeParseException e1) {
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm");
                return LocalDateTime.parse(data,dtf).toLocalTime();
            } catch (DateTimeParseException e2) {
                try {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm:ss");
                    return LocalDateTime.parse(data, dtf).toLocalTime();
                } catch (DateTimeParseException e) {
                    /*
                     * Todas as tentativas publicas de hora falharam. Preservar a
                     * ultima falha mantem o detalhe tecnico do formatter sem mudar
                     * a mensagem funcional ja exposta pelos chamadores.
                     */
                    throw new DateTimeParseException("Incompatible date format : " + data, data, 0, e);
                }
            }
        }
    }

    /**
     * Retorna o total de horas + minutos + segundos entre 2 LocalDateTimes
     * @param init
     * @param end
     * @return 
     */
    public static String timeBetween(LocalDateTime init, LocalDateTime end) {
        long seconds = init.until(end, ChronoUnit.SECONDS) > 59 ? init.until(end, ChronoUnit.SECONDS) % 60 : init.until(end, ChronoUnit.SECONDS);
        long minutes = init.until(end, ChronoUnit.MINUTES) > 59 ? init.until(end, ChronoUnit.MINUTES) % 60 : init.until(end, ChronoUnit.MINUTES);
        long hour = init.until(end, ChronoUnit.HOURS);
        return hour + "h :" + minutes + "m :" + seconds + "s";
    }
    
    
    public String getDescricaoPeriodoDePosicaoPeriodo(int posicaoPeriodo) {
        LocalDateTime ultimaDataHorarioPeriodo = getUltimaDataHorarioPeriodo(posicaoPeriodo);
        return getDescricaoPeriodoDeLocalDateTime(ultimaDataHorarioPeriodo, getTamanhoBucket());
    }
    public static String getDescricaoPeriodoDeLocalDate(LocalDate localDate, Constantes.TamanhoBucket tamanhoBucket) {
        switch (tamanhoBucket) {
            case DIARIO:
                return localDate.toString();
            case SEMANAL:
                return getPrimeiroDiaSemana(localDate).toString();
            case MENSAL:
                return localDate.format(DateTimeFormatter.ofPattern("MMM/yyyy", Locale.ENGLISH));//, Locale.getDefault()));
            case ANUAL:
                return String.valueOf(localDate.getYear());
            default:
                return localDate.atStartOfDay().toString();
        }
    }
    public static String getDescricaoPeriodoDeLocalDateTime(LocalDateTime localDateTime, Constantes.TamanhoBucket tamanhoBucket) {
        switch (tamanhoBucket) {
            case DIARIO:
                return localDateTime.toLocalDate().toString();
            case SEMANAL:
                return getPrimeiroDiaSemana(localDateTime.toLocalDate()).toString();
            case MENSAL:
                return  localDateTime.format(DateTimeFormatter.ofPattern("MMM/yyyy", Locale.ENGLISH));//, Locale.getDefault()));
            case ANUAL:
                return  String.valueOf(localDateTime.getYear());
            default:
                return localDateTime.toString();
        }
    }
    
    /**
     * Exemplo 1: calendario = mensal e outro calendario = diario, posicaoOutroCalendario = 25/03/2022. Retorno : 03/2022
     * Exemplo 2: calendario = diario e outro calendario = mensal, posicaoOutroCalendario = 03/2022. Retorno : 31/03/2022
     * @param outroCalendario
     * @param posicaoPeriodoOutroCalendario
     * @return 
     */
    public int getPosicaoPeriodoDePosicaoPeriodoOutroCalendario(Calendario outroCalendario, int posicaoPeriodoOutroCalendario) {
        
        return getPosicaoPeriodo(outroCalendario.getPrimeiraDataHorarioPeriodo(posicaoPeriodoOutroCalendario + 1).minusNanos(1));
        
    }
    
    /**
     * Retorna o início do primeiro dia do período
     * @param posicaoPeriodo
     * @return 
     */
    public LocalDateTime getPrimeiroHorarioDiaInicialPeriodo(int posicaoPeriodo) {
        return getPrimeiraDataHorarioPeriodo(posicaoPeriodo).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
    /**
     * Retorna o início do último dia do período
     * @param posicaoPeriodo
     * @return 
     */
    public LocalDateTime getPrimeiroHorarioDiaFinalPeriodo(int posicaoPeriodo) {
        return getPrimeiraDataHorarioPeriodo(posicaoPeriodo + 1).minusNanos(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
    
    /**
     * 
     * @param tamanhoBucketAVerificar
     * @param tamanhoBucketReferencia
     * @return 1: a verificar mais agregado que referencia. 0: iguais. -1: a verificar menos agregado que referencia
     */
    public static int comparaNivelAgregacao(TamanhoBucket tamanhoBucketAVerificar, TamanhoBucket tamanhoBucketReferencia) {
        
        if (tamanhoBucketAVerificar.equals(tamanhoBucketReferencia)) return 0;
        
        switch (tamanhoBucketAVerificar) {
            case SEGUNDO: return -1;
            case MINUTO:
                switch(tamanhoBucketReferencia) {
                    case SEGUNDO: return +1;
                    default: return -1;
                }
            case SEXTO_HORA:
                switch(tamanhoBucketReferencia) {
                    case SEGUNDO: return +1;
                    case MINUTO: return +1;
                    default: return -1;
                }
            case QUARTO_HORA:
                switch(tamanhoBucketReferencia) {
                    case SEGUNDO: return +1;
                    case MINUTO: return +1;
                    case SEXTO_HORA: return +1;
                    default: return -1;
                }
            case MEIA_HORA:
                switch(tamanhoBucketReferencia) {
                    case SEGUNDO: return +1;
                    case MINUTO: return +1;
                    case SEXTO_HORA: return +1;
                    case QUARTO_HORA: return +1;
                    default: return -1;
                }
            case HORARIO:
                switch(tamanhoBucketReferencia) {
                    case SEGUNDO: return +1;
                    case MINUTO: return +1;
                    case SEXTO_HORA: return +1;
                    case QUARTO_HORA: return +1;
                    case MEIA_HORA: return +1;
                    default: return -1;
                }
            case TURNO:
                switch(tamanhoBucketReferencia) {
                    case SEGUNDO: return +1;
                    case MINUTO: return +1;
                    case SEXTO_HORA: return +1;
                    case QUARTO_HORA: return +1;
                    case MEIA_HORA: return +1;
                    case HORARIO: return +1;
                    default: return -1;
                }
            case DIARIO:
                switch(tamanhoBucketReferencia) {
                    case SEGUNDO: return +1;
                    case MINUTO: return +1;
                    case SEXTO_HORA: return +1;
                    case QUARTO_HORA: return +1;
                    case MEIA_HORA: return +1;
                    case HORARIO: return +1;
                    case TURNO: return +1;
                    default: return -1;
                }
            case SEMANAL:
                switch(tamanhoBucketReferencia) {
                    case SEGUNDO: return +1;
                    case MINUTO: return +1;
                    case SEXTO_HORA: return +1;
                    case QUARTO_HORA: return +1;
                    case MEIA_HORA: return +1;
                    case HORARIO: return +1;
                    case TURNO: return +1;
                    case DIARIO: return +1;
                    default: return -1;
                }
            case MENSAL:
                switch(tamanhoBucketReferencia) {
                    case ANUAL: return -1;
                    default: return +1;
                }
            case ANUAL:
                return +1;
            default: throw new IllegalArgumentException(
                    "Calendario.comparaNivelAgregacao does not support calendar bucket pair "
                            + tamanhoBucketAVerificar + " / " + tamanhoBucketReferencia);
                
        }
    }
    
    /**
     * Exemplo 1 : este calendário = mensal e bucket referencia (ex. dados capacidade) = diario
     * PosicaoPeriodo refere-se ao mês de maio. Retorno = 31
     * Exemplo 2 : este calendário = diario e bucket referencia mensal
     * PosicaoPeriodo refere-se ao dia 03/05. Retorno = 1/31 (premissa de linearização)
     * @param posicaoPeriodo relativos a este calendário, não ao calendário referência
     * @param tamanhoBucketReferencia ex. dado agrupado por dia, apesar do calendario planejamento em meses
     * @return 
     */
    public double getNumeroPeriodosNoBucketReferencia(int posicaoPeriodo, TamanhoBucket tamanhoBucketReferencia) {
        
        int comparacaoAgregacaoCalendarioReferencia = comparaNivelAgregacao(tamanhoBucketReferencia, tamanhoBucket);
        if (comparacaoAgregacaoCalendarioReferencia == 0) return 1;
        // calendário referência mais agregado : para cada período do calendário referência há múltiplos períodos deste calendário
        if (comparacaoAgregacaoCalendarioReferencia == 1) {
            LocalDateTime dataHorarioInicialPeriodo = getPrimeiraDataHorarioPeriodo(posicaoPeriodo);
            
            // data inicial no calendario referencia
            LocalDateTime dataHorarioInicialPeriodoReferencia = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataHorarioInicialPeriodo, 0, tamanhoBucketReferencia);
            // data do periodo +1 no calendario referencia
            LocalDateTime dataHorarioInicialAposPeriodoReferencia = getPrimeiraDataHorarioPeriodoCalendarioComOffset(dataHorarioInicialPeriodo, 1, tamanhoBucketReferencia);
            
            int posicaoIterada = getPosicaoPeriodo(dataHorarioInicialPeriodoReferencia);
            int posicaoAposFimPeriodoReferencia = getPosicaoPeriodo(dataHorarioInicialAposPeriodoReferencia);
            
            return 1d / (posicaoAposFimPeriodoReferencia - posicaoIterada);
        } else if (comparacaoAgregacaoCalendarioReferencia == -1) {
            
            LocalDateTime dataHorarioInicialPeriodo = getPrimeiraDataHorarioPeriodo(posicaoPeriodo);
            LocalDateTime dataHorarioInicialAposPeriodo = getPrimeiraDataHorarioPeriodo(posicaoPeriodo + 1);
            
            Calendario calendarioReferencia = Calendario.criaCalendarioPeriodosFuturosDeDatas(tamanhoBucketReferencia, dataHorarioInicialPeriodo, dataHorarioInicialAposPeriodo.minusNanos(1));

            return calendarioReferencia.getNumeroPeriodosFuturos();
            
        } else {
            throw new IllegalStateException(
                    "Calendario.getNumeroPeriodosNoBucketReferencia received invalid aggregation comparison "
                            + comparacaoAgregacaoCalendarioReferencia
                            + " for calendar bucket " + tamanhoBucket
                            + " and reference bucket " + tamanhoBucketReferencia);
        }
        
    }
    
    public double consolidaDadosNoCalendario(int posicaoPeriodoCalendario, TamanhoBucket tamanhoBucketDados, ToDoubleFunction<LocalDateTime> valorPorDataHorarioDados) {
        
        double numeroPeriodosNoBucketDados = getNumeroPeriodosNoBucketReferencia(0, tamanhoBucketDados);
        
        if (numeroPeriodosNoBucketDados == 1d) {
            return valorPorDataHorarioDados.applyAsDouble(getPrimeiraDataHorarioPeriodo(posicaoPeriodoCalendario));
        }
        // dado mais agregado que o calendário : split homogêneo do valor do dado
        // nesse caso, varrer um a um os períodos do calendário que fazem parte do periodo dos dados
        else if (numeroPeriodosNoBucketDados < 1d) {
            LocalDateTime primeiraDataHorarioPosicaoPeriodo = getPrimeiraDataHorarioPeriodo(posicaoPeriodoCalendario);
            // um localdatetime no bucket dos dados é necessário para se aplicar a função
            LocalDateTime primeiraDataHorarioPeriodoDados = getPrimeiraDataHorarioPeriodoCalendarioComOffset(primeiraDataHorarioPosicaoPeriodo, 0, tamanhoBucketDados);
            return numeroPeriodosNoBucketDados * valorPorDataHorarioDados.applyAsDouble(primeiraDataHorarioPeriodoDados);
        }
        // dado mais desagregado que o calendário
        // nesse caso, chamar a função uma vez para cada ocorrência dentro do período calendário
        else {
            LocalDateTime primeiraDataHorarioPosicaoPeriodo = getPrimeiraDataHorarioPeriodo(posicaoPeriodoCalendario);
            LocalDateTime primeiraDataHorarioAposPosicaoPeriodo = getPrimeiraDataHorarioPeriodo(posicaoPeriodoCalendario + 1);
            
            Calendario calendarioDados = criaCalendarioPeriodosFuturosDeDatas(tamanhoBucketDados, primeiraDataHorarioPosicaoPeriodo, primeiraDataHorarioAposPosicaoPeriodo.minusNanos(1));
            // um localdatetime no bucket dos dados é necessário para se aplicar a função
            LocalDateTime primeiraDataHorarioPeriodoDados = getPrimeiraDataHorarioPeriodoCalendarioComOffset(primeiraDataHorarioPosicaoPeriodo, 0, tamanhoBucketDados);
            LocalDateTime primeiraDataHorarioAposPeriodoDados = getPrimeiraDataHorarioPeriodoCalendarioComOffset(primeiraDataHorarioAposPosicaoPeriodo, 0, tamanhoBucketDados);
            
            double valorAcumulado = 0;
            for (int i=0; i < calendarioDados.getNumeroPeriodosFuturos(); i++) {
                LocalDateTime dataHorarioInicioPeriodoDados = calendarioDados.getPrimeiraDataHorarioPeriodo(i);
                valorAcumulado += valorPorDataHorarioDados.applyAsDouble(dataHorarioInicioPeriodoDados);
            }
            return valorAcumulado;
        }
                        
    }
    
    /**
     * Retorna o último período dentro do horizonte
     * Conta a partir do último segundo do periodo presente - 1
     * @param horizontePeriodos
     * @param tamahoBucket
     * @return 
     */
    public int getUltimoPeriodoFuturoEmHorizontePeriodos(int horizontePeriodos, TamanhoBucket tamahoBucket) {
        return getPosicaoPeriodoAposOffsetDoFimPeriodoReferencia(
                getPosicaoPeriodoPresente() - 1, horizontePeriodos, tamanhoBucket);
    }
    
    /**
     * Retorna formatter 'yyyy-MM-dd' para buckets diario/semanal/mensal 
     * e 'yyyy-MM-dd hh:mm:ss' para os demais buckets
     * @return 
     */
    public DateTimeFormatter getDateTimeFormatter() {
        if (getTamanhoBucket().equals(TamanhoBucket.ANUAL)) return DateTimeFormatter.ofPattern("yyyy");
        if (getTamanhoBucket().equals(TamanhoBucket.MENSAL)) return DateTimeFormatter.ofPattern("yyyy-MM");
        return (getTamanhoBucket().equals(TamanhoBucket.DIARIO) || getTamanhoBucket().equals(TamanhoBucket.SEMANAL)) ?
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
                : DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

    }
    
    public List<Integer> getListaPosicoesPeriodo() {
        return getMapaDatasHorarios().values().stream().sorted().collect(Collectors.toList());
    }
    
    public double getPercentualDoPeriodoCobertoPeloRange(int posicaoPeriodo, LocalDateTime dataHorarioInicialReferencia, LocalDateTime dataHorarioFinalReferencia) {
        LocalDateTime dataHorarioInicialPeriodo = getPrimeiraDataHorarioPeriodo(posicaoPeriodo);
        LocalDateTime dataHorarioFinalPeriodo = getUltimaDataHorarioPeriodo(posicaoPeriodo);
        
        LocalDateTime dataHorarioInicialReferenciaConsiderada;
        if (dataHorarioInicialReferencia.isBefore(dataHorarioInicialPeriodo)) {
            dataHorarioInicialReferenciaConsiderada = dataHorarioInicialPeriodo;
        } else if (dataHorarioInicialReferencia.isAfter(dataHorarioFinalPeriodo)) {
            return 0d; // não há overlap com período : o início já fica no fim do período
        } else {
            dataHorarioInicialReferenciaConsiderada = dataHorarioInicialReferencia;
        }
        
        LocalDateTime dataHorarioFinalReferenciaConsiderada;
        if (dataHorarioFinalReferencia.isAfter(dataHorarioFinalPeriodo)) {
            dataHorarioFinalReferenciaConsiderada = dataHorarioFinalPeriodo;
        } else if (dataHorarioFinalReferencia.isBefore(dataHorarioInicialPeriodo)) {
            return 0d; // não há overlap com período : o início já fica no fim do período
        } else {
            dataHorarioFinalReferenciaConsiderada = dataHorarioFinalReferencia;
        }
        
        if (dataHorarioInicialReferenciaConsiderada.isAfter(dataHorarioFinalReferenciaConsiderada)) return 0;
        
        return ((double) (dataHorarioFinalReferenciaConsiderada.toEpochSecond(ZoneOffset.UTC) - dataHorarioInicialReferenciaConsiderada.toEpochSecond(ZoneOffset.UTC)))
                / ((double) (dataHorarioFinalPeriodo.toEpochSecond(ZoneOffset.UTC) - dataHorarioInicialPeriodo.toEpochSecond(ZoneOffset.UTC)));
    
    }
    
    public double getNumeroMedioPeriodosNoAno() {
        switch (tamanhoBucket) {
            case ANUAL: return 1;
            case MENSAL: return 12;
            case SEMANAL: return 52.1775;
            case DIARIO: return 365.25;
            case HORARIO: return (365.25 * 24);
            default : throw new IllegalArgumentException(
                    "Calendario.getNumeroMedioPeriodosNoAno does not support calendar bucket "
                            + tamanhoBucket);
        }
    }

    public Map<Integer, Double> getNumeroHorasPorPeriodoDeRangeDatasHorarios(
            LocalDateTime dataHorarioInicial, LocalDateTime dataHorarioFinal) {

        Map<Integer,Double> horasPorPeriodo = new HashMap<>();

        Integer posicaoPeriodoInicio = getPosicaoPeriodo(dataHorarioInicial);
        Integer posicaoPeriodoFim = getPosicaoPeriodo(dataHorarioFinal);

        double numeroHorasNoPrimeiroPeriodo = Duration
                .between(
                        dataHorarioInicial,
                        Calendario.getMinDataHorario(getUltimaDataHorarioPeriodo(posicaoPeriodoInicio), dataHorarioFinal))
                .toSeconds() / 3600d;
        double numeroHorasNoUltimoPeriodo = Duration
                .between(
                        Calendario.getMaxDataHorario(getPrimeiraDataHorarioPeriodo(posicaoPeriodoFim), dataHorarioInicial),
                        dataHorarioFinal)
                .toSeconds() / 3600d;

        for (int i = posicaoPeriodoInicio; i <= posicaoPeriodoFim; i++) {
            double numeroHoras;
            if (i == posicaoPeriodoInicio) {
                numeroHoras = numeroHorasNoPrimeiroPeriodo;
            } else if (i == posicaoPeriodoFim) {
                numeroHoras = numeroHorasNoUltimoPeriodo;
            } else {
                numeroHoras = getNumeroDiasNoPeriodo(i) * 24;
            }

            if (numeroHoras > 0) {
                horasPorPeriodo.put(i, numeroHoras);
            }
        }

        return horasPorPeriodo;

    }

    public List<LocalDate> getDatasEmPeriodo(int posicaoPeriodo) {

        List<LocalDate> datasEmPeriodo = new ArrayList<>();

        LocalDate dataInicial = getPrimeiraDataPeriodo(posicaoPeriodo);
        LocalDate dataFinal = getUltimaDataPeriodo(posicaoPeriodo);

        LocalDate dataAtual = dataInicial;
        while (!dataAtual.isAfter(dataFinal)) {
            datasEmPeriodo.add(dataAtual);
            dataAtual = dataAtual.plusDays(1);
        }

        return datasEmPeriodo;

    }

}
