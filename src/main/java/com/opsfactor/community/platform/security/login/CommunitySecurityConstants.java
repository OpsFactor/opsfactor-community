package com.opsfactor.community.platform.security.login;

/**
 * Constantes publicas do modelo de seguranca Community.
 * <p>
 * A edicao Community nao possui matriz granular de permissoes: todo usuario
 * funcional opera como administrador da instancia. O Enterprise deve introduzir
 * suas proprias authorities e fluxos avancados sem alterar este contrato basico.
 */
public final class CommunitySecurityConstants {

    public static final String COMMUNITY_ADMIN_ROLE = "ROLE_ADMIN";

    private CommunitySecurityConstants() {

    }

}
