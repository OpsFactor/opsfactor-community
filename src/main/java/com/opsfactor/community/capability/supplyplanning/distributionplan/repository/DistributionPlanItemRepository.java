package com.opsfactor.community.capability.supplyplanning.distributionplan.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem.DistributionPlanItemKey;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Repository JPA de DistributionPlanItemRepository.
 */
@Repository
public interface DistributionPlanItemRepository extends JpaRepository<DistributionPlanItem,DistributionPlanItemKey> {

    Boolean existsByKeySupplyPlanIdAndKeyLocationDestino(
            Long supplyPlanId, Location locationDestino);


    /**
     * Traz todas as linhas do plano de distribuição com origem em uma location, para uma data creation_date do plano de distribuição
     */
    Collection<DistributionPlanItem> findByKeySupplyPlanIdAndKeyLocationOrigem(
            Long supplyPlanId, Location locationOrigem);

    /**
     * Traz todas as linhas do plano de distribuição com destino em uma location, para uma data creation_date do plano de distribuição
     */
    Collection<DistributionPlanItem> findByKeySupplyPlanIdAndKeyLocationDestino(
            Long supplyPlanId, Location locationDestino);

    /**
     * Retorna apenas chaves e quantidades baseline efetivas para o gate de
     * runtime Community. A projection escalar preserva diagnostico acionavel
     * sem carregar grafo de transferencias antes da exportacao/projection.
     */
    @Query("SELECT dpl.key.supplyPlan.id AS supplyPlanId, "
            + "dpl.key.locationOrigem.id AS originLocationId, "
            + "dpl.key.locationDestino.id AS destinationLocationId, "
            + "dpl.key.produto.id AS materialId, "
            + "dpl.key.dataExpedicao AS shippingDate, "
            + "dpl.key.dataRecebimento AS receivingDate, "
            + "dpl.quantidadeRequisicaoBaseline AS plannedOrderBaselineUnconstrained, "
            + "dpl.quantidadeRequisicaoBaselineAtendida AS plannedOrderBaselineConstrained, "
            + "dpl.quantidadePedidoBaseline AS firmOrderBaselineUnconstrained, "
            + "dpl.quantidadePedidoBaselineAtendido AS firmOrderBaselineConstrained "
            + "FROM DistributionPlanItem dpl "
            + "WHERE dpl.key.supplyPlan.id IN :supplyPlanIds "
            + "AND (COALESCE(dpl.quantidadeRequisicaoBaseline, 0) <> 0 "
            + "OR COALESCE(dpl.quantidadeRequisicaoBaselineAtendida, 0) <> 0 "
            + "OR COALESCE(dpl.quantidadePedidoBaseline, 0) <> 0 "
            + "OR COALESCE(dpl.quantidadePedidoBaselineAtendido, 0) <> 0)")
    List<DistributionPlanLegacyBaselineRequirement> findLegacyBaselineRequirementsBySupplyPlanIdIn(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds,
            Pageable pageable);

    /**
     * Conta todas as pendencias baseline efetivas do envelope sem carregar o
     * grafo de transferencias nem depender da pagina de evidencia.
     */
    @Query("SELECT COUNT(dpl) FROM DistributionPlanItem dpl "
            + "WHERE dpl.key.supplyPlan.id IN :supplyPlanIds "
            + "AND (COALESCE(dpl.quantidadeRequisicaoBaseline, 0) <> 0 "
            + "OR COALESCE(dpl.quantidadeRequisicaoBaselineAtendida, 0) <> 0 "
            + "OR COALESCE(dpl.quantidadePedidoBaseline, 0) <> 0 "
            + "OR COALESCE(dpl.quantidadePedidoBaselineAtendido, 0) <> 0)")
    long countLegacyBaselineRequirementsBySupplyPlanIdIn(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds);

    /**
     * Carrega o envelope de distribuicao com lock pessimista para uma janela
     * offline autorizada. Nenhum fluxo de runtime deve chamar esta consulta.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT dpl FROM DistributionPlanItem dpl "
            + "WHERE dpl.key.supplyPlan.id IN :supplyPlanIds")
    List<DistributionPlanItem> findBySupplyPlanIdInForPersistedBaselineCutover(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds);

    @Query("SELECT dpl FROM DistributionPlanItem dpl "
            + "LEFT JOIN FETCH dpl.key.supplyPlan sp "
            + "LEFT JOIN FETCH dpl.key.locationDestino ld "
            + "LEFT JOIN FETCH dpl.key.locationOrigem lo "
            + "LEFT JOIN FETCH dpl.key.produto p "
            + "LEFT JOIN FETCH dpl.unidadeMedida um "
            + "WHERE sp = :supplyPlan")
    Collection<DistributionPlanItem> customFindBySupplyPlan(
            @Param("supplyPlan") SupplyPlan supplyPlan);

    @Query("SELECT dpl FROM DistributionPlanItem dpl "
            + "LEFT JOIN FETCH dpl.key.supplyPlan sp "
            + "LEFT JOIN FETCH dpl.key.locationDestino ld "
            + "LEFT JOIN FETCH dpl.key.locationOrigem lo "
            + "LEFT JOIN FETCH dpl.key.produto p "
            + "LEFT JOIN FETCH dpl.unidadeMedida um "
            + "WHERE sp = :supplyPlan "
            + "AND p IN :materiais")
    Collection<DistributionPlanItem> customFindBySupplyPlanEMateriaisDeLista(
            @Param("supplyPlan") SupplyPlan supplyPlan,
            @Param("materiais") Collection<Produto> materiais);

    @Query("SELECT dpl FROM DistributionPlanItem dpl "
            + "LEFT JOIN FETCH dpl.key.supplyPlan sp "
            + "LEFT JOIN FETCH dpl.key.locationDestino ld "
            + "LEFT JOIN FETCH dpl.key.locationOrigem lo "
            + "LEFT JOIN FETCH dpl.key.produto p "
            + "LEFT JOIN FETCH dpl.unidadeMedida um "
            + "WHERE sp = :supplyPlan "
            + "AND lo IN :locationsOrigem "
            + "AND p IN :materiais")
    Collection<DistributionPlanItem> customFindBySupplyPlanELocationsOrigemDeListaEMateriaisDeLista(
            @Param("supplyPlan") SupplyPlan supplyPlan,
            @Param("locationsOrigem") Collection<Location> locationsOrigem,
            @Param("materiais") Collection<Produto> materiais);

    @Query("SELECT dpl FROM DistributionPlanItem dpl "
            + "LEFT JOIN FETCH dpl.key.supplyPlan sp "
            + "LEFT JOIN FETCH dpl.key.locationDestino ld "
            + "LEFT JOIN FETCH dpl.key.locationOrigem lo "
            + "LEFT JOIN FETCH dpl.key.produto p "
            + "LEFT JOIN FETCH dpl.unidadeMedida um "
            + "WHERE sp = :supplyPlan "
            + "AND (ld IN :locationsOrigemOuDestino "
            + "OR lo IN :locationsOrigemOuDestino) "
            + "AND p IN :materiais")
    Collection<DistributionPlanItem> customFindBySupplyPlanELocationsOrigemDestinoDeListaEMateriaisDeLista(
            @Param("supplyPlan") SupplyPlan supplyPlan,
            @Param("locationsOrigemOuDestino") Collection<Location> locationsOrigemOuDestino,
            @Param("materiais") Collection<Produto> materiais);

    @Query("SELECT dpl FROM DistributionPlanItem dpl "
            + "LEFT JOIN FETCH dpl.key.supplyPlan sp "
            + "LEFT JOIN FETCH dpl.key.locationDestino ld "
            + "LEFT JOIN FETCH dpl.key.locationOrigem lo "
            + "LEFT JOIN FETCH dpl.key.produto p "
            + "LEFT JOIN FETCH dpl.unidadeMedida um "
            + "WHERE sp.id = :supplyPlanId")
    Collection<DistributionPlanItem> customFindBySupplyPlanId(
            @Param("supplyPlanId") Long supplyPlanId);

    @Query("SELECT dpl FROM DistributionPlanItem dpl "
            + "LEFT JOIN FETCH dpl.key.supplyPlan sp "
            + "LEFT JOIN FETCH dpl.key.locationDestino ld "
            + "LEFT JOIN FETCH dpl.key.locationOrigem lo "
            + "LEFT JOIN FETCH dpl.key.produto p "
            + "LEFT JOIN FETCH dpl.unidadeMedida um "
            + "WHERE sp.id IN :supplyPlanIdCollection")
    Collection<DistributionPlanItem> customFindBySupplyPlanIdInForDistributionPlanExport(
            @Param("supplyPlanIdCollection") Collection<Long> supplyPlanIdCollection);

    Collection<DistributionPlanItem> findByKeySupplyPlanAndKeyLocationDestinoIn(
            SupplyPlan supplyPlan, Collection<Location> locationsDestino);


    /**
     *
     * @param supplyPlanId
     * @param locationOrigem
     * @param produtos
     * @return
     */
    List<DistributionPlanItem> findByKeySupplyPlanIdAndKeyLocationOrigemAndKeyProdutoIn(
            Long supplyPlanId , Location locationOrigem, Collection<Produto> produtos);

    List<DistributionPlanItem> findByKeySupplyPlanIdAndKeyLocationOrigemInAndKeyProdutoIn(
            Long supplyPlanId , Collection<Location> locationsOrigem, Collection<Produto> produtos);

    /**
     *
     * @param supplyPlanId
     * @param locationDestino
     * @param produtos
     * @return
     */
    List<DistributionPlanItem> findByKeySupplyPlanIdAndKeyLocationDestinoAndKeyProdutoIn(
            Long supplyPlanId , Location locationDestino, Collection<Produto> produtos);

    List<DistributionPlanItem> findByKeySupplyPlanIdAndKeyLocationDestinoInAndKeyProdutoIn(
            Long supplyPlanId , Collection<Location> locationsDestino, Collection<Produto> produtos);

    List<DistributionPlanItem> findByKeySupplyPlanIdAndKeyLocationDestinoAndKeyLocationOrigemAndKeyProdutoIn(
            Long supplyPlanId , Location locationDestino, Location locationOrigem, Collection<Produto> produtos);

    Boolean existsByKeySupplyPlanId(Long supplyPlanId);

    /**
     * Remove linhas de distribuicao do Supply Plan para as locations de origem informadas.
     */
    @Transactional
    void removeByKeySupplyPlanAndKeyLocationOrigemIn(SupplyPlan supplyPlan, Collection<Location> locations);

    /**
     * Remove linhas de distribuicao do Supply Plan para as locations de destino informadas.
     */
    @Transactional
    void removeByKeySupplyPlanAndKeyLocationDestinoIn(SupplyPlan supplyPlan, Collection<Location> locations);

    /**
     * Remove linhas de distribuicao que usam as locations informadas como origem.
     */
    @Transactional
    void removeByKeyLocationOrigemIn(Collection<Location> locations);

    /**
     * Remove linhas de distribuicao que usam as locations informadas como destino.
     */
    @Transactional
    void removeByKeyLocationDestinoIn(Collection<Location> locations);

    /**
     * Remove todas as linhas de distribuicao do Supply Plan informado.
     */
    @Transactional
    void removeByKeySupplyPlanId(Long supplyPlanId);

    boolean existsByKeySupplyPlanAndKeyLocationDestino(
            SupplyPlan supplyPlan, Location locationDestino);
    boolean existsByKeySupplyPlanAndKeyLocationOrigem(
            SupplyPlan supplyPlan, Location locationOrigem);

    /** Atualiza o plano restrito de distribuição a partir do plano irrestrito. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE DistributionPlanItem dpl "
            + "SET dpl.quantidadeOrdemPlanejadaRestrita = dpl.quantidadeOrdemPlanejadaIrrestrita, "
            + "dpl.quantidadeOrdemFirmeRestrita = dpl.quantidadeOrdemFirmeIrrestrita, "
            + "dpl.parcelaOrdemPlanejadaRestritaAtendimentoDemandaDireta = dpl.parcelaOrdemPlanejadaIrrestritaAtendimentoDemandaDireta, "
            + "dpl.parcelaOrdemFirmeRestritaAtendimentoDemandaDireta = dpl.parcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta "
            + "WHERE dpl.key.supplyPlan.id = :supplyPlanId")
    public void atualizaPlanoRestritoComPlanoIrrestrito(Long supplyPlanId);

    @Query("SELECT dpl FROM DistributionPlanItem dpl "
            + "LEFT JOIN FETCH dpl.key.supplyPlan sp "
            + "LEFT JOIN FETCH dpl.key.locationDestino ld "
            + "LEFT JOIN FETCH dpl.key.locationOrigem lo "
            + "LEFT JOIN FETCH dpl.key.produto p "
            + "LEFT JOIN FETCH dpl.unidadeMedida um "
            + "WHERE dpl.key.supplyPlan = :supplyPlan "
            + "AND (dpl.quantidadeOrdemPlanejadaRestrita < dpl.quantidadeOrdemPlanejadaIrrestrita "
            + "OR dpl.quantidadeOrdemFirmeRestrita < dpl.quantidadeOrdemFirmeIrrestrita)")
    public List<DistributionPlanItem> customFindBySupplyPlanComRestricoesAplicadas(SupplyPlan supplyPlan);

    /**
     * Copia ordens planejadas restritas para o plano de trabalho de distribuicao.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE DistributionPlanItem dpl "
            + "SET dpl.quantidadeOrdemPlanejadaTrabalho = dpl.quantidadeOrdemPlanejadaRestrita "
            + "WHERE dpl.key.supplyPlan.id = :supplyPlanId")
    public void atualizaOrdensPlanejadasPlanoTrabalhoComPlanoRestrito(Long supplyPlanId);

    /**
     * Neutraliza ordens firmes no plano de trabalho de distribuicao ao sincronizar com o plano restrito Community.
     *
     * <p>Community nao possui transferencias firmes transacionais. O metodo conserva o nome legado porque ainda e
     * chamado por fluxos comuns de atualizacao do plano de trabalho, mas o update zera a coluna para impedir que
     * residuos Enterprise voltem a aparecer no Planning Book/calculo.</p>
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE DistributionPlanItem dpl "
            + "SET dpl.quantidadeOrdemFirmeTrabalho = 0 "
            + "WHERE dpl.key.supplyPlan.id = :supplyPlanId")
    public void atualizaOrdensFirmesPlanoTrabalhoComPlanoRestrito(Long supplyPlanId);

    /**
     * Copia ordens planejadas irrestritas para o plano de trabalho de distribuicao.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE DistributionPlanItem dpl "
            + "SET dpl.quantidadeOrdemPlanejadaTrabalho = dpl.quantidadeOrdemPlanejadaIrrestrita "
            + "WHERE dpl.key.supplyPlan.id = :supplyPlanId")
    public void atualizaOrdensPlanejadasPlanoTrabalhoComPlanoIrrestrito(Long supplyPlanId);

    /**
     * Neutraliza ordens firmes no plano de trabalho de distribuicao ao sincronizar com o plano irrestrito Community.
     *
     * <p>Community nao possui transferencias firmes transacionais. O metodo conserva o nome legado porque ainda e
     * chamado por fluxos comuns de atualizacao do plano de trabalho, mas o update zera a coluna para impedir que
     * residuos Enterprise voltem a aparecer no Planning Book/calculo.</p>
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE DistributionPlanItem dpl "
            + "SET dpl.quantidadeOrdemFirmeTrabalho = 0 "
            + "WHERE dpl.key.supplyPlan.id = :supplyPlanId")
    public void atualizaOrdensFirmesPlanoTrabalhoComPlanoIrrestrito(Long supplyPlanId);

}
