package com.opsfactor.community.capability.configuration.facade.dto;

import lombok.Data;

/**
 * DTO publico dos parametros operacionais de material expostos pelo Community.
 *
 * <p>A entidade fisica ainda se chama `Produto` por compatibilidade de schema,
 * mas a superficie REST nova usa a nomenclatura funcional material.</p>
 */
@Data
public class ParametrosMaterialDTO {

    private String id;
    private String descricao;
    private Boolean foraLinha;
    private Boolean ativo;
    private Boolean novo;

}
