package com.opsfactor.community.capability.masterdata.production.productionresource.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import lombok.*;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Disponibilidade diaria de um recurso produtivo.
 *
 * <p>No Community, a capacidade produtiva padrao do Supply Planning e horas
 * totais por dia. O campo de capacidade em quantidade permanece apenas como
 * dado operacional historico do schema, sem abrir turnos, custos ou line
 * scheduling Enterprise.</p>
 */
@Entity
@Data
@ToString(of="disponibilidadeRecursoProdutivoCompositeKey")
@EqualsAndHashCode(of = "disponibilidadeRecursoProdutivoCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
public class DisponibilidadeRecursoProdutivo {

    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private DisponibilidadeRecursoProdutivoCompositeKey disponibilidadeRecursoProdutivoCompositeKey;

    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class DisponibilidadeRecursoProdutivoCompositeKey implements Serializable {

        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private RecursoProdutivo recursoProdutivo;
        
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private LocalDate dataReferencia;
                
    }
        
    /*
     * Capacidade diaria em horas. Null preserva ausencia operacional zero; valor
     * cadastrado precisa ser finito e nao negativo antes de alimentar BI de
     * capacidade produtiva.
     */
    private Float horasDisponiveis;
    
    /*
     * Capacidade diaria em quantidade. O Community usa horas por dia como modo
     * habilitado, mas este campo pode existir em snapshots compartilhados e deve
     * falhar cedo se for fisicamente invalido.
     */
    private Float capacidadeEmQuantidade;
    
        
    public RecursoProdutivo getRecursoProdutivo() {
        return getDisponibilidadeRecursoProdutivoCompositeKey().getRecursoProdutivo();
    }

    public Location getLocation() {
        return getRecursoProdutivo().getLocation();
    }

    public LocalDate getDataReferencia() {
        return getDisponibilidadeRecursoProdutivoCompositeKey().getDataReferencia();
    }
    
    public Float getHorasDisponiveis() {

        return getValorCapacidadeNaoNegativoOuZero(
                horasDisponiveis,
                "available hours");

    }
    public Float getHorasDisponiveisCadastrado() {
        return horasDisponiveis;
    }
    
    public Float getCapacidadeEmQuantidade() {

        return getValorCapacidadeNaoNegativoOuZero(
                capacidadeEmQuantidade,
                "quantity capacity");

    }
    public Float getCapacidadeEmQuantidadeCadastrado() {
        return capacidadeEmQuantidade;
    }

    /**
     * Valida escalares fisicos de capacidade produtiva, mantendo `null` como
     * ausencia operacional compativel com cadastros historicos.
     */
    private Float getValorCapacidadeNaoNegativoOuZero(
            Float valorCapacidade,
            String contextoValorCapacidade) {

        if (valorCapacidade == null) {
            return 0.0f;
        }
        if (!Float.isFinite(valorCapacidade) || valorCapacidade < 0.0f) {
            throw new IllegalStateException(
                    "Production resource "
                            + contextoValorCapacidade
                            + " must be finite and non-negative for resource "
                            + getRecursoProdutivo().getId()
                            + " / date "
                            + getDataReferencia()
                            + ": "
                            + valorCapacidade
                            + ".");
        }
        return valorCapacidade;

    }
        
}
