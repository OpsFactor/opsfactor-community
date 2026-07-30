package com.opsfactor.community.platform.bi.facade.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Preserva o achatamento JSON histórico das características do agrupamento.
 *
 * <p>O Community publica apenas a pseudo-característica {@code materialId}.
 * Por isso a dimensão é uma cópia simples já derivada do MaterialProjection,
 * sem carregar o agrupamento dinâmico privado do legado.</p>
 */
abstract class CommunityMaterialCharacteristicGroupingDTO {

    @JsonIgnore
    protected final Map<String, String> materialCharacteristicValues;

    protected CommunityMaterialCharacteristicGroupingDTO(
            Map<String, String> materialCharacteristicValues) {

        this.materialCharacteristicValues = new LinkedHashMap<>(materialCharacteristicValues);

    }

    /** Expõe cada característica no mesmo nível das medidas da série. */
    @JsonAnyGetter
    public Map<String, String> getMaterialCharacteristicValues() {

        return materialCharacteristicValues;

    }
}
