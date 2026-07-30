package com.opsfactor.community.capability.supplyplanning.supplyplan.repository;

import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA de SupplyPlanRepository.
 */
@Repository
public interface SupplyPlanRepository extends JpaRepository<SupplyPlan,Long> {
    
    public List<SupplyPlan> findAll();
        
    public boolean existsById(Long supplyPlanId);
    
    /**
     * Remove o Supply Plan pelo id informado.
     */
    @Transactional
    public void deleteById(Long supplyPlanId);

    /**
     * Desassocia em lote todos os planos que apontam para o plano removido.
     *
     * <p>Os planos referenciadores continuam existindo; somente a referência
     * opcional é anulada antes que a FK autorreferente bloqueie o header.</p>
     *
     * @param referencedSupplyPlanId identificador do plano referenciado.
     * @return quantidade de planos que tiveram a referência anulada.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("UPDATE SupplyPlan referencingSupplyPlan "
            + "SET referencingSupplyPlan.supplyPlanReferencia = null "
            + "WHERE referencingSupplyPlan.supplyPlanReferencia.id = :referencedSupplyPlanId")
    int clearSupplyPlanReferenceByReferencedSupplyPlanId(
            @Param("referencedSupplyPlanId") Long referencedSupplyPlanId);
    
    @Query("SELECT DISTINCT sp FROM SupplyPlan sp " + // distinct pois há um join com 2 coleções
            "LEFT JOIN FETCH sp.perfilExecucaoSupplyPlan pesp " +
            "WHERE sp.id = :supplyPlanId")
    Optional<SupplyPlan> customFindById(
            Long supplyPlanId);

    /**
     * Carrega os metadados exibidos no seletor de Supply Planning em uma unica
     * consulta, evitando navegar por relacoes lazy para cada plano listado.
     */
    @Query("SELECT sp FROM SupplyPlan sp "
            + "LEFT JOIN FETCH sp.demandPlan demandPlan "
            + "LEFT JOIN FETCH sp.versaoMalha versaoMalha "
            + "LEFT JOIN FETCH sp.perfilExecucaoSupplyPlan perfilExecucaoSupplyPlan")
    List<SupplyPlan> customFindAllForSelector();

    /**
     * Atualiza somente os metadados (horario geracao e usuario gerador) de geração do Supply Plan existente.
     *
     * <p>Esse caminho evita chamar {@code save/merge} sobre um {@link SupplyPlan}
     * que ja foi carregado com referencias grandes de perfil/malha. O merge pode
     * atravessar colecoes lazy da malha e materializar volumes massivos de
     * {@code LinhaTransporteProduto}, antes mesmo da etapa de otimizacao.</p>
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("UPDATE SupplyPlan sp " +
            "SET sp.horarioGeracao = :horarioGeracao, " +
            "sp.usuarioGeradorPlano = :usuarioGeradorPlano " +
            "WHERE sp.id = :supplyPlanId")
    public void atualizaMetadadosGeracao(
            Long supplyPlanId,
            LocalDateTime horarioGeracao,
            String usuarioGeradorPlano);

    /**
     * Localiza, sem materializar os planos, os identificadores dos Supply Plans
     * que ainda apontam para algum grupo de preset constraints informado.
     *
     * <p>O resultado serve como guarda de integridade antes da exclusão de
     * grupos: a relação permanece no aggregate Community, enquanto as regras
     * de preset constraints e a decisão de exclusão pertencem ao Enterprise.
     * A consulta projeta somente o id e resolve todo o conjunto em uma única
     * operação, evitando carregamento lazy ou uma consulta por grupo.</p>
     *
     * @param presetConstraintGroupIdCollection identificadores dos grupos que
     *                                          se pretende remover.
     * @return identificadores dos planos que ainda referenciam esses grupos.
     */
    @Query("SELECT supplyPlan.id FROM SupplyPlan supplyPlan "
            + "WHERE supplyPlan.presetConstraintGroup.id IN :presetConstraintGroupIdCollection")
    List<Long> customFindIdsByPresetConstraintGroupIdIn(
            @Param("presetConstraintGroupIdCollection")
            Collection<String> presetConstraintGroupIdCollection);

    List<SupplyPlan> findByDemandPlanId(Long demandPlanId);

}
