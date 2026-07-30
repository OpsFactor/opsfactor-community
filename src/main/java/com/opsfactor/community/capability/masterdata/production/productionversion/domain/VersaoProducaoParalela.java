package com.opsfactor.community.capability.masterdata.production.productionversion.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import lombok.*;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Versao de producao paralela preservada no schema compartilhado.
 *
 * <p>O Supply Planning Community nao seleciona nem executa parallel
 * routing/output. A classe permanece no modelo para leitura defensiva de bases
 * existentes e para permitir que o Enterprise materialize a capacidade privada
 * em overlay proprio.</p>
 */
@Entity
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@DiscriminatorValue("paralela")
@NoArgsConstructor
public class VersaoProducaoParalela extends VersaoProducao {

    public VersaoProducaoParalela(String id, Location location, Integer prioridade) {
        this.id = id;
        setLocation(location);
        setPrioridade(prioridade);
    }
        
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "versaoProducaoParalelaComponenteCompositeKey.versaoProducaoParalela", fetch = FetchType.LAZY)
    private Set<VersaoProducaoParalelaComponente> versaoProducaoParalelaComponenteSet = new HashSet<>();
            
    @Override
    public void geraErroSeDadosInconsistentes() {
        for (VersaoProducaoParalelaComponente versaoProducaoParalelaComponente : versaoProducaoParalelaComponenteSet) {
            if (!versaoProducaoParalelaComponente.getRoteiro().getLocation().equals(getLocation())) {
                throw new IllegalStateException("Routing location " + versaoProducaoParalelaComponente.getRoteiro().getLocation().getId() + " different than version location " + getLocation().getId());
            } else if (!versaoProducaoParalelaComponente.getRoteiro().getMaterialOutput().equals(versaoProducaoParalelaComponente.getListaTecnica().getMaterialOutput())) {
                throw new IllegalStateException("Routing material " + versaoProducaoParalelaComponente.getRoteiro().getMaterialOutput().getId() + " different than BOM output material " + versaoProducaoParalelaComponente.getListaTecnica().getMaterialOutput().getId());
            } else if (!versaoProducaoParalelaComponente.getListaTecnica().getLocation().equals(getLocation())) {
                throw new IllegalStateException("Bill of Materials location " + versaoProducaoParalelaComponente.getListaTecnica().getLocation().getId() + " different than version location " + getLocation().getId());
            }
        }
    }
    
    @Override
    public boolean contemRoteiro(Roteiro roteiro) {
        return versaoProducaoParalelaComponenteSet.stream().anyMatch(x -> x.getRoteiro().equals(roteiro));
    }
    
    @Override
    public boolean contemListaTecnica(ListaTecnica listaTecnica) {
        return versaoProducaoParalelaComponenteSet.stream().anyMatch(x -> x.getListaTecnica().equals(listaTecnica));
    }
    
    @Override
    public Set<Produto> getMateriaisOutput() {
        return versaoProducaoParalelaComponenteSet.stream()
                .map(x -> x.getRoteiro().getMaterialOutput())
                .collect(Collectors.toSet());
    }
    
    @Override
    public Set<Produto> getMateriaisInput() {
        return versaoProducaoParalelaComponenteSet.stream()
                .flatMap(x -> x.getListaTecnica().getMateriaisInput().stream())
                .collect(Collectors.toSet());
    }
    
    @Override
    public Set<Roteiro> getRoteiros() {
        return versaoProducaoParalelaComponenteSet.stream()
                .map(x -> x.getRoteiro())
                .collect(Collectors.toSet());
    }
    
    @Override
    public Set<ListaTecnica> getListasTecnicas() {
        return versaoProducaoParalelaComponenteSet.stream()
                .map(x -> x.getListaTecnica())
                .collect(Collectors.toSet());
    }
    
    /**
     * Retorna as quantidades dos componentes da versão de produção na unidade de medida desejada
     * Ex: 200kg produto A : 1ton produto B
     * Ao chamar com argumento de unidade = kg, retorna 200kg produto A : 1000kg produto B
     * @return
     */
    public Map<Produto,Double> getMapaQuantidadePorMaterial(
            UnidadeMedidaProjection unidadeMedidaProjection,
            UnidadeMedida unidadeMedidaReferencia) {
        
        return versaoProducaoParalelaComponenteSet.stream()
                .collect(Collectors.groupingBy(
                        x -> x.getMaterialOutput(),
                        Collectors.summingDouble(x -> x.getQuantidadeProporcaoNaUnidadeMedidaTarget(unidadeMedidaProjection, unidadeMedidaReferencia))));
        
    }
    
    /**
     * Extrai uma lista de triplets com a quantidade de cada combinação roteiro/lista técnica
     * e a quantidade total na unidade de medida referência
     * As quantidades serão calculadas de forma a se atingir a produção total do materialReferencia
     * na quantidade desejada
     * Exemplo : proporção A:B:C = 1:2:4
     * getDetalhePorVersaoProducao(B, onde qtde = 5) retornará:
     * A: 5
     * B: 10
     * C: 20
     * Todas as quantidades estarão na unidadeMedidaMaterialReferencia
     * @param unidadeMedidaProjection
     * @param materialReferencia
     * @param unidadeMedidaMaterialReferencia
     * @param quantidadeMaterialReferencia
     * @return 
     */
    @Override
    public List<Triplet<Roteiro,ListaTecnica,Double>> getDetalhePorVersaoProducao(
            UnidadeMedidaProjection unidadeMedidaProjection,
            Produto materialReferencia,
            UnidadeMedida unidadeMedidaMaterialReferencia,
            double quantidadeMaterialReferencia) {
        
        Map<Produto,Double> quantidadePorMaterial = getMapaQuantidadePorMaterial(
                unidadeMedidaProjection,
                unidadeMedidaMaterialReferencia);
        
        // ex. se qtde material referencia = 5 e nas versoes producao componentes a qtde total for 10, então vamos acionar
        // 50% das 'quantidades proporcao' dos componentes
        double percentualProducao = quantidadeMaterialReferencia / quantidadePorMaterial.get(materialReferencia);
        
        return versaoProducaoParalelaComponenteSet.stream()
                .map(componente -> Triplet.with(
                        componente.getRoteiro(), 
                        componente.getListaTecnica(), 
                        percentualProducao * componente.getQuantidadeProporcaoNaUnidadeMedidaTarget(
                                unidadeMedidaProjection, unidadeMedidaMaterialReferencia)))
                .collect(Collectors.toList());        
        
    }
    
    public List<Pair<Roteiro,ListaTecnica>> getCombinacoesRoteiroListaTecnica() {
        return versaoProducaoParalelaComponenteSet.stream()
                .map(componente -> Pair.with(
                        componente.getRoteiro(), 
                        componente.getListaTecnica()))
                .collect(Collectors.toList());
    }
    
}
