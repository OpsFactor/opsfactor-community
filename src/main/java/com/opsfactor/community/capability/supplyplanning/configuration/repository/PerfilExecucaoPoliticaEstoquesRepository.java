package com.opsfactor.community.capability.supplyplanning.configuration.repository;


import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoPoliticaEstoques;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA de PerfilExecucaoPoliticaEstoquesRepository.
 */
@Repository
public interface PerfilExecucaoPoliticaEstoquesRepository extends JpaRepository<PerfilExecucaoPoliticaEstoques,PerfilExecucaoPoliticaEstoques.PerfilExecucaoPoliticaEstoquesCompositeKey> {
	
    @Query("SELECT DISTINCT pepe FROM PerfilExecucaoPoliticaEstoques pepe " +
            "LEFT JOIN FETCH pepe.perfilExecucaoPoliticaEstoquesCompositeKey.politicaEstoques pe " +
            "LEFT JOIN FETCH pe.politicaEstoquesMaterialLocationList peml " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.material mat " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.location loc " +
            "WHERE pepe.perfilExecucaoPoliticaEstoquesCompositeKey.perfilExecucaoSupplyPlan.id = :perfilExecucaoSupplyPlanId")
    public List<PerfilExecucaoPoliticaEstoques> customFindByPerfilExecucaoSupplyPlan(
            String perfilExecucaoSupplyPlanId);
    
}
