package com.opsfactor.community.capability.planningbook.keyfigure.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.utility.Constantes.TipoDemanda;

/**
 * Catalogo tecnico de Key Figures padrao conhecidas pelo backend.
 *
 * <p>Nem todo valor deste enum e selecionavel no OpsFactor Community. Alguns
 * valores Enterprise permanecem aqui para decodificar payloads do front
 * compartilhado e permitir bloqueios claros antes de chegar em projections ou
 * entidades fisicas.</p>
 */
public enum KeyFigureStandardEnum {
    
    // KFs Demand Plan
    @JsonProperty("Baseline") BASELINE,
    /*
     * New Materials e Uplift sao KFs Enterprise. O JsonProperty continua com o
     * rotulo legado New Products nesta fase para que o backend Community
     * consiga decodificar payloads antigos e lançar
     * RequiresEnterpriseVersionException com mensagem clara. A presenca aqui
     * nao autoriza exibicao ou edicao no Planning Book Community.
     */
    @JsonProperty("New Products") ITENS_NOVOS,
    @JsonProperty("Uplift") UPLIFT,
    @JsonProperty("Demand Adjustment") AJUSTE_DEMANDA,
    @JsonProperty("Direct Demand") DEMANDA_DIRETA_TOTAL_DP,
    @JsonProperty("Direct Demand / Working Day") DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL,
    @JsonProperty("Direct Demand") DEMANDA_DIRETA_TOTAL_SNP,
    @JsonProperty("Direct Demand - Demand Plan") DEMANDA_DIRETA_PLANO_DEMANDA_SNP,
    /*
     * KFs de Sales Orders/Client Orders sao Enterprise. Permanecem no enum
     * compartilhado para decodificar payloads do front e permitir bloqueio claro
     * na factory/validador Community.
     */
    @JsonProperty("Direct Demand - Sales Orders") DEMANDA_DIRETA_CARTEIRA_SNP,
    @JsonProperty("Comparison Plan") DEMANDA_DIRETA_TOTAL_COMPARACAO,
    @JsonProperty("Client Orders") CARTEIRA,
    @JsonProperty("Historical Sales") HISTORICO_VENDAS,
    /*
     * Gross/Net Sales sao identidades compartilhadas para que configuracoes
     * Enterprise possam ser serializadas e rejeitadas claramente no
     * Community. A presenca no enum nao as torna uma capability Community.
     */
    @JsonProperty("Gross Sales") VENDAS_GROSS,
    @JsonProperty("Net Sales") VENDAS_NET,
    @JsonProperty("Gross Average Price") PRECO_MEDIO_GROSS,
    @JsonProperty("Net Average Price") PRECO_MEDIO_NET,
    // KFs Supply Plan
    @JsonProperty("Total Demand") DEMANDA_TOTAL,
    @JsonProperty("Indirect Demand") DEMANDA_INDIRETA_TOTAL,
    @JsonProperty("Stock") ESTOQUE,
    @JsonProperty("Stock in Days") ESTOQUE_DIAS,
    @JsonProperty("Safety Stock") ESTOQUE_SEGURANCA,
    @JsonProperty("Writeoff") WRITEOFF,
    @JsonProperty("Planned Inbound") INBOUND_PLANEJADO,
    @JsonProperty("Planned Production") PRODUCAO_PLANEJADA,
    @JsonProperty("Inbound Orders") INBOUND_FIRME,
    @JsonProperty("In Transit Inbound") INBOUND_ESTOQUE_EM_TRANSITO,
    @JsonProperty("Production Orders") PRODUCAO_FIRME,
    // KFs de fluxo planejado
    @JsonProperty("Planned Outbound") OUTBOUND_PLANEJADO;

    public enum TipoPlanoKeyFigure {
        DEMAND_PLAN, SUPPLY_PLAN
    }

    /** Indica KFs cujo pai precisa consolidar numerador e denominador. */
    public KeyFigureInterface.ModeloAgregacaoKeyFigure getModeloAgregacaoKeyFigure() {

        return switch (this) {
            case DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL ->
                    KeyFigureInterface.ModeloAgregacaoKeyFigure.RELACAO_ENTRE_VALORES;
            case PRECO_MEDIO_GROSS, PRECO_MEDIO_NET ->
                    KeyFigureInterface.ModeloAgregacaoKeyFigure.RAZAO_ENTRE_SOMAS;
            case ESTOQUE_DIAS -> KeyFigureInterface.ModeloAgregacaoKeyFigure.COBERTURA_ESTOQUE;
            default -> KeyFigureInterface.ModeloAgregacaoKeyFigure.PADRAO;
        };

    }
    
    public TipoPlanoKeyFigure getTipoPlanoKeyFigure() {
        switch (this) {
            case BASELINE : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case ITENS_NOVOS : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case UPLIFT : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case AJUSTE_DEMANDA : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case DEMANDA_DIRETA_TOTAL_DP : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case DEMANDA_DIRETA_TOTAL_COMPARACAO : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case CARTEIRA : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case DEMANDA_DIRETA_TOTAL_SNP : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case DEMANDA_DIRETA_PLANO_DEMANDA_SNP : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case DEMANDA_DIRETA_CARTEIRA_SNP : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case HISTORICO_VENDAS : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case VENDAS_GROSS : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case VENDAS_NET : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case PRECO_MEDIO_GROSS : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case PRECO_MEDIO_NET : return TipoPlanoKeyFigure.DEMAND_PLAN;
            case DEMANDA_TOTAL : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case DEMANDA_INDIRETA_TOTAL : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case ESTOQUE : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case ESTOQUE_DIAS : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case ESTOQUE_SEGURANCA : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case WRITEOFF : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case INBOUND_PLANEJADO : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case INBOUND_FIRME : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case INBOUND_ESTOQUE_EM_TRANSITO : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case PRODUCAO_PLANEJADA : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case PRODUCAO_FIRME : return TipoPlanoKeyFigure.SUPPLY_PLAN;
            case OUTBOUND_PLANEJADO : return TipoPlanoKeyFigure.SUPPLY_PLAN;
        }
        throw new EnumConstantNotPresentException(KeyFigureStandardEnum.class, this.name());
    }
    
    public TipoDemanda getTipoDemanda() {
        switch (this) {
            case DEMANDA_DIRETA_TOTAL_DP : return TipoDemanda.TOTAL; 
            case DEMANDA_DIRETA_TOTAL_SNP : return TipoDemanda.TOTAL; 
            case BASELINE : return TipoDemanda.BASELINE;
            case ITENS_NOVOS : return TipoDemanda.ITENS_NOVOS;
            case UPLIFT : return TipoDemanda.UPLIFT;
            case AJUSTE_DEMANDA : return TipoDemanda.AJUSTE_DEMANDA;
            default : throw new EnumConstantNotPresentException(TipoDemanda.class, this + " não possui equivalente em TipoDemanda");
        }
    }
    
    public EditMode getEditMode() {
        switch (this) {
            case DEMANDA_DIRETA_TOTAL_DP : return EditMode.CELLEDIT; // DP : cascateia ajuste no total para uma das linhas dependentes
            case DEMANDA_DIRETA_TOTAL_SNP : return EditMode.NOEDIT; // SNP : não se ajusta a demanda direta
            case DEMANDA_DIRETA_PLANO_DEMANDA_SNP : return EditMode.NOEDIT;
            case DEMANDA_DIRETA_CARTEIRA_SNP : return EditMode.NOEDIT;
            case DEMANDA_TOTAL : return EditMode.NOEDIT;
            case DEMANDA_DIRETA_TOTAL_COMPARACAO : return EditMode.NOEDIT;
            case DEMANDA_INDIRETA_TOTAL : return EditMode.DETAIL_AGGREGATED_DISAGGREGATED; // SNP
            case BASELINE : return EditMode.NOEDIT; // DP
            case ITENS_NOVOS : return EditMode.CELLEDIT; // DP Enterprise; bloqueado antes da UI/API Community
            case UPLIFT : return EditMode.CELLEDIT; // DP Enterprise; bloqueado antes da UI/API Community
            case AJUSTE_DEMANDA : return EditMode.CELLEDIT; // DP
            case CARTEIRA : return EditMode.NOEDIT; // DP Enterprise; bloqueado antes da UI/API Community
            case DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL : return EditMode.CELLEDIT;
            case HISTORICO_VENDAS : return EditMode.NOEDIT; // DP
            case VENDAS_GROSS : return EditMode.CELLEDIT; // DP Enterprise; bloqueado antes da UI/API Community
            case VENDAS_NET : return EditMode.CELLEDIT; // DP Enterprise; bloqueado antes da UI/API Community
            case PRECO_MEDIO_GROSS : return EditMode.NOEDIT; // DP Enterprise; derivado de Gross Sales / Direct Demand
            case PRECO_MEDIO_NET : return EditMode.NOEDIT; // DP Enterprise; derivado de Net Sales / Direct Demand
            case ESTOQUE : return EditMode.CELLEDIT; // SNP
            case ESTOQUE_DIAS : return EditMode.NOEDIT; // SNP
            case ESTOQUE_SEGURANCA : return EditMode.NOEDIT; // SNP
            case WRITEOFF : return EditMode.NOEDIT; // SNP
            case INBOUND_PLANEJADO : return EditMode.DETAIL_OR_CELL_EDIT; // SNP
            case INBOUND_FIRME : return EditMode.NOEDIT; // SNP
            case INBOUND_ESTOQUE_EM_TRANSITO : return EditMode.NOEDIT; // SNP Enterprise; bloqueado antes da UI/API Community
            case PRODUCAO_PLANEJADA : return EditMode.DETAIL_OR_CELL_EDIT; // SNP
            case PRODUCAO_FIRME : return EditMode.NOEDIT; // SNP
            case OUTBOUND_PLANEJADO : return EditMode.NOEDIT; // SNP Enterprise; bloqueado antes da UI/API Community
        }
        throw new EnumConstantNotPresentException(KeyFigureStandardEnum.class, this.name());
    }

}
