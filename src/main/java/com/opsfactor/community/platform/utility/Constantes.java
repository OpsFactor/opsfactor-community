package com.opsfactor.community.platform.utility;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Constantes {

    /**
     * Tipo de documento usado para historico de vendas.
     *
     * <p>Community processa apenas `SELLOUT`. `SELLIN` e `PEDIDO` permanecem no
     * enum para desserializar payloads/configuracoes compartilhadas e permitir
     * rejeicao explicita nas bordas de service/controller.</p>
     */
    public enum TipoDocumentoVenda {
        @JsonProperty("Sell-out") SELLOUT,
        @JsonProperty("Sell-in") SELLIN,
        @JsonProperty("Sales Orders") PEDIDO
    }

    public enum ModoArredondamento {
        @JsonProperty("Round Up") ARREDONDA_PARA_CIMA, 
        @JsonProperty("Round") ARREDONDA, 
        @JsonProperty("Round Down") ARREDONDA_PARA_BAIXO;
    }
    
    /**
     * Exemplo : produção consome capacidade no período D e após tempo de cura material é liberado em D+2
     * Exemplo : requisição consome estoque na origem em D e material é disponibilizado no destino em D+3
     */
    public enum ReferenciaPeriodo {
        CONSUMO_CAPACIDADE,
        DISPONIBILIZACAO_MATERIAL
    }

    // CONFIGURAÇÕES GLOBAIS DE PLANEJAMENTO ----------------------------------------------------------------------------
    public enum TamanhoBucket {
        @JsonProperty("Daily") DIARIO,
        @JsonProperty("Weekly") SEMANAL, 
        @JsonProperty("Monthly") MENSAL,
        @JsonProperty("Second") SEGUNDO,
        @JsonProperty("Minute") MINUTO,
        @JsonProperty("10 minutes") SEXTO_HORA,
        @JsonProperty("Quarter-Hourly") QUARTO_HORA,
        @JsonProperty("Half-Hourly") MEIA_HORA,
        @JsonProperty("Hourly") HORARIO, 
        @JsonProperty("8-hour turn") TURNO,
        @JsonProperty("Yearly") ANUAL;

        public int getNivelAgregacao() {
            switch (this) {
                case SEGUNDO : return 1;
                case MINUTO : return 2;
                case SEXTO_HORA : return 3;
                case QUARTO_HORA : return 4;
                case MEIA_HORA : return 5;
                case HORARIO : return 6;
                case TURNO : return 7;
                case DIARIO : return 8;
                case SEMANAL : return 9;
                case MENSAL : return 10;
                case ANUAL: return 11;
                default : throw new IllegalStateException("No aggregation level defined for " + this);
            }
        }

        public String getNomePeriodoSingular() {
            switch (this) {
                case SEGUNDO : return "Second";
                case MINUTO : return "Minute";
                case SEXTO_HORA : return "Sixth of Hour";
                case QUARTO_HORA : return "Quarter of Hour";
                case MEIA_HORA : return "Half Hour";
                case HORARIO : return "Hour";
                case TURNO : return "Turn";
                case DIARIO : return "Day";
                case SEMANAL : return "Week";
                case MENSAL : return "Month";
                case ANUAL: return "Year";
                default : throw new IllegalStateException("No period name defined for " + this);
            }
        }

        public String getNomePeriodoPlural() {
            return getNomePeriodoSingular() + "s";
        }


    }

    public static final TamanhoBucket HISTORICAL_PLAN_BUCKET_PADRAO = TamanhoBucket.MENSAL;
    public static final int GLOBAL_PADRAO_DIAS_HISTORICOS_DOH = 28; // semanas consideradas na cobertura c/ vendas históricas - 4 semanas
    public static final int GLOBAL_PADRAO_DIAS_HISTORICOS_ROI = 60;
    public static final int GLOBAL_PADRAO_DIAS_HISTORICOS_DOH_STOCKOUT = 4; // DP : número de dias de estoque para ser considerado stockout
    public static final int GLOBAL_PADRAO_PLANEJAMENTO_CENTRALIZADO = 0; // se 1, a demanda e compras de todas as filiais são centralizadas em um único plano (filial 0)
    public static final int GLOBAL_PADRAO_DIAS_JANELA_ANTERIOR_FORA_LINHA = 56; // 8 semanas
    public static final int GLOBAL_PADRAO_DIAS_JANELA_POSTERIOR_FORA_LINHA = 28; // 4 semanas
    public static final float GLOBAL_PADRAO_PERCENTUAL_VENDAS_FORA_LINHA = 0.05f; // se janela posterior < 5% da janela anterior, FDL
    public static final int DIAS_PLANO_HISTORICO = 90; //dias historicos para a montagem do historical plan

    // CONFIGURACOES DO DEMAND PLANNING ------------------------------------------------------------------------------
    public enum DPModeloEstatistico {
        @JsonProperty("Moving Average") MM, 
        @JsonProperty("Holt-Winters") HOLT_WINTERS, 
        @JsonProperty("ARIMA") ARIMA, 
        @JsonProperty("TBATS") TBATS, 
        @JsonProperty("Exponential Smoothing") ES, 
        @JsonProperty("Rolling Moving Average") RMM, 
        @JsonProperty("Prophet") PROPHET,
        @JsonProperty("Seasonal Naive") SNAIVE,
        @JsonProperty("STL") STL,
        @JsonProperty("ETS") ETS,
        @JsonProperty("Budget as Forecast") BUDGET_DECOMPOSITION, // RMM = ROLLING MOVING AVERAGE
        @JsonProperty("Chronos") CHRONOS,
        @JsonProperty("Price/Campaign/Seasonal/Trend") PRICING_ML
    }

    /**
     * Modelo de split do forecast agregado para DFU.
     *
     * <p>No Community somente `HISTORICAL_SALES` e aceito. Os demais valores
     * ficam no contrato para que configuracoes Enterprise sejam rejeitadas com
     * erro claro em vez de falha de desserializacao.</p>
     */
    public enum DPModeloSplit {
        @JsonProperty("Historical Sales") HISTORICAL_SALES,
        @JsonProperty("DFU-Level STL Forecast") FORECAST_PROPORTION,
        @JsonProperty("Hierarchical Reconciliation") HTS,
    }

    /**
     * Define como a série estatística de Demand Planning será materializada em cada dimensão.
     *
     * A ordem é proposital: `BOTTOM_UP` permanece no ordinal 0 e `TOP_DOWN` no ordinal 1,
     * preservando a semântica dos enums legados separados por material/location.
     */
    public enum DPNivelAgregacao {
        @JsonProperty("Bottom-Up") BOTTOM_UP,
        @JsonProperty("Top-Down") TOP_DOWN,
    }
    
    /**
     * Determina como o like modeling será feito
     * HERDA_VOLUME : parte do forecast do material/location original será migrado para o novo material/location, com ou sem canibalização
     * HERDA_CURVA_INTRODUCAO : se copiará a curva de introdução do produto/material espelho
     * --- A CONSTRUIR --- : modelo que segue cluster de curvas de introdução
     */
    public enum DPLikeModel {
        @JsonProperty("Inherit Forecast") HERDA_VOLUME, 
        @JsonProperty("Inherit Introduction Curve") HERDA_CURVA_INTRODUCAO;
    }
    
    public enum DPModeloUplift {
        @JsonProperty("No Uplift Calculation") DESATIVADO, 
        @JsonProperty("Event Uplift") ALAVANCAGEM_EVENTO;
    }

    /**
     * Medidas de quantidade/valor compartilhadas com o DTO de perfil Supply.
     *
     * <p>Community zera e rejeita qualquer uso funcional desse campo ligado a
     * otimizacao, custos, P&L ou cost-to-serve.</p>
     */
    public enum TipoQuantidadeValor {
        @JsonProperty("Quantity") QUANTIDADE,
        @JsonProperty("Gross") GROSS,
        @JsonProperty("Net") NET,
        @JsonProperty("COGS") COGS,
        @JsonProperty("Margin") MARGEM;

        /**
         * Converte medidas economicas para o enum de valores.
         *
         * <p>`QUANTIDADE` e mantido no enum compartilhado porque muitos DTOs
         * permitem escolher entre quantidade e valor, mas ele nao possui
         * correspondencia monetaria. Quem precisa de valor deve estar em um dos
         * quatro ramos economicos explicitamente aceitos.</p>
         */
        public TipoValor converteParaTipoValor() {
            switch (this) {
                case QUANTIDADE: 
                    throw new IllegalArgumentException("Impossivel converter QUANTITY para TipoValor");
                case GROSS:
                    return TipoValor.GROSS;
                case NET:
                    return TipoValor.NET;
                case COGS:
                    return TipoValor.COGS;
                case MARGEM:
                    return TipoValor.MARGEM;
                default:
                    throw new IllegalStateException(
                            "TipoQuantidadeValor " + this
                                    + " cannot be converted to TipoValor. Supported values are GROSS, NET, COGS and MARGEM; QUANTIDADE is not a monetary value.");
            }
        }
        
    }
    
    /**
     * Medidas economicas preservadas apenas como contrato transicional.
     * Calculos de Gross/Net/COGS/Margem pertencem ao Enterprise.
     */
    public enum TipoValor {
        @JsonProperty("Gross") GROSS,
        @JsonProperty("Net") NET,
        @JsonProperty("COGS") COGS,
        @JsonProperty("Margin") MARGEM
    }
    
    public enum FirmePlanejado {
        @JsonProperty("Firm Delivery") REMESSA,
        @JsonProperty("Firm Order") ORDEM,
        @JsonProperty("Planned Order") PLANEJADO,
        @JsonProperty("Firm + Planned Orders") TOTAL
    }
    
    public enum RacionalReposicao {
        @JsonProperty("Client Order Fulfillment (Direct)") ATENDIMENTO_DIRETO_CARTEIRA,
        @JsonProperty("Client Order Fulfillment (Indirect)") ATENDIMENTO_INDIRETO_CARTEIRA,
        @JsonProperty("Demand Plan Fulfillment (Direct)") ATENDIMENTO_DIRETO_DEMANDA,
        @JsonProperty("Demand Plan Fulfillment (Indirect)") ATENDIMENTO_INDIRETO_DEMANDA,
        @JsonProperty("Stock Replenishment") REPOSICAO_ESTOQUE,
        @JsonProperty("Manual Adjustment (Indirect)") AJUSTE_MANUAL_PROPAGADO,
        @JsonProperty("Manual Adjustment (Direct)") AJUSTE_MANUAL,
        @JsonProperty("Total") TOTAL
    }
    
    /**
     * Descreve o comportamento a se seguir quando se altera o plano em nível agregado (TOTAL)
     */
    public enum ModificacaoAgregadaPlano {
        /**
         * Ajustes negativos : redução proporcional em todos os tipos de demanda
         * Ajustes positivos : se já houver tipos de demanda populados, proporcional a eles. Caso contrário, aloca 100% em Baseline
         */
        PROPORCIONAL_OU_BASELINE, 
        /**
         * Ajustes positivos ou negativos alocados em AJUSTE_SUPPLY ou AJUSTE_DEMANDA, a depender do plano
         */
        LINHA_AJUSTE
    }
    
    /**
     * Modelo transicional do tratamento de stockout do histórico de forecast.
     *
     * <p>DESATIVADO mantém {@code vendaHistoricaTratamentoStockouts} como cópia
     * da venda histórica observada. DOH_ESTOQUE_FIM_PERIODO pertence ao Enterprise:
     * ele substitui períodos identificados como
     * stockout pela média de períodos anteriores sem stockout. O Community
     * preserva o valor no enum apenas para desserializar payloads legados e
     * bloquear a configuração na borda.</p>
     */
    public enum DPModeloDemandaBase {
        @JsonProperty("Inactive") DESATIVADO, 
        @JsonProperty("Smoothing of Stockout Periods") DOH_ESTOQUE_FIM_PERIODO
    }

    /**
     * DESATIVADO : Community copia a venda histórica tratada por stockout para
     * `vendaHistoricaTratamentoOutliers`, sem limpeza adicional.
     * PERCENTIS : opção Enterprise para limitar outliers por percentil.
     * CAMPANHA : opção Enterprise para remover efeitos promocionais/campanhas com infraestrutura de pricing.
     *
     * Os valores Enterprise permanecem no enum compartilhado para desserializar configurações legadas e permitir que
     * controllers/services Community rejeitem explicitamente uma configuração não suportada.
     */
    public enum DPModeloNormalizacao {
        @JsonProperty("Inactive") DESATIVADO, 
        @JsonProperty("Percentile") PERCENTIS, 
        @JsonProperty("Campaign") CAMPANHA
    }
    
    // CONFIGURAÇÕES DE PLANEJAMENTO PARA O DP ----------------------------------------------------------------------------
    public static final int DP_PADRAO_DIAS_HORIZONTE_FORECAST = 150; // 5 meses como padrão
    public static final DPModeloEstatistico DP_PADRAO_TIPO_MODELO_FORECAST = DPModeloEstatistico.MM; // DP : (1:MM,2:Arima,3:TBATS)
    // DP : nível de agregação:
    // 0:nivel SKU para todos os produtos
    // 1:nivel sub com split para todos os skus
    // 2:curva A nivel SKU . restante split de (Sub - Curva A)
    // 3:curvas A e B nivel SKU . restante split de (Sub - Curva A - Curva B)
    public static final DPNivelAgregacao DP_PADRAO_NIVEL_AGREGACAO_MATERIAL = DPNivelAgregacao.TOP_DOWN;
    public static final DPNivelAgregacao DP_PADRAO_NIVEL_AGREGACAO_LOCATION = DPNivelAgregacao.TOP_DOWN;
    public static final int DP_PADRAO_DIAS_MEDIA_MOVEL = 120; // 3 meses
    public static final int PP_PADRAO_PERIODOS_MEDIA_HISTORICA = 5;
    public static final int DP_PADRAO_DIAS_NORMALIZACAO = 365; // 12 meses
    public static final int DP_PADRAO_MINIMO_DIAS_PARA_NORMALIZACAO = (365 * 4 / 12); // normalização começa com no mínimo 4 meses de histórico
    public static final int DP_PADRAO_DIAS_HISTORICOS_TOP_DOWN = (365 * 4 / 12); // 4 meses
    public static final double DP_PADRAO_PERCENTIL_OUTLIERS_VENDA = 0.8; // DP : int de 0 a 100 : vendas acima desse percentil são removidos da normalização
    public static final int DP_PADRAO_DIAS_HISTORICOS_FORECAST_ESTATISTICO = 740; // o padrão são 2 anos de histórico
    public static final DPModeloNormalizacao DP_PADRAO_TIPO_MODELO_NORMALIZACAO = DPModeloNormalizacao.DESATIVADO; // o padrão são 4 meses de histórico
    public static final DPModeloDemandaBase DP_PADRAO_TIPO_MODELO_DEMANDA_BASE = DPModeloDemandaBase.DESATIVADO;
    public static final TamanhoBucket DP_PADRAO_TAMANHO_BUCKET = TamanhoBucket.SEMANAL;

    public static final TamanhoBucket ESTOQUE_SEGMENTADO_PADRAO_TAMANHO_BUCKET = TamanhoBucket.DIARIO;


    public enum UtilityModelo {
        MLN, NESTED_LOGIT, MIXED_LOGIT
    }

    public static final UtilityModelo UTILITY_PADRAO_MODELO = UtilityModelo.MLN;

    // CONFIGURAÇÕES DE PLANEJAMENTO PARA O DRP ----------------------------------------------------------------------------
    // TIPOS DE MODELOS DE REPOSIÇÃO DRP
    // modelo de reabastecimento
    public enum SNPModeloReabastecimento {
        @JsonProperty("DRP") DRP, // modelo safety stock, usa vendas projetadas e cobertura de segurança
        @JsonProperty("KANBAN") KANBAN, // modelo kanban, usa vendas passadas e ponto de reposição
    }

    public enum SNPCalculoSafetyStock {
        @JsonProperty("DAYS") DAYS, 
        @JsonProperty("QUANTITY") QUANTITY, 
    }

    // modelo operacional
    public enum SNPModeloOperacional {
        @JsonProperty("MTS") MTS, @JsonProperty("MTO") MTO
    }

    /**
     * SEGMENTADO : se faz o cálculo com o estoque segmentado de partida, remessas entrada/saída e estoque total final
     * TOTAL : se usa diretamente a base de estoque alocando 100% do mesmo como Baseline
     */
    public enum SNPTipoEstoque {
        SEGMENTADO, TOTAL
    }

    public enum SNPOrigemReabastecimento {
        @JsonProperty("Production") PRODUCAO,
        @JsonProperty("Requisition") REQUISICAO,
        @JsonProperty("No Replenishment") SEM_RESSUPRIMENTO;
    }

    public enum SNPTipoPedidoRequisicao {
        CLIENTE_FINAL, TRANSFERENCIA
    }

    public static final boolean DRP_PADRAO_PERMITE_BACKLOG = false;
    public static final float DRP_PADRAO_ESTOQUE_SEGURANCA = 0;
    public static final SNPModeloReabastecimento DRP_PADRAO_MODELO_REPOSICAO = SNPModeloReabastecimento.DRP; // DRP :  1:cobertura de segurança  2:kanban
    public static final SNPCalculoSafetyStock DRP_PADRAO_CALCULO_SAFETY_STOCK = SNPCalculoSafetyStock.DAYS;
    public static final TamanhoBucket DRP_PADRAO_TAMANHO_BUCKET = TamanhoBucket.SEMANAL;
    public static final SNPTipoEstoque DRP_PADRAO_TIPO_ESTOQUE = SNPTipoEstoque.TOTAL;


    // CONFIGURAÇÕES DE PLANEJAMENTO POR SKU ------------------------------------------------------------------------------
    public static final int SKU_PADRAO_FORA_LINHA = 0; // RM : se o item não deve ser mais comprado. o padrão é que item seja em linha

    // CONFIGURAÇÕES DAS SIMULAÇÕES DE DEMANDA E ESTOQUE-------------------------------------------------------------------
    public static final int SIMULACAO_PADRAO_NUMERO_SEMANAS_PASSADAS = 52;
    public static final int SIMULACAO_PADRAO_NUMERO_SEMANAS_FUTURAS = 8;
    public static final int SIMULACAO_PADRAO_DP_NUMERO_SIMULACOES_MM = 4;
    public static final int SIMULACAO_PADRAO_DP_STEP_MM = 4;
    public static final int SIMULACAO_PADRAO_RP_NUMERO_SIMULACOES = 8;
    public static final int SIMULACAO_PADRAO_RP_STEP_COBERTURA_ESTOQUE = 2;
    public static final int SIMULACAO_PADRAO_RP_TAXA_JUROS = 6; // valor será dividido por 100 para se chegar ao respectivo float

    // Indices historicos mantidos somente por compatibilidade com estruturas antigas de carga.
    // O Community nao expoe as cargas transacionais de pedidos associadas a estes valores.
    public final static int TABELAS_HISTORICO_FATURAMENTO = 1;
    public final static int TABELAS_HISTORICO_RECEBIMENTO = 2;
    public final static int TABELAS_HISTORICO_ESTOQUE = 3;
    public final static int TABELAS_HISTORICO_PEDIDOS_CLIENTES = 4;
    public final static int TABELAS_HISTORICO_PEDIDOS_FORNECEDORES = 5;

    public final static int TABELAS_CADASTRO_PRODUTOS = 6;
    public final static int TABELAS_CADASTRO_SUBCATEGORIAS = 7;
    public final static int TABELAS_CADASTRO_CATEGORIAS = 8;
    public final static int TABELAS_CADASTRO_FORNECEDORES = 9;
    public final static int TABELAS_CADASTRO_LOCATIONS = 10;

    // ORIGENS DA DEMANDA/RECEBIMENTOS/ESTOQUE
    
    public enum TrendSeasonal {
        @JsonProperty("Seasonal Component") SEASONAL, 
        @JsonProperty("Trend Component") TREND,
        @JsonProperty("Trend+Seasonal Components") TREND_E_SEASONAL;
    }

    /**
     *  BASELINE("Baseline"), 
     *  ITENS_NOVOS("New Items"), 
     *  UPLIFT("Uplift"), 
     *  AJUSTE_DEMANDA("Demand Adjustment"), 
     *  AJUSTE_SUPPLY("Supply Adjustment"), 
     *  TOTAL("Total");
     */
    public enum TipoDemanda {
        @JsonProperty("Baseline") BASELINE("Baseline"), 
        @JsonProperty("New Items") ITENS_NOVOS("New Items"), 
        @JsonProperty("Uplift") UPLIFT("Uplift"), 
        @JsonProperty("Demand Adjustment") AJUSTE_DEMANDA("Demand Adjustment"), 
        @JsonProperty("Supply Adjustment") AJUSTE_SUPPLY("Supply Adjustment"), 
        @JsonProperty("Total") TOTAL("Total");
        
        private String nomeExibicao;
        
        // construtor obrigatório para permitir nome exibição
        TipoDemanda(String nomeExibicao) {
            this.nomeExibicao = nomeExibicao;
        } 
        
        // Optionally and/or additionally, toString.
        @Override 
        public String toString() { return nomeExibicao; }
    }
    
    public enum InboundOutbound {
        @JsonProperty("Inbound") INBOUND, 
        @JsonProperty("Outbound") OUTBOUND
    }
        
    public enum StatusProduto {
        @JsonProperty("Not Launched") NAO_LANCADO("Not Launched"),
        @JsonProperty("New") NOVO("New"),
        @JsonProperty("Regular") REGULAR("Regular"),
        @JsonProperty("Discontinued") DESCONTINUADO("Discontinued");
        
        private String nomeExibicao;
        
        // construtor obrigatório para permitir nome exibição
        StatusProduto(String nomeExibicao) {
            this.nomeExibicao = nomeExibicao;
        } 
        
        // Optionally and/or additionally, toString.
        @Override 
        public String toString() { return nomeExibicao; }
    }
    
    public enum TipoSerie {
        VENDA, ESTOQUE, RECEBIMENTO_REQUISICAO, RECEBIMENTO_PEDIDO
    }
    
    /**
     * PLANO_IRRESTRITO, PLANO_RESTRITO, PLANO_NAO_ATENDIDO, BUDGET, HISTORICO
     */
    public enum TipoPlano {
        @JsonProperty("Unconstrained Plan") PLANO_IRRESTRITO, 
        @JsonProperty("Constrained Plan") PLANO_RESTRITO, 
        @JsonProperty("Working Plan") PLANO_TRABALHO,
        @JsonProperty("Unmet Demand") PLANO_NAO_ATENDIDO,
        @JsonProperty("Budget") BUDGET, 
        @JsonProperty("Historical Values") HISTORICO
    }

    // CONFIGURAÇÕES DE VALORES DEFAULT
    // CONFIGURAÇÕES DO BANCO DE DADOS
    public final static int BD_NUMERO_MAXIMO_LINHAS_EXCLUSAO = 100000; // usado na remoção de linhas do BD : o máximo impede que a rotina trave por falta de memória

    public final static String LOCATION_PADRAO_ID = "0";
    public final static String LOCATION_PADRAO_DESCRICAO = "DEFAULT_LOCATION";

    public final static long CLUSTER_LOCATION_PADRAO_ID = 1;
    public final static String CLUSTER_LOCATION_PADRAO_DESCRICAO = "Default Location Cluster";

    // CONFIGURAÇÕES DO SCHEDULER ---------------------------------------------------------------------    
    public enum TaskTipo {
        DP("DP - Demand Planning"), 
        SNP("SNP - Supply Network Planning"), 
        PLANO_RESTRITO("Constrained Plan"),

        /**
         * Etapa de Deployment usada por diagnosticos de conversao de UOM em
         * planos de distribuicao persistidos.
         */
        DEPLOY("Deployment");
        
        private String nomeExibicao;
        
        // construtor obrigatório para permitir nome exibição
        TaskTipo(String nomeExibicao) {
            this.nomeExibicao = nomeExibicao;
        } 
        
        // Optionally and/or additionally, toString.
        @Override 
        public String toString() { return nomeExibicao; }
    }

    public enum TaskStatus {
        WAITING, EXECUTING, EXECUTED, ERROR
    }

    // CONFIGURAÇÕES DAS REGRAS DE ALOCAÇÃO DE PRODUTOS EM CLUSTERS -----------------------------------
    public enum RegraAlocacaoClusterProdutosTipo {
        @JsonProperty("Characteristic") CARACTERISTICA,
        @JsonProperty("Status") STATUS_PRODUTO
    }

    public enum RegraAlocacaoClusterLocationsTipo {
        @JsonProperty("Location Type") TIPO_LOCATION,
        @JsonProperty("Characteristic") CARACTERISTICA,
        @JsonProperty("Country / State") PAIS_ESTADO,
    }
    
    public enum AndOr {
        AND, OR 
    }

    public enum TipoCluster {
        DP
    }

    // VALORES PADRÃO PARA CONFIGURAÇÃO CLUSTERS
    public final static int CLUSTERING_PADRAO_NUMERO_DIAS_HISTORICOS = 120;
    
    /**
     * Modos historicos de execucao de tasks.
     *
     * <p>No Community, apenas {@link #SYNC} e executavel. {@link #ASYNC} e
     * {@link #BATCH} permanecem no enum para desserializar payloads legados ou
     * compartilhados e falhar explicitamente com RequiresEnterpriseVersionException
     * nas bordas web/scheduler. O Enterprise deve reativar async/batch por beans
     * privados de mensageria, nao por flag de edicao.</p>
     */
    public enum ModoExecucaoProcesso {
        SYNC, 
        ASYNC, 
        BATCH
    }

}


