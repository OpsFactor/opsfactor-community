package com.opsfactor.community.capability.configuration.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import java.util.List;

/**
 * Item de configuracao de caracteristica em uma view.
 *
 * <p>No Community, o DTO representa apenas filtros por atributos públicos de
 * material e location. {@code aggregationType}, {@code columnPosition} e
 * características material-location/DFU continuam Pro; o service rejeita
 * esses campos antes de persistir a view.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfiguredViewCaracteristicaDTO {
    
    public String characteristicId;
    public String characteristicDescription;
    
    public ConfiguredView.TipoAgregacao aggregationType;
    
    public Integer columnPosition;
    
    public List<String> filteredValues;
    
}
