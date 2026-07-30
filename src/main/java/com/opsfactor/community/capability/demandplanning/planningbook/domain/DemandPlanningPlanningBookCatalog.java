package com.opsfactor.community.capability.demandplanning.planningbook.domain;

import java.util.List;

/**
 * Catalogo Community das opcoes de Planning Book de Demand Planning.
 *
 * <p>O Community trabalha apenas no nivel material/location e nao permite
 * selecao livre de key figures na view. Mesmo assim o front novo precisa
 * descobrir em runtime quais linhas padrao deve renderizar e quais delas podem
 * receber ajuste manual. Este catalogo centraliza esses labels publicos sem
 * criar enum de edicao ou dependencia de classes de projection.</p>
 */
public final class DemandPlanningPlanningBookCatalog {

    private static final List<String> KEY_FIGURES_VISIVEIS_DEMAND_PLANNING_BOOK_COMMUNITY = List.of(
            "Direct Demand",
            "Historical Sales",
            "Baseline",
            "Demand Adjustment");

    private static final List<String> KEY_FIGURES_EDITAVEIS_DEMAND_PLANNING_BOOK_COMMUNITY = List.of(
            "Direct Demand",
            "Demand Adjustment");

    private DemandPlanningPlanningBookCatalog() {

    }

    /**
     * Retorna as key figures exibidas por padrao no Planning Book de demanda
     * Community.
     *
     * <p>`Direct Demand` e totalizadora; `Historical Sales` e historico
     * somente-leitura; `Baseline` e `Demand Adjustment` sao as linhas fisicas
     * abertas no fluxo Community. New Materials, Uplift, Customer Orders,
     * Reference Plan e custom key figures permanecem Enterprise. O backend
     * ainda decodifica o rótulo legado New Products para bloquear payloads
     * antigos com mensagem funcional.</p>
     */
    public static List<String> getKeyFiguresVisiveisDemandPlanningBookCommunity() {

        return KEY_FIGURES_VISIVEIS_DEMAND_PLANNING_BOOK_COMMUNITY;

    }

    /**
     * Retorna as key figures que uma Configured View Community pode selecionar.
     *
     * <p>O Community nao possui linhas adicionais fora do seu catalogo aberto.
     * Por isso o conjunto selecionavel coincide deliberadamente com o conjunto
     * visivel, sem expor identidades Enterprise pela API de RuntimeInfo.</p>
     */
    public static List<String> getKeyFiguresSelecionaveisDemandPlanningBookCommunity() {

        return KEY_FIGURES_VISIVEIS_DEMAND_PLANNING_BOOK_COMMUNITY;

    }

    /**
     * Retorna as key figures que o Planning Book de demanda Community aceita
     * para edicao manual.
     *
     * <p>`Direct Demand` continua na lista porque a UI permite ajuste na linha
     * totalizadora e o backend redistribui para `Demand Adjustment` conforme a
     * regra Community. `Baseline` permanece visivel, mas nao editavel: ela e a
     * fotografia estatistica base gerada pela rodada de demanda, enquanto a
     * colaboracao manual Community deve passar pela linha totalizadora ou pela
     * propria linha de ajuste. `Historical Sales` tambem nao aparece aqui por
     * ser uma serie observada somente-leitura.</p>
     */
    public static List<String> getKeyFiguresEditaveisDemandPlanningBookCommunity() {

        return KEY_FIGURES_EDITAVEIS_DEMAND_PLANNING_BOOK_COMMUNITY;

    }

}
