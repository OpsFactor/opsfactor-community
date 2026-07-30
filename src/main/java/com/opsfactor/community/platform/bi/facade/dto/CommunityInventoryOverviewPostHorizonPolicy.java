package com.opsfactor.community.platform.bi.facade.dto;

/**
 * Define como a cobertura física se comporta quando o saldo não se esgota
 * dentro dos buckets materializados no Supply Plan.
 */
public enum CommunityInventoryOverviewPostHorizonPolicy {

    /** Publica somente a cobertura observável no horizonte do plano. */
    LIMIT_TO_PLANNING_HORIZON,

    /** Continua a cobertura pela média diária ponderada pelos dias reais dos buckets. */
    AVERAGE_ALL_PERIODS

}
