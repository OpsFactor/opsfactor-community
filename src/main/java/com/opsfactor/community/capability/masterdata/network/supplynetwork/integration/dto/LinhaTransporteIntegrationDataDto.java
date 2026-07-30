package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO da transportation lane origem/destino Community.
 *
 * <p>Este contrato representa a malha operacional consumida pelo Supply
 * Planning heuristico: versao de malha, origem, destino, lead time, prioridade,
 * lotes de transferencia e status. Distancia, frete, mapa, baricentro e Supply
 * Network Flows sao capacidades Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinhaTransporteIntegrationDataDto extends IntegrationDataDtoAbstract<LinhaTransporteIntegrationDataDto, LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte> {
 
    public Integer priority;
    public Double leadTimeDays;

    /**
     * Distancia da rota. Esse dado alimenta visualizacao geografica, frete e
     * analises Enterprise; o heuristico Community usa lead time/prioridade e
     * nao deve persistir distancia. O campo fica no DTO compartilhado para
     * falhar explicitamente quando payloads legados vierem preenchidos.
     */
    public Double distanceKm;
    
    public Boolean enableDiscontinuedMaterials;
    public Boolean enablePresalesMaterials;
    public Boolean enableAllMaterials;
    
    public String multipleMinimumTransferLotSizeUomId;
    public Double minimumTransferLotSize;
    public Double multipleTransfer;
    
    public Boolean active;

    @EqualsAndHashCode
    public static class LinhaTransportePrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte> {

        public String supplyNetworkVersionId;
        public String originLocationId;
        public String destinationLocationId;

        @JsonCreator
        public LinhaTransportePrimaryKeyIntegrationDTO(
                @JsonProperty("supplyNetworkVersionId") String supplyNetworkVersionId,
                @JsonProperty("originLocationId") String originLocationId,
                @JsonProperty("destinationLocationId") String destinationLocationId) {
            this.supplyNetworkVersionId = supplyNetworkVersionId;
            this.originLocationId = originLocationId;
            this.destinationLocationId = destinationLocationId;
        }

        @Override
        public boolean hasSameKeyAsEntity(LinhaTransporte entity) {
            return (entity.getVersaoMalha().getId().equals(this.supplyNetworkVersionId)
                    && entity.getLocationOrigem().getId().equals(this.originLocationId)
                    && entity.getLocationDestino().getId().equals(this.destinationLocationId));
        }

    }
    
}
