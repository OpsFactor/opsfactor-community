package com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO do recurso produtivo operacional Community.
 *
 * <p>O Community usa recursos produtivos apenas como capacidade basica em horas
 * totais por dia para o Supply Planning heuristico. Custos de recurso,
 * manutencao, turnos, line scheduling e capacidade por quantidade/UOM sao
 * capacidades Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecursoProdutivoIntegrationDataDto extends IntegrationDataDtoAbstract<RecursoProdutivoIntegrationDataDto, RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO, RecursoProdutivo> {

    public String locationId;
    public String description;
    public Boolean active;
    public Float efficiency;

    /**
     * Unidade usada quando a capacidade produtiva e medida em quantidade por
     * UOM. Esse modo de capacidade e Enterprise; no Community o perfil Supply
     * sempre usa total de horas por dia. O campo permanece no DTO compartilhado
     * apenas para que payloads/arquivos legados falhem de forma explicita.
     */
    public String capacityInQuantityUomId;

    @EqualsAndHashCode
    public static class RecursoProdutivoPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<RecursoProdutivoPrimaryKeyIntegrationDTO, RecursoProdutivo> {

        public String id;

        @JsonCreator
        public RecursoProdutivoPrimaryKeyIntegrationDTO(@JsonProperty("id") String id) {
            this.id = id;
        }

        @Override
        public boolean hasSameKeyAsEntity(RecursoProdutivo entity) {
            return entity.getId().equals(this.id);
        }

    }
    
}
