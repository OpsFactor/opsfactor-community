package com.opsfactor.community.capability.demandplanning.demandplan.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repository Community das linhas oficiais de Demand Plan.
 *
 * <p>As consultas agregadas por calendario retornam {@link List} para preservar
 * a cardinalidade entregue pelo banco. A deduplicacao por chave funcional deve
 * acontecer nas factories/projections consumidoras, onde o erro consegue receber
 * contexto de calendario, material, location e unidade de medida.</p>
 */
@Repository
public interface DemandPlanItemRepository extends JpaRepository<DemandPlanItem,DemandPlanItem.DemandPlanItemKey> {
    
    Boolean existsByKeyDemandPlanIdAndKeyLocation(
            Long demandPlanId, Location location);    

    /**
     * Carrega em lote as linhas de um plano para um conjunto de locations.
     *
     * <p>Além das dimensões da chave, a UOM precisa estar inicializada no
     * mesmo snapshot: pós-processamentos Enterprise, como o espelhamento de
     * locations, convertem os baselines sem poder disparar uma consulta lazy
     * por linha.</p>
     */
    @Query("SELECT dpl FROM DemandPlanItem dpl " +
            "INNER JOIN FETCH dpl.key.demandPlan dp " + 
            "INNER JOIN FETCH dpl.key.location loc " + 
            "INNER JOIN FETCH dpl.key.produto p " +
            "LEFT JOIN FETCH dpl.unidadeMedida " +
            "WHERE loc IN :locations " +
            "AND dp.id = :demandPlanId ")
    List<DemandPlanItem> customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyLocationInLocations(
            Long demandPlanId, Collection<Location> locations);
    
    List<DemandPlanItem> findByKeyDemandPlanIdAndKeyLocation(
            Long demandPlanId, Location location);
            
    @Query("SELECT dpl FROM DemandPlanItem dpl " +
            "INNER JOIN FETCH dpl.key.demandPlan dp " + 
            "INNER JOIN FETCH dpl.key.location loc " + 
            "INNER JOIN FETCH dpl.key.produto p " + 
            "WHERE loc IN :locationCollection " +
            "AND dp.id = :demandPlanId " +
            "AND p IN :produtoCollection")
    List<DemandPlanItem> customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyLocationInAndDemandPlanItemKeyProdutoIn(
            Long demandPlanId, Collection<Location> locationCollection, Collection<Produto> produtoCollection);
    
    @Query("SELECT dpl FROM DemandPlanItem dpl " +
            "INNER JOIN FETCH dpl.key.demandPlan dp " + 
            "INNER JOIN FETCH dpl.key.location loc " + 
            "INNER JOIN FETCH dpl.key.produto p " + 
            "WHERE loc IN :locationCollection " +
            "AND dp.id = :demandPlanId " +
            "AND p IN :produtoCollection " + 
            "AND dpl.key.dataReferencia = :dataReferencia")
    List<DemandPlanItem> customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyLocationInAndDemandPlanItemKeyProdutoInAndDemandPlanItemKeyDataReferencia(
            Long demandPlanId, Collection<Location> locationCollection, Collection<Produto> produtoCollection, LocalDateTime dataReferencia);

    @Query("SELECT dpl FROM DemandPlanItem dpl " +
            "INNER JOIN FETCH dpl.key.demandPlan dp " +
            "INNER JOIN FETCH dpl.key.location loc " +
            "INNER JOIN FETCH dpl.key.produto p " +
            "WHERE loc IN :locationCollection " +
            "AND dp.id = :demandPlanId " +
            "AND p IN :produtoCollection " +
            "AND dpl.key.dataReferencia BETWEEN :dataReferenciaInicial AND :dataReferenciaFinal")
    List<DemandPlanItem> customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyLocationInAndDemandPlanItemKeyProdutoInAndDemandPlanItemKeyDataReferenciaBetween(
            Long demandPlanId,
            Collection<Location> locationCollection,
            Collection<Produto> produtoCollection,
            LocalDateTime dataReferenciaInicial,
            LocalDateTime dataReferenciaFinal);

    /**
     * Carrega a fotografia completa das linhas standard de um Demand Plan para
     * uma exportação detalhada. As dimensões e a UOM seguem fetchadas em um
     * único batch para que consumidores Enterprise não inicializem relações
     * lazy por linha do arquivo.
     */
    @Query("SELECT dpl FROM DemandPlanItem dpl "
            + "INNER JOIN FETCH dpl.key.demandPlan dp "
            + "INNER JOIN FETCH dpl.key.location loc "
            + "INNER JOIN FETCH dpl.key.produto p "
            + "LEFT JOIN FETCH dpl.unidadeMedida "
            + "WHERE dp.id = :demandPlanId")
    List<DemandPlanItem> customFindSnapshotForDetailedExport(Long demandPlanId);

    /**
     * Carrega uma fotografia de periodo das linhas standard de um Demand Plan
     * para a exportacao detalhada. Dimensoes e UOM seguem no mesmo batch para
     * impedir inicializacao lazy por linha do arquivo.
     */
    @Query("SELECT dpl FROM DemandPlanItem dpl "
            + "INNER JOIN FETCH dpl.key.demandPlan dp "
            + "INNER JOIN FETCH dpl.key.location loc "
            + "INNER JOIN FETCH dpl.key.produto p "
            + "LEFT JOIN FETCH dpl.unidadeMedida "
            + "WHERE dp.id = :demandPlanId "
            + "AND dpl.key.dataReferencia BETWEEN :dataReferenciaInicial AND :dataReferenciaFinal")
    List<DemandPlanItem> customFindSnapshotForDetailedExport(
            Long demandPlanId,
            LocalDateTime dataReferenciaInicial,
            LocalDateTime dataReferenciaFinal);
    
    @Query("SELECT dpl FROM DemandPlanItem dpl " +
            "INNER JOIN FETCH dpl.key.demandPlan dp " + 
            "INNER JOIN FETCH dpl.key.location loc " + 
            "INNER JOIN FETCH dpl.key.produto p " + 
            "WHERE loc IN :locationCollection " +
            "AND dp = :demandPlan")
    List<DemandPlanItem> customFindByDemandPlanItemKeyDemandPlanAndDemandPlanItemKeyLocationIn(
            DemandPlan demandPlan, Collection<Location> locationCollection);

    @Query("SELECT dpl FROM DemandPlanItem dpl " +
            "INNER JOIN FETCH dpl.key.demandPlan dp " + 
            "INNER JOIN FETCH dpl.key.location loc " + 
            "INNER JOIN FETCH dpl.key.produto p " + 
            "WHERE dp.id = :demandPlanId " +
            "AND p = :produto")
    Collection<DemandPlanItem> customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyProduto(
            Long demandPlanId, Produto produto);
    
    @Query("select max(dpl.key.dataReferencia) FROM DemandPlanItem dpl " +
            "WHERE dpl.key.demandPlan.id = :demandPlanId")
    LocalDateTime customFindMaxDataReferencia(@Param("demandPlanId") Long demandPlanId);
    
    @Query("SELECT dpl FROM DemandPlanItem dpl " +
            "INNER JOIN FETCH dpl.key.demandPlan dp " + 
            "INNER JOIN FETCH dpl.key.location loc " + 
            "INNER JOIN FETCH dpl.key.produto p " + 
            "WHERE loc =:location " +
            "AND dp.id =:demandPlanId")
    List<DemandPlanItem> customFindByDemandPlanIdAndLocation(
            @Param("demandPlanId") String demandPlanId,
            @Param("location") Location location);

    /**
     * Remove linhas de Demand Plan associadas as locations informadas.
     */
    void removeByKeyLocationIn(Collection<Location> locations);
    
    /**
     * Remove linhas oficiais do Demand Plan informado.
     */
    @Transactional
    void removeByKeyDemandPlanId(Long demandPlanId);
    
    /**
     * Zera as quantidades atendidas do plano de demanda restrito para as locations informadas.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE DemandPlanItem dpl "
            + "SET dpl.quantidadeBaselineAtendida = 0, "
            + "dpl.quantidadeUpliftAtendida = 0, "
            + "dpl.quantidadeItensNovosAtendida = 0, "
            + "dpl.quantidadeAjusteDemandaAtendida = 0 "
            + "WHERE dpl.key.demandPlan = :demandPlan "
            + "AND dpl.key.location IN :locations")
    public void zeraPlanoDemandaRestritoByDemandPlanAndLocationIn(DemandPlan demandPlan, Collection<Location> locations);
            
    @Query("SELECT dpl.key.produto AS material, "
            + "dpl.key.location AS location, "
            + "um AS uom, "
            + "ULTIMO_DIA_MES_SEM_HORARIO(dpl.key.dataReferencia)  AS referenceDate, "
            + "SUM(COALESCE(dpl.quantidadeBaseline, 0) + COALESCE(dpl.quantidadeAjusteDemanda, 0)) AS totalQuantity "
            + "FROM DemandPlanItem dpl "
            + "LEFT JOIN dpl.unidadeMedida um "
            + "WHERE dpl.key.dataReferencia BETWEEN :dataInicio AND :dataFim "
            + "AND dpl.key.demandPlan = :demandPlan "
            + "AND dpl.key.location IN :locations "
            + "AND dpl.key.produto IN :produtos "
            + "GROUP BY dpl.key.produto, dpl.key.location, dpl.unidadeMedida, ULTIMO_DIA_MES_SEM_HORARIO(dpl.key.dataReferencia)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedDemandPlanItemByLocationMaterialUOMMonthForProductsLocations(
            @Param("demandPlan") DemandPlan demandPlan, 
            @Param("dataInicio") LocalDateTime dataInicio, 
            @Param("dataFim") LocalDateTime dataFim, 
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT dpl.key.produto AS material, "
            + "dpl.key.location AS location, "
            + "um AS uom, "
            + "ULTIMO_DIA_MES_SEM_HORARIO(dpl.key.dataReferencia)  AS referenceDate, "
            + "SUM(COALESCE(dpl.quantidadeBaseline, 0) + COALESCE(dpl.quantidadeAjusteDemanda, 0)) AS totalQuantity "
            + "FROM DemandPlanItem dpl "
            + "LEFT JOIN dpl.unidadeMedida um "
            + "WHERE dpl.key.dataReferencia BETWEEN :dataInicio AND :dataFim "
            + "AND dpl.key.demandPlan = :demandPlan "
            + "GROUP BY dpl.key.produto, dpl.key.location, dpl.unidadeMedida, ULTIMO_DIA_MES_SEM_HORARIO(dpl.key.dataReferencia)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedDemandPlanItemByLocationMaterialUOMMonth(
            @Param("demandPlan") DemandPlan demandPlan, 
            @Param("dataInicio") LocalDateTime dataInicio, 
            @Param("dataFim") LocalDateTime dataFim);
    
    @Query("SELECT dpl.key.produto AS material, "
            + "dpl.key.location AS location, "
            + "um AS uom, "
            + "DOMINGO_DA_SEMANA_SEM_HORARIO(dpl.key.dataReferencia)  AS referenceDate, "
            + "SUM(COALESCE(dpl.quantidadeBaseline, 0) + COALESCE(dpl.quantidadeAjusteDemanda, 0)) AS totalQuantity "
            + "FROM DemandPlanItem dpl "
            + "LEFT JOIN dpl.unidadeMedida um "
            + "WHERE dpl.key.dataReferencia BETWEEN :dataInicio AND :dataFim "
            + "AND dpl.key.demandPlan = :demandPlan "
            + "AND dpl.key.location IN :locations "
            + "AND dpl.key.produto IN :produtos "
            + "GROUP BY dpl.key.produto, dpl.key.location, dpl.unidadeMedida, DOMINGO_DA_SEMANA_SEM_HORARIO(dpl.key.dataReferencia)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedDemandPlanItemByLocationMaterialUOMWeekForProductsLocations(
            @Param("demandPlan") DemandPlan demandPlan, 
            @Param("dataInicio") LocalDateTime dataInicio, 
            @Param("dataFim") LocalDateTime dataFim, 
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT dpl.key.produto AS material, "
            + "dpl.key.location AS location, "
            + "um AS uom, "
            + "DOMINGO_DA_SEMANA_SEM_HORARIO(dpl.key.dataReferencia)  AS referenceDate, "
            + "SUM(COALESCE(dpl.quantidadeBaseline, 0) + COALESCE(dpl.quantidadeAjusteDemanda, 0)) AS totalQuantity "
            + "FROM DemandPlanItem dpl "
            + "LEFT JOIN dpl.unidadeMedida um "
            + "WHERE dpl.key.dataReferencia BETWEEN :dataInicio AND :dataFim "
            + "AND dpl.key.demandPlan = :demandPlan "
            + "GROUP BY dpl.key.produto, dpl.key.location, dpl.unidadeMedida, DOMINGO_DA_SEMANA_SEM_HORARIO(dpl.key.dataReferencia)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedDemandPlanItemByLocationMaterialUOMWeek(
            @Param("demandPlan") DemandPlan demandPlan, 
            @Param("dataInicio") LocalDateTime dataInicio, 
            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT dpl.key.produto AS material, "
            + "dpl.key.location AS location, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(dpl.key.dataReferencia)  AS referenceDate, "
            + "SUM(COALESCE(dpl.quantidadeBaseline, 0) + COALESCE(dpl.quantidadeAjusteDemanda, 0)) AS totalQuantity "
            + "FROM DemandPlanItem dpl "
            + "LEFT JOIN dpl.unidadeMedida um "
            + "WHERE dpl.key.dataReferencia BETWEEN :dataInicio AND :dataFim "
            + "AND dpl.key.demandPlan = :demandPlan "
            + "AND dpl.key.location IN :locations "
            + "AND dpl.key.produto IN :produtos "
            + "GROUP BY dpl.key.produto, dpl.key.location, dpl.unidadeMedida, DATA_SEM_HORARIO(dpl.key.dataReferencia)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedDemandPlanItemByLocationMaterialUOMDayForProductsLocations(
            @Param("demandPlan") DemandPlan demandPlan, 
            @Param("dataInicio") LocalDateTime dataInicio, 
            @Param("dataFim") LocalDateTime dataFim, 
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT dpl.key.produto AS material, "
            + "dpl.key.location AS location, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(dpl.key.dataReferencia)  AS referenceDate, "
            + "SUM(COALESCE(dpl.quantidadeBaseline, 0) + COALESCE(dpl.quantidadeAjusteDemanda, 0)) AS totalQuantity "
            + "FROM DemandPlanItem dpl "
            + "LEFT JOIN dpl.unidadeMedida um "
            + "WHERE dpl.key.dataReferencia BETWEEN :dataInicio AND :dataFim "
            + "AND dpl.key.demandPlan = :demandPlan "
            + "GROUP BY dpl.key.produto, dpl.key.location, dpl.unidadeMedida, DATA_SEM_HORARIO(dpl.key.dataReferencia)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedDemandPlanItemByLocationMaterialUOMDay(
            @Param("demandPlan") DemandPlan demandPlan, 
            @Param("dataInicio") LocalDateTime dataInicio, 
            @Param("dataFim") LocalDateTime dataFim);
    
}
