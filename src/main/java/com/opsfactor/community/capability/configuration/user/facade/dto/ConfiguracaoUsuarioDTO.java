package com.opsfactor.community.capability.configuration.user.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO de preferencia simples de usuario.
 *
 * <p>Configuracoes avancadas de seguranca/tenant pertencem ao Enterprise; este
 * contrato guarda apenas chave escopo/parametro/valor usada pela UI.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfiguracaoUsuarioDTO {
    
    public String userId;
    public String scope;
    public String parameter;
    
    public String parameterValue;
        
}
