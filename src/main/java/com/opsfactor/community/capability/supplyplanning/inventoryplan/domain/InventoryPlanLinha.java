package com.opsfactor.community.capability.supplyplanning.inventoryplan.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlanningDataContract;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Linha de estoque projetado, safety stock e estoque maximo do Supply Plan.
 *
 * <p>O Community usa esta entidade em nivel material/location para planos
 * irrestrito, restrito e de trabalho. Campos `Baseline` antigos permanecem
 * apenas para leitura de bases migradas e nao representam novas colunas
 * funcionais do contrato aberto.</p>
 */
@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@EqualsAndHashCode(of="inventoryPlanLinhaCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class InventoryPlanLinha {

    /**
     * Chave composta de ReplenishmentPlan
     */
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    @EmbeddedId
    private InventoryPlanLinhaCompositeKey inventoryPlanLinhaCompositeKey;

    /**
     * Chave composta de ReplenishmentPlanLinha
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class InventoryPlanLinhaCompositeKey implements Serializable {

        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        @ManyToOne(optional = false)
        private SupplyPlan supplyPlan;

        /**
         * Location do estoque
         */
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Location location;
                        
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        @ManyToOne(optional = false)
        private Produto produto;
        
        /**
         * Último segundo do período
         */
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private LocalDateTime dataReferencia;
        
    }
    
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedida;
    
    // estoque de segurança e estoque máximo : irrestrito
    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeEstoqueSegurancaBaseline; // Compatibilidade legado: campo antigo ainda pode existir em bases migradas.
    private Double quantidadeEstoqueSegurancaIrrestrito;

    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeEstoqueMaximoBaseline; // Compatibilidade legado: campo antigo ainda pode existir em bases migradas.
    private Double quantidadeEstoqueMaximoIrrestrito;

    // estoque de segurança : restrito
    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeEstoqueSegurancaRestritoBaseline; // Compatibilidade legado: campo antigo ainda pode existir em bases migradas.
    private Double quantidadeEstoqueSegurancaRestrito;

    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeEstoqueMaximoRestritoBaseline; // Compatibilidade legado: campo antigo ainda pode existir em bases migradas.
    private Double quantidadeEstoqueMaximoRestrito;

    // estoque projetado
    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeEstoqueBaseline; // Compatibilidade legado: campo antigo ainda pode existir em bases migradas.
    private Double quantidadeEstoqueProjetadoIrrestrito;

    // estoque projetado com as restrições de abastecimento ao longo da cadeia
    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeEstoqueRestritoBaseline; // Compatibilidade legado: campo antigo ainda pode existir em bases migradas.
    private Double quantidadeEstoqueProjetadoRestrito;

    // estoque da versão de trabalho do plano
    private Double quantidadeEstoqueProjetadoTrabalho;

    // valor derivado de remessas em aberto. não deduz o estoque da origem, 
    // portanto não é salvo no distribution plan linha
    // CONSIDERADO NOS CÁLCULOS COMO ENTRADA BASELINE, NÃO COMO ESTOQUE
    private Double quantidadeEstoqueTransitoInbound;
    
    public Location getLocation() {
        return inventoryPlanLinhaCompositeKey.getLocation();
    }
    
    public Produto getProduto() {
        return inventoryPlanLinhaCompositeKey.getProduto();
    }
    
    public UnidadeMedida getUnidadeMedida(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedida == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedida;
    }
    
    public SupplyPlan getSupplyPlan() {
        return inventoryPlanLinhaCompositeKey.getSupplyPlan();
    }
    
    public LocalDateTime getDataReferencia() {
        return inventoryPlanLinhaCompositeKey.getDataReferencia();
    }


    public double getEstoqueMaximoIrrestrito() {

        double estoqueSegurancaIrrestrito = getQuantidadeEstoqueSegurancaIrrestrito();

        // Compatibilidade legado: campo antigo ainda pode existir em bases migradas.
        double estoqueMaximoAntigoConsiderado = Math.max(
                getQuantidadeInventoryPlanNaoNegativaOuZero(
                        quantidadeEstoqueMaximoBaseline,
                        "legacy unrestricted maximum stock"),
                estoqueSegurancaIrrestrito);

        double estoqueMaximoConsiderado = Math.max(
                getQuantidadeEstoqueMaximoIrrestrito(),
                estoqueSegurancaIrrestrito);

        return Math.max(estoqueMaximoAntigoConsiderado, estoqueMaximoConsiderado);
    }

    public double getEstoqueMaximoRestrito() {

        double estoqueSegurancaRestrito = getQuantidadeEstoqueSegurancaRestrito();

        // Compatibilidade legado: campo antigo ainda pode existir em bases migradas.
        double estoqueMaximoAntigoConsiderado = Math.max(
                getQuantidadeInventoryPlanNaoNegativaOuZero(
                        quantidadeEstoqueMaximoRestritoBaseline,
                        "legacy restricted maximum stock"),
                estoqueSegurancaRestrito);

        double estoqueMaximoConsiderado = Math.max(
                getQuantidadeEstoqueMaximoRestrito(),
                estoqueSegurancaRestrito);

        return Math.max(estoqueMaximoAntigoConsiderado, estoqueMaximoConsiderado);
    }

    public double getQuantidadeEstoqueProjetadoIrrestrito() {
        return ((quantidadeEstoqueBaseline == null) ? 0 : quantidadeEstoqueBaseline) + // Compatibilidade legado: campo antigo ainda pode existir em bases migradas.
                ((quantidadeEstoqueProjetadoIrrestrito == null) ? 0 : quantidadeEstoqueProjetadoIrrestrito);
    }

    public double getQuantidadeEstoqueProjetadoTrabalho() {
        return (quantidadeEstoqueProjetadoTrabalho == null) ? 0 : quantidadeEstoqueProjetadoTrabalho;
    }

    public double getQuantidadeEstoqueProjetadoRestrito() {
        return ((quantidadeEstoqueRestritoBaseline == null) ? 0 : quantidadeEstoqueRestritoBaseline) + // Compatibilidade legado: campo antigo ainda pode existir em bases migradas.
                ((quantidadeEstoqueProjetadoRestrito == null) ? 0 : quantidadeEstoqueProjetadoRestrito);
    }

    public double getQuantidadeEstoqueSegurancaIrrestrito() {
        return getQuantidadeInventoryPlanNaoNegativaOuZero(
                quantidadeEstoqueSegurancaBaseline,
                "legacy unrestricted safety stock")
                + getQuantidadeInventoryPlanNaoNegativaOuZero(
                quantidadeEstoqueSegurancaIrrestrito,
                "unrestricted safety stock");
    }

    public double getQuantidadeEstoqueSegurancaRestrito() {
        return getQuantidadeInventoryPlanNaoNegativaOuZero(
                quantidadeEstoqueSegurancaRestritoBaseline,
                "legacy restricted safety stock")
                + getQuantidadeInventoryPlanNaoNegativaOuZero(
                quantidadeEstoqueSegurancaRestrito,
                "restricted safety stock");
    }

    public double getQuantidadeEstoqueMaximoIrrestrito() {
        return getQuantidadeInventoryPlanNaoNegativaOuZero(
                quantidadeEstoqueMaximoIrrestrito,
                "unrestricted maximum stock");
    }

    public double getQuantidadeEstoqueMaximoRestrito() {
        return getQuantidadeInventoryPlanNaoNegativaOuZero(
                quantidadeEstoqueMaximoRestrito,
                "restricted maximum stock");
    }

    public double getQuantidadeEstoqueTransitoInbound() {
        return getQuantidadeInventoryPlanNaoNegativaOuZero(
                quantidadeEstoqueTransitoInbound,
                "inbound transit stock");
    }

    /**
     * Transfere as parcelas baseline historicas para as colunas canônicas da
     * mesma linha durante um cutover offline, preservando exatamente o valor
     * efetivo que os getters expunham antes da limpeza.
     *
     * <p>Safety stock e estoque projetado eram parcelas aditivas. Estoque
     * máximo, por outro lado, já era exposto como o maior entre baseline,
     * valor canônico e safety stock. Por isso o destino recebe o valor efetivo
     * correspondente antes de a origem ser anulada. O método não escolhe
     * outra série e deve ser chamado somente pelo executor transacional que
     * tomou lock pessimista no envelope selecionado.</p>
     *
     * @return quantidade de colunas baseline anuladas nesta linha.
     */
    public int transferLegacyBaselineToCanonicalFields() {

        double safetyStockUnconstrained = getQuantidadeEstoqueSegurancaIrrestrito();
        double maximumStockUnconstrained = getEstoqueMaximoIrrestrito();
        double safetyStockConstrained = getQuantidadeEstoqueSegurancaRestrito();
        double maximumStockConstrained = getEstoqueMaximoRestrito();
        double projectedStockUnconstrained = getQuantidadeEstoqueProjetadoIrrestrito();
        double projectedStockConstrained = getQuantidadeEstoqueProjetadoRestrito();
        int clearedLegacyColumnCount = 0;

        if (quantidadeEstoqueSegurancaBaseline != null) {
            quantidadeEstoqueSegurancaIrrestrito = safetyStockUnconstrained;
            quantidadeEstoqueSegurancaBaseline = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadeEstoqueMaximoBaseline != null) {
            quantidadeEstoqueMaximoIrrestrito = maximumStockUnconstrained;
            quantidadeEstoqueMaximoBaseline = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadeEstoqueSegurancaRestritoBaseline != null) {
            quantidadeEstoqueSegurancaRestrito = safetyStockConstrained;
            quantidadeEstoqueSegurancaRestritoBaseline = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadeEstoqueMaximoRestritoBaseline != null) {
            quantidadeEstoqueMaximoRestrito = maximumStockConstrained;
            quantidadeEstoqueMaximoRestritoBaseline = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadeEstoqueBaseline != null) {
            quantidadeEstoqueProjetadoIrrestrito = projectedStockUnconstrained;
            quantidadeEstoqueBaseline = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadeEstoqueRestritoBaseline != null) {
            quantidadeEstoqueProjetadoRestrito = projectedStockConstrained;
            quantidadeEstoqueRestritoBaseline = null;
            clearedLegacyColumnCount++;
        }

        return clearedLegacyColumnCount;

    }

    /**
     * Valida quantidades auxiliares persistidas no Inventory Plan antes de
     * somas entre campos legados/novos, conversoes de unidade ou exposicao
     * para Planning Book.
     *
     * <p>Null continua ausencia operacional zero. Valor presente negativo ou
     * nao finito em safety stock, estoque maximo ou estoque em transito indica
     * snapshot fisico quebrado e nao deve virar zero silencioso.</p>
     */
    private double getQuantidadeInventoryPlanNaoNegativaOuZero(
            Number quantidadeInventoryPlan,
            String nomeCampoQuantidade) {

        if (quantidadeInventoryPlan == null) {
            return 0.0d;
        }

        double valorQuantidadeInventoryPlan = quantidadeInventoryPlan.doubleValue();
        if (!Double.isFinite(valorQuantidadeInventoryPlan)
                || valorQuantidadeInventoryPlan < 0.0d) {
            throw new IllegalStateException(
                    "Inventory plan quantity "
                            + nomeCampoQuantidade
                            + " must be finite and non-negative for "
                            + getContextoLinhaParaMensagem()
                            + ": "
                            + valorQuantidadeInventoryPlan
                            + ".");
        }
        return valorQuantidadeInventoryPlan;

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

        if (inventoryPlanLinhaCompositeKey == null
                || inventoryPlanLinhaCompositeKey.getProduto() == null
                || inventoryPlanLinhaCompositeKey.getProduto().getId() == null) {
            return "<sem-material>";
        }
        return inventoryPlanLinhaCompositeKey.getProduto().getId();

    }

    private String getLocationIdParaMensagem() {

        if (inventoryPlanLinhaCompositeKey == null
                || inventoryPlanLinhaCompositeKey.getLocation() == null
                || inventoryPlanLinhaCompositeKey.getLocation().getId() == null) {
            return "<sem-location>";
        }
        return inventoryPlanLinhaCompositeKey.getLocation().getId();

    }

    private String getDataReferenciaParaMensagem() {

        if (inventoryPlanLinhaCompositeKey == null
                || inventoryPlanLinhaCompositeKey.getDataReferencia() == null) {
            return "<sem-data>";
        }
        return inventoryPlanLinhaCompositeKey.getDataReferencia().toString();

    }

    public double getQuantidadeEstoqueProjetado(Constantes.TipoPlano tipoPlano) {

        switch(tipoPlano) {
            case PLANO_IRRESTRITO:
                return getQuantidadeEstoqueProjetadoIrrestrito();
            case PLANO_RESTRITO:
                return getQuantidadeEstoqueProjetadoRestrito();
            case PLANO_TRABALHO:
                return getQuantidadeEstoqueProjetadoTrabalho();
            default:
                throw unsupportedTipoPlano("getQuantidadeEstoqueProjetado", tipoPlano);
        }

    }

    public void setQuantidadeEstoqueProjetado(
            double valor,
            Constantes.TipoPlano tipoPlano) {

        switch(tipoPlano) {
            case PLANO_IRRESTRITO:
                setQuantidadeEstoqueProjetadoIrrestrito(valor);
                return;
            case PLANO_RESTRITO:
                setQuantidadeEstoqueProjetadoRestrito(valor);
                return;
            case PLANO_TRABALHO:
                setQuantidadeEstoqueProjetadoTrabalho(valor);
                return;
            default:
                throw unsupportedTipoPlano("setQuantidadeEstoqueProjetado", tipoPlano);
        }

    }

    public void setQuantidadeEstoqueProjetadoEmUnidadeMedida(
            double valor,
            UnidadeMedida unidadeMedidaValor,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaInventoryPlanLinha = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                unidadeMedidaValor,
                getUnidadeMedida(parametrosGlobais));

        setQuantidadeEstoqueProjetado(valor * conversaoParaUnidadeMedidaInventoryPlanLinha, tipoPlano);

    }

    public double getQuantidadeEstoqueProjetadoNaUnidadeTarget(
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaTarget = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                getUnidadeMedida(parametrosGlobais),
                unidadeMedidaTarget);

        return getQuantidadeEstoqueProjetado(tipoPlano) * conversaoParaUnidadeMedidaTarget;

    }

    /**
     * Retorna o estoque de segurança (minimo) em quantidade na unidade de medida target
     * No caso de modelo kanban se retorna o estoque target kanban
     */
    public double getQuantidadeEstoqueSeguranca(Constantes.TipoPlano tipoPlano) {

        switch(tipoPlano) {
            case PLANO_IRRESTRITO:
                return getQuantidadeEstoqueSegurancaIrrestrito();
            case PLANO_RESTRITO:
                return getQuantidadeEstoqueSegurancaRestrito();
            case PLANO_TRABALHO:
                return getQuantidadeEstoqueSegurancaIrrestrito(); // retorna o plano irrestrito
            default:
                throw unsupportedTipoPlano("getQuantidadeEstoqueSeguranca", tipoPlano);
        }

    }

    /**
     * Retorna o estoque de segurança (minimo) em quantidade na unidade de medida target
     * No caso de modelo kanban se retorna o estoque target kanban
     */
    public double getQuantidadeEstoqueSegurancaNaUnidadeTarget(
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaTarget = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                getUnidadeMedida(parametrosGlobais),
                unidadeMedidaTarget);

        return getQuantidadeEstoqueSeguranca(tipoPlano) * conversaoParaUnidadeMedidaTarget;

    }

    public void setQuantidadeEstoqueSeguranca(
            double valor,
            Constantes.TipoPlano tipoPlano) {

        switch(tipoPlano) {
            case PLANO_IRRESTRITO:
                setQuantidadeEstoqueSegurancaIrrestrito(valor);
                return;
            case PLANO_RESTRITO:
                setQuantidadeEstoqueSegurancaRestrito(valor);
                return;
            case PLANO_TRABALHO:
                // O work plan reutiliza a mesma coluna persistida de safety stock usada no irrestrito.
                setQuantidadeEstoqueSegurancaIrrestrito(valor);
                return;
            default:
                throw unsupportedTipoPlano("setQuantidadeEstoqueSeguranca", tipoPlano);
        }

    }

    public void setQuantidadeEstoqueSegurancaEmUnidadeMedida(
            double valor,
            UnidadeMedida unidadeMedidaValor,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaInventoryPlanLinha = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                unidadeMedidaValor,
                getUnidadeMedida(parametrosGlobais));

        setQuantidadeEstoqueSeguranca(valor * conversaoParaUnidadeMedidaInventoryPlanLinha, tipoPlano);

    }

    public double getQuantidadeEstoqueMaximo(Constantes.TipoPlano tipoPlano) {

        switch(tipoPlano) {
            case PLANO_IRRESTRITO:
                return getEstoqueMaximoIrrestrito();
            case PLANO_RESTRITO:
                return getQuantidadeEstoqueMaximoRestrito();
            case PLANO_TRABALHO:
                return getEstoqueMaximoIrrestrito(); // retorna o plano irrestrito
            default:
                throw unsupportedTipoPlano("getQuantidadeEstoqueMaximo", tipoPlano);
        }

    }

    /**
     * Retorna o estoque maximo em quantidade na unidade de medida target
     * Caso o estoque máximo não exista (ex. não cadastrado ou modelo kanban) se retorna o estoque de segurança
     */
    public double getQuantidadeEstoqueMaximoNaUnidadeTarget(
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaTarget = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                getUnidadeMedida(parametrosGlobais),
                unidadeMedidaTarget);

        return getQuantidadeEstoqueMaximo(tipoPlano) * conversaoParaUnidadeMedidaTarget;

    }

    public void setQuantidadeEstoqueMaximo(
            double valor,
            Constantes.TipoPlano tipoPlano) {

        switch(tipoPlano) {
            case PLANO_IRRESTRITO:
                setQuantidadeEstoqueMaximoIrrestrito(valor);
                return;
            case PLANO_RESTRITO:
                setQuantidadeEstoqueMaximoRestrito(valor);
                return;
            case PLANO_TRABALHO:
                // O work plan reutiliza a mesma coluna persistida de estoque maximo usada no irrestrito.
                setQuantidadeEstoqueMaximoIrrestrito(valor);
                return;
            default:
                throw unsupportedTipoPlano("setQuantidadeEstoqueMaximo", tipoPlano);
        }

    }

    public void setQuantidadeEstoqueMaximoEmUnidadeMedida(
            double valor,
            UnidadeMedida unidadeMedidaValor,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaInventoryPlanLinha = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                unidadeMedidaValor,
                getUnidadeMedida(parametrosGlobais));

        setQuantidadeEstoqueMaximo(valor * conversaoParaUnidadeMedidaInventoryPlanLinha, tipoPlano);

    }

    public double getQuantidadeEstoqueTransitoInboundNaUnidadeTarget(
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaTarget = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                getUnidadeMedida(parametrosGlobais),
                unidadeMedidaTarget);

        return getQuantidadeEstoqueTransitoInbound() * conversaoParaUnidadeMedidaTarget;

    }

    public void setQuantidadeEstoqueTransitoInboundEmUnidadeMedida(
            double valor,
            UnidadeMedida unidadeMedidaValor,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaInventoryPlanLinha = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                unidadeMedidaValor,
                getUnidadeMedida(parametrosGlobais));

        setQuantidadeEstoqueTransitoInbound(valor * conversaoParaUnidadeMedidaInventoryPlanLinha);

    }

    private IllegalArgumentException unsupportedTipoPlano(
            String operationName,
            Constantes.TipoPlano tipoPlano) {

        return SupplyPlanningDataContract.unsupportedTipoPlano(
                InventoryPlanLinha.class,
                operationName,
                tipoPlano,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                Constantes.TipoPlano.PLANO_RESTRITO,
                Constantes.TipoPlano.PLANO_TRABALHO);

    }

}
