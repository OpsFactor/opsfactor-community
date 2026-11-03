package com.opsfactor.community.capability.cluster.domain.location;

import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import com.opsfactor.community.capability.cluster.domain.ClusterKey;

import com.opsfactor.community.capability.configuration.domain.cluster.location.ParametrosClusterLocations;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Cluster de locations usado por Demand Planning, Supply Planning e Planning
 * Book Community.
 *
 * <p>O Community permite clusters operacionais de location e regras simples de
 * alocacao por pais/estado ou tipo de location. Caracteristicas dinamicas,
 * capacidades logisticas, visao em mapa e agrupamentos Enterprise devem ser
 * bloqueados nas bordas de service/controller, nao reintroduzidos nesta
 * entidade fisica compartilhada.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@NamedEntityGraph(
        name = "ClusterLocations.completo", attributeNodes = {
        @NamedAttributeNode("regrasAlocacaoClusterLocations")
})
@IdClass(ClusterKey.class)
public class ClusterLocations implements Serializable {

    /** Parte real da PK. LEGADO é compatibilidade explícita, nunca escolha de perfil ativo. */
    @Id @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_clusterizacao_id", nullable = false, updatable = false)
    private PerfilClusterizacao perfilClusterizacao = PerfilClusterizacao.referenciaLegada();

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "cluster_location_ids")
    @TableGenerator(name = "cluster_location_ids", table = "cluster_id_allocator", pkColumnName = "segmento",
            valueColumnName = "proximo_valor", pkColumnValue = "LOCATION", allocationSize = 50)
    private Long id;
    
    private String descricao;

    /**
     * Se true, este representa o cluster padrão que será usado
     * em caso de locations que não se enquadrem nos demais clusters.
     */
    @ColumnDefault("false")
    private Boolean padrao = false;

    /**
     * Prioridade de avaliacao das regras de alocacao de locations.
     *
     * <p>O cluster padrao recebe prioridade maxima tecnica para ficar como
     * fallback depois das regras especificas.</p>
     */
    private Integer prioridade;

    /*
     * Fetch LAZY evita ciclo eager no bootstrap JPA: ParametrosClusterLocations
     * referencia ClusterLocations de volta pela chave composta.
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy="parametrosClusterLocationsCompositeKey.clusterLocations", orphanRemoval = true, fetch = FetchType.LAZY)
    private ParametrosClusterLocations parametrosClusterLocations;

    public ClusterLocations(String descricao, Boolean padrao, Integer prioridade) {
        this.descricao = descricao;
        this.padrao = padrao;
        this.prioridade = prioridade;
    }
        
    public ParametrosClusterLocations getParametrosClusterLocations() {
        if (parametrosClusterLocations == null) {
            ParametrosClusterLocations parametrosClusterLocations = new ParametrosClusterLocations(new ParametrosClusterLocations.ParametrosClusterLocationsCompositeKey(this));
            setParametrosClusterLocations(parametrosClusterLocations);
            return parametrosClusterLocations;
        } else {
            return this.parametrosClusterLocations;
        }
    }
    /**
     * Regras persistidas do cluster. O recorte Community aceita apenas os
     * subtipos simples validados por `ClusteringFrontService`.
     */
    @OneToMany(mappedBy = "clusterLocations",cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<RegraAlocacaoClusterLocations> regrasAlocacaoClusterLocations = new HashSet<>();

    public Integer getPrioridade() {
        if (getPadrao()) {
            return Integer.MAX_VALUE;
        } else {
            return (prioridade == null) ? 99999 : Math.min(99999, prioridade);
        }
    }

    /** Identidade completa sem consulta; IDs de proxies JPA não exigem carregar o perfil. */
    @EqualsAndHashCode.Include
    public ClusterKey getClusterKey() {

        return new ClusterKey(perfilClusterizacao.getId(), id);

    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", descricao='" + descricao + '\'' +
                ", padrao=" + padrao +
                ", prioridade=" + prioridade +
                '}';
    }

}
