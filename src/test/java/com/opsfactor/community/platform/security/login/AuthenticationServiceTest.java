package com.opsfactor.community.platform.security.login;

import com.opsfactor.community.platform.security.login.model.User;
import com.opsfactor.community.platform.security.login.model.UserRole;
import com.opsfactor.community.platform.security.login.model.UserRole.UserRoleCompositeKey;
import com.opsfactor.community.platform.security.login.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

/**
 * Contratos Community do service comum de autenticacao.
 *
 * <p>A borda de seguranca aberta e propositalmente simples, mas saves internos
 * de usuario ainda precisam validar identidade minima e snapshot salvo para nao
 * mascarar repository quebrado como cadastro concluido.</p>
 */
public class AuthenticationServiceTest {

    @AfterEach
    public void clearSecurityContext() {

        SecurityContextHolder.clearContext();

    }

    @Test
    public void getAuthenticationShouldRejectMissingSecurityContextAuthentication() {

        AuthenticationService authenticationService = new AuthenticationService();

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                authenticationService::getAuthentication);

        Assertions.assertEquals(
                "Community security context authentication is required.",
                illegalStateException.getMessage());

    }

    @Test
    public void getUserShouldRejectMissingAuthenticatedUserIdBeforeRepository() {

        AuthenticationService authenticationService = new AuthenticationService();
        Authentication authentication = Mockito.mock(Authentication.class);

        Mockito.when(authentication.getName()).thenReturn(" ");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                authenticationService::getUser);

        Assertions.assertEquals(
                "Community authenticated user id is required.",
                illegalStateException.getMessage());

    }

    @Test
    public void getUserByIdShouldRejectMissingLookupIdBeforeRepository() {

        AuthenticationService authenticationService = new AuthenticationService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.getUser(" "));

        Assertions.assertEquals(
                "Community user id lookup is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getUserByIdShouldRejectNullOptionalFromRepository() {

        AuthenticationService authenticationService = new AuthenticationService();
        UserRepository userRepository = Mockito.mock(UserRepository.class);

        Mockito.when(userRepository.findByIdIgnoreCase("admin")).thenReturn(null);
        ReflectionTestUtils.setField(authenticationService, "userRepository", userRepository);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> authenticationService.getUser("admin"));

        Assertions.assertEquals(
                "Community user repository returned null Optional for user admin.",
                illegalStateException.getMessage());

    }

    @Test
    public void saveUserShouldRejectMissingUserBeforeRepository() {

        AuthenticationService authenticationService = new AuthenticationService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.saveUser(null));

        Assertions.assertEquals(
                "User to save is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveUserShouldRejectUserWithoutIdBeforeRepository() {

        AuthenticationService authenticationService = new AuthenticationService();
        User user = new User();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.saveUser(user));

        Assertions.assertEquals(
                "User to save must have an id.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveUserShouldPersistValidUserSnapshot() {

        AuthenticationService authenticationService = new AuthenticationService();
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        User user = getUser("admin");

        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        ReflectionTestUtils.setField(authenticationService, "userRepository", userRepository);

        authenticationService.saveUser(user);

        Mockito.verify(userRepository).save(user);

    }

    @Test
    public void saveUserShouldRejectNullSavedUserSnapshot() {

        AuthenticationService authenticationService = new AuthenticationService();
        UserRepository userRepository = Mockito.mock(UserRepository.class);

        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenReturn(null);
        ReflectionTestUtils.setField(authenticationService, "userRepository", userRepository);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> authenticationService.saveUser(getUser("admin")));

        Assertions.assertEquals(
                "Saved user snapshot is required.",
                illegalStateException.getMessage());

    }

    @Test
    public void saveUserShouldRejectSavedUserSnapshotWithDifferentId() {

        AuthenticationService authenticationService = new AuthenticationService();
        UserRepository userRepository = Mockito.mock(UserRepository.class);

        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenReturn(getUser("other"));
        ReflectionTestUtils.setField(authenticationService, "userRepository", userRepository);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> authenticationService.saveUser(getUser("admin")));

        Assertions.assertEquals(
                "Saved user id other does not match requested user id admin.",
                illegalStateException.getMessage());

    }

    @Test
    public void currentUserHasAnyRoleShouldValidateRequestedRolesBeforeAuthorities() {

        AuthenticationService authenticationService = new AuthenticationService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> authenticationService.currentUserHasAnyRole(null));

        Assertions.assertEquals(
                "Requested Community roles collection is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void currentUserHasAnyRoleShouldRejectBrokenAuthoritiesSnapshot() {

        AuthenticationService authenticationService = new AuthenticationService();
        Authentication authentication = Mockito.mock(Authentication.class);

        Mockito.doReturn(null).when(authentication).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        IllegalStateException nullAuthoritiesException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> authenticationService.currentUserHasAnyRole(List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)));
        Assertions.assertEquals(
                "Community authentication authorities snapshot is required.",
                nullAuthoritiesException.getMessage());

        GrantedAuthority grantedAuthority = Mockito.mock(GrantedAuthority.class);
        Mockito.when(grantedAuthority.getAuthority()).thenReturn(" ");
        Mockito.doReturn(List.of(grantedAuthority)).when(authentication).getAuthorities();

        IllegalStateException blankAuthorityException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> authenticationService.currentUserHasAnyRole(List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)));
        Assertions.assertEquals(
                "Community authentication authority at index 0 has no role name.",
                blankAuthorityException.getMessage());

    }

    @Test
    public void currentUserHasAnyRoleShouldUseValidatedAuthorities() {

        AuthenticationService authenticationService = new AuthenticationService();
        SecurityContextHolder.getContext().setAuthentication(getAuthenticationComAuthorities(
                CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE));

        Assertions.assertTrue(authenticationService.currentUserHasAnyRole(List.of(
                CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)));
        Assertions.assertFalse(authenticationService.currentUserHasAnyRole(List.of(
                "ROLE_ENTERPRISE_FINANCE")));

    }

    @Test
    public void isUserAdminShouldRejectBrokenPersistedRoleSnapshot() {

        AuthenticationService authenticationService = new AuthenticationService();
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        User user = getUser("admin");

        user.setUserRoles(null);
        Mockito.when(userRepository.findByIdIgnoreCase("admin")).thenReturn(Optional.of(user));
        ReflectionTestUtils.setField(authenticationService, "userRepository", userRepository);
        SecurityContextHolder.getContext().setAuthentication(getAuthenticationComAuthorities(
                CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                authenticationService::isUserAdmin);

        Assertions.assertEquals(
                "Authenticated Community user admin has no role snapshot.",
                illegalStateException.getMessage());

    }

    @Test
    public void isUserAdminShouldReadOnlyCommunityAdminRoleFromPersistedUser() {

        AuthenticationService authenticationService = new AuthenticationService();
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        User adminUser = getUser("admin");
        User enterpriseOnlyUser = getUser("enterprise");

        adminUser.getUserRoles().add(new UserRole(new UserRoleCompositeKey(
                adminUser,
                CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)));
        adminUser.getUserRoles().add(new UserRole(new UserRoleCompositeKey(
                adminUser,
                "ROLE_ENTERPRISE_FINANCE")));
        enterpriseOnlyUser.getUserRoles().add(new UserRole(new UserRoleCompositeKey(
                enterpriseOnlyUser,
                "ROLE_ENTERPRISE_FINANCE")));

        Mockito.when(userRepository.findByIdIgnoreCase("admin")).thenReturn(Optional.of(adminUser));
        Mockito.when(userRepository.findByIdIgnoreCase("enterprise")).thenReturn(Optional.of(enterpriseOnlyUser));
        ReflectionTestUtils.setField(authenticationService, "userRepository", userRepository);

        SecurityContextHolder.getContext().setAuthentication(getAuthenticationComAuthorities(
                CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE));
        Assertions.assertTrue(authenticationService.isUserAdmin());

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "enterprise",
                "password",
                List.of(new SimpleGrantedAuthority(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE))));
        Assertions.assertFalse(authenticationService.isUserAdmin());

    }

    @Test
    public void getUserRolesShouldSerializeValidatedAuthorities() {

        AuthenticationService authenticationService = new AuthenticationService();
        SecurityContextHolder.getContext().setAuthentication(getAuthenticationComAuthorities(
                CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE,
                "ROLE_ENTERPRISE_FINANCE"));

        Assertions.assertEquals(
                CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE + ",ROLE_ENTERPRISE_FINANCE",
                authenticationService.getUserRoles());

    }

    private static User getUser(String userId) {

        User user = new User();
        user.setId(userId);
        return user;

    }

    private static Authentication getAuthenticationComAuthorities(String... authorityNames) {

        List<GrantedAuthority> grantedAuthorityList = java.util.Arrays.stream(authorityNames)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return new UsernamePasswordAuthenticationToken("admin", "password", grantedAuthorityList);

    }

}
