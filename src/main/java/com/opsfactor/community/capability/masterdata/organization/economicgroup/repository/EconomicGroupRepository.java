package com.opsfactor.community.capability.masterdata.organization.economicgroup.repository;

import com.opsfactor.community.capability.masterdata.organization.economicgroup.domain.EconomicGroup;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso batch aos cabeçalhos de grupo econômico compartilhados.
 *
 * <p>O repositório não publica CRUD Community para o conceito: ele apenas
 * permite que consumidores Enterprise resolvam a FK de {@code Location} sem
 * executar uma consulta por linha em integrações de dados.</p>
 */
public interface EconomicGroupRepository extends JpaRepository<EconomicGroup, String> {
}
