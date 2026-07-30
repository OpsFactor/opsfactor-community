package com.opsfactor.community.capability.masterdata.production.productionversion.domain;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.List;
import java.util.Set;

/**
 * Sentinela para roteiros/listas tecnicas habilitados sem versao de producao.
 *
 * <p>Esta classe permite representar explicitamente o caso operacional antigo
 * em que a malha usa roteiro e BOM sem cadastro de uma versao simples. O
 * Community deve resolver essa sentinela nas bordas de viabilidade antes de
 * persistir ou consultar mapas que exigem uma versao real.</p>
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@DiscriminatorValue("inexistente")
public class VersaoProducaoInexistente extends VersaoProducao {

    public static final String ID_VERSAO_PRODUCAO_VAZIA = "DEFAULT_PRODUCTION_VERSION";
        
    public VersaoProducaoInexistente() {
        this.id = ID_VERSAO_PRODUCAO_VAZIA;
    }
    
    public void setId(String id) {
        this.id = ID_VERSAO_PRODUCAO_VAZIA;
    }
    
    @Override
    public void geraErroSeDadosInconsistentes() {
        if (!getId().equals(ID_VERSAO_PRODUCAO_VAZIA)) {
            throw new IllegalStateException("Inexisting production version has different Id than " + ID_VERSAO_PRODUCAO_VAZIA);
        }
    }
    
    @Override
    public boolean contemRoteiro(Roteiro roteiro) {
        return roteiro.getHabilitadoParaUsoSemVersaoProducao();
    }
    
    @Override
    public boolean contemListaTecnica(ListaTecnica listaTecnica) {
        return listaTecnica.getHabilitadoParaUsoSemVersaoProducao();
    }
    
    @Override
    public Set<Produto> getMateriaisOutput() {

        throw getUsoProdutivoNaoSuportadoException("output materials");

    }
    
    @Override
    public Set<Produto> getMateriaisInput() {

        throw getUsoProdutivoNaoSuportadoException("input materials");

    }
    
    @Override
    public Set<Roteiro> getRoteiros() {

        throw getUsoProdutivoNaoSuportadoException("routings");

    }
    
    @Override
    public Set<ListaTecnica> getListasTecnicas() {

        throw getUsoProdutivoNaoSuportadoException("bills of materials");

    }
    
    @Override
    public List<Triplet<Roteiro,ListaTecnica,Double>> getDetalhePorVersaoProducao(
            UnidadeMedidaProjection unidadeMedidaProjection,
            Produto materialReferencia,
            UnidadeMedida unidadeMedidaMaterialReferencia,
            double quantidadeMaterialReferencia) {

        throw getUsoProdutivoNaoSuportadoException("routing/BOM production details");
        
    }
    
    @Override
    public List<Pair<Roteiro,ListaTecnica>> getCombinacoesRoteiroListaTecnica() {

        throw getUsoProdutivoNaoSuportadoException("routing/BOM combinations");

    }
    
    @Override
    public String toString() {
        return ID_VERSAO_PRODUCAO_VAZIA;
    }

    /**
     * Falha padronizada para chamadas que tentam usar a sentinela como versao
     * produtiva real.
     *
     * <p>A sentinela e valida para cadastro/projecao defensiva, mas nao possui
     * roteiro, lista tecnica nem materiais. Retornar colecoes vazias seria
     * perigoso porque mascararia um erro de viabilidade; retornar {@code null}
     * produziria NPE tardio. Por isso a propria entidade falha explicitamente.</p>
     */
    private IllegalStateException getUsoProdutivoNaoSuportadoException(
            String detalheSolicitado) {

        return new IllegalStateException(
                "Production version sentinel "
                        + ID_VERSAO_PRODUCAO_VAZIA
                        + " does not expose "
                        + detalheSolicitado
                        + ". Resolve a real production version before using production planning maps.");

    }
            
}
