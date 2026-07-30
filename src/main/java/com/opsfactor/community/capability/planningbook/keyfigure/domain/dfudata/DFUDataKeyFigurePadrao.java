package com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Valor numerico padrao de uma Key Figure por DFU/data.
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
@SuperBuilder
public class DFUDataKeyFigurePadrao extends DFUDataKeyFigureAbstract {
    
    /**
     * Valor cadastrado/calculado. `null` significa ausencia fisica de valor e
     * e lido como zero em calculos agregados.
     */
    private Double valor;

    public double getValor() {

        if (valor == null) {
            return 0.0d;
        }

        if (!Double.isFinite(valor)) {
            throw new IllegalStateException(
                    "DFU key figure value must be finite for "
                            + getContextoValorParaMensagem()
                            + ": "
                            + valor
                            + ".");
        }
        return valor;

    }

    private String getContextoValorParaMensagem() {

        return "key figure "
                + getKeyFigureIdParaMensagem()
                + " / material "
                + getMaterialIdParaMensagem()
                + " / location "
                + getLocationIdParaMensagem()
                + " / reference date "
                + getDataParaMensagem();

    }

    private String getKeyFigureIdParaMensagem() {

        return getKeyFigure() == null ? "<sem-key-figure>" : String.valueOf(getKeyFigure().getId());

    }

    private String getMaterialIdParaMensagem() {

        return getProduto() == null ? "<sem-material>" : String.valueOf(getProduto().getId());

    }

    private String getLocationIdParaMensagem() {

        return getLocation() == null ? "<sem-location>" : String.valueOf(getLocation().getId());

    }

    private String getDataParaMensagem() {

        return getData() == null ? "<sem-data>" : String.valueOf(getData());

    }

}
