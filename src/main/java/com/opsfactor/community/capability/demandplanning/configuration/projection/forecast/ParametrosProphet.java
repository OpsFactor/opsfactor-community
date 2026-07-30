package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract;
import lombok.Getter;
import lombok.Setter;

/**
 * Parametros configuraveis do modelo Prophet.
 *
 * <p>O Community nao executa Prophet. Esta classe fica no modelo compartilhado
 * porque `ParametrosForecastProjection` e as entidades de configuracao sao o
 * contrato comum consumido pelo overlay Enterprise. A borda Community continua
 * bloqueando o modelo e qualquer parametro Prophet nao neutro antes de salvar
 * ou executar.</p>
 */
@Getter
@Setter
public class ParametrosProphet {

    /**
     * Usa o default do Prophet para a forca da sazonalidade quando verdadeiro.
     */
    public boolean prophetAutoSeasonalityPriorScale;

    /**
     * Forca manual da sazonalidade. Default historico: 10.
     */
    public double prophetSeasonalityPriorScale;

    /**
     * Usa o default do Prophet para flexibilidade de changepoints quando
     * verdadeiro.
     */
    public boolean prophetAutoChangepointPriorScale;

    /**
     * Flexibilidade manual de changepoints. Default historico: 0.05.
     */
    public double prophetChangepointPriorScale;

    /**
     * Usa sazonalidade anual default do Prophet quando verdadeiro.
     */
    public boolean prophetAutoYearlyFourierOrder;

    /**
     * Fourier order manual para sazonalidade anual. Default historico: 10.
     */
    public int prophetYearlyFourierOrder;

    public ParametrosProphet(
            Boolean prophetAutoSeasonalityPriorScale,
            Double prophetSeasonalityPriorScale,
            Boolean prophetAutoChangepointPriorScale,
            Double prophetChangepointPriorScale,
            Boolean prophetAutoYearlyFourierOrder,
            Integer prophetYearlyFourierOrder) {

        this.prophetAutoSeasonalityPriorScale =
                prophetAutoSeasonalityPriorScale == null || prophetAutoSeasonalityPriorScale;
        this.prophetSeasonalityPriorScale =
                prophetSeasonalityPriorScale == null ? 10.0d : prophetSeasonalityPriorScale;
        this.prophetAutoChangepointPriorScale =
                prophetAutoChangepointPriorScale == null || prophetAutoChangepointPriorScale;
        this.prophetChangepointPriorScale =
                prophetChangepointPriorScale == null ? 0.05d : prophetChangepointPriorScale;
        this.prophetAutoYearlyFourierOrder =
                prophetAutoYearlyFourierOrder == null || prophetAutoYearlyFourierOrder;
        this.prophetYearlyFourierOrder =
                prophetYearlyFourierOrder == null ? 10 : prophetYearlyFourierOrder;

    }

    public ParametrosProphet(
            ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract) {

        this(
                parametrosModeloEstatisticoAbstract.getProphetAutoSeasonalityPriorScale(),
                parametrosModeloEstatisticoAbstract.getProphetSeasonalityPriorScale(),
                parametrosModeloEstatisticoAbstract.getProphetAutoChangepointPriorScale(),
                parametrosModeloEstatisticoAbstract.getProphetChangepointPriorScale(),
                parametrosModeloEstatisticoAbstract.getProphetAutoYearlyFourierOrder(),
                parametrosModeloEstatisticoAbstract.getProphetYearlyFourierOrder());

    }

    public ParametrosProphet(
            ParametrosProphet parametrosProphet) {

        this(
                parametrosProphet == null ? null : parametrosProphet.prophetAutoSeasonalityPriorScale,
                parametrosProphet == null ? null : parametrosProphet.prophetSeasonalityPriorScale,
                parametrosProphet == null ? null : parametrosProphet.prophetAutoChangepointPriorScale,
                parametrosProphet == null ? null : parametrosProphet.prophetChangepointPriorScale,
                parametrosProphet == null ? null : parametrosProphet.prophetAutoYearlyFourierOrder,
                parametrosProphet == null ? null : parametrosProphet.prophetYearlyFourierOrder);

    }

}
