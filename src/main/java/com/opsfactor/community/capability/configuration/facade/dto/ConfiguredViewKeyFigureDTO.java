package com.opsfactor.community.capability.configuration.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;

/**
 * DTO de key figure configurada em uma view.
 *
 * <p>No Community a selecao dinamica de KFs para Planning Book fica bloqueada
 * pelas bordas de service/runtime info. Este DTO permanece para leitura de
 * configuracoes legadas e para overlays Enterprise.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfiguredViewKeyFigureDTO {
    
    public String keyFigure;
    public Boolean allowChanges;
    public Integer position;

    // não são mapeados diretamente via AutoMapper : apenas para carregar informações da visão no momento da remoção ----------------------
    public String userId;
    public String viewName;
    public ConfiguredView.TipoView viewType;
    
}
