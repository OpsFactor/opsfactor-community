package com.opsfactor.community.capability.supplyplanning.productionplan.domain;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Capacidade produtiva efetiva considerada em uma execucao do Supply Plan.
 */
@Data
@EqualsAndHashCode(of = "capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey")
@ToString(of = "capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "capacidade_produtiva_efetiva_recurso_produtivo_supply_plan")
public class CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan implements Serializable {

    @EmbeddedId
    @AttributeOverride(name = "dataReferencia", column = @Column(name = "data_referencia", nullable = false))
    @AssociationOverrides({
            @AssociationOverride(name = "supplyPlan", joinColumns = @JoinColumn(name = "supply_plan_id", nullable = false)),
            @AssociationOverride(name = "recursoProdutivo", joinColumns = @JoinColumn(name = "recurso_produtivo_id", nullable = false))
    })
    @NonNull
    private CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey;

    /**
     * Origem conceitual da capacidade antes de ela ser normalizada para o valor
     * efetivo usado no plano.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva;

    /**
     * Valor efetivo no periodo: horas quando a capacidade e em horas/turnos,
     * ou quantidade quando o recurso esta configurado por UOM.
     */
    @Column(nullable = false)
    private Double capacidadeEfetiva;

    @ManyToOne
    @JoinColumn(name = "unidade_medida_id")
    private UnidadeMedida unidadeMedidaCapacidade;

    @Data
    @Embeddable
    @NoArgsConstructor
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static class CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey implements Serializable {

        @ManyToOne(optional = false)
        @NonNull
        private SupplyPlan supplyPlan;

        @ManyToOne(optional = false)
        @NonNull
        private RecursoProdutivo recursoProdutivo;

        @NonNull
        private LocalDateTime dataReferencia;

    }

    public SupplyPlan getSupplyPlan() {
        return capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey.getSupplyPlan();
    }

    public RecursoProdutivo getRecursoProdutivo() {
        return capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey.getRecursoProdutivo();
    }

    public LocalDateTime getDataReferencia() {
        return capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey.getDataReferencia();
    }

}
