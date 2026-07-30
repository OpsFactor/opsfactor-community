package com.opsfactor.community.capability.demandplanning.demandplan.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Historico persistido das series usadas na execucao de um Demand Plan.
 *
 * <p>As colunas fisicas continuam com os nomes transicionais
 * {@code quantidadeBase} e {@code quantidadeNormalizada}. O codigo novo deve
 * preferir os aliases de dominio {@code vendaHistoricaTratamentoStockouts} e
 * {@code vendaHistoricaTratamentoOutliers}, deixando claro qual etapa da
 * limpeza historica alimentou cada valor persistido.</p>
 */
@EqualsAndHashCode(of="key")
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "historico_demand_plan_item")
public class HistoricoDemandPlanItem implements Serializable {

    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private HistoricoDemandPlanItemKey key;

    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @Getter 
    @Setter
    @EqualsAndHashCode
    public static class HistoricoDemandPlanItemKey implements Serializable {
        
        @ManyToOne(optional = false)
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
    
    /**
     * Venda historica apos a etapa de tratamento de stockouts.
     *
     * <p>`null` preserva o fallback operacional zero para series sem dado
     * persistido. Valor presente negativo ou nao finito indica historico
     * tratado inconsistente e nao deve alimentar engines estatisticas.</p>
     */
    private Double quantidadeBase;

    /**
     * Venda historica apos a etapa de tratamento de outliers/eventos.
     */
    private Double quantidadeNormalizada;
    
    // Gerado apenas por modelos que fazem decomposição em seasonal/trend.
    private Double quantidadeTrend;
    private Double quantidadeSeasonal;
    
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
        if(key == null) key = new HistoricoDemandPlanItemKey();
        key.setLocation(location);
    }
    
    public void setProduto(Produto produto) {
        if (key == null) key = new HistoricoDemandPlanItemKey();
        key.setProduto(produto);
    }
    
    public void setDataReferencia(LocalDateTime dataReferencia) {
        if (key == null) key = new HistoricoDemandPlanItemKey();
        key.setDataReferencia(dataReferencia);
    }

    public void setDemandPlan(DemandPlan demandPlan) {
        if (key == null) key = new HistoricoDemandPlanItemKey();
        key.setDemandPlan(demandPlan);
    }
    
    public double getQuantidadeBase() {
        return getVendaHistoricaTratadaNaoNegativaOuZero(
                quantidadeBase,
                "stockout-treated historical sales");
    }
    public Double getQuantidadeBaseCadastrada() {
        return quantidadeBase;
    }

    /**
     * Alias de dominio para a serie historica apos tratamento de stockouts.
     */
    public double getVendaHistoricaTratamentoStockouts() {
        return getQuantidadeBase();
    }

    /**
     * Retorna o valor cadastrado antes do fallback zero usado em calculos.
     */
    public Double getVendaHistoricaTratamentoStockoutsCadastrada() {
        return getQuantidadeBaseCadastrada();
    }

    /**
     * Grava a serie historica apos tratamento de stockouts na coluna fisica
     * transicional {@code quantidadeBase}.
     */
    public void setVendaHistoricaTratamentoStockouts(Double vendaHistoricaTratamentoStockouts) {
        this.quantidadeBase = vendaHistoricaTratamentoStockouts;
    }

    public double getQuantidadeNormalizada() {
        return getVendaHistoricaTratadaNaoNegativaOuZero(
                quantidadeNormalizada,
                "outlier-treated historical sales");
    }
    public Double getQuantidadeNormalizadaCadastrada() {
        return quantidadeNormalizada;
    }

    /**
     * Alias de dominio para a serie historica apos tratamento de outliers.
     */
    public double getVendaHistoricaTratamentoOutliers() {
        return getQuantidadeNormalizada();
    }

    /**
     * Retorna o valor cadastrado antes do fallback zero usado em calculos.
     */
    public Double getVendaHistoricaTratamentoOutliersCadastrada() {
        return getQuantidadeNormalizadaCadastrada();
    }

    /**
     * Grava a serie historica apos tratamento de outliers na coluna fisica
     * transicional {@code quantidadeNormalizada}.
     */
    public void setVendaHistoricaTratamentoOutliers(Double vendaHistoricaTratamentoOutliers) {
        this.quantidadeNormalizada = vendaHistoricaTratamentoOutliers;
    }

    /**
     * Valida as series fisicas de demanda historica tratada.
     *
     * <p>Os componentes `trend` e `seasonal` podem ser negativos conforme a
     * decomposicao estatistica e, por isso, nao usam este helper. A regra aqui
     * vale apenas para a demanda historica tratada que alimenta forecast.</p>
     */
    private double getVendaHistoricaTratadaNaoNegativaOuZero(
            Double vendaHistoricaTratada,
            String nomeSerie) {

        if (vendaHistoricaTratada == null) {
            return 0.0d;
        }
        if (!Double.isFinite(vendaHistoricaTratada)
                || vendaHistoricaTratada < 0.0d) {
            throw new IllegalStateException(
                    "Demand Plan "
                            + nomeSerie
                            + " must be finite and non-negative for material "
                            + getMaterialIdParaMensagem()
                            + " / location "
                            + getLocationIdParaMensagem()
                            + " / reference date "
                            + getDataReferenciaParaMensagem()
                            + ": "
                            + vendaHistoricaTratada
                            + ".");
        }
        return vendaHistoricaTratada;

    }

    private String getMaterialIdParaMensagem() {

        if (key == null
                || key.getProduto() == null
                || key.getProduto().getId() == null) {
            return "<sem-material>";
        }
        return key.getProduto().getId();

    }

    private String getLocationIdParaMensagem() {

        if (key == null
                || key.getLocation() == null
                || key.getLocation().getId() == null) {
            return "<sem-location>";
        }
        return key.getLocation().getId();

    }

    private String getDataReferenciaParaMensagem() {

        if (key == null
                || key.getDataReferencia() == null) {
            return "<sem-data>";
        }
        return key.getDataReferencia().toString();

    }

    public double getQuantidadeTrend() {
        return (quantidadeTrend == null) ? 0 : quantidadeTrend;
    }
    public Double getQuantidadeTrendCadastrada() {
        return quantidadeTrend;
    }

    public double getQuantidadeSeasonal() {
        return (quantidadeSeasonal == null) ? 0 : quantidadeSeasonal;
    }
    public Double getQuantidadeSeasonalCadastrada() {
        return quantidadeSeasonal;
    }

}
