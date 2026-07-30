package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO de integracao da versao de malha Community.
 *
 * <p>A versao de malha continua no Community porque o planejamento heuristico
 * precisa escolher explicitamente a rede operacional usada por linhas de
 * transporte, Supply Plan e filtros relacionados. O contrato publica somente a
 * identidade da malha, descricao e origens padrao operacionais; mapa, frota,
 * custos logisticos e analises de rede seguem fora desta edicao.</p>
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VersaoMalhaIntegrationDataDto extends IntegrationDataDtoAbstract<
        VersaoMalhaIntegrationDataDto,
        VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO,
        VersaoMalha> {

    public String description;
    public String defaultClientOriginLocationId;
    public String defaultRawMaterialOriginLocationId;
    public Double defaultRawMaterialOriginLeadTimeDays;

    /**
     * Chave primaria publica da versao de malha.
     */
    @EqualsAndHashCode
    public static class VersaoMalhaPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<
            VersaoMalhaPrimaryKeyIntegrationDTO,
            VersaoMalha> {

        public String supplyNetworkVersionId;

        @JsonCreator
        public VersaoMalhaPrimaryKeyIntegrationDTO(
                @JsonProperty("supplyNetworkVersionId") String supplyNetworkVersionId) {

            this.supplyNetworkVersionId = supplyNetworkVersionId;

        }

        @Override
        public boolean hasSameKeyAsEntity(VersaoMalha entity) {

            return entity.getId().equals(this.supplyNetworkVersionId);

        }

    }

}
