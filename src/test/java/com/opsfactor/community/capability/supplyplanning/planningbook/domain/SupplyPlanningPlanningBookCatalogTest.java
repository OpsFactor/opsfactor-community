package com.opsfactor.community.capability.supplyplanning.planningbook.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class SupplyPlanningPlanningBookCatalogTest {

    @Test
    void getKeyFiguresVisiveisSupplyPlanningBookCommunityShouldReturnDefaultWorkingPlanIds() {

        Assertions.assertEquals(
                List.of(
                        "Total Demand-Working Plan",
                        "Direct Demand-Working Plan",
                        "Direct Demand - Demand Plan-Working Plan",
                        "Indirect Demand-Working Plan",
                        "Safety Stock-Working Plan",
                        "Stock-Working Plan",
                        "Planned Production-Working Plan",
                        "Planned Inbound-Working Plan"),
                SupplyPlanningPlanningBookCatalog.getKeyFiguresVisiveisSupplyPlanningBookCommunity());

    }

    @Test
    void getKeyFiguresEditaveisSupplyPlanningBookCommunityShouldReturnOnlyPersistedManualAdjustments() {

        Assertions.assertEquals(
                List.of(
                        "Stock-Working Plan",
                        "Planned Production-Working Plan",
                        "Planned Inbound-Working Plan"),
                SupplyPlanningPlanningBookCatalog.getKeyFiguresEditaveisSupplyPlanningBookCommunity());

    }

    @Test
    void catalogsShouldBeImmutableSnapshots() {

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> SupplyPlanningPlanningBookCatalog
                        .getKeyFiguresVisiveisSupplyPlanningBookCommunity()
                        .add("Production Orders-Working Plan"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> SupplyPlanningPlanningBookCatalog
                        .getKeyFiguresEditaveisSupplyPlanningBookCommunity()
                        .add("Indirect Demand-Working Plan"));

    }

}
