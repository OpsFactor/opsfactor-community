package com.opsfactor.community.capability.masterdata.calendar.profile.repository;

import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendarioSimples;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consulta tipada que impede Demand Planning de resolver uma extensão não uniforme. */
public interface PerfilCalendarioSimplesRepository extends JpaRepository<PerfilCalendarioSimples, String> {
}
