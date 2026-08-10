package com.opsfactor.community.capability.supplyplanning.supplyplan.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes.TipoPlano;
import lombok.*;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

/**
 * Linha de demanda direta considerada pelo Supply Planning Community.
 *
 * <p>O Community alimenta o canal `PLANO_DEMANDA` a partir do Demand Plan e
 * deixa `CARTEIRA` zerado. Os campos de valores permanecem por compatibilidade
 * de schema e para overlay Enterprise, mas nao entram em calculo financeiro no
 * runtime Community.</p>
 */
@EqualsAndHashCode(of="demandaDiretaConsideradaLinhaCompositeKey")
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class DemandaDiretaConsideradaLinha {

    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey demandaDiretaConsideradaLinhaCompositeKey;

    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @Getter 
    @Setter
    @EqualsAndHashCode
    public static class DemandaDiretaConsideradaLinhaCompositeKey implements Serializable {
        
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private SupplyPlan supplyPlan;
        
        @ManyToOne(optional = false, fetch = FetchType.LAZY) // lazy para evitar N+1 onde produto busca precoCogsPadrao
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Location location;
        
        @ManyToOne(optional = false, fetch = FetchType.LAZY) // lazy para evitar N+1 onde produto busca precoCogsPadrao
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Produto material;
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
    
    // --------------------- DADOS ORIGINAIS, SEM APLICAR MAJORAÇÃO DP/CARTEIRA -------------------
    // QUANTIDADE PARA:
    // 1) PLANO DEMANDA NA PRÓPRIA LOCATION
    // 2) CARTEIRA COM DESTINO = ESTA LOCATION (CLIENTE FINAL)
    // 3) CARTEIRA SEM DESTINO COM ORIGEM NESTA LOCATION
    private Double quantidadePlanoDemandaOriginal;
    private Double quantidadeCarteiraOriginal;

    // QUANTIDADE PARA:
    // 1) PLANO DE DEMANDA PROPAGADO PARA LOCATION INTERNA
    // 2) CARTEIRA COM ORIGEM OU DESTINO = LOCATION CLIENTE FINAL PROPAGADO PARA LOCATION INTERNA
    // usados nos casos em que o perfil de execução determina a consolidação da demanda no nível location interna
    // a demanda é propagada de lead time até as locations internas -----------------------------
    private Double quantidadePlanoDemandaOriginalPropagadaLocationInterna;
    private Double quantidadeCarteiraOriginalPropagadaLocationInterna;

    // FATURAMENTO E CUSTOS PARA:
    // 1) PLANO DEMANDA NA PRÓPRIA LOCATION
    // 2) CARTEIRA COM DESTINO = ESTA LOCATION (CLIENTE FINAL)
    // 3) CARTEIRA SEM DESTINO COM ORIGEM NESTA LOCATION
    private Double grossSalesPlanoDemandaOriginal;
    private Double custoFretePlanoDemandaOriginal;
    private Double custoImpostosPlanoDemandaOriginal;
    private Double grossSalesCarteiraOriginal;
    private Double custoFreteCarteiraOriginal;
    private Double custoImpostosCarteiraOriginal;

    // FATURAMENTO E CUSTOS PARA:
    // 1) PLANO DE DEMANDA PROPAGADO PARA LOCATION INTERNA
    // 2) CARTEIRA COM ORIGEM OU DESTINO = LOCATION CLIENTE FINAL PROPAGADO PARA LOCATION INTERNA
    // (apenas se perfil de execução
    // indicar consolidação em locations internas)
    private Double grossSalesPlanoDemandaOriginalPropagadaLocationInterna;
    private Double custoFretePlanoDemandaOriginalPropagadaLocationInterna;
    private Double custoImpostosPlanoDemandaOriginalPropagadaLocationInterna;
    private Double grossSalesCarteiraOriginalPropagadaLocationInterna;
    private Double custoFreteCarteiraOriginalPropagadaLocationInterna;
    private Double custoImpostosCarteiraOriginalPropagadaLocationInterna;

    // ------------ DADOS COM MAJORAÇÃO DP/CARTEIRA PARA USO NO SUPPLY PLAN  -------------------
    // DEMANDA DIRETA (CONSIDERADA EFETIVAMENTE NO SUPPLY PLAN) PARA PROJEÇÃO DE ESTOQUES
    // incorpora a regra de majoração da demanda (DP vs carteira)
    private Double quantidadeDemandaDiretaPlanoDemandaIrrestrita;
    private Double grossSalesDemandaDiretaPlanoDemandaIrrestrita;
    private Double custoFreteDemandaDiretaPlanoDemandaIrrestrita;
    private Double custoImpostosDemandaDiretaPlanoDemandaIrrestrita;
    private Double quantidadeDemandaDiretaCarteiraIrrestrita;
    private Double grossSalesDemandaDiretaCarteiraIrrestrita;
    private Double custoFreteDemandaDiretaCarteiraIrrestrita;
    private Double custoImpostosDemandaDiretaCarteiraIrrestrita;

    private Double quantidadeDemandaDiretaPlanoDemandaRestrita;
    private Double grossSalesDemandaDiretaPlanoDemandaRestrita;
    private Double custoFreteDemandaDiretaPlanoDemandaRestrita;
    private Double custoImpostosDemandaDiretaPlanoDemandaRestrita;
    private Double quantidadeDemandaDiretaCarteiraRestrita;
    private Double grossSalesDemandaDiretaCarteiraRestrita;
    private Double custoFreteDemandaDiretaCarteiraRestrita;
    private Double custoImpostosDemandaDiretaCarteiraRestrita;

    // DEMANDA DIRETA PARA CÁLCULO DOS SAFETY STOCKS (PODE USAR OUTRO MODELO DE MAJORAÇÃO DP/CARTEIRA) : SEMPRE IRRESTRITO
    // incorpora a regra de majoração da demanda (DP vs carteira)
    private Double quantidadeDemandaDiretaEstoqueSeguranca;

    // Importante : os valores em R$ deverão ser obtidos através do cruzamento com o demand plan com o pricing plan

    public Location getLocation() {
        return demandaDiretaConsideradaLinhaCompositeKey.getLocation();
    }
    
    public Produto getMaterial(){
        return demandaDiretaConsideradaLinhaCompositeKey.getMaterial();
    }
   
    public UnidadeMedida getUnidadeMedidaCadastrado() {
        return unidadeMedida;
    }
    public UnidadeMedida getUnidadeMedida(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedida == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedida;
    }

    public SupplyPlan getSupplyPlan() {
        return demandaDiretaConsideradaLinhaCompositeKey.getSupplyPlan();
    }

    public LocalDateTime getDataReferencia(){
        return demandaDiretaConsideradaLinhaCompositeKey.getDataReferencia();
    }

    // GETTERS - QUANTIDADE SELLOUT
    public double getQuantidadePlanoDemandaOriginal() {
        return getQuantidadeDemandaDiretaNaoNegativaOuZero(
                quantidadePlanoDemandaOriginal,
                "original demand plan");
    }
    public double getQuantidadeCarteiraOriginal() {
        return getQuantidadeDemandaDiretaNaoNegativaOuZero(
                quantidadeCarteiraOriginal,
                "original customer orders");
    }
    public double getQuantidadePlanoDemandaOriginalPropagadaLocationInterna() {
        return getQuantidadeDemandaDiretaNaoNegativaOuZero(
                quantidadePlanoDemandaOriginalPropagadaLocationInterna,
                "propagated original demand plan");
    }
    public double getQuantidadeCarteiraOriginalPropagadaLocationInterna() {
        return getQuantidadeDemandaDiretaNaoNegativaOuZero(
                quantidadeCarteiraOriginalPropagadaLocationInterna,
                "propagated original customer orders");
    }
    public double getGrossSalesPlanoDemandaOriginal() {
        return (grossSalesPlanoDemandaOriginal == null) ? 0 : grossSalesPlanoDemandaOriginal;
    }
    public double getCustoFretePlanoDemandaOriginal() {
        return (custoFretePlanoDemandaOriginal == null) ? 0 : custoFretePlanoDemandaOriginal;
    }
    public double getCustoImpostosPlanoDemandaOriginal() {
        return (custoImpostosPlanoDemandaOriginal == null) ? 0 : custoImpostosPlanoDemandaOriginal;
    }
    public double getGrossSalesCarteiraOriginal() {
        return (grossSalesCarteiraOriginal == null) ? 0 : grossSalesCarteiraOriginal;
    }
    public double getCustoFreteCarteiraOriginal() {
        return (custoFreteCarteiraOriginal == null) ? 0 : custoFreteCarteiraOriginal;
    }
    public double getCustoImpostosCarteiraOriginal() {
        return (custoImpostosCarteiraOriginal == null) ? 0 : custoImpostosCarteiraOriginal;
    }
    public double getGrossSalesPlanoDemandaOriginalPropagadaLocationInterna() {
        return (grossSalesPlanoDemandaOriginalPropagadaLocationInterna == null) ? 0 : grossSalesPlanoDemandaOriginalPropagadaLocationInterna;
    }
    public double getCustoFretePlanoDemandaOriginalPropagadaLocationInterna() {
        return (custoFretePlanoDemandaOriginalPropagadaLocationInterna == null) ? 0 : custoFretePlanoDemandaOriginalPropagadaLocationInterna;
    }
    public double getCustoImpostosPlanoDemandaOriginalPropagadaLocationInterna() {
        return (custoImpostosPlanoDemandaOriginalPropagadaLocationInterna == null) ? 0 : custoImpostosPlanoDemandaOriginalPropagadaLocationInterna;
    }
    public double getGrossSalesCarteiraOriginalPropagadaLocationInterna() {
        return (grossSalesCarteiraOriginalPropagadaLocationInterna == null) ? 0 : grossSalesCarteiraOriginalPropagadaLocationInterna;
    }
    public double getCustoFreteCarteiraOriginalPropagadaLocationInterna() {
        return (custoFreteCarteiraOriginalPropagadaLocationInterna == null) ? 0 : custoFreteCarteiraOriginalPropagadaLocationInterna;
    }
    public double getCustoImpostosCarteiraOriginalPropagadaLocationInterna() {
        return (custoImpostosCarteiraOriginalPropagadaLocationInterna == null) ? 0 : custoImpostosCarteiraOriginalPropagadaLocationInterna;
    }
    public double getQuantidadeDemandaDiretaIrrestrita() {
        return getQuantidadeDemandaDiretaTotalIrrestrita();
    }
    public double getGrossSalesDemandaDiretaIrrestrita() {
        return getGrossSalesDemandaDiretaTotalIrrestrita();
    }
    public double getCustoImpostosDemandaDiretaIrrestrita() {
        return getCustoImpostosDemandaDiretaTotalIrrestrita();
    }
    public double getCustoFreteDemandaDiretaIrrestrita() {
        return getCustoFreteDemandaDiretaTotalIrrestrita();
    }
    public double getQuantidadeDemandaDiretaPlanoDemandaIrrestrita() {
        return getQuantidadeDemandaDiretaNaoNegativaOuZero(
                quantidadeDemandaDiretaPlanoDemandaIrrestrita,
                "unrestricted demand plan direct demand");
    }
    public double getGrossSalesDemandaDiretaPlanoDemandaIrrestrita() {
        return (grossSalesDemandaDiretaPlanoDemandaIrrestrita == null) ? 0 : grossSalesDemandaDiretaPlanoDemandaIrrestrita;
    }
    public double getCustoImpostosDemandaDiretaPlanoDemandaIrrestrita() {
        return (custoImpostosDemandaDiretaPlanoDemandaIrrestrita == null) ? 0 : custoImpostosDemandaDiretaPlanoDemandaIrrestrita;
    }
    public double getCustoFreteDemandaDiretaPlanoDemandaIrrestrita() {
        return (custoFreteDemandaDiretaPlanoDemandaIrrestrita == null) ? 0 : custoFreteDemandaDiretaPlanoDemandaIrrestrita;
    }
    public double getQuantidadeDemandaDiretaCarteiraIrrestrita() {
        return getQuantidadeDemandaDiretaNaoNegativaOuZero(
                quantidadeDemandaDiretaCarteiraIrrestrita,
                "unrestricted customer orders direct demand");
    }
    public double getGrossSalesDemandaDiretaCarteiraIrrestrita() {
        return (grossSalesDemandaDiretaCarteiraIrrestrita == null) ? 0 : grossSalesDemandaDiretaCarteiraIrrestrita;
    }
    public double getCustoImpostosDemandaDiretaCarteiraIrrestrita() {
        return (custoImpostosDemandaDiretaCarteiraIrrestrita == null) ? 0 : custoImpostosDemandaDiretaCarteiraIrrestrita;
    }
    public double getCustoFreteDemandaDiretaCarteiraIrrestrita() {
        return (custoFreteDemandaDiretaCarteiraIrrestrita == null) ? 0 : custoFreteDemandaDiretaCarteiraIrrestrita;
    }
    public double getQuantidadeDemandaDiretaRestrita() {
        return getQuantidadeDemandaDiretaTotalRestrita();
    }
    public double getGrossSalesDemandaDiretaRestrita() {
        return getGrossSalesDemandaDiretaTotalRestrita();
    }
    public double getCustoImpostosDemandaDiretaRestrita() {
        return getCustoImpostosDemandaDiretaTotalRestrita();
    }
    public double getCustoFreteDemandaDiretaRestrita() {
        return getCustoFreteDemandaDiretaTotalRestrita();
    }
    public double getQuantidadeDemandaDiretaPlanoDemandaRestrita() {
        return getQuantidadeDemandaDiretaNaoNegativaOuZero(
                quantidadeDemandaDiretaPlanoDemandaRestrita,
                "restricted demand plan direct demand");
    }
    public double getGrossSalesDemandaDiretaPlanoDemandaRestrita() {
        return (grossSalesDemandaDiretaPlanoDemandaRestrita == null) ? 0 : grossSalesDemandaDiretaPlanoDemandaRestrita;
    }
    public double getCustoImpostosDemandaDiretaPlanoDemandaRestrita() {
        return (custoImpostosDemandaDiretaPlanoDemandaRestrita == null) ? 0 : custoImpostosDemandaDiretaPlanoDemandaRestrita;
    }
    public double getCustoFreteDemandaDiretaPlanoDemandaRestrita() {
        return (custoFreteDemandaDiretaPlanoDemandaRestrita == null) ? 0 : custoFreteDemandaDiretaPlanoDemandaRestrita;
    }
    public double getQuantidadeDemandaDiretaCarteiraRestrita() {
        return getQuantidadeDemandaDiretaNaoNegativaOuZero(
                quantidadeDemandaDiretaCarteiraRestrita,
                "restricted customer orders direct demand");
    }
    public double getGrossSalesDemandaDiretaCarteiraRestrita() {
        return (grossSalesDemandaDiretaCarteiraRestrita == null) ? 0 : grossSalesDemandaDiretaCarteiraRestrita;
    }
    public double getCustoImpostosDemandaDiretaCarteiraRestrita() {
        return (custoImpostosDemandaDiretaCarteiraRestrita == null) ? 0 : custoImpostosDemandaDiretaCarteiraRestrita;
    }
    public double getCustoFreteDemandaDiretaCarteiraRestrita() {
        return (custoFreteDemandaDiretaCarteiraRestrita == null) ? 0 : custoFreteDemandaDiretaCarteiraRestrita;
    }
    public double getQuantidadeDemandaDiretaEstoqueSeguranca() {
        return getQuantidadeDemandaDiretaNaoNegativaOuZero(
                quantidadeDemandaDiretaEstoqueSeguranca,
                "safety stock direct demand");
    }

    /**
     * Valida quantidades fisicas de demanda direta antes de qualquer majoracao,
     * conversao de unidade ou soma de segmentos.
     *
     * <p>No Community, o segmento efetivo vem do Demand Plan e a carteira deve
     * ficar zerada. No Enterprise, os mesmos campos tambem recebem carteira e
     * valores propagados pelo otimizador. Em ambos os runtimes, `null` continua
     * representando ausencia operacional igual a zero, mas valor presente
     * negativo ou nao finito sinaliza snapshot fisico corrompido e deve falhar
     * antes de alimentar estoque projetado, safety stock, heuristico ou modelo
     * otimizado.</p>
     */
    private double getQuantidadeDemandaDiretaNaoNegativaOuZero(
            Number quantidadeDemandaDireta,
            String nomeCampoQuantidade) {

        if (quantidadeDemandaDireta == null) {
            return 0.0d;
        }

        double valorQuantidadeDemandaDireta = quantidadeDemandaDireta.doubleValue();
        if (!Double.isFinite(valorQuantidadeDemandaDireta)
                || valorQuantidadeDemandaDireta < 0.0d) {
            throw new IllegalStateException(
                    "Direct demand quantity "
                            + nomeCampoQuantidade
                            + " must be finite and non-negative for "
                            + getContextoLinhaParaMensagem()
                            + ": "
                            + valorQuantidadeDemandaDireta
                            + ".");
        }
        return valorQuantidadeDemandaDireta;

    }

    private String getContextoLinhaParaMensagem() {

        return "material "
                + getMaterialIdParaMensagem()
                + " / location "
                + getLocationIdParaMensagem()
                + " / reference date "
                + getDataReferenciaParaMensagem();

    }

    private String getMaterialIdParaMensagem() {

        Produto material = demandaDiretaConsideradaLinhaCompositeKey == null
                ? null
                : demandaDiretaConsideradaLinhaCompositeKey.getMaterial();
        return material == null ? "<null>" : String.valueOf(material.getId());

    }

    private String getLocationIdParaMensagem() {

        Location location = demandaDiretaConsideradaLinhaCompositeKey == null
                ? null
                : demandaDiretaConsideradaLinhaCompositeKey.getLocation();
        return location == null ? "<null>" : String.valueOf(location.getId());

    }

    private String getDataReferenciaParaMensagem() {

        LocalDateTime dataReferencia = demandaDiretaConsideradaLinhaCompositeKey == null
                ? null
                : demandaDiretaConsideradaLinhaCompositeKey.getDataReferencia();
        return dataReferencia == null ? "<null>" : String.valueOf(dataReferencia);

    }

    public double getQuantidadeDemandaDiretaTotalIrrestrita() {
        return getQuantidadeDemandaDiretaPlanoDemandaIrrestrita() + getQuantidadeDemandaDiretaCarteiraIrrestrita();
    }

    public double getQuantidadeDemandaDiretaTotalRestrita() {
        return getQuantidadeDemandaDiretaPlanoDemandaRestrita() + getQuantidadeDemandaDiretaCarteiraRestrita();
    }

    public double getGrossSalesDemandaDiretaTotalIrrestrita() {
        return getGrossSalesDemandaDiretaPlanoDemandaIrrestrita() + getGrossSalesDemandaDiretaCarteiraIrrestrita();
    }

    public double getGrossSalesDemandaDiretaTotalRestrita() {
        return getGrossSalesDemandaDiretaPlanoDemandaRestrita() + getGrossSalesDemandaDiretaCarteiraRestrita();
    }

    public double getCustoImpostosDemandaDiretaTotalIrrestrita() {
        return getCustoImpostosDemandaDiretaPlanoDemandaIrrestrita() + getCustoImpostosDemandaDiretaCarteiraIrrestrita();
    }

    public double getCustoImpostosDemandaDiretaTotalRestrita() {
        return getCustoImpostosDemandaDiretaPlanoDemandaRestrita() + getCustoImpostosDemandaDiretaCarteiraRestrita();
    }

    public double getCustoFreteDemandaDiretaTotalIrrestrita() {
        return getCustoFreteDemandaDiretaPlanoDemandaIrrestrita() + getCustoFreteDemandaDiretaCarteiraIrrestrita();
    }

    public double getCustoFreteDemandaDiretaTotalRestrita() {
        return getCustoFreteDemandaDiretaPlanoDemandaRestrita() + getCustoFreteDemandaDiretaCarteiraRestrita();
    }

    // GETTER GERAL PARA TODAS AS COMBINACOES DE TIPO DE DEMANDA DIRETA, VALOR DA DEMANDA DIRETA, CANAL DA DEMANDA DIRETA
    public enum TipoDemandaDireta {
        PLANO_DEMANDA, CARTEIRA, TOTAL
    }
    public enum ValorDemandaDireta {
        GROSS, IMPOSTOS, FRETE
    }
    public enum PropagacaoDemandaDireta {
        NAO_PROPAGADA, PROPAGADA, TOTAL
    }
    public enum UsoDemandaDireta {
        PROJECAO_ESTOQUE, ESTOQUE_SEGURANCA
    }

    public double getQuantidadeConsideradaSupplyPlan(
            UsoDemandaDireta usoDemandaDireta,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        return getQuantidadeConsideradaSupplyPlan(
                usoDemandaDireta,
                TipoDemandaDireta.TOTAL,
                tipoPlano,
                unidadeMedidaTarget,
                unidadeMedidaProjection);
    }

    /**
     *
     * @param usoDemandaDireta PROJECAO_ESTOQUE ou ESTOQUE_SEGURANCA
     * @param tipoDemandaDireta para usoDemandaDireta = PROJECAO_ESTOQUE : PLANO_DEMANDA, CARTEIRA, TOTAL. para usoDemandaDireta = ESTOQUE_SEGURANÇA : APENAS TOTAL (não há segregação entre demanda para safety stock carteira ou plano demanda)
     * @param tipoPlano
     * @param unidadeMedidaTarget
     * @param unidadeMedidaProjection
     * @return
     */
    public double getQuantidadeConsideradaSupplyPlan(
            UsoDemandaDireta usoDemandaDireta,
            TipoDemandaDireta tipoDemandaDireta,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        double quantidade;

        switch (usoDemandaDireta) {
            case PROJECAO_ESTOQUE:
                quantidade = getQuantidadeDemandaDiretaConsideradaSegregada(tipoDemandaDireta, tipoPlano);
                break;
            case ESTOQUE_SEGURANCA:
                if (!tipoDemandaDireta.equals(TipoDemandaDireta.TOTAL)) {
                    throw unsupportedCombination("getQuantidadeConsideradaSupplyPlan", usoDemandaDireta, tipoDemandaDireta);
                }
                quantidade = getQuantidadeDemandaDiretaEstoqueSeguranca();
                break;
            default:
                throw unsupportedCombination("getQuantidadeConsideradaSupplyPlan", usoDemandaDireta);
        }

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        return quantidade * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getMaterial(),
                getUnidadeMedida(parametrosGlobais),
                unidadeMedidaTarget);

    }

    public double getQuantidadeDemandaDiretaConsideradaSegregada(
            TipoDemandaDireta tipoDemandaDireta,
            TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                switch (tipoDemandaDireta) {
                    case PLANO_DEMANDA:
                        return getQuantidadeDemandaDiretaPlanoDemandaIrrestrita();
                    case CARTEIRA:
                        return getQuantidadeDemandaDiretaCarteiraIrrestrita();
                    case TOTAL:
                        return getQuantidadeDemandaDiretaIrrestrita();
                    default:
                        throw unsupportedCombination("getQuantidadeDemandaDiretaConsideradaSegregada", tipoPlano, tipoDemandaDireta);
                }
            case PLANO_RESTRITO:
            case PLANO_TRABALHO:
                switch (tipoDemandaDireta) {
                    case PLANO_DEMANDA:
                        return getQuantidadeDemandaDiretaPlanoDemandaRestrita();
                    case CARTEIRA:
                        return getQuantidadeDemandaDiretaCarteiraRestrita();
                    case TOTAL:
                        return getQuantidadeDemandaDiretaRestrita();
                    default:
                        throw unsupportedCombination("getQuantidadeDemandaDiretaConsideradaSegregada", tipoPlano, tipoDemandaDireta);
                }
            default:
                throw unsupportedTipoPlano("getQuantidadeDemandaDiretaConsideradaSegregada", tipoPlano);
        }
    }

    public void setQuantidadeDemandaDiretaConsideradaSegregada(
            double quantidade,
            TipoDemandaDireta tipoDemandaDireta,
            TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                switch (tipoDemandaDireta) {
                    case PLANO_DEMANDA:
                        setQuantidadeDemandaDiretaPlanoDemandaIrrestrita(quantidade);
                        return;
                    case CARTEIRA:
                        setQuantidadeDemandaDiretaCarteiraIrrestrita(quantidade);
                        return;
                    case TOTAL:
                        throw SupplyPlanningDataContract.unsupportedTotalWrite(
                                DemandaDiretaConsideradaLinha.class,
                                "setQuantidadeDemandaDiretaConsideradaSegregada");
                    default:
                        throw unsupportedCombination("setQuantidadeDemandaDiretaConsideradaSegregada", tipoPlano, tipoDemandaDireta);
                }
            case PLANO_RESTRITO:
            case PLANO_TRABALHO:
                switch (tipoDemandaDireta) {
                    case PLANO_DEMANDA:
                        setQuantidadeDemandaDiretaPlanoDemandaRestrita(quantidade);
                        return;
                    case CARTEIRA:
                        setQuantidadeDemandaDiretaCarteiraRestrita(quantidade);
                        return;
                    case TOTAL:
                        throw SupplyPlanningDataContract.unsupportedTotalWrite(
                                DemandaDiretaConsideradaLinha.class,
                                "setQuantidadeDemandaDiretaConsideradaSegregada");
                    default:
                        throw unsupportedCombination("setQuantidadeDemandaDiretaConsideradaSegregada", tipoPlano, tipoDemandaDireta);
                }
            default:
                throw unsupportedTipoPlano("setQuantidadeDemandaDiretaConsideradaSegregada", tipoPlano);
        }
    }

    /**
     * Atualiza um segmento fisico da demanda direta considerada.
     *
     * <p>Projection e heuristico passam a quantidade na unidade em que estao
     * trabalhando. A entidade armazena o valor em sua unidade propria, por isso
     * a atualizacao converte a unidade antes de gravar o segmento
     * `PLANO_DEMANDA` ou `CARTEIRA`.</p>
     */
    public void updateQuantidadeDemandaDiretaConsideradaSegregada(
            DoubleUnaryOperator funcaoAtualizacaoQuantidade,
            UnidadeMedida unidadeMedidaQuantidade,
            TipoDemandaDireta tipoDemandaDireta,
            TipoPlano tipoPlano,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeDemandaDiretaConsiderada = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getMaterial(),
                unidadeMedidaQuantidade,
                getUnidadeMedida(parametrosGlobais));
        double quantidadeAtual = getQuantidadeDemandaDiretaConsideradaSegregada(tipoDemandaDireta, tipoPlano);

        setQuantidadeDemandaDiretaConsideradaSegregada(
                funcaoAtualizacaoQuantidade.applyAsDouble(quantidadeAtual) * conversaoParaUnidadeDemandaDiretaConsiderada,
                tipoDemandaDireta,
                tipoPlano);

    }

    /**
     * Inicializa o plano irrestrito Community a partir do Demand Plan.
     *
     * <p>O runtime aberto nao possui carteira/sales orders como fonte de
     * demanda. Assim, a soma da quantidade local com a quantidade propagada
     * de clientes vira a demanda direta irrestrita no segmento
     * `PLANO_DEMANDA`, enquanto `CARTEIRA` permanece explicitamente zerado.</p>
     */
    public void atualizaPlanoIrrestritoCommunityComPlanoDemanda() {

        setQuantidadeDemandaDiretaPlanoDemandaIrrestrita(getQuantidadePlanoDemandaTotal());
        setQuantidadeDemandaDiretaCarteiraIrrestrita(0.0);

    }

    public void updateQuantidadeConsideradaSupplyPlan(
            DoubleUnaryOperator funcaoAtualizacaoQuantidade,
            UnidadeMedida unidadeMedidaQuantidade,
            UsoDemandaDireta usoDemandaDireta,
            TipoPlano tipoPlano,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeDemandaDiretaConsiderada = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getMaterial(),
                unidadeMedidaQuantidade,
                getUnidadeMedida(parametrosGlobais));

        switch (usoDemandaDireta) {
            case PROJECAO_ESTOQUE:
                throw SupplyPlanningDataContract.unsupportedTotalWrite(DemandaDiretaConsideradaLinha.class, "updateQuantidadeConsideradaSupplyPlan");
            case ESTOQUE_SEGURANCA:
                setQuantidadeDemandaDiretaEstoqueSeguranca(
                        funcaoAtualizacaoQuantidade.applyAsDouble(getQuantidadeDemandaDiretaEstoqueSeguranca())
                                * conversaoParaUnidadeDemandaDiretaConsiderada);
                return;
            default:
                throw unsupportedCombination("updateQuantidadeConsideradaSupplyPlan", usoDemandaDireta);
        }

    }

    public void setQuantidadeConsideradaSupplyPlan(
            double quantidade,
            UnidadeMedida unidadeMedidaQuantidade,
            UsoDemandaDireta usoDemandaDireta,
            TipoPlano tipoPlano,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        updateQuantidadeConsideradaSupplyPlan(
                quantidadeAtualNoObjeto -> quantidade,
                unidadeMedidaQuantidade, usoDemandaDireta, tipoPlano, unidadeMedidaProjection);

    }

    public double getValorConsideradoSupplyPlan(
            ValorDemandaDireta valorDemandaDireta,
            TipoPlano tipoPlano) {

        return getValorConsideradoSupplyPlan(TipoDemandaDireta.TOTAL, valorDemandaDireta, tipoPlano);
    }

    public double getValorConsideradoSupplyPlan(
            TipoDemandaDireta tipoDemandaDireta,
            ValorDemandaDireta valorDemandaDireta,
            TipoPlano tipoPlano) {

        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                switch (tipoDemandaDireta) {
                    case PLANO_DEMANDA:
                        switch (valorDemandaDireta) {
                            case GROSS:
                                return getGrossSalesDemandaDiretaPlanoDemandaIrrestrita();
                            case IMPOSTOS:
                                return getCustoImpostosDemandaDiretaPlanoDemandaIrrestrita();
                            case FRETE:
                                return getCustoFreteDemandaDiretaPlanoDemandaIrrestrita();
                            default:
                                throw unsupportedCombination("directDemandValue", tipoPlano, tipoDemandaDireta, valorDemandaDireta);
                        }
                    case CARTEIRA:
                        switch (valorDemandaDireta) {
                            case GROSS:
                                return getGrossSalesDemandaDiretaCarteiraIrrestrita();
                            case IMPOSTOS:
                                return getCustoImpostosDemandaDiretaCarteiraIrrestrita();
                            case FRETE:
                                return getCustoFreteDemandaDiretaCarteiraIrrestrita();
                            default:
                                throw unsupportedCombination("directDemandValue", tipoPlano, tipoDemandaDireta, valorDemandaDireta);
                        }
                    case TOTAL:
                        switch (valorDemandaDireta) {
                            case GROSS:
                                return getGrossSalesDemandaDiretaIrrestrita();
                            case IMPOSTOS:
                                return getCustoImpostosDemandaDiretaIrrestrita();
                            case FRETE:
                                return getCustoFreteDemandaDiretaIrrestrita();
                            default:
                                throw unsupportedCombination("directDemandValue", tipoPlano, tipoDemandaDireta, valorDemandaDireta);
                        }
                    default:
                        throw unsupportedCombination("directDemandSegment", tipoPlano, tipoDemandaDireta);
                }
            case PLANO_RESTRITO:
            case PLANO_TRABALHO:
                switch (tipoDemandaDireta) {
                    case PLANO_DEMANDA:
                        switch (valorDemandaDireta) {
                            case GROSS:
                                return getGrossSalesDemandaDiretaPlanoDemandaRestrita();
                            case IMPOSTOS:
                                return getCustoImpostosDemandaDiretaPlanoDemandaRestrita();
                            case FRETE:
                                return getCustoFreteDemandaDiretaPlanoDemandaRestrita();
                            default:
                                throw unsupportedCombination("directDemandValue", tipoPlano, tipoDemandaDireta, valorDemandaDireta);
                        }
                    case CARTEIRA:
                        switch (valorDemandaDireta) {
                            case GROSS:
                                return getGrossSalesDemandaDiretaCarteiraRestrita();
                            case IMPOSTOS:
                                return getCustoImpostosDemandaDiretaCarteiraRestrita();
                            case FRETE:
                                return getCustoFreteDemandaDiretaCarteiraRestrita();
                            default:
                                throw unsupportedCombination("directDemandValue", tipoPlano, tipoDemandaDireta, valorDemandaDireta);
                        }
                    case TOTAL:
                        switch (valorDemandaDireta) {
                            case GROSS:
                                return getGrossSalesDemandaDiretaRestrita();
                            case IMPOSTOS:
                                return getCustoImpostosDemandaDiretaRestrita();
                            case FRETE:
                                return getCustoFreteDemandaDiretaRestrita();
                            default:
                                throw unsupportedCombination("directDemandValue", tipoPlano, tipoDemandaDireta, valorDemandaDireta);
                        }
                    default:
                        throw unsupportedCombination("directDemandSegment", tipoPlano, tipoDemandaDireta);
                }
            default:
                throw unsupportedTipoPlano("directDemandPlan", tipoPlano);
        }
    }

    public void setValorConsideradoSupplyPlan(
            double novoValor,
            ValorDemandaDireta valorDemandaDireta,
            TipoPlano tipoPlano) {
        setValorConsideradoSupplyPlan(novoValor, TipoDemandaDireta.TOTAL, valorDemandaDireta, tipoPlano);
    }

    public void setValorConsideradoSupplyPlan(
            double novoValor,
            TipoDemandaDireta tipoDemandaDireta,
            ValorDemandaDireta valorDemandaDireta,
            TipoPlano tipoPlano) {
        updateValorConsideradoSupplyPlan(valorOriginal -> novoValor, tipoDemandaDireta, valorDemandaDireta, tipoPlano);
    }
    public void updateValorConsideradoSupplyPlan(
            DoubleUnaryOperator funcaoAtualizacaoValor,
            ValorDemandaDireta valorDemandaDireta,
            TipoPlano tipoPlano) {

        updateValorConsideradoSupplyPlan(funcaoAtualizacaoValor, TipoDemandaDireta.TOTAL, valorDemandaDireta, tipoPlano);
    }

    public void updateValorConsideradoSupplyPlan(
            DoubleUnaryOperator funcaoAtualizacaoValor,
            TipoDemandaDireta tipoDemandaDireta,
            ValorDemandaDireta valorDemandaDireta,
            TipoPlano tipoPlano) {

        double valorAtual = getValorConsideradoSupplyPlan(tipoDemandaDireta, valorDemandaDireta, tipoPlano);
        double novoValor = funcaoAtualizacaoValor.applyAsDouble(valorAtual);

        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                switch (tipoDemandaDireta) {
                    case PLANO_DEMANDA:
                        switch (valorDemandaDireta) {
                            case GROSS:
                                setGrossSalesDemandaDiretaPlanoDemandaIrrestrita(novoValor);
                                return;
                            case IMPOSTOS:
                                setCustoImpostosDemandaDiretaPlanoDemandaIrrestrita(novoValor);
                                return;
                            case FRETE:
                                setCustoFreteDemandaDiretaPlanoDemandaIrrestrita(novoValor);
                                return;
                            default:
                                throw unsupportedCombination("directDemandValue", tipoPlano, tipoDemandaDireta, valorDemandaDireta);
                        }
                    case CARTEIRA:
                        switch (valorDemandaDireta) {
                            case GROSS:
                                setGrossSalesDemandaDiretaCarteiraIrrestrita(novoValor);
                                return;
                            case IMPOSTOS:
                                setCustoImpostosDemandaDiretaCarteiraIrrestrita(novoValor);
                                return;
                            case FRETE:
                                setCustoFreteDemandaDiretaCarteiraIrrestrita(novoValor);
                                return;
                            default:
                                throw unsupportedCombination("directDemandValue", tipoPlano, tipoDemandaDireta, valorDemandaDireta);
                        }
                    case TOTAL:
                        throw SupplyPlanningDataContract.unsupportedTotalWrite(DemandaDiretaConsideradaLinha.class, "updateValorConsideradoSupplyPlan");
                    default:
                        throw unsupportedCombination("directDemandSegment", tipoPlano, tipoDemandaDireta);
                }
            case PLANO_RESTRITO:
            case PLANO_TRABALHO:
                switch (tipoDemandaDireta) {
                    case PLANO_DEMANDA:
                        switch (valorDemandaDireta) {
                            case GROSS:
                                setGrossSalesDemandaDiretaPlanoDemandaRestrita(novoValor);
                                return;
                            case IMPOSTOS:
                                setCustoImpostosDemandaDiretaPlanoDemandaRestrita(novoValor);
                                return;
                            case FRETE:
                                setCustoFreteDemandaDiretaPlanoDemandaRestrita(novoValor);
                                return;
                            default:
                                throw unsupportedCombination("directDemandValue", tipoPlano, tipoDemandaDireta, valorDemandaDireta);
                        }
                    case CARTEIRA:
                        switch (valorDemandaDireta) {
                            case GROSS:
                                setGrossSalesDemandaDiretaCarteiraRestrita(novoValor);
                                return;
                            case IMPOSTOS:
                                setCustoImpostosDemandaDiretaCarteiraRestrita(novoValor);
                                return;
                            case FRETE:
                                setCustoFreteDemandaDiretaCarteiraRestrita(novoValor);
                                return;
                            default:
                                throw unsupportedCombination("directDemandValue", tipoPlano, tipoDemandaDireta, valorDemandaDireta);
                        }
                    case TOTAL:
                        throw SupplyPlanningDataContract.unsupportedTotalWrite(DemandaDiretaConsideradaLinha.class, "updateValorConsideradoSupplyPlan");
                    default:
                        throw unsupportedCombination("directDemandSegment", tipoPlano, tipoDemandaDireta);
                }
            default:
                throw unsupportedTipoPlano("directDemandPlan", tipoPlano);
        }

    }

    public double getValorOriginal(
            TipoDemandaDireta tipoDemandaDireta,
            PropagacaoDemandaDireta propagacaoDemandaDireta,
            ValorDemandaDireta valorDemandaDireta) {
        switch (tipoDemandaDireta) {
            case PLANO_DEMANDA:
                switch (propagacaoDemandaDireta) {
                    case NAO_PROPAGADA:
                        switch(valorDemandaDireta) {
                            case GROSS:
                                return getGrossSalesPlanoDemandaOriginal();
                            case IMPOSTOS:
                                return getCustoImpostosPlanoDemandaOriginal();
                            case FRETE:
                                return getCustoFretePlanoDemandaOriginal();
                            default:
                                throw unsupportedCombination("getValorOriginal", tipoDemandaDireta, valorDemandaDireta);
                        }
                    case PROPAGADA:
                        switch(valorDemandaDireta) {
                            case GROSS:
                                return getGrossSalesPlanoDemandaOriginalPropagadaLocationInterna();
                            case IMPOSTOS:
                                return getCustoImpostosPlanoDemandaOriginalPropagadaLocationInterna();
                            case FRETE:
                                return getCustoFretePlanoDemandaOriginalPropagadaLocationInterna();
                            default:
                                throw unsupportedCombination("getValorOriginal", tipoDemandaDireta, valorDemandaDireta);
                        }
                    case TOTAL:
                        return getValorOriginal(tipoDemandaDireta, PropagacaoDemandaDireta.NAO_PROPAGADA, valorDemandaDireta)
                                + getValorOriginal(tipoDemandaDireta, PropagacaoDemandaDireta.PROPAGADA, valorDemandaDireta);
                    default:
                        throw unsupportedCombination("getValorOriginal", tipoDemandaDireta, propagacaoDemandaDireta);
                }
            case CARTEIRA:
                switch (propagacaoDemandaDireta) {
                    case NAO_PROPAGADA:
                        switch(valorDemandaDireta) {
                            case GROSS:
                                return getGrossSalesCarteiraOriginal();
                            case IMPOSTOS:
                                return getCustoImpostosCarteiraOriginal();
                            case FRETE:
                                return getCustoFreteCarteiraOriginal();
                            default:
                                throw unsupportedCombination("getValorOriginal", tipoDemandaDireta, valorDemandaDireta);
                        }
                    case PROPAGADA:
                        switch(valorDemandaDireta) {
                            case GROSS:
                                return getGrossSalesCarteiraOriginalPropagadaLocationInterna();
                            case IMPOSTOS:
                                return getCustoImpostosCarteiraOriginalPropagadaLocationInterna();
                            case FRETE:
                                return getCustoFreteCarteiraOriginalPropagadaLocationInterna();
                            default:
                                throw unsupportedCombination("getValorOriginal", tipoDemandaDireta, valorDemandaDireta);
                        }
                    case TOTAL:
                        return getValorOriginal(tipoDemandaDireta, PropagacaoDemandaDireta.NAO_PROPAGADA, valorDemandaDireta)
                                + getValorOriginal(tipoDemandaDireta, PropagacaoDemandaDireta.PROPAGADA, valorDemandaDireta);
                    default:
                        throw unsupportedCombination("getValorOriginal", tipoDemandaDireta, propagacaoDemandaDireta);
                }
            case TOTAL:
                return getValorOriginal(TipoDemandaDireta.PLANO_DEMANDA, propagacaoDemandaDireta, valorDemandaDireta)
                        + getValorOriginal(TipoDemandaDireta.CARTEIRA, propagacaoDemandaDireta, valorDemandaDireta);
            default:
                throw unsupportedCombination("getValorOriginal", tipoDemandaDireta);
        }
    }

    public double getQuantidadeOriginal(
            TipoDemandaDireta tipoDemandaDireta,
            PropagacaoDemandaDireta propagacaoDemandaDireta,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        double quantidade = 0;
        switch (tipoDemandaDireta) {
            case PLANO_DEMANDA:
                switch (propagacaoDemandaDireta) {
                    case NAO_PROPAGADA:
                        quantidade = getQuantidadePlanoDemandaOriginal();
                        break;
                    case PROPAGADA:
                        quantidade = getQuantidadePlanoDemandaOriginalPropagadaLocationInterna();
                        break;
                    case TOTAL:
                        quantidade = getQuantidadeOriginal(tipoDemandaDireta, PropagacaoDemandaDireta.NAO_PROPAGADA, unidadeMedidaTarget, unidadeMedidaProjection)
                                + getQuantidadeOriginal(tipoDemandaDireta, PropagacaoDemandaDireta.PROPAGADA, unidadeMedidaTarget, unidadeMedidaProjection);
                        break;
                    default:
                        throw unsupportedCombination("getValorOriginal", tipoDemandaDireta, propagacaoDemandaDireta);
                }
                break;
            case CARTEIRA:
                switch (propagacaoDemandaDireta) {
                    case NAO_PROPAGADA:
                        quantidade = getQuantidadeCarteiraOriginal();
                        break;
                    case PROPAGADA:
                        quantidade = getQuantidadeCarteiraOriginalPropagadaLocationInterna();
                        break;
                    case TOTAL:
                        quantidade = getQuantidadeOriginal(tipoDemandaDireta, PropagacaoDemandaDireta.NAO_PROPAGADA, unidadeMedidaTarget, unidadeMedidaProjection)
                                + getQuantidadeOriginal(tipoDemandaDireta, PropagacaoDemandaDireta.PROPAGADA, unidadeMedidaTarget, unidadeMedidaProjection);
                        break;
                    default:
                        throw unsupportedCombination("getValorOriginal", tipoDemandaDireta, propagacaoDemandaDireta);
                }
                break;
            case TOTAL:
                quantidade = getQuantidadeOriginal(TipoDemandaDireta.PLANO_DEMANDA, propagacaoDemandaDireta, unidadeMedidaTarget, unidadeMedidaProjection)
                        + getQuantidadeOriginal(TipoDemandaDireta.CARTEIRA, propagacaoDemandaDireta, unidadeMedidaTarget, unidadeMedidaProjection);
                break;
            default:
                throw unsupportedCombination("getQuantidadeOriginal", tipoDemandaDireta, propagacaoDemandaDireta);
        }

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        return quantidade * unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getMaterial(),
                getUnidadeMedida(parametrosGlobais),
                unidadeMedidaTarget);

    }

    public void setQuantidadeOriginal(
            double quantidade,
            UnidadeMedida unidadeMedidaQuantidade,
            TipoDemandaDireta tipoDemandaDireta,
            PropagacaoDemandaDireta propagacaoDemandaDireta,
            UnidadeMedidaProjection unidadeMedidaProjection) {
        updateQuantidadeOriginal(
                valorAnterior -> quantidade,
                unidadeMedidaQuantidade,
                tipoDemandaDireta,
                propagacaoDemandaDireta,
                unidadeMedidaProjection);
    }

    public void addQuantidadeOriginal(
            double quantidade,
            UnidadeMedida unidadeMedidaQuantidade,
            TipoDemandaDireta tipoDemandaDireta,
            PropagacaoDemandaDireta propagacaoDemandaDireta,
            UnidadeMedidaProjection unidadeMedidaProjection) {
        updateQuantidadeOriginal(
                valorAnterior -> valorAnterior + quantidade,
                unidadeMedidaQuantidade,
                tipoDemandaDireta,
                propagacaoDemandaDireta,
                unidadeMedidaProjection);
    }

    public void updateQuantidadeOriginal(
            DoubleUnaryOperator funcaoAtualizacaoQuantidadeAtual,
            UnidadeMedida unidadeMedidaAjuste,
            TipoDemandaDireta tipoDemandaDireta,
            PropagacaoDemandaDireta propagacaoDemandaDireta,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeDemandaDiretaConsiderada = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getMaterial(),
                unidadeMedidaAjuste,
                getUnidadeMedida(parametrosGlobais));

        switch (tipoDemandaDireta) {
            case PLANO_DEMANDA:
                switch (propagacaoDemandaDireta) {
                    case NAO_PROPAGADA:
                        setQuantidadePlanoDemandaOriginal(
                                funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getQuantidadePlanoDemandaOriginal())
                                * conversaoParaUnidadeDemandaDiretaConsiderada);
                        return;
                    case PROPAGADA:
                        setQuantidadePlanoDemandaOriginalPropagadaLocationInterna(
                                funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getQuantidadePlanoDemandaOriginalPropagadaLocationInterna())
                                * conversaoParaUnidadeDemandaDiretaConsiderada);
                        return;
                    default:
                        throw unsupportedCombination("setQuantidadeOriginal", tipoDemandaDireta, propagacaoDemandaDireta);
                }
            case CARTEIRA:
                switch (propagacaoDemandaDireta) {
                    case NAO_PROPAGADA:
                        setQuantidadeCarteiraOriginal(
                                funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getQuantidadeCarteiraOriginal())
                                * conversaoParaUnidadeDemandaDiretaConsiderada);
                        return;
                    case PROPAGADA:
                        setQuantidadeCarteiraOriginalPropagadaLocationInterna(
                                funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getQuantidadeCarteiraOriginalPropagadaLocationInterna())
                                * conversaoParaUnidadeDemandaDiretaConsiderada);
                        return;
                    default:
                        throw unsupportedCombination("setQuantidadeOriginal", tipoDemandaDireta, propagacaoDemandaDireta);
                }
            default:
                throw unsupportedCombination("setQuantidadeOriginal", tipoDemandaDireta);
        }

    }

    public void setValorOriginal(
            double valor,
            TipoDemandaDireta tipoDemandaDireta,
            PropagacaoDemandaDireta propagacaoDemandaDireta,
            ValorDemandaDireta valorDemandaDireta) {
        updateValorOriginal(
                valorAnterior -> valor,
                tipoDemandaDireta,
                propagacaoDemandaDireta,
                valorDemandaDireta);
    }

    public void addValorOriginal(
            double valor,
            TipoDemandaDireta tipoDemandaDireta,
            PropagacaoDemandaDireta propagacaoDemandaDireta,
            ValorDemandaDireta valorDemandaDiretao) {
        updateValorOriginal(
                valorAnterior -> valorAnterior + valor,
                tipoDemandaDireta,
                propagacaoDemandaDireta,
                valorDemandaDiretao);
    }

    public void updateValorOriginal(
            DoubleUnaryOperator funcaoAtualizacaoQuantidadeAtual,
            TipoDemandaDireta tipoDemandaDireta,
            PropagacaoDemandaDireta propagacaoDemandaDireta,
            ValorDemandaDireta valorDemandaDireta) {
        switch (tipoDemandaDireta) {
            case PLANO_DEMANDA:
                switch (propagacaoDemandaDireta) {
                    case NAO_PROPAGADA:
                        switch(valorDemandaDireta) {
                            case GROSS:
                                setGrossSalesPlanoDemandaOriginal(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getGrossSalesPlanoDemandaOriginal()));
                                return;
                            case IMPOSTOS:
                                setCustoImpostosPlanoDemandaOriginal(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getCustoImpostosPlanoDemandaOriginal()));
                                return;
                            case FRETE:
                                setCustoFretePlanoDemandaOriginal(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getCustoFretePlanoDemandaOriginal()));
                                return;
                            default:
                                throw unsupportedCombination("setValorOriginal", tipoDemandaDireta, propagacaoDemandaDireta, valorDemandaDireta);
                        }
                    case PROPAGADA:
                        switch(valorDemandaDireta) {
                            case GROSS:
                                setGrossSalesPlanoDemandaOriginalPropagadaLocationInterna(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getGrossSalesPlanoDemandaOriginalPropagadaLocationInterna()));
                                return;
                            case IMPOSTOS:
                                setCustoImpostosPlanoDemandaOriginalPropagadaLocationInterna(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getCustoImpostosPlanoDemandaOriginalPropagadaLocationInterna()));
                                return;
                            case FRETE:
                                setCustoFretePlanoDemandaOriginalPropagadaLocationInterna(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getCustoFretePlanoDemandaOriginalPropagadaLocationInterna()));
                                return;
                            default:
                                throw unsupportedCombination("setValorOriginal", tipoDemandaDireta, propagacaoDemandaDireta, valorDemandaDireta);
                        }
                }
            case CARTEIRA:
                switch (propagacaoDemandaDireta) {
                    case NAO_PROPAGADA:
                        switch(valorDemandaDireta) {
                            case GROSS:
                                setGrossSalesCarteiraOriginal(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getGrossSalesCarteiraOriginal()));
                                return;
                            case IMPOSTOS:
                                setCustoImpostosCarteiraOriginal(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getCustoImpostosCarteiraOriginal()));
                                return;
                            case FRETE:
                                setCustoFreteCarteiraOriginal(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getCustoFreteCarteiraOriginal()));
                                return;
                            default:
                                throw unsupportedCombination("setValorOriginal", tipoDemandaDireta, propagacaoDemandaDireta, valorDemandaDireta);
                        }
                    case PROPAGADA:
                        switch(valorDemandaDireta) {
                            case GROSS:
                                setGrossSalesCarteiraOriginalPropagadaLocationInterna(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getGrossSalesCarteiraOriginalPropagadaLocationInterna()));
                                return;
                            case IMPOSTOS:
                                setCustoImpostosCarteiraOriginalPropagadaLocationInterna(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getCustoImpostosCarteiraOriginalPropagadaLocationInterna()));
                                return;
                            case FRETE:
                                setCustoFreteCarteiraOriginalPropagadaLocationInterna(
                                        funcaoAtualizacaoQuantidadeAtual.applyAsDouble(getCustoFreteCarteiraOriginalPropagadaLocationInterna()));
                                return;
                            default:
                                throw unsupportedCombination("setValorOriginal", tipoDemandaDireta, propagacaoDemandaDireta, valorDemandaDireta);
                        }
                    default:
                        throw unsupportedCombination("setValorOriginal", tipoDemandaDireta, propagacaoDemandaDireta);
                }
            default:
                throw unsupportedCombination("setValorOriginal", tipoDemandaDireta);
        }
    }

    private double getQuantidadePlanoDemandaTotal() {
        return getQuantidadePlanoDemandaOriginal() + getQuantidadePlanoDemandaOriginalPropagadaLocationInterna();
    }

    private double getQuantidadeCarteiraTotal() {
        return getQuantidadeCarteiraOriginal() + getQuantidadeCarteiraOriginalPropagadaLocationInterna();
    }

    private double getValorPlanoDemandaTotal(ValorDemandaDireta valorDemandaDireta) {
        return getValorOriginal(TipoDemandaDireta.PLANO_DEMANDA, PropagacaoDemandaDireta.TOTAL, valorDemandaDireta);
    }

    private double getValorCarteiraTotal(ValorDemandaDireta valorDemandaDireta) {
        return getValorOriginal(TipoDemandaDireta.CARTEIRA, PropagacaoDemandaDireta.TOTAL, valorDemandaDireta);
    }

    private boolean verificaSePeriodoDaLinhaEPeriodoPresente(Calendario calendario) {
        int posicaoPeriodoPresente = calendario.getPosicaoPeriodoPresente();
        int posicaoPeriodoLinha = calendario.getPosicaoPeriodo(getDataReferencia());
        return posicaoPeriodoLinha == posicaoPeriodoPresente;
    }

    private double getQuantidadeCarteiraNoPeriodoPresente(Calendario calendario) {
        return verificaSePeriodoDaLinhaEPeriodoPresente(calendario) ? getQuantidadeCarteiraTotal() : 0.0;
    }

    private double getValorCarteiraNoPeriodoPresente(ValorDemandaDireta valorDemandaDireta, Calendario calendario) {
        return verificaSePeriodoDaLinhaEPeriodoPresente(calendario) ? getValorCarteiraTotal(valorDemandaDireta) : 0.0;
    }

    private void atualizaDemandaDiretaIrrestritaComComponentes(
            double quantidadePlanoDemandaOriginal,
            double quantidadeCarteiraOriginal,
            double grossPlanoDemanda,
            double grossCarteira,
            double impostosPlanoDemanda,
            double impostosCarteira,
            double fretePlanoDemanda,
            double freteCarteira) {

        setQuantidadeDemandaDiretaPlanoDemandaIrrestrita(quantidadePlanoDemandaOriginal);
        setQuantidadeDemandaDiretaCarteiraIrrestrita(quantidadeCarteiraOriginal);
        setGrossSalesDemandaDiretaPlanoDemandaIrrestrita(grossPlanoDemanda);
        setGrossSalesDemandaDiretaCarteiraIrrestrita(grossCarteira);
        setCustoImpostosDemandaDiretaPlanoDemandaIrrestrita(impostosPlanoDemanda);
        setCustoImpostosDemandaDiretaCarteiraIrrestrita(impostosCarteira);
        setCustoFreteDemandaDiretaPlanoDemandaIrrestrita(fretePlanoDemanda);
        setCustoFreteDemandaDiretaCarteiraIrrestrita(freteCarteira);
    }

    private void atualizaDemandaDiretaIrrestritaComPlanoDemanda() {
        atualizaDemandaDiretaIrrestritaComComponentes(
                getQuantidadePlanoDemandaTotal(),
                0.0,
                getValorPlanoDemandaTotal(ValorDemandaDireta.GROSS),
                0.0,
                getValorPlanoDemandaTotal(ValorDemandaDireta.IMPOSTOS),
                0.0,
                getValorPlanoDemandaTotal(ValorDemandaDireta.FRETE),
                0.0);
    }

    private void atualizaDemandaDiretaIrrestritaComCarteira() {
        atualizaDemandaDiretaIrrestritaComComponentes(
                0.0,
                getQuantidadeCarteiraTotal(),
                0.0,
                getValorCarteiraTotal(ValorDemandaDireta.GROSS),
                0.0,
                getValorCarteiraTotal(ValorDemandaDireta.IMPOSTOS),
                0.0,
                getValorCarteiraTotal(ValorDemandaDireta.FRETE));
    }

    private void atualizaDemandaDiretaIrrestritaComPlanoDemandaEMaisCarteiraBacklog(Calendario calendario) {
        atualizaDemandaDiretaIrrestritaComComponentes(
                getQuantidadePlanoDemandaTotal(),
                getQuantidadeCarteiraNoPeriodoPresente(calendario),
                getValorPlanoDemandaTotal(ValorDemandaDireta.GROSS),
                getValorCarteiraNoPeriodoPresente(ValorDemandaDireta.GROSS, calendario),
                getValorPlanoDemandaTotal(ValorDemandaDireta.IMPOSTOS),
                getValorCarteiraNoPeriodoPresente(ValorDemandaDireta.IMPOSTOS, calendario),
                getValorPlanoDemandaTotal(ValorDemandaDireta.FRETE),
                getValorCarteiraNoPeriodoPresente(ValorDemandaDireta.FRETE, calendario));
    }

    private void atualizaDemandaDiretaIrrestritaComPlanoDemandaEMaisCarteira() {
        atualizaDemandaDiretaIrrestritaComComponentes(
                getQuantidadePlanoDemandaTotal(),
                getQuantidadeCarteiraTotal(),
                getValorPlanoDemandaTotal(ValorDemandaDireta.GROSS),
                getValorCarteiraTotal(ValorDemandaDireta.GROSS),
                getValorPlanoDemandaTotal(ValorDemandaDireta.IMPOSTOS),
                getValorCarteiraTotal(ValorDemandaDireta.IMPOSTOS),
                getValorPlanoDemandaTotal(ValorDemandaDireta.FRETE),
                getValorCarteiraTotal(ValorDemandaDireta.FRETE));
    }

    private void atualizaDemandaDiretaEstoqueSegurancaComPlanoDemanda() {
        setQuantidadeDemandaDiretaEstoqueSeguranca(getQuantidadePlanoDemandaTotal());
    }

    private void atualizaDemandaDiretaEstoqueSegurancaComCarteira() {
        setQuantidadeDemandaDiretaEstoqueSeguranca(getQuantidadeCarteiraTotal());
    }

    private void atualizaDemandaDiretaEstoqueSegurancaComPlanoDemandaEMaisCarteiraBacklog(Calendario calendario) {
        setQuantidadeDemandaDiretaEstoqueSeguranca(getQuantidadePlanoDemandaTotal() + getQuantidadeCarteiraNoPeriodoPresente(calendario));
    }

    private void atualizaDemandaDiretaEstoqueSegurancaComPlanoDemandaEMaisCarteira() {
        setQuantidadeDemandaDiretaEstoqueSeguranca(getQuantidadePlanoDemandaTotal() + getQuantidadeCarteiraTotal());
    }

    public void atualizaPlanoIrrestritoComQuantidadesOriginaisEMajoracaoPerfilExecucao(
            PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta modeloMajoracaoDemandaDiretaProjecaoEstoques,
            PerfilExecucaoSupplyPlan.ModeloMajoracaoDemandaDireta modeloMajoracaoDemandaDiretaEstoqueSeguranca,
            Calendario calendario,
            @Nullable Integer periodoFinalCarteiraEmHorizonteFixo) {

        switch (modeloMajoracaoDemandaDiretaProjecaoEstoques) {
            case SOMENTE_FORECAST:
                atualizaDemandaDiretaIrrestritaComPlanoDemanda();
                break;
            case SOMENTE_PEDIDOS_CLIENTES:
                atualizaDemandaDiretaIrrestritaComCarteira();
                break;
            case MAIOR_POR_PERIODO:
                if (getQuantidadePlanoDemandaTotal() > getQuantidadeCarteiraTotal()) {
                    atualizaDemandaDiretaIrrestritaComPlanoDemanda();
                } else {
                    atualizaDemandaDiretaIrrestritaComCarteira();
                }
                break;
            case CARTEIRA_EM_HORIZONTE_FIXO:
                // usa o forecast
                if (periodoFinalCarteiraEmHorizonteFixo == null || calendario.getPosicaoPeriodo(getDataReferencia()) > periodoFinalCarteiraEmHorizonteFixo) {
                    atualizaDemandaDiretaIrrestritaComPlanoDemanda();
                    // usa a carteira
                } else {
                    atualizaDemandaDiretaIrrestritaComCarteira();
                }
                break;
            case PLANO_DEMANDA_MAIS_CARTEIRA:
                atualizaDemandaDiretaIrrestritaComPlanoDemandaEMaisCarteira();
                break;
            default:
                throw unsupportedCombination(
                        "atualizaDemandaDiretaIrrestritaEEstoqueSeguranca",
                        modeloMajoracaoDemandaDiretaProjecaoEstoques);
        }

        switch (modeloMajoracaoDemandaDiretaEstoqueSeguranca) {
            case SOMENTE_FORECAST:
                atualizaDemandaDiretaEstoqueSegurancaComPlanoDemanda();
                break;
            case SOMENTE_PEDIDOS_CLIENTES:
                atualizaDemandaDiretaEstoqueSegurancaComCarteira();
                break;
            case MAIOR_POR_PERIODO:
                if (getQuantidadePlanoDemandaTotal() > getQuantidadeCarteiraTotal()) {
                    atualizaDemandaDiretaEstoqueSegurancaComPlanoDemanda();
                } else {
                    atualizaDemandaDiretaEstoqueSegurancaComCarteira();
                }
                break;
            case CARTEIRA_EM_HORIZONTE_FIXO:
                // usa o forecast
                if (periodoFinalCarteiraEmHorizonteFixo == null || calendario.getPosicaoPeriodo(getDataReferencia()) > periodoFinalCarteiraEmHorizonteFixo) {
                    atualizaDemandaDiretaEstoqueSegurancaComPlanoDemanda();
                    // usa a carteira
                } else {
                    atualizaDemandaDiretaEstoqueSegurancaComCarteira();
                }
                break;
            case PLANO_DEMANDA_MAIS_CARTEIRA:
                atualizaDemandaDiretaEstoqueSegurancaComPlanoDemandaEMaisCarteira();
                break;
            default:
                throw unsupportedCombination(
                        "atualizaDemandaDiretaIrrestritaEEstoqueSeguranca",
                        modeloMajoracaoDemandaDiretaEstoqueSeguranca);
        }

    }

    private IllegalArgumentException unsupportedTipoPlano(
            String operationName,
            TipoPlano tipoPlano) {

        return SupplyPlanningDataContract.unsupportedTipoPlano(
                DemandaDiretaConsideradaLinha.class,
                operationName,
                tipoPlano,
                TipoPlano.PLANO_IRRESTRITO,
                TipoPlano.PLANO_RESTRITO,
                TipoPlano.PLANO_TRABALHO);

    }

    private IllegalArgumentException unsupportedCombination(
            String operationName,
            Object... receivedValueArray) {

        return SupplyPlanningDataContract.unsupportedEnumValue(
                DemandaDiretaConsideradaLinha.class,
                operationName,
                "combination",
                Arrays.toString(receivedValueArray),
                "valid Community direct-demand combinations",
                "Community stores Demand Plan and client-orders segments explicitly; TOTAL is derived and unsupported enum branches must fail before mutating the line.");

    }

}
