package com.opsfactor.community.capability.cluster.repository;

import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fragmento Spring Data compartilhado por todos os repositórios de clusters.
 * Uma associação que faz parte da PK composta não pode receber diretamente
 * uma entidade de perfil transitória: Hibernate pode tentar persistir o perfil
 * novamente. getReference fixa a identidade gerenciada sem SELECT por cluster.
 */
@Transactional
public class ClusterProfilePersistenceImpl<T> implements ClusterProfilePersistence<T> {

    private final EntityManager entityManager;

    public ClusterProfilePersistenceImpl(EntityManager entityManager) {

        this.entityManager = entityManager;

    }

    @Override
    public <S extends T> S save(S entity) {

        return persistirComPerfilGerenciado(entity, new HashMap<>());

    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {

        Objects.requireNonNull(entities, "Informe os clusters");
        Map<String, PerfilClusterizacao> perfisGerenciados = new HashMap<>();
        List<S> resultados = new ArrayList<>();
        // Uma unidade de trabalho e um cache de referências por lote: nenhum
        // findById/save de repositório em loop nem consulta adicional por item.
        for (S entity : entities) {
            resultados.add(persistirComPerfilGerenciado(entity, perfisGerenciados));
        }
        return resultados;

    }

    @Override
    public <S extends T> S saveAndFlush(S entity) {

        S resultado = save(entity);
        entityManager.flush();
        return resultado;

    }

    @Override
    public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {

        List<S> resultados = saveAll(entities);
        entityManager.flush();
        return resultados;

    }

    /**
     * IDs numéricos nulos são gerados antes do INSERT pelo gerador TABLE.
     * IDs já informados seguem a semântica merge do Spring Data. O perfil é
     * sempre o informado na entidade; nunca se busca um perfil ativo/default.
     * Perfil inexistente falha pela FK no flush, sem fallback ou criação.
     */
    private <S extends T> S persistirComPerfilGerenciado(S entity,
            Map<String, PerfilClusterizacao> perfisGerenciados) {

        Objects.requireNonNull(entity, "Informe o cluster");
        PerfilClusterizacao perfil;
        Long clusterId;
        if (entity instanceof ClusterProdutos cluster) {
            perfil = cluster.getPerfilClusterizacao();
            clusterId = cluster.getId();
        } else if (entity instanceof ClusterLocations cluster) {
            perfil = cluster.getPerfilClusterizacao();
            clusterId = cluster.getId();
        } else {
            throw new IllegalArgumentException("Tipo incompatível com a persistência de clusters");
        }
        if (perfil == null || perfil.getId() == null || perfil.getId().isBlank()) {
            throw new IllegalArgumentException("O cluster exige um perfil de clusterização explícito");
        }
        PerfilClusterizacao referencia = perfisGerenciados.computeIfAbsent(perfil.getId(),
                id -> entityManager.getReference(PerfilClusterizacao.class, id));
        if (entity instanceof ClusterProdutos cluster) {
            cluster.setPerfilClusterizacao(referencia);
        } else if (entity instanceof ClusterLocations cluster) {
            cluster.setPerfilClusterizacao(referencia);
        }
        if (clusterId == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);

    }
}
