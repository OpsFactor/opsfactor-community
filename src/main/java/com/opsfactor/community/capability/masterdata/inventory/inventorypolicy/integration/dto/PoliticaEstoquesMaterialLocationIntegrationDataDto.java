package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto;

import com.opsfactor.community.platform.integration.validation.IntegrationFileColumn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO de data upload Community para a regra material/location de politica de estoque.
 *
 * <p>O contrato cobre apenas parametros operacionais de safety stock, DRP e
 * Kanban simples consumidos pelo Supply Planning heuristico. A frequencia de
 * reabastecimento permanece como campo transicional do schema compartilhado
 * apenas para rejeicao explicita de payloads Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoliticaEstoquesMaterialLocationIntegrationDataDto extends IntegrationDataDtoAbstract<PoliticaEstoquesMaterialLocationIntegrationDataDto, PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO, PoliticaEstoquesMaterialLocation> {

    /**
     * Modelo operacional especifico da combinacao material/location. Valor nulo
     * preserva o default resolvido via projection.
     */
    @IntegrationFileColumn(3)
    public Constantes.SNPModeloOperacional operationalModel;

    /**
     * Modelo de reposicao operacional. Valor nulo significa DRP.
     */
    @IntegrationFileColumn(4)
    public Constantes.SNPModeloReabastecimento reorderModel;

    /**
     * Forma de interpretacao do safety stock. Valor nulo significa DAYS.
     */
    @IntegrationFileColumn(5)
    public Constantes.SNPCalculoSafetyStock safetyStockType;

    /**
     * Valor de safety stock DRP ou target stock de Kanban.
     */
    @IntegrationFileColumn(6)
    public Double drpSafetyStockOrKanbanTargetStockValue;

    /**
     * Estoque maximo operacional de DRP.
     */
    @IntegrationFileColumn(7)
    public Double drpMaximumStockValue;

    /**
     * Campo Enterprise transicional.
     *
     * <p>No Community qualquer valor preenchido falha com
     * {@code RequiresEnterpriseVersionException}; o mapper tambem nao publica
     * esta coluna em headers ou exports.</p>
     */
    public Double reorderFrequencyDays;

    /**
     * Chave publica da regra material/location dentro de uma politica.
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO, PoliticaEstoquesMaterialLocation> {

        /**
         * Politica que define vigencia e prioridade.
         */
        @IntegrationFileColumn(0)
        public String inventoryPolicyId;

        /**
         * Material da DFU operacional.
         */
        @IntegrationFileColumn(1)
        public String materialId;

        /**
         * Location da DFU operacional.
         */
        @IntegrationFileColumn(2)
        public String locationId;

        @Override
        public boolean hasSameKeyAsEntity(PoliticaEstoquesMaterialLocation entity) {

            return entity.getPoliticaEstoques().getId().equals(this.inventoryPolicyId)
                    && entity.getMaterial().getId().equals(this.materialId)
                    && entity.getLocation().getId().equals(this.locationId);

        }

    }

}
