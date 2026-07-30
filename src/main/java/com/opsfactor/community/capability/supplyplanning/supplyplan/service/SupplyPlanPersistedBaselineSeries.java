package com.opsfactor.community.capability.supplyplanning.supplyplan.service;

/**
 * Séries físicas cujo preflight de baseline ainda precisa ser informado de
 * forma explícita.
 *
 * <p>O enum pertence ao contrato read-only usado pelo runtime. A operação
 * administrativa que regulariza os valores fica no módulo independente de
 * cutover.</p>
 */
public enum SupplyPlanPersistedBaselineSeries {
    INVENTORY,
    DISTRIBUTION,
    PRODUCTION
}
