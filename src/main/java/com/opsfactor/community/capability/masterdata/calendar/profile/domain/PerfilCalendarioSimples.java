package com.opsfactor.community.capability.masterdata.calendar.profile.domain;

import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.calendar.PerfilCalendarioSimplesSupplyPlan;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Perfil uniforme, aceito por Demand Planning e Supply Planning. */
@Entity
@DiscriminatorValue("SIMPLES")
@Getter
@Setter
@NoArgsConstructor
public class PerfilCalendarioSimples extends PerfilCalendario {

    /** Horizonte expresso no bucket-base uniforme. */
    private int numeroPeriodosBucketBase;

    public PerfilCalendarioSimples(String id, TamanhoBucket tamanhoBucketBase, int numeroPeriodosBucketBase) {

        super(id, tamanhoBucketBase);
        this.numeroPeriodosBucketBase = numeroPeriodosBucketBase;

    }

    /** O calendário uniforme usa o próprio bucket-base desde o primeiro período. */
    @Override
    public TamanhoBucket getTamanhoBucketPrimeiroPeriodo() {

        return getTamanhoBucketBase();

    }

    /** Usa o mesmo resolvedor uniforme adotado pelas rotinas de cálculo. */
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
    public PerfilCalendarioSimplesSupplyPlan copiarParaSupplyPlan(SupplyPlan supplyPlan) {

        validarEstrutura();
        return new PerfilCalendarioSimplesSupplyPlan(supplyPlan, this);

    }

    @Override
    public void validarEstrutura() {

        super.validarEstrutura();
        if (numeroPeriodosBucketBase <= 0) {
            throw new IllegalStateException("O calendário simples deve possuir pelo menos um período.");
        }

    }

}
