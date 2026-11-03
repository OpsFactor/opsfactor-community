package com.opsfactor.community.capability.cluster.repository;

import java.util.*;

/**
 * Borda única de escrita de clusters. Identidades de perfil recebidas como
 * tokens são resolvidas como referências JPA gerenciadas antes do persist.
 * Isso vale também para integrações e inicializadores, sem depender do front.
 */
public interface ClusterProfilePersistence<T> {

    <S extends T> S save(S entity);
    <S extends T> List<S> saveAll(Iterable<S> entities);
    <S extends T> S saveAndFlush(S entity);
    <S extends T> List<S> saveAllAndFlush(Iterable<S> entities);
}
