package com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Versao por DFU da projection temporal usada no Community.
 *
 * <p>No Community a escolha por material/location e mantida apenas na
 * assinatura dos metodos consumidores. A implementacao sempre retorna a curva
 * flat implicita; curvas cadastradas por DFU pertencem ao Enterprise e sao
 * barradas na factory antes desta projection ser criada.</p>
 */
@Getter
public class SplitTemporalProjectionPorDfu extends SplitTemporalProjection {

    // CONSTRUTOR
    public SplitTemporalProjectionPorDfu(Calendario calendarioOrigem, Calendario calendarioTarget) {
        super(calendarioOrigem,
                calendarioTarget);
    }

    /**
     * Retorna sempre a curva flat Community.
     *
     * <p>Os parametros continuam presentes porque varios consumidores chamam a
     * projection por DFU, mas eles nao participam da escolha da curva nesta
     * edicao.</p>
     */
    public SplitTemporalProjectionCurva getSplitTemporalProjectionCurva(Location location, Produto material) {
        return splitTemporalProjectionCurvaBase;

    }

    public Set<Integer> getPeriodosOrigemAPartirPeriodoCalendarioTarget(int periodoCalendarioTarget) {
        return mapaSetPeriodosTargetDentroDePeriodoOrigem
                .entrySet()
                .stream()
                .filter(x -> x.getValue().contains(periodoCalendarioTarget))
                .map(x -> x.getKey())
                .collect(Collectors.toSet());

    }

    public double getValorNoCalendarioTargetSplitTemporal(
            Location location, Produto material,
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoCalendarioTarget) {
        // Seleção da curva continua polimórfica por DFU no Enterprise, mas a
        // regra de interseção é compartilhada com o split flat do Community.
        return super.getValorNoCalendarioTargetSplitTemporal(
                getSplitTemporalProjectionCurva(location, material),
                valorPorPeriodoCalendarioOriginal, posicaoPeriodoCalendarioTarget);

    }

    public double getValorNoCalendarioTargetSplitTemporal(
            Location location, Produto material,
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoInicialCalendarioTarget, int posicaoPeriodoFinalCalendarioTarget) {
                double valorAcumulado = 0;
                for (int i = posicaoPeriodoInicialCalendarioTarget; i <= posicaoPeriodoFinalCalendarioTarget; i++) {
                    valorAcumulado += getValorNoCalendarioTargetSplitTemporal(location, material, valorPorPeriodoCalendarioOriginal, i);
                }
                return valorAcumulado;
            }

    /** Consulta deslocada por lead time em dias, sem diarizar a série inteira. */
    public double getValorNoRangeSplitTemporal(Location location, Produto material,
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            LocalDateTime inicioInclusivo, LocalDateTime fimInclusivo) {

        SplitTemporalProjectionCurva curva = getSplitTemporalProjectionCurva(location, material);
        if (curva == null) {
            throw new IllegalStateException("Curva de split obrigatória");
        }
        return curva.getValorNoRangeSplitTemporal(valorPorPeriodoCalendarioOriginal, inicioInclusivo, fimInclusivo);

    }
}
