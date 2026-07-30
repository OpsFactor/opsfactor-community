package com.opsfactor.community.platform.security.login.service;

import com.opsfactor.community.platform.security.login.AuthenticationService;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import com.opsfactor.community.platform.security.login.model.User;
import com.opsfactor.community.platform.security.login.model.UserRole;
import com.opsfactor.community.platform.security.login.model.UserRole.UserRoleCompositeKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Contratos estruturais do login Community.
 *
 * <p>A edicao aberta autentica por usuario/senha, valida apenas usuario ativo e
 * materializa somente {@code ROLE_ADMIN} como authority Spring. Roles legadas
 * que eventualmente existam em banco antigo nao devem virar permissoes
 * funcionais nesta edicao.</p>
 */
class CustomUserDetailsServiceTest {

    @Test
    void customUserDetailsServiceShouldDeclareComponentAndExplicitAuthenticationServiceInjection() throws Exception {

        Assertions.assertTrue(CustomUserDetailsService.class.isAnnotationPresent(Component.class));

        Field authenticationServiceField =
                CustomUserDetailsService.class.getDeclaredField("authenticationService");
        Autowired autowired = authenticationServiceField.getAnnotation(Autowired.class);

        Assertions.assertEquals(AuthenticationService.class, authenticationServiceField.getType());
        Assertions.assertNotNull(autowired);
        Assertions.assertTrue(autowired.required());

    }

    @Test
    void loadUserByUsernameShouldRejectInactiveCommunityUserWithSpringSecurityException() {

        User user = criaUser("admin", false);
        CustomUserDetailsService customUserDetailsService = criaCustomUserDetailsService(user);

        DisabledException disabledException = Assertions.assertThrows(
                DisabledException.class,
                () -> customUserDetailsService.loadUserByUsername("admin"));

        Assertions.assertTrue(disabledException.getMessage().contains("not active"));

    }

    @Test
    void loadUserByUsernameShouldExposeOnlyCommunityAdminAuthority() {

        User user = criaUser("admin", true);
        user.getUserRoles().add(new UserRole(new UserRoleCompositeKey(
                user,
                CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)));
        user.getUserRoles().add(new UserRole(new UserRoleCompositeKey(
                user,
                "ROLE_DEMAND_PLANNING_EXECUTION")));
        user.getUserRoles().add(new UserRole(new UserRoleCompositeKey(
                user,
                "ROLE_SUPPLY_PLANNING_EXECUTION")));
        CustomUserDetailsService customUserDetailsService = criaCustomUserDetailsService(user);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin");

        Assertions.assertEquals("admin", userDetails.getUsername());
        Assertions.assertEquals(1, userDetails.getAuthorities().size());
        Assertions.assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE.equals(
                        grantedAuthority.getAuthority())));
        Assertions.assertTrue(userDetails.getAuthorities().stream()
                .noneMatch(grantedAuthority -> "ROLE_DEMAND_PLANNING_EXECUTION".equals(
                        grantedAuthority.getAuthority())));
        Assertions.assertTrue(userDetails.getAuthorities().stream()
                .noneMatch(grantedAuthority -> "ROLE_SUPPLY_PLANNING_EXECUTION".equals(
                        grantedAuthority.getAuthority())));

    }

    @Test
    void loadUserByUsernameShouldRejectNullOptionalFromAuthenticationService() {

        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        Mockito.when(authenticationService.getUser("admin")).thenReturn(null);
        CustomUserDetailsService customUserDetailsService = criaCustomUserDetailsService(authenticationService);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> customUserDetailsService.loadUserByUsername("admin"));

        Assertions.assertEquals(
                "Community authentication service returned null Optional for user lookup.",
                illegalStateException.getMessage());

    }

    @Test
    void loadUserByUsernameShouldRejectUserWithoutPasswordHashBeforeBuildingUserDetails() {

        User user = criaUser("admin", true);
        user.setPassword(null);
        CustomUserDetailsService customUserDetailsService = criaCustomUserDetailsService(user);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> customUserDetailsService.loadUserByUsername("admin"));

        Assertions.assertEquals(
                "Community login user password hash is required for user admin.",
                illegalStateException.getMessage());

    }

    @Test
    void loadUserByUsernameShouldRejectBrokenRoleSnapshotBeforeBuildingAuthorities() {

        User userSemRoles = criaUser("admin", true);
        userSemRoles.setUserRoles(null);
        CustomUserDetailsService serviceSemRoles = criaCustomUserDetailsService(userSemRoles);

        IllegalStateException rolesNulasException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceSemRoles.loadUserByUsername("admin"));
        Assertions.assertEquals(
                "Community login user admin has no role snapshot.",
                rolesNulasException.getMessage());

        User userComRoleNula = criaUser("admin", true);
        userComRoleNula.getUserRoles().add(null);
        CustomUserDetailsService serviceComRoleNula = criaCustomUserDetailsService(userComRoleNula);

        IllegalStateException roleNulaException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComRoleNula.loadUserByUsername("admin"));
        Assertions.assertEquals(
                "Community login user admin has null role at index 0 in role snapshot.",
                roleNulaException.getMessage());

        User userComRoleSemChave = criaUser("admin", true);
        userComRoleSemChave.getUserRoles().add(new UserRole());
        CustomUserDetailsService serviceComRoleSemChave = criaCustomUserDetailsService(userComRoleSemChave);

        IllegalStateException roleSemChaveException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComRoleSemChave.loadUserByUsername("admin"));
        Assertions.assertEquals(
                "Community login user admin has role at index 0 without composite key in role snapshot.",
                roleSemChaveException.getMessage());

        User userComRoleSemTipo = criaUser("admin", true);
        userComRoleSemTipo.getUserRoles().add(new UserRole(new UserRoleCompositeKey(userComRoleSemTipo, " ")));
        CustomUserDetailsService serviceComRoleSemTipo = criaCustomUserDetailsService(userComRoleSemTipo);

        IllegalStateException roleSemTipoException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> serviceComRoleSemTipo.loadUserByUsername("admin"));
        Assertions.assertEquals(
                "Community login user admin has role at index 0 without role type in role snapshot.",
                roleSemTipoException.getMessage());

    }

    private CustomUserDetailsService criaCustomUserDetailsService(User user) {

        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        Mockito.when(authenticationService.getUser(user.getId()))
                .thenReturn(Optional.of(user));

        return criaCustomUserDetailsService(authenticationService);

    }

    private CustomUserDetailsService criaCustomUserDetailsService(AuthenticationService authenticationService) {

        CustomUserDetailsService customUserDetailsService = new CustomUserDetailsService();
        ReflectionTestUtils.setField(
                customUserDetailsService,
                "authenticationService",
                authenticationService);

        return customUserDetailsService;

    }

    private User criaUser(String userId, boolean active) {

        User user = new User();
        user.setId(userId);
        user.setPassword("{noop}password");
        user.setActive(active);

        return user;

    }

}
