package com.opsfactor.community.platform.forecast;

/**
 * Participações de desagregação independentes da janela de ajuste do modelo.
 * Recebe somente estatísticas do treino e curvas já calculadas, sem consultas,
 * fits adicionais ou acesso ao realizado futuro. Compartilhado por poda e DP.
 */
public final class MixHistoricoForecast {

    private MixHistoricoForecast() { }

    /** A regra fica disponível para auditoria; histórico vazio nunca vira volume inventado. */
    public record Resultado(double[] participacoes, String regra) { }

    /**
     * Usa demanda recente vezes resposta relativa local (forecast/escala).
     * Com regressores comuns, a resposta é igual e cancela: resta o mix recente.
     * Com clima/eventos locais, conserva a diferença de resposta antes de
     * reconciliar com o total do nó. Escalas são médias do treino, não do futuro.
     * Se não há massa recente no escopo, utiliza explicitamente as escalas do
     * treino. Se todas as respostas são zero, usa o mix histórico sem resposta.
     * Um escopo inteiramente sem histórico retorna participações zero: somente
     * um total previsto zero é distribuível nesse caso.
     */
    public static Resultado calcular(double[] volumesRecentes, double[] escalasTreino,
            double[] forecastsLocais) {

        int quantidade = volumesRecentes.length;
        if (quantidade == 0 || escalasTreino.length != quantidade
                || forecastsLocais != null && forecastsLocais.length != quantidade) {
            throw new IllegalArgumentException("Vetores incompatíveis para mix histórico");
        }
        double totalRecente = 0D;
        double totalEscalas = 0D;
        for (int indice = 0; indice < quantidade; indice++) {
            validar(volumesRecentes[indice]);
            validar(escalasTreino[indice]);
            if (forecastsLocais != null) validar(forecastsLocais[indice]);
            if (volumesRecentes[indice] > 0D && escalasTreino[indice] == 0D) {
                throw new IllegalArgumentException("Mix recente contém demanda fora do treino informado");
            }
            totalRecente += volumesRecentes[indice];
            totalEscalas += escalasTreino[indice];
        }
        double[] massas = totalRecente > 0D ? volumesRecentes : escalasTreino;
        String regra = totalRecente > 0D ? "RECENT_HISTORY" : "FULL_TRAINING_HISTORY";
        double[] pesos = new double[quantidade];
        double totalPesos = 0D;
        for (int indice = 0; indice < quantidade; indice++) {
            double resposta = forecastsLocais == null ? 1D : escalasTreino[indice] == 0D
                    ? 0D : forecastsLocais[indice] / escalasTreino[indice];
            pesos[indice] = massas[indice] * resposta;
            totalPesos += pesos[indice];
        }
        if (totalPesos == 0D) {
            pesos = massas.clone();
            totalPesos = totalRecente > 0D ? totalRecente : totalEscalas;
            regra += "_NO_POSITIVE_LOCAL_RESPONSE";
        }
        if (!Double.isFinite(totalPesos)) throw new ArithmeticException("Overflow no mix histórico");
        if (totalPesos > 0D) {
            for (int indice = 0; indice < quantidade; indice++) pesos[indice] /= totalPesos;
        } else {
            regra = "NO_TRAINING_MASS";
        }
        return new Resultado(pesos, regra);

    }

    /** Conserva o total autoritativo do nó, sem redistribuir fitted values históricos. */
    public static double[] distribuir(double totalForecast, Resultado mix) {

        validar(totalForecast);
        double[] valores = new double[mix.participacoes().length];
        double soma = 0D;
        for (int indice = 0; indice < valores.length; indice++) {
            valores[indice] = totalForecast * mix.participacoes()[indice];
            soma += valores[indice];
        }
        if (Math.abs(soma - totalForecast) > 1e-8 * Math.max(1D, totalForecast)) {
            throw new IllegalStateException("Forecast positivo sem suporte histórico para distribuição");
        }
        return valores;

    }

    private static void validar(double valor) {

        if (!Double.isFinite(valor) || valor < 0D) {
            throw new IllegalArgumentException("Mix exige valores finitos e não negativos");
        }

    }
}
