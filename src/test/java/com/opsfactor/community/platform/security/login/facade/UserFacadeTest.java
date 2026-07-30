package com.opsfactor.community.platform.security.login.facade;

import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import com.opsfactor.community.platform.security.login.facade.dto.UserDTO;
import com.opsfactor.community.platform.security.login.model.User;
import com.opsfactor.community.platform.security.login.model.UserRole;
import com.opsfactor.community.platform.security.login.model.UserRole.UserRoleCompositeKey;
import com.opsfactor.community.platform.security.login.repository.UserRepository;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserFacadeTest {

    @Test
    void userFrontServiceShouldDeclareServiceAndExplicitBeanFields() throws Exception {

        assertTrue(UserFacade.class.isAnnotationPresent(Service.class));
        assertRequiredAutowiredField("userRepository");
        assertRequiredAutowiredField("passwordEncoder");

    }

    @Test
    void saveUserDTOShouldNormalizeMissingRolesToCommunityAdmin() {

        UserRepository userRepository = mock(UserRepository.class);
        UserFacade userFrontService = criaUserFrontService(userRepository);
        User user = criaUser("admin");
        UserDTO userDTO = UserDTO.builder()
                .id("admin")
                .active(true)
                .build();

        when(userRepository.findById("admin")).thenReturn(Optional.of(user));

        userFrontService.saveUserDTO(userDTO);

        assertEquals(Set.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE), userDTO.userRoles);
        assertEquals(Set.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE), extraiUserRoleTypes(user));
        verify(userRepository).save(user);

    }

    @Test
    void saveUserDTOShouldRejectNullPasswordWhenCreatingUser() {

        UserRepository userRepository = mock(UserRepository.class);
        UserFacade userFrontService = criaUserFrontService(userRepository);
        UserDTO userDTO = UserDTO.builder()
                .id("new-admin")
                .active(true)
                .password(null)
                .build();

        when(userRepository.findById("new-admin")).thenReturn(Optional.empty());

        IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> userFrontService.saveUserDTO(userDTO));

        assertEquals(
                "Community user password is required when creating a user.",
                illegalArgumentException.getMessage());
        verify(userRepository, never()).save(any(User.class));

    }

    @Test
    void saveUserDTOShouldRejectEmptyPasswordWhenCreatingUser() {

        UserRepository userRepository = mock(UserRepository.class);
        UserFacade userFrontService = criaUserFrontService(userRepository);
        UserDTO userDTO = UserDTO.builder()
                .id("new-admin")
                .active(true)
                .password("")
                .build();

        when(userRepository.findById("new-admin")).thenReturn(Optional.empty());

        IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> userFrontService.saveUserDTO(userDTO));

        assertEquals(
                "Community user password is required when creating a user.",
                illegalArgumentException.getMessage());
        verify(userRepository, never()).save(any(User.class));

    }

    @Test
    void saveUserDTOShouldPreservePasswordWhenUpdatingWithNullPassword() {

        UserRepository userRepository = mock(UserRepository.class);
        UserFacade userFrontService = criaUserFrontService(userRepository);
        User user = criaUser("admin");
        String passwordHashOriginal = new BCryptPasswordEncoder().encode("current-password");
        user.setPassword(passwordHashOriginal);
        UserDTO userDTO = UserDTO.builder()
                .id("admin")
                .active(true)
                .password(null)
                .build();

        when(userRepository.findById("admin")).thenReturn(Optional.of(user));

        userFrontService.saveUserDTO(userDTO);

        assertEquals(passwordHashOriginal, user.getPassword());
        verify(userRepository).save(user);

    }

    @Test
    void saveUserDTOShouldRejectBlankPasswordWhenUpdatingUser() {

        UserRepository userRepository = mock(UserRepository.class);
        UserFacade userFrontService = criaUserFrontService(userRepository);
        User user = criaUser("admin");
        String passwordHashOriginal = new BCryptPasswordEncoder().encode("current-password");
        user.setPassword(passwordHashOriginal);
        UserDTO userDTO = UserDTO.builder()
                .id("admin")
                .active(true)
                .password("")
                .build();

        when(userRepository.findById("admin")).thenReturn(Optional.of(user));

        IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> userFrontService.saveUserDTO(userDTO));

        assertEquals(
                "Community user password must be null to preserve the existing password or non-blank to replace it.",
                illegalArgumentException.getMessage());
        assertEquals(passwordHashOriginal, user.getPassword());
        verify(userRepository, never()).save(any(User.class));

    }

    @Test
    void saveUserDTOShouldRejectBrokenCreatedUserSnapshot() {

        UserRepository userRepository = mock(UserRepository.class);
        UserFacade userFrontService = criaUserFrontService(userRepository);
        UserDTO userDTO = UserDTO.builder()
                .id("new-admin")
                .active(true)
                .password("new-password")
                .build();

        when(userRepository.findById("new-admin")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(null);

        IllegalStateException illegalStateException = assertThrows(
                IllegalStateException.class,
                () -> userFrontService.saveUserDTO(userDTO));

        assertEquals(
                "Community user creation save returned invalid snapshot.",
                illegalStateException.getMessage());

    }

    @Test
    void saveUserDTOShouldRejectBrokenFinalSavedUserSnapshot() {

        UserRepository userRepository = mock(UserRepository.class);
        UserFacade userFrontService = criaUserFrontService(userRepository);
        User user = criaUser("admin");
        UserDTO userDTO = UserDTO.builder()
                .id("admin")
                .active(true)
                .build();

        when(userRepository.findById("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(null);

        IllegalStateException illegalStateException = assertThrows(
                IllegalStateException.class,
                () -> userFrontService.saveUserDTO(userDTO));

        assertEquals(
                "Community user save returned invalid snapshot.",
                illegalStateException.getMessage());

    }

    @Test
    void saveUserDTOShouldRejectLoadedUserWithoutRoleSnapshotBeforeMutation() {

        UserRepository userRepository = mock(UserRepository.class);
        UserFacade userFrontService = criaUserFrontService(userRepository);
        User user = criaUser("admin");
        user.setUserRoles(null);
        UserDTO userDTO = UserDTO.builder()
                .id("admin")
                .active(true)
                .build();

        when(userRepository.findById("admin")).thenReturn(Optional.of(user));

        IllegalStateException illegalStateException = assertThrows(
                IllegalStateException.class,
                () -> userFrontService.saveUserDTO(userDTO));

        assertEquals(
                "Community user loaded for save has no role snapshot.",
                illegalStateException.getMessage());

    }

    @Test
    void saveUserDTOShouldRejectFinalSavedUserWithoutRoleSnapshot() {

        UserRepository userRepository = mock(UserRepository.class);
        UserFacade userFrontService = criaUserFrontService(userRepository);
        User user = criaUser("admin");
        User userSalvoSemRoles = criaUser("admin");
        userSalvoSemRoles.setUserRoles(null);
        UserDTO userDTO = UserDTO.builder()
                .id("admin")
                .active(true)
                .build();

        when(userRepository.findById("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(userSalvoSemRoles);

        IllegalStateException illegalStateException = assertThrows(
                IllegalStateException.class,
                () -> userFrontService.saveUserDTO(userDTO));

        assertEquals(
                "Community user saved snapshot has no role snapshot.",
                illegalStateException.getMessage());

    }

    @Test
    void saveUserDTOShouldRejectMissingPayloadBeforeRepository() {

        UserFacade userFrontService = new UserFacade();

        IllegalArgumentException missingPayloadException = assertThrows(
                IllegalArgumentException.class,
                () -> userFrontService.saveUserDTO(null));
        assertEquals(
                "Community user payload is required.",
                missingPayloadException.getMessage());

        UserDTO userDTO = UserDTO.builder()
                .id(" ")
                .build();

        IllegalArgumentException missingIdException = assertThrows(
                IllegalArgumentException.class,
                () -> userFrontService.saveUserDTO(userDTO));
        assertEquals(
                "Community user id is required.",
                missingIdException.getMessage());

    }

    @Test
    void saveUserDTOShouldRejectEnterpriseRoles() {

        UserFacade userFrontService = criaUserFrontService(mock(UserRepository.class));
        UserDTO userDTO = UserDTO.builder()
                .id("enterprise-user")
                .active(true)
                .userRoles(new HashSet<>(Set.of(
                        CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE,
                        "ROLE_DEMAND_PLANNING")))
                .build();

        assertThrows(IllegalArgumentException.class, () -> userFrontService.saveUserDTO(userDTO));

    }

    @Test
    void getUserDTOListShouldExposeOnlyCommunityAdminRole() {

        UserRepository userRepository = mock(UserRepository.class);
        UserFacade userFrontService = criaUserFrontService(userRepository);
        User adminUser = criaUser("admin", CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE, "ROLE_SUPPLY_PLANNING");
        User enterpriseOnlyUser = criaUser("enterprise-only", "ROLE_DEMAND_PLANNING");

        when(userRepository.findAll()).thenReturn(List.of(adminUser, enterpriseOnlyUser));

        List<UserDTO> userDTOList = userFrontService.getUserDTOList();

        assertEquals(Set.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE), userDTOList.get(0).userRoles);
        assertEquals(Set.of(), userDTOList.get(1).userRoles);

    }

    @Test
    void getUserDTOListShouldRejectBrokenRepositorySnapshotsBeforeMapping() {

        UserRepository userRepositoryComListaNula = mock(UserRepository.class);
        UserFacade serviceComListaNula = criaUserFrontService(userRepositoryComListaNula);
        when(userRepositoryComListaNula.findAll()).thenReturn(null);

        IllegalStateException nullListException = assertThrows(
                IllegalStateException.class,
                serviceComListaNula::getUserDTOList);
        assertEquals(
                "Community user repository returned null list.",
                nullListException.getMessage());

        UserRepository userRepositoryComItemNulo = mock(UserRepository.class);
        UserFacade serviceComItemNulo = criaUserFrontService(userRepositoryComItemNulo);
        when(userRepositoryComItemNulo.findAll()).thenReturn(java.util.Arrays.asList((User) null));

        IllegalStateException nullItemException = assertThrows(
                IllegalStateException.class,
                serviceComItemNulo::getUserDTOList);
        assertEquals(
                "Community user at index 0 is required in repository snapshot.",
                nullItemException.getMessage());

        UserRepository userRepositoryComUsuarioSemId = mock(UserRepository.class);
        UserFacade serviceComUsuarioSemId = criaUserFrontService(userRepositoryComUsuarioSemId);
        when(userRepositoryComUsuarioSemId.findAll()).thenReturn(List.of(new User()));

        IllegalStateException missingIdException = assertThrows(
                IllegalStateException.class,
                serviceComUsuarioSemId::getUserDTOList);
        assertEquals(
                "Community user at index 0 has no id in repository snapshot.",
                missingIdException.getMessage());

        UserRepository userRepositoryComRolesNulas = mock(UserRepository.class);
        UserFacade serviceComRolesNulas = criaUserFrontService(userRepositoryComRolesNulas);
        User userComRolesNulas = criaUser("admin");
        userComRolesNulas.setUserRoles(null);
        when(userRepositoryComRolesNulas.findAll()).thenReturn(List.of(userComRolesNulas));

        IllegalStateException nullRolesException = assertThrows(
                IllegalStateException.class,
                serviceComRolesNulas::getUserDTOList);
        assertEquals(
                "Community user at index 0 has no role snapshot.",
                nullRolesException.getMessage());

    }

    @Test
    void getUserDTOListShouldRejectBrokenRoleEntriesBeforeMapping() {

        UserRepository userRepositoryComRoleNula = mock(UserRepository.class);
        UserFacade serviceComRoleNula = criaUserFrontService(userRepositoryComRoleNula);
        User userComRoleNula = criaUser("admin");
        userComRoleNula.getUserRoles().add(null);
        when(userRepositoryComRoleNula.findAll()).thenReturn(List.of(userComRoleNula));

        IllegalStateException nullRoleException = assertThrows(
                IllegalStateException.class,
                serviceComRoleNula::getUserDTOList);
        assertEquals(
                "Community user at index 0 has null role at index 0 in role snapshot.",
                nullRoleException.getMessage());

        UserRepository userRepositoryComRoleSemChave = mock(UserRepository.class);
        UserFacade serviceComRoleSemChave = criaUserFrontService(userRepositoryComRoleSemChave);
        User userComRoleSemChave = criaUser("admin");
        userComRoleSemChave.getUserRoles().add(new UserRole());
        when(userRepositoryComRoleSemChave.findAll()).thenReturn(List.of(userComRoleSemChave));

        IllegalStateException roleSemChaveException = assertThrows(
                IllegalStateException.class,
                serviceComRoleSemChave::getUserDTOList);
        assertEquals(
                "Community user at index 0 has role at index 0 without composite key in role snapshot.",
                roleSemChaveException.getMessage());

        UserRepository userRepositoryComRoleSemTipo = mock(UserRepository.class);
        UserFacade serviceComRoleSemTipo = criaUserFrontService(userRepositoryComRoleSemTipo);
        User userComRoleSemTipo = criaUser("admin", " ");
        when(userRepositoryComRoleSemTipo.findAll()).thenReturn(List.of(userComRoleSemTipo));

        IllegalStateException roleSemTipoException = assertThrows(
                IllegalStateException.class,
                serviceComRoleSemTipo::getUserDTOList);
        assertEquals(
                "Community user at index 0 has role at index 0 without role type in role snapshot.",
                roleSemTipoException.getMessage());

    }

    private UserFacade criaUserFrontService(UserRepository userRepository) {

        UserFacade userFrontService = new UserFacade();
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        setPrivateField(userFrontService, "userRepository", userRepository);
        setPrivateField(userFrontService, "passwordEncoder", new BCryptPasswordEncoder());
        return userFrontService;

    }

    private static void setPrivateField(UserFacade userFrontService, String fieldName, Object value) {

        try {
            Field field = UserFacade.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(userFrontService, value);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("Unable to configure UserFrontService test field " + fieldName, reflectiveOperationException);
        }

    }

    private User criaUser(String id, String... userRoleTypes) {

        User user = new User();
        user.setId(id);
        for (String userRoleType : userRoleTypes) {
            user.getUserRoles().add(new UserRole(new UserRoleCompositeKey(user, userRoleType)));
        }
        return user;

    }

    private Set<String> extraiUserRoleTypes(User user) {

        Set<String> userRoleTypes = new HashSet<>();
        for (UserRole userRole : user.getUserRoles()) {
            userRoleTypes.add(userRole.getUserRoleType());
        }
        return userRoleTypes;

    }

    private static void assertRequiredAutowiredField(String fieldName) throws Exception {

        Field field = UserFacade.class.getDeclaredField(fieldName);
        Autowired autowired = field.getAnnotation(Autowired.class);

        assertNotNull(autowired);
        assertTrue(autowired.required());

    }

}
