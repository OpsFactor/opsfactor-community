package com.opsfactor.community.capability.supplyplanning.productionplan.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.SetupPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.SetupPlanLinha.SetupPlanLinhaCompositeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Repository Enterprise dos outputs de setup do Production Plan.
 */
@Repository
public interface SetupPlanLinhaRepository extends JpaRepository<SetupPlanLinha, SetupPlanLinhaCompositeKey> {

    /**
     * Remove em lote os setups pertencentes ao Supply Plan antes da exclusao
     * do cabecalho Community.
     *
     * <p>A relacao inversa foi removida do aggregate Community. O cleanup
     * Enterprise precisa, portanto, apagar os filhos explicitamente sem
     * materializar chaves compostas ou executar delete entidade a entidade.</p>
     *
     * @param supplyPlanId identificador do Supply Plan removido.
     * @return quantidade de linhas de setup excluidas.
     */
    @Transactional
    @Modifying
    @Query("""
            DELETE FROM SetupPlanLinha setupPlanLinha
            WHERE setupPlanLinha.setupPlanLinhaCompositeKey.supplyPlan.id = :supplyPlanId
            """)
    int deleteBySupplyPlanId(@Param("supplyPlanId") Long supplyPlanId);

    /**
     * Carrega setups de um supply plan com as dimensoes necessarias para
     * exportacao de ocupacao produtiva.
     */
    @Query("SELECT DISTINCT spl FROM SetupPlanLinha spl "
            + "LEFT JOIN FETCH spl.setupPlanLinhaCompositeKey.supplyPlan sp "
            + "LEFT JOIN FETCH spl.setupPlanLinhaCompositeKey.recursoProdutivo rp "
            + "LEFT JOIN FETCH rp.location "
            + "LEFT JOIN FETCH rp.unidadeMedidaCapacidadeEmUom "
            + "LEFT JOIN FETCH spl.setupPlanLinhaCompositeKey.versaoProducao vp "
            + "LEFT JOIN FETCH spl.setupPlanLinhaCompositeKey.roteiro rot "
            + "LEFT JOIN FETCH rot.location "
            + "LEFT JOIN FETCH rot.materialOutput "
            + "LEFT JOIN FETCH spl.setupPlanLinhaCompositeKey.listaTecnica lt "
            + "LEFT JOIN FETCH lt.location "
            + "LEFT JOIN FETCH lt.materialOutput "
            + "WHERE sp.id = :supplyPlanId")
    List<SetupPlanLinha> customFindBySupplyPlanIdForProductionPlanOccupationExport(
            @Param("supplyPlanId") Long supplyPlanId);

    /**
     * Carrega o envelope de supply plans recebido por um batch de chaves.
     */
    @Query("SELECT DISTINCT spl FROM SetupPlanLinha spl "
            + "LEFT JOIN FETCH spl.setupPlanLinhaCompositeKey.supplyPlan sp "
            + "LEFT JOIN FETCH spl.setupPlanLinhaCompositeKey.recursoProdutivo rp "
            + "LEFT JOIN FETCH rp.location "
            + "LEFT JOIN FETCH rp.unidadeMedidaCapacidadeEmUom "
            + "LEFT JOIN FETCH spl.setupPlanLinhaCompositeKey.versaoProducao vp "
            + "LEFT JOIN FETCH spl.setupPlanLinhaCompositeKey.roteiro rot "
            + "LEFT JOIN FETCH rot.location "
            + "LEFT JOIN FETCH rot.materialOutput "
            + "LEFT JOIN FETCH spl.setupPlanLinhaCompositeKey.listaTecnica lt "
            + "LEFT JOIN FETCH lt.location "
            + "LEFT JOIN FETCH lt.materialOutput "
            + "WHERE sp.id IN :supplyPlanIds")
    List<SetupPlanLinha> customFindBySupplyPlanIdInForProductionPlanOccupationExport(
            @Param("supplyPlanIds") Collection<Long> supplyPlanIds);

    /**
     * Carrega em uma unica consulta os setups de um Supply Plan e location
     * para materializar a fotografia read-only de Line Scheduling.
     *
     * <p>O fetch explicito das dimensoes da chave composta impede que a futura
     * montagem do Gantt navegue linha a linha por recurso, versao, roteiro ou
     * lista tecnica. A consulta nao altera o output nem recompõe setups.</p>
     *
     * @param supplyPlan plano cuja fotografia de setup sera lida.
     * @param location location produtiva exibida pela superficie privada.
     * @return linhas de setup do plano filtradas pela location do recurso.
     */
    @Query("SELECT DISTINCT setupPlanLine FROM SetupPlanLinha setupPlanLine "
            + "JOIN FETCH setupPlanLine.setupPlanLinhaCompositeKey.supplyPlan supplyPlan "
            + "JOIN FETCH setupPlanLine.setupPlanLinhaCompositeKey.recursoProdutivo productionResource "
            + "JOIN FETCH productionResource.location location "
            + "JOIN FETCH setupPlanLine.setupPlanLinhaCompositeKey.versaoProducao productionVersion "
            + "JOIN FETCH setupPlanLine.setupPlanLinhaCompositeKey.roteiro routing "
            + "JOIN FETCH setupPlanLine.setupPlanLinhaCompositeKey.listaTecnica billOfMaterials "
            + "WHERE supplyPlan = :supplyPlan "
            + "AND location = :location")
    List<SetupPlanLinha> findBySupplyPlanAndLocationForLineScheduling(
            @Param("supplyPlan") SupplyPlan supplyPlan,
            @Param("location") Location location);

}
