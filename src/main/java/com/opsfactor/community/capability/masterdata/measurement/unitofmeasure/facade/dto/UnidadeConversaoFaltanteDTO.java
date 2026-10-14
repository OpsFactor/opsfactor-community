package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/** Five-column conversion report shared by the API, screen and spreadsheet export. */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
public class UnidadeConversaoFaltanteDTO {

    public String materialId;
    public String originUnitOfMeasure;
    public String targetUnitOfMeasure;
    /** The business data that requires this conversion, in English. */
    public String reason;
    /** Record identifiers and locations needed to correct the source data. */
    public String additionalInformation;

    /** Joins record identifiers without exposing solver internals or repeated period rows. */
    public static String additionalInformation(String... contexts) {

        return Arrays.stream(contexts).filter(Objects::nonNull)
                .filter(context -> !context.isBlank()).distinct().collect(Collectors.joining("; "));

    }

    /** Maps the diagnostic source selected by the calculation to an understandable English need. */
    public static String economicReason(String family, String source) {

        if (source != null && source.startsWith("Bill of Materials output ")) return "Bill of Materials output for manufacturing cost estimation";
        if (source != null && source.startsWith("Bill of Materials component ")) return "Bill of Materials component for manufacturing cost estimation";
        if (source != null && source.startsWith("Componente da lista técnica ")) return "Bill of Materials composition for manufacturing cost estimation";
        if (source != null && source.startsWith("Composição de lista técnica ")) return "Bill of Materials output for manufacturing cost estimation";
        if (source != null && source.startsWith("Tempo operacional do roteiro ")) return "Production routing base quantity for operation time and cost";
        if (source != null && source.startsWith("Roteiro ")) return "Production routing unit cost";
        if ("Custo unitário máximo global".equals(source)) return "Manufacturing cost estimation normalization";
        if ("Fallback de COGS pelo valor unitário do perfil".equals(source)) return "Manufacturing cost estimation from the planning profile unit value";
        if (family == null) return "Cost calculation";
        family = family.replace(" (condicional à etapa de cálculo)", "");
        return switch (family) {
            case "Estimativa COGS" -> "Manufacturing cost estimation";
            case "Armazenagem" -> "Storage cost";
            case "Inbound" -> "Inbound handling cost";
            case "Outbound" -> "Outbound handling cost";
            case "Frete simplificado por material", "Frete simplificado", "Tabela de frete", "Curva de frete", "Frete" -> "Transportation cost";
            case "Tributos de transferência por material", "Tributos de transferência" -> "Transfer tax cost";
            case "Custo de produção", "Run cost" -> "Production cost";
            case "Preço de fornecedor" -> "Supplier purchase price";
            case "Incentivo de fluxo firme" -> "Firm transfer order valuation";
            case "Incentivo de produção firme simples", "Incentivo de produção firme múltipla" -> "Firm production order valuation";
            case "Preparo da demanda direta" -> "Direct demand preparation";
            case "Preparo de estoque inicial", "Preparo de séries e estoque inicial" -> "Opening stock preparation";
            case "Preparo de preços/faturamento", "Preparo de preços e faturamento" -> "Price and sales data preparation";
            case "Preparo de custos" -> "Cost data preparation";
            case "Preparo de fluxos firmes" -> "Firm transfer order preparation";
            case "Preparo de produção firme", "Preparo de ordens firmes" -> "Firm order preparation";
            case "Função objetivo" -> "Planning objective valuation";
            case "Penalização de demanda não atendida", "Demanda não atendida" -> "Unmet demand penalty";
            case "Demanda considerada", "Mérito da demanda considerada" -> "Direct demand valuation";
            case "Estoque" -> "Stock data preparation";
            case "Working capital por aging" -> "Inventory aging valuation";
            case "Fair share safety stock" -> "Safety stock allocation valuation";
            case "Safety stock" -> "Safety stock valuation";
            case "Writeoff" -> "Inventory writeoff valuation";
            case "Working capital" -> "Working capital valuation";
            default -> family.startsWith("Preço/") ? "Material price or cost valuation" : "Cost calculation";
        };

    }

    /** Converts only known diagnostic source labels; record IDs remain unchanged. */
    public static String economicInformation(String source, String originLocation, String targetLocation) {

        String record = source;
        if (source != null) {
            record = switch (source) {
                case "Custo unitário máximo global" -> "Global planning unit cost";
                case "Fallback de COGS pelo valor unitário do perfil" -> "Planning profile unit value";
                case "CustoLocation" -> "Location cost data";
                case "CustoLinhaTransporteProduto" -> "Transportation lane material cost data";
                case "CustoLinhaTransporte" -> "Transportation lane cost data";
                case "CurvasCustoLogistico" -> "Logistics cost curve";
                case "Valor padrão material" -> "Material default value";
                case "Valor padrão material/location" -> "Material/location default value";
                case "Lista vigente ou faturamento" -> "Current price/cost list or sales data";
                case "Tabela de frete" -> "Freight price table";
                case "Preparo de preços/faturamento", "Preparo de preços e faturamento" -> "Price and sales data";
                case "Preparo de custos" -> "Cost data";
                case "Preparo de fluxos firmes" -> "Firm transfer order data";
                case "Preparo de séries e estoque inicial" -> "Opening stock data";
                case "Demanda considerada", "Mérito da demanda considerada" -> "Direct demand data";
                case "Demanda não atendida" -> "Unmet demand data";
                case "Estoque" -> "Stock data";
                case "Working capital por aging" -> "Inventory aging data";
                case "Safety stock condicional" -> "Safety stock data (conditional on the planning stage)";
                case "Valor unitário do perfil por quantidade", "Parcela unitária do perfil" -> "Planning profile unit value";
                case "Valor econômico obrigatório" -> "Required material price or cost";
                case "COGS/NET/GROSS positivos" -> "Positive material cost or sales value required";
                case "Conversão da demanda considerada" -> "Direct demand data";
                case "Seleção canônica de consumidores econômicos", "Consumo econômico da série selecionada" -> "Selected material data";
                case "Linha/pacote de produção firme", "Ordens de produção", "Produção firme" -> "Firm production orders";
                case "Ordens de transferência", "Fluxos firmes" -> "Firm transfer orders";
                case "Preparo da demanda direta" -> "Direct demand data";
                case "Preparo de estoque inicial" -> "Opening stock data";
                case "Preparo de produção firme", "Preparo de ordens firmes" -> "Firm order data";
                default -> source;
            };
            for (String[] prefix : new String[][] {
                    {"Componente da lista técnica ", "Bill of Materials "},
                    {"Composição de lista técnica ", "Bill of Materials "},
                    {"Tempo operacional do roteiro ", "Routing "}, {"Roteiro ", "Routing "},
                    {"Frota ", "Fleet "}, {"Lista de preços ", "Price List "}, {"Lista de custos ", "Cost List "}}) {
                if (source.startsWith(prefix[0])) { record = prefix[1] + source.substring(prefix[0].length()); break; }
            }
        }
        return additionalInformation(record,
                originLocation == null ? null : "Location " + originLocation,
                targetLocation == null || targetLocation.equals(originLocation) ? null : "Target location " + targetLocation);

    }

    /** Internal classifications use English labels when constructing the public reason. */
    public enum NecessidadeConversao {
        ROTEIRO_QUANTIDADE_BASE("Production Routing Base-Quantity UOM"),
        LISTA_TECNICA_OUTPUT("Bill of Materials output UOM"),
        LISTA_TECNICA_COMPONENTE("Bill of Materials component UOM"),
        PADRAO_MATERIAL_LOCATION("Material/Location Default UOM"),
        PADRAO_MATERIAL("Material Default UOM"),
        REQUISICAO_OUTBOUND("Outbound requisition"),
        MINIMO_MULTIPLO_TRANSFERENCIA("Transfer lot size / multiple"),
        PLANO_DEMANDA("Demand Plan UOM"),
        ESTOQUE("Stock UOM"),
        VENDAS("Sales UOM"),
        ESTIMATIVA_COGS_FALLBACK("Estimated COGS fallback UOM"),
        ESTIMATIVA_COGS_NORMALIZACAO_GLOBAL("Estimated COGS global normalization UOM"),
        ESTIMATIVA_COGS_AGREGACAO_VAZIA("Estimated COGS empty aggregation"),
        CUSTO_ECONOMICO("Economic cost UOM"),
        PREPARO_DEMANDA_DIRETA("Direct demand preparation UOM"),
        FUNCAO_OBJETIVO_VALOR("Optimization objective value UOM"),
        PLANNING_BOOK_OU_RELATORIO("Planning Book / Report UOM"),
        EXPEDICAO("Expedition UOM"),
        TRANSFERENCIA("Transfer UOM"),
        LISTA_PRECOS("Price List UOM"),
        LISTA_CUSTOS("Cost List UOM"),
        CARTEIRA_PEDIDOS("Sell-out Order UOM"),
        PEDIDOS_TRANSFERENCIA("Sell-in / Transfer / Purchase Order UOM"),
        ROTEIRO_OPERACAO("Production Routing Operation UOM");

        private final String description;

        NecessidadeConversao(String description) {

            this.description = description;

        }

        public String getDescription() {

            return description;

        }

    }

}
