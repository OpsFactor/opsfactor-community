package com.opsfactor.community.capability.configuration.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO de data upload Community para parametros operacionais material/location.
 *
 * <p>O contrato cobre dados necessarios para planejamento heuristico e
 * projections material/location. Frequencia de reabastecimento e
 * caracteristicas de DFU permanecem no schema compartilhado apenas para
 * rejeicao explicita de payloads Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParametrosMaterialLocationIntegrationDataDto extends IntegrationDataDtoAbstract<ParametrosMaterialLocationIntegrationDataDto, ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO, ParametrosProdutoLocation> {

    public Boolean active;
    
    public LocalDateTime introductionDate;
    public LocalDateTime discontinuationDate;

    /*
     * Campo transicional de schema compartilhado. No Community, qualquer valor
     * preenchido e rejeitado porque frequencia de reabastecimento pertence a
     * otimizacao de politica de estoques Enterprise.
     */
    public Double reorderFrequencyDays;
    
    public String productionMinimumMultipleUomId;
    public Double productionMinimumQuantity;
    public Double productionMultipleQuantity;
    
    public String defaultUomId;
    
    public Integer frozenHorizonDpInDays;
    
    /*
     * Caracteristicas material-location/DFU sao Enterprise. O campo permanece
     * no DTO compartilhado para rejeitar payloads legados de forma explicita,
     * mas o mapper Community nao exporta nem persiste valores aqui.
     */
    public Map<String,String> valueByCharacteristic = new HashMap<>();

    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ParametrosMaterialLocationPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<ParametrosMaterialLocationPrimaryKeyIntegrationDTO, ParametrosProdutoLocation> {

        public String locationId;
        public String materialId;

        @Override
        public boolean hasSameKeyAsEntity(ParametrosProdutoLocation entity) {
            return entity.getProduto().getId().equals(this.materialId)
                    && entity.getLocation().getId().equals(this.locationId);
        }

    }
    
    
}
