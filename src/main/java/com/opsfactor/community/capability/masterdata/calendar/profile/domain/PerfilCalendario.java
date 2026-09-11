package com.opsfactor.community.capability.masterdata.calendar.profile.domain;

import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.calendar.PerfilCalendarioSupplyPlan;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Receita reutilizável de calendário. O identificador é funcional, informado no
 * cadastro; o calendário de uma execução é construído a partir desta receita e
 * da data inicial do plano. Supply Plan conserva sua própria cópia da receita.
 */
@Entity
@Table(name = "perfil_calendario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_perfil_calendario", length = 20)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public abstract class PerfilCalendario implements Serializable {

    @Id
    @Column(length = 50, nullable = false)
    private String id;

    private String descricao;

    /** Unidade que mede a extensão dos itens e a extrapolação fora da grade. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TamanhoBucket tamanhoBucketBase;

    protected PerfilCalendario(String id, TamanhoBucket tamanhoBucketBase) {

        this.id = id;
        this.tamanhoBucketBase = tamanhoBucketBase;

    }

    /**
     * Bucket nominal usado para selecionar o primeiro período de uma execução.
     *
     * <p>Esse metadado pertence à receita e, portanto, não exige data inicial nem
     * materialização de {@code Calendario}. Em perfis compostos, os itens devem
     * estar carregados antes da chamada.</p>
     */
    public abstract TamanhoBucket getTamanhoBucketPrimeiroPeriodo();

    /** Materializa a grade transitória sem dependência do módulo Enterprise. */
    public abstract Calendario criarCalendario(LocalDateTime dataInicioPlano);

    /** Cada subtipo copia sua receita por valor; o plano reutiliza a cópia nas reexecuções. */
    public abstract PerfilCalendarioSupplyPlan copiarParaSupplyPlan(SupplyPlan supplyPlan);

    /** Valida o cadastro antes de persistir, copiar ou montar a projection. */
    public void validarEstrutura() {

        if (id == null || id.isBlank() || id.length() > 50) {
            throw new IllegalStateException("O perfil calendário exige id com até 50 caracteres.");
        }
        if (tamanhoBucketBase == null) {
            throw new IllegalStateException("O perfil calendário exige tamanhoBucketBase.");
        }

    }

}
