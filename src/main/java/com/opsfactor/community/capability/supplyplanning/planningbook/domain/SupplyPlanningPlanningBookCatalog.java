package com.opsfactor.community.capability.supplyplanning.planningbook.domain;

import java.util.List;

/**
 * Catalogo Community das key figures padrao do Supply Planning Book.
 *
 * <p>O Planning Book de Supply usa key figures tipadas por plano. Por isso os
 * valores publicados aqui sao os mesmos ids enviados no DTO da grade, incluindo
 * o sufixo `-Working Plan`. Manter o contrato nesse formato evita que o front
 * precise reconstruir ids a partir de labels visuais ou conhecer detalhes da
 * classe de projection.</p>
 *
 * <p>Carteira, ordens firmes, estoque em transito, writeoff/batch aging, custos,
 * outbound planejado e key figures customizadas pertencem ao Enterprise e nao
 * aparecem neste catalogo.</p>
 */
public final class SupplyPlanningPlanningBookCatalog {

    private static final String SUFIXO_PLANO_TRABALHO = "-Working Plan";

    private static final List<String> KEY_FIGURES_VISIVEIS_SUPPLY_PLANNING_BOOK_COMMUNITY = List.of(
            "Total Demand" + SUFIXO_PLANO_TRABALHO,
            "Direct Demand" + SUFIXO_PLANO_TRABALHO,
            "Direct Demand - Demand Plan" + SUFIXO_PLANO_TRABALHO,
            "Indirect Demand" + SUFIXO_PLANO_TRABALHO,
            "Safety Stock" + SUFIXO_PLANO_TRABALHO,
            "Stock" + SUFIXO_PLANO_TRABALHO,
            "Planned Production" + SUFIXO_PLANO_TRABALHO,
            "Planned Inbound" + SUFIXO_PLANO_TRABALHO);

    /*
     * Community permite selecionar exatamente as linhas que ja compoem sua
     * grade padrao. O catalogo permanece separado da lista visivel porque o
     * Enterprise amplia apenas a selecao, sem transformar linhas privadas em
     * default da grade.
     */
    private static final List<String> KEY_FIGURES_SELECIONAVEIS_SUPPLY_PLANNING_BOOK_COMMUNITY =
            List.copyOf(KEY_FIGURES_VISIVEIS_SUPPLY_PLANNING_BOOK_COMMUNITY);

    private static final List<String> KEY_FIGURES_EDITAVEIS_SUPPLY_PLANNING_BOOK_COMMUNITY = List.of(
            "Stock" + SUFIXO_PLANO_TRABALHO,
            "Planned Production" + SUFIXO_PLANO_TRABALHO,
            "Planned Inbound" + SUFIXO_PLANO_TRABALHO);

    private SupplyPlanningPlanningBookCatalog() {

    }

    /**
     * Retorna as key figures exibidas por padrao no Supply Planning Book
     * Community.
     *
     * <p>A lista espelha o conjunto criado pela projection padrao de Supply:
     * demanda total/direta/indireta, estoque de seguranca, estoque e fluxos
     * planejados. Todas sao publicadas no `Working Plan`, que e o plano aberto
     * para ajustes manuais na edicao Community.</p>
     */
    public static List<String> getKeyFiguresVisiveisSupplyPlanningBookCommunity() {

        return KEY_FIGURES_VISIVEIS_SUPPLY_PLANNING_BOOK_COMMUNITY;

    }

    /**
     * Retorna as key figures que uma Configured View Community pode selecionar
     * explicitamente para o Supply Planning Book.
     */
    public static List<String> getKeyFiguresSelecionaveisSupplyPlanningBookCommunity() {

        return KEY_FIGURES_SELECIONAVEIS_SUPPLY_PLANNING_BOOK_COMMUNITY;

    }

    /**
     * Retorna as key figures que aceitam ajuste manual no Supply Planning Book
     * Community.
     *
     * <p>`Stock`, `Planned Production` e `Planned Inbound` sao as linhas que o
     * service atual persiste via `SupplyPlanningModificacoesService`. As demais
     * key figures visiveis sao calculadas ou observadas e permanecem
     * somente-leitura no contrato aberto.</p>
     */
    public static List<String> getKeyFiguresEditaveisSupplyPlanningBookCommunity() {

        return KEY_FIGURES_EDITAVEIS_SUPPLY_PLANNING_BOOK_COMMUNITY;

    }

}
