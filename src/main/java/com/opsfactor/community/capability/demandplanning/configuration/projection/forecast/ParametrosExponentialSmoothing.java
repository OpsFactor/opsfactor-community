package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParametrosExponentialSmoothing {

    public boolean autoSelectionAlfa;
    public Double alfa;

    public ParametrosExponentialSmoothing() {
        autoSelectionAlfa = true;
    }

    public ParametrosExponentialSmoothing(Double alfa) {
        autoSelectionAlfa = (alfa == null);
        this.alfa = alfa;
    }

    /**
     * Construtor com a entidade que representa as configurações nível cluster locations / cluster produtos DP
     * @param parametrosModeloEstatisticoAbstract
     */
    public ParametrosExponentialSmoothing(
            ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract) {
        this(parametrosModeloEstatisticoAbstract.getAlfa());
    }

    public ParametrosExponentialSmoothing(
            ParametrosExponentialSmoothing parametrosExponentialSmoothing) {
        this(parametrosExponentialSmoothing.getAlfa());
    }

}
