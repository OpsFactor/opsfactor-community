package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParametrosHoltWinters {

    public boolean autoSelectionAlfa;
    public Double alfa;
    public boolean autoSelectionBeta;
    public Double beta;
    public boolean autoSelectionGama;
    public Double gama;

    public ParametrosHoltWinters(Double alfa, Double beta, Double gama) {
        autoSelectionAlfa = (alfa == null);
        autoSelectionBeta = (beta == null);
        autoSelectionGama = (gama == null);

        this.alfa = alfa;
        this.beta = beta;
        this.gama = gama;
    }

    /**
     * Construtor com a entidade que representa as configurações nível cluster locations / cluster produtos DP
     * @param parametrosModeloEstatisticoAbstract
     */
    public ParametrosHoltWinters(
            ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract) {
        this(
                parametrosModeloEstatisticoAbstract.getAlfa(),
                parametrosModeloEstatisticoAbstract.getBeta(),
                parametrosModeloEstatisticoAbstract.getGama());
    }

    public ParametrosHoltWinters(
            ParametrosHoltWinters parametrosHoltWinters) {
        this(
                parametrosHoltWinters.getAlfa(),
                parametrosHoltWinters.getBeta(),
                parametrosHoltWinters.getGama());
    }

}
