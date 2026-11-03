package com.opsfactor.community.capability.cluster.repository;

import com.opsfactor.community.capability.cluster.domain.ClusterKey;
import org.springframework.data.jpa.repository.JpaRepository;
import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.*;

/**
 * Identidade JPA canônica é sempre (perfil,id). Os overloads Long são uma
 * borda legada explícita e fixa em LEGADO; nunca procuram em outro perfil.
 */
@NoRepositoryBean
public interface ClusterProfileRepository<T> extends JpaRepository<T, ClusterKey>, ClusterProfilePersistence<T> {

    Optional<T> findByPerfilClusterizacaoIdAndId(String perfilClusterizacaoId, Long id);
    List<T> findAllByPerfilClusterizacaoId(String perfilClusterizacaoId);

    /** Leitura sem contexto pertence exclusivamente ao catálogo LEGADO. */
    @Override @Deprecated
    default List<T> findAll() {

        return findAllByPerfilClusterizacaoId(PerfilClusterizacao.ID_LEGADO);

    }

    @Deprecated
    default Optional<T> findById(Long id) {

        return findById(ClusterKey.legado(id));

    }

    @Deprecated
    default T getReferenceById(Long id) {

        return getReferenceById(ClusterKey.legado(id));

    }

    @Deprecated
    default T getById(Long id) {

        return getReferenceById(ClusterKey.legado(id));

    }

    @Deprecated
    default T getOne(Long id) {

        return getReferenceById(ClusterKey.legado(id));

    }

    @Deprecated
    default boolean existsById(Long id) {

        return existsById(ClusterKey.legado(id));

    }

    @Deprecated
    default void deleteById(Long id) {

        deleteById(ClusterKey.legado(id));

    }

    @Deprecated
    default List<T> findAllById(Collection<Long> ids) {

        return findAllById(ids.stream().map(ClusterKey::legado).toList());

    }
}
