package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningMultiplasLocationsProjection;

/**
 * Ponto opcional para adicionar ordens firmes de producao ao snapshot inicial
 * de uma rodada Supply.
 *
 * <p>O Community nao possui fonte transacional de ordens de producao e, sem
 * implementacao deste SPI, preserva a projection recebida sem alteracao. O
 * Enterprise pode enriquecer a mesma fotografia antes que heuristico e
 * optimizer a reabram como {@code ProductionPlanLinha}, sem relacao JPA
 * inversa nem tipo privado no modulo aberto.</p>
 */
public interface SupplyPlanFirmProductionOrdersSpi {

    /**
     * Materializa somente as linhas firmes habilitadas pelo perfil Enterprise.
     *
     * <p>A implementacao deve ser no-op quando ambos os flags privados estiverem
     * desligados e falhar explicitamente quando BOM, roteiro ou conversao de
     * unidade nao permitirem representar a ordem.</p>
     */
    void populaOrdensFirmesProducao(
            SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection);

}
