package com.opsfactor.community.capability.transactionaldata.inventory.stock.repository;

import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque.EstoqueCompositeKey;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA do snapshot de estoque transacional.
 *
 * <p>No Community esse dado representa somente estoque inicial por
 * location/material/data/UOM/quantidade. Consultas usadas por upload devem
 * evitar leituras amplas do historico inteiro; quando o fluxo precisa
 * reconciliar um batch, use {@link #customFindUploadBatchEnvelope(LocalDateTime, LocalDateTime, Collection, Collection)}
 * para carregar apenas o envelope minimo do arquivo recebido.</p>
 */
@Repository
public interface EstoqueRepository extends JpaRepository<Estoque,EstoqueCompositeKey> {

    Collection<Estoque> findAllByEstoqueCompositeKeyDataReferencia(LocalDateTime dataReferencia);
    Collection<Estoque> findByEstoqueCompositeKeyDataReferenciaAndEstoqueCompositeKeyLocation(
            LocalDateTime dataReferencia, Location location);
    Collection<Estoque> findByEstoqueCompositeKeyDataReferenciaBetweenAndEstoqueCompositeKeyLocationAndEstoqueCompositeKeyProdutoIn(
            LocalDateTime dataInicial, LocalDateTime dataFinal, Location location, Collection<Produto> produtos);
    Collection<Estoque> findByEstoqueCompositeKeyDataReferenciaAndEstoqueCompositeKeyLocationAndEstoqueCompositeKeyProdutoIn(
            LocalDateTime dataEstoque, Location location, Collection<Produto> produtos);
    Collection<Estoque> findByEstoqueCompositeKeyDataReferenciaAndEstoqueCompositeKeyLocationInAndEstoqueCompositeKeyProdutoIn(
            LocalDateTime dataEstoque, Collection<Location> locations, Collection<Produto> produtos);
    Collection<Estoque> findByEstoqueCompositeKeyDataReferenciaAndEstoqueCompositeKeyLocationIn(
            LocalDateTime dataEstoque, Collection<Location> locations);

    Collection<Estoque> findByEstoqueCompositeKeyDataReferenciaBetweenAndEstoqueCompositeKeyLocation(LocalDateTime dataInicial, LocalDateTime dataFinal, Location location);
    List<Estoque> findByEstoqueCompositeKeyDataReferenciaBetween(LocalDateTime dataInicial, LocalDateTime dataFinal);
    List<Estoque> findByEstoqueCompositeKeyDataReferenciaBetweenAndEstoqueCompositeKeyLocationTipoLocationIn(LocalDateTime dataInicial, LocalDateTime dataFinal, Collection<LocationAbstract.TipoLocation> tiposLocation);

    /**
     * Busca somente o envelope necessario para reconciliar um batch de upload.
     *
     * <p>A chave do estoque e composta por location, material e data. Como JPQL
     * portavel nao permite usar tuple IN de forma consistente em todos os bancos
     * suportados, filtramos pelo envelope minimo do lote e deixamos o fluxo
     * generico de integracao fazer o matching exato pela chave primaria em
     * memoria. Isso evita o antigo {@code findAll()} sem transformar o upload
     * em uma consulta por linha.</p>
     */
    @Query("SELECT est FROM Estoque est "
            + "LEFT JOIN FETCH est.unidadeMedida um "
            + "LEFT JOIN FETCH est.estoqueCompositeKey.location locationEntity "
            + "LEFT JOIN FETCH est.estoqueCompositeKey.produto materialEntity "
            + "WHERE est.estoqueCompositeKey.dataReferencia BETWEEN :dataInicial AND :dataFinal "
            + "AND locationEntity.id IN :locationIds "
            + "AND materialEntity.id IN :materialIds")
    List<Estoque> customFindUploadBatchEnvelope(
            @Param("dataInicial") LocalDateTime dataInicial,
            @Param("dataFinal") LocalDateTime dataFinal,
            @Param("locationIds") Collection<String> locationIds,
            @Param("materialIds") Collection<String> materialIds);

    @Query("select e from Estoque e where e.estoqueCompositeKey.produto.id = :materialId " +
            "and e.estoqueCompositeKey.dataReferencia = (select Max(e2.estoqueCompositeKey.dataReferencia) from Estoque e2 where e2.estoqueCompositeKey.produto.id = :materialId)")
    Collection<Estoque> findByLastMaterial(@Param("materialId") String materialId);

    @Query("SELECT MAX(est.estoqueCompositeKey.dataReferencia) "
            + "FROM Estoque est "
            + "WHERE est.estoqueCompositeKey.dataReferencia <= :dataHorarioReferencia")
    Optional<LocalDateTime> getUltimaDataHorarioIgualOuMenorDataHorarioReferencia(
            @Param("dataHorarioReferencia") LocalDateTime dataHorarioReferencia);

    @Query("SELECT est.estoqueCompositeKey.produto AS material, est.estoqueCompositeKey.location AS location, "
            + "um AS uom, "
            + "SUM(est.quantidade) AS totalQuantity "
            + "FROM Estoque est "
            + "LEFT JOIN est.unidadeMedida um "
            + "WHERE est.estoqueCompositeKey.dataReferencia = :dataReferencia "
            + "AND est.estoqueCompositeKey.location IN :locations "
            + "AND est.estoqueCompositeKey.produto IN :produtos "
            + "GROUP BY est.estoqueCompositeKey.produto, est.estoqueCompositeKey.location, est.unidadeMedida")
    List<AggregatedByLocationMaterialUOM> consolidatedStockQuantityByMaterialAndLocation(
            @Param("dataReferencia") LocalDateTime dataReferencia,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT est.estoqueCompositeKey.produto AS material, est.estoqueCompositeKey.location AS location, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(est.estoqueCompositeKey.dataReferencia) AS referenceDate, "
            + "SUM(est.quantidade) AS totalQuantity "
            + "FROM Estoque est "
            + "LEFT JOIN est.unidadeMedida um "
            + "WHERE est.estoqueCompositeKey.dataReferencia BETWEEN :dataInicio AND :dataFim "
            + "AND est.estoqueCompositeKey.location IN :locations "
            + "AND est.estoqueCompositeKey.produto IN :produtos "
            + "GROUP BY est.estoqueCompositeKey.produto, est.estoqueCompositeKey.location, DATA_SEM_HORARIO(est.estoqueCompositeKey.dataReferencia), est.unidadeMedida")
    List<AggregatedByLocationMaterialUOMDate> consolidatedStockQuantityByMaterialAndLocationAndReferenceDate(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    /**
     * Consolida snapshots de estoque somente nas datas de referência pedidas.
     *
     * <p>Fluxos que calculam métricas por período devem passar apenas os fins
     * dos períodos. Isso evita carregar a faixa diária completa para depois
     * descartar snapshots intermediários em memória.</p>
     */
    @Query("SELECT est.estoqueCompositeKey.produto AS material, est.estoqueCompositeKey.location AS location, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(est.estoqueCompositeKey.dataReferencia) AS referenceDate, "
            + "SUM(est.quantidade) AS totalQuantity "
            + "FROM Estoque est "
            + "LEFT JOIN est.unidadeMedida um "
            + "WHERE DATA_SEM_HORARIO(est.estoqueCompositeKey.dataReferencia) IN :datasReferencia "
            + "AND est.estoqueCompositeKey.location IN :locations "
            + "AND est.estoqueCompositeKey.produto IN :produtos "
            + "GROUP BY est.estoqueCompositeKey.produto, est.estoqueCompositeKey.location, "
            + "DATA_SEM_HORARIO(est.estoqueCompositeKey.dataReferencia), est.unidadeMedida")
    List<AggregatedByLocationMaterialUOMDate> consolidatedStockQuantityByMaterialAndLocationAndReferenceDates(
            @Param("datasReferencia") Collection<LocalDate> datasReferencia,
            @Param("locations") Collection<Location> locations,
            @Param("produtos") Collection<Produto> produtos);

    @Query("SELECT est.estoqueCompositeKey.produto AS material, est.estoqueCompositeKey.location AS location, "
            + "um AS uom, "
            + "SUM(est.quantidade) AS totalQuantity "
            + "FROM Estoque est "
            + "LEFT JOIN est.unidadeMedida um "
            + "WHERE est.estoqueCompositeKey.dataReferencia = :dataReferencia "
            + "GROUP BY est.estoqueCompositeKey.produto, est.estoqueCompositeKey.location, est.unidadeMedida")
    List<AggregatedByLocationMaterialUOM> consolidatedStockQuantityByMaterialAndLocation(
            @Param("dataReferencia") LocalDateTime dataReferencia);

    @Query("SELECT est.estoqueCompositeKey.produto AS material, "
            + "um AS uom, "
            + "SUM(est.quantidade) AS totalQuantity "
            + "FROM Estoque est "
            + "LEFT JOIN est.unidadeMedida um "
            + "WHERE est.estoqueCompositeKey.dataReferencia = :dataReferencia "
            + "AND est.estoqueCompositeKey.location = :location "
            + "AND est.estoqueCompositeKey.produto IN :produtos "
            + "GROUP BY est.estoqueCompositeKey.produto, est.unidadeMedida")
    List<AggregatedByMaterialUOM> consolidatedStockQuantityByMaterial(
            @Param("dataReferencia") LocalDateTime dataReferencia,
            @Param("location") Location location,
            @Param("produtos") Collection<Produto> produtos);


    /**
     * Remove historico de estoque das locations informadas.
     */
    @Transactional
    void removeByEstoqueCompositeKeyLocationIn(Collection<Location> locations);

    /**
     * Remove historico de estoque no intervalo de datas informado.
     */
    @Transactional
    void removeByEstoqueCompositeKeyDataReferenciaBetween(LocalDateTime dataInicial, LocalDateTime dataFinal);

    /**
     * Remove historico de estoque no intervalo de datas informado filtrando tipos de location.
     */
    @Transactional
    void removeByEstoqueCompositeKeyDataReferenciaBetweenAndEstoqueCompositeKeyLocationTipoLocationIn(LocalDateTime dataInicial, LocalDateTime dataFinal, Collection<LocationAbstract.TipoLocation> tiposLocation);

}
