package com.opsfactor.community.platform.security.login;

import com.opsfactor.community.platform.security.login.model.User;
import com.opsfactor.community.platform.security.login.model.UserRole;
import com.opsfactor.community.platform.security.login.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servico de apoio para acesso ao usuario autenticado na edicao Community.
 *
 * <p>A seguranca Community e deliberadamente simples: autenticacao por
 * usuario/senha, usuario ativo/inativo e autoridade unica
 * {@link CommunitySecurityConstants#COMMUNITY_ADMIN_ROLE}. Este service mantem
 * os contratos historicos consumidos pelos controllers, mas nao deve introduzir
 * SSO, lockout, tenant, filtros por location ou matriz granular de permissoes.</p>
 */
@Slf4j
@Service
public class AuthenticationService {

    /**
     * Repository Community de usuarios. O campo permanece com @Autowired
     * explicito para deixar claro que este atributo e um bean Spring, nao dado
     * de estado local do service.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Retorna a autenticacao atual mantida no contexto do Spring Security.
     */
    public Authentication getAuthentication() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("Community security context authentication is required.");
        }

        return authentication;

    }

    /**
     * Verifica se o usuario logado possui ao menos uma authority informada.
     *
     * <p>Na Community a lista esperada deve conter apenas ROLE_ADMIN. O metodo
     * permanece generico porque controllers e services legados usam este ponto
     * para preservar assinaturas compartilhadas com o Enterprise.</p>
     */
    public boolean currentUserHasAnyRole(Collection<String> userRoles) {

        validaUserRolesSolicitadasCommunity(userRoles);

        Authentication authentication = getAuthentication();
        validaAuthoritiesAuthenticationCommunity(authentication);

        return authentication
                .getAuthorities()
                .stream().anyMatch(grantedAuthority -> userRoles.contains(grantedAuthority.getAuthority()));

    }

    /**
     * Busca a entidade do usuario autenticado pelo identificador presente no
     * contexto de seguranca.
     */
    public Optional<User> getUser() {

        String userId = getUserIdAuthenticationCommunity();
        return getUserOptionalRepositoryCommunity(userId);

    }

    /**
     * Busca um usuario especifico ignorando diferencas de caixa no identificador.
     */
    public Optional<User> getUser(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Community user id lookup is required.");
        }

        return getUserOptionalRepositoryCommunity(userId);

    }

    /**
     * Retorna o identificador do usuario autenticado ja validado.
     *
     * <p>Controllers Community devem usar este metodo em vez de acessar
     * diretamente o {@code SecurityContextHolder}, mantendo em um unico ponto a
     * regra de que contexto sem usuario e falha estrutural de seguranca.</p>
     */
    public String getAuthenticatedUserId() {

        return getUserIdAuthenticationCommunity();

    }

    /**
     * Persiste o usuario usando o repository Community.
     *
     * <p>A normalizacao de roles em chamadas vindas do front fica em
     * UserFrontService. Aqui validamos apenas a identidade minima de entrada e
     * o snapshot salvo, porque este service e usado como borda comum por
     * consumidores internos de seguranca.</p>
     */
    public void saveUser(User user) {

        validaUserParaSaveCommunity(user);
        User userSalvo = userRepository.save(user);
        validaUserSalvoCommunity(user.getId(), userSalvo);

    }

    private void validaUserParaSaveCommunity(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User to save is required.");
        }
        if (user.getId() == null || user.getId().isBlank()) {
            throw new IllegalArgumentException("User to save must have an id.");
        }

    }

    private void validaUserSalvoCommunity(
            String userIdEsperado,
            User userSalvo) {

        if (userSalvo == null) {
            throw new IllegalStateException("Saved user snapshot is required.");
        }
        if (userSalvo.getId() == null || userSalvo.getId().isBlank()) {
            throw new IllegalStateException("Saved user id is required.");
        }
        if (!userIdEsperado.equals(userSalvo.getId())) {
            throw new IllegalStateException(
                    "Saved user id "
                            + userSalvo.getId()
                            + " does not match requested user id "
                            + userIdEsperado
                            + ".");
        }

    }

    /**
     * Executa lookup de usuario garantindo que o repository respeitou o contrato
     * de devolver {@link Optional} real.
     */
    private Optional<User> getUserOptionalRepositoryCommunity(String userId) {

        Optional<User> userOptional = userRepository.findByIdIgnoreCase(userId);
        if (userOptional == null) {
            throw new IllegalStateException(
                    "Community user repository returned null Optional for user " + userId + ".");
        }

        return userOptional;

    }

    /**
     * Confirma se o usuario autenticado possui a unica authority funcional da
     * Community.
     *
     * <p>Se a autenticacao apontar para um usuario inexistente, falhamos
     * explicitamente. Um fallback silencioso para falso mascararia erro de
     * sessao, cache ou cadastro.</p>
     */
    public boolean isUserAdmin() {

        String userId = getUserIdAuthenticationCommunity();
        User user = userRepository.findByIdIgnoreCase(userId)
                .orElseThrow(() -> {
                    log.warn("Authenticated user={} was not found while checking admin role", userId);
                    return new UsernameNotFoundException("Username not found");
                });
        validaUserRolesSnapshotUsuarioCommunity(user, "Authenticated Community user " + userId);

        return user.getUserRoles().stream()
                .anyMatch(x -> x.getUserRoleType().equals(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE));

    }

    /**
     * Serializa as authorities efetivas do usuario autenticado para contratos
     * legados que esperam uma string simples separada por virgula.
     */
    public String getUserRoles() {

        Authentication authentication = getAuthentication();
        validaAuthoritiesAuthenticationCommunity(authentication);

        return authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.joining(","));

    }

    /**
     * Le o identificador do usuario autenticado com contrato explicito.
     *
     * <p>Controllers Community usam este service assumindo que a autenticacao
     * Spring ja foi resolvida. Se o contexto existir sem usuario, isso indica
     * falha de filtro/configuracao e nao deve virar consulta ao repository com
     * chave nula ou vazia.</p>
     */
    private String getUserIdAuthenticationCommunity() {

        String userId = getAuthentication().getName();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Community authenticated user id is required.");
        }

        return userId;

    }

    /**
     * Valida a lista de roles solicitada por callers internos.
     *
     * <p>Lista vazia significa que nenhum papel satisfaz a regra e retorna
     * falso. Lista nula ou item vazio, por outro lado, e bug do caller ou de
     * configuracao de controller e deve falhar antes de comparar authorities.</p>
     */
    private void validaUserRolesSolicitadasCommunity(Collection<String> userRoles) {

        if (userRoles == null) {
            throw new IllegalArgumentException("Requested Community roles collection is required.");
        }

        int indiceRole = 0;
        for (String userRole : userRoles) {
            if (userRole == null || userRole.isBlank()) {
                throw new IllegalArgumentException(
                        "Requested Community role at index " + indiceRole + " is required.");
            }
            indiceRole++;
        }

    }

    /**
     * Valida as authorities ja materializadas no contexto de seguranca.
     *
     * <p>No Community a authority efetiva esperada e `ROLE_ADMIN`, mas a lista
     * pode estar vazia para usuarios sem acesso administrativo. O que nao pode
     * existir e snapshot nulo ou authority sem nome, pois isso quebraria
     * comparacoes de permissao de forma silenciosa.</p>
     */
    private void validaAuthoritiesAuthenticationCommunity(Authentication authentication) {

        if (authentication.getAuthorities() == null) {
            throw new IllegalStateException("Community authentication authorities snapshot is required.");
        }

        int indiceAuthority = 0;
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            if (grantedAuthority == null) {
                throw new IllegalStateException(
                        "Community authentication authority at index " + indiceAuthority + " is required.");
            }
            if (grantedAuthority.getAuthority() == null || grantedAuthority.getAuthority().isBlank()) {
                throw new IllegalStateException(
                        "Community authentication authority at index " + indiceAuthority + " has no role name.");
            }
            indiceAuthority++;
        }

    }

    /**
     * Valida as roles persistidas do usuario usado em verificacoes internas.
     */
    private void validaUserRolesSnapshotUsuarioCommunity(User user, String contextoOperacional) {

        if (user.getUserRoles() == null) {
            throw new IllegalStateException(contextoOperacional + " has no role snapshot.");
        }

        for (int indiceRole = 0; indiceRole < user.getUserRoles().size(); indiceRole++) {
            UserRole userRole = user.getUserRoles().get(indiceRole);
            if (userRole == null) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " has null role at index "
                                + indiceRole
                                + " in role snapshot.");
            }
            if (userRole.getUserRoleCompositeKey() == null) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " has role at index "
                                + indiceRole
                                + " without composite key in role snapshot.");
            }
            if (userRole.getUserRoleType() == null || userRole.getUserRoleType().isBlank()) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " has role at index "
                                + indiceRole
                                + " without role type in role snapshot.");
            }
        }

    }

}
