package com.opsfactor.community.platform.projection.inmemorybi.applied.annotation;

import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * Contrato dos helpers refletivos usados pelo BI anotado para compartilhar
 * atributos entre classes irmas de projection.
 */
class BIProjectionAnotacoesNullableContractTest {

    @Test
    void reflectiveHelpersShouldDeclareNullableMissContracts() throws ReflectiveOperationException {

        assertMethodNullable("getIdObjetoSeDisponivel", Object.class);
        assertMethodNullable("getFieldValue", Object.class, java.lang.reflect.Field.class);
        assertMethodNullable("getFieldValueByName", Object.class, String.class);
        assertMethodNullable("getMethodValue", Object.class, Method.class);
        assertMethodNullable("getMethodValueByName", Object.class, String.class);
        assertMethodNullable("getCachedField", Class.class, String.class);
        assertMethodNullable("getCachedMethod", Class.class, String.class);

    }

    @Test
    void reflectiveCacheMissesShouldRemainNullContracts() throws ReflectiveOperationException {

        BIProjectionAnotacoes<ProjectionWithoutIdOrMembers> biProjectionAnotacoes =
                new BIProjectionAnotacoes<>(ProjectionWithoutIdOrMembers.class);
        ProjectionWithoutIdOrMembers projectionWithoutIdOrMembers =
                new ProjectionWithoutIdOrMembers();

        Assertions.assertNull(invokePrivateMethod(
                biProjectionAnotacoes,
                "getIdObjetoSeDisponivel",
                new Class<?>[] {Object.class},
                projectionWithoutIdOrMembers));
        Assertions.assertNull(invokePrivateMethod(
                biProjectionAnotacoes,
                "getFieldValueByName",
                new Class<?>[] {Object.class, String.class},
                projectionWithoutIdOrMembers,
                "missingField"));
        Assertions.assertNull(invokePrivateMethod(
                biProjectionAnotacoes,
                "getMethodValueByName",
                new Class<?>[] {Object.class, String.class},
                projectionWithoutIdOrMembers,
                "getMissingValue"));
        Assertions.assertNull(invokePrivateMethod(
                biProjectionAnotacoes,
                "getCachedField",
                new Class<?>[] {Class.class, String.class},
                ProjectionWithoutIdOrMembers.class,
                "missingField"));
        Assertions.assertNull(invokePrivateMethod(
                biProjectionAnotacoes,
                "getCachedMethod",
                new Class<?>[] {Class.class, String.class},
                ProjectionWithoutIdOrMembers.class,
                "getMissingValue"));

    }

    private static void assertMethodNullable(
            String methodName,
            Class<?>... parameterTypes) throws ReflectiveOperationException {

        Method method = BIProjectionAnotacoes.class.getDeclaredMethod(methodName, parameterTypes);

        Assertions.assertTrue(
                method.isAnnotationPresent(Nullable.class),
                methodName + " deve declarar retorno @Nullable para cache miss ou valor refletivo ausente.");

    }

    private static Object invokePrivateMethod(
            BIProjectionAnotacoes<?> biProjectionAnotacoes,
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments) throws ReflectiveOperationException {

        Method method = BIProjectionAnotacoes.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        return method.invoke(biProjectionAnotacoes, arguments);

    }

    private static class ProjectionWithoutIdOrMembers {
    }

}
