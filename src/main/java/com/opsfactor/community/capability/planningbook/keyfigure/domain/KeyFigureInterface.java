package com.opsfactor.community.capability.planningbook.keyfigure.domain;

/**
 * Contrato minimo de uma Key Figure disponivel para projections e Planning Book.
 *
 * <p>No Community as implementacoes funcionais sao as Key Figures padrao
 * filtradas pelos catalogos da edicao. Key Figures customizadas e selecao
 * dinamica por view pertencem ao Enterprise, mas o tipo permanece generico para
 * manter a fronteira de comparacao/ordenacao entre implementacoes.</p>
 */
public interface KeyFigureInterface <KF extends KeyFigureInterface> extends Comparable<KF> {
    
    public enum TipoKeyFigure {
        STANDARD, CUSTOM   
    }

    /** Define como valores de folha devem ser consolidados no Planning Book. */
    public enum ModeloAgregacaoKeyFigure {
        PADRAO,
        RELACAO_ENTRE_VALORES,
        RAZAO_ENTRE_SOMAS,
        COBERTURA_ESTOQUE
    }

    public String getId();
    public String getDescricao();
    public TipoKeyFigure getTipoKeyFigure();
    public boolean getPadraoPermiteEdicao();
    public EditMode getEditModePadrao();

    /**
     * Custom Key Figures e KFs legadas seguem soma por padrao. Relacoes precisam declarar
     * explicitamente seus componentes para que pais nao somem taxas.
     */
    default ModeloAgregacaoKeyFigure getModeloAgregacaoKeyFigure() {

        return ModeloAgregacaoKeyFigure.PADRAO;

    }

    @Override
    default int compareTo(KF o) {
        if (o == null) {
            return 1;
        }
        return this.getId().compareTo(o.getId());
    }
    
}
