package com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
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
        super(calendarioOrigem, calendarioTarget);
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
                if (calendarioTarget.getTamanhoBucket().equals(calendarioOrigem.getTamanhoBucket())) {
                    int posicaoPeriodoCalendarioOrigem = calendarioOrigem.getPosicaoPeriodo(calendarioTarget.getPrimeiraDataHorarioPeriodo(posicaoPeriodoCalendarioTarget));
                    return valorPorPeriodoCalendarioOriginal.applyAsDouble(posicaoPeriodoCalendarioOrigem);
                } else if (calendarioTarget.getTamanhoBucket().getNivelAgregacao() > calendarioOrigem.getTamanhoBucket().getNivelAgregacao()) {
                    return getValorOndeCalendarioTargetMaisAgregadoQueCalendarioOriginal(valorPorPeriodoCalendarioOriginal, posicaoPeriodoCalendarioTarget);
                } else {
                    // se não houver curva associada ao material/location se usará o split padrão (flat)
                    SplitTemporalProjectionCurva splitTemporalProjectionCurva = getSplitTemporalProjectionCurva(location, material);
                    double valorAcumulado = 0;
                    Map<Integer, Double> mapaParticipacaoPeriodosTargetNoCalendarioOrigem = splitTemporalProjectionCurva.getMapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem().get(posicaoPeriodoCalendarioTarget);
                    if (mapaParticipacaoPeriodosTargetNoCalendarioOrigem == null) {
                        return 0;
                    }
                    for (Integer posicaoPeriodoCalendarioOrigem : mapaParticipacaoPeriodosTargetNoCalendarioOrigem.keySet()) {
                        double valorNoCalendarioOriginal = valorPorPeriodoCalendarioOriginal.applyAsDouble(posicaoPeriodoCalendarioOrigem);
                        valorAcumulado += mapaParticipacaoPeriodosTargetNoCalendarioOrigem.get(posicaoPeriodoCalendarioOrigem) * valorNoCalendarioOriginal;
                    }
                    return valorAcumulado;
                }
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

    protected double getValorOndeCalendarioTargetMaisAgregadoQueCalendarioOriginal(
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoCalendarioTarget) {
                LocalDateTime dataHorarioInicialPosicaoPeriodoCalendarioTarget = calendarioTarget.getPrimeiraDataHorarioPeriodo(posicaoPeriodoCalendarioTarget);
                LocalDateTime dataHorarioFinalPosicaoPeriodoCalendarioTarget = calendarioTarget.getUltimaDataHorarioPeriodo(posicaoPeriodoCalendarioTarget);
                int posicaoPeriodoInicialCalendarioOriginal = calendarioOrigem.getPosicaoPeriodo(dataHorarioInicialPosicaoPeriodoCalendarioTarget);
                int posicaoPeriodoFinalCalendarioOriginal = calendarioOrigem.getPosicaoPeriodo(dataHorarioFinalPosicaoPeriodoCalendarioTarget);
                double valorAcumulado = 0;
                for (int i = posicaoPeriodoInicialCalendarioOriginal; i <= posicaoPeriodoFinalCalendarioOriginal; i++) {
                    valorAcumulado += valorPorPeriodoCalendarioOriginal.applyAsDouble(i);
                }
                return valorAcumulado;
            }

    
}
