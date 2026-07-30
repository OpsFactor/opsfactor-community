package com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.planningbook.facade.dto.KeyFigureDTOAbstract;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigurePadrao;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.FuncoesMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Key figure padrao com um unico mapa de valores agregados por soma.
 *
 * <p>Este e o formato usado pelas KFs Community publicadas no RuntimeInfo.
 * Quando o Planning Book precisa consolidar subgrupos, valores de mesmo
 * periodo sao somados e tooltips/classes adicionais sao herdados pela classe
 * base.</p>
 */

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyFigureDTOPadrao extends KeyFigureDTOAbstract <DFUDataKeyFigurePadrao, KeyFigureDTOPadrao> {

    public KeyFigureDTOPadrao(String keyFigure, EditMode editMode) {
        super(keyFigure, editMode);
    }

    @Override
    public void importaDadosDFUDataKeyFigure(Calendario calendario, List<DFUDataKeyFigurePadrao> keyFigureData) {
        values = keyFigureData.parallelStream()
                .collect(Collectors.groupingBy(
                        x -> calendario.getUltimaDataHorarioPeriodo(((DFUDataKeyFigurePadrao) x).getData()).toString(),
                        Collectors.summingDouble(DFUDataKeyFigurePadrao::getValor)));
    }

    @Override
    public KeyFigureDTOPadrao getCopiaSomenteComKeyFigureIdEEditMode() {
        return new KeyFigureDTOPadrao(keyFigure, editMode);
    }
    @Override
    public KeyFigureDTOPadrao getCopiaCompleta() {

        KeyFigureDTOPadrao novoKeyFigureDTO = new KeyFigureDTOPadrao(keyFigure, editMode);
        /*
         * Uma KF pode ter apenas valores, sem tooltip nem classe visual. Esse
         * e o formato normal de folhas simples do Planning Book e tambem dos
         * novos pais Enterprise read-only. A copia completa nao pode exigir
         * metadados que a origem nunca publicou.
         */
        novoKeyFigureDTO.additionalClasses = additionalClasses == null
                ? null
                : new HashMap(additionalClasses);
        novoKeyFigureDTO.toolTips = toolTips == null
                ? null
                : new HashMap(toolTips);
        novoKeyFigureDTO.editMode = editMode;
        novoKeyFigureDTO.values = values == null ? null : new HashMap(values);
        novoKeyFigureDTO.unavailableReasons = unavailableReasons == null
                ? null
                : new HashMap(unavailableReasons);
        return novoKeyFigureDTO;

    }

    @Override
    public void incorporaValoresDeKeyFigure(KeyFigureDTOPadrao keyFigureDtoAIncorporar) {

        if (keyFigureDtoAIncorporar.values != null && !keyFigureDtoAIncorporar.values.isEmpty()) {
            if (values == null) values = new HashMap<>();
            // comportamento padrão : soma valores de A e B por período
            keyFigureDtoAIncorporar.values.entrySet()
                    .stream()
                    .forEach(entryDataStringEValorDouble -> {
                        String dataComoString = entryDataStringEValorDouble.getKey();
                        Double valor = entryDataStringEValorDouble.getValue();

                        if (hasUnavailableReason(dataComoString)
                                || keyFigureDtoAIncorporar.hasUnavailableReason(dataComoString)) {
                            return;
                        }
                        if (valor == 0) return;
                        // soma valor de b no período (data) no mapa de valores de aggregatedKeyFigureDTO
                        FuncoesMap.updateElementoNoNestedMap(
                                0.0,
                                valorAnterior -> valorAnterior + valor,
                                Double.class,
                                values,
                                dataComoString);
                    });
        }

    }


}
