package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Regra operacional de politica de estoque para uma combinacao material/location.
 *
 * <p>Esta entidade fica no Community porque o Supply Planning heuristico usa
 * estes parametros para safety stock, DRP e Kanban simples. O mesmo schema
 * historico ainda possui campos que foram reaproveitados pela otimizacao de
 * politica de estoques; quando isso acontece, o getter/DTO Community precisa
 * deixar claro se o campo e operacional ou apenas transicional.</p>
 */
@Getter
@Setter
@Entity
@NoArgsConstructor 
@RequiredArgsConstructor
@EqualsAndHashCode(of = "politicaEstoquesMaterialLocationCompositeKey")
public class PoliticaEstoquesMaterialLocation implements Serializable {

    @EmbeddedId 
    @NonNull
    private PoliticaEstoquesMaterialLocationCompositeKey politicaEstoquesMaterialLocationCompositeKey;

    /**
     * Chave composta da regra operacional. A politica define a vigencia e a
     * prioridade; material e location definem a DFU operacional do safety
     * stock.
     */
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @Embeddable
    @AllArgsConstructor 
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class PoliticaEstoquesMaterialLocationCompositeKey implements Serializable {

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private PoliticaEstoques politicaEstoques;
        
        @NonNull
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private Produto material;

        @NonNull
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private Location location;

    }

    /**
     * Modelo operacional de reposicao usado pelo heuristico Community.
     */
    @Enumerated(EnumType.ORDINAL) // DRP, KANBAN
    private Constantes.SNPModeloReabastecimento modeloReabastecimento; // Kanban ou Safety Stock

    /**
     * Modelo operacional especifico da DFU. Quando nulo, o fluxo consulta o
     * default do material via projection de parametros.
     */
    @Getter(AccessLevel.NONE) // caso não se encontre o modelo operacional o ClusterEParametrosProjection será usado para extrair o valor da entidade Produto
    @Enumerated(EnumType.ORDINAL) // MTS, MTO
    private Constantes.SNPModeloOperacional modeloOperacional; // MTS ou MTO

    /**
     * Forma de interpretacao do safety stock: quantidade fisica ou dias de
     * cobertura que serao convertidos por etapa anterior do Supply Planning.
     */
    @Enumerated(EnumType.ORDINAL) // DIAS, QUANTIDADE
    private Constantes.SNPCalculoSafetyStock calculoSafetyStock; // safety stock em dias ou quantidade
    
    /**
     * Valor operacional de safety stock para DRP ou target de Kanban.
     *
     * <p>`null` continua ausencia operacional zero. Valor presente negativo ou
     * nao finito e inconsistencia de cadastro, pois alimenta diretamente o
     * safety stock do heuristico Community e restricoes Enterprise que consomem
     * a mesma projection.</p>
     */
    private Double estoqueSegurancaDrpOuTargetKanban;

    /**
     * Estoque maximo operacional de DRP.
     */
    private Double estoqueMaximoDrp;

    /**
     * Campo transicional do schema compartilhado.
     *
     * <p>O uso de frequenciaReabastecimentoDias passou de input operacional
     * para calculo de estoque maximo a parametro usado exclusivamente na
     * otimizacao de politica de estoques Enterprise. Services e projections
     * Community devem neutralizar esse valor, mesmo que bases antigas possuam
     * dado preenchido.</p>
     */
    @Getter(AccessLevel.NONE) // caso não se encontre o modelo operacional o ClusterEParametrosProjection será usado para extrair o valor da entidade Produto
    private Double frequenciaReabastecimentoDias; // valor padrao = 0 dias (ressuprimento a qualquer momento)


    public PoliticaEstoques getPoliticaEstoques() {
        return getPoliticaEstoquesMaterialLocationCompositeKey().getPoliticaEstoques();
    }
    public Produto getMaterial() {
        return getPoliticaEstoquesMaterialLocationCompositeKey().getMaterial();
    }
    public Location getLocation() {
        return getPoliticaEstoquesMaterialLocationCompositeKey().getLocation();
    }
    
    /**
     * Valor cadastrado sem default, usado por DTOs e exports que precisam
     * diferenciar ausencia de parametrizacao de valor operacional resolvido.
     */
    public Constantes.SNPModeloReabastecimento getModeloReabastecimentoCadastrado() {
        return modeloReabastecimento;
    }

    /**
     * Valor operacional com default Community para consumo do heuristico.
     */
    public Constantes.SNPModeloReabastecimento getModeloReabastecimento() {
        return (modeloReabastecimento == null) ? Constantes.SNPModeloReabastecimento.DRP : modeloReabastecimento;
    }

    /**
     * Valor cadastrado sem default, usado por DTOs e exports.
     */
    public Constantes.SNPModeloOperacional getModeloOperacionalCadastrado() {
        return modeloOperacional;
    }

    /**
     * Valor operacional resolvido com fallback no default do material.
     */
    public Constantes.SNPModeloOperacional getModeloOperacional(ClusterEParametrosProjection clusterEParametrosProjection) {
        return (modeloOperacional == null) ?
                clusterEParametrosProjection.getMaterialPersistido(getMaterial().getId()).getModeloOperacional()
                : modeloOperacional;
    }

    /**
     * Valor cadastrado sem default, usado por DTOs e exports.
     */
    public Constantes.SNPCalculoSafetyStock getCalculoSafetyStockCadastrado() {
        return calculoSafetyStock;
    }

    /**
     * Valor operacional com default em dias, preservando o comportamento
     * historico do heuristico.
     */
    public Constantes.SNPCalculoSafetyStock getCalculoSafetyStock() {
        return (calculoSafetyStock == null) ? Constantes.SNPCalculoSafetyStock.DAYS : calculoSafetyStock;
    }

    
    /**
     * Valor cadastrado de safety stock sem default, usado por DTOs e exports.
     */
    public Double getEstoqueSegurancaDrpOuTargetKanbanCadastrado() {
        return estoqueSegurancaDrpOuTargetKanban;
    }

    /**
     * Valor operacional de safety stock com default zero.
     */
    public Double getEstoqueSegurancaDrpOuTargetKanban() {
        return getValorPoliticaEstoqueNaoNegativoOuZero(
                estoqueSegurancaDrpOuTargetKanban,
                "safety stock / Kanban target");
    }

    /**
     * Valor cadastrado de estoque maximo sem default, usado por DTOs e exports.
     */
    public Double getEstoqueMaximoDrpCadastrado() {
        return estoqueMaximoDrp;
    }

    /**
     * Valor operacional de estoque maximo com default zero.
     */
    public Double getEstoqueMaximoDrp() {
        return getValorPoliticaEstoqueNaoNegativoOuZero(
                estoqueMaximoDrp,
                "maximum DRP stock");
    }

    /**
     * Valida grandezas fisicas operacionais da politica de estoque.
     *
     * <p>Ausencia de cadastro ainda significa zero operacional para preservar
     * o comportamento Community. Quando o usuario cadastrou um valor, ele deve
     * ser finito e nao negativo antes de alimentar Safety Stock, Kanban ou
     * consumidores Enterprise de restricao.</p>
     */
    private Double getValorPoliticaEstoqueNaoNegativoOuZero(
            Double valorPoliticaEstoque,
            String nomeCampo) {

        if (valorPoliticaEstoque == null) {
            return 0.0d;
        }
        if (!Double.isFinite(valorPoliticaEstoque)
                || valorPoliticaEstoque < 0.0d) {
            throw new IllegalStateException(
                    "Inventory policy "
                            + nomeCampo
                            + " must be finite and non-negative for material "
                            + getMaterialIdParaMensagem()
                            + " / location "
                            + getLocationIdParaMensagem()
                            + ": "
                            + valorPoliticaEstoque
                            + ".");
        }
        return valorPoliticaEstoque;

    }

    private String getMaterialIdParaMensagem() {

        if (politicaEstoquesMaterialLocationCompositeKey == null
                || politicaEstoquesMaterialLocationCompositeKey.getMaterial() == null
                || politicaEstoquesMaterialLocationCompositeKey.getMaterial().getId() == null) {
            return "<sem-material>";
        }
        return politicaEstoquesMaterialLocationCompositeKey.getMaterial().getId();

    }

    private String getLocationIdParaMensagem() {

        if (politicaEstoquesMaterialLocationCompositeKey == null
                || politicaEstoquesMaterialLocationCompositeKey.getLocation() == null
                || politicaEstoquesMaterialLocationCompositeKey.getLocation().getId() == null) {
            return "<sem-location>";
        }
        return politicaEstoquesMaterialLocationCompositeKey.getLocation().getId();

    }

    /**
     * Valor fisico cadastrado da frequencia transicional.
     *
     * <p>No Community, este metodo existe para que services possam ocultar ou
     * rejeitar explicitamente o campo. Ele nao deve ser usado para alimentar
     * calculo operacional de safety stock.</p>
     */
    public Double getFrequenciaReabastecimentoDiasCadastrado() {
        return frequenciaReabastecimentoDias;
    }

    /**
     * Valor historico resolvido da frequencia.
     *
     * <p>Este metodo fica preservado por compatibilidade com fluxos migrados,
     * mas novos consumidores Community devem preferir a projection, que
     * neutraliza o valor para impedir reentrada de Inventory Policy
     * Optimization.</p>
     */
    public double getFrequenciaReabastecimentoDias(ClusterEParametrosProjection clusterEParametrosProjection) {
        return (frequenciaReabastecimentoDias == null) ?
                clusterEParametrosProjection.getSNPFrequenciaReabascecimentoDias(getMaterial(), getLocation())
                : frequenciaReabastecimentoDias;
    }

}
