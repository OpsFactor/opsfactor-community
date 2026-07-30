package com.opsfactor.community.capability.demandplanning.forecast.configuration;

import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Catalogo Community dos modelos estatisticos de Demand Planning disponiveis.
 *
 * <p>O enum compartilhado possui valores Enterprise para aceitar payloads
 * transicionais e gerar erros funcionais claros. Esta classe concentra o
 * subconjunto realmente aberto no Community, para que mapper, service, OpenAPI
 * e runtime-info nao mantenham allowlists paralelas.</p>
 */
public final class DemandPlanningModelCatalog {

    private static final List<Constantes.DPModeloEstatistico> DP_MODELOS_ESTATISTICOS_OPENAPI_ORDER_COMMUNITY =
            List.of(
                    Constantes.DPModeloEstatistico.MM,
                    Constantes.DPModeloEstatistico.RMM,
                    Constantes.DPModeloEstatistico.ARIMA,
                    Constantes.DPModeloEstatistico.HOLT_WINTERS,
                    Constantes.DPModeloEstatistico.ES);

    private static final Set<Constantes.DPModeloEstatistico> DP_MODELOS_ESTATISTICOS_COMMUNITY =
            Collections.unmodifiableSet(EnumSet.copyOf(DP_MODELOS_ESTATISTICOS_OPENAPI_ORDER_COMMUNITY));

    private static final List<String> DP_MODELOS_ESTATISTICOS_OPENAPI_COMMUNITY =
            getJsonPropertyLabels(DP_MODELOS_ESTATISTICOS_OPENAPI_ORDER_COMMUNITY);

    private static final List<Constantes.DPModeloEstatistico> DP_MODELOS_ESTATISTICOS_RUNTIME_OPTIONS_OPENAPI_ORDER =
            List.of(
                    Constantes.DPModeloEstatistico.MM,
                    Constantes.DPModeloEstatistico.RMM,
                    Constantes.DPModeloEstatistico.ARIMA,
                    Constantes.DPModeloEstatistico.HOLT_WINTERS,
                    Constantes.DPModeloEstatistico.ES,
                    Constantes.DPModeloEstatistico.SNAIVE,
                    Constantes.DPModeloEstatistico.STL,
                    Constantes.DPModeloEstatistico.PROPHET,
                    Constantes.DPModeloEstatistico.ETS,
                    Constantes.DPModeloEstatistico.TBATS,
                    Constantes.DPModeloEstatistico.BUDGET_DECOMPOSITION,
                    Constantes.DPModeloEstatistico.CHRONOS);

    private static final List<String> DP_MODELOS_ESTATISTICOS_RUNTIME_OPTIONS_OPENAPI =
            getJsonPropertyLabels(DP_MODELOS_ESTATISTICOS_RUNTIME_OPTIONS_OPENAPI_ORDER);

    private static final List<Constantes.DPModeloSplit> DP_MODELOS_SPLIT_OPENAPI_ORDER_COMMUNITY =
            List.of(
                    Constantes.DPModeloSplit.HISTORICAL_SALES);

    private static final Set<Constantes.DPModeloSplit> DP_MODELOS_SPLIT_COMMUNITY =
            Collections.unmodifiableSet(EnumSet.copyOf(DP_MODELOS_SPLIT_OPENAPI_ORDER_COMMUNITY));

    private static final List<String> DP_MODELOS_SPLIT_OPENAPI_COMMUNITY =
            getJsonPropertyLabels(DP_MODELOS_SPLIT_OPENAPI_ORDER_COMMUNITY);

    private static final List<Constantes.DPModeloSplit> DP_MODELOS_SPLIT_RUNTIME_OPTIONS_OPENAPI_ORDER =
            List.of(
                    Constantes.DPModeloSplit.HISTORICAL_SALES,
                    Constantes.DPModeloSplit.FORECAST_PROPORTION,
                    Constantes.DPModeloSplit.HTS);

    private static final List<String> DP_MODELOS_SPLIT_RUNTIME_OPTIONS_OPENAPI =
            getJsonPropertyLabels(DP_MODELOS_SPLIT_RUNTIME_OPTIONS_OPENAPI_ORDER);

    private static final List<Constantes.DPModeloDemandaBase> DP_MODELOS_TRATAMENTO_STOCKOUT_OPENAPI_ORDER_COMMUNITY =
            List.of(
                    Constantes.DPModeloDemandaBase.DESATIVADO);

    private static final List<String> DP_MODELOS_TRATAMENTO_STOCKOUT_OPENAPI_COMMUNITY =
            getJsonPropertyLabels(DP_MODELOS_TRATAMENTO_STOCKOUT_OPENAPI_ORDER_COMMUNITY);

    private static final List<Constantes.DPModeloDemandaBase> DP_MODELOS_TRATAMENTO_STOCKOUT_RUNTIME_OPTIONS_OPENAPI_ORDER =
            List.of(
                    Constantes.DPModeloDemandaBase.DESATIVADO,
                    Constantes.DPModeloDemandaBase.DOH_ESTOQUE_FIM_PERIODO);

    private static final List<String> DP_MODELOS_TRATAMENTO_STOCKOUT_RUNTIME_OPTIONS_OPENAPI =
            getJsonPropertyLabels(DP_MODELOS_TRATAMENTO_STOCKOUT_RUNTIME_OPTIONS_OPENAPI_ORDER);

    private static final List<Constantes.DPModeloNormalizacao> DP_MODELOS_LIMPEZA_HISTORICO_OPENAPI_ORDER_COMMUNITY =
            List.of(
                    Constantes.DPModeloNormalizacao.DESATIVADO);

    private static final Set<Constantes.DPModeloNormalizacao> DP_MODELOS_LIMPEZA_HISTORICO_COMMUNITY =
            Collections.unmodifiableSet(EnumSet.copyOf(DP_MODELOS_LIMPEZA_HISTORICO_OPENAPI_ORDER_COMMUNITY));

    private static final List<String> DP_MODELOS_LIMPEZA_HISTORICO_OPENAPI_COMMUNITY =
            getJsonPropertyLabels(DP_MODELOS_LIMPEZA_HISTORICO_OPENAPI_ORDER_COMMUNITY);

    private static final List<Constantes.DPModeloNormalizacao> DP_MODELOS_LIMPEZA_HISTORICO_RUNTIME_OPTIONS_OPENAPI_ORDER =
            List.of(
                    Constantes.DPModeloNormalizacao.DESATIVADO,
                    Constantes.DPModeloNormalizacao.PERCENTIS,
                    Constantes.DPModeloNormalizacao.CAMPANHA);

    private static final List<String> DP_MODELOS_LIMPEZA_HISTORICO_RUNTIME_OPTIONS_OPENAPI =
            getJsonPropertyLabels(DP_MODELOS_LIMPEZA_HISTORICO_RUNTIME_OPTIONS_OPENAPI_ORDER);

    private static final List<Constantes.DPModeloUplift> DP_MODELOS_UPLIFT_OPENAPI_ORDER_COMMUNITY =
            List.of(
                    Constantes.DPModeloUplift.DESATIVADO);

    private static final List<String> DP_MODELOS_UPLIFT_OPENAPI_COMMUNITY =
            getJsonPropertyLabels(DP_MODELOS_UPLIFT_OPENAPI_ORDER_COMMUNITY);

    private static final List<Constantes.DPModeloUplift> DP_MODELOS_UPLIFT_RUNTIME_OPTIONS_OPENAPI_ORDER =
            List.of(
                    Constantes.DPModeloUplift.DESATIVADO,
                    Constantes.DPModeloUplift.ALAVANCAGEM_EVENTO);

    private static final List<String> DP_MODELOS_UPLIFT_RUNTIME_OPTIONS_OPENAPI =
            getJsonPropertyLabels(DP_MODELOS_UPLIFT_RUNTIME_OPTIONS_OPENAPI_ORDER);

    private static final List<Constantes.TipoDocumentoVenda> TIPOS_DOCUMENTO_HISTORICO_OPENAPI_ORDER_COMMUNITY =
            List.of(
                    Constantes.TipoDocumentoVenda.SELLOUT);

    private static final Set<Constantes.TipoDocumentoVenda> TIPOS_DOCUMENTO_HISTORICO_COMMUNITY =
            Collections.unmodifiableSet(EnumSet.copyOf(TIPOS_DOCUMENTO_HISTORICO_OPENAPI_ORDER_COMMUNITY));

    private static final List<String> TIPOS_DOCUMENTO_HISTORICO_OPENAPI_COMMUNITY =
            getJsonPropertyLabels(TIPOS_DOCUMENTO_HISTORICO_OPENAPI_ORDER_COMMUNITY);

    private static final List<String> TIPOS_DOCUMENTO_HISTORICO_CANONICAL_OPENAPI_COMMUNITY =
            getEnumNames(TIPOS_DOCUMENTO_HISTORICO_OPENAPI_ORDER_COMMUNITY);

    private static final List<Constantes.TipoDocumentoVenda> TIPOS_DOCUMENTO_HISTORICO_RUNTIME_OPTIONS_OPENAPI_ORDER =
            List.of(
                    Constantes.TipoDocumentoVenda.SELLOUT,
                    Constantes.TipoDocumentoVenda.SELLIN,
                    Constantes.TipoDocumentoVenda.PEDIDO);

    private static final List<String> TIPOS_DOCUMENTO_HISTORICO_RUNTIME_OPTIONS_OPENAPI =
            getJsonPropertyLabels(TIPOS_DOCUMENTO_HISTORICO_RUNTIME_OPTIONS_OPENAPI_ORDER);

    private DemandPlanningModelCatalog() {

    }

    /**
     * Retorna modelos estatisticos aceitos pela edicao Community.
     */
    public static Set<Constantes.DPModeloEstatistico> getDpModelosEstatisticosCommunity() {

        return DP_MODELOS_ESTATISTICOS_COMMUNITY;

    }

    /**
     * Centraliza a checagem de modelo estatistico Community.
     *
     * <p>Valor nulo retorna {@code false}; defaults para payload ausente devem
     * permanecer nos callers que conhecem a borda funcional.</p>
     */
    public static boolean isDpModeloEstatisticoCommunity(Constantes.DPModeloEstatistico dpModeloEstatistico) {

        return DP_MODELOS_ESTATISTICOS_COMMUNITY.contains(dpModeloEstatistico);

    }

    /**
     * Labels JSON publicados para UI/OpenAPI Community, na ordem desejada.
     */
    public static List<String> getDpModelosEstatisticosOpenApiCommunity() {

        return DP_MODELOS_ESTATISTICOS_OPENAPI_COMMUNITY;

    }

    /**
     * Catalogo visual completo para seletores de modelo de forecast.
     *
     * <p>Ele inclui opcoes Enterprise para que a SPA Community possa renderizar
     * itens cinza/bloqueados com badge Enterprise. Nao usar esta lista para
     * validacao funcional; services devem continuar consultando
     * {@link #getDpModelosEstatisticosCommunity()}.</p>
     */
    public static List<String> getDpModelosEstatisticosOpenApiRuntimeOptions() {

        return DP_MODELOS_ESTATISTICOS_RUNTIME_OPTIONS_OPENAPI;

    }

    /**
     * Retorna modelos de split aceitos pela edicao Community.
     */
    public static Set<Constantes.DPModeloSplit> getDpModelosSplitCommunity() {

        return DP_MODELOS_SPLIT_COMMUNITY;

    }

    /**
     * Centraliza a checagem de split Community.
     */
    public static boolean isDpModeloSplitCommunity(Constantes.DPModeloSplit dpModeloSplit) {

        return DP_MODELOS_SPLIT_COMMUNITY.contains(dpModeloSplit);

    }

    /**
     * Labels JSON de split publicados para UI/OpenAPI Community.
     */
    public static List<String> getDpModelosSplitOpenApiCommunity() {

        return DP_MODELOS_SPLIT_OPENAPI_COMMUNITY;

    }

    /**
     * Catalogo visual completo dos modelos de split/desagregacao.
     */
    public static List<String> getDpModelosSplitOpenApiRuntimeOptions() {

        return DP_MODELOS_SPLIT_RUNTIME_OPTIONS_OPENAPI;

    }

    /**
     * Labels JSON de tratamento de stockout selecionaveis no Community.
     */
    public static List<String> getDpModelosTratamentoStockoutOpenApiCommunity() {

        return DP_MODELOS_TRATAMENTO_STOCKOUT_OPENAPI_COMMUNITY;

    }

    /**
     * Catalogo visual completo dos tratamentos de stockout.
     */
    public static List<String> getDpModelosTratamentoStockoutOpenApiRuntimeOptions() {

        return DP_MODELOS_TRATAMENTO_STOCKOUT_RUNTIME_OPTIONS_OPENAPI;

    }

    /**
     * Retorna modelos de limpeza historica/outlier smoothing aceitos pela
     * edicao Community.
     *
     * <p>O Community executa somente a copia da serie pos-stockout para a serie
     * pos-outlier. Percentis e campanha permanecem no enum compartilhado para
     * desserializacao e rejeicao funcional, mas nao sao executaveis aqui.</p>
     */
    public static Set<Constantes.DPModeloNormalizacao> getDpModelosLimpezaHistoricoCommunity() {

        return DP_MODELOS_LIMPEZA_HISTORICO_COMMUNITY;

    }

    /**
     * Centraliza a checagem de modelo de limpeza historica Community.
     */
    public static boolean isDpModeloLimpezaHistoricoCommunity(
            Constantes.DPModeloNormalizacao dpModeloNormalizacao) {

        return DP_MODELOS_LIMPEZA_HISTORICO_COMMUNITY.contains(dpModeloNormalizacao);

    }

    /**
     * Labels JSON de limpeza historica publicados para UI/runtime Community.
     */
    public static List<String> getDpModelosLimpezaHistoricoOpenApiCommunity() {

        return DP_MODELOS_LIMPEZA_HISTORICO_OPENAPI_COMMUNITY;

    }

    /**
     * Catalogo visual completo dos modelos de limpeza historica/outlier.
     */
    public static List<String> getDpModelosLimpezaHistoricoOpenApiRuntimeOptions() {

        return DP_MODELOS_LIMPEZA_HISTORICO_RUNTIME_OPTIONS_OPENAPI;

    }

    /**
     * Labels JSON de uplift selecionaveis no Community.
     */
    public static List<String> getDpModelosUpliftOpenApiCommunity() {

        return DP_MODELOS_UPLIFT_OPENAPI_COMMUNITY;

    }

    /**
     * Catalogo visual completo dos modelos de uplift.
     */
    public static List<String> getDpModelosUpliftOpenApiRuntimeOptions() {

        return DP_MODELOS_UPLIFT_RUNTIME_OPTIONS_OPENAPI;

    }

    /**
     * Retorna tipos de documento historico aceitos pelo Demand Planning
     * Community.
     */
    public static Set<Constantes.TipoDocumentoVenda> getTiposDocumentoHistoricoCommunity() {

        return TIPOS_DOCUMENTO_HISTORICO_COMMUNITY;

    }

    /**
     * Centraliza a checagem de documento historico Community.
     *
     * <p>Sell-in e Sales Orders permanecem no enum para rejeicao funcional de
     * payloads Enterprise/transicionais, mas nao sao opcoes executaveis no
     * Community.</p>
     */
    public static boolean isTipoDocumentoHistoricoCommunity(
            Constantes.TipoDocumentoVenda tipoDocumentoVenda) {

        return TIPOS_DOCUMENTO_HISTORICO_COMMUNITY.contains(tipoDocumentoVenda);

    }

    /**
     * Labels JSON de documento historico publicados para UI/OpenAPI Community.
     */
    public static List<String> getTiposDocumentoHistoricoOpenApiCommunity() {

        return TIPOS_DOCUMENTO_HISTORICO_OPENAPI_COMMUNITY;

    }

    /**
     * Catalogo visual completo dos tipos de documento historico.
     */
    public static List<String> getTiposDocumentoHistoricoOpenApiRuntimeOptions() {

        return TIPOS_DOCUMENTO_HISTORICO_RUNTIME_OPTIONS_OPENAPI;

    }

    /**
     * Valores canonicos aceitos no payload de parametros globais Community.
     *
     * <p>Alguns DTOs legados expõem o nome do enum (`SELLOUT`) em vez do label
     * JSON (`Sell-out`). Manter esta lista no catalogo evita que OpenAPI,
     * RuntimeInfo e validacoes passem a divergir quando novas fontes historicas
     * forem migradas.</p>
     */
    public static List<String> getTiposDocumentoHistoricoCanonicalOpenApiCommunity() {

        return TIPOS_DOCUMENTO_HISTORICO_CANONICAL_OPENAPI_COMMUNITY;

    }

    private static <T extends Enum<T>> List<String> getJsonPropertyLabels(List<T> enumValues) {

        /*
         * RuntimeInfo e OpenAPI precisam publicar exatamente os labels JSON dos
         * enums compartilhados. A ordem continua definida nos catalogos, mas o
         * texto vem do @JsonProperty para nao criar uma segunda fonte de verdade
         * entre DTO/front e serializacao Jackson.
         */
        return enumValues
                .stream()
                .map(MetodosUtilidade::getValorJsonPropertyDeEnum)
                .toList();

    }

    private static <T extends Enum<T>> List<String> getEnumNames(List<T> enumValues) {

        return enumValues
                .stream()
                .map(Enum::name)
                .toList();

    }

}
