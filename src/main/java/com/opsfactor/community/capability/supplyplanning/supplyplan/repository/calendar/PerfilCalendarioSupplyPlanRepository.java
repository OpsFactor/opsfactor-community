package com.opsfactor.community.capability.supplyplanning.supplyplan.repository.calendar;

import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.calendar.PerfilCalendarioSupplyPlan;

import org.springframework.data.jpa.repository.JpaRepository;

/** A cópia tem a identidade do Supply Plan e não depende do cadastro original. */
public interface PerfilCalendarioSupplyPlanRepository extends JpaRepository<PerfilCalendarioSupplyPlan, Long> {
}
