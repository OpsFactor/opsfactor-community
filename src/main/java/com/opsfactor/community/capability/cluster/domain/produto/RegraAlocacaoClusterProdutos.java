package com.opsfactor.community.capability.cluster.domain.produto;

import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutosStatus.RegraAlocacaoClusterProdutosStatusCompositeKey;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regra de alocacao de materiais em cluster de Demand Planning.
 *
 * <p>No Community, apenas regras por status de material sao funcionais.
 * Regras por caracteristica permanecem no enum/DTO apenas para rejeicao
 * explicita na camada de service, pois caracteristicas dinamicas pertencem ao
 * Enterprise.</p>
 */
@Getter @Setter // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@NoArgsConstructor
@Entity 
@EqualsAndHashCode(of={"id"})
public class RegraAlocacaoClusterProdutos {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    private ClusterProdutos clusterProdutos;

    @Enumerated(EnumType.STRING)
    private Constantes.RegraAlocacaoClusterProdutosTipo regraAlocacaoTipo;

    // Critérios de filtragem : apenas um deles não será nulo, de acordo com criterion
    @OneToMany(mappedBy = "regraAlocacaoClusterProdutosStatusCompositeKey.regraAlocacaoClusterProdutos", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<RegraAlocacaoClusterProdutosStatus> regraAlocacaoClusterProdutosStatusSet = new HashSet<>();

    public void addStatusProduto(Constantes.StatusProduto statusProduto) {
        Optional<RegraAlocacaoClusterProdutosStatus> optionalRegraAlocacaoStatus = regraAlocacaoClusterProdutosStatusSet.stream()
                .filter(x -> x.getStatusProduto().equals(statusProduto))
                .findAny();
        
        if (!optionalRegraAlocacaoStatus.isPresent()) {
            RegraAlocacaoClusterProdutosStatus regraAlocacaoClusterProdutosStatus = new RegraAlocacaoClusterProdutosStatus(
                    new RegraAlocacaoClusterProdutosStatusCompositeKey(this, statusProduto));
            regraAlocacaoClusterProdutosStatusSet.add(regraAlocacaoClusterProdutosStatus);
        }
    }
    
    public Set<Constantes.StatusProduto> getStatusProdutoSet() {
        return regraAlocacaoClusterProdutosStatusSet.stream()
                .map(RegraAlocacaoClusterProdutosStatus::getStatusProduto)
                .collect(Collectors.toSet());
    }

    @ColumnDefault("0")
    private Integer prioridade = 0;

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", criterion=" + regraAlocacaoTipo +
                ", prioridade=" + prioridade +
                '}';
    }
}
