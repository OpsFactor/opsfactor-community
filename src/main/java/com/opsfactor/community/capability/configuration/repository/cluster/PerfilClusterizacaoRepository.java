package com.opsfactor.community.capability.configuration.repository.cluster;

import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import org.springframework.data.jpa.repository.JpaRepository;

/** Catálogo de versões explícitas; referências legadas são migradas, nunca criadas em leitura. */
public interface PerfilClusterizacaoRepository extends JpaRepository<PerfilClusterizacao, String> {
}
