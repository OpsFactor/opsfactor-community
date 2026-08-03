package com.opsfactor.community.web.restcontroller.admin;

import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import com.opsfactor.community.platform.security.login.facade.dto.UserDTO;
import com.opsfactor.community.platform.security.login.facade.UserFacade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class AdminRestControllerCommunityContractTest {

    private static final List<ControllerEndpoint> COMMUNITY_ADMIN_ENDPOINTS = List.of(
            new ControllerEndpoint("GET", "api/secured/clearallcaches"),
            new ControllerEndpoint("GET", "api/secured/usedmemory"),
            new ControllerEndpoint("GET", "api/secured/user"),
            new ControllerEndpoint("GET", "api/secured/user/configuredview/{configuredViewType}"),
            new ControllerEndpoint("GET", "api/secured/user/rolelist"),
            new ControllerEndpoint("POST", "api/open/createdefaultuser"),
            new ControllerEndpoint("POST", "api/secured/user"));

    @Test
    public void getUserRoleTypeListShouldExposeOnlyCommunityAdminRole() {

        AdminRestController adminRestController = new AdminRestController();

        Assertions.assertEquals(
                List.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE),
                adminRestController.getUserRoleTypeList());

    }

    @Test
    public void adminControllerShouldExposeOnlyCommunityEndpoints() {

        List<ControllerEndpoint> controllerEndpointList = Arrays
                .stream(AdminRestController.class.getDeclaredMethods())
                .flatMap(this::getControllerEndpoints)
                .sorted(Comparator.comparing(ControllerEndpoint::httpMethod).thenComparing(ControllerEndpoint::path))
                .toList();

        Assertions.assertEquals(
                COMMUNITY_ADMIN_ENDPOINTS,
                controllerEndpointList,
                "Admin Community deve expor apenas usuarios simples, bootstrap inicial e utilitarios tecnicos.");

    }

    @Test
    public void adminControllerSecuredEndpointsShouldStayAdminOnly() {

        Arrays
                .stream(AdminRestController.class.getDeclaredMethods())
                .filter(method -> getControllerEndpoints(method).anyMatch(endpoint -> endpoint.path().startsWith("api/secured/")))
                .forEach(method -> {
                    Secured secured = method.getAnnotation(Secured.class);
                    Assertions.assertNotNull(
                            secured,
                            method.getName() + " deve declarar @Secured(\"ROLE_ADMIN\").");
                    Assertions.assertArrayEquals(
                            new String[]{CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE},
                            secured.value(),
                            method.getName() + " deve ficar restrito ao admin Community.");
                });

    }

    @Test
    public void adminControllerShouldNotExposeApplicationPropertiesPrintingEndpoint() {

        boolean printApplicationPropertiesEndpointExists = Arrays.stream(AdminRestController.class.getDeclaredMethods())
                .flatMap(this::getEndpointPaths)
                .anyMatch(path -> path.toLowerCase().contains("printapplicationproperties"));

        /*
         * Community usa seguranca simples e nao deve oferecer endpoint que
         * despeje propriedades da aplicacao no log. Mesmo protegido por admin,
         * esse tipo de endpoint pode expor credenciais, URLs internas ou
         * detalhes de infraestrutura no repositorio Community.
         */
        Assertions.assertFalse(printApplicationPropertiesEndpointExists);

    }

    @Test
    public void adminControllerShouldNotExposeUnlockOrLockoutEndpoints() {

        List<String> endpointPaths = Arrays.stream(AdminRestController.class.getDeclaredMethods())
                .flatMap(this::getEndpointPaths)
                .map(String::toLowerCase)
                .toList();

        /*
         * Community nao possui lockout por IP nem rotina de desbloqueio. Essas
         * capacidades ficam no Enterprise junto com politicas de seguranca mais
         * ricas; portanto nenhum endpoint administrativo Community deve
         * publicar unlock, blocklist ou blocked IP.
         */
        Assertions.assertTrue(endpointPaths.stream().noneMatch(path -> path.contains("unlock")));
        Assertions.assertTrue(endpointPaths.stream().noneMatch(path -> path.contains("lockout")));
        Assertions.assertTrue(endpointPaths.stream().noneMatch(path -> path.contains("blocked")));
        Assertions.assertTrue(endpointPaths.stream().noneMatch(path -> path.contains("blocklist")));

    }

    @Test
    public void adminControllerSpringBeansShouldKeepExplicitAutowiredFields() throws NoSuchFieldException {

        /*
         * Campos que sao beans Spring devem continuar visualmente marcados com
         * @Autowired no controller. Isso evita confundir dependencias de
         * runtime com atributos simples como defaultUser/defaultUserPassword.
         */
        assertFieldHasAutowired("userFrontService");
        assertFieldHasAutowired("cachingService");

    }

    @Test
    public void adminControllerShouldNotExposeEnterpriseAppearanceEndpoints() {

        List<String> endpointPaths = Arrays.stream(AdminRestController.class.getDeclaredMethods())
                .flatMap(this::getEndpointPaths)
                .toList();

        /*
         * Community tem marca e tema fixos. Logo administrativo e preferências
         * visuais por usuário pertencem ao overlay Enterprise e não podem
         * ressurgir neste controller por acidente.
         */
        Assertions.assertTrue(endpointPaths.stream().noneMatch(path -> path.contains("applicationappearance")));

    }

    @Test
    public void createDefaultUserShouldFailClearlyWhenExternalBootstrapPropertiesAreMissing() {

        AdminRestController adminRestController = new AdminRestController();

        ResponseEntity<String> responseEntity = adminRestController.createDefaultUser();

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        Assertions.assertEquals(
                "Default user creation not allowed: configure defaultuser and defaultuser.password externally before calling the bootstrap endpoint",
                responseEntity.getBody());

    }

    @Test
    public void createDefaultUserShouldFailClearlyWhenUsersAlreadyExist() {

        AdminRestController adminRestController = new AdminRestController();
        UserFacade userFrontService = Mockito.mock(UserFacade.class);

        Mockito.when(userFrontService.getUserDTOList())
                .thenReturn(List.of(UserDTO.builder().id("admin").build()));
        ReflectionTestUtils.setField(adminRestController, "defaultUser", "admin");
        ReflectionTestUtils.setField(adminRestController, "defaultUserPassword", "admin");
        ReflectionTestUtils.setField(adminRestController, "userFrontService", userFrontService);

        ResponseEntity<String> responseEntity = adminRestController.createDefaultUser();

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        Assertions.assertEquals(
                "Users have already been created; use the secured Community user administration endpoint instead",
                responseEntity.getBody());
        Mockito.verify(userFrontService).getUserDTOList();
        Mockito.verifyNoMoreInteractions(userFrontService);

    }

    @Test
    public void saveUserHttpJsonShouldDeserializeExplicitNullPasswordAndFixedCommunityRole() throws Exception {

        AdminRestController adminRestController = new AdminRestController();
        UserFacade userFrontService = Mockito.mock(UserFacade.class);
        ReflectionTestUtils.setField(adminRestController, "userFrontService", userFrontService);
        MockMvc mockMvc = standaloneSetup(adminRestController).build();

        /*
         * Este teste atravessa Jackson e o argumento @RequestBody real. A SPA
         * usa password:null para editar dados/lifecycle sem trocar o hash;
         * testar somente UserFrontService em memoria nao provaria esse
         * contrato HTTP.
         */
        mockMvc.perform(post("/api/secured/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "community-admin",
                                  "firstName": "Community",
                                  "lastName": "Administrator",
                                  "email": "administrator@example.invalid",
                                  "active": false,
                                  "password": null,
                                  "userRoles": ["ROLE_ADMIN"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("User data saved"));

        org.mockito.ArgumentCaptor<UserDTO> userDTOCaptor = org.mockito.ArgumentCaptor.forClass(UserDTO.class);
        Mockito.verify(userFrontService).saveUserDTO(userDTOCaptor.capture());
        UserDTO receivedUserDTO = userDTOCaptor.getValue();
        Assertions.assertEquals("community-admin", receivedUserDTO.id);
        Assertions.assertFalse(receivedUserDTO.active);
        Assertions.assertNull(receivedUserDTO.password);
        Assertions.assertEquals(Set.of(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE), receivedUserDTO.userRoles);

    }

    @Test
    public void devSeedShouldNotCreateTenantOrTenantBoundUser() throws IOException {

        InputStream dataSqlInputStream = getClass().getClassLoader().getResourceAsStream("data.sql");
        Assertions.assertNotNull(dataSqlInputStream, "data.sql resource should exist for Community dev seed test");

        String dataSql = new String(
                dataSqlInputStream.readAllBytes(),
                StandardCharsets.UTF_8);

        /*
         * Community possui login simples e nao carrega a modelagem multi-tenant
         * do legado/Enterprise. O seed de desenvolvimento precisa seguir a
         * mesma regra, caso contrario o bootstrap local falha quando o schema
         * e criado apenas a partir das entidades Community.
         */
        Assertions.assertFalse(dataSql.toLowerCase().contains("insert into tenant"));
        Assertions.assertFalse(dataSql.toLowerCase().contains("tenant_id"));

    }

    private Stream<String> getEndpointPaths(Method method) {

        return Stream.of(
                        getEndpointPaths(method, GetMapping.class),
                        getEndpointPaths(method, PostMapping.class),
                        getEndpointPaths(method, PutMapping.class),
                        getEndpointPaths(method, DeleteMapping.class),
                        getEndpointPaths(method, RequestMapping.class))
                .flatMap(paths -> paths);

    }

    private static void assertFieldHasAutowired(String fieldName) throws NoSuchFieldException {

        Field field = AdminRestController.class.getDeclaredField(fieldName);
        Autowired autowired = field.getAnnotation(Autowired.class);

        Assertions.assertNotNull(
                autowired,
                fieldName + " deve manter @Autowired explicito.");
        Assertions.assertTrue(
                autowired.required(),
                fieldName + " deve ser bean obrigatorio.");

    }

    private Stream<ControllerEndpoint> getControllerEndpoints(Method method) {

        return Stream.of(
                        getDirectEndpointPaths(method, GetMapping.class).map(path -> new ControllerEndpoint("GET", path)),
                        getDirectEndpointPaths(method, PostMapping.class).map(path -> new ControllerEndpoint("POST", path)),
                        getDirectEndpointPaths(method, PutMapping.class).map(path -> new ControllerEndpoint("PUT", path)),
                        getDirectEndpointPaths(method, DeleteMapping.class).map(path -> new ControllerEndpoint("DELETE", path)),
                        getDirectEndpointPaths(method, PatchMapping.class).map(path -> new ControllerEndpoint("PATCH", path)),
                        getDirectEndpointPaths(method, RequestMapping.class).map(path -> new ControllerEndpoint("REQUEST", path)))
                .flatMap(controllerEndpointStream -> controllerEndpointStream);

    }

    private <T extends Annotation> Stream<String> getDirectEndpointPaths(
            Method method,
            Class<T> annotationClass) {

        T annotation = method.getAnnotation(annotationClass);
        if (annotation == null) return Stream.empty();

        try {
            String[] valueArray = (String[]) annotationClass.getMethod("value").invoke(annotation);
            String[] pathArray = (String[]) annotationClass.getMethod("path").invoke(annotation);
            return Stream.concat(Arrays.stream(valueArray), Arrays.stream(pathArray)).distinct();
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(
                    "Nao foi possivel ler paths de " + annotationClass.getSimpleName(),
                    reflectiveOperationException);
        }

    }

    private <T extends Annotation> Stream<String> getEndpointPaths(
            Method method,
            Class<T> annotationClass) {

        MergedAnnotation<T> mergedAnnotation = MergedAnnotations.from(method).get(annotationClass);
        if (!mergedAnnotation.isPresent()) return Stream.empty();

        return Stream.concat(
                Arrays.stream(mergedAnnotation.getStringArray("value")),
                Arrays.stream(mergedAnnotation.getStringArray("path")));

    }

    private record ControllerEndpoint(String httpMethod, String path) {

    }

}
