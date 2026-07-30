package com.opsfactor.community.capability.demandplanning.demandplan.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.HistoricoDemandPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;


/**
 * Repository Community das linhas historicas usadas por Demand Planning.
 *
 * <p>As consultas carregam location/material em lote para evitar acesso lazy
 * linha a linha durante montagem de snapshots de planejamento.</p>
 */
@Repository
public interface HistoricoDemandPlanItemRepository extends JpaRepository<HistoricoDemandPlanItem,HistoricoDemandPlanItem.HistoricoDemandPlanItemKey> {

    List<HistoricoDemandPlanItem> findByKeyDemandPlanIdAndKeyLocationInAndKeyProdutoInAndKeyDataReferencia(
            Long demandPlanId, Collection<Location> locationCollection, Collection<Produto> materialCollection, LocalDateTime dataReferencia);

    @Query("SELECT DISTINCT hdpl FROM HistoricoDemandPlanItem hdpl "
            + "LEFT JOIN FETCH hdpl.key.location loc "
            + "LEFT JOIN FETCH hdpl.key.produto prd "
            + "WHERE hdpl.key.demandPlan.id = :demandPlanId "
            + "AND hdpl.key.location IN :locationCollection "
            + "AND hdpl.key.produto IN :materialCollection")
    List<HistoricoDemandPlanItem> customFindByHistoricoDemandPlanItemKeyDemandPlanIdAndHistoricoDemandPlanItemKeyLocationInAndHistoricoDemandPlanItemKeyProdutoIn(
            Long demandPlanId, Collection<Location> locationCollection, Collection<Produto> materialCollection);

    /**
     * Remove o historico de linhas do Demand Plan informado.
     */
    @Transactional
    public void deleteByKeyDemandPlanId(Long demandPlanId);

}
