package com.opsfactor.community.capability.supplyplanning.supplyplan.domain.calendar;

import com.opsfactor.community.capability.masterdata.calendar.profile.domain.*;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Cópia independente da receita usada por um Supply Plan. A PK é o próprio
 * identificador do plano, sem identidade artificial independente.
 * Não aponta para o perfil editável nem armazena versão de algoritmo.
 */
@Entity
@Table(name = "perfil_calendario_supply_plan")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_perfil_calendario", length = 20)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "supplyPlanId")
public abstract class PerfilCalendarioSupplyPlan implements Serializable {

    @Id
    @Column(name = "supply_plan_id", nullable = false)
    private Long supplyPlanId;

    /**
     * Identidade derivada: a coluna continua sendo simultaneamente PK e FK.
     * A PK unitária é escalar deliberadamente: no Hibernate 6.6, envolvê-la em
     * EmbeddedId causa erros tanto no LEFT JOIN de planos legados sem cópia como
     * no SELECT implícito de uma cópia polimórfica (Long recebido como wrapper).
     * Isso evita workarounds nos consumidores e não altera o schema físico.
     */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supply_plan_id", nullable = false)
    private SupplyPlan supplyPlan;

    private String descricao;

    /**
     * Identidade histórica da origem, copiada uma única vez, sem FK para o cadastro
     * editável. Não pode ser deduzida da receita nem do perfil de execução atual.
     * Cópias anteriores à introdução desta proveniência permanecem sem informação.
     */
    @Setter(AccessLevel.NONE)
    @Column(name = "perfil_calendario_origem_id", length = 50, updatable = false)
    private String perfilCalendarioOrigemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TamanhoBucket tamanhoBucketBase;

    protected PerfilCalendarioSupplyPlan(SupplyPlan supplyPlan, TamanhoBucket tamanhoBucketBase) {

        if (supplyPlan == null) {
            throw new IllegalArgumentException("O Supply Plan da cópia é obrigatório.");
        }
        this.supplyPlan = supplyPlan;
        // Mesmo para um plano já persistido, esta cópia é uma entidade nova.
        // A PK deve permanecer null até @MapsId derivá-la durante o persist:
        // preenchê-la aqui faz Spring Data save/saveAll escolher merge, e o
        // Hibernate 6.6 rejeita como detached a cópia cuja linha ainda não existe.
        this.tamanhoBucketBase = tamanhoBucketBase;

    }

    /** Copia identidade histórica por valor; não cria FK para o cadastro editável. */
    protected PerfilCalendarioSupplyPlan(SupplyPlan supplyPlan, PerfilCalendario perfilCalendario) {

        this(supplyPlan, perfilCalendario.getTamanhoBucketBase());
        this.descricao = perfilCalendario.getDescricao();
        this.perfilCalendarioOrigemId = perfilCalendario.getId();

    }

    /** Reconstrói somente da receita copiada, nunca do cadastro editável. */
    public abstract Calendario criarCalendario(LocalDateTime dataInicioPlano);

    public abstract TamanhoBucket getTamanhoBucketPrimeiroPeriodo();

    /** Valida a cópia antes de persistir ou reconstruir o calendário da execução. */
    public void validarEstrutura() {

        if (getSupplyPlan() == null) {
            throw new IllegalStateException("A cópia do perfil calendário deve pertencer a um Supply Plan.");
        }
        if (tamanhoBucketBase == null) {
            throw new IllegalStateException("A cópia do perfil calendário exige tamanhoBucketBase.");
        }

    }

}
