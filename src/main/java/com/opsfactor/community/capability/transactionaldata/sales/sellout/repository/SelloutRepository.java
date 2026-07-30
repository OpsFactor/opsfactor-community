package com.opsfactor.community.capability.transactionaldata.sales.sellout.repository;

import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.FirstLastByLocation;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.FirstLastByMaterial;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.FirstLastByMaterialLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repository JPA de SelloutRepository.
 */
@Repository
public interface SelloutRepository extends JpaRepository<Sellout,String> {

    @Query("SELECT so FROM Sellout so "
            + "LEFT JOIN FETCH so.unidadeMedida um "
            + "LEFT JOIN FETCH so.produto p "
            + "LEFT JOIN FETCH so.locationOrigem lo "
            + "WHERE so.id IN :selloutIds")
    public List<Sellout> customFindBySelloutIdIn(@Param("selloutIds") Collection<String> selloutIds);

    @Query("SELECT so FROM Sellout so "
            + "LEFT JOIN FETCH so.unidadeMedida um "
            + "LEFT JOIN FETCH so.produto p "
            + "LEFT JOIN FETCH so.locationOrigem lo "
            + "WHERE so.dataVenda BETWEEN :dataInicio AND :dataFim")
    public List<Sellout> customFindByDataVendaBetween(@Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT so FROM Sellout so "
            + "LEFT JOIN FETCH so.unidadeMedida um "
            + "LEFT JOIN FETCH so.produto p "
            + "LEFT JOIN FETCH so.locationOrigem lo "
            + "WHERE so.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND lo.tipoLocation IN :tiposLocation")
    public List<Sellout> customFindByDataVendaBetweenAndLocationDestinoTypeIn(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("tiposLocation") Collection<LocationAbstract.TipoLocation> tiposLocation);

    @Query("SELECT so FROM Sellout so "
            + "LEFT JOIN FETCH so.unidadeMedida um "
            + "LEFT JOIN FETCH so.produto p "
            + "LEFT JOIN FETCH so.locationOrigem lo "
            + "WHERE so.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND so.locationOrigem IN :locations "
            + "AND so.produto IN :produtos")
    public List<Sellout> customFindByDataVendaBetweenMaterialInAndLocationIn(
            @Param("dataInicio") LocalDateTime dataInicio, @Param("dataFim") LocalDateTime dataFim,
            @Param("locations") Collection<Location> locations, @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT so FROM Sellout so "
            + "LEFT JOIN FETCH so.unidadeMedida um "
            + "LEFT JOIN FETCH so.produto p "
            + "LEFT JOIN FETCH so.locationOrigem lo")
    public List<Sellout> customFindAll();

    /**
     * Usado para extração acumulada das vendas por produto em um grupo de locations
     * Usado em:
     * ClusteringService.getProdutosDeClusterProdutosClusterLocationsComVendaOuEstoque : extração de produtos com vendas
     * @param dataInicio
     * @param dataFim
     * @param clusterLocations
     * @return
     */
    @Query("SELECT pl.produto AS material, "
            + "um AS uom, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem IN :locations "
            + "AND pl.produto IN :produtos "
            + "GROUP BY pl.produto, pl.unidadeMedida")
    List<AggregatedByMaterialUOM> consolidatedSelloutByMaterialUOMAtLocations(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT pl.produto AS material, "
            + "um AS uom, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem.id IN :locationIds "
            + "AND pl.produto.id IN :materialIds "
            + "GROUP BY pl.produto, pl.unidadeMedida")
    List<AggregatedByMaterialUOM> consolidatedSelloutByMaterialUOMAtLocationIds(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locationIds") Collection<String> locationIds,
            @Param("materialIds") Collection<String> materialIds);

    /**
     * Usado para extração acumulada das vendas por material/location
     * Usado em:
     * UnidadeConversaoFrontService.getUnidadeConversaoFaltanteDPListDTO
     * @param dataInicio
     * @param dataFim
     * @param clusterLocations
     * @return
     */
    @Query("SELECT pl.produto AS material, pl.locationOrigem AS location, "
            + "um AS uom, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem IN :locations "
            + "AND pl.produto IN :produtos "
            + "GROUP BY pl.produto, pl.locationOrigem, pl.unidadeMedida")
    List<AggregatedByLocationMaterialUOM> consolidatedSelloutByLocationMaterialUOMAtLocations(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT pl.produto AS material, pl.locationOrigem AS location, "
            + "um AS uom, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem.id IN :locationIds "
            + "AND pl.produto.id IN :materialIds "
            + "GROUP BY pl.produto, pl.locationOrigem, pl.unidadeMedida")
    List<AggregatedByLocationMaterialUOM> consolidatedSelloutByLocationMaterialUOMAtLocationIds(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locationIds") Collection<String> locationIds,
            @Param("materialIds") Collection<String> materialIds);

    /**
     * Usado para extração acumulada das vendas por material/location
     */
    @Query("SELECT pl.produto AS material, pl.locationOrigem AS location, "
            + "um AS uom, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "GROUP BY pl.produto, pl.locationOrigem, pl.unidadeMedida")
    List<AggregatedByLocationMaterialUOM> consolidatedSelloutByLocationMaterialUOM(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);


    @Query("SELECT pl.produto AS material, "
            + "um AS uom, "
            + "DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)  AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem = :location "
            + "GROUP BY pl.produto, pl.unidadeMedida, DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByMaterialUOMDate> consolidatedSelloutByMaterialUOMWeekAtLocation(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("location") Location location);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "um AS uom, "
            + "ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda)  AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem = :location "
            + "GROUP BY pl.produto, pl.unidadeMedida, ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByMaterialUOMDate> consolidatedSelloutByMaterialUOMMonthAtLocation(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("location") Location location);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(pl.dataVenda) AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um " // necessário pois select pl.unidadeMedida AS uom gera um inner join no SQL executado que remove linhas com unidademedida nulo
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem = :location "
            + "GROUP BY pl.produto, DATA_SEM_HORARIO(pl.dataVenda), pl.unidadeMedida")
    List<AggregatedByMaterialUOMDate> consolidatedSelloutByMaterialUOMDayAtLocation(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("location") Location location);

    @Query("SELECT pl.produto AS material, "
            + "um AS uom, "
            + "DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)  AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem IN :locations "
            + "AND pl.produto IN :produtos "
            + "GROUP BY pl.produto, pl.unidadeMedida, DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByMaterialUOMDate> consolidatedSelloutByMaterialUOMWeekForProductsLocations(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT pl.produto AS material, "
            + "um AS uom, "
            + "DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)  AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem.id IN :locationIds "
            + "AND pl.produto.id IN :materialIds "
            + "GROUP BY pl.produto, pl.unidadeMedida, DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByMaterialUOMDate> consolidatedSelloutByMaterialUOMWeekForMaterialLocationIds(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locationIds") Collection<String> locationIds,
            @Param("materialIds") Collection<String> materialIds);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "um AS uom, "
            + "ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda)  AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem IN :locations "
            + "AND pl.produto IN :produtos "
            + "GROUP BY pl.produto, pl.unidadeMedida, ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByMaterialUOMDate> consolidatedSelloutByMaterialUOMMonthForProductsLocations(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "um AS uom, "
            + "ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda)  AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem.id IN :locationIds "
            + "AND pl.produto.id IN :materialIds "
            + "GROUP BY pl.produto, pl.unidadeMedida, ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByMaterialUOMDate> consolidatedSelloutByMaterialUOMMonthForMaterialLocationIds(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locationIds") Collection<String> locationIds,
            @Param("materialIds") Collection<String> materialIds);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(pl.dataVenda) AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um " // necessário pois select pl.unidadeMedida AS uom gera um inner join no SQL executado que remove linhas com unidademedida nulo
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem IN :locations "
            + "AND pl.produto IN :produtos "
            + "GROUP BY pl.produto, DATA_SEM_HORARIO(pl.dataVenda), pl.unidadeMedida")
    List<AggregatedByMaterialUOMDate> consolidatedSelloutByMaterialUOMDayForProductsLocations(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(pl.dataVenda) AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem.id IN :locationIds "
            + "AND pl.produto.id IN :materialIds "
            + "GROUP BY pl.produto, DATA_SEM_HORARIO(pl.dataVenda), pl.unidadeMedida")
    List<AggregatedByMaterialUOMDate> consolidatedSelloutByMaterialUOMDayForMaterialLocationIds(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locationIds") Collection<String> locationIds,
            @Param("materialIds") Collection<String> materialIds);

    @Query("SELECT pl.produto AS material, "
            + "pl.locationOrigem AS location, "
            + "um AS uom, "
            + "DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)  AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "GROUP BY pl.produto, pl.locationOrigem, pl.unidadeMedida, DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedSelloutByLocationMaterialUOMWeek(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT pl.produto AS material, "
            + "pl.locationOrigem AS location, "
            + "um AS uom, "
            + "DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)  AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem IN :locations "
            + "AND pl.produto IN :produtos "
            + "GROUP BY pl.produto, pl.locationOrigem, pl.unidadeMedida, DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedSelloutByLocationMaterialUOMWeekForProductsLocations(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT pl.produto AS material, "
            + "pl.locationOrigem AS location, "
            + "um AS uom, "
            + "DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)  AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem.id IN :locationIds "
            + "AND pl.produto.id IN :materialIds "
            + "GROUP BY pl.produto, pl.locationOrigem, pl.unidadeMedida, DOMINGO_DA_SEMANA_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedSelloutByLocationMaterialUOMWeekForMaterialLocationIds(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locationIds") Collection<String> locationIds,
            @Param("materialIds") Collection<String> materialIds);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "pl.locationOrigem AS location, "
            + "um AS uom, "
            + "ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda) AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "GROUP BY pl.produto, pl.locationOrigem, pl.unidadeMedida, ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedSelloutByLocationMaterialUOMMonth(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "pl.locationOrigem AS location, "
            + "um AS uom, "
            + "ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda) AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem IN :locations "
            + "AND pl.produto IN :produtos "
            + "GROUP BY pl.produto, pl.locationOrigem, pl.unidadeMedida, ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedSelloutByLocationMaterialUOMMonthForProductsLocations(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "pl.locationOrigem AS location, "
            + "um AS uom, "
            + "ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda) AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um "
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem.id IN :locationIds "
            + "AND pl.produto.id IN :materialIds "
            + "GROUP BY pl.produto, pl.locationOrigem, pl.unidadeMedida, ULTIMO_DIA_MES_SEM_HORARIO(pl.dataVenda)")
    List<AggregatedByLocationMaterialUOMDate> consolidatedSelloutByLocationMaterialUOMMonthForMaterialLocationIds(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locationIds") Collection<String> locationIds,
            @Param("materialIds") Collection<String> materialIds);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "pl.locationOrigem AS location, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(pl.dataVenda) AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um " // necessário pois select pl.unidadeMedida AS uom gera um inner join no SQL executado que remove linhas com unidademedida nulo
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "GROUP BY pl.produto, pl.locationOrigem, DATA_SEM_HORARIO(pl.dataVenda), pl.unidadeMedida")
    List<AggregatedByLocationMaterialUOMDate> consolidatedSelloutByLocationMaterialUOMDay(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "pl.locationOrigem AS location, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(pl.dataVenda) AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um " // necessário pois select pl.unidadeMedida AS uom gera um inner join no SQL executado que remove linhas com unidademedida nulo
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem IN :locations "
            + "AND pl.produto IN :produtos "
            + "GROUP BY pl.produto, pl.locationOrigem, DATA_SEM_HORARIO(pl.dataVenda), pl.unidadeMedida")
    List<AggregatedByLocationMaterialUOMDate> consolidatedSelloutByLocationMaterialUOMDayForProductsLocations(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT "
            + "pl.produto AS material, "
            + "pl.locationOrigem AS location, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(pl.dataVenda) AS referenceDate, "
            + "SUM(COALESCE(pl.quantidade, 0)) AS totalQuantity "
            + "FROM Sellout pl "
            + "LEFT JOIN pl.unidadeMedida um " // necessário pois select pl.unidadeMedida AS uom gera um inner join no SQL executado que remove linhas com unidademedida nulo
            + "WHERE pl.dataVenda BETWEEN :dataInicio AND :dataFim "
            + "AND pl.locationOrigem.id IN :locationIds "
            + "AND pl.produto.id IN :materialIds "
            + "GROUP BY pl.produto, pl.locationOrigem, DATA_SEM_HORARIO(pl.dataVenda), pl.unidadeMedida")
    List<AggregatedByLocationMaterialUOMDate> consolidatedSelloutByLocationMaterialUOMDayForMaterialLocationIds(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locationIds") Collection<String> locationIds,
            @Param("materialIds") Collection<String> materialIds);


    @Query("SELECT MIN(pl.dataVenda) FROM Sellout pl")
    LocalDateTime customFindPrimeiroSellout();

    @Query("SELECT MIN(pl.dataVenda) AS firstDateTime, MAX(pl.dataVenda) AS lastDateTime, pl.produto AS material, pl.locationOrigem AS location FROM Sellout pl " +
            "GROUP BY pl.produto, pl.locationOrigem")
    List<FirstLastByMaterialLocation> findFirstLastSelloutPorMaterialLocation();

    @Query("SELECT MIN(pl.dataVenda) AS firstDateTime, MAX(pl.dataVenda) AS lastDateTime,pl.locationOrigem AS location FROM Sellout pl " +
            "GROUP BY pl.locationOrigem")
    List<FirstLastByLocation> findFirstLastSelloutPorLocation();

    @Query("SELECT MIN(pl.dataVenda) AS firstDateTime, MAX(pl.dataVenda) AS lastDateTime, pl.produto AS material FROM Sellout pl " +
            "GROUP BY pl.produto")
    List<FirstLastByMaterial> findFirstLastSelloutPorMaterial();

    @Query("SELECT MAX(pl.dataVenda) FROM Sellout pl")
    LocalDateTime customFindUltimoSellout();

    /**
     * Remove historico sell-out pelos ids de location de origem informados.
     */
    @Transactional
    void deleteByLocationOrigemIdIn(List<String> listaLocationOrigemId);

    /**
     * Remove historico sell-out das locations de origem informadas.
     */
    @Transactional
    void removeByLocationOrigemIn(Collection<Location> locations);

    /**
     * Remove historico sell-out no intervalo de datas informado.
     */
    @Transactional
    void deleteByDataVendaBetween(LocalDateTime inicio, LocalDateTime fim);

}
