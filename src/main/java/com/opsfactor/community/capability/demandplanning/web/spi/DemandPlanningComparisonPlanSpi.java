package com.opsfactor.community.capability.demandplanning.web.spi;

import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjectionFactory;

/**
 * Define a extensao opcional de Comparison Plan no Planning Book de Demand
 * Planning.
 *
 * <p>O Community conserva a validacao e o fluxo normal do Planning Book. O
 * Enterprise substitui somente a montagem da projection quando a selecao traz
 * um plano de comparacao, sem alterar o contrato do DTO ou persistir dados.</p>
 */
public interface DemandPlanningComparisonPlanSpi {

    /**
     * Valida a solicitacao antes de carregar planos, views ou projections.
     */
    void validateReferencePlanRequest(String referencePlanId);

    /**
     * Monta a projection exibida para a selecao do Planning Book.
     */
    KeyFigureProjection getKeyFigureProjection(
            ConfiguredViewSelectionDTO configuredViewSelectionDTO,
            DemandPlan demandPlan,
            ConfiguredViewProjection configuredViewProjection,
            KeyFigureProjectionFactory keyFigureProjectionFactory);

}
