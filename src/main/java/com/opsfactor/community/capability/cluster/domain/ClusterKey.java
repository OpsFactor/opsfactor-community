package com.opsfactor.community.capability.cluster.domain;

import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import lombok.*;
import java.io.Serializable;

/** Identidade canônica de cluster; id numérico isolado identifica somente o legado explícito. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode @ToString
public class ClusterKey implements Serializable {

    // O nome coincide com a associação @Id da entidade; o tipo é o ID String do perfil.
    private String perfilClusterizacao;
    private Long id;

    public static ClusterKey legado(Long id) {

        return new ClusterKey(PerfilClusterizacao.ID_LEGADO, id);

    }
}
