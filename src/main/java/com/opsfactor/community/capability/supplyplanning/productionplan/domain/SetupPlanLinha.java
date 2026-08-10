package com.opsfactor.community.capability.supplyplanning.productionplan.domain;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoInexistente;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Output Enterprise de setup planejado por supply plan, recurso, roteiro/BOM e
 * periodo.
 *
 * <p>O Community preserva o plano produtivo basico, mas setup e sequenciamento
 * pertencem ao overlay Enterprise de optimizer/line scheduling. Esta entidade
 * reabre somente a leitura do snapshot ja persistido para exportacao de
 * ocupacao produtiva; ela nao implementa o calculo nem a edicao do Gantt.</p>
 */
@Getter
@Setter
@EqualsAndHashCode(of = "setupPlanLinhaCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class SetupPlanLinha implements Serializable {

    /**
     * Chave funcional do setup calculado para um periodo e recurso produtivo.
     */
    @EmbeddedId
    @NonNull
    private SetupPlanLinhaCompositeKey setupPlanLinhaCompositeKey;

    /**
     * Modo de sequenciamento que originou a linha de setup.
     */
    @Enumerated(EnumType.ORDINAL)
    private PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial tipoSequenciamentoLinha;

    /**
     * Horas de setup alocadas ao periodo no modo SNP scheduling.
     */
    private Double numeroHorasSetupSequenciamentoSNP;

    /**
     * Posicao da alocacao de setup dentro do periodo no modo SNP scheduling.
     */
    private Integer posicaoSequenciaPeriodoSequenciamentoSNP;

    /**
     * Chave composta do output de setup.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class SetupPlanLinhaCompositeKey implements Serializable {

        /**
         * Supply plan que gerou o setup.
         */
        @ManyToOne(optional = false)
        @NonNull
        private SupplyPlan supplyPlan;

        /**
         * Recurso produtivo ocupado pelo setup.
         */
        @ManyToOne(optional = false)
        @NonNull
        private RecursoProdutivo recursoProdutivo;

        /**
         * Versao de producao associada ao setup.
         */
        @ManyToOne(optional = false)
        @NonNull
        private VersaoProducao versaoProducao;

        /**
         * Roteiro associado ao setup.
         */
        @ManyToOne(optional = false)
        @NonNull
        private Roteiro roteiro;

        /**
         * Lista tecnica associada ao setup.
         */
        @ManyToOne(optional = false)
        @NonNull
        private ListaTecnica listaTecnica;

        /**
         * Periodo de referencia do setup.
         */
        @NonNull
        private LocalDateTime dataReferencia;

    }

    public SupplyPlan getSupplyPlan() {

        return setupPlanLinhaCompositeKey.getSupplyPlan();

    }

    public RecursoProdutivo getRecursoProdutivo() {

        return setupPlanLinhaCompositeKey.getRecursoProdutivo();

    }

    public VersaoProducao getVersaoProducaoCadastrada() {

        return setupPlanLinhaCompositeKey.getVersaoProducao();

    }

    public VersaoProducao getVersaoProducaoAlocadaOuNulaSeInexistente() {

        return getVersaoProducaoCadastrada() instanceof VersaoProducaoInexistente
                ? null
                : getVersaoProducaoCadastrada();

    }

    public Roteiro getRoteiro() {

        return setupPlanLinhaCompositeKey.getRoteiro();

    }

    public ListaTecnica getListaTecnica() {

        return setupPlanLinhaCompositeKey.getListaTecnica();

    }

    public LocalDateTime getDataReferencia() {

        return setupPlanLinhaCompositeKey.getDataReferencia();

    }

    public PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial getTipoSequenciamentoLinha() {

        return tipoSequenciamentoLinha == null
                ? PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial.DESABILITADO
                : tipoSequenciamentoLinha;

    }

    public Double getNumeroHorasSetupSequenciamentoSNP() {

        return numeroHorasSetupSequenciamentoSNP == null
                ? 0.0d
                : numeroHorasSetupSequenciamentoSNP;

    }

}
