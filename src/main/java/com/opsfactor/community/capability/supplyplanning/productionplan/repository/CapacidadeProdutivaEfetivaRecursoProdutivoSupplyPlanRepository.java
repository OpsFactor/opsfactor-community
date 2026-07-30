package com.opsfactor.community.capability.supplyplanning.productionplan.repository;

import com.opsfactor.community.capability.supplyplanning.productionplan.domain.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository Community de capacidade produtiva efetiva por recurso no Supply Plan.
 *
 * <p>A consulta principal carrega recurso, location e unidade de medida em lote
 * para consumo por projections e persistencias de Supply Planning.</p>
 */
@Repository
public interface CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository
        extends JpaRepository<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan, CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey> {

    /**
     * Remove o snapshot de capacidade produtiva efetiva por recurso do Supply Plan informado.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan capacidade " +
            "WHERE capacidade.capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey.supplyPlan.id = :supplyPlanId")
    void removeBySupplyPlanId(@Param("supplyPlanId") Long supplyPlanId);

    @Query("SELECT capacidade FROM CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan capacidade " +
            "LEFT JOIN FETCH capacidade.capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey.supplyPlan supplyPlan " +
            "LEFT JOIN FETCH capacidade.capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey.recursoProdutivo recursoProdutivo " +
            "LEFT JOIN FETCH recursoProdutivo.location location " +
            "LEFT JOIN FETCH capacidade.unidadeMedidaCapacidade unidadeMedida " +
            "WHERE supplyPlan = :supplyPlan")
    List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> customFindBySupplyPlan(SupplyPlan supplyPlan);

}
