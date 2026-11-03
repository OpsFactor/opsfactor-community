package com.opsfactor.community.capability.configuration.domain.cluster;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

/**
 * Versão explícita de uma classificação conjunta de materiais e locations.
 * Perfis usados por planos, AutoFits ou sharding são preservados: alterações
 * de composição criam outro identificador, sem reatribuir históricos.
 */
@Entity
@Table(name = "perfil_clusterizacao")
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(of = "id") @ToString(of = "id")
public class PerfilClusterizacao implements Serializable {

    /** Perfil padrão obrigatório; conserva o ID técnico usado nas referências anteriores. */
    public static final String ID_LEGADO = "LEGADO";
    public static final String DESCRICAO_PADRAO = "Perfil padrão de clusterização";

    @Id @Column(length = 50, nullable = false, updatable = false)
    private String id;
    private String descricao;
    @Version
    private long version;

    public PerfilClusterizacao(String id, String descricao) {

        if (id == null || id.isBlank() || id.length() > 50)
            throw new IllegalArgumentException("Identificador do perfil de clusterização inválido");
        this.id = id;
        this.descricao = descricao;

    }

    /** Referência não gerenciada, sem consulta, persistência ou escolha de perfil ativo.
     * Antes de gravar associações, o service deve resolver uma referência gerenciada
     * por getReferenceById: usar este token diretamente em uma associação @Id pode
     * fazer o Hibernate tentar persistir novamente o perfil existente.
     */
    public static PerfilClusterizacao referenciaLegada() {

        return new PerfilClusterizacao(ID_LEGADO, null);

    }
}
