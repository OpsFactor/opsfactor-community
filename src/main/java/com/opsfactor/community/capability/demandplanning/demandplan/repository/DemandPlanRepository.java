package com.opsfactor.community.capability.demandplanning.demandplan.repository;

import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA de DemandPlanRepository.
 */
@Repository
public interface DemandPlanRepository extends JpaRepository<DemandPlan,Long> {

    @Query("SELECT dp FROM DemandPlan dp " +
            "LEFT JOIN FETCH dp.perfilExecucaoDemandPlan pedp")
    List<DemandPlan> customFindAllComPerfilExecucao();

    @Query("SELECT dp FROM DemandPlan dp " +
            "LEFT JOIN FETCH dp.perfilExecucaoDemandPlan pedp " +
            "WHERE dp.id = :id")
    Optional<DemandPlan> customFindByIdComPerfilExecucao(Long id);

    List<DemandPlan> findByTamanhoBucketAndDataInicioPlanoBetween(
            Constantes.TamanhoBucket tamanhoBucket, 
            LocalDate dataInicioPlanoInicial, 
            LocalDate dataInicioPlanoFinal);
    
    List<DemandPlan> findBySupplyPlansIsEmpty();

    /**
     * Remove o Demand Plan pelo id informado.
     */
    @Override
    @Transactional
    public void deleteById(Long id);

    public List<DemandPlan> findByDemandPlanCopiadoNoHorizonteCongeladoId(Long demandPlanId);
    
}
