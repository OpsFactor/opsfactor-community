package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

/**
 * Configuracao imutavel dos regressores que a plataforma deriva de dados ja
 * disponiveis na rodada de Demand Planning.
 *
 * <p>Ao contrario de support series externas, estes sinais nao possuem
 * cadastro, entidade nem integracao propria: o Enterprise calcula trend a
 * partir do STL historico e dias uteis a partir do calendario/feriados. O
 * Community mantem os valores neutros e bloqueia sua ativacao na borda.</p>
 */
public record ForecastInternalRegressorParameters(
        boolean includeTargetTrendGrowthRegressor,
        int trendHistoricalWindowInDays,
        double targetTrendGrowthYearOverYear,
        boolean includeWorkingDaysRegressor) {

    private static final int DEFAULT_TREND_HISTORICAL_WINDOW_IN_DAYS = 365;

    public ForecastInternalRegressorParameters {

        if (trendHistoricalWindowInDays <= 0) {
            throw new IllegalArgumentException(
                    "Internal forecast trend historical window must be positive.");
        }
        if (!Double.isFinite(targetTrendGrowthYearOverYear)) {
            throw new IllegalArgumentException(
                    "Internal forecast target trend growth must be finite.");
        }

    }

    public ForecastInternalRegressorParameters(
            Boolean includeTargetTrendGrowthRegressor,
            Integer trendHistoricalWindowInDays,
            Double targetTrendGrowthYearOverYear,
            Boolean includeWorkingDaysRegressor) {

        this(
                Boolean.TRUE.equals(includeTargetTrendGrowthRegressor),
                trendHistoricalWindowInDays == null
                        ? DEFAULT_TREND_HISTORICAL_WINDOW_IN_DAYS
                        : trendHistoricalWindowInDays,
                targetTrendGrowthYearOverYear == null
                        ? 0.0d
                        : targetTrendGrowthYearOverYear,
                Boolean.TRUE.equals(includeWorkingDaysRegressor));

    }

    public static ForecastInternalRegressorParameters neutral() {

        return new ForecastInternalRegressorParameters(
                false,
                DEFAULT_TREND_HISTORICAL_WINDOW_IN_DAYS,
                0.0d,
                false);

    }

    /**
     * Indica se a rodada precisa materializar ao menos um sinal derivado.
     */
    public boolean isActive() {

        return includeTargetTrendGrowthRegressor || includeWorkingDaysRegressor;

    }
}
