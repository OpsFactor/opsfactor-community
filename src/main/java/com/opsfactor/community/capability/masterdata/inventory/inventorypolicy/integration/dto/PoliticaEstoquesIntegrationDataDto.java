package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * DTO de data upload Community para o cabecalho de politica operacional de estoque.
 *
 * <p>A politica permanece no Community porque define prioridade e vigencia das
 * regras de safety stock usadas pelo Supply Planning heuristico. Dados de
 * simulacao, resultado otimizado ou parametrizacao economica da otimizacao de
 * politica de estoques ficam fora deste contrato.</p>
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoliticaEstoquesIntegrationDataDto extends IntegrationDataDtoAbstract<PoliticaEstoquesIntegrationDataDto, PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO, PoliticaEstoques> {

    /**
     * Prioridade operacional usada para desempatar politicas vigentes.
     */
    public Integer priority;

    /**
     * Inicio da vigencia da politica.
     */
    public LocalDateTime startDateTime;

    /**
     * Fim da vigencia da politica.
     */
    public LocalDateTime endDateTime;

    /**
     * Chave publica da politica operacional de estoque.
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PoliticaEstoquesPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<PoliticaEstoquesPrimaryKeyIntegrationDTO, PoliticaEstoques> {

        /**
         * Identificador funcional da politica.
         */
        public String id;

        @Override
        public boolean hasSameKeyAsEntity(PoliticaEstoques entity) {

            return entity.getId().equals(this.id);

        }

    }

}
