package com.opsfactor.community.platform.security.login.facade.dto;

import java.util.Set;
import lombok.Builder;

/**
 * DTO publico de administracao simples de usuarios Community.
 *
 * <p>O campo {@link #password} e usado somente como entrada de criacao ou
 * troca de senha. Respostas de front-service nao devem preencher esse campo
 * com hash ou senha em claro.</p>
 */
@Builder
public class UserDTO {
    
    public String id = "";
    public String firstName = "";
    public String lastName = "";
    public String email = "";
    public Boolean active;
    
    // senha só para recebimento da informação, e não para envio p/ front-end
    public String password = "";

    public Set<String> userRoles;
    
}
