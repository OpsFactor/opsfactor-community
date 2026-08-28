package com.opsfactor.community.capability.supplyplanning.productionplan.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha.ProductionPlanLinhaCompositeKey;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Repository JPA de ProductionPlanLinhaRepository.
 */
@Repository
public interface ProductionPlanLinhaRepository extends JpaRepository<ProductionPlanLinha,ProductionPlanLinhaCompositeKey> {

    /**
     * Extrai todos os production plan linhas associados à location
     * @param supplyPlanId
     * @param location
     * @return
     */
    List<ProductionPlanLinha> findByProductionPlanLinhaCompositeKeySupplyPlanIdAndProductionPlanLinhaCompositeKeyLocation(
            Long supplyPlanId, Location location);

    /**
     * Recorte paginado de chaves de producao que ainda possuem valores
     * baseline efetivos. Nao materializa entidade, BOM ou roteiro no preflight.
     */
    @Query("SELECT ppl.productionPlanLinhaCompositeKey.supplyPlan.id AS supplyPlanId, "
            + "ppl.productionPlanLinhaCompositeKey.location.id AS locationId, "
            + "ppl.materialOutput.id AS outputMaterialId, "
            + "ppl.productionPlanLinhaCompositeKey.versaoProducao.id AS productionVersionId, "
            + "ppl.productionPlanLinhaCompositeKey.roteiro.id AS routingId, "
            + "ppl.productionPlanLinhaCompositeKey.listaTecnica.id AS billOfMaterialsId, "
            + "ppl.productionPlanLinhaCompositeKey.dataReferencia AS referenceDate, "
            + "ppl.quantidadeSugestaoProducaoBaseline AS plannedProductionBaselineUnconstrained, "
            + "ppl.quantidadeSugestaoProducaoBaselineAtendida AS plannedProductionBaselineConstrained, "
            + "ppl.quantidadeOrdemProducaoBaseline AS firmProductionBaselineUnconstrained, "
            + "ppl.quantidadeOrdemProducaoBaselineAtendida AS firmProductionBaselineConstrained "
            + "FROM ProductionPlanLinha ppl "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan.id IN :supplyPlanIds "
            + "AND (COALESCE(ppl.quantidadeSugestaoProducaoBaseline, 0) <> 0 "
            + "OR COALESCE(ppl.quantidadeSugestaoProducaoBaselineAtendida, 0) <> 0 "
            + "OR COALESCE(ppl.quantidadeOrdemProducaoBaseline, 0) <> 0 "
            + "OR COALESCE(ppl.quantidadeOrdemProducaoBaselineAtendida, 0) <> 0)")
    List<ProductionPlanLegacyBaselineRequirement> findLegacyBaselineRequirementsBySupplyPlanIdIn(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds,
            Pageable pageable);

    /**
     * Conta todas as pendencias baseline efetivas do envelope sem carregar
     * entidades de producao para a fotografia administrativa.
     */
    @Query("SELECT COUNT(ppl) FROM ProductionPlanLinha ppl "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan.id IN :supplyPlanIds "
            + "AND (COALESCE(ppl.quantidadeSugestaoProducaoBaseline, 0) <> 0 "
            + "OR COALESCE(ppl.quantidadeSugestaoProducaoBaselineAtendida, 0) <> 0 "
            + "OR COALESCE(ppl.quantidadeOrdemProducaoBaseline, 0) <> 0 "
            + "OR COALESCE(ppl.quantidadeOrdemProducaoBaselineAtendida, 0) <> 0)")
    long countLegacyBaselineRequirementsBySupplyPlanIdIn(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds);

    /**
     * Carrega o envelope de producao sob lock pessimista exclusivamente para
     * o executor offline de regularizacao de baseline persistido.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan.id IN :supplyPlanIds")
    List<ProductionPlanLinha> findBySupplyPlanIdInForPersistedBaselineCutover(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds);

    /**
     * Loads the saved material envelope for one Production Planning Book in a
     * single query. The book needs only the material identity at this point;
     * routings, BOMs and production values are loaded by the established
     * SupplyPlanningProjection factory afterwards.
     */
    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl "
            + "JOIN FETCH ppl.materialOutput materialOutput "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan = :supplyPlan "
            + "AND ppl.productionPlanLinhaCompositeKey.location = :location")
    List<ProductionPlanLinha> findProductionPlanMaterialsForPlanningBook(
            SupplyPlan supplyPlan,
            Location location);

    /**
     * Extrai todos os production plan linhas associados à location, incluindo listas técnicas e respectivos materiais input
     * Usado no planning projection factory, para evitar N+1 com materiais input das LTs
     * @param supplyPlanId
     * @param location
     * @return
     */
    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location location "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao versao "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro roteiro "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica listaTecnica "
            + "LEFT JOIN FETCH ppl.materialOutput materialOutput "
            + "LEFT JOIN FETCH ppl.unidadeMedida unidadeMedida "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan = :supplyPlan "
            + "AND ppl.productionPlanLinhaCompositeKey.location = :location")
    List<ProductionPlanLinha> customFindByProductionPlanLinhaCompositeKeySupplyPlanAndProductionPlanLinhaCompositeKeyLocationIncluindoListaTecnicaEMateriaisInput(
            SupplyPlan supplyPlan, Location location);

    /**
     * Carrega em lote as linhas de produção de todas as locations do snapshot,
     * incluindo BOM e componentes usados pelo índice de inputs do heurístico.
     */
    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location location "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao versao "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro roteiro "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica listaTecnica "
            + "LEFT JOIN FETCH ppl.materialOutput materialOutput "
            + "LEFT JOIN FETCH ppl.unidadeMedida unidadeMedida "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan = :supplyPlan "
            + "AND ppl.productionPlanLinhaCompositeKey.location IN :locations")
    List<ProductionPlanLinha> customFindByProductionPlanLinhaCompositeKeySupplyPlanAndProductionPlanLinhaCompositeKeyLocationInIncluindoListaTecnicaEMateriaisInput(
            SupplyPlan supplyPlan,
            Collection<Location> locations);

    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location location "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao versao "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro roteiro "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica listaTecnica "
            + "LEFT JOIN FETCH ppl.materialOutput materialOutput "
            + "LEFT JOIN FETCH ppl.unidadeMedida unidadeMedida "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan = :supplyPlan")
    List<ProductionPlanLinha> customFindByProductionPlanLinhaCompositeKeySupplyPlan(
            SupplyPlan supplyPlan);

    /**
     * Extrai as linhas de Production Plan para exportacao de volume, trazendo
     * as dimensoes de cabecalho usadas no Data Upload sem disparar lazy load
     * linha a linha.
     */
    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.supplyPlan sp " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location loc " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao vp " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro rot " +
            "LEFT JOIN FETCH rot.location " +
            "LEFT JOIN FETCH rot.materialOutput " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica lt " +
            "LEFT JOIN FETCH lt.location " +
            "LEFT JOIN FETCH lt.materialOutput " +
            "LEFT JOIN FETCH ppl.materialOutput mat " +
            "LEFT JOIN FETCH ppl.unidadeMedida uom " +
            "WHERE sp.id = :supplyPlanId")
    List<ProductionPlanLinha> customFindBySupplyPlanIdForProductionPlanVolumeExport(
            Long supplyPlanId);

    /**
     * Variante por envelope de supply plans usada pela infraestrutura generica
     * de reconciliacao em batch, mantendo o mesmo conjunto de fetch joins da
     * exportacao filtrada.
     */
    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.supplyPlan sp " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location loc " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao vp " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro rot " +
            "LEFT JOIN FETCH rot.location " +
            "LEFT JOIN FETCH rot.materialOutput " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica lt " +
            "LEFT JOIN FETCH lt.location " +
            "LEFT JOIN FETCH lt.materialOutput " +
            "LEFT JOIN FETCH ppl.materialOutput mat " +
            "LEFT JOIN FETCH ppl.unidadeMedida uom " +
            "WHERE sp.id IN :supplyPlanIds")
    List<ProductionPlanLinha> customFindBySupplyPlanIdInForProductionPlanVolumeExport(
            Collection<Long> supplyPlanIds);

    /**
     * Carrega a fotografia de producao materializada de um plano juntamente
     * com o cabecalho, a BOM e os componentes que explicam o consumo.
     *
     * <p>A consulta e usada por leitura Enterprise do snapshot ja persistido.
     * Ela nao restringe materiais pela configuracao atual do perfil, pois uma
     * resposta de observabilidade deve preservar o recorte efetivamente salvo
     * na rodada.</p>
     */
    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.supplyPlan sp "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location loc "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao vp "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro rot "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica lt "
            + "LEFT JOIN FETCH lt.listaTecnicaComponenteSet ltc "
            + "LEFT JOIN FETCH ltc.listaTecnicaComponenteCompositeKey.materialComponente inputMat "
            + "LEFT JOIN FETCH ltc.unidadeMedidaMaterialComponente inputUom "
            + "LEFT JOIN FETCH ppl.materialOutput outputMat "
            + "LEFT JOIN FETCH ppl.unidadeMedida outputUom "
            + "WHERE sp.id = :supplyPlanId")
    List<ProductionPlanLinha> customFindBySupplyPlanIdForFirmProductionOrdersEffectiveRead(
            Long supplyPlanId);

    /**
     * Extrai as linhas de Production Plan para exportacao de ocupacao,
     * carregando o mesmo cabecalho do volume e as operacoes do roteiro usadas
     * para derivar consumo por recurso produtivo.
     */
    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.supplyPlan sp " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location loc " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao vp " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro rot " +
            "LEFT JOIN FETCH rot.location " +
            "LEFT JOIN FETCH rot.materialOutput " +
            "LEFT JOIN FETCH rot.operacaoRoteiroSet opr " +
            "LEFT JOIN FETCH opr.recursoProdutivo rp " +
            "LEFT JOIN FETCH rp.location " +
            "LEFT JOIN FETCH rp.unidadeMedidaCapacidadeEmUom " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica lt " +
            "LEFT JOIN FETCH lt.location " +
            "LEFT JOIN FETCH lt.materialOutput " +
            "LEFT JOIN FETCH ppl.materialOutput mat " +
            "LEFT JOIN FETCH ppl.unidadeMedida uom " +
            "WHERE sp.id = :supplyPlanId")
    List<ProductionPlanLinha> customFindBySupplyPlanIdForProductionPlanOccupationExport(
            Long supplyPlanId);

    /**
     * Variante por envelope de supply plans usada pela infraestrutura generica
     * de reconciliacao em batch do recorte read-only de ocupacao.
     */
    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.supplyPlan sp " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location loc " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao vp " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro rot " +
            "LEFT JOIN FETCH rot.location " +
            "LEFT JOIN FETCH rot.materialOutput " +
            "LEFT JOIN FETCH rot.operacaoRoteiroSet opr " +
            "LEFT JOIN FETCH opr.recursoProdutivo rp " +
            "LEFT JOIN FETCH rp.location " +
            "LEFT JOIN FETCH rp.unidadeMedidaCapacidadeEmUom " +
            "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica lt " +
            "LEFT JOIN FETCH lt.location " +
            "LEFT JOIN FETCH lt.materialOutput " +
            "LEFT JOIN FETCH ppl.materialOutput mat " +
            "LEFT JOIN FETCH ppl.unidadeMedida uom " +
            "WHERE sp.id IN :supplyPlanIds")
    List<ProductionPlanLinha> customFindBySupplyPlanIdInForProductionPlanOccupationExport(
            Collection<Long> supplyPlanIds);

    /**
     * Extrai todos os production plan linhas associados à location onde o material output esteja na lista
     * @param supplyPlanId
     * @param location
     * @param produtos
     * @return
     */
    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location location "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao versao "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro roteiro "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica listaTecnica "
            + "LEFT JOIN FETCH ppl.materialOutput materialOutput "
            + "LEFT JOIN FETCH ppl.unidadeMedida unidadeMedida "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan.id = :supplyPlanId "
            + "AND ppl.productionPlanLinhaCompositeKey.location = :location "
            + "AND ppl.materialOutput IN :produtos")
    List<ProductionPlanLinha> findByProductionPlanLinhaCompositeKeySupplyPlanIdAndProductionPlanLinhaCompositeKeyLocationAndMaterialOutputIn(
            Long supplyPlanId , Location location, Collection<Produto> produtos);

    @Query("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location location "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao versao "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro roteiro "
            + "LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica listaTecnica "
            + "LEFT JOIN FETCH ppl.materialOutput materialOutput "
            + "LEFT JOIN FETCH ppl.unidadeMedida unidadeMedida "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan = :supplyPlan "
            + "AND ppl.productionPlanLinhaCompositeKey.location IN :locations "
            + "AND ppl.materialOutput IN :produtos")
    List<ProductionPlanLinha> findByProductionPlanLinhaCompositeKeySupplyPlanAndProductionPlanLinhaCompositeKeyLocationInAndMaterialOutputIn(
            SupplyPlan supplyPlan, Collection<Location> locations, Collection<Produto> produtos);


    boolean existsByProductionPlanLinhaCompositeKeySupplyPlanIdAndProductionPlanLinhaCompositeKeyLocation(
            Long supplyPlanId, Location location);
    boolean existsByProductionPlanLinhaCompositeKeySupplyPlanAndProductionPlanLinhaCompositeKeyLocation(
            SupplyPlan supplyPlan, Location location);

    /**
     * Remove linhas de producao do Supply Plan associadas as locations informadas.
     */
    @Transactional
    void removeByProductionPlanLinhaCompositeKeyLocationIn(Collection<Location> locations);

    /**
     * Remove todas as linhas de producao do Supply Plan informado.
     */
    @Transactional
    void removeByProductionPlanLinhaCompositeKeySupplyPlanId(Long supplyPlanId);

    /**
     * Atualiza o plano restrito de producao a partir do plano irrestrito.
     *
     * <p>No Community puro as ordens firmes já chegam neutralizadas pelo SPI
     * ausente. Copiar o valor permite que o runtime Enterprise preserve suas
     * ordens firmes ao reutilizar este repository compartilhado.</p>
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE ProductionPlanLinha ppl "
            + "SET ppl.quantidadeOrdemPlanejadaProducaoRestrita = ppl.quantidadeOrdemPlanejadaProducaoIrrestrita, "
            + "ppl.quantidadeOrdemFirmeProducaoRestrita = ppl.quantidadeOrdemFirmeProducaoIrrestrita "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan.id = :supplyPlanId")
    public void atualizaPlanoRestritoComPlanoIrrestrito(Long supplyPlanId);

    /**
     * Copia ordens planejadas restritas de producao para o plano de trabalho.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE ProductionPlanLinha ppl "
            + "SET ppl.quantidadeOrdemPlanejadaProducaoTrabalho = COALESCE(ppl.quantidadeOrdemPlanejadaProducaoRestrita, 0) "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan.id = :supplyPlanId")
    public void atualizaOrdensPlanejadasPlanoTrabalhoComPlanoRestrito(Long supplyPlanId);

    /**
     * Neutraliza ordens firmes de producao no plano de trabalho ao sincronizar com o plano restrito Community.
     *
     * <p>Community nao carrega ordens firmes de producao transacionais. O metodo conserva o nome legado porque ainda
     * e chamado por fluxos comuns de atualizacao do plano de trabalho, mas neutraliza a coluna em vez de copiar
     * valores possivelmente vindos de base Enterprise/transicional.</p>
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE ProductionPlanLinha ppl "
            + "SET ppl.quantidadeOrdemFirmeProducaoTrabalho = 0 "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan.id = :supplyPlanId")
    public void atualizaOrdensFirmesPlanoTrabalhoComPlanoRestrito(Long supplyPlanId);

    /**
     * Copia ordens planejadas irrestritas de producao para o plano de trabalho.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE ProductionPlanLinha ppl "
            + "SET ppl.quantidadeOrdemPlanejadaProducaoTrabalho = COALESCE(ppl.quantidadeOrdemPlanejadaProducaoIrrestrita, 0) "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan.id = :supplyPlanId")
    public void atualizaOrdensPlanejadasPlanoTrabalhoComPlanoIrrestrito(Long supplyPlanId);

    /**
     * Neutraliza ordens firmes de producao no plano de trabalho ao sincronizar com o plano irrestrito Community.
     *
     * <p>Community nao carrega ordens firmes de producao transacionais. O metodo conserva o nome legado porque ainda
     * e chamado por fluxos comuns de atualizacao do plano de trabalho, mas neutraliza a coluna em vez de copiar
     * valores possivelmente vindos de base Enterprise/transicional.</p>
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true) // https://www.baeldung.com/spring-data-jpa-modifying-annotation
    @Query("UPDATE ProductionPlanLinha ppl "
            + "SET ppl.quantidadeOrdemFirmeProducaoTrabalho = 0 "
            + "WHERE ppl.productionPlanLinhaCompositeKey.supplyPlan.id = :supplyPlanId")
    public void atualizaOrdensFirmesPlanoTrabalhoComPlanoIrrestrito(Long supplyPlanId);

}
