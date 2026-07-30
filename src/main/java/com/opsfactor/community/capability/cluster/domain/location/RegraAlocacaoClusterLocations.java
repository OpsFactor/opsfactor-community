package com.opsfactor.community.capability.cluster.domain.location;

import com.opsfactor.community.platform.utility.Constantes;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Regra de alocacao de locations em cluster de location.
 *
 * <p>No Community, regras por tipo de location e pais/estado sao funcionais.
 * Regras por caracteristica permanecem no enum/DTO apenas para rejeicao
 * explicita na camada de service, pois caracteristicas dinamicas pertencem ao
 * Enterprise.</p>
 */
@Getter @Setter // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@NoArgsConstructor
@Entity 
@EqualsAndHashCode(of={"id"})
public class RegraAlocacaoClusterLocations {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    private ClusterLocations clusterLocations;

    /**
     * CARACTERISTICA, PAIS_ESTADO
     */
    @Enumerated(EnumType.STRING)
    private Constantes.RegraAlocacaoClusterLocationsTipo regraAlocacaoTipo;

    @OneToMany(mappedBy = "regraAlocacaoClusterLocationsPaisEstadoCompositeKey.regraAlocacaoClusterLocations", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<RegraAlocacaoClusterLocationsPaisEstado> regrasAlocacaoClusterLocationsPaisEstadoSet = new HashSet<>();

    @OneToMany(mappedBy = "regraAlocacaoClusterLocationsTipoLocationCompositeKey.regraAlocacaoClusterLocations", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<RegraAlocacaoClusterLocationsTipoLocation> regrasAlocacaoClusterLocationsTipoLocationSet = new HashSet<>();

    @ColumnDefault("0")
    private Integer prioridade = 0;

    public void addRegraAlocacaoTipoLocation(RegraAlocacaoClusterLocationsTipoLocation regraAlocacaoClusterLocationsTipoLocation){
        regrasAlocacaoClusterLocationsTipoLocationSet.add(regraAlocacaoClusterLocationsTipoLocation);
    }
    public void addRegraAlocacaoPaisEstado(RegraAlocacaoClusterLocationsPaisEstado regraAlocacaoClusterLocationsPaisEstado){
        regrasAlocacaoClusterLocationsPaisEstadoSet.add(regraAlocacaoClusterLocationsPaisEstado);
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", criterion=" + regraAlocacaoTipo +
                ", prioridade=" + prioridade +
                '}';
    }
}
