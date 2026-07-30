package com.opsfactor.community.platform.bi.service;

import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewPostHorizonPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommunityInventoryOverviewCoverageCalculatorTest {

    @Test
    void shouldDepleteEndOfPeriodAggregateStockOnlyAgainstFollowingPeriods() {

        double[] coverageDays = CommunityInventoryOverviewCoverageCalculator.calculateCoverageDays(
                new double[]{400, 0, 0, 0},
                new double[]{9_999, 100, 100, 400},
                new double[]{31, 30, 31, 30},
                CommunityInventoryOverviewPostHorizonPolicy.LIMIT_TO_PLANNING_HORIZON);

        assertEquals(76, coverageDays[0], 0.00001);
        assertEquals(0, coverageDays[1], 0.00001);

    }

    @Test
    void shouldKeepZeroConsumptionBucketsAndApplyAverageOnlyAfterTheHorizon() {

        double[] limitedCoverageDays = CommunityInventoryOverviewCoverageCalculator.calculateCoverageDays(
                new double[]{300, 0, 0},
                new double[]{0, 100, 100},
                new double[]{31, 30, 31},
                CommunityInventoryOverviewPostHorizonPolicy.LIMIT_TO_PLANNING_HORIZON);
        double[] averageCoverageDays = CommunityInventoryOverviewCoverageCalculator.calculateCoverageDays(
                new double[]{300, 0, 0},
                new double[]{0, 100, 100},
                new double[]{31, 30, 31},
                CommunityInventoryOverviewPostHorizonPolicy.AVERAGE_ALL_PERIODS);

        assertEquals(61, limitedCoverageDays[0], 0.00001);
        assertEquals(107, averageCoverageDays[0], 0.00001);
        assertArrayEquals(new double[]{61, 0, 0}, limitedCoverageDays, 0.00001);

    }

    @Test
    void shouldRemainFiniteWhenThereIsNoConsumption() {

        double[] coverageDays = CommunityInventoryOverviewCoverageCalculator.calculateCoverageDays(
                new double[]{100, 100},
                new double[]{0, 0},
                new double[]{30, 31},
                CommunityInventoryOverviewPostHorizonPolicy.AVERAGE_ALL_PERIODS);

        assertArrayEquals(new double[]{31, 0}, coverageDays, 0.00001);

    }

}
