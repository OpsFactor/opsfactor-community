package com.opsfactor.community.capability.supplyplanning.supplyplan.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;

/**
 * Repository JPA de DemandaDiretaConsideradaLinhaRepository.
 */
@Repository
public interface DemandaDiretaConsideradaLinhaRepository extends JpaRepository<DemandaDiretaConsideradaLinha, DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey> {

    /**
     * Verifica de forma leve se a fotografia de demanda direta do Supply Plan
     * foi persistida e pode ser preservada em uma reexecucao.
     *
     * @param supplyPlanId identificador do Supply Plan.
     * @return {@code true} quando existe ao menos uma linha da fotografia.
     */
    boolean existsByDemandaDiretaConsideradaLinhaCompositeKeySupplyPlanId(Long supplyPlanId);
     
    /**
     * Remove linhas de demanda direta considerada do Supply Plan informado.
     */
    @Transactional
    public void removeAllByDemandaDiretaConsideradaLinhaCompositeKeySupplyPlanId(Long supplyPlanId);

    /**
     * Copia a demanda direta irrestrita para o campo restrito do Supply Plan informado.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE DemandaDiretaConsideradaLinha ddc "
            + "SET ddc.quantidadeDemandaDiretaPlanoDemandaRestrita = ddc.quantidadeDemandaDiretaPlanoDemandaIrrestrita "
            + "WHERE ddc.demandaDiretaConsideradaLinhaCompositeKey.supplyPlan.id = :supplyPlanId")
    public void atualizaPlanoRestritoComPlanoIrrestrito(Long supplyPlanId);

    @Query("SELECT dpril FROM DemandaDiretaConsideradaLinha dpril " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.supplyPlan sp " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.location loc " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.material mat " +
            "LEFT JOIN FETCH dpril.unidadeMedida um " +
            "WHERE dpril.demandaDiretaConsideradaLinhaCompositeKey.supplyPlan = :supplyPlan")
    public Collection<DemandaDiretaConsideradaLinha> customFindAllBySupplyPlan(SupplyPlan supplyPlan);

    @Query("SELECT dpril FROM DemandaDiretaConsideradaLinha dpril " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.supplyPlan sp " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.location loc " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.material mat " +
            "LEFT JOIN FETCH dpril.unidadeMedida um " +
            "WHERE dpril.demandaDiretaConsideradaLinhaCompositeKey.supplyPlan = :supplyPlan " +
            "AND dpril.demandaDiretaConsideradaLinhaCompositeKey.dataReferencia BETWEEN :dataInicial AND :dataFinal")
    public Collection<DemandaDiretaConsideradaLinha> customFindAllBySupplyPlanAndDataReferenciaBetween(
            SupplyPlan supplyPlan,
            LocalDateTime dataInicial,
            LocalDateTime dataFinal);

    @Query("SELECT dpril FROM DemandaDiretaConsideradaLinha dpril " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.supplyPlan sp " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.location loc " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.material mat " +
            "LEFT JOIN FETCH dpril.unidadeMedida um " +
            "WHERE dpril.demandaDiretaConsideradaLinhaCompositeKey.supplyPlan = :supplyPlan " +
            "AND loc = :location")
    public Collection<DemandaDiretaConsideradaLinha> customFindAllBySupplyPlanAndLocation(SupplyPlan supplyPlan, Location location);

    @Query("SELECT dpril FROM DemandaDiretaConsideradaLinha dpril " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.supplyPlan sp " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.location loc " +
            "LEFT JOIN FETCH dpril.demandaDiretaConsideradaLinhaCompositeKey.material mat " +
            "LEFT JOIN FETCH dpril.unidadeMedida um " +
            "WHERE dpril.demandaDiretaConsideradaLinhaCompositeKey.supplyPlan.id IN :supplyPlanIds")
    public Collection<DemandaDiretaConsideradaLinha> customFindAllBySupplyPlanSet(Set<Long> supplyPlanIds);

}
