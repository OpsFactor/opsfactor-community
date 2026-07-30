package com.opsfactor.community.web.restcontroller.admin;

import com.opsfactor.community.platform.cache.CachingService;
import com.opsfactor.community.capability.configuration.facade.ApplicationAppearanceFacade;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import com.opsfactor.community.platform.security.login.facade.dto.UserDTO;
import com.opsfactor.community.platform.security.login.facade.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova HTTP do contrato administrativo Community.
 *
 * <p>O teste cobre a desserializacao real do JSON de usuario antes de ele
 * chegar ao service. Isso impede que uma mudanca de Lombok/Jackson transforme
 * uma tela administrativa aparentemente tipada em erro de request.</p>
 */
class AdminRestControllerHttpTest {

    private MockMvc mockMvc;

    private UserFacade userFrontService;

    @BeforeEach
    void setUp() {

        AdminRestController adminRestController = new AdminRestController();
        userFrontService = mock(UserFacade.class);
        ReflectionTestUtils.setField(adminRestController, "userFrontService", userFrontService);
        ReflectionTestUtils.setField(adminRestController, "cachingService", mock(CachingService.class));
        ReflectionTestUtils.setField(
                adminRestController,
                "applicationAppearanceFrontService",
                mock(ApplicationAppearanceFacade.class));
        mockMvc = MockMvcBuilders.standaloneSetup(adminRestController).build();

    }

    @Test
    void saveUserDTOShouldDeserializeCanonicalJsonAndDelegateToFrontService() throws Exception {

        String userPayload = """
                {
                  "id": "community-admin",
                  "firstName": "Community",
                  "lastName": "Administrator",
                  "email": "admin@example.invalid",
                  "active": true,
                  "password": null,
                  "userRoles": ["ROLE_ADMIN"]
                }
                """;

        mockMvc.perform(post("/api/secured/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("User data saved"));

        ArgumentCaptor<UserDTO> userDTOCaptor = ArgumentCaptor.forClass(UserDTO.class);
        verify(userFrontService).saveUserDTO(userDTOCaptor.capture());

        UserDTO userDTO = userDTOCaptor.getValue();
        assertEquals("community-admin", userDTO.id);
        assertEquals("Community", userDTO.firstName);
        assertEquals("Administrator", userDTO.lastName);
        assertEquals("admin@example.invalid", userDTO.email);
        assertEquals(true, userDTO.active);
        assertNull(userDTO.password);
        assertEquals(Set.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE), userDTO.userRoles);

    }

}
