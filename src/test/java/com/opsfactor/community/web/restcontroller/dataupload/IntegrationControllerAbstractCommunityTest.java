package com.opsfactor.community.web.restcontroller.dataupload;

import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationDataDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationFiltroDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.platform.integration.service.IntegrationLoggingContext;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.web.configuration.UserRoleType;
import com.opsfactor.community.web.restcontroller.dataupload.transactionaldata.EstoqueIntegrationController;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Garante que a camada generica de data upload Community nao aceite execucao
 * assíncrona. O campo permanece no DTO compartilhado para o Enterprise, mas
 * deve falhar antes de qualquer service quando chegar preenchido como async.
 */
public class IntegrationControllerAbstractCommunityTest {

    @Test
    public void integrationControllerAbstractShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredField("handlerMapping");
        assertRequiredAutowiredField("webControllerTaskSchedulingService");
        assertRequiredAutowiredField("authenticationService");
        assertRequiredAutowiredField("integrationService");
        assertRequiredAutowiredField("httpServletRequest");
        assertRequiredAutowiredField("objectMapper");

        Field handlerMappingField = IntegrationControllerAbstract.class.getDeclaredField("handlerMapping");
        Qualifier qualifier = handlerMappingField.getAnnotation(Qualifier.class);
        Assertions.assertNotNull(qualifier);
        Assertions.assertEquals("requestMappingHandlerMapping", qualifier.value());

    }

    @Test
    public void requestContentLoggingHelpersShouldDeclareNullableContracts() throws Exception {

        /*
         * Quando o log de conteudo esta desligado, a camada de integracao nao
         * deve serializar payload JSON nem copiar multipart para memoria apenas
         * para gerar texto. Null e o contrato explicito desse no-op.
         */
        assertNullableMethod(
                "getJsonRequestContent",
                Object.class);
        assertNullableMethod(
                "getMultipartRequestContent",
                MultipartFile.class);
        assertNullableMethod(
                "limitRequestContent",
                String.class);

    }

    @Test
    public void validaThreadSyncCommunityShouldAcceptMissingThreadSync() throws Exception {

        EstoqueIntegrationController estoqueIntegrationController = new EstoqueIntegrationController();
        IntegrationDto<EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, EstoqueIntegrationFiltroDto, IntegrationOptionsDto> integrationDto =
                new IntegrationDto<>();

        invokeValidation(estoqueIntegrationController, integrationDto);

    }

    @Test
    public void validaThreadSyncCommunityShouldAcceptExplicitSync() throws Exception {

        EstoqueIntegrationController estoqueIntegrationController = new EstoqueIntegrationController();
        IntegrationDto<EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, EstoqueIntegrationFiltroDto, IntegrationOptionsDto> integrationDto =
                new IntegrationDto<>();
        integrationDto.threadSync = IntegrationDto.ThreadSync.SYNC;

        invokeValidation(estoqueIntegrationController, integrationDto);

    }

    @Test
    public void validaThreadSyncCommunityShouldRejectMissingPayload() {

        EstoqueIntegrationController estoqueIntegrationController = new EstoqueIntegrationController();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidation(estoqueIntegrationController, null));
        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Data integration payload is required.",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaThreadSyncCommunityShouldRejectAsync() throws Exception {

        EstoqueIntegrationController estoqueIntegrationController = new EstoqueIntegrationController();
        IntegrationDto<EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, EstoqueIntegrationFiltroDto, IntegrationOptionsDto> integrationDto =
                new IntegrationDto<>();
        integrationDto.threadSync = IntegrationDto.ThreadSync.ASYNC;

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidation(estoqueIntegrationController, integrationDto));
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    @Test
    public void getFileShouldRejectUnauthorizedUserCommunity() throws Exception {

        EstoqueIntegrationController estoqueIntegrationController = new EstoqueIntegrationController();
        setAuthenticationService(
                estoqueIntegrationController,
                new AuthenticationServiceDenyAllStub());

        ResponseStatusException responseStatusException = Assertions.assertThrows(
                ResponseStatusException.class,
                estoqueIntegrationController::getFile);
        Assertions.assertEquals(
                HttpStatus.UNAUTHORIZED,
                responseStatusException.getStatusCode());

    }

    @Test
    public void mutableJsonEndpointsShouldUsePostRolesInsteadOfGetRoles() throws Exception {

        RoleDifferentiatingEstoqueIntegrationController estoqueIntegrationController =
                new RoleDifferentiatingEstoqueIntegrationController();
        AuthenticationServiceAllowAdminStub authenticationServiceAllowAdminStub =
                new AuthenticationServiceAllowAdminStub();
        setAuthenticationService(
                estoqueIntegrationController,
                authenticationServiceAllowAdminStub);

        IntegrationDto<EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, EstoqueIntegrationFiltroDto, IntegrationOptionsDto> integrationDto =
                new IntegrationDto<>();
        integrationDto.threadSync = IntegrationDto.ThreadSync.ASYNC;

        /*
         * A subclasse de teste zera os papeis de GET e mantem ROLE_ADMIN
         * apenas em POST. Se os endpoints mutaveis voltarem a chamar
         * getUserRoleTypesGet(), a resposta sera 401 e o teste nao chegara ao
         * gate Community de execucao assincrona.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> estoqueIntegrationController.saveIntegrationDto(integrationDto));
        Assertions.assertEquals(
                List.of(UserRoleType.ROLE_ADMIN.name()),
                authenticationServiceAllowAdminStub.lastRequestedRoles);

        authenticationServiceAllowAdminStub.lastRequestedRoles = null;

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> estoqueIntegrationController.deleteDtoOuFiltro(integrationDto));
        Assertions.assertEquals(
                List.of(UserRoleType.ROLE_ADMIN.name()),
                authenticationServiceAllowAdminStub.lastRequestedRoles);

    }

    @Test
    public void dataIntegrationRoleListsShouldFailClearlyWhenSubclassReturnsInvalidRoles() {

        NullPostRolesEstoqueIntegrationController nullPostRolesEstoqueIntegrationController =
                new NullPostRolesEstoqueIntegrationController();
        IntegrationDto<EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, EstoqueIntegrationFiltroDto, IntegrationOptionsDto> integrationDto =
                new IntegrationDto<>();

        IllegalStateException nullPostRolesException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> nullPostRolesEstoqueIntegrationController.saveIntegrationDto(integrationDto));
        Assertions.assertEquals(
                "Data integration POST role list is required.",
                nullPostRolesException.getMessage());

        NullItemGetRolesEstoqueIntegrationController nullItemGetRolesEstoqueIntegrationController =
                new NullItemGetRolesEstoqueIntegrationController();

        IllegalStateException nullGetRoleException = Assertions.assertThrows(
                IllegalStateException.class,
                nullItemGetRolesEstoqueIntegrationController::getFile);
        Assertions.assertEquals(
                "Data integration GET role at index 0 is required.",
                nullGetRoleException.getMessage());

    }

    @Test
    public void getMethodShouldFailWithControllerAndMethodWhenMappingMethodIsMissing() throws Exception {

        EstoqueIntegrationController estoqueIntegrationController = new EstoqueIntegrationController();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetMethod(
                        estoqueIntegrationController,
                        "missingDataIntegrationMappingMethod"));

        Assertions.assertInstanceOf(
                IllegalStateException.class,
                invocationTargetException.getCause());
        Assertions.assertTrue(
                invocationTargetException.getCause().getMessage().contains("missingDataIntegrationMappingMethod"));
        Assertions.assertTrue(
                invocationTargetException.getCause().getMessage().contains(EstoqueIntegrationController.class.getName()));

    }

    @Test
    public void getEntityClassNameShouldResolveConcreteEntityFromControllerGenericParameter() {

        EstoqueIntegrationController estoqueIntegrationController = new EstoqueIntegrationController();

        Assertions.assertEquals(
                Estoque.class.getSimpleName(),
                estoqueIntegrationController.getEntityClassName());

    }

    @Test
    public void loggedSupplierShouldMarkErrorAsFailureBeforeRethrowing() throws Exception {

        EstoqueIntegrationController estoqueIntegrationController = new EstoqueIntegrationController();
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        AtomicReference<IntegrationLoggingContext> integrationLoggingContextAtomicReference =
                new AtomicReference<>();
        Supplier<String> supplierExecucaoIntegracao = () -> {
            integrationLoggingContextAtomicReference.set(IntegrationLoggingContext.getCurrent());
            throw new AssertionError("boom");
        };

        Mockito.when(httpServletRequest.getRequestURI())
                .thenReturn("/api/secured/data/stock");
        Mockito.when(httpServletRequest.getMethod())
                .thenReturn("POST");
        setField(
                estoqueIntegrationController,
                "httpServletRequest",
                httpServletRequest);
        setField(
                estoqueIntegrationController,
                "dataIntegrationLifecycleLoggingEnabled",
                true);

        Supplier<String> loggedSupplier = invokeBuildLoggedSupplier(
                estoqueIntegrationController,
                supplierExecucaoIntegracao);

        AssertionError assertionError = Assertions.assertThrows(
                AssertionError.class,
                loggedSupplier::get);

        Assertions.assertEquals("boom", assertionError.getMessage());
        Assertions.assertNotNull(integrationLoggingContextAtomicReference.get());
        Assertions.assertEquals(
                "FAILED",
                integrationLoggingContextAtomicReference.get().getStatus());
        Assertions.assertEquals(
                "boom",
                integrationLoggingContextAtomicReference.get().getFailureMessage());
        Assertions.assertNull(
                IntegrationLoggingContext.getCurrent(),
                "ThreadLocal de logging deve ser limpo mesmo quando a integracao falha com Error.");

    }

    private static void invokeValidation(
            EstoqueIntegrationController estoqueIntegrationController,
            IntegrationDto<EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, EstoqueIntegrationFiltroDto, IntegrationOptionsDto> integrationDto) throws Exception {

        Method validationMethod = IntegrationControllerAbstract.class.getDeclaredMethod(
                "validaThreadSyncCommunity",
                IntegrationDto.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                estoqueIntegrationController,
                integrationDto);

    }

    private static void invokeGetMethod(
            EstoqueIntegrationController estoqueIntegrationController,
            String methodName) throws Exception {

        Method getMethodMethod = IntegrationControllerAbstract.class.getDeclaredMethod(
                "getMethod",
                String.class,
                Class[].class);
        getMethodMethod.setAccessible(true);
        getMethodMethod.invoke(
                estoqueIntegrationController,
                methodName,
                new Class<?>[0]);

    }

    private static void setAuthenticationService(
            EstoqueIntegrationController estoqueIntegrationController,
            AuthenticationService authenticationService) throws Exception {

        setField(
                estoqueIntegrationController,
                "authenticationService",
                authenticationService);

    }

    private static void setField(
            EstoqueIntegrationController estoqueIntegrationController,
            String fieldName,
            Object value) throws Exception {

        Field field = IntegrationControllerAbstract.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(estoqueIntegrationController, value);

    }

    @SuppressWarnings("unchecked")
    private static Supplier<String> invokeBuildLoggedSupplier(
            EstoqueIntegrationController estoqueIntegrationController,
            Supplier<String> supplierExecucaoIntegracao) throws Exception {

        Method buildLoggedSupplierMethod = IntegrationControllerAbstract.class.getDeclaredMethod(
                "buildLoggedSupplier",
                Supplier.class,
                String.class,
                Integer.class,
                String.class);
        buildLoggedSupplierMethod.setAccessible(true);

        return (Supplier<String>) buildLoggedSupplierMethod.invoke(
                estoqueIntegrationController,
                supplierExecucaoIntegracao,
                "saveIntegrationDto",
                1,
                null);

    }

    private static void assertRequiredAutowiredField(String fieldName) throws Exception {

        Field field = IntegrationControllerAbstract.class.getDeclaredField(fieldName);
        Autowired autowired = field.getAnnotation(Autowired.class);

        Assertions.assertNotNull(
                autowired,
                "IntegrationControllerAbstract." + fieldName + " deve usar @Autowired explicito");
        Assertions.assertTrue(
                autowired.required(),
                "IntegrationControllerAbstract." + fieldName + " deve ser bean obrigatorio");

    }

    private static void assertNullableMethod(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {

        Method method = IntegrationControllerAbstract.class.getDeclaredMethod(methodName, parameterTypes);
        Assertions.assertTrue(
                method.isAnnotationPresent(Nullable.class),
                methodName + " deve declarar @Nullable porque null e contrato esperado de logging no-op.");

    }

    private static class AuthenticationServiceDenyAllStub extends AuthenticationService {

        @Override
        public boolean currentUserHasAnyRole(Collection<String> userRoles) {

            return false;

        }

    }

    private static class AuthenticationServiceAllowAdminStub extends AuthenticationService {

        private List<String> lastRequestedRoles;

        @Override
        public boolean currentUserHasAnyRole(Collection<String> userRoles) {

            lastRequestedRoles = List.copyOf(userRoles);
            return userRoles.contains(UserRoleType.ROLE_ADMIN.name());

        }

    }

    private static class RoleDifferentiatingEstoqueIntegrationController extends EstoqueIntegrationController {

        @Override
        protected List<UserRoleType> getUserRoleTypesGet() {

            return List.of();

        }

        @Override
        protected List<UserRoleType> getUserRoleTypesPost() {

            return List.of(UserRoleType.ROLE_ADMIN);

        }

    }

    private static class NullPostRolesEstoqueIntegrationController extends EstoqueIntegrationController {

        @Override
        protected List<UserRoleType> getUserRoleTypesPost() {

            return null;

        }

    }

    private static class NullItemGetRolesEstoqueIntegrationController extends EstoqueIntegrationController {

        @Override
        protected List<UserRoleType> getUserRoleTypesGet() {

            return java.util.Arrays.asList((UserRoleType) null);

        }

    }

}
