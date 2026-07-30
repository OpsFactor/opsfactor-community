package com.opsfactor.community.platform.security.login.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import lombok.Data;

/**
 * Usuario local da seguranca Community.
 *
 * <p>Esta entidade representa somente login/senha simples, dados cadastrais
 * basicos e a colecao de roles persistidas. O runtime Community usa apenas
 * {@code ROLE_ADMIN}; roles legadas eventualmente presentes na tabela sao
 * filtradas nos services de login/front e nao viram autorizacao funcional.</p>
 */
@Entity
@Data
public class User {

    /** Identificador de login usado pelo HTTP Basic. */
    @Id
    private String id;

    /** Hash da senha gerado pelo PasswordEncoder configurado no modulo de seguranca. */
    private String password;

    /** Nome exibido nas telas administrativas simples. */
    private String firstName;

    /** Sobrenome exibido nas telas administrativas simples. */
    private String lastName;

    /** E-mail meramente cadastral; nao ha fluxo SSO/OIDC associado no Community. */
    private String email;

    /** Usuario inativo nao autentica, mas nao ha lockout por tentativa/IP nesta edicao. */
    private Boolean active = true;

    /**
     * Roles persistidas para compatibilidade de schema.
     *
     * <p>O Community interpreta apenas {@code ROLE_ADMIN}. A lista fica lazy e
     * deve ser carregada por service/repository com fetch adequado quando usada
     * em lote.</p>
     */
    @OneToMany(mappedBy = "userRoleCompositeKey.user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserRole> userRoles = new ArrayList<>();

    /**
     * Mantem compatibilidade historica: ausencia de valor cadastrado significa
     * usuario ativo, nao bloqueio por politica avancada.
     */
    public boolean getActive() {
        return (active == null) ? true : active;
    }
    
}
