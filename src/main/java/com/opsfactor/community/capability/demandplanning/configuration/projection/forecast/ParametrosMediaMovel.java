package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParametrosMediaMovel {

    /**
     * Janela historica, em dias, usada pelo modelo Moving Average.
     *
     * <p>{@code null} significa "usar default publico". Valor preenchido menor
     * ou igual a zero e configuracao invalida e deve falhar aqui, antes da
     * engine converter dias em periodos e executar o calculo estatistico.</p>
     */
    public Integer diasHistoricosMediaMovel;

    public ParametrosMediaMovel(Integer diasHistoricosMediaMovel) {

        this.diasHistoricosMediaMovel = getDiasHistoricosMediaMovel(
                diasHistoricosMediaMovel);

    }

    /**
     * Construtor com a entidade que representa as configurações nível cluster locations / cluster produtos DP
     * @param parametrosModeloEstatisticoAbstract
     */
    public ParametrosMediaMovel(
            ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract) {
        this(parametrosModeloEstatisticoAbstract.getDiasMediaMovelDp());
    }

    public ParametrosMediaMovel(
            ParametrosMediaMovel parametrosMediaMovel) {
        this(parametrosMediaMovel.getDiasHistoricosMediaMovel());
    }

    private static int getDiasHistoricosMediaMovel(
            Integer diasHistoricosMediaMovel) {

        if (diasHistoricosMediaMovel == null) {
            return Constantes.DP_PADRAO_DIAS_MEDIA_MOVEL;
        }
        if (diasHistoricosMediaMovel <= 0) {
            throw new IllegalArgumentException(
                    "Moving Average historical window must be positive.");
        }

        return diasHistoricosMediaMovel;

    }

}
