package com.opsfactor.community.platform.bi.service;

import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewPostHorizonPolicy;

/**
 * Calcula Days of Supply a partir de séries físicas já agregadas.
 *
 * <p>Manter o cálculo sem entidades nem repositories torna explícito que a
 * cobertura é uma propriedade do estoque agregado no fim do período, não uma
 * soma de coberturas por SKU ou location.</p>
 */
public final class CommunityInventoryOverviewCoverageCalculator {

    private static final double EPSILON = 0.00001;

    private CommunityInventoryOverviewCoverageCalculator() {

    }

    /**
     * Consome o saldo de cada período somente pelos consumos posteriores.
     *
     * <p>Um bucket sem consumo contribui todos os seus dias. Se ainda houver
     * saldo após o horizonte, a política escolhida limita o resultado ou usa
     * a média diária de todos os buckets, ponderada por dias reais.</p>
     */
    public static double[] calculateCoverageDays(
            double[] stockByPeriod,
            double[] consumptionByPeriod,
            double[] daysByPeriod,
            CommunityInventoryOverviewPostHorizonPolicy postHorizonPolicy) {

        if (stockByPeriod == null) {
            throw new IllegalArgumentException("stockByPeriod is required");
        }
        if (consumptionByPeriod == null) {
            throw new IllegalArgumentException("consumptionByPeriod is required");
        }
        if (daysByPeriod == null) {
            throw new IllegalArgumentException("daysByPeriod is required");
        }
        if (stockByPeriod.length != consumptionByPeriod.length
                || stockByPeriod.length != daysByPeriod.length) {
            throw new IllegalArgumentException("Inventory Overview series must have the same number of periods");
        }

        CommunityInventoryOverviewPostHorizonPolicy effectivePolicy =
                postHorizonPolicy == null
                        ? CommunityInventoryOverviewPostHorizonPolicy.LIMIT_TO_PLANNING_HORIZON
                        : postHorizonPolicy;
        double[] coverageDays = new double[stockByPeriod.length];
        double averageDailyConsumption = getAverageDailyConsumption(consumptionByPeriod, daysByPeriod);

        for (int stockPeriodIndex = 0; stockPeriodIndex < stockByPeriod.length; stockPeriodIndex++) {

            double remainingStock = stockByPeriod[stockPeriodIndex];
            if (remainingStock <= EPSILON) {
                continue;
            }

            for (int consumptionPeriodIndex = stockPeriodIndex + 1;
                 consumptionPeriodIndex < consumptionByPeriod.length;
                 consumptionPeriodIndex++) {

                double consumptionInPeriod = consumptionByPeriod[consumptionPeriodIndex];
                double daysInPeriod = daysByPeriod[consumptionPeriodIndex];
                if (daysInPeriod < 0) {
                    throw new IllegalArgumentException("Inventory Overview period days cannot be negative");
                }
                if (consumptionInPeriod <= EPSILON) {
                    coverageDays[stockPeriodIndex] += daysInPeriod;
                    continue;
                }
                if (remainingStock > consumptionInPeriod) {
                    coverageDays[stockPeriodIndex] += daysInPeriod;
                    remainingStock -= consumptionInPeriod;
                    continue;
                }

                coverageDays[stockPeriodIndex] += daysInPeriod * remainingStock / consumptionInPeriod;
                remainingStock = 0;
                break;
            }

            if (remainingStock > EPSILON
                    && effectivePolicy == CommunityInventoryOverviewPostHorizonPolicy.AVERAGE_ALL_PERIODS
                    && averageDailyConsumption > EPSILON) {
                coverageDays[stockPeriodIndex] += remainingStock / averageDailyConsumption;
            }
        }

        return coverageDays;

    }

    /** Calcula média por dia usando todos os buckets, inclusive os de consumo zero. */
    private static double getAverageDailyConsumption(double[] consumptionByPeriod, double[] daysByPeriod) {

        double totalConsumption = 0;
        double totalDays = 0;
        for (int periodIndex = 0; periodIndex < consumptionByPeriod.length; periodIndex++) {

            if (daysByPeriod[periodIndex] < 0) {
                throw new IllegalArgumentException("Inventory Overview period days cannot be negative");
            }
            totalConsumption += consumptionByPeriod[periodIndex];
            totalDays += daysByPeriod[periodIndex];
        }
        return totalDays == 0 ? 0 : totalConsumption / totalDays;

    }

}
