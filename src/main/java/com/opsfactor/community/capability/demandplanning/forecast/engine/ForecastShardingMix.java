package com.opsfactor.community.capability.demandplanning.forecast.engine;

import com.opsfactor.community.capability.demandplanning.demandplan.projection.*;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.forecast.MixHistoricoForecast;

import java.util.*;

/**
 * Abertura padrão de uma folha de sharding: nó -> MAPE -> DFU, em memória.
 * A janela do mix não muda a janela do ajuste. Recebe forecasts já calculados;
 * nenhuma regressão, consulta ou leitura de realizado futuro acontece aqui.
 */
public final class ForecastShardingMix {

    private ForecastShardingMix() { }

    /**
     * Reconciliacao com o total do nó. Quando o modelo gerou curvas locais,
     * preserva suas respostas relativas ao histórico em ambos os níveis.
     * Modelos exclusivamente agregados usam apenas participações históricas.
     * O identificador MAPE é obrigatório: inferir DFU como MAPE mudaria o peso
     * de exposições locais e tornaria avaliação e execução inconsistentes.
     */
    public static void aplicar(CalendarioSimples calendario, Integer numeroPeriodos,
            DemandPlanForecastProjection raiz, boolean possuiForecastsLocais) {

        if (numeroPeriodos == null) return;
        int historicos = calendario.getNumeroPeriodosPassados();
        if (numeroPeriodos < 1 || historicos < 1) {
            throw new IllegalArgumentException("Mix de sharding exige histórico e janela positivos");
        }
        int janela = Math.min(numeroPeriodos, historicos);
        var folhas = raiz.getDemandPlanForecastProjectionMaterialLocationList();
        if (folhas.isEmpty()) throw new IllegalArgumentException("Nó de sharding sem DFUs");
        if (folhas.stream().noneMatch(DemandPlanForecastProjectionMaterialLocation::isElegivelMixSharding)) {
            folhas.forEach(folha -> Arrays.fill(folha.forecastBaseline, historicos, calendario.getNumeroPeriodosTotais(), 0D));
            reagregarFuturo(raiz, historicos, calendario.getNumeroPeriodosTotais());
            raiz.setDiagnosticoMixSharding(Map.of("numeroPeriodosSolicitados", numeroPeriodos,
                    "numeroPeriodosUsados", janela, "regra", "NO_ELIGIBLE_DFU"));
            return;
        }
        Map<String, List<Integer>> indicesPorMape = new LinkedHashMap<>();
        double[] recentes = new double[folhas.size()];
        double[] escalas = new double[folhas.size()];
        for (int indice = 0; indice < folhas.size(); indice++) {
            var folha = folhas.get(indice);
            String grupo = Objects.requireNonNull(folha.getGrupoMapeForecast(),
                    "Identidade MAPE não preparada para mix de sharding");
            indicesPorMape.computeIfAbsent(grupo, ignorado -> new ArrayList<>()).add(indice);
            if (!folha.isElegivelMixSharding()) continue;
            for (int periodo = 0; periodo < historicos; periodo++) {
                double demanda = folha.demanda[periodo];
                if (!Double.isFinite(demanda) || demanda < 0D) {
                    throw new IllegalArgumentException("Mix exige demanda histórica física não negativa");
                }
                escalas[indice] += demanda / historicos;
                if (periodo >= historicos - janela) recentes[indice] += demanda;
            }
        }
        var grupos = new ArrayList<>(indicesPorMape.values());
        double[] recentesMape = new double[grupos.size()];
        double[] escalasMape = new double[grupos.size()];
        for (int grupo = 0; grupo < grupos.size(); grupo++) {
            for (int indice : grupos.get(grupo)) {
                recentesMape[grupo] += recentes[indice];
                escalasMape[grupo] += escalas[indice];
            }
        }
        Map<String, Integer> regras = new LinkedHashMap<>();
        for (int periodo = historicos; periodo < calendario.getNumeroPeriodosTotais(); periodo++) {
            double totalNo = raiz.forecastBaseline[periodo];
            double[] forecastsMape = possuiForecastsLocais ? new double[grupos.size()] : null;
            if (possuiForecastsLocais) {
                for (int grupo = 0; grupo < grupos.size(); grupo++) {
                    for (int indice : grupos.get(grupo)) {
                        if (folhas.get(indice).isElegivelMixSharding()) forecastsMape[grupo] += folhas.get(indice).forecastBaseline[periodo];
                    }
                }
            }
            var mixMape = MixHistoricoForecast.calcular(recentesMape, escalasMape, forecastsMape);
            regras.merge("NODE:" + mixMape.regra(), 1, Integer::sum);
            double[] totaisMape = MixHistoricoForecast.distribuir(totalNo, mixMape);
            for (int grupo = 0; grupo < grupos.size(); grupo++) {
                var indices = grupos.get(grupo);
                double[] recentesDfu = new double[indices.size()];
                double[] escalasDfu = new double[indices.size()];
                double[] forecastsDfu = possuiForecastsLocais ? new double[indices.size()] : null;
                for (int local = 0; local < indices.size(); local++) {
                    int indice = indices.get(local);
                    recentesDfu[local] = recentes[indice];
                    escalasDfu[local] = escalas[indice];
                    if (possuiForecastsLocais && folhas.get(indice).isElegivelMixSharding()) forecastsDfu[local] = folhas.get(indice).forecastBaseline[periodo];
                }
                var mixDfu = MixHistoricoForecast.calcular(recentesDfu, escalasDfu, forecastsDfu);
                regras.merge("MAPE:" + mixDfu.regra(), 1, Integer::sum);
                double[] valores = MixHistoricoForecast.distribuir(totaisMape[grupo], mixDfu);
                for (int local = 0; local < indices.size(); local++) {
                    folhas.get(indices.get(local)).forecastBaseline[periodo] = valores[local];
                }
            }
        }
        // Somente baseline futuro: ajustes/fitted values e demanda não são reagregados.
        reagregarFuturo(raiz, historicos, calendario.getNumeroPeriodosTotais());
        raiz.setDiagnosticoMixSharding(Map.of("numeroPeriodosSolicitados", numeroPeriodos,
                "numeroPeriodosUsados", janela, "respostaLocal", possuiForecastsLocais,
                "numeroGruposMape", grupos.size(), "regras", Map.copyOf(regras)));

    }

    private static void reagregarFuturo(DemandPlanForecastProjection projection, int primeiro, int total) {

        if (projection instanceof DemandPlanForecastProjectionAgregado agregado) {
            for (var filho : agregado.getDemandPlanForecastProjectionDesagregados()) reagregarFuturo(filho, primeiro, total);
            for (int periodo = primeiro; periodo < total; periodo++) {
                double soma = 0D;
                for (var filho : agregado.getDemandPlanForecastProjectionDesagregados()) soma += filho.forecastBaseline[periodo];
                projection.forecastBaseline[periodo] = soma;
            }
        }

    }
}
