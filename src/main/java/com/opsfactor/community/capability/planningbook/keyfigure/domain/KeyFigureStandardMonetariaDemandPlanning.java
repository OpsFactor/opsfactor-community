package com.opsfactor.community.capability.planningbook.keyfigure.domain;

import com.opsfactor.community.platform.utility.Constantes;

/**
 * Identidade standard de uma venda monetaria derivada da demanda direta.
 *
 * <p>A classe nao carrega preco nem calcula valores. Ela permanece no modulo
 * compartilhado somente para que Gross/Net Sales preservem a mesma identidade
 * em configuracoes serializadas. O Community rejeita essas key figures antes
 * da projection; a materializacao economica pertence ao overlay Enterprise.</p>
 */
public class KeyFigureStandardMonetariaDemandPlanning extends KeyFigureStandard {

    private final Constantes.TipoValor tipoValor;

    /**
     * Cria a identidade monetaria e impede o uso acidental para outra key
     * figure standard de Demand Planning.
     */
    public KeyFigureStandardMonetariaDemandPlanning(
            KeyFigureStandardEnum keyFigureStandardEnum) {

        super(keyFigureStandardEnum);

        tipoValor = switch (keyFigureStandardEnum) {
            case VENDAS_GROSS -> Constantes.TipoValor.GROSS;
            case VENDAS_NET -> Constantes.TipoValor.NET;
            default -> throw new IllegalArgumentException(
                    "Key Figure "
                            + keyFigureStandardEnum
                            + " does not represent a monetary Demand Planning sale.");
        };

    }

    /**
     * Informa se a identidade representa a serie de preco gross ou net.
     */
    public Constantes.TipoValor getTipoValor() {

        return tipoValor;

    }

    /**
     * A capability Enterprise definira a editabilidade efetiva por celula.
     */
    @Override
    public boolean getPadraoPermiteEdicao() {

        return true;

    }

    /**
     * Gross/Net seguem o contrato tecnico de uma edicao de celula de demanda.
     */
    @Override
    public EditMode getEditModePadrao() {

        return EditMode.CELLEDIT;

    }
}
