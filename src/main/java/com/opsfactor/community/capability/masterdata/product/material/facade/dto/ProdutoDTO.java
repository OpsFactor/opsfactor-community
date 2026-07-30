package com.opsfactor.community.capability.masterdata.product.material.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO publico de material usado pelas telas e cargas Community.
 *
 * <p>A entidade fisica ainda se chama Produto em alguns pontos migrados, mas a
 * borda publica deve usar material como nomenclatura operacional.</p>
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProdutoDTO {
    /**
     * Não é o codigo do produto no BD e sim o campo codigoErp (não necessariamente um número)
     */
    public String id;
    public String description;
    public Boolean active;
    /** Status operacional do material na data atual. */
    public String materialStatus;

}
