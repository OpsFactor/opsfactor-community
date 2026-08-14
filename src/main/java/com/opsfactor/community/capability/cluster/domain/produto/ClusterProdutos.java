package com.opsfactor.community.capability.cluster.domain.produto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


/**
 * Raiz persistida dos clusters de materiais.
 *
 * <p>A classificacao de materiais e unica para a plataforma: os consumidores
 * usam o mesmo conjunto de clusters, prioridades e regras de alocacao, sem
 * uma variante por processo.</p>
 */
@Entity
@Data
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_cluster")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@NamedEntityGraph(
    name = "ClusterProdutos.completo", attributeNodes = {
        @NamedAttributeNode("regrasAlocacaoClusterProdutos")
    }
)
public class ClusterProdutos implements Serializable {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    private String descricao;

    public ClusterProdutos(String descricao, Boolean padrao, Integer prioridade) {
        this.descricao = descricao;
        this.padrao = padrao;
        this.prioridade = prioridade;
    }

    /**
     * Se true, este representa o cluster padrão que será usado 
     * em caso de materiais que não se enquadrem nos demais clusters.
     */
    @ColumnDefault("false")
    private Boolean padrao = false;

    /**
     * Prioridade de avaliacao das regras de alocacao de materiais.
     *
     * <p>O nome fisico da entidade ainda usa `Produtos`, mas novos comentarios
     * e contratos publicos devem falar em material.</p>
     */
    private Integer prioridade;

    /**
     * Regras persistidas do cluster de materiais.
     *
     * <p>No Community, apenas regras por status de material sao aceitas pela
     * service layer. Regras por caracteristica permanecem apenas como schema
     * transicional para rejeicao funcional.</p>
     */
    @OneToMany(mappedBy = "clusterProdutos",cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<RegraAlocacaoClusterProdutos> regrasAlocacaoClusterProdutos = new HashSet<>();

    public Integer getPrioridade() {
        if (getPadrao()) {
            return Integer.MAX_VALUE;
        } else {
            return (prioridade == null) ? 99999 : Math.min(99999, prioridade);
        }
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
