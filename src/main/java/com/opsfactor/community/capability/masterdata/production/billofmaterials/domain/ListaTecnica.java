package com.opsfactor.community.capability.masterdata.production.billofmaterials.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import lombok.*;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lista tecnica simples de producao para um material output em uma location.
 *
 * <p>O heuristico Community usa a lista tecnica para calcular consumo de
 * componentes e validar viabilidade produtiva basica. Parallel routing/output,
 * custos de recurso, calendarios complexos de scheduling e escolhas
 * otimizadas de receita ficam no Enterprise.</p>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_lista_tecnica")
@DiscriminatorValue("simples")
@Data
@Builder
@ToString(of="id")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class ListaTecnica implements Comparable<ListaTecnica> {

    @Id
    @Column(length = 50)
    private String id;
        
    @NonNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Location location;

    /** Output do cabeçalho simples; listas múltiplas usam a coleção do subtipo. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Produto materialOutput;
        
    private Double quantidade;
    
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unidadeMedidaMaterialOutput;
    
    private String descricao;
    
    /**
     * Quanto menor o número, maior a prioridade
     */
    private Integer prioridade;
    
    // se false, somente poderá ser usado quando referenciado por uma versão de produção
    private Boolean habilitadoParaUsoSemVersaoProducao;
    
    private Boolean ativo;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "listaTecnicaComponenteCompositeKey.listaTecnica", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ListaTecnicaComponente> listaTecnicaComponenteSet = new HashSet<>();
        
    public Set<Produto> getMateriaisInput() {
        return getListaTecnicaComponenteSet().stream()
                .map(ListaTecnicaComponente::getMaterialComponente)
                .collect(Collectors.toSet());
    }

    /** Outputs físicos do pacote; a lista simples preserva exatamente um. */
    public Set<Produto> getMateriaisOutput() {

        if (materialOutput == null) {
            throw new IllegalStateException("BOM " + getId() + " has no output material");
        }
        return Set.of(materialOutput);

    }

    /** Atalho estritamente singular para consumidores de lista técnica simples. */
    public Produto getMaterialOutput() {

        Set<Produto> materiaisOutput = getMateriaisOutput();
        if (materiaisOutput.size() != 1) {
            throw new IllegalStateException("BOM " + getId() + " does not have exactly one output material");
        }
        return materiaisOutput.iterator().next();

    }

    /** Quantidade-base do output indicado, na unidade cadastrada para ele. */
    public double getQuantidadeBaseOutput(Produto produtoOutput) {

        if (!getMaterialOutput().equals(produtoOutput)) {
            throw new IllegalArgumentException("Material " + produtoOutput.getId() + " is not an output of BOM " + getId());
        }
        return getQuantidade();

    }

    /** Unidade cadastrada do output indicado. */
    public UnidadeMedida getUnidadeMedidaMaterialOutput(
            Produto produtoOutput,
            ParametrosGlobais parametrosGlobais) {

        if (!getMaterialOutput().equals(produtoOutput)) {
            throw new IllegalArgumentException("Material " + produtoOutput.getId() + " is not an output of BOM " + getId());
        }
        return getUnidadeMedidaMaterialOutput(parametrosGlobais);

    }

    /**
     * Retorna a quantidade de material input na unidadeMedidaInput
     * @param materialInput
     * @param unidadeMedidaOutput unidade de medida da quantidadeOutput
     * @param quantidadeOutput 
     * @param unidadeMedidaInput unidade de saída do método
     * @param unidadeMedidaProjection
     * @return 
     */
    public OptionalDouble getQuantidadeInputDeQuantidadeOutput(
            Produto materialInput, 
            UnidadeMedida unidadeMedidaOutput,
            double quantidadeOutput,
            UnidadeMedida unidadeMedidaInput,
            UnidadeMedidaProjection unidadeMedidaProjection) {
        
        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();

        double quantidadeInput = getListaTecnicaComponenteSet().stream()
                .filter(x -> x.getMaterialComponente().equals(materialInput))
                // qtde input na unidadeMedidaInput para cada unidadeMedidaListaTecnica do output
                .map(x -> x.getQuantidade() * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                        materialInput, x.getUnidadeMedidaMaterialComponente(parametrosGlobais), unidadeMedidaInput))
                // divide a quantidade input pela quantidade desejada do material output
                // para isso, faz o calculo quantidadeInput<UnidadeDesejadaInput> / quantidadeOutputNaListaTecnica<UnidadeDesejadaOutput> * quantidadeOutputDesejada<UnidadeDesejadaOutput>
                .mapToDouble(x -> x
                        / (getQuantidade() * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                                getMaterialOutput(), getUnidadeMedidaMaterialOutput(parametrosGlobais), unidadeMedidaOutput))
                        * quantidadeOutput)
                .sum();
        
        return (quantidadeInput == 0 && quantidadeOutput != 0) ? OptionalDouble.empty() : OptionalDouble.of(quantidadeInput);
                
    }
    
    public UnidadeMedida getUnidadeMedidaMaterialInput(Produto materialInput, ParametrosGlobais parametrosGlobais) {
        for (ListaTecnicaComponente listaTecnicaComponente : getListaTecnicaComponenteSet()) {
            if (listaTecnicaComponente.getMaterialComponente().equals(materialInput)) {
                return listaTecnicaComponente.getUnidadeMedidaMaterialComponente(parametrosGlobais);
            }
        }
        return null;
    }
    
    public UnidadeMedida getUnidadeMedidaMaterialOutputCadastrada() {
        return unidadeMedidaMaterialOutput;
    }
    public UnidadeMedida getUnidadeMedidaMaterialOutput(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaMaterialOutput == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedidaMaterialOutput;
    }
    
    public double getQuantidade() {
        return (quantidade == null) ? 1 : quantidade;
    }
    public Double getQuantidadeCadastrdo() {
        return quantidade;
    }

    public boolean getAtivo() {
        return (ativo == null) ? true : ativo;
    }
    
    /**
     * Método usado para data upload
     * @return 
     */
    public Boolean getAtivoCadastrado() {
        return ativo;
    }
    
    public int getPrioridade() {
        return (prioridade == null) ? Integer.MAX_VALUE : prioridade;
    }
    
    public Integer getPrioridadeCadastrada() {
        return prioridade;
    }
    
    public boolean getHabilitadoParaUsoSemVersaoProducao() {
        return (habilitadoParaUsoSemVersaoProducao == null) ? true : habilitadoParaUsoSemVersaoProducao;
    }

    /**
     * Retorna a configuracao persistida para uso sem versao de producao.
     *
     * <p>A integracao deve preservar {@code null} para que o fallback
     * operacional permissivo continue sendo aplicado somente no ponto de
     * consumo. Usar o getter efetivo aqui transformaria uma configuracao
     * ausente em {@code true} persistido numa exportacao seguida de importacao.</p>
     */
    public Boolean getHabilitadoParaUsoSemVersaoProducaoCadastrado() {

        return habilitadoParaUsoSemVersaoProducao;

    }

    @Override
    public int compareTo(ListaTecnica listaTecnica) {
        return getId().compareTo(listaTecnica.getId());
    }
    
}
