package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * DTO da transportation lane por material Community.
 *
 * <p>Complementa a lane origem/destino com material, lead time, prioridade,
 * lotes de transferencia e status. Distancia por material, custos de frete,
 * frota, veiculos e analises visuais de rede pertencem ao Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinhaTransporteProdutoIntegrationDataDto extends IntegrationDataDtoAbstract<LinhaTransporteProdutoIntegrationDataDto, LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO, LinhaTransporteProduto> {
 
    public Integer priority;
    public Integer leadTimeDays;

    /**
     * Distancia por material e Enterprise. No Community a distancia da malha
     * nao e usada pelo heuristico e payload preenchido deve ser bloqueado pelo
     * mapper.
     */
    public Double distanceKm;
    public String multipleMinimumTransferLotSizeUomId;
    public Double minimumTransferLotSize;
    public Double multipleTransfer;
    
    public Boolean active;

    @EqualsAndHashCode
    public static class LinhaTransporteProdutoPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<LinhaTransporteProdutoPrimaryKeyIntegrationDTO, LinhaTransporteProduto> {

        public String supplyNetworkVersionId;
        public String originLocationId;
        public String destinationLocationId;
        public String materialId;

        @JsonCreator
        public LinhaTransporteProdutoPrimaryKeyIntegrationDTO(
                @JsonProperty("supplyNetworkVersionId") String supplyNetworkVersionId,
                @JsonProperty("originLocationId") String originLocationId,
                @JsonProperty("destinationLocationId") String destinationLocationId,
                @JsonProperty("materialId") String materialId) {
            this.supplyNetworkVersionId = supplyNetworkVersionId;
            this.originLocationId = originLocationId;
            this.destinationLocationId = destinationLocationId;
            this.materialId = materialId;
        }

        @Override
        public boolean hasSameKeyAsEntity(LinhaTransporteProduto entity) {
            return (entity.getVersaoMalha().getId().equals(this.supplyNetworkVersionId)
                    && entity.getLocationOrigem().getId().equals(this.originLocationId)
                    && entity.getLocationDestino().getId().equals(this.destinationLocationId)
                    && entity.getProduto().getId().equals(this.materialId));
        }

    }

    /**
     * Filtro usado exclusivamente pelos endpoints de desativacao de lanes por
     * material.
     */
    public static class LinhaTransporteProdutoDeactivationFilterIntegrationDTO {
        public List<String> supplyNetworkVersionId;
        public List<LocationAbstract.TipoLocation> originLocationType;
        public List<LocationAbstract.TipoLocation> destinationLocationType;
    }

}
