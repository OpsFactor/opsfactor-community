package com.opsfactor.community.platform.security.login.service;

import com.opsfactor.community.platform.security.login.AuthenticationService;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import com.opsfactor.community.platform.security.login.model.User;
import com.opsfactor.community.platform.security.login.model.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador Community entre a entidade {@link User} e o contrato
 * {@link UserDetailsService} do Spring Security.
 *
 * <p>O simples fato de ser marcado como {@link Component} permite ao Spring
 * Security localizar este serviço automaticamente. A implementação permanece
 * pequena para expor somente login/senha e {@code ROLE_ADMIN}; SSO, JWT,
 * lockout e roles granulares pertencem ao Enterprise.</p>
 */
@Slf4j
@Component
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Porta funcional de consulta de usuarios usada no login. O @Autowired fica
     * explicito para separar beans Spring de estado local da classe.
     */
    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Carrega o usuario e suas roles dentro de uma transacao curta para que a
     * colecao lazy {@link User#getUserRoles()} possa ser convertida em
     * authorities do Spring Security durante o login.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userId)
            throws UsernameNotFoundException {
        
        log.info("Login attempt for user={}", userId);
        
        Optional<User> userOptional = authenticationService.getUser(userId);
        if (userOptional == null) {
            throw new IllegalStateException("Community authentication service returned null Optional for user lookup.");
        }

        User user = userOptional
                .orElseThrow(() -> {
                    log.warn("Login failed because user={} was not found", userId);
                    return new UsernameNotFoundException("Username not found");
                });
        validaUserSnapshotLoginCommunity(user, userId);

        /*
         * Community opera com segurança intencionalmente simples:
         * usuário/senha, flag de ativo e ROLE_ADMIN. Lockout por IP,
         * desbloqueio automático e administração de tentativas pertencem
         * ao Enterprise e não devem influenciar o login desta edição.
         */
        if (!user.getActive()) {
            throw new DisabledException("User is not active");
        }

        return new org.springframework.security.core.userdetails.User(user.getId(), user.getPassword(),
                user.getActive(), true, true, true, getGrantedAuthorities(user));

    }

    /**
     * Valida a entidade carregada para o login Community.
     *
     * <p>O login precisa de id, hash de senha e lista de roles estruturalmente
     * integra para construir o {@link UserDetails}. Roles Enterprise legadas
     * podem existir e serao ignoradas depois, mas item nulo, chave composta
     * ausente ou tipo de role vazio indicam snapshot corrompido.</p>
     */
    private void validaUserSnapshotLoginCommunity(User user, String userIdSolicitado) {

        if (user == null) {
            throw new IllegalStateException("Community login user snapshot is required.");
        }
        if (user.getId() == null || user.getId().isBlank()) {
            throw new IllegalStateException(
                    "Community login user id is required for requested user " + userIdSolicitado + ".");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalStateException(
                    "Community login user password hash is required for user " + user.getId() + ".");
        }
        if (user.getUserRoles() == null) {
            throw new IllegalStateException(
                    "Community login user " + user.getId() + " has no role snapshot.");
        }

        for (int indiceRole = 0; indiceRole < user.getUserRoles().size(); indiceRole++) {
            UserRole userRole = user.getUserRoles().get(indiceRole);
            if (userRole == null) {
                throw new IllegalStateException(
                        "Community login user "
                                + user.getId()
                                + " has null role at index "
                                + indiceRole
                                + " in role snapshot.");
            }
            if (userRole.getUserRoleCompositeKey() == null) {
                throw new IllegalStateException(
                        "Community login user "
                                + user.getId()
                                + " has role at index "
                                + indiceRole
                                + " without composite key in role snapshot.");
            }
            if (userRole.getUserRoleType() == null || userRole.getUserRoleType().isBlank()) {
                throw new IllegalStateException(
                        "Community login user "
                                + user.getId()
                                + " has role at index "
                                + indiceRole
                                + " without role type in role snapshot.");
            }
        }

    }

    private List<GrantedAuthority> getGrantedAuthorities(User user) {

        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        
        if (!user.getActive()) {
            log.warn("User={} is not active. No authorities are assigned", user.getId());
            return authorities;
        }
        
        for (UserRole userRole : user.getUserRoles()) {
            /*
             * Community usa segurança simples: apenas ROLE_ADMIN vira
             * autoridade efetiva. Roles granulares eventualmente presentes em
             * bancos legados são ignoradas nesta edição e devem ser
             * reintroduzidas pelo Enterprise.
             */
            if (CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE.equals(userRole.getUserRoleType())) {
                authorities.add(new SimpleGrantedAuthority(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE));
            }
        }
        return authorities;
        
    }
}
