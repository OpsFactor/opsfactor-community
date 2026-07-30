package com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opsfactor.community.capability.planningbook.facade.dto.KeyFigureDTOAbstract;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureRelacaoEntreValores;
import com.opsfactor.community.platform.calendar.Calendario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Consolida uma razão usando numerador e denominador, sem somar taxas. */
public class KeyFigureDTORelacaoEntreValores extends KeyFigureDTOAbstract<
        DFUDataKeyFigureRelacaoEntreValores, KeyFigureDTORelacaoEntreValores> {

    @JsonIgnore private Map<String, Double> numeratorValues = new HashMap<>();
    @JsonIgnore private Map<String, Double> numeratorTimesDenominatorValues = new HashMap<>();
    @JsonIgnore private Map<String, Double> denominatorValues = new HashMap<>();
    @JsonIgnore private Map<String, Integer> valueCounts = new HashMap<>();

    public KeyFigureDTORelacaoEntreValores(String keyFigure, EditMode editMode) {

        super(keyFigure, editMode);

    }

    @Override
    public void importaDadosDFUDataKeyFigure(
            Calendario calendario,
            List<DFUDataKeyFigureRelacaoEntreValores> dados) {

        for (DFUDataKeyFigureRelacaoEntreValores dado : dados) {
            String periodo = calendario.getUltimaDataHorarioPeriodo(dado.getData()).toString();
            adicionaComponentes(periodo, dado.getNumeratorValue(), dado.getDenominatorValue(), 1);
        }
        recalculaValores();

    }

    @Override
    public KeyFigureDTORelacaoEntreValores getCopiaSomenteComKeyFigureIdEEditMode() {

        return new KeyFigureDTORelacaoEntreValores(keyFigure, editMode);

    }

    @Override
    public KeyFigureDTORelacaoEntreValores getCopiaCompleta() {

        KeyFigureDTORelacaoEntreValores copia = getCopiaSomenteComKeyFigureIdEEditMode();
        copia.values = values == null ? null : new HashMap<>(values);
        copia.unavailableReasons = unavailableReasons == null ? null : new HashMap<>(unavailableReasons);
        copia.toolTips = toolTips == null ? null : new HashMap<>(toolTips);
        copia.additionalClasses = additionalClasses == null ? null : new HashMap<>(additionalClasses);
        copia.numeratorValues = new HashMap<>(numeratorValues);
        copia.numeratorTimesDenominatorValues = new HashMap<>(numeratorTimesDenominatorValues);
        copia.denominatorValues = new HashMap<>(denominatorValues);
        copia.valueCounts = new HashMap<>(valueCounts);
        return copia;

    }

    @Override
    public void incorporaValoresDeKeyFigure(KeyFigureDTORelacaoEntreValores outro) {

        outro.numeratorValues.forEach((periodo, numeratorValue) -> {
            numeratorValues.merge(periodo, numeratorValue, Double::sum);
            numeratorTimesDenominatorValues.merge(
                    periodo,
                    outro.numeratorTimesDenominatorValues.get(periodo),
                    Double::sum);
            denominatorValues.merge(periodo, outro.denominatorValues.get(periodo), Double::sum);
            valueCounts.merge(periodo, outro.valueCounts.get(periodo), Integer::sum);
        });
        recalculaValores();

    }

    private void adicionaComponentes(String periodo, double numeratorValue, double denominatorValue, int valueCount) {

        numeratorValues.merge(periodo, numeratorValue, Double::sum);
        numeratorTimesDenominatorValues.merge(periodo, numeratorValue * denominatorValue, Double::sum);
        denominatorValues.merge(periodo, denominatorValue, Double::sum);
        valueCounts.merge(periodo, valueCount, Integer::sum);

    }

    private void recalculaValores() {

        values = new HashMap<>();
        numeratorValues.forEach((periodo, numeratorValue) -> {
            if (hasUnavailableReason(periodo)) {
                return;
            }
            double weightedDenominatorValue = numeratorValue == 0.0d
                    ? denominatorValues.get(periodo) / valueCounts.get(periodo)
                    : numeratorTimesDenominatorValues.get(periodo) / numeratorValue;
            if (Double.isFinite(weightedDenominatorValue) && weightedDenominatorValue > 0.0d) {
                values.put(periodo, numeratorValue / weightedDenominatorValue);
            }
        });

    }

}
