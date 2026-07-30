package com.opsfactor.community.capability.supplyplanning.configuration.repository;


import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA de PerfilExecucaoSupplyPlanRepository.
 */
@Repository
public interface PerfilExecucaoSupplyPlanRepository extends JpaRepository<PerfilExecucaoSupplyPlan,String> {
	 
    @Query("SELECT DISTINCT pesp FROM PerfilExecucaoSupplyPlan pesp " +
            "LEFT JOIN FETCH pesp.setPerfilExecucaoPoliticaEstoques pepe " +
            "LEFT JOIN FETCH pepe.perfilExecucaoPoliticaEstoquesCompositeKey.politicaEstoques")
    List<PerfilExecucaoSupplyPlan> customFindAll();

    @Query("SELECT DISTINCT pesp FROM PerfilExecucaoSupplyPlan pesp " +
            "LEFT JOIN FETCH pesp.setPerfilExecucaoPoliticaEstoques pepe " +
            "LEFT JOIN FETCH pepe.perfilExecucaoPoliticaEstoquesCompositeKey.politicaEstoques " +
            "WHERE pesp.id = :perfilExecucaoSupplyPlanId")
    Optional<PerfilExecucaoSupplyPlan> customFindById(
            String perfilExecucaoSupplyPlanId);
    
}
