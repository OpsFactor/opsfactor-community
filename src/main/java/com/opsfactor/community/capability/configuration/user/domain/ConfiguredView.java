package com.opsfactor.community.capability.configuration.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import lombok.*;
import org.springframework.lang.NonNull;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Configuracao Community de uma view de Planning Book.
 *
 * <p>O Community persiste preferencias basicas de view, unidade de medida,
 * janela historica, comportamento de edicao e a selecao de key figures
 * standard dos catalogos publicos. Filtros por atributos públicos de material
 * e location também pertencem ao Community; agrupamentos por característica,
 * filtros DFU e key figures privadas/customizadas pertencem ao Enterprise e
 * são bloqueados nas services de front. Os identificadores
 * escalares de workflow permanecem no agregado compartilhado para que o
 * overlay Enterprise possa reconcilia-los sem introduzir uma tabela de
 * vinculo; o Community nao os interpreta nem permite configura-los.</p>
 */
@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@EqualsAndHashCode(of="configuredViewCompositeKey")
@NoArgsConstructor 
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
public class ConfiguredView {
    
    
    public enum TipoView {
        @JsonProperty("Supply Planning Book") SUPPLYPLANNINGBOOK,
        @JsonProperty("Demand Planning Book") DEMANDPLANNINGBOOK;
    }
    
    public enum TipoAgregacao {
        @JsonProperty("Group by Characteristic") GROUPBY, 
        @JsonProperty("Do Not Show Characteristic") DESCONSIDERAR;
    }
    
    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private ConfiguredViewCompositeKey configuredViewCompositeKey;

    
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ConfiguredViewCompositeKey implements Serializable {

        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        @Column(length = 50)
        private String userId;
        
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        @Column(length = 100)
        private String nomeView;
        
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        @Enumerated(EnumType.STRING)
        private TipoView tipoView;
        
    }
    
    // determinam se a exibição é feita apenas em nível agregado ou se desce também para o nível material/location
    private Boolean exibeMateriais;
    private Boolean exibeLocations;
    
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedidaView;

    /**
     * Campo temporário, apenas para ajustes no front-end DP
     * Indica para qual key figure os ajustes feitos na linha Direct Demand serão propagados
     */
    private String keyFigureAjustesDemandaDiretaTotal;

    /**
     * Identificador escalar do workflow de Demand Planning selecionado pela
     * view Enterprise.
     *
     * <p>Nao ha relacao JPA nem FK para uma entidade privada: o Community
     * preserva a coluna compartilhada sem resolver seu significado, enquanto o
     * overlay Enterprise valida o par workflow/etapa no repository oficial.</p>
     */
    @Column(length = 50)
    private String demandPlanWorkflowId;

    /**
     * Identificador escalar da etapa do workflow de Demand Planning
     * selecionada pela view Enterprise. Forma, junto com
     * {@link #demandPlanWorkflowId}, uma chave logica externa ao Community.
     */
    @Column(length = 50)
    private String demandPlanWorkflowStageId;
    
    private Integer numeroPeriodosHistoricosDemandPlanningBook;
        
    private Boolean submissaoAutomaticaAlteracoes;
    
    private Boolean permiteAlteracaoHorizonteCongelado;

    private Boolean exibeVendaMediaHistorica;
    
    private Boolean exibeMateriaisDescontinuados;
    
    private Boolean exibeDfusSemFaturamentoNoHorizonteHistorico;

    /**
     * Filtro Community persistido por identificadores de material.
     *
     * <p>A coleção vazia significa todos os materiais ativos. A coleção é
     * carregada por subselect quando uma lista de views é aberta, evitando uma
     * consulta adicional por view e sem introduzir o modelo de características
     * dinâmicas reservado ao Enterprise.</p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "configured_view_material_id_filter")
    @Column(name = "material_id", length = 100, nullable = false)
    @Fetch(FetchMode.SUBSELECT)
    private Set<String> materialIdFilterSet = new LinkedHashSet<>();

    /**
     * Filtro Community persistido por identificadores de location.
     *
     * <p>A semântica e a estratégia de carregamento são idênticas ao filtro de
     * materiais: vazio representa todas as locations ativas e o snapshot é
     * carregado em lote para a tela de User Views.</p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "configured_view_location_id_filter")
    @Column(name = "location_id", length = 100, nullable = false)
    @Fetch(FetchMode.SUBSELECT)
    private Set<String> locationIdFilterSet = new LinkedHashSet<>();

    /**
     * Filtros por atributos públicos de material, sem semântica de agrupamento.
     *
     * <p>Entradas da mesma característica são alternativas (OR); características
     * diferentes são cumulativas (AND). O carregamento por subselect evita uma
     * consulta por view na administração.</p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "configured_view_material_characteristic_filter")
    @Fetch(FetchMode.SUBSELECT)
    private Set<ConfiguredViewCharacteristicFilter> materialCharacteristicFilterSet = new LinkedHashSet<>();

    /** Filtros equivalentes aplicados aos atributos públicos de location. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "configured_view_location_characteristic_filter")
    @Fetch(FetchMode.SUBSELECT)
    private Set<ConfiguredViewCharacteristicFilter> locationCharacteristicFilterSet = new LinkedHashSet<>();
    
    public TipoView getTipoView() {
        return getConfiguredViewCompositeKey().getTipoView();
    }
    
    public boolean getExibeMateriais() {
        return (exibeMateriais == null) ? false : exibeMateriais;
    }
    
    public boolean getExibeLocations() {
        return (exibeLocations == null) ? false : exibeLocations;
    }
    
    public UnidadeMedida getUnidadeMedidaView(ParametrosGlobais parametrosGlobais) {
        if (unidadeMedidaView == null) {
            switch (getTipoView()) {
                case DEMANDPLANNINGBOOK:
                    return parametrosGlobais.getUnidadeMedidaPadraoDP();
                case SUPPLYPLANNINGBOOK:
                    return parametrosGlobais.getUnidadeMedidaPadraoSNP();
            }
            return parametrosGlobais.getUnidadeMedidaPadraoSNP();
        } else {
            return unidadeMedidaView;
        }
    }

    /**
     * Retorna somente a unidade explicitamente persistida na view.
     *
     * <p>Ao contrário de {@link #getUnidadeMedidaView(ParametrosGlobais)},
     * este método não aplica o default global. Ele permite que superfícies de
     * administração preservem a diferença entre uma UOM configurada e uma
     * view que deliberadamente herda o default do seu Planning Book.</p>
     */
    @JsonIgnore
    public UnidadeMedida getUnidadeMedidaViewCadastrada() {

        return unidadeMedidaView;

    }

    
    public String getUserId() {
        return getConfiguredViewCompositeKey().getUserId();
    }
    
    public String getNomeView() {
        return configuredViewCompositeKey.getNomeView();
    }
    
    public int getNumeroPeriodosHistoricosDemandPlanningBook() {
        return (numeroPeriodosHistoricosDemandPlanningBook == null) ? 0 : Math.max(numeroPeriodosHistoricosDemandPlanningBook, 0);
    }
    
    /**
     * Retorna a selecao persistida de key figures da view.
     *
     * <p>A selecao e persistida em {@link ConfiguredViewKeyFigure}, filha
     * unidirecional da view, para evitar uma colecao inversa lazy neste
     * agregado. Este getter legado permanece vazio: consumidores da selecao
     * usam o repository batch e a KeyFigureProjectionFactory.</p>
     */
    public Set<String> getKeyFigures() {
        return Set.of();
    }
        
    public boolean getSubmissaoAutomaticaAlteracoes() {
        return (submissaoAutomaticaAlteracoes == null) ? false : submissaoAutomaticaAlteracoes;
    }
    public Boolean getSubmissaoAutomaticaAlteracoesCadastrado() {
        return submissaoAutomaticaAlteracoes;
    }
    
    public boolean getPermiteAlteracaoHorizonteCongelado() {
        return (permiteAlteracaoHorizonteCongelado == null) ? false : permiteAlteracaoHorizonteCongelado;
    }
    public Boolean getPermiteAlteracaoHorizonteCongeladoCadastrado() {
        return permiteAlteracaoHorizonteCongelado;
    }
    
    public boolean getExibeVendaMediaHistorica() {
        return (exibeVendaMediaHistorica == null) ? false : exibeVendaMediaHistorica;
    }
    public Boolean getExibeVendaMediaHistoricaCadastrada() {
        return exibeVendaMediaHistorica;
    }
    
    public boolean getExibeMateriaisDescontinuados() {
        return (exibeMateriaisDescontinuados == null) ? true : exibeMateriaisDescontinuados;
    }
    public Boolean getExibeMateriaisDescontinuadosCadastrado() {
        return exibeMateriaisDescontinuados;
    }
    
    public boolean getExibeDfusSemFaturamentoNoHorizonteHistorico() {
        return (exibeDfusSemFaturamentoNoHorizonteHistorico == null) ? true : exibeDfusSemFaturamentoNoHorizonteHistorico;
    }
    public Boolean getExibeDfusSemFaturamentoNoHorizonteHistoricoCadastrado() {
        return exibeDfusSemFaturamentoNoHorizonteHistorico;
    }
    
    public String getKeyFigureAjustesDemandaDiretaTotal() {
        return (keyFigureAjustesDemandaDiretaTotal == null) ? MetodosUtilidade.getValorJsonPropertyDeEnum(KeyFigureStandardEnum.AJUSTE_DEMANDA) : keyFigureAjustesDemandaDiretaTotal;
    }
    public String getKeyFigureAjustesDemandaDiretaTotalCadastrado() {
        return keyFigureAjustesDemandaDiretaTotal;
    }
    
}
