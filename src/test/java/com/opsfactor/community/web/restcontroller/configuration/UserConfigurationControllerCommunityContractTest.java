package com.opsfactor.community.web.restcontroller.configuration;

import com.opsfactor.community.capability.configuration.user.facade.dto.ConfiguracaoUsuarioDTO;
import com.opsfactor.community.capability.configuration.user.facade.dto.UserInterfacePreferencesDTO;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.facade.ConfiguracaoUsuarioFacade;
import com.opsfactor.community.capability.configuration.facade.ConfiguredViewFacade;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewDTO;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Contratos Community do controller de preferencias e views de usuario.
 *
 * <p>O controller continua exposto apenas para {@code ROLE_ADMIN}, mas deve ler
 * usuario e authorities pelo {@link AuthenticationService}. Assim a validacao
 * de contexto/authorities fica centralizada na seguranca Community, sem acesso
 * direto ao {@code SecurityContextHolder} nos endpoints.</p>
 */
class UserConfigurationControllerCommunityContractTest {

    @Test
    void userConfigurationControllerShouldDeclareExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredField("configuredViewFrontService");
        assertRequiredAutowiredField("configuracaoUsuarioFrontService");
        assertRequiredAutowiredField("authenticationService");

    }

    @Test
    void currentDemandPlanningViewsShouldUseAuthenticatedUserFromAuthenticationService() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguredViewFacade configuredViewFrontService = Mockito.mock(ConfiguredViewFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(configuredViewFrontService.getConfiguredViewDTOListDemandPlanningBook("admin"))
                .thenReturn(List.of(configuredViewDTO));
        setCommonFields(
                userConfigurationController,
                configuredViewFrontService,
                Mockito.mock(ConfiguracaoUsuarioFacade.class),
                authenticationService);

        ResponseEntity<List<ConfiguredViewDTO>> responseEntity =
                userConfigurationController.getConfiguredViewDTOListDemandPlanning();

        Assertions.assertEquals(List.of(configuredViewDTO), responseEntity.getBody());
        Mockito.verify(authenticationService).getAuthenticatedUserId();
        Mockito.verify(configuredViewFrontService).getConfiguredViewDTOListDemandPlanningBook("admin");

    }

    @Test
    void requestedSupplyPlanningViewsShouldUseAuthenticatedUserAndAdminFlagFromAuthenticationService() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguredViewFacade configuredViewFrontService = Mockito.mock(ConfiguredViewFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(authenticationService.currentUserHasAnyRole(List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)))
                .thenReturn(true);
        Mockito.when(configuredViewFrontService.getConfiguredViewDTOListSupplyPlanningBook("admin", "other-user", true))
                .thenReturn(List.of(configuredViewDTO));
        setCommonFields(
                userConfigurationController,
                configuredViewFrontService,
                Mockito.mock(ConfiguracaoUsuarioFacade.class),
                authenticationService);

        ResponseEntity<List<ConfiguredViewDTO>> responseEntity =
                userConfigurationController.getConfiguredViewDTOListSupplyPlanning("other-user");

        Assertions.assertEquals(List.of(configuredViewDTO), responseEntity.getBody());
        Mockito.verify(authenticationService).getAuthenticatedUserId();
        Mockito.verify(authenticationService).currentUserHasAnyRole(
                List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE));
        Mockito.verify(configuredViewFrontService).getConfiguredViewDTOListSupplyPlanningBook(
                "admin",
                "other-user",
                true);

    }

    @Test
    void configuredViewListAliasShouldDelegateDemandPlanningReadUsingAuthenticatedAdmin() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguredViewFacade configuredViewFrontService = Mockito.mock(ConfiguredViewFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        ConfiguredViewDTO configuredViewDTOResult = new ConfiguredViewDTO();
        configuredViewDTO.userId = "other-user";
        configuredViewDTO.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(authenticationService.currentUserHasAnyRole(List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)))
                .thenReturn(true);
        Mockito.when(configuredViewFrontService.getConfiguredViewDTOListDemandPlanningBook("other-user"))
                .thenReturn(List.of(configuredViewDTOResult));
        setCommonFields(
                userConfigurationController,
                configuredViewFrontService,
                Mockito.mock(ConfiguracaoUsuarioFacade.class),
                authenticationService);

        ResponseEntity<List<ConfiguredViewDTO>> responseEntity =
                userConfigurationController.getConfiguredViewDTOList(configuredViewDTO);

        Assertions.assertEquals(List.of(configuredViewDTOResult), responseEntity.getBody());
        Mockito.verify(authenticationService).getAuthenticatedUserId();
        Mockito.verify(authenticationService).currentUserHasAnyRole(
                List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE));
        Mockito.verify(configuredViewFrontService).getConfiguredViewDTOListDemandPlanningBook("other-user");
        Mockito.verifyNoMoreInteractions(configuredViewFrontService);

    }

    @Test
    void configuredViewListAliasShouldDelegateSupplyPlanningReadUsingExistingVisibilityService() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguredViewFacade configuredViewFrontService = Mockito.mock(ConfiguredViewFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        ConfiguredViewDTO configuredViewDTOResult = new ConfiguredViewDTO();
        configuredViewDTO.userId = "other-user";
        configuredViewDTO.viewType = ConfiguredView.TipoView.SUPPLYPLANNINGBOOK;

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(authenticationService.currentUserHasAnyRole(List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)))
                .thenReturn(true);
        Mockito.when(configuredViewFrontService.getConfiguredViewDTOListSupplyPlanningBook(
                        "admin",
                        "other-user",
                        true))
                .thenReturn(List.of(configuredViewDTOResult));
        setCommonFields(
                userConfigurationController,
                configuredViewFrontService,
                Mockito.mock(ConfiguracaoUsuarioFacade.class),
                authenticationService);

        ResponseEntity<List<ConfiguredViewDTO>> responseEntity =
                userConfigurationController.getConfiguredViewDTOList(configuredViewDTO);

        Assertions.assertEquals(List.of(configuredViewDTOResult), responseEntity.getBody());
        Mockito.verify(configuredViewFrontService).getConfiguredViewDTOListSupplyPlanningBook(
                "admin",
                "other-user",
                true);
        Mockito.verifyNoMoreInteractions(configuredViewFrontService);

    }

    @Test
    void configuredViewListAliasShouldKeepDemandPlanningViewsIsolatedWhenAdminAuthorityIsAbsent() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguredViewFacade configuredViewFrontService = Mockito.mock(ConfiguredViewFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = "other-user";
        configuredViewDTO.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(authenticationService.currentUserHasAnyRole(List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)))
                .thenReturn(false);
        setCommonFields(
                userConfigurationController,
                configuredViewFrontService,
                Mockito.mock(ConfiguracaoUsuarioFacade.class),
                authenticationService);

        ResponseEntity<List<ConfiguredViewDTO>> responseEntity =
                userConfigurationController.getConfiguredViewDTOList(configuredViewDTO);

        Assertions.assertEquals(List.of(), responseEntity.getBody());
        Mockito.verifyNoInteractions(configuredViewFrontService);

    }

    @Test
    void configuredViewListAliasShouldRejectMissingLookupKeyBeforeCallingServices() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguredViewFacade configuredViewFrontService = Mockito.mock(ConfiguredViewFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        setCommonFields(
                userConfigurationController,
                configuredViewFrontService,
                Mockito.mock(ConfiguracaoUsuarioFacade.class),
                authenticationService);

        ResponseStatusException missingRequestException = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> userConfigurationController.getConfiguredViewDTOList(null));

        ConfiguredViewDTO configuredViewDTOWithoutViewType = new ConfiguredViewDTO();
        configuredViewDTOWithoutViewType.userId = "admin";
        ResponseStatusException missingViewTypeException = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> userConfigurationController.getConfiguredViewDTOList(configuredViewDTOWithoutViewType));

        ConfiguredViewDTO configuredViewDTOWithoutUserId = new ConfiguredViewDTO();
        configuredViewDTOWithoutUserId.userId = " ";
        configuredViewDTOWithoutUserId.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;
        ResponseStatusException missingUserIdException = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> userConfigurationController.getConfiguredViewDTOList(configuredViewDTOWithoutUserId));

        Assertions.assertEquals(400, missingRequestException.getStatusCode().value());
        Assertions.assertEquals("Configured View request is required.", missingRequestException.getReason());
        Assertions.assertEquals(400, missingViewTypeException.getStatusCode().value());
        Assertions.assertEquals("View type must be informed.", missingViewTypeException.getReason());
        Assertions.assertEquals(400, missingUserIdException.getStatusCode().value());
        Assertions.assertEquals("User must be informed.", missingUserIdException.getReason());
        Mockito.verifyNoInteractions(configuredViewFrontService, authenticationService);

    }

    @Test
    void createConfiguredViewAliasShouldDispatchToTheTypedDemandPlanningService() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguredViewFacade configuredViewFrontService = Mockito.mock(ConfiguredViewFacade.class);
        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = "admin";
        configuredViewDTO.viewName = "Demand default";
        configuredViewDTO.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;
        setCommonFields(
                userConfigurationController,
                configuredViewFrontService,
                Mockito.mock(ConfiguracaoUsuarioFacade.class),
                Mockito.mock(AuthenticationService.class));

        ResponseEntity<String> responseEntity = userConfigurationController.createConfiguredView(configuredViewDTO);

        Assertions.assertEquals("View Successfully Created", responseEntity.getBody());
        Mockito.verify(configuredViewFrontService).createConfiguredViewDTODemandPlanningBook(
                "admin",
                "Demand default");
        Mockito.verifyNoMoreInteractions(configuredViewFrontService);

    }

    @Test
    void createConfiguredViewAliasShouldDispatchToTheTypedSupplyPlanningService() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguredViewFacade configuredViewFrontService = Mockito.mock(ConfiguredViewFacade.class);
        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = "admin";
        configuredViewDTO.viewName = "Supply default";
        configuredViewDTO.viewType = ConfiguredView.TipoView.SUPPLYPLANNINGBOOK;
        setCommonFields(
                userConfigurationController,
                configuredViewFrontService,
                Mockito.mock(ConfiguracaoUsuarioFacade.class),
                Mockito.mock(AuthenticationService.class));

        ResponseEntity<String> responseEntity = userConfigurationController.createConfiguredView(configuredViewDTO);

        Assertions.assertEquals("View Successfully Created", responseEntity.getBody());
        Mockito.verify(configuredViewFrontService).createConfiguredViewSupplyPlanningBook(
                "admin",
                "Supply default");
        Mockito.verifyNoMoreInteractions(configuredViewFrontService);

    }

    @Test
    void createConfiguredViewAliasShouldRejectMissingNameBeforeCallingServices() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguredViewFacade configuredViewFrontService = Mockito.mock(ConfiguredViewFacade.class);
        ConfiguredViewDTO configuredViewDTOWithoutName = new ConfiguredViewDTO();
        configuredViewDTOWithoutName.userId = "admin";
        configuredViewDTOWithoutName.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;
        setCommonFields(
                userConfigurationController,
                configuredViewFrontService,
                Mockito.mock(ConfiguracaoUsuarioFacade.class),
                Mockito.mock(AuthenticationService.class));

        ResponseStatusException missingNameException = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> userConfigurationController.createConfiguredView(configuredViewDTOWithoutName));

        Assertions.assertEquals(400, missingNameException.getStatusCode().value());
        Assertions.assertEquals("View name must be informed.", missingNameException.getReason());
        Mockito.verifyNoInteractions(configuredViewFrontService);

    }

    @Test
    void saveConfiguredViewShouldUseAuthenticatedUserAndAdminFlagFromAuthenticationService() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguredViewFacade configuredViewFrontService = Mockito.mock(ConfiguredViewFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(authenticationService.currentUserHasAnyRole(List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)))
                .thenReturn(true);
        setCommonFields(
                userConfigurationController,
                configuredViewFrontService,
                Mockito.mock(ConfiguracaoUsuarioFacade.class),
                authenticationService);

        ResponseEntity<String> responseEntity =
                userConfigurationController.postConfiguredViewDTOList(configuredViewDTO);

        Assertions.assertEquals("User View Saved", responseEntity.getBody());
        Mockito.verify(configuredViewFrontService).saveConfiguredViewDTO(configuredViewDTO, "admin", true);

    }

    @Test
    void userPreferenceEndpointsShouldUseAuthenticatedUserFromAuthenticationService() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService =
                Mockito.mock(ConfiguracaoUsuarioFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        ConfiguracaoUsuarioDTO configuracaoUsuarioDTO = new ConfiguracaoUsuarioDTO();
        List<ConfiguracaoUsuarioDTO> configuracaoUsuarioDTOList = List.of(configuracaoUsuarioDTO);

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(configuracaoUsuarioFrontService.getConfiguredViewDTOList("admin", "theme"))
                .thenReturn(configuracaoUsuarioDTOList);
        setCommonFields(
                userConfigurationController,
                Mockito.mock(ConfiguredViewFacade.class),
                configuracaoUsuarioFrontService,
                authenticationService);

        ResponseEntity<List<ConfiguracaoUsuarioDTO>> responseEntity =
                userConfigurationController.getConfiguracaoUsuarioDTOList("theme");
        ResponseEntity<String> saveResponseEntity =
                userConfigurationController.postConfiguracaoUsuarioDTOList(configuracaoUsuarioDTOList);

        Assertions.assertEquals(configuracaoUsuarioDTOList, responseEntity.getBody());
        Assertions.assertEquals("User Configurations Saved", saveResponseEntity.getBody());
        Mockito.verify(configuracaoUsuarioFrontService).getConfiguredViewDTOList("admin", "theme");
        Mockito.verify(configuracaoUsuarioFrontService).saveConfigurationViewDTOList(
                "admin",
                configuracaoUsuarioDTOList);

    }

    @Test
    void userInterfacePreferenceEndpointsShouldUseAuthenticatedUserAndReturnTypedDto() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService =
                Mockito.mock(ConfiguracaoUsuarioFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        UserInterfacePreferencesDTO userInterfacePreferencesDTO = new UserInterfacePreferencesDTO();
        userInterfacePreferencesDTO.themeMode = "light";

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(configuracaoUsuarioFrontService.getUserInterfacePreferencesDTO("admin"))
                .thenReturn(userInterfacePreferencesDTO);
        Mockito.when(configuracaoUsuarioFrontService.saveUserInterfacePreferencesDTO(
                        "admin",
                        userInterfacePreferencesDTO))
                .thenReturn(userInterfacePreferencesDTO);
        setCommonFields(
                userConfigurationController,
                Mockito.mock(ConfiguredViewFacade.class),
                configuracaoUsuarioFrontService,
                authenticationService);

        ResponseEntity<UserInterfacePreferencesDTO> getResponseEntity =
                userConfigurationController.getUserInterfacePreferencesDTO();
        ResponseEntity<UserInterfacePreferencesDTO> postResponseEntity =
                userConfigurationController.postUserInterfacePreferencesDTO(userInterfacePreferencesDTO);

        Assertions.assertEquals(userInterfacePreferencesDTO, getResponseEntity.getBody());
        Assertions.assertEquals(userInterfacePreferencesDTO, postResponseEntity.getBody());
        Mockito.verify(configuracaoUsuarioFrontService).getUserInterfacePreferencesDTO("admin");
        Mockito.verify(configuracaoUsuarioFrontService).saveUserInterfacePreferencesDTO(
                "admin",
                userInterfacePreferencesDTO);

    }

    @Test
    void userInterfacePreferenceEndpointsShouldTranslateRuntimeFailureToHttp500() {

        UserConfigurationController userConfigurationController = new UserConfigurationController();
        ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService =
                Mockito.mock(ConfiguracaoUsuarioFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(configuracaoUsuarioFrontService.getUserInterfacePreferencesDTO("admin"))
                .thenThrow(new IllegalArgumentException("invalid theme"));
        setCommonFields(
                userConfigurationController,
                Mockito.mock(ConfiguredViewFacade.class),
                configuracaoUsuarioFrontService,
                authenticationService);

        ResponseStatusException responseStatusException = Assertions.assertThrows(
                ResponseStatusException.class,
                userConfigurationController::getUserInterfacePreferencesDTO);

        Assertions.assertEquals(500, responseStatusException.getStatusCode().value());
        Assertions.assertEquals("invalid theme", responseStatusException.getReason());

    }

    private static void setCommonFields(
            UserConfigurationController userConfigurationController,
            ConfiguredViewFacade configuredViewFrontService,
            ConfiguracaoUsuarioFacade configuracaoUsuarioFrontService,
            AuthenticationService authenticationService) {

        ReflectionTestUtils.setField(
                userConfigurationController,
                "configuredViewFrontService",
                configuredViewFrontService);
        ReflectionTestUtils.setField(
                userConfigurationController,
                "configuracaoUsuarioFrontService",
                configuracaoUsuarioFrontService);
        ReflectionTestUtils.setField(
                userConfigurationController,
                "authenticationService",
                authenticationService);

    }

    private static void assertRequiredAutowiredField(String fieldName) throws Exception {

        Field field = UserConfigurationController.class.getDeclaredField(fieldName);
        Autowired autowired = field.getAnnotation(Autowired.class);

        Assertions.assertNotNull(
                autowired,
                fieldName + " deve manter @Autowired explicito.");
        Assertions.assertTrue(
                autowired.required(),
                fieldName + " deve ser bean obrigatorio.");

    }

}
