package com.opsfactor.community.capability.masterdata.production.productionversion.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Versao de producao base para roteiros/listas tecnicas Community.
 *
 * <p>O Community usa apenas versoes simples ou sentinelas para viabilidade
 * produtiva heuristica. Cluster de roteiros, parallel routing/output funcional
 * e escolha otimizada de alternativas ficam no Enterprise.</p>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_versao_producao")
@Getter
@Setter
public abstract class VersaoProducao implements Serializable, Comparable<VersaoProducao> {
    
    /**
     * Identificador persistido da versao de producao.
     *
     * <p>Pode ser nulo em instancias transitorias usadas para envelopar
     * roteiro/lista tecnica habilitados para uso sem versao de producao
     * cadastrada. Por isso a hierarquia nao usa equals/hashCode apenas por id.</p>
     */
    @Id
    @Column(length = 50)
    String id;

    /**
     * Location operacional da versao produtiva.
     */
    @ManyToOne
    private Location location;
        
    private Integer prioridade;
        
    private Boolean ativo;
    
    public int getPrioridade() {
        return (prioridade == null) ? Integer.MAX_VALUE : prioridade;
    }
    public Integer getPrioridadeCadastrada() {
        return prioridade;
    }
    
    public boolean getAtivo() {
        return (ativo == null) ? true : ativo;
    }
    public Boolean getAtivoCadastrado() {
        return ativo;
    }
    
    
    public abstract void geraErroSeDadosInconsistentes();
    
    public abstract Set<Produto> getMateriaisOutput();
    public abstract Set<Produto> getMateriaisInput();
    public abstract Set<ListaTecnica> getListasTecnicas();
    public abstract Set<Roteiro> getRoteiros();
    
    public Set<RecursoProdutivo> getRecursosProdutivos() {
        return getRoteiros().stream()
                .flatMap(roteiro -> roteiro.getRecursoProdutivoSet().stream())
                .collect(Collectors.toSet());
    }
    
    public abstract boolean contemRoteiro(Roteiro roteiro);
    public abstract boolean contemListaTecnica(ListaTecnica listaTecnica);
        
    @Override
    public int hashCode() {
        if (id != null) return id.hashCode();
        
        return 2 * getRoteiros().hashCode() + 3 * getListasTecnicas().hashCode();
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
     * Se versão de produção for simples, retorna exatamente o valor solicitado
     * Todas as quantidades estarão na unidadeMedidaMaterialReferencia
     * @param versaoProducao
     * @param unidadeMedidaProjection
     * @param materialReferencia
     * @param unidadeMedidaMaterialReferencia
     * @param quantidadeMaterialReferencia
     * @return 
     */
    public abstract List<Triplet<Roteiro,ListaTecnica,Double>> getDetalhePorVersaoProducao(
            UnidadeMedidaProjection unidadeMedidaProjection,
            Produto materialReferencia,
            UnidadeMedida unidadeMedidaMaterialReferencia,
            double quantidadeMaterialReferencia);
        
    public abstract List<Pair<Roteiro,ListaTecnica>> getCombinacoesRoteiroListaTecnica();
    
    public double getQuantidadeDeMaterialInputConsumidoPorProducaoDeOutput(
            UnidadeMedidaProjection unidadeMedidaProjection,
            Produto materialInput,
            UnidadeMedida unidadeMedidaTargetMaterialInput,
            Produto materialReferenciaOutput,
            UnidadeMedida unidadeMedidaMaterialOutput,
            double quantidadeMaterialOutput) {
        
        return getDetalhePorVersaoProducao(
                unidadeMedidaProjection, 
                materialReferenciaOutput, 
                unidadeMedidaMaterialOutput, 
                quantidadeMaterialOutput)
                .stream()
                .mapToDouble(triplet -> triplet.getValue1().getQuantidadeInputDeQuantidadeOutput(
                        materialInput, 
                        unidadeMedidaMaterialOutput, 
                        quantidadeMaterialOutput, 
                        unidadeMedidaTargetMaterialInput, 
                        unidadeMedidaProjection).orElse(0))
                .sum();
        
    }
    
    public Optional<String> getOptionalId() {
        return Optional.ofNullable(id);
    }

    /**
     * A indexação do BI em memória usa {@code NavigableIndex} para dimensões
     * OBJECT, então a versão de produção precisa expor ordenação determinística.
     *
     * Para versões persistidas, a identidade natural é o id.
     * Para versões temporárias sem id, a ordenação precisa continuar estável;
     * nesse caso usamos uma chave derivada da combinação efetiva
     * location/roteiro/lista técnica.
     */
    @Override
    public int compareTo(VersaoProducao outraVersaoProducao) {
        if (outraVersaoProducao == null) {
            return 1;
        }

        if (getId() != null && outraVersaoProducao.getId() != null) {
            return getId().compareTo(outraVersaoProducao.getId());
        }

        return getChaveOrdenacaoComparavel().compareTo(outraVersaoProducao.getChaveOrdenacaoComparavel());
    }

    /**
     * Versões temporárias podem existir sem id para envelopar uma combinação de
     * roteiro/lista técnica. Nesses casos, a ordenação do BI não pode colapsar
     * todas as instâncias no mesmo bucket "null"; por isso derivamos uma chave
     * textual estável a partir das dimensões reais da versão.
     */
    private String getChaveOrdenacaoComparavel() {
        if (getId() != null) {
            return "ID#" + getId();
        }

        String locationId = (getLocation() == null || getLocation().getId() == null)
                ? ""
                : getLocation().getId();

        String combinacoesRoteiroListaTecnica = getCombinacoesRoteiroListaTecnica().stream()
                .map(parRoteiroListaTecnica -> getIdOrdenacao(parRoteiroListaTecnica.getValue0())
                        + "->"
                        + getIdOrdenacao(parRoteiroListaTecnica.getValue1()))
                .sorted()
                .collect(Collectors.joining("|"));

        return "TEMP#"
                + getClass().getSimpleName()
                + "#"
                + locationId
                + "#"
                + combinacoesRoteiroListaTecnica;
    }

    private static String getIdOrdenacao(Roteiro roteiro) {
        return (roteiro == null || roteiro.getId() == null) ? "" : roteiro.getId();
    }

    private static String getIdOrdenacao(ListaTecnica listaTecnica) {
        return (listaTecnica == null || listaTecnica.getId() == null) ? "" : listaTecnica.getId();
    }
    
    @Override
    public String toString() {
        if (id == null) {
            return "No Production Version";
        }
        return id;
    }
    
    public boolean isVersaoProducaoTemporaria() {
        return getId() == null;
    }
    
    public static VersaoProducao getVersaoProducaoAlocadaOuTemporariaSeInexistente(
            VersaoProducao versaoProducao,
            Roteiro roteiro,
            ListaTecnica listaTecnica,
            SupplyNetworkProjection supplyNetworkProjection) {
        
        if (versaoProducao == null || versaoProducao instanceof VersaoProducaoInexistente) {
            Optional<VersaoProducaoSimples> optionalVersaoProducao = supplyNetworkProjection
                    .getVersaoProducaoSimplesViavelPrioritaria(roteiro, listaTecnica);
            if (optionalVersaoProducao.isEmpty()) {
                optionalVersaoProducao = supplyNetworkProjection.getVersaoProducaoSimplesPrioritaria(roteiro, listaTecnica);
            }

            return optionalVersaoProducao.orElseGet(() -> {

                /*
                 * Nao ha nenhum candidato viavel ou cadastrado: cria versao
                 * producao temporaria com id = null para envelopar a combinacao
                 * roteiro/lista tecnica recebida pelo fluxo heuristico.
                 */
                VersaoProducaoSimples versaoProducaoTemporaria = new VersaoProducaoSimples();
                versaoProducaoTemporaria.setId(null);
                versaoProducaoTemporaria.setLocation(roteiro.getLocation());
                versaoProducaoTemporaria.setAtivo(true);
                versaoProducaoTemporaria.setMaterialOutput(roteiro.getMaterialOutput());
                versaoProducaoTemporaria.setRoteiro(roteiro);
                versaoProducaoTemporaria.setListaTecnica(listaTecnica);
                return versaoProducaoTemporaria;

            });
                    
        } else {
            return versaoProducao;
        }
        
    }
    
}
