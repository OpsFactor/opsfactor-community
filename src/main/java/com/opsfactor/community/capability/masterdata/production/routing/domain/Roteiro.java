package com.opsfactor.community.capability.masterdata.production.routing.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoAbstract;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Roteiro produtivo simples para fabricar um material em uma location.
 *
 * <p>O plano heuristico Community usa o roteiro para ordenar operacoes,
 * consumir capacidade total por dia e vincular recursos produtivos. Recursos
 * avancados como parallel routing/output, scheduling fino e escolha otimizada
 * entre alternativas pertencem ao Enterprise.</p>
 */
@Entity
@Table(name = "roteiro")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_roteiro")
@DiscriminatorValue("simples")
@Data
@Builder
@ToString(of="id")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class Roteiro implements Comparable<Roteiro> {

    @Id
    @Column(length = 50)
    private String id;

    private String descricao;

    @NonNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Location location;

    /** Output do roteiro simples; o subtipo múltiplo mantém sua coleção própria. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Produto materialOutput;

    /**
     * Identificador do cluster de roteiros usado exclusivamente por
     * capacidades Enterprise de line scheduling.
     *
     * <p>O campo fica como escalar no schema Community para preservar a
     * mesma tabela de {@code Roteiro}, sem criar uma dependencia JPA para a
     * entidade Enterprise que representa o cluster. O Community nao o usa
     * nem o administra por suas bordas publicas; o Enterprise resolve o ID
     * em lote quando a capacidade correspondente for executada.</p>
     */
    @Column(length = 50)
    private String routingClusterId;
    
    /**
     * Quanto menor o número, maior a prioridade
     */
    private Integer prioridade;
        
    // se false, somente poderá ser usado quando referenciado por uma versão de produção
    private Boolean habilitadoParaUsoSemVersaoProducao;
    
    private Boolean ativo;

    /** Quantidade-base produzida por uma execução das operações do roteiro. */
    private Double quantidadeBase;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unidadeMedidaQuantidadeBase;
        
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "operacaoRoteiroCompositeKey.roteiro", fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<OperacaoRoteiro> operacaoRoteiroSet = new HashSet<>();

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "roteiro", fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<VersaoProducao> versaoProducaoSet = new HashSet<>();
    
    public int getPrioridade() {
        return (prioridade == null) ? Integer.MAX_VALUE : prioridade;
    }
    
    public Integer getPrioridadeCadastrada() {
        return prioridade;
    }
    
    /**
     * 1o elemento da lista = primeira operação do roteiro
     * @return 
     */
    public List<OperacaoRoteiro> getOperacaoRoteiroListOrdenadaPorPosicaoAsc() {
        return getOperacaoRoteiroSet().stream()
                .sorted(Comparator.comparing(x -> x.getPosicao()))
                .collect(Collectors.toList());
    }
    
    public Integer getUltimaPosicaoOperacao() {
        return getOperacaoRoteiroSet().stream()
                .sorted(Comparator.comparingInt(OperacaoRoteiro::getPosicao).reversed())
                .findFirst()
                .map(OperacaoRoteiro::getPosicao)
                .orElse(null);
    }
    
    /**
     * Se ativo = falso, roteiro inativo
     * Se recurso produtivo de alguma das operações estiver inativo, se considera que receita é inativa
     * Caso contrário receita é considerada ativa
     * @return 
     */
    public boolean getAtivo() {
        if (ativo != null && ativo == false) return false;
        return true;
    }
    
    /**
     * Método usado para data upload
     * @return 
     */
    public Boolean getAtivoCadastrado() {
        return ativo;
    }
    
    /**
     * Varre as operações e extrai os recursos produtivos associados a cada uma
     * @return 
     */
    public Set<RecursoProdutivo> getRecursoProdutivoSet() {
        return getOperacaoRoteiroSet().stream()
                .map(OperacaoAbstract::getRecursoProdutivo)
                .collect(Collectors.toSet());
    }

    /** Outputs físicos do pacote; o tipo simples preserva exatamente um. */
    public Set<Produto> getMateriaisOutput() {

        if (materialOutput == null) {
            throw new IllegalStateException("Routing " + getId() + " has no output material");
        }
        return Set.of(materialOutput);

    }

    /** Atalho estritamente singular para consumidores de roteiro simples. */
    public Produto getMaterialOutput() {

        Set<Produto> materiaisOutput = getMateriaisOutput();
        if (materiaisOutput.size() != 1) {
            throw new IllegalStateException("Routing " + getId() + " does not have exactly one output material");
        }
        return materiaisOutput.iterator().next();

    }
    
    public void verificaCompatibilidadeListaTecnica(ListaTecnica listaTecnica) {
        if (!getMateriaisOutput().equals(listaTecnica.getMateriaisOutput())) {
            throw new IllegalStateException("Routing outputs incompatible with BOM outputs");
        }
        if (!getLocation().equals(listaTecnica.getLocation())) {
            throw new IllegalStateException("Routing location " + getLocation().getId() + " incompatible with BOM location " + listaTecnica.getLocation().getId());
        }
    }
    
    public boolean getHabilitadoParaUsoSemVersaoProducao() {
        return (habilitadoParaUsoSemVersaoProducao == null) ? true : habilitadoParaUsoSemVersaoProducao;
    }
    public Boolean getHabilitadoParaUsoSemVersaoProducaoCadastrado() {
        return habilitadoParaUsoSemVersaoProducao;
    }
    
    /**
     * Considera a menor produtividade entre todas as operações
     * @param unidadeMedidaTarget
     * @param unidadeMedidaProjection
     * @return 
     */
    public double getQuantidadeMaximaProduzidaPorHora(UnidadeMedida unidadeMedidaTarget, UnidadeMedidaProjection unidadeMedidaProjection) {
        
        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        
        return operacaoRoteiroSet.stream()
                .mapToDouble(operacaoRoteiro -> getQuantidadeBase() * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                        materialOutput, getUnidadeMedidaQuantidadeBase(parametrosGlobais), unidadeMedidaTarget)
                        / operacaoRoteiro.getHorasPorQuantidadeBase())
                .min().orElse(0);
        
    }

    public double getQuantidadeBase() {

        if (quantidadeBase == null) {
            return 1d;
        }
        if (!Double.isFinite(quantidadeBase) || quantidadeBase <= 0d) {
            throw new IllegalStateException("Routing base quantity must be finite and positive");
        }
        return quantidadeBase;

    }

    public Double getQuantidadeBaseCadastrada() {

        return quantidadeBase;

    }

    public UnidadeMedida getUnidadeMedidaQuantidadeBase(ParametrosGlobais parametrosGlobais) {

        return unidadeMedidaQuantidadeBase == null
                ? parametrosGlobais.getUnidadeMedidaPadraoSNP()
                : unidadeMedidaQuantidadeBase;

    }

    public UnidadeMedida getUnidadeMedidaQuantidadeBaseCadastrada() {

        return unidadeMedidaQuantidadeBase;

    }

    /**
     * O BI em memória usa {@code NavigableIndex} para dimensões de objeto.
     * Para manter busca rápida e ordenação estável, roteiros seguem o mesmo
     * contrato já adotado por outras entidades mestre do domínio: comparação
     * direta pelo identificador.
     */
    @Override
    public int compareTo(Roteiro roteiro) {
        return getId().compareTo(roteiro.getId());
    }
    
}
