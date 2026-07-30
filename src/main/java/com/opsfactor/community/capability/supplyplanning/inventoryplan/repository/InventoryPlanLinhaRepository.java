package com.opsfactor.community.capability.supplyplanning.inventoryplan.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha.InventoryPlanLinhaCompositeKey;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDatePlanType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repository JPA das linhas de Inventory Plan.
 *
 * <p>Os snapshots agregados por bucket de calendario retornam {@link List}
 * para preservar a cardinalidade da consulta ate a factory/projection
 * consumidora. A colecao nao deve deduplicar linhas no boundary do repository,
 * pois duplicidades estruturais precisam chegar ao ponto que conhece a chave
 * funcional de Supply Planning e consegue falhar com contexto suficiente.</p>
 */
@Repository
public interface InventoryPlanLinhaRepository extends JpaRepository<InventoryPlanLinha,InventoryPlanLinhaCompositeKey> {

    /**
     * Traz todas as linhas do plano de distribuição com origem em uma location, para uma data creation_date do plano de distribuição
     * @param supplyPlanId
     * @param location
     * @return
     */
    Collection<InventoryPlanLinha> findByInventoryPlanLinhaCompositeKeySupplyPlanIdAndInventoryPlanLinhaCompositeKeyLocation(
            Long supplyPlanId, Location location);

    /**
     *
     * @param supplyPlanId
     * @param location
     * @param produtos
     * @return
     */
    List<InventoryPlanLinha> findByInventoryPlanLinhaCompositeKeySupplyPlanIdAndInventoryPlanLinhaCompositeKeyLocationAndInventoryPlanLinhaCompositeKeyProdutoIn(
            Long supplyPlanId , Location location, Collection<Produto> produtos);

    List<InventoryPlanLinha> findByInventoryPlanLinhaCompositeKeySupplyPlanIdAndInventoryPlanLinhaCompositeKeyLocationInAndInventoryPlanLinhaCompositeKeyProdutoIn(
            Long supplyPlanId , Collection<Location> locations, Collection<Produto> produtos);

    /**
     * Lista, em forma estritamente escalar e paginada, as chaves que ainda
     * possuem quantidade efetiva nas colunas baseline depreciadas.
     *
     * <p>Zero e nulo nao bloqueiam o runtime: ambos nao alteram a serie
     * canonica. A consulta nao faz fetch de entidades, evitando N+1 no gate
     * anterior a projections e exportacoes.</p>
     */
    @Query("SELECT ipl.inventoryPlanLinhaCompositeKey.supplyPlan.id AS supplyPlanId, "
            + "ipl.inventoryPlanLinhaCompositeKey.location.id AS locationId, "
            + "ipl.inventoryPlanLinhaCompositeKey.produto.id AS materialId, "
            + "ipl.inventoryPlanLinhaCompositeKey.dataReferencia AS referenceDate, "
            + "ipl.quantidadeEstoqueSegurancaBaseline AS safetyStockBaselineUnconstrained, "
            + "ipl.quantidadeEstoqueMaximoBaseline AS maximumStockBaselineUnconstrained, "
            + "ipl.quantidadeEstoqueSegurancaRestritoBaseline AS safetyStockBaselineConstrained, "
            + "ipl.quantidadeEstoqueMaximoRestritoBaseline AS maximumStockBaselineConstrained, "
            + "ipl.quantidadeEstoqueBaseline AS projectedStockBaselineUnconstrained, "
            + "ipl.quantidadeEstoqueRestritoBaseline AS projectedStockBaselineConstrained "
            + "FROM InventoryPlanLinha ipl "
            + "WHERE ipl.inventoryPlanLinhaCompositeKey.supplyPlan.id IN :supplyPlanIds "
            + "AND (COALESCE(ipl.quantidadeEstoqueSegurancaBaseline, 0) <> 0 "
            + "OR COALESCE(ipl.quantidadeEstoqueMaximoBaseline, 0) <> 0 "
            + "OR COALESCE(ipl.quantidadeEstoqueSegurancaRestritoBaseline, 0) <> 0 "
            + "OR COALESCE(ipl.quantidadeEstoqueMaximoRestritoBaseline, 0) <> 0 "
            + "OR COALESCE(ipl.quantidadeEstoqueBaseline, 0) <> 0 "
            + "OR COALESCE(ipl.quantidadeEstoqueRestritoBaseline, 0) <> 0)")
    List<InventoryPlanLegacyBaselineRequirement> findLegacyBaselineRequirementsBySupplyPlanIdIn(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds,
            Pageable pageable);

    /**
     * Conta todas as pendencias baseline efetivas do envelope sem materializar
     * entidades ou limitar a evidencia retornada ao operador.
     */
    @Query("SELECT COUNT(ipl) FROM InventoryPlanLinha ipl "
            + "WHERE ipl.inventoryPlanLinhaCompositeKey.supplyPlan.id IN :supplyPlanIds "
            + "AND (COALESCE(ipl.quantidadeEstoqueSegurancaBaseline, 0) <> 0 "
            + "OR COALESCE(ipl.quantidadeEstoqueMaximoBaseline, 0) <> 0 "
            + "OR COALESCE(ipl.quantidadeEstoqueSegurancaRestritoBaseline, 0) <> 0 "
            + "OR COALESCE(ipl.quantidadeEstoqueMaximoRestritoBaseline, 0) <> 0 "
            + "OR COALESCE(ipl.quantidadeEstoqueBaseline, 0) <> 0 "
            + "OR COALESCE(ipl.quantidadeEstoqueRestritoBaseline, 0) <> 0)")
    long countLegacyBaselineRequirementsBySupplyPlanIdIn(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds);

    /**
     * Carrega e bloqueia somente as linhas do envelope aprovado para o
     * cutover offline. A escrita continua restrita ao executor interno; esta
     * consulta nao e usada por projection, exportacao ou calculo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ipl FROM InventoryPlanLinha ipl "
            + "WHERE ipl.inventoryPlanLinhaCompositeKey.supplyPlan.id IN :supplyPlanIds")
    List<InventoryPlanLinha> findBySupplyPlanIdInForPersistedBaselineCutover(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds);

    @Query("SELECT ipl FROM InventoryPlanLinha ipl "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.supplyPlan sp "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.location loc "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.produto p "
            + "LEFT JOIN FETCH ipl.unidadeMedida um "
            + "WHERE sp.id = :supplyPlanId "
            + "AND loc IN :locations")
    List<InventoryPlanLinha> findByInventoryPlanLinhaCompositeKeySupplyPlanIdAndInventoryPlanLinhaCompositeKeyLocationInLocations(
            Long supplyPlanId, Collection<Location> locations);

    @Query("SELECT ipl FROM InventoryPlanLinha ipl "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.supplyPlan sp "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.location loc "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.produto p "
            + "LEFT JOIN FETCH ipl.unidadeMedida um "
            + "WHERE sp = :supplyPlan ")
        Collection<InventoryPlanLinha> customFindBySupplyPlan(
            @Param("supplyPlan") SupplyPlan supplyPlan);

    /**
     * Exporta linhas de Inventory Plan para um envelope de Supply Plans.
     *
     * <p>Usado pelo Data Upload read-only para evitar N+1 em location,
     * material, Supply Plan e UOM quando a infraestrutura generica consulta
     * chaves recebidas.</p>
     */
    @Query("SELECT ipl FROM InventoryPlanLinha ipl "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.supplyPlan sp "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.location loc "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.produto p "
            + "LEFT JOIN FETCH ipl.unidadeMedida um "
            + "WHERE sp.id IN :supplyPlanIds ")
        Collection<InventoryPlanLinha> customFindBySupplyPlanIdInForInventoryPlanExport(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds);

    @Query("SELECT ipl FROM InventoryPlanLinha ipl "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.supplyPlan sp "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.location loc "
            + "LEFT JOIN FETCH ipl.inventoryPlanLinhaCompositeKey.produto p "
            + "LEFT JOIN FETCH ipl.unidadeMedida um "
            + "WHERE sp = :supplyPlan "
            + "AND loc IN :locations "
            + "AND p IN :materiais")
        Collection<InventoryPlanLinha> customFindBySupplyPlanELocationsDeListaEMateriaisDeLista(
            @Param("supplyPlan") SupplyPlan supplyPlan,
            @Param("locations") Collection<Location> locations,
            @Param("materiais") Collection<Produto> materiais);

    /**
     * Remove linhas de estoque do Supply Plan associadas as locations informadas.
     */
    @Transactional
    void removeByInventoryPlanLinhaCompositeKeyLocationIn(Collection<Location> locations);

    /**
     * Remove todas as linhas de estoque do Supply Plan informado.
     */
    @Transactional
    void removeByInventoryPlanLinhaCompositeKeySupplyPlanId(Long supplyPlanId);

    /**
     * Copia as quantidades irrestritas de estoque para os campos restritos do Supply Plan informado.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE InventoryPlanLinha ipl "
            + "SET ipl.quantidadeEstoqueSegurancaRestrito = ipl.quantidadeEstoqueSegurancaIrrestrito, "
            + "ipl.quantidadeEstoqueMaximoRestrito = ipl.quantidadeEstoqueMaximoIrrestrito, "
            + "ipl.quantidadeEstoqueProjetadoRestrito = ipl.quantidadeEstoqueProjetadoIrrestrito "
            + "WHERE ipl.inventoryPlanLinhaCompositeKey.supplyPlan.id = :supplyPlanId")
    public void atualizaPlanoRestritoComPlanoIrrestrito(Long supplyPlanId);

    /**
     * Copia o estoque projetado restrito para o plano de trabalho do Supply Plan informado.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE InventoryPlanLinha ipl "
            + "SET ipl.quantidadeEstoqueProjetadoTrabalho = ipl.quantidadeEstoqueProjetadoRestrito "
            + "WHERE ipl.inventoryPlanLinhaCompositeKey.supplyPlan.id = :supplyPlanId")
    public void atualizaInventoryPlanTrabalhoComPlanoRestrito(Long supplyPlanId);

    /**
     * Copia o estoque projetado irrestrito para o plano de trabalho do Supply Plan informado.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE InventoryPlanLinha ipl "
            + "SET ipl.quantidadeEstoqueProjetadoTrabalho = ipl.quantidadeEstoqueProjetadoIrrestrito "
            + "WHERE ipl.inventoryPlanLinhaCompositeKey.supplyPlan.id = :supplyPlanId")
    public void atualizaInventoryPlanTrabalhoComPlanoIrrestrito(Long supplyPlanId);

    /**
     * Não funciona para planos com bucket temporal menor que diário : podemos ter diversos horários para cada data
     * @param supplyPlan
     * @param dataInicio
     * @param dataFim
     * @return
     */
    @Query("SELECT ivpl.inventoryPlanLinhaCompositeKey.produto AS material, "
            + "ivpl.inventoryPlanLinhaCompositeKey.location AS location, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(ivpl.inventoryPlanLinhaCompositeKey.dataReferencia)  AS referenceDate, "
            + "COALESCE(ivpl.quantidadeEstoqueProjetadoIrrestrito, 0) AS totalQuantityUnconstrained, "
            + "COALESCE(ivpl.quantidadeEstoqueProjetadoRestrito, 0) AS totalQuantityConstrained, "
            + "COALESCE(ivpl.quantidadeEstoqueProjetadoTrabalho, 0) AS totalQuantityWorking "
            + "FROM InventoryPlanLinha ivpl "
            + "LEFT JOIN ivpl.unidadeMedida um "
            + "WHERE ivpl.inventoryPlanLinhaCompositeKey.dataReferencia BETWEEN :dataInicio AND :dataFim "
            + "AND DATA_SEM_HORARIO(ivpl.inventoryPlanLinhaCompositeKey.dataReferencia) = ULTIMO_DIA_MES_SEM_HORARIO(ivpl.inventoryPlanLinhaCompositeKey.dataReferencia) "
            + "AND ivpl.inventoryPlanLinhaCompositeKey.supplyPlan = :supplyPlan ")
    List<AggregatedByLocationMaterialUOMDatePlanType> consolidatedInventoryPlanLinhaByLocationMaterialUOMMonth(
            @Param("supplyPlan") SupplyPlan supplyPlan,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    /**
     * Não funciona para planos com bucket temporal menor que diário : podemos ter diversos horários para cada data
     * @param supplyPlan
     * @param dataInicio
     * @param dataFim
     * @return
     */
    @Query("SELECT ivpl.inventoryPlanLinhaCompositeKey.produto AS material, "
            + "ivpl.inventoryPlanLinhaCompositeKey.location AS location, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(ivpl.inventoryPlanLinhaCompositeKey.dataReferencia)  AS referenceDate, "
            + "COALESCE(ivpl.quantidadeEstoqueProjetadoIrrestrito, 0) AS totalQuantityUnconstrained, "
            + "COALESCE(ivpl.quantidadeEstoqueProjetadoRestrito, 0) AS totalQuantityConstrained, "
            + "COALESCE(ivpl.quantidadeEstoqueProjetadoTrabalho, 0) AS totalQuantityWorking "
            + "FROM InventoryPlanLinha ivpl "
            + "LEFT JOIN ivpl.unidadeMedida um "
            + "WHERE ivpl.inventoryPlanLinhaCompositeKey.dataReferencia BETWEEN :dataInicio AND :dataFim "
            + "AND DATA_SEM_HORARIO(ivpl.inventoryPlanLinhaCompositeKey.dataReferencia) = DOMINGO_DA_SEMANA_SEM_HORARIO(ivpl.inventoryPlanLinhaCompositeKey.dataReferencia) "
            + "AND ivpl.inventoryPlanLinhaCompositeKey.supplyPlan = :supplyPlan ")
    List<AggregatedByLocationMaterialUOMDatePlanType> consolidatedInventoryPlanLinhaByLocationMaterialUOMWeek(
            @Param("supplyPlan") SupplyPlan supplyPlan,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    /**
     * Não funciona para planos com bucket temporal menor que diário : podemos ter diversos horários para cada data
     * @param supplyPlan
     * @param dataInicio
     * @param dataFim
     * @return
     */
    @Query("SELECT ivpl.inventoryPlanLinhaCompositeKey.produto AS material, "
            + "ivpl.inventoryPlanLinhaCompositeKey.location AS location, "
            + "um AS uom, "
            + "DATA_SEM_HORARIO(ivpl.inventoryPlanLinhaCompositeKey.dataReferencia)  AS referenceDate, "
            + "COALESCE(ivpl.quantidadeEstoqueProjetadoIrrestrito, 0) AS totalQuantityUnconstrained, "
            + "COALESCE(ivpl.quantidadeEstoqueProjetadoRestrito, 0) AS totalQuantityConstrained, "
            + "COALESCE(ivpl.quantidadeEstoqueProjetadoTrabalho, 0) AS totalQuantityWorking "
            + "FROM InventoryPlanLinha ivpl "
            + "LEFT JOIN ivpl.unidadeMedida um "
            + "WHERE ivpl.inventoryPlanLinhaCompositeKey.dataReferencia BETWEEN :dataInicio AND :dataFim "
            + "AND ivpl.inventoryPlanLinhaCompositeKey.supplyPlan = :supplyPlan ")
    List<AggregatedByLocationMaterialUOMDatePlanType> consolidatedInventoryPlanLinhaByLocationMaterialUOMDay(
            @Param("supplyPlan") SupplyPlan supplyPlan,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

}
