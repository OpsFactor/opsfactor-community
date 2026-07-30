package com.opsfactor.community.web.configuration;

/**
 * Role unica exposta pelo OpsFactor Community.
 *
 * <p>As permissoes por dominio existem apenas no Enterprise. No Community, o
 * login converte exclusivamente {@code ROLE_ADMIN} em autoridade Spring
 * Security, a API de usuarios so aceita esta role e os controllers de
 * integracao dinamica tambem usam apenas esta constante.</p>
 */
public enum UserRoleType {
    
    ROLE_ADMIN
    
}
