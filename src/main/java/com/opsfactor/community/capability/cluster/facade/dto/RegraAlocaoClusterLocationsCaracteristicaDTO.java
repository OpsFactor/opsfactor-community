package com.opsfactor.community.capability.cluster.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaLocationDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data @JsonInclude(JsonInclude.Include.NON_NULL)
public class RegraAlocaoClusterLocationsCaracteristicaDTO extends RegraAlocaoClusterLocationsDTO {

    /**
     * Nivel 2
     * Campo Enterprise/compatibilidade.
     *
     * <p>O Community nao salva regra de cluster de location por caracteristica;
     * este subtipo existe para desserializar o payload compartilhado e permitir
     * bloqueio explicito no service.</p>
     */
    private CaracteristicaLocationDTO caracteristicaDTO;

    /**
     * Nivel 3
     * Campo Enterprise/compatibilidade para criterio por caracteristica.
     */
    private List<String> atributosCaracteristica = new ArrayList<>();

}
