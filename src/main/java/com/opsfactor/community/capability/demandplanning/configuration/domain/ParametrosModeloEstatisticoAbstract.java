package com.opsfactor.community.capability.demandplanning.configuration.domain;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;

/**
 * Base persistida dos parametros de modelos estatisticos de Demand Planning.
 *
 * <p>O Community mantem o enum completo para desserializar/configurar payloads
 * compartilhados com o front e para conseguir rejeitar modelos Enterprise com
 * erro explicito. Apenas MM, RMM, ARIMA, Holt-Winters e Exponential Smoothing
 * possuem execucao Community.</p>
 */
@Getter
@Setter
@MappedSuperclass // não é um Entity!
@AllArgsConstructor
@NoArgsConstructor
public abstract class ParametrosModeloEstatisticoAbstract {

    // DEMAND PLANNING --------------------------------------------------------------------------------------------------------------------------------
    @Enumerated(EnumType.ORDINAL)
    private Constantes.DPModeloEstatistico dpModeloEstatistico;

    private Integer diasMediaMovelDp; // # dias passados usados no calculo MM/RMM e no split Historical Sales entre materiais/locations

    // parâmetro usado para Exponential Smoothing + Holt Winters. se nulo = seleção automática
    private Double alfa;
    // parâmetro usado para Holt Winters. se nulo = seleção automática
    private Double beta;
    // parâmetro usado para Holt Winters. se nulo = seleção automática
    private Double gama;

    // Split
    @Enumerated(EnumType.ORDINAL)
    private Constantes.DPModeloSplit dpModeloSplit;
    private Integer numeroDiasSplitTopDown; // apenas para dpModeloSplit = HISTORICAL_SALES

    /*
     * Modelo de uplift pos-forecast. Community persiste apenas DESATIVADO e
     * bloqueia qualquer tentativa funcional na borda de DTO. O Enterprise usa
     * o mesmo campo para ativar Event Uplift sem duplicar a entidade de
     * parametros estatisticos.
     */
    @Enumerated(EnumType.ORDINAL)
    private Constantes.DPModeloUplift modeloUplift;

    /*
     * Parametros transicionais de tratamento historico. Community persiste
     * apenas defaults neutros e bloqueia qualquer uso funcional no mapper. O
     * Enterprise reutiliza os mesmos campos para habilitar a limpeza por
     * percentis sem duplicar a entidade de configuracao.
     */
    private Boolean consideraDadosEstoque;
    @Enumerated(EnumType.ORDINAL)
    private Constantes.DPModeloNormalizacao modeloNormalizacao;
    private Integer diasHistoricosNormalizacao;
    private Boolean habilitaLimpezaHistoricoPercentilSuperior;
    private Double percentilSuperiorLimpezaHistorico;
    private Boolean habilitaLimpezaHistoricoPercentilInferior;
    private Double percentilInferiorLimpezaHistorico;

    /*
     * Parametros Prophet ficam na entidade compartilhada porque o Enterprise
     * reutiliza a mesma tabela/configuracao. O Community conserva os campos
     * apenas para rejeicao defensiva e nao executa Prophet.
     */
    private Boolean prophetAutoSeasonalityPriorScale;
    private Double prophetSeasonalityPriorScale;
    private Boolean prophetAutoChangepointPriorScale;
    private Double prophetChangepointPriorScale;
    private Boolean prophetAutoYearlyFourierOrder;
    private Integer prophetYearlyFourierOrder;

    /*
     * Parametro transicional do Chronos/foundation model. O Community conserva
     * o campo para que configuracoes Enterprise compartilhem a mesma base, mas
     * continua bloqueando `CHRONOS` e qualquer valor ativo na borda.
     */
    private Boolean chronosForcaForecastAgregado;

    // --------------------------------------------------------------------------------------------------------------------------------------------

    public String getDescricaoModeloEstatistico() {
        Constantes.DPModeloEstatistico dpModeloEstatisticoConsiderado = (dpModeloEstatistico == null) ? Constantes.DPModeloEstatistico.MM : dpModeloEstatistico;
        switch (dpModeloEstatisticoConsiderado) {
            case ES:
                return "Exponential Smoothing (alpha="
                        + ((alfa == null ? "auto" : alfa))
                        + ")";
            case ETS:
                return "ETS";
            case ARIMA:
                return "ARIMA";
            case HOLT_WINTERS:
                return "Holt Winters (alpha="
                        + ((alfa == null ? "auto" : alfa))
                        + ", beta="
                        + ((beta == null ? "auto" : beta))
                        + ", gamma="
                        + ((gama == null ? "auto" : gama))
                        + ")";
            case MM:
                return "Moving Average (# days=" + ((diasMediaMovelDp == null) ? 30 : diasMediaMovelDp) + ")";
            case RMM:
                return "Rolling Moving Average (# days=" + ((diasMediaMovelDp == null) ? 30 : diasMediaMovelDp) + ")";
            case STL:
                return "Seasonal-Trend Loess";
            case TBATS:
                return "TBATS";
            case PROPHET:
                return "Prophet";
            case SNAIVE:
                return "Seasonal Naive";
            case CHRONOS:
                return "Chronos";
            default:
                return dpModeloEstatisticoConsiderado.toString();
        }
    }

    /**
     * Cria uma fotografia independente dos parâmetros estatísticos.
     *
     * <p>Este construtor é usado por entidades que materializam parâmetros
     * derivados de uma configuração já existente. No Community ele é mantido
     * somente para preservar as entidades de parametrização; a execução de
     * auto-fit e os resultados de avaliação são responsabilidade do
     * Enterprise.</p>
     *
     * @param parametrosModeloEstatisticoAbstract parâmetros estatísticos já
     *                                           calculados ou configurados
     */
    public ParametrosModeloEstatisticoAbstract(ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract) {
        this(
                parametrosModeloEstatisticoAbstract.getDpModeloEstatistico(),
                parametrosModeloEstatisticoAbstract.getDiasMediaMovelDp(),
                parametrosModeloEstatisticoAbstract.getAlfa(),
                parametrosModeloEstatisticoAbstract.getBeta(),
                parametrosModeloEstatisticoAbstract.getGama(),
                parametrosModeloEstatisticoAbstract.getDpModeloSplit(),
                parametrosModeloEstatisticoAbstract.getNumeroDiasSplitTopDown(),
                parametrosModeloEstatisticoAbstract.getModeloUplift(),
                parametrosModeloEstatisticoAbstract.getConsideraDadosEstoque(),
                parametrosModeloEstatisticoAbstract.getModeloNormalizacao(),
                parametrosModeloEstatisticoAbstract.getDiasHistoricosNormalizacao(),
                parametrosModeloEstatisticoAbstract.getHabilitaLimpezaHistoricoPercentilSuperior(),
                parametrosModeloEstatisticoAbstract.getPercentilSuperiorLimpezaHistorico(),
                parametrosModeloEstatisticoAbstract.getHabilitaLimpezaHistoricoPercentilInferior(),
                parametrosModeloEstatisticoAbstract.getPercentilInferiorLimpezaHistorico(),
                parametrosModeloEstatisticoAbstract.getProphetAutoSeasonalityPriorScale(),
                parametrosModeloEstatisticoAbstract.getProphetSeasonalityPriorScale(),
                parametrosModeloEstatisticoAbstract.getProphetAutoChangepointPriorScale(),
                parametrosModeloEstatisticoAbstract.getProphetChangepointPriorScale(),
                parametrosModeloEstatisticoAbstract.getProphetAutoYearlyFourierOrder(),
                parametrosModeloEstatisticoAbstract.getProphetYearlyFourierOrder(),
                parametrosModeloEstatisticoAbstract.getChronosForcaForecastAgregado());
    }

    /**
     * Cria uma fotografia persistível a partir dos parâmetros carregados para
     * uma rodada de forecast.
     *
     * <p>A conversão fica centralizada nesta classe porque entidades de
     * parametrização podem ser criadas a partir de projections em fluxos
     * diferentes. No Community esses snapshots não devem disparar auto-fit; eles
     * apenas reaproveitam o mesmo contrato de parâmetros estatísticos.</p>
     *
     * @param parametrosForecastProjection parâmetros já resolvidos pela factory
     *                                     de forecast
     */
    public ParametrosModeloEstatisticoAbstract(ParametrosForecastProjection parametrosForecastProjection) {
        this(
                parametrosForecastProjection.getDpModeloEstatistico(),
                (parametrosForecastProjection.getParametrosMediaMovel() == null) ? null : parametrosForecastProjection.getParametrosMediaMovel().getDiasHistoricosMediaMovel(),
                (parametrosForecastProjection.getParametrosHoltWinters() == null) ? null : parametrosForecastProjection.getParametrosHoltWinters().getAlfa(),
                (parametrosForecastProjection.getParametrosHoltWinters() == null) ? null : parametrosForecastProjection.getParametrosHoltWinters().getBeta(),
                (parametrosForecastProjection.getParametrosHoltWinters() == null) ? null : parametrosForecastProjection.getParametrosHoltWinters().getGama(),
                parametrosForecastProjection.getDpModeloSplit(),
                parametrosForecastProjection.getNumeroDiasSplitTopDown(),
                parametrosForecastProjection.getModeloUplift(),
                parametrosForecastProjection.getParametrosLimpezaHistoricoForecast() == null
                        ? null
                        : parametrosForecastProjection.getParametrosLimpezaHistoricoForecast().isConsideraDadosEstoque(),
                parametrosForecastProjection.getParametrosLimpezaHistoricoForecast() == null
                        ? null
                        : parametrosForecastProjection.getParametrosLimpezaHistoricoForecast().getModeloNormalizacao(),
                parametrosForecastProjection.getParametrosLimpezaHistoricoForecast() == null
                        ? null
                        : parametrosForecastProjection.getParametrosLimpezaHistoricoForecast().getDiasHistoricosNormalizacao(),
                parametrosForecastProjection.getParametrosLimpezaHistoricoForecast() == null
                        ? null
                        : parametrosForecastProjection.getParametrosLimpezaHistoricoForecast().isHabilitaLimpezaHistoricoPercentilSuperior(),
                parametrosForecastProjection.getParametrosLimpezaHistoricoForecast() == null
                        ? null
                        : parametrosForecastProjection.getParametrosLimpezaHistoricoForecast().getPercentilSuperiorLimpezaHistorico(),
                parametrosForecastProjection.getParametrosLimpezaHistoricoForecast() == null
                        ? null
                        : parametrosForecastProjection.getParametrosLimpezaHistoricoForecast().isHabilitaLimpezaHistoricoPercentilInferior(),
                parametrosForecastProjection.getParametrosLimpezaHistoricoForecast() == null
                        ? null
                        : parametrosForecastProjection.getParametrosLimpezaHistoricoForecast().getPercentilInferiorLimpezaHistorico(),
                (parametrosForecastProjection.getParametrosProphet() == null) ? null : parametrosForecastProjection.getParametrosProphet().isProphetAutoSeasonalityPriorScale(),
                (parametrosForecastProjection.getParametrosProphet() == null) ? null : parametrosForecastProjection.getParametrosProphet().getProphetSeasonalityPriorScale(),
                (parametrosForecastProjection.getParametrosProphet() == null) ? null : parametrosForecastProjection.getParametrosProphet().isProphetAutoChangepointPriorScale(),
                (parametrosForecastProjection.getParametrosProphet() == null) ? null : parametrosForecastProjection.getParametrosProphet().getProphetChangepointPriorScale(),
                (parametrosForecastProjection.getParametrosProphet() == null) ? null : parametrosForecastProjection.getParametrosProphet().isProphetAutoYearlyFourierOrder(),
                (parametrosForecastProjection.getParametrosProphet() == null) ? null : parametrosForecastProjection.getParametrosProphet().getProphetYearlyFourierOrder(),
                (parametrosForecastProjection.getParametrosChronos() == null) ? null : parametrosForecastProjection.getParametrosChronos().isForceAggregatedForecast());
    }

}
