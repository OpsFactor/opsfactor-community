package com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto;

import com.opsfactor.community.web.dto.template.DTO;

import java.util.List;
import java.util.Map;

/**
 * Contrato compartilhado para restringir material e location por ids
 * explícitos ou por combinações de características públicas.
 *
 * <p>O contrato preserva a semântica do legado: ids explícitos são
 * intersectados com características; características diferentes usam AND e
 * valores da mesma característica usam OR.</p>
 */
public class FiltroMaterialLocationDeCombinacaoCaracteristicasDTO extends DTO {

    /** Valores públicos selecionados, agrupados pelo id da característica material. */
    public Map<String,List<String>> valuesByMaterialCharacteristicId;

    /** IDs explícitos de materiais que restringem a mesma seleção. */
    public List<String> materialIds;

    /** Valores públicos selecionados, agrupados pelo id da característica location. */
    public Map<String,List<String>> valuesByLocationCharacteristicId;

    /** IDs explícitos de locations que restringem a mesma seleção. */
    public List<String> locationIds;

    public boolean isSelecaoMateriaisVazia() {
        return
                (materialIds == null || materialIds.isEmpty())
                && (
                        valuesByMaterialCharacteristicId == null
                        || valuesByMaterialCharacteristicId.isEmpty()
                        // ex. lista com keys (caracteristicas) mas sem nenhum valor para nenhuma caracteristica (listas vazias)
                        || !valuesByMaterialCharacteristicId
                                .values()
                                .stream()
                                .anyMatch(lista -> !lista.isEmpty())
                );
    }

    public boolean isSelecaoLocationsVazia() {
        return
                (locationIds == null || locationIds.isEmpty())
                && (
                        valuesByLocationCharacteristicId == null
                        || valuesByLocationCharacteristicId.isEmpty()
                        // ex. lista com keys (caracteristicas) mas sem nenhum valor para nenhuma caracteristica (listas vazias)
                        || !valuesByLocationCharacteristicId
                                .values()
                                .stream()
                                .anyMatch(lista -> !lista.isEmpty())
                );
    }


}
