package com.opsfactor.community.platform.security.login.model;

import java.io.Serializable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Associacao entre usuario local e authority Spring persistida.
 *
 * <p>O schema continua permitindo mais de uma role para facilitar transicao a
 * partir de bancos legados, mas a edicao Community somente materializa
 * {@code ROLE_ADMIN}. Qualquer matriz granular deve ser implementada no
 * Enterprise, sem alterar este contrato simples.</p>
 */
@Getter
@Setter
@EqualsAndHashCode(of="userRoleCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class UserRole implements Serializable {

    /** Chave composta por usuario e nome da authority. */
    @EmbeddedId
    @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
    private UserRoleCompositeKey userRoleCompositeKey;
    
    /**
     * Chave persistida da role de usuario.
     */
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class UserRoleCompositeKey implements Serializable {

        /** Usuario proprietario da role. */
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private User user;

        /*
         * Community persiste apenas
         * com.opsfactor.community.platform.security.login.CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE.
         * O prefixo ROLE_ e mantido por contrato do Spring Security.
         */
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private String userRoleType;

    }
        
    /**
     * Retorna o nome persistido da authority sem expor a chave composta aos
     * consumers funcionais.
     */
    public String getUserRoleType() {
        return userRoleCompositeKey.getUserRoleType();
    }
    
    /**
     * Retorna o usuario proprietario da role.
     */
    public User getUser() {
        return userRoleCompositeKey.getUser();
    }
    

}
