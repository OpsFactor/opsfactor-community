package com.opsfactor.community.capability.planningbook.keyfigure.domain;

import com.opsfactor.community.platform.utility.Constantes;

/**
 * Identidade standard de um preco medio derivado no Demand Planning Book.
 *
 * <p>O valor exibido e a razao entre as somas de vendas monetarias e demanda
 * direta. Por isso a grade nunca deve soma-lo diretamente nos niveis
 * agregados, nem permitir edicao manual.</p>
 */
public class KeyFigureStandardPrecoMedioDemandPlanning extends KeyFigureStandard {

    private final Constantes.TipoValor tipoValor;

    /** Cria a identidade de preco medio Gross ou Net. */
    public KeyFigureStandardPrecoMedioDemandPlanning(
            KeyFigureStandardEnum keyFigureStandardEnum) {

        super(keyFigureStandardEnum);

        tipoValor = switch (keyFigureStandardEnum) {
            case PRECO_MEDIO_GROSS -> Constantes.TipoValor.GROSS;
            case PRECO_MEDIO_NET -> Constantes.TipoValor.NET;
            default -> throw new IllegalArgumentException(
                    "Key Figure "
                            + keyFigureStandardEnum
                            + " does not represent an average Demand Planning price.");
        };

    }

    /** Informa se a serie usa a base monetaria Gross ou Net. */
    public Constantes.TipoValor getTipoValor() {

        return tipoValor;

    }

    /** Preco medio e sempre derivado das series fonte. */
    @Override
    public boolean getPadraoPermiteEdicao() {

        return false;

    }

    /** A grade recebe a KF somente como leitura. */
    @Override
    public EditMode getEditModePadrao() {

        return EditMode.NOEDIT;

    }
}
