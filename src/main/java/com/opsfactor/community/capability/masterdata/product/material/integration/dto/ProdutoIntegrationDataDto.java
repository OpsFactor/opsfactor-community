package com.opsfactor.community.capability.masterdata.product.material.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO de data upload Community para materiais.
 *
 * <p>O nome fisico ainda acompanha a entidade `Produto`, mas o contrato publico
 * novo deve ser lido como material. Caracteristicas dinamicas ficam no schema
 * compartilhado apenas para bloqueio explicito no mapper Community.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProdutoIntegrationDataDto extends IntegrationDataDtoAbstract<ProdutoIntegrationDataDto, ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO, Produto> {

    public String description;
    public Boolean active;
    public Constantes.StatusProduto lifecycleStage;
    /**
     * Modelo operacional do material para Supply Planning: MTS ou MTO.
     *
     * <p>Quando ausente, o agregado preserva seu fallback historico para
     * MTS. O campo e compartilhado porque e consumido pela politica de
     * estoques Community, sem exigir capability ou tabela Enterprise.</p>
     */
    public Constantes.SNPModeloOperacional operationalModel;
    public LocalDateTime introductionDate;
    public LocalDateTime discontinuationDate;
    public String defaultUomId;
    public String salesUomId;
    public String transferUomId;

    /**
     * COGS unitario Enterprise do proprio material. O Community conserva o
     * campo para rejeitar payloads privados de forma explicita.
     */
    public Double unitCogs;

    /**
     * Id da UOM cadastrada para {@link #unitCogs}; quando ausente, o runtime
     * Enterprise aplica a regra economica de fallback propria.
     */
    public String unitCogsUnitOfMeasureId;

    /**
     * Caracteristicas de material sao Enterprise. O campo permanece no DTO
     * compartilhado para compatibilidade de contrato e para que o mapper
     * Community consiga rejeitar payloads Enterprise com mensagem explicita.
     */
    public Map<String,String> valueByCharacteristic = new HashMap<>();

    @EqualsAndHashCode
    public static class ProdutoPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<ProdutoPrimaryKeyIntegrationDTO, Produto> {

        public String id;

        @JsonCreator
        public ProdutoPrimaryKeyIntegrationDTO(@JsonProperty("id") String id) {
            this.id = id;
        }

        @Override
        public boolean hasSameKeyAsEntity(Produto entity) {
            return entity.getId().equals(this.id);
        }

    }

    /**
     * DTO usado exclusivamente para filtrar desativacao em lote de materiais.
     */
    public static class MaterialDeactivationFilterIntegrationDTO {
        public List<String> id;
    }


}
