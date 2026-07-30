package com.opsfactor.community.capability.demandplanning.planningbook.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Protege o catalogo Community das key figures publicadas para o Planning Book
 * de Demand Planning.
 */
class DemandPlanningPlanningBookCatalogTest {

    @Test
    void getKeyFiguresVisiveisDemandPlanningBookCommunityShouldExposeDefaultCommunityView() {

        Assertions.assertEquals(
                List.of(
                        "Direct Demand",
                        "Historical Sales",
                        "Baseline",
                        "Demand Adjustment"),
                DemandPlanningPlanningBookCatalog.getKeyFiguresVisiveisDemandPlanningBookCommunity());

    }

    @Test
    void getKeyFiguresEditaveisDemandPlanningBookCommunityShouldExposeOnlyCommunityEditableRows() {

        Assertions.assertEquals(
                List.of(
                        "Direct Demand",
                        "Demand Adjustment"),
                DemandPlanningPlanningBookCatalog.getKeyFiguresEditaveisDemandPlanningBookCommunity());

    }

    @Test
    void keyFigureListsShouldBeImmutable() {

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> DemandPlanningPlanningBookCatalog
                        .getKeyFiguresVisiveisDemandPlanningBookCommunity()
                        .add("Uplift"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> DemandPlanningPlanningBookCatalog
                        .getKeyFiguresEditaveisDemandPlanningBookCommunity()
                        .add("Historical Sales"));

    }

}
