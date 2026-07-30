package com.opsfactor.community.capability.masterdata.production.productionversion.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import lombok.*;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;

/**
 * Componente de uma versao de producao paralela.
 *
 * <p>No Community, estes registros sao apenas parte do schema compartilhado e
 * nao entram no motor heuristico. O Enterprise pode consumi-los para
 * parallel routing/output.</p>
 */
@Entity
@Data
@ToString(of="versaoProducaoParalelaComponenteCompositeKey")
@EqualsAndHashCode(of = "versaoProducaoParalelaComponenteCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
public class VersaoProducaoParalelaComponente {

    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private VersaoProducaoParalelaComponenteCompositeKey versaoProducaoParalelaComponenteCompositeKey;

    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class VersaoProducaoParalelaComponenteCompositeKey implements Serializable {
        
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private VersaoProducaoParalela versaoProducaoParalela;
        
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Roteiro roteiro;
        
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private ListaTecnica listaTecnica;

    }
    
    // para cada 1 acionamento da versão de produção paralela, quanto deste material será produzido
    private Double quantidadeProporcao;
    
    @Getter(AccessLevel.NONE)
    @ManyToOne
    UnidadeMedida unidadeMedidaProporcao;
        
    public VersaoProducaoParalela getVersaoProducaoParalela() {
        return getVersaoProducaoParalelaComponenteCompositeKey().getVersaoProducaoParalela();
    }
    
    public Roteiro getRoteiro() {
        return getVersaoProducaoParalelaComponenteCompositeKey().getRoteiro();
    }
    
    public ListaTecnica getListaTecnica() {
        return getVersaoProducaoParalelaComponenteCompositeKey().getListaTecnica();
    }
        
    public double getQuantidadeProporcao() {
        return (quantidadeProporcao == null) ? 1 : quantidadeProporcao;
    }
    
    public UnidadeMedida getUnidadeMedidaProporcao(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaProporcao == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedidaProporcao;
    }
    public UnidadeMedida getUnidadeMedidaProporcaoCadastrada() {
        return unidadeMedidaProporcao;
    }
    
    public Produto getMaterialOutput() {
        
        if (!getRoteiro().getMaterialOutput().equals(getListaTecnica().getMaterialOutput())) throw new IllegalStateException("Routing material different from BOM material for production version component " + this.toString());

        return getRoteiro().getMaterialOutput();
        
    }
    
    public double getQuantidadeProporcaoNaUnidadeMedidaTarget(UnidadeMedidaProjection unidadeMedidaProjection, UnidadeMedida unidadeMedidaTarget) {
        
        return getQuantidadeProporcao()
                * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                        getMaterialOutput(),
                        getUnidadeMedidaProporcao(
                                unidadeMedidaProjection.getParametrosGlobais()),
                        unidadeMedidaTarget);
        
    }
    
    
}
