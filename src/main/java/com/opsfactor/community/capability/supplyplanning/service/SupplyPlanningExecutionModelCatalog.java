package com.opsfactor.community.capability.supplyplanning.service;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.platform.utility.MetodosUtilidade;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Catalogo Community dos motores de execucao de Supply Planning disponiveis.
 *
 * <p>O enum compartilhado conserva valores Enterprise para desserializar
 * payloads transicionais e gerar erros funcionais claros. Esta classe concentra
 * o subconjunto realmente executavel no Community para que services,
 * runtime-info e futuras configuracoes de OpenAPI nao mantenham allowlists
 * paralelas.</p>
 */
public final class SupplyPlanningExecutionModelCatalog {

    private static final List<PerfilExecucaoSupplyPlan.ModoExecucao> MODOS_EXECUCAO_SUPPLY_PLAN_OPENAPI_ORDER_COMMUNITY =
            List.of(
                    PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

    private static final Set<PerfilExecucaoSupplyPlan.ModoExecucao> MODOS_EXECUCAO_SUPPLY_PLAN_COMMUNITY =
            Collections.unmodifiableSet(EnumSet.copyOf(MODOS_EXECUCAO_SUPPLY_PLAN_OPENAPI_ORDER_COMMUNITY));

    private static final List<String> MODOS_EXECUCAO_SUPPLY_PLAN_OPENAPI_COMMUNITY =
            getJsonPropertyLabels(MODOS_EXECUCAO_SUPPLY_PLAN_OPENAPI_ORDER_COMMUNITY);

    private static final List<PerfilExecucaoSupplyPlan.ModoExecucao> MODOS_EXECUCAO_SUPPLY_PLAN_RUNTIME_OPTIONS_OPENAPI_ORDER =
            List.of(
                    PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO,
                    PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR,
                    PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN);

    private static final List<String> MODOS_EXECUCAO_SUPPLY_PLAN_RUNTIME_OPTIONS_OPENAPI =
            getJsonPropertyLabels(MODOS_EXECUCAO_SUPPLY_PLAN_RUNTIME_OPTIONS_OPENAPI_ORDER);

    private SupplyPlanningExecutionModelCatalog() {

    }

    /**
     * Retorna motores de execucao aceitos pela edicao Community.
     */
    public static Set<PerfilExecucaoSupplyPlan.ModoExecucao> getModosExecucaoSupplyPlanCommunity() {

        return MODOS_EXECUCAO_SUPPLY_PLAN_COMMUNITY;

    }

    /**
     * Centraliza a checagem de motor Community.
     *
     * <p>Valor nulo retorna {@code false}; defaults de payload ausente devem
     * permanecer nos callers que conhecem a borda funcional.</p>
     */
    public static boolean isModoExecucaoSupplyPlanCommunity(
            PerfilExecucaoSupplyPlan.ModoExecucao modoExecucao) {

        return MODOS_EXECUCAO_SUPPLY_PLAN_COMMUNITY.contains(modoExecucao);

    }

    /**
     * Labels JSON publicados para UI/OpenAPI Community, na ordem desejada.
     */
    public static List<String> getModosExecucaoSupplyPlanOpenApiCommunity() {

        return MODOS_EXECUCAO_SUPPLY_PLAN_OPENAPI_COMMUNITY;

    }

    /**
     * Catalogo visual completo dos motores de Supply Planning.
     *
     * <p>O Community usa esta lista somente para a SPA mostrar `Optimizer` e
     * `Process Chain` como opcoes Enterprise bloqueadas. Validacao funcional
     * deve continuar em {@link #getModosExecucaoSupplyPlanCommunity()}.</p>
     */
    public static List<String> getModosExecucaoSupplyPlanOpenApiRuntimeOptions() {

        return MODOS_EXECUCAO_SUPPLY_PLAN_RUNTIME_OPTIONS_OPENAPI;

    }

    private static <T extends Enum<T>> List<String> getJsonPropertyLabels(List<T> enumValues) {

        /*
         * A lista publicada para RuntimeInfo/OpenAPI segue a ordem do catalogo,
         * mas o texto vem do @JsonProperty do enum de configuracao. Assim a UI
         * nao fica presa a uma segunda string duplicada no service.
         */
        return enumValues
                .stream()
                .map(MetodosUtilidade::getValorJsonPropertyDeEnum)
                .toList();

    }

}
