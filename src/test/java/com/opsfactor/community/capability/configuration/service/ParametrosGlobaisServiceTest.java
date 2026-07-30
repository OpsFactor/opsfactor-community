package com.opsfactor.community.capability.configuration.service;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.repository.ParametrosGlobaisRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;

/**
 * Contratos do service Community do registro unico de parametros globais.
 */
class ParametrosGlobaisServiceTest {

    @Test
    void serviceShouldKeepExplicitAutowiredRepository() throws Exception {

        Field parametrosGlobaisRepositoryField =
                ParametrosGlobaisService.class.getDeclaredField("parametrosGlobaisRepository");

        Assertions.assertNotNull(parametrosGlobaisRepositoryField.getAnnotation(Autowired.class));

    }

    @Test
    void saveParametrosGlobaisShouldRejectMissingEntityBeforeRepository() {

        ParametrosGlobaisRepositoryStub parametrosGlobaisRepositoryStub =
                new ParametrosGlobaisRepositoryStub();
        ParametrosGlobaisService parametrosGlobaisService =
                createParametrosGlobaisService(parametrosGlobaisRepositoryStub);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosGlobaisService.saveParametrosGlobais(null));

        Assertions.assertEquals(
                "Global parameters are required.",
                illegalArgumentException.getMessage());
        Assertions.assertNull(parametrosGlobaisRepositoryStub.lastCalledMethodName);

    }

    @Test
    void getParametrosGlobaisShouldRejectNullRepositoryOptional() {

        ParametrosGlobaisRepositoryStub parametrosGlobaisRepositoryStub =
                new ParametrosGlobaisRepositoryStub();
        parametrosGlobaisRepositoryStub.returnNullOptional = true;
        ParametrosGlobaisService parametrosGlobaisService =
                createParametrosGlobaisService(parametrosGlobaisRepositoryStub);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                parametrosGlobaisService::getParametrosGlobais);

        Assertions.assertEquals(
                "Global parameters repository returned null Optional.",
                illegalStateException.getMessage());

    }

    @Test
    void saveParametrosGlobaisShouldRejectNullRepositorySaveReturn() {

        ParametrosGlobaisRepositoryStub parametrosGlobaisRepositoryStub =
                new ParametrosGlobaisRepositoryStub();
        parametrosGlobaisRepositoryStub.returnNullOnSave = true;
        ParametrosGlobaisService parametrosGlobaisService =
                createParametrosGlobaisService(parametrosGlobaisRepositoryStub);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> parametrosGlobaisService.saveParametrosGlobais(new ParametrosGlobais()));

        Assertions.assertEquals(
                "Global parameters repository returned null after save.",
                illegalStateException.getMessage());

    }

    private static ParametrosGlobaisService createParametrosGlobaisService(
            ParametrosGlobaisRepositoryStub parametrosGlobaisRepositoryStub) {

        ParametrosGlobaisService parametrosGlobaisService =
                new ParametrosGlobaisService();
        setField(
                parametrosGlobaisService,
                "parametrosGlobaisRepository",
                parametrosGlobaisRepositoryStub.getRepository());
        return parametrosGlobaisService;

    }

    private static void setField(
            ParametrosGlobaisService parametrosGlobaisService,
            String fieldName,
            Object value) {

        try {
            Field field = ParametrosGlobaisService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(parametrosGlobaisService, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nao foi possivel injetar stub no teste", e);
        }

    }

    private static class ParametrosGlobaisRepositoryStub {

        private String lastCalledMethodName;
        private boolean returnNullOptional;
        private boolean returnNullOnSave;

        private ParametrosGlobaisRepository getRepository() {

            return (ParametrosGlobaisRepository) Proxy.newProxyInstance(
                    ParametrosGlobaisRepository.class.getClassLoader(),
                    new Class[]{ParametrosGlobaisRepository.class},
                    this::invoke);

        }

        private Object invoke(Object proxy, Method method, Object[] args) {

            lastCalledMethodName = method.getName();
            return switch (method.getName()) {
                case "customFindComDependencias" -> returnNullOptional
                        ? null
                        : Optional.empty();
                case "save" -> returnNullOnSave
                        ? null
                        : args[0];
                case "toString" -> "ParametrosGlobaisRepositoryStub";
                case "hashCode" -> System.identityHashCode(this);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(
                        "Metodo nao suportado no stub: " + method.getName());
            };

        }

    }

}
