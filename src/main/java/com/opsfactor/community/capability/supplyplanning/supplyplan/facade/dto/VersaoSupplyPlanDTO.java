package com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Versao de Supply Plan usada por seletores e pelo disparo de nova rodada.
 *
 * <p>No Community a execucao e sempre heuristica. Este DTO referencia Demand
 * Plan, malha, perfil e horizonte, mas nao transporta parametros de otimizador,
 * process chain, P&L, custos ou line scheduling.</p>
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersaoSupplyPlanDTO implements Serializable {

    /** Identificador da versao de Supply Plan quando ja existe plano salvo. */
    Long supplyPlanId;

    /** Plano usado como ponto de partida para projecao de estoque inicial. */
    @Nullable Long supplyPlanIdForStartingStockProjection;

    /** Perfil heuristico de execucao. */
    String executionProfileId;

    /** Demand Plan usado como demanda futura. */
    Long demandPlanId;

    /** Versao de malha Supply Network usada na rodada. */
    String supplyNetworkVersionId;

    /**
     * Grupo Enterprise de preset constraints selecionado para um plano novo.
     *
     * <p>O campo permanece nulo no Community. Ele é mantido neste DTO comum
     * para que uma reexecução preserve o vínculo já salvo sem criar outro
     * payload ou uma tabela auxiliar.</p>
     */
    String presetConstraintGroupId;

    /** Descricao do Supply Plan. */
    String descricaoSupplyPlan;

    /** Descricao do Demand Plan associado. */
    String descricaoDemandPlan;

    /** Bucket temporal do plano. */
    Constantes.TamanhoBucket tamanhoBucket;

    /** Horario de geracao do plano. */
    LocalDateTime horarioGeracao;

    /** Periodo de referencia derivado do Demand Plan. */
    String periodoReferencia;

    /**
     * Monta o DTO a partir da entidade persistida.
     *
     * <p>A leitura navega por Demand Plan, perfil e malha porque estas
     * dimensoes sao exibidas no seletor. Consumers devem garantir que essas
     * associacoes estejam carregadas em lote para evitar N+1.</p>
     */
    public VersaoSupplyPlanDTO(SupplyPlan supplyPlan) {

        supplyPlanId = supplyPlan.getId();
        supplyPlanIdForStartingStockProjection = supplyPlan.getSupplyPlanIdParaProjecaoEstoqueInicial();
        executionProfileId = supplyPlan.getPerfilExecucaoSupplyPlan().getId();
        demandPlanId = supplyPlan.getDemandPlan().getId();
        supplyNetworkVersionId = supplyPlan.getVersaoMalha().getId();
        presetConstraintGroupId = supplyPlan.getPresetConstraintGroup() == null
                ? null
                : supplyPlan.getPresetConstraintGroup().getId();
        descricaoSupplyPlan = supplyPlan.getDescricao();
        descricaoDemandPlan = supplyPlan.getDemandPlan().getDescricao();
        tamanhoBucket = supplyPlan.getTamanhoBucket();
        horarioGeracao = supplyPlan.getHorarioGeracao();
        periodoReferencia = Calendario.getDescricaoPeriodo(supplyPlan.getDemandPlan().getDataInicioPlano(), supplyPlan.getDemandPlan().getTamanhoBucket());

    }
}
