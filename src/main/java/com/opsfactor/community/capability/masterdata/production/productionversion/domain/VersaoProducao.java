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
import lombok.NoArgsConstructor;
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
 * Entidade única de versão de produção.
 *
 * <p>A diferenciação simples/múltipla pertence aos mestres referenciados. A
 * versão permanece neutra e aponta para {@link Roteiro} e {@link ListaTecnica}
 * pelas abstrações comuns.</p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class VersaoProducao implements Serializable, Comparable<VersaoProducao> {

    public static final String ID_VERSAO_PRODUCAO_VAZIA = "DEFAULT_PRODUCTION_VERSION";
    
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
    @ManyToOne(fetch = FetchType.LAZY)
    private Location location;

    /**
     * Roteiro polimórfico da versão.
     *
     * <p>O Community persiste somente {@code Roteiro} simples. O Enterprise
     * pode associar um subtipo múltiplo sem criar outra coluna ou exigir que
     * services escolham entre getters concorrentes.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private Roteiro roteiro;

    /** Lista técnica polimórfica vinculada ao mesmo pacote produtivo. */
    @ManyToOne(fetch = FetchType.LAZY)
    private ListaTecnica listaTecnica;
        
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
    
    
    public VersaoProducao(
            String id,
            Location location,
            Integer prioridade,
            Roteiro roteiro,
            ListaTecnica listaTecnica) {

        this.id = id;
        this.location = location;
        this.prioridade = prioridade;
        this.roteiro = roteiro;
        this.listaTecnica = listaTecnica;
        geraErroSeDadosInconsistentes();

    }

    public void geraErroSeDadosInconsistentes() {

        if (isVersaoProducaoInexistente()) {
            return;
        }
        if (location == null || roteiro == null || listaTecnica == null) {
            throw new IllegalStateException("Production version requires location, routing and BOM");
        }
        if (!roteiro.getLocation().equals(location)) {
            throw new IllegalStateException("Routing location differs from production version location");
        }
        if (!listaTecnica.getLocation().equals(location)) {
            throw new IllegalStateException("BOM location differs from production version location");
        }
        if (!listaTecnica.getMateriaisOutput().equals(roteiro.getMateriaisOutput())) {
            throw new IllegalStateException("Routing and BOM output materials differ");
        }

    }

    public Set<Produto> getMateriaisOutput() {

        geraErroSeUsoProdutivoDaSentinela();
        return listaTecnica.getMateriaisOutput();

    }

    /**
     * Informa se os mestres associados representam um pacote de múltiplos
     * outputs. No Community o resultado permanece sempre falso, pois apenas
     * roteiro e lista técnica simples são persistidos. O contrato é genérico
     * para que a especialização Enterprise não exija outro tipo de versão.
     */
    public boolean isProducaoMultipla() {

        return !isVersaoProducaoInexistente() && getMateriaisOutput().size() > 1;

    }

    public Set<Produto> getMateriaisInput() {

        geraErroSeUsoProdutivoDaSentinela();
        return listaTecnica.getMateriaisInput();

    }

    public Set<ListaTecnica> getListasTecnicas() {

        geraErroSeUsoProdutivoDaSentinela();
        return Set.of(listaTecnica);

    }

    public Set<Roteiro> getRoteiros() {

        geraErroSeUsoProdutivoDaSentinela();
        return Set.of(roteiro);

    }
    
    public Set<RecursoProdutivo> getRecursosProdutivos() {
        return getRoteiros().stream()
                .flatMap(roteiro -> roteiro.getRecursoProdutivoSet().stream())
                .collect(Collectors.toSet());
    }
    
    public boolean contemRoteiro(Roteiro roteiro) {

        return isVersaoProducaoInexistente()
                ? roteiro.getHabilitadoParaUsoSemVersaoProducao()
                : this.roteiro.equals(roteiro);

    }

    public boolean contemListaTecnica(ListaTecnica listaTecnica) {

        return isVersaoProducaoInexistente()
                ? listaTecnica.getHabilitadoParaUsoSemVersaoProducao()
                : this.listaTecnica.equals(listaTecnica);

    }

    /**
     * Atalho estritamente singular para consumidores que exigem uma versão
     * simples. Cálculos genéricos devem usar {@link #getMateriaisOutput()}.
     */
    public Produto getMaterialOutput() {

        Set<Produto> materiaisOutput = getMateriaisOutput();
        if (materiaisOutput.size() != 1) {
            throw new IllegalStateException("Production version " + getId()
                    + " does not have exactly one output material");
        }
        return materiaisOutput.iterator().next();

    }
        
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
    public List<Triplet<Roteiro,ListaTecnica,Double>> getDetalhePorVersaoProducao(
            UnidadeMedidaProjection unidadeMedidaProjection,
            Produto materialReferencia,
            UnidadeMedida unidadeMedidaMaterialReferencia,
            double quantidadeMaterialReferencia) {

        geraErroSeUsoProdutivoDaSentinela();
        return List.of(Triplet.with(roteiro, listaTecnica, 1.0d));

    }
        
    public List<Pair<Roteiro,ListaTecnica>> getCombinacoesRoteiroListaTecnica() {

        geraErroSeUsoProdutivoDaSentinela();
        return List.of(Pair.with(roteiro, listaTecnica));

    }
    
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

    public boolean isVersaoProducaoInexistente() {

        return ID_VERSAO_PRODUCAO_VAZIA.equals(id);

    }

    /**
     * Cria a sentinela do contrato único sem introduzir um subtipo JPA.
     *
     * <p>A instância retornada ainda não está persistida. O service responsável
     * pela sentinela decide se deve reutilizar a linha existente ou salvá-la.</p>
     */
    public static VersaoProducao criaVersaoProducaoInexistente() {

        VersaoProducao versaoProducao = new VersaoProducao();
        versaoProducao.setId(ID_VERSAO_PRODUCAO_VAZIA);
        versaoProducao.setAtivo(false);
        return versaoProducao;

    }
    
    public static VersaoProducao getVersaoProducaoAlocadaOuTemporariaSeInexistente(
            VersaoProducao versaoProducao,
            Roteiro roteiro,
            ListaTecnica listaTecnica,
            SupplyNetworkProjection supplyNetworkProjection) {
        
        if (versaoProducao == null || versaoProducao.isVersaoProducaoInexistente()) {
            Optional<VersaoProducao> optionalVersaoProducao = supplyNetworkProjection
                    .getVersaoProducaoViavelPrioritaria(roteiro, listaTecnica);
            if (optionalVersaoProducao.isEmpty()) {
                optionalVersaoProducao = supplyNetworkProjection.getVersaoProducaoPrioritaria(roteiro, listaTecnica);
            }

            return optionalVersaoProducao.orElseGet(() -> {

                /*
                 * Nao ha nenhum candidato viavel ou cadastrado: cria versao
                 * producao temporaria com id = null para envelopar a combinacao
                 * roteiro/lista tecnica recebida pelo fluxo heuristico.
                 */
                VersaoProducao versaoProducaoTemporaria = new VersaoProducao();
                versaoProducaoTemporaria.setId(null);
                versaoProducaoTemporaria.setLocation(roteiro.getLocation());
                versaoProducaoTemporaria.setAtivo(true);
                versaoProducaoTemporaria.setRoteiro(roteiro);
                versaoProducaoTemporaria.setListaTecnica(listaTecnica);
                return versaoProducaoTemporaria;

            });
                    
        } else {
            return versaoProducao;
        }
        
    }

    private void geraErroSeUsoProdutivoDaSentinela() {

        if (isVersaoProducaoInexistente()) {
            throw new IllegalStateException(
                    "Production version sentinel does not expose productive master data");
        }

    }
    
}
