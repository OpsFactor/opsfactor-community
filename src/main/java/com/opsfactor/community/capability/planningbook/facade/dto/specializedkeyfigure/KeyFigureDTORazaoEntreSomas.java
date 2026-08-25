package com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opsfactor.community.capability.planningbook.facade.dto.KeyFigureDTOAbstract;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureRelacaoEntreValores;
import com.opsfactor.community.platform.calendar.Calendario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Consolida uma razao pela soma dos numeradores e dos denominadores. */
public class KeyFigureDTORazaoEntreSomas extends KeyFigureDTOAbstract<
        DFUDataKeyFigureRelacaoEntreValores, KeyFigureDTORazaoEntreSomas> {

    private static final double DENOMINATOR_TOLERANCE = 0.000000001d;
    private static final String ZERO_DENOMINATOR_REASON =
            "Ratio unavailable because the denominator total is zero.";

    @JsonIgnore private Map<String, Double> aggregatedNumeratorValues = new HashMap<>();
    @JsonIgnore private Map<String, Double> aggregatedDenominatorValues = new HashMap<>();

    public KeyFigureDTORazaoEntreSomas(String keyFigure, EditMode editMode) {

        super(keyFigure, editMode);
        values = new HashMap<>();

    }

    @Override
    public void importaDadosDFUDataKeyFigure(
            Calendario calendario,
            List<DFUDataKeyFigureRelacaoEntreValores> dados) {

        values = new HashMap<>();
        unavailableReasons = new HashMap<>();
        aggregatedNumeratorValues = new HashMap<>();
        aggregatedDenominatorValues = new HashMap<>();

        for (DFUDataKeyFigureRelacaoEntreValores dado : dados) {
            String periodo = calendario.getUltimaDataHorarioPeriodo(dado.getData()).toString();
            if (!Double.isFinite(dado.getNumeratorValue())
                    || !Double.isFinite(dado.getDenominatorValue())) {
                unavailableReasons.putIfAbsent(
                        periodo,
                        "Ratio unavailable because at least one contributing value is not finite.");
                continue;
            }
            aggregatedNumeratorValues.merge(periodo, dado.getNumeratorValue(), Double::sum);
            aggregatedDenominatorValues.merge(periodo, dado.getDenominatorValue(), Double::sum);
        }

        atualizaValores();

    }

    @Override
    public KeyFigureDTORazaoEntreSomas getCopiaSomenteComKeyFigureIdEEditMode() {

        return new KeyFigureDTORazaoEntreSomas(keyFigure, editMode);

    }

    @Override
    public KeyFigureDTORazaoEntreSomas getCopiaCompleta() {

        KeyFigureDTORazaoEntreSomas copia = getCopiaSomenteComKeyFigureIdEEditMode();
        copia.values = values == null ? null : new HashMap<>(values);
        copia.unavailableReasons = unavailableReasons == null ? null : new HashMap<>(unavailableReasons);
        copia.toolTips = toolTips == null ? null : new HashMap<>(toolTips);
        copia.additionalClasses = additionalClasses == null ? null : new HashMap<>(additionalClasses);
        copia.aggregatedNumeratorValues = new HashMap<>(aggregatedNumeratorValues);
        copia.aggregatedDenominatorValues = new HashMap<>(aggregatedDenominatorValues);
        return copia;

    }

    @Override
    public void incorporaValoresDeKeyFigure(KeyFigureDTORazaoEntreSomas outro) {

        outro.aggregatedNumeratorValues.forEach(
                (periodo, valor) -> aggregatedNumeratorValues.merge(periodo, valor, Double::sum));
        outro.aggregatedDenominatorValues.forEach(
                (periodo, valor) -> aggregatedDenominatorValues.merge(periodo, valor, Double::sum));
        atualizaValores();

    }

    /**
     * Falta de volume em uma folha e neutra para o grupo pai: outra folha com
     * volume continua podendo formar o preco medio agregado. Indisponibilidade
     * de preco, por outro lado, permanece uma falha real e e propagada.
     */
    @Override
    public void consolidateUnavailableReasons(KeyFigureDTORazaoEntreSomas outro) {

        if (outro.unavailableReasons == null || outro.unavailableReasons.isEmpty()) {
            return;
        }

        KeyFigureDTORazaoEntreSomas copia = outro.getCopiaCompleta();
        copia.unavailableReasons.entrySet().removeIf(
                entry -> ZERO_DENOMINATOR_REASON.equals(entry.getValue()));
        if (!copia.unavailableReasons.isEmpty()) {
            super.consolidateUnavailableReasons(copia);
        }

    }

    /** Recalcula a razao apos importar folhas ou consolidar grupos pais. */
    private void atualizaValores() {

        if (values == null) {
            values = new HashMap<>();
        }
        if (unavailableReasons == null) {
            unavailableReasons = new HashMap<>();
        }

        for (String periodo : aggregatedNumeratorValues.keySet()) {
            if (unavailableReasons.containsKey(periodo)
                    && !ZERO_DENOMINATOR_REASON.equals(unavailableReasons.get(periodo))) {
                values.remove(periodo);
                continue;
            }

            double denominador = aggregatedDenominatorValues.getOrDefault(periodo, 0.0d);
            if (Math.abs(denominador) <= DENOMINATOR_TOLERANCE) {
                values.remove(periodo);
                unavailableReasons.put(periodo, ZERO_DENOMINATOR_REASON);
                continue;
            }

            unavailableReasons.remove(periodo);
            values.put(periodo, aggregatedNumeratorValues.get(periodo) / denominador);
        }

        if (unavailableReasons.isEmpty()) {
            unavailableReasons = null;
        }

    }
}
