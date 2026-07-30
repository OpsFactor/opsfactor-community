package com.opsfactor.community.platform.rinstance;

import com.github.rcaller.rstuff.RCaller;
import com.github.rcaller.rstuff.RCode;
import com.opsfactor.community.platform.rinstance.model.ResultadoForecastEstatistico;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Ponte Community para modelos estatisticos executados em R.
 *
 * <p>Este modulo aberto chama apenas modelos usados pela edicao Community:
 * Holt-Winters, ARIMA sem regressores e Exponential Smoothing. Modelos
 * Enterprise como Prophet, TBATS, STL, Chronos/foundation models e ARIMA com
 * support series devem entrar por platform/capability Enterprise separados.</p>
 */
@Slf4j
public class InstanciaRCaller {

    public static ResultadoForecastEstatistico geraForecastHoltWinters(
            double[] demandaHistorica, Calendario calendario,
            Double alfa, Double beta, Double gama) {

        double[] historico = getHistoricoTratamentoOutliersObrigatorio(
                demandaHistorica,
                calendario,
                "Holt-Winters");
        RCaller caller = RCaller.create();
        RCode code = RCode.create();

        code.addRCode("library(forecast)");
        code.addDoubleArray("historico", historico);

        String especificacaoModelo = "";
        if (alfa != null) especificacaoModelo += "alpha=" + alfa + ",";
        if (beta != null) especificacaoModelo += "beta=" + beta + ",";
        if (gama != null) especificacaoModelo += "gamma=" + gama + ",";

        code.addRCode("ts = ts(historico, frequency = " + calendario.getFrequenciaCalendario() + ")");
        // Sempre aditivo, para evitar explosao multiplicativa e garantir que
        // fitted = trend + level + seasonal.
        code.addRCode("fit_raw <- ets(ts, model='AAA'" +
                (!especificacaoModelo.isEmpty() ? ", " + especificacaoModelo : "") + ")");

        // Converte ETS para estrutura equivalente a HoltWinters.
        code.addRCode("states <- fit_raw$states");
        code.addRCode("n_season <- frequency(ts)");
        code.addRCode("n <- length(ts)");
        code.addRCode("level_ts   <- ts(states[1:n,1], start=start(ts), frequency=n_season)");
        code.addRCode("trend_ts    <- ts(states[1:n,2], start=start(ts), frequency=n_season)");
        code.addRCode("season_ts   <- ts(states[1:n,3], start=start(ts), frequency=n_season)");
        code.addRCode("fitted_ts   <- level_ts + trend_ts + season_ts");  // vetor de fitted values (compatível com fitted(fit))
        code.addRCode("last_state  <- states[n+1, ]");  // +1 porque states tem n+1 linhas
        code.addRCode("s_current_to_past <- last_state[3:(n_season+2)]");
        code.addRCode("s <- rev(s_current_to_past)");  // ordem correta para forecast.HoltWinters
        code.addRCode("fit <- list(" +
                "fitted = ts(cbind(level=level_ts, trend=trend_ts, season=season_ts), start=start(ts), frequency=n_season), " +
                "x = ts, " +
                "alpha = fit_raw$par['alpha'], " +
                "beta = fit_raw$par['beta'], " +
                "gamma = fit_raw$par['gamma'], " +
                "coefficients = c(a=last_state[1], b=last_state[2], s=s), " +
                "seasonal = \"additive\", " +
                "bounds = \"usual\""+
                ")");
        code.addRCode("class(fit) <- \"HoltWinters\"");
        // gera o forecast
        code.addRCode("fc <- forecast(fit,h=" + calendario.getNumeroPeriodosFuturos() + ")");

        // cria outputs
        // as.numeric converte time series para vetores
        // passado decomposto em trend/seasonal/level
        code.addRCode("trend_adjusted <- as.numeric(fit$fitted[, 'trend'])");
        code.addRCode("seasonal_adjusted <- as.numeric(fit$fitted[, 'season'])");
        code.addRCode("level_adjusted <- as.numeric(fit$fitted[, 'level'])");
        code.addRCode("fitted_values <- as.numeric(fitted(fit))");
        // forecast e lower/upper bounds 95%
        code.addRCode("forecast_result <- as.numeric(fc$mean)");
        code.addRCode("upper_forecast <- as.numeric(fc$upper[, 2])"); // 95% upper bound
        code.addRCode("lower_forecast <- as.numeric(fc$lower[, 2])"); // 95% lower bound
        // Extrair componentes de fc$model para gerar decomposição forecast result em componentes trend/season
        code.addRCode("model_coefficients <- as.list(fc$model$coefficients)");
        code.addRCode("a <- model_coefficients$a");
        code.addRCode("b <- model_coefficients$b");
        code.addRCode("s <- unlist(model_coefficients[-c(1,2)])");
        code.addRCode("forecast_index <- 1:length(fc$mean)");
        code.addRCode("trend_forecast <- a + b * forecast_index");
        code.addRCode("seasonal_forecast <- s[(forecast_index - 1) %% length(s) + 1]");

        // Cria uma lista com todos os outputs
        code.addRCode("result_list <- list(trend_adjusted=trend_adjusted, fitted_values=fitted_values, seasonal_adjusted=seasonal_adjusted, " +
                "level_adjusted=level_adjusted, trend_forecast=trend_forecast, seasonal_forecast=seasonal_forecast, " +
                "forecast_result=forecast_result, upper_forecast=upper_forecast, lower_forecast=lower_forecast)");

        caller.setRCode(code);

        ResultadoForecastEstatistico resultadoForecastEstatistico = new ResultadoForecastEstatistico(calendario);

        try{
            caller.runAndReturnResult("result_list");
            // extrai yhat de dentro de fcst
            double[] historicalTrend = caller.getParser().getAsDoubleArray("trend_adjusted");
            double[] historicalLevel = caller.getParser().getAsDoubleArray("level_adjusted");
            double[] historicalSeasonal = caller.getParser().getAsDoubleArray("seasonal_adjusted");
            double[] fittedHistorico = caller.getParser().getAsDoubleArray("fitted_values");
            double[] forecast = caller.getParser().getAsDoubleArray("forecast_result");
            double[] forecastTrend = caller.getParser().getAsDoubleArray("trend_forecast");
            double[] forecastSeasonal = caller.getParser().getAsDoubleArray("seasonal_forecast");
            double[] lowerBound = caller.getParser().getAsDoubleArray("lower_forecast");
            double[] upperBound = caller.getParser().getAsDoubleArray("upper_forecast");

            int periodoPresente    = calendario.getPosicaoPeriodoPresente();
            // o modelo holt-winters não faz o fit histórico de 100% dos dados históricos.
            // ele descarta períodos de um ciclo que não está completo (ex. primeiros períodos que não fecham 12 meses)
            int historicoFittedHoltWinters    = historicalTrend.length;    // quantos pontos ajustados temos
            int numeroPeriodosPorCiclo = (int) calendario.getFrequenciaCalendario();
            int periodoCalendarioInicioFitted = periodoPresente - historicoFittedHoltWinters;

            // Preenche valores históricos não cobertos pelo fitted do holt winters
            for (int periodo = 0; periodo < periodoCalendarioInicioFitted; periodo++) {
                int posicaoArrayHoltWintersTrend = 0;
                // fixa a 1a observação de trend/level
                double trend = historicalLevel[posicaoArrayHoltWintersTrend] + historicalTrend[posicaoArrayHoltWintersTrend];
                // seasonal : acompanha o mês-a-mês do fitted e o desloca para períodos anteriores ao fit
                int posicaoArrayHoltWintersSeasonal = periodo + numeroPeriodosPorCiclo - periodoCalendarioInicioFitted;
                double seasonal = historicalSeasonal[posicaoArrayHoltWintersSeasonal];
                // assume que o modelo é aditivo para que a soma dos componentes faça sentido
                double fitted = trend + seasonal;

                resultadoForecastEstatistico.trend[periodo] = trend;
                resultadoForecastEstatistico.seasonal[periodo] = seasonal;
                resultadoForecastEstatistico.fitHistorico[periodo] = fitted;
            }

            // Períodos passados que foram contemplados no fit do holt-winters
            for (int periodo = periodoCalendarioInicioFitted; periodo < periodoPresente; periodo++) {
                int posicaoArrayHoltWinters = periodo - periodoCalendarioInicioFitted;

                resultadoForecastEstatistico.trend[periodo] = historicalLevel[posicaoArrayHoltWinters] + historicalTrend[posicaoArrayHoltWinters];
                resultadoForecastEstatistico.seasonal[periodo] = historicalSeasonal[posicaoArrayHoltWinters];
                resultadoForecastEstatistico.fitHistorico[periodo] = fittedHistorico[posicaoArrayHoltWinters];
            }

            // periodos futuros : forecast, trend, seasonal e bounds
            for (int i = calendario.getPosicaoPeriodoPresente(); i <= calendario.getPosicaoPeriodoFinalFuturo(); i++) {
                resultadoForecastEstatistico.forecast[i] = forecast[i - calendario.getPosicaoPeriodoPresente()];
                resultadoForecastEstatistico.lowerBound[i] = lowerBound[i - calendario.getPosicaoPeriodoPresente()];
                resultadoForecastEstatistico.upperBound[i] = upperBound[i - calendario.getPosicaoPeriodoPresente()];
                resultadoForecastEstatistico.trend[i] = forecastTrend[i - calendario.getPosicaoPeriodoPresente()];
                resultadoForecastEstatistico.seasonal[i] = forecastSeasonal[i - calendario.getPosicaoPeriodoPresente()];
            }

        // se gerar erro retorna a média dos valores de input
        } catch (RuntimeException runtimeException) {
            log.error("Error generating holt-winters forecast for parameters alpha = {}, beta = {}, gamma = {}",
                    alfa,
                    beta,
                    gama,
                    runtimeException);
            RCaller callerException = RCaller.create();
            code = RCode.create();
            code.addDoubleArray("y", historico);
            code.addRCode("fcst <- rep(mean(y)," + calendario.getNumeroPeriodosTotais() + ")");
            callerException.setRCode(code);
            callerException.runAndReturnResult("fcst");
            // extrai yhat de dentro de fcst
            resultadoForecastEstatistico.forecast = callerException.getParser().getAsDoubleArray("fcst");
            resultadoForecastEstatistico.trend = resultadoForecastEstatistico.forecast;
        }

        caller.deleteTempFiles();

        return resultadoForecastEstatistico;
    }

    public static ResultadoForecastEstatistico geraForecastAutoArima(
            double[] demandaHistorica,
            Calendario calendario) {

        double[] historico = getHistoricoTratamentoOutliersObrigatorio(
                demandaHistorica,
                calendario,
                "ARIMA");
        RCaller caller = RCaller.create();
        RCode code = RCode.create();

        code.addRCode("library(forecast)");
        code.addDoubleArray("historico", historico);
        code.addRCode("tsHistorico = ts(historico, frequency = " + calendario.getFrequenciaCalendario() + ")");

        /*
         * Community executa ARIMA puro. Regressores internos/externos,
         * séries de suporte e xreg pertencem ao Enterprise porque dependem
         * de dados e configurações que não fazem parte do recorte aberto.
         */
        code.addRCode("fit <- auto.arima(tsHistorico)");
        code.addRCode("fc <- forecast(fit,h=" + calendario.getNumeroPeriodosFuturos() + ")");

        // forecast e lower/upper bounds 95%
        code.addRCode("forecast_result <- as.numeric(fc$mean)");
        code.addRCode("upper_forecast <- as.numeric(fc$upper[, 2])"); // 95% upper bound
        code.addRCode("lower_forecast <- as.numeric(fc$lower[, 2])"); // 95% lower bound
        code.addRCode("fitted_values <- as.numeric(fitted(fit))");

        // Cria uma lista com todos os outputs
        code.addRCode("result_list <- list(forecast_result=forecast_result, upper_forecast=upper_forecast, lower_forecast=lower_forecast, fitted_values=fitted_values )");

        caller.setRCode(code);

        ResultadoForecastEstatistico resultadoForecastEstatistico = new ResultadoForecastEstatistico(calendario);

        try{
            caller.runAndReturnResult("result_list");
            // extrai yhat de dentro de fcst
            double[] forecast = caller.getParser().getAsDoubleArray("forecast_result");
            double[] lowerBound = caller.getParser().getAsDoubleArray("lower_forecast");
            double[] upperBound = caller.getParser().getAsDoubleArray("upper_forecast");
            double[] fittedValues = caller.getParser().getAsDoubleArray("fitted_values");

            // periodos futuros : forecast, trend, seasonal e bounds
            for (int i = calendario.getPosicaoPeriodoPresente(); i <= calendario.getPosicaoPeriodoFinalFuturo(); i++) {
                resultadoForecastEstatistico.forecast[i] = forecast[i - calendario.getPosicaoPeriodoPresente()];
                resultadoForecastEstatistico.lowerBound[i] = lowerBound[i - calendario.getPosicaoPeriodoPresente()];
                resultadoForecastEstatistico.upperBound[i] = upperBound[i - calendario.getPosicaoPeriodoPresente()];
            }
            resultadoForecastEstatistico.fitHistorico = fittedValues;

            // se gerar erro retorna a média dos valores de input
        } catch (RuntimeException runtimeException) {
            log.error("Error generating auto-arima forecast", runtimeException);
            RCaller callerException = RCaller.create();
            code = RCode.create();
            code.addDoubleArray("y", historico);
            code.addRCode("fcst <- rep(mean(y)," + calendario.getNumeroPeriodosTotais() + ")");
            callerException.setRCode(code);
            callerException.runAndReturnResult("fcst");
            // extrai yhat de dentro de fcst
            resultadoForecastEstatistico.forecast = callerException.getParser().getAsDoubleArray("fcst");
            resultadoForecastEstatistico.trend = resultadoForecastEstatistico.forecast;
        }

        caller.deleteTempFiles();

        return resultadoForecastEstatistico;

    }

    public static ResultadoForecastEstatistico geraForecastExponentialSmoothing(
            double[] demandaHistorica, Calendario calendario) {

        double[] historico = getHistoricoTratamentoOutliersObrigatorio(
                demandaHistorica,
                calendario,
                "Exponential Smoothing");
        RCaller caller = RCaller.create();
        RCode code = RCode.create();

        code.addRCode("library(forecast)");
        code.addDoubleArray("historico", historico);

        // Define a série temporal com a frequência do calendário
        code.addRCode("ts = ts(historico, frequency = " + calendario.getFrequenciaCalendario() + ")");

        // Ajusta o modelo usando Exponential Smoothing (ETS) com componentes aditivas
        code.addRCode("fit <- ets(ts)");

        // Gera o forecast
        code.addRCode("fc <- forecast(fit,h=" + calendario.getNumeroPeriodosFuturos() + ")");

        // Cria outputs
        code.addRCode("forecast_result <- as.numeric(fc$mean)");
        code.addRCode("upper_forecast <- as.numeric(fc$upper[, 2])"); // Limite superior de 95%
        code.addRCode("lower_forecast <- as.numeric(fc$lower[, 2])"); // Limite inferior de 95%

        code.addRCode("result_list <- list(forecast_result=forecast_result, upper_forecast=upper_forecast, lower_forecast=lower_forecast)");

        caller.setRCode(code);

        ResultadoForecastEstatistico resultadoForecastEstatistico = new ResultadoForecastEstatistico(calendario);

        try {
            caller.runAndReturnResult("result_list");
            // Extrai o forecast e os limites
            double[] forecast = caller.getParser().getAsDoubleArray("forecast_result");
            double[] lowerBound = caller.getParser().getAsDoubleArray("lower_forecast");
            double[] upperBound = caller.getParser().getAsDoubleArray("upper_forecast");

            // Atribui valores para o forecast dos períodos futuros
            for (int i = calendario.getPosicaoPeriodoPresente(); i <= calendario.getPosicaoPeriodoFinalFuturo(); i++) {
                resultadoForecastEstatistico.forecast[i] = forecast[i - calendario.getPosicaoPeriodoPresente()];
                resultadoForecastEstatistico.lowerBound[i] = lowerBound[i - calendario.getPosicaoPeriodoPresente()];
                resultadoForecastEstatistico.upperBound[i] = upperBound[i - calendario.getPosicaoPeriodoPresente()];
            }

        } catch (RuntimeException runtimeException) {
            log.error("Error generating exponential smoothing forecast", runtimeException);
            RCaller callerException = RCaller.create();
            code = RCode.create();
            code.addDoubleArray("y", historico);
            code.addRCode("fcst <- rep(mean(y)," + calendario.getNumeroPeriodosTotais() + ")");
            callerException.setRCode(code);
            callerException.runAndReturnResult("fcst");
            resultadoForecastEstatistico.forecast = callerException.getParser().getAsDoubleArray("fcst");
            resultadoForecastEstatistico.trend = resultadoForecastEstatistico.forecast;
        }

        caller.deleteTempFiles();

        return resultadoForecastEstatistico;
    }

    /**
     * Extrai a janela historica tratada que sera enviada ao R no Community.
     *
     * <p>As projections de forecast carregam arrays no calendario operacional,
     * mas os modelos estatisticos R devem receber somente os periodos
     * historicos. Validar antes de copiar e importante porque
     * {@code Arrays.copyOf(...)} completaria uma serie curta com zeros,
     * transformando projection incompleta em venda historica aparentemente
     * valida. Como este caller e usado pelas engines abertas de ARIMA,
     * Holt-Winters e Exponential Smoothing, a mensagem identifica o modelo que
     * quebrou o contrato.</p>
     */
    private static double[] getHistoricoTratamentoOutliersObrigatorio(
            double[] vendaHistoricaTratamentoOutliers,
            Calendario calendario,
            String modeloEstatistico) {

        if (calendario == null) {
            throw new IllegalArgumentException(
                    "Community R "
                            + modeloEstatistico
                            + " forecast requires calendar.");
        }
        if (vendaHistoricaTratamentoOutliers == null) {
            throw new IllegalArgumentException(
                    "Community R "
                            + modeloEstatistico
                            + " forecast requires treated historical sales array.");
        }

        int numeroPeriodosPassados = calendario.getNumeroPeriodosPassados();
        if (vendaHistoricaTratamentoOutliers.length < numeroPeriodosPassados) {
            throw new IllegalArgumentException(
                    "Community R "
                            + modeloEstatistico
                            + " forecast requires treated historical sales array with at least "
                            + numeroPeriodosPassados
                            + " past periods, received "
                            + vendaHistoricaTratamentoOutliers.length
                            + ".");
        }

        double[] historico =
                Arrays.copyOf(
                        vendaHistoricaTratamentoOutliers,
                        numeroPeriodosPassados);
        for (int indiceHistorico = 0; indiceHistorico < historico.length; indiceHistorico++) {
            if (!Double.isFinite(historico[indiceHistorico])) {
                throw new IllegalStateException(
                        "Community R "
                                + modeloEstatistico
                                + " forecast requires finite treated historical values. Index "
                                + indiceHistorico
                                + " has value "
                                + historico[indiceHistorico]
                                + ".");
            }
        }
        return historico;

    }

}
