package com.opsfactor.community.capability.planningbook.keyfigure.domain;

import com.opsfactor.community.platform.utility.MetodosUtilidade;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Implementacao de Key Figure padrao baseada em {@link KeyFigureStandardEnum}.
 *
 * <p>O objeto e deliberadamente fino: descricao, id e modo de edicao nascem do
 * enum para que projections, RuntimeInfo e Planning Book usem o mesmo catalogo
 * estatico da edicao Community.</p>
 */
@EqualsAndHashCode(of = "keyFigureStandardEnum")
public class KeyFigureStandard implements KeyFigureInterface<KeyFigureStandard> {
    
    /**
     * Valor canonico da Key Figure padrao.
     */
    @Getter
    KeyFigureStandardEnum keyFigureStandardEnum;

    public KeyFigureStandard (KeyFigureStandardEnum keyFigureStandardEnum) {
        this.keyFigureStandardEnum = keyFigureStandardEnum;
    }
    
    @Override
    public String getId() {
        return MetodosUtilidade.getValorJsonPropertyDeEnum(keyFigureStandardEnum);
    }

    @Override
    public String getDescricao() {
        return MetodosUtilidade.getValorJsonPropertyDeEnum(keyFigureStandardEnum);
    }

    @Override
    public TipoKeyFigure getTipoKeyFigure() {
        return TipoKeyFigure.STANDARD;
    }

    @Override
    public boolean getPadraoPermiteEdicao() {
        switch (keyFigureStandardEnum.getEditMode()) {
            case DETAIL_OR_CELL_EDIT:
                return true;
            case CELLEDIT:
                return true;
        }
        return false;
    }

    @Override
    public EditMode getEditModePadrao() {
        return keyFigureStandardEnum.getEditMode();
    }

    @Override
    public ModeloAgregacaoKeyFigure getModeloAgregacaoKeyFigure() {

        return keyFigureStandardEnum.getModeloAgregacaoKeyFigure();

    }

}
