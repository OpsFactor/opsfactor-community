package com.opsfactor.community.capability.configuration.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import java.util.List;

/**
 * Item de configuracao de caracteristica em uma view.
 *
 * <p>No Community, configuracoes reais de caracteristicas dinamicas sao
 * recursos Enterprise para material, location e material-location/DFU. O DTO
 * permanece no modulo Community para rejeitar payloads legados ou transicionais
 * amplos; o service rejeita qualquer configuracao real antes de persistir a
 * view.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfiguredViewCaracteristicaDTO {
    
    public String characteristicId;
    public String characteristicDescription;
    
    public ConfiguredView.TipoAgregacao aggregationType;
    
    public Integer columnPosition;
    
    public List<String> filteredValues;
    
}
