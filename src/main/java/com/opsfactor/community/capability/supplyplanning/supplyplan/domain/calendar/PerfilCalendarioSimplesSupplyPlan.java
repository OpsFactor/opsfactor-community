package com.opsfactor.community.capability.supplyplanning.supplyplan.domain.calendar;

import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendarioSimples;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Receita uniforme preservada para visualização e reexecução de um Supply Plan. */
@Entity
@DiscriminatorValue("SIMPLES")
@Getter
@Setter
@NoArgsConstructor
public class PerfilCalendarioSimplesSupplyPlan extends PerfilCalendarioSupplyPlan {

    private int numeroPeriodosBucketBase;

    public PerfilCalendarioSimplesSupplyPlan(SupplyPlan supplyPlan, TamanhoBucket tamanhoBucketBase,
                                             int numeroPeriodosBucketBase) {

        super(supplyPlan, tamanhoBucketBase);
        this.numeroPeriodosBucketBase = numeroPeriodosBucketBase;

    }

    /** Constrói uma cópia independente, incluindo proveniência da receita. */
    public PerfilCalendarioSimplesSupplyPlan(SupplyPlan supplyPlan, PerfilCalendarioSimples perfilCalendario) {

        super(supplyPlan, perfilCalendario);
        this.numeroPeriodosBucketBase = perfilCalendario.getNumeroPeriodosBucketBase();
        validarEstrutura();

    }

    @Override
    public TamanhoBucket getTamanhoBucketPrimeiroPeriodo() {

        return getTamanhoBucketBase();

    }

    @Override
    public Calendario criarCalendario(LocalDateTime dataInicioPlano) {

        validarEstrutura();
        if (dataInicioPlano == null) {
            throw new IllegalArgumentException("A data inicial do plano é obrigatória.");
        }
        return CalendarioSimples.criaCalendarioDeOffsetsPeriodos(
                getTamanhoBucketBase(), dataInicioPlano, 0, 0, numeroPeriodosBucketBase, 0);

    }

    @Override
    public void validarEstrutura() {

        super.validarEstrutura();
        if (numeroPeriodosBucketBase <= 0) {
            throw new IllegalStateException("A cópia do calendário simples deve possuir pelo menos um período.");
        }

    }

}
