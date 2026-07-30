package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Opções específicas para integração de Transportation Lane Material.
 */
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinhaTransporteProdutoIntegrationOptionsDto extends IntegrationOptionsDto {

    /**
     * Quando true, cria automaticamente Transportation Lanes origem/destino ausentes
     * antes de persistir os registros por material.
     */
    public Boolean createTransportationLaneIfNotPresent;

}
