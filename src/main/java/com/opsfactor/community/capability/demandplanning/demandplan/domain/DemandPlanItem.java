package com.opsfactor.community.capability.demandplanning.demandplan.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.TipoDemanda;
import com.opsfactor.community.platform.utility.Constantes.TipoPlano;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Linha material/location/periodo de uma versao de Demand Plan Community.
 *
 * <p>A entidade representa as quantidades fisicas persistidas no planejamento
 * colaborativo. O contrato funcional Community edita e totaliza Baseline e
 * Demand Adjustment; campos transicionais como Uplift e New Materials existem
 * apenas para compatibilidade de schema e devem ser neutralizados quando uma
 * escrita agregada Community recalcula a linha.</p>
 */
@EqualsAndHashCode(of="key")
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "demand_plan_item")
public class DemandPlanItem {

    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private DemandPlanItemKey key;

    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @Getter 
    @Setter
    @EqualsAndHashCode
    public static class DemandPlanItemKey implements Serializable {
        
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private DemandPlan demandPlan;
        
        @ManyToOne(optional = false, fetch = FetchType.LAZY) // lazy para evitar N+1 onde produto busca precoCogsPadrao
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Location location;
        
        @ManyToOne(optional = false, fetch = FetchType.LAZY) // lazy para evitar N+1 onde produto busca precoCogsPadrao
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Produto produto;
        /**
         * A data de referência indica qual o período para o qual se está estimando a venda.
         * Pode representar uma data / semana / mês
         */
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private LocalDateTime dataReferencia;
        
    }
    
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedida;
    
    // Demand plan - quantidades
    private Double quantidadeBaseline;
    private Double quantidadeUplift;
    private Double quantidadeItensNovos;
    private Double quantidadeAjusteDemanda;
    
    // Demand plan - quantidades efetivamente atendidas (calculado após execução do DRP + restrição do plano)
    @Deprecated private Double quantidadeBaselineAtendida;
    @Deprecated private Double quantidadeUpliftAtendida;
    @Deprecated private Double quantidadeItensNovosAtendida;
    @Deprecated private Double quantidadeAjusteDemandaAtendida;
    
    // gerado apenas por modelos que fazem decomposição em seazonal-trend
    private Double quantidadeBaselineTrend;
    private Double quantidadeBaselineSeasonal;
    
    // Importante : os valores em R$ deverão ser obtidos através do cruzamento com o demand plan com o pricing plan

    public Location getLocation() {
        return getKey().getLocation();
    }
    
    public Produto getProduto(){
        return key.getProduto();
    }
   
    public UnidadeMedida getUnidadeMedida(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedida == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedida;
    }
    
    public DemandPlan getDemandPlan() {
        return key.getDemandPlan();
    }

    public LocalDateTime getDataReferencia(){
        return key.getDataReferencia();
    }
    
    public void setLocation(Location location){
        if(key == null) key = new DemandPlanItemKey();
        key.setLocation(location);
    }
    
    public void setProduto(Produto produto) {
        if (key == null) key = new DemandPlanItemKey();
        key.setProduto(produto);
    }
    
    public void setDataReferencia(LocalDateTime dataReferencia) {
        if (key == null) key = new DemandPlanItemKey();
        key.setDataReferencia(dataReferencia);
    }

    public void setDemandPlan(DemandPlan demandPlan) {
        if (key == null) key = new DemandPlanItemKey();
        key.setDemandPlan(demandPlan);
    }
    
    public double getQuantidadeBaselineAtendida() {
        return getQuantidadeDemandPlanFinitaOuZero(
                quantidadeBaselineAtendida,
                "restricted baseline");
    }
    
    public double getQuantidadeUpliftAtendida() {
        return getQuantidadeDemandPlanFinitaOuZero(
                quantidadeUpliftAtendida,
                "restricted uplift");
    }
    
    public double getQuantidadeItensNovosAtendida() {
        return getQuantidadeDemandPlanFinitaOuZero(
                quantidadeItensNovosAtendida,
                "restricted new materials");
    }
    
    public double getQuantidadeAjusteDemandaAtendida() {
        return getQuantidadeDemandPlanFinitaOuZero(
                quantidadeAjusteDemandaAtendida,
                "restricted demand adjustment");
    }
    
    public double getQuantidadeTotalAtendida() {
        return getQuantidadeBaselineAtendida() + getQuantidadeItensNovosAtendida()
                + getQuantidadeUpliftAtendida() + getQuantidadeAjusteDemandaAtendida();
    }
    
    public double getQuantidadeBaseline() {
        return getQuantidadeDemandPlanFinitaOuZero(
                quantidadeBaseline,
                "unrestricted baseline");
    }
    
    public double getQuantidadeUplift() {
        return getQuantidadeDemandPlanFinitaOuZero(
                quantidadeUplift,
                "unrestricted uplift");
    }
    
    public double getQuantidadeItensNovos() {
        return getQuantidadeDemandPlanFinitaOuZero(
                quantidadeItensNovos,
                "unrestricted new materials");
    }
    
    public double getQuantidadeAjusteDemanda() {
        return getQuantidadeDemandPlanFinitaOuZero(
                quantidadeAjusteDemanda,
                "unrestricted demand adjustment");
    }

    /**
     * Valida que componentes numericos de Demand Plan sejam finitos antes de
     * totals, conversoes de unidade ou materializacao para Planning Book.
     *
     * <p>O contrato generico desta entidade nao decide sinal: Demand Adjustment
     * pode ser negativo e outros componentes terao validacoes funcionais
     * especificas em recortes separados. Aqui bloqueamos apenas `NaN` e
     * infinito, que nao representam quantidade persistivel nem serializavel de
     * maneira consistente.</p>
     */
    private double getQuantidadeDemandPlanFinitaOuZero(
            Double quantidadeDemandPlan,
            String nomeCampoQuantidade) {

        if (quantidadeDemandPlan == null) {
            return 0.0d;
        }

        if (!Double.isFinite(quantidadeDemandPlan)) {
            throw new IllegalStateException(
                    "Demand Plan quantity "
                            + nomeCampoQuantidade
                            + " must be finite for "
                            + getContextoLinhaParaMensagem()
                            + ": "
                            + quantidadeDemandPlan
                            + ".");
        }
        return quantidadeDemandPlan;

    }

    private String getContextoLinhaParaMensagem() {

        return "material "
                + getProdutoIdParaMensagem()
                + " / location "
                + getLocationIdParaMensagem()
                + " / reference date "
                + getDataReferenciaParaMensagem();

    }

    private String getProdutoIdParaMensagem() {

        Produto produto = key == null
                ? null
                : key.getProduto();
        return produto == null ? "<sem-material>" : String.valueOf(produto.getId());

    }

    private String getLocationIdParaMensagem() {

        Location location = key == null
                ? null
                : key.getLocation();
        return location == null ? "<sem-location>" : String.valueOf(location.getId());

    }

    private String getDataReferenciaParaMensagem() {

        LocalDateTime dataReferencia = key == null
                ? null
                : key.getDataReferencia();
        return dataReferencia == null ? "<sem-data>" : String.valueOf(dataReferencia);

    }

    public double getQuantidadeTotal() {
        return getQuantidadeBaseline() + getQuantidadeUplift() + getQuantidadeItensNovos() + getQuantidadeAjusteDemanda();
    }
    
    public double getQuantidadeAtendidaTotal() {
        return getQuantidadeBaselineAtendida() + getQuantidadeUpliftAtendida() 
                + getQuantidadeItensNovosAtendida() + getQuantidadeAjusteDemandaAtendida();
    }
    
    /**
     * Atualiza o total colaborativo da linha dentro do contrato Community.
     *
     * <p>O Community nao possui Uplift nem New Materials como linhas editaveis.
     * Por isso uma escrita agregada sempre zera esses campos transicionais e
     * redistribui o valor somente entre Baseline e Demand Adjustment.</p>
     */
    @Deprecated // TipoPlano não está mais sendo usado no DP
    public void setQuantidadeTotal(double valor, Constantes.TipoPlano tipoPlano, Constantes.ModificacaoAgregadaPlano modificacaoAgregadaPlano) {

        double quantidadeTotalAtual;
        double quantidadeAjusteDemanda;
        
        switch (tipoPlano) {
            case PLANO_RESTRITO:
                quantidadeTotalAtual = getQuantidadeTotalCommunity(tipoPlano);
                quantidadeAjusteDemanda = getQuantidadeAjusteDemandaAtendida();
                break;
            case PLANO_IRRESTRITO:
                quantidadeTotalAtual = getQuantidadeTotalCommunity(tipoPlano);
                quantidadeAjusteDemanda = getQuantidadeAjusteDemanda();
                break;
            default:
                throw unsupportedTipoPlano("setQuantidadeTotal", tipoPlano);
        }
        
        switch (modificacaoAgregadaPlano) {
            case LINHA_AJUSTE:
                setQuantidade(Math.max(0, valor) - (quantidadeTotalAtual - quantidadeAjusteDemanda), TipoDemanda.AJUSTE_DEMANDA, tipoPlano);
                neutralizaKeyFiguresEnterpriseTransicionaisCommunity(tipoPlano);
                return;
            case PROPORCIONAL_OU_BASELINE:
                // valor a ser alocado mas não há nenhuma linha disponível : aloca tudo em baseline
                if (valor > 0.00001 && quantidadeTotalAtual < 0.00001) {
                    setQuantidade(valor, Constantes.TipoDemanda.BASELINE, tipoPlano);
                    setQuantidade(0, Constantes.TipoDemanda.AJUSTE_DEMANDA, tipoPlano);
                } else if (valor <= 0.00001) {
                    setQuantidade(0, Constantes.TipoDemanda.BASELINE, tipoPlano);
                    setQuantidade(0, Constantes.TipoDemanda.AJUSTE_DEMANDA, tipoPlano);
                } else {
                    setQuantidade(valor * getQuantidade(Constantes.TipoDemanda.BASELINE, tipoPlano) / quantidadeTotalAtual,
                            Constantes.TipoDemanda.BASELINE, tipoPlano);
                    setQuantidade(valor * getQuantidade(Constantes.TipoDemanda.AJUSTE_DEMANDA, tipoPlano) / quantidadeTotalAtual,
                            Constantes.TipoDemanda.AJUSTE_DEMANDA, tipoPlano);
                }
                neutralizaKeyFiguresEnterpriseTransicionaisCommunity(tipoPlano);
                break;
            default:
                throw unsupportedModificacaoAgregadaPlano(modificacaoAgregadaPlano);
            
        }
        
    }

    @Deprecated // TipoPlano não está mais sendo usado no DP
    private void setQuantidade(double valor, Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                switch (tipoDemanda) {
                    case BASELINE:
                        setQuantidadeBaseline(valor);
                        return;
                    case ITENS_NOVOS:
                        setQuantidadeItensNovos(valor);
                        return;
                    case UPLIFT:
                        setQuantidadeUplift(valor);
                        return;
                    case AJUSTE_DEMANDA:
                        setQuantidadeAjusteDemanda(valor);
                        return;
                    default:
                        throw unsupportedTipoDemanda("setQuantidade", tipoDemanda);
                }
            case PLANO_RESTRITO:
                switch (tipoDemanda) {
                    case BASELINE:
                        setQuantidadeBaselineAtendida(valor);
                        return;
                    case ITENS_NOVOS:
                        setQuantidadeItensNovosAtendida(valor);
                        return;
                    case UPLIFT:
                        setQuantidadeUpliftAtendida(valor);
                        return;
                    case AJUSTE_DEMANDA:
                        setQuantidadeAjusteDemandaAtendida(valor);
                        return;
                    default:
                        throw unsupportedTipoDemanda("setQuantidade", tipoDemanda);
                }
            default:
                throw unsupportedTipoPlano("setQuantidade", tipoPlano);
        }
    }

    @Deprecated
    public double getQuantidade(TipoDemanda tipoDemanda, TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                switch (tipoDemanda) {
                    case BASELINE:
                        return getQuantidadeBaseline();
                    case ITENS_NOVOS:
                        return getQuantidadeItensNovos();
                    case UPLIFT:
                        return getQuantidadeUplift();
                    case AJUSTE_DEMANDA:
                        return getQuantidadeAjusteDemanda();
                    case TOTAL:
                        return getQuantidadeTotal();
                    default:
                        throw unsupportedTipoDemanda("getQuantidade", tipoDemanda);
                }
            case PLANO_RESTRITO:
                switch (tipoDemanda) {
                    case BASELINE:
                        return getQuantidadeBaselineAtendida();
                    case ITENS_NOVOS:
                        return getQuantidadeItensNovosAtendida();
                    case UPLIFT:
                        return getQuantidadeUpliftAtendida();
                    case AJUSTE_DEMANDA:
                        return getQuantidadeAjusteDemandaAtendida();
                    case TOTAL:
                        return getQuantidadeTotalAtendida();
                    default:
                        throw unsupportedTipoDemanda("getQuantidade", tipoDemanda);
                }
            default:
                throw unsupportedTipoPlano("getQuantidade", tipoPlano);
        }
    }

    public double getQuantidade(TipoDemanda tipoDemanda) {
        switch (tipoDemanda) {
            case BASELINE:
                return getQuantidadeBaseline();
            case ITENS_NOVOS:
                return getQuantidadeItensNovos();
            case UPLIFT:
                return getQuantidadeUplift();
            case AJUSTE_DEMANDA:
                return getQuantidadeAjusteDemanda();
            case TOTAL:
                return getQuantidadeTotal();
            default:
                throw unsupportedTipoDemanda("getQuantidade", tipoDemanda);
        }
    }

    @Deprecated
    public double getQuantidade(KeyFigureStandard keyFigureStandardDemandPlanning, TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                switch (keyFigureStandardDemandPlanning.getKeyFigureStandardEnum()) {
                    case BASELINE:
                        return getQuantidadeBaseline();
                    case ITENS_NOVOS:
                        return getQuantidadeItensNovos();
                    case UPLIFT:
                        return getQuantidadeUplift();
                    case AJUSTE_DEMANDA:
                        return getQuantidadeAjusteDemanda();
                    default:
                        throw unsupportedKeyFigureStandard("getQuantidade", keyFigureStandardDemandPlanning);
                }
            case PLANO_RESTRITO:
                switch (keyFigureStandardDemandPlanning.getKeyFigureStandardEnum()) {
                    case BASELINE:
                        return getQuantidadeBaselineAtendida();
                    case ITENS_NOVOS:
                        return getQuantidadeItensNovosAtendida();
                    case UPLIFT:
                        return getQuantidadeUpliftAtendida();
                    case AJUSTE_DEMANDA:
                        return getQuantidadeAjusteDemandaAtendida();
                    default:
                        throw unsupportedKeyFigureStandard("getQuantidade", keyFigureStandardDemandPlanning);
                }
            default:
                throw unsupportedTipoPlano("getQuantidade", tipoPlano);
        }
    }

    public double getQuantidade(KeyFigureStandard keyFigureStandardDemandPlanning) {
        switch (keyFigureStandardDemandPlanning.getKeyFigureStandardEnum()) {
            case BASELINE:
                return getQuantidadeBaseline();
            case ITENS_NOVOS:
                return getQuantidadeItensNovos();
            case UPLIFT:
                return getQuantidadeUplift();
            case AJUSTE_DEMANDA:
                return getQuantidadeAjusteDemanda();
            default:
                throw unsupportedKeyFigureStandard("getQuantidade", keyFigureStandardDemandPlanning);
        }
    }

    @Deprecated
    public double getQuantidadeNaUnidadeMedidaTarget(
            TipoDemanda tipoDemanda, TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget, UnidadeMedidaProjection unidadeMedidaProjection) {
        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        return getQuantidade(tipoDemanda, tipoPlano) * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(), getUnidadeMedida(parametrosGlobais), unidadeMedidaTarget);
    }

    public double getQuantidadeNaUnidadeMedidaTarget(
            TipoDemanda tipoDemanda,
            UnidadeMedida unidadeMedidaTarget, UnidadeMedidaProjection unidadeMedidaProjection) {
        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        return getQuantidade(tipoDemanda) * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(), getUnidadeMedida(parametrosGlobais), unidadeMedidaTarget);
    }

    public double getQuantidadeNaUnidadeMedidaTarget(
            KeyFigureStandard keyFigureStandardDemandPlanning,
            UnidadeMedida unidadeMedidaTarget, UnidadeMedidaProjection unidadeMedidaProjection) {
        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        return getQuantidade(keyFigureStandardDemandPlanning) * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(), getUnidadeMedida(parametrosGlobais), unidadeMedidaTarget);
    }


    public double getQuantidadeBaselineSeasonal() {
        return getQuantidadeDemandPlanFinitaOuZero(
                quantidadeBaselineSeasonal,
                "baseline seasonal component");
    }
    public double getQuantidadeBaselineTrend() {
        return getQuantidadeDemandPlanFinitaOuZero(
                quantidadeBaselineTrend,
                "baseline trend component");
    }
    
    public double getQuantidadeTrendSeason(
            int posicaoPeriodo, Location location, Produto material, 
            Constantes.TrendSeasonal trendSeasonal,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        double quantidadeRetorno = 0;
        if (trendSeasonal == null) {
            throw unsupportedTrendSeasonal(null);
        }
        switch (trendSeasonal) {
            case TREND:
                quantidadeRetorno = getQuantidadeBaselineTrend();
                break;
            case SEASONAL:
                quantidadeRetorno = getQuantidadeBaselineSeasonal();
                break;
            case TREND_E_SEASONAL:
                quantidadeRetorno = getQuantidadeBaselineTrend() + getQuantidadeBaselineSeasonal();
                break;
            default:
                throw unsupportedTrendSeasonal(trendSeasonal);
        }

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();

        // converte quantidade no demand plan linha para a unidade de referência do projection                
        return quantidadeRetorno * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                material, 
                getUnidadeMedida(parametrosGlobais), 
                unidadeMedidaTarget);
        
    }

    /**
     * Retorna o total funcional Community usado em escritas agregadas.
     *
     * <p>Este total exclui Uplift e New Materials porque esses componentes sao
     * Enterprise. Os getters fisicos permanecem disponiveis para leituras
     * tecnicas transicionais e overlays privados.</p>
     */
    private double getQuantidadeTotalCommunity(Constantes.TipoPlano tipoPlano) {

        return switch (tipoPlano) {
            case PLANO_IRRESTRITO -> getQuantidadeBaseline() + getQuantidadeAjusteDemanda();
            case PLANO_RESTRITO -> getQuantidadeBaselineAtendida() + getQuantidadeAjusteDemandaAtendida();
            default -> throw unsupportedTipoPlano("getQuantidadeTotalCommunity", tipoPlano);
        };

    }

    /**
     * Zera campos Enterprise transicionais depois de uma escrita agregada
     * Community para impedir que valores antigos sejam reintroduzidos no total.
     */
    private void neutralizaKeyFiguresEnterpriseTransicionaisCommunity(Constantes.TipoPlano tipoPlano) {

        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                setQuantidadeItensNovos(0.0);
                setQuantidadeUplift(0.0);
                return;
            case PLANO_RESTRITO:
                setQuantidadeItensNovosAtendida(0.0);
                setQuantidadeUpliftAtendida(0.0);
                return;
            default:
                throw unsupportedTipoPlano("neutralizaKeyFiguresEnterpriseTransicionaisCommunity", tipoPlano);
        }

    }

    private IllegalArgumentException unsupportedTipoPlano(String operationName, Constantes.TipoPlano tipoPlano) {

        return new IllegalArgumentException(
                "DemandPlanItem "
                        + operationName
                        + " accepts only Unconstrained Plan or Constrained Plan physical quantities "
                        + "[PLANO_IRRESTRITO, PLANO_RESTRITO]; received plan="
                        + tipoPlano);

    }

    private IllegalArgumentException unsupportedTipoDemanda(String operationName, Constantes.TipoDemanda tipoDemanda) {

        return new IllegalArgumentException(
                "DemandPlanItem "
                        + operationName
                        + " accepts only materialized demand components "
                        + "[Baseline, New Materials, Uplift, Demand Adjustment, Total]; received demand component="
                        + tipoDemanda);

    }

    private IllegalArgumentException unsupportedKeyFigureStandard(
            String operationName,
            KeyFigureStandard keyFigureStandardDemandPlanning) {

        return new IllegalArgumentException(
                "DemandPlanItem "
                        + operationName
                        + " accepts only Demand Plan physical key figures "
                        + "[Baseline, New Materials, Uplift, Demand Adjustment]; received key figure="
                        + keyFigureStandardDemandPlanning.getKeyFigureStandardEnum());

    }

    private IllegalArgumentException unsupportedModificacaoAgregadaPlano(
            Constantes.ModificacaoAgregadaPlano modificacaoAgregadaPlano) {

        return new IllegalArgumentException(
                "DemandPlanItem setQuantidadeTotal accepts only Community aggregate update modes "
                        + "[LINHA_AJUSTE, PROPORCIONAL_OU_BASELINE]; received update mode="
                        + modificacaoAgregadaPlano);

    }

    private IllegalArgumentException unsupportedTrendSeasonal(Constantes.TrendSeasonal trendSeasonal) {

        return new IllegalArgumentException(
                "DemandPlanItem can read only materialized trend/seasonal components "
                        + "[TREND, SEASONAL, TREND_E_SEASONAL]; received "
                        + trendSeasonal);

    }
    
}
