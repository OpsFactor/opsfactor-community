package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.optimizer.presetconstraint.RestricaoPredefinidaGrupo;

/**
 * Resolve o cabeçalho de preset constraints selecionado ao criar um Supply
 * Plan.
 *
 * <p>O {@code SupplyPlan} compartilhado conserva somente a referência
 * unidirecional ao cabeçalho. As regras filhas, a validação do agregado e o
 * consumo pelo optimizer continuam privados do Enterprise. No runtime
 * Community não existe implementação desta SPI: uma seleção explícita deve
 * falhar com a mensagem de edição Enterprise, sem consultar tabelas privadas.</p>
 */
public interface SupplyPlanPresetConstraintGroupSpi {

    /**
     * Resolve o cabeçalho persistido que será associado ao Supply Plan novo.
     *
     * @param presetConstraintGroupId identificador informado pelo payload da rodada.
     * @return cabeçalho persistido e válido para associação ao plano.
     */
    RestricaoPredefinidaGrupo resolvePresetConstraintGroup(
            String presetConstraintGroupId);

}
