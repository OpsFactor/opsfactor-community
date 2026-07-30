package com.opsfactor.community.platform.exception;

import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * Contrato Community da excecao que anexa o DTO original a falhas de Data
 * Upload. O DTO e seu conteudo formatado sao opcionais porque algumas falhas
 * sao criadas apenas a partir da mensagem funcional.
 */
class MissingDependencyDataUploadExceptionCommunityTest {

    @Test
    void exceptionWithoutDtoShouldKeepNullableDiagnosticFieldsEmpty() {

        MissingDependencyDataUploadException missingDependencyDataUploadException =
                new MissingDependencyDataUploadException("Material MAT-001 not found");

        Assertions.assertEquals(
                "Material MAT-001 not found",
                missingDependencyDataUploadException.getMessage());
        Assertions.assertNull(missingDependencyDataUploadException.getDto());
        Assertions.assertNull(missingDependencyDataUploadException.getDtoContent());

    }

    @Test
    void exceptionWithDtoShouldAppendFormattedDtoToPublicMessage() {

        MissingDependencySampleDto missingDependencySampleDto =
                new MissingDependencySampleDto();

        MissingDependencyDataUploadException missingDependencyDataUploadException =
                new MissingDependencyDataUploadException(
                        "Material MAT-001 not found",
                        missingDependencySampleDto);

        Assertions.assertSame(
                missingDependencySampleDto,
                missingDependencyDataUploadException.getDto());
        Assertions.assertNotNull(missingDependencyDataUploadException.getDtoContent());
        Assertions.assertTrue(
                missingDependencyDataUploadException.getMessage().startsWith(
                        "Material MAT-001 not found | DTO: MissingDependencySampleDto{"));
        Assertions.assertTrue(
                missingDependencyDataUploadException.getMessage().contains("materialId=MAT-001"));
        Assertions.assertTrue(
                missingDependencyDataUploadException.getMessage().contains("quantity=3"));

    }

    @Test
    void nullableDiagnosticContractShouldBeDeclaredExplicitly() throws ReflectiveOperationException {

        Field dtoField = MissingDependencyDataUploadException.class.getDeclaredField("dto");
        Field dtoContentField = MissingDependencyDataUploadException.class.getDeclaredField("dtoContent");
        Constructor<MissingDependencyDataUploadException> constructor =
                MissingDependencyDataUploadException.class.getConstructor(String.class, Object.class);
        Method getDtoMethod = MissingDependencyDataUploadException.class.getDeclaredMethod("getDto");
        Method getDtoContentMethod = MissingDependencyDataUploadException.class.getDeclaredMethod("getDtoContent");
        Method formatNullableObjectMethod =
                MissingDependencyDataUploadException.class.getDeclaredMethod("formatObject", Object.class);
        Method formatNestedObjectMethod =
                MissingDependencyDataUploadException.class.getDeclaredMethod("formatObject", Object.class, Set.class);

        Assertions.assertTrue(
                dtoField.isAnnotationPresent(Nullable.class),
                "dto deve declarar @Nullable porque a excecao pode ser criada sem payload.");
        Assertions.assertTrue(
                dtoContentField.isAnnotationPresent(Nullable.class),
                "dtoContent deve declarar @Nullable porque ausencia de payload nao gera texto.");
        assertParameterNullable(
                constructor,
                1,
                "O construtor deve declarar DTO @Nullable.");
        Assertions.assertTrue(
                getDtoMethod.isAnnotationPresent(Nullable.class),
                "getDto deve declarar retorno @Nullable.");
        Assertions.assertTrue(
                getDtoContentMethod.isAnnotationPresent(Nullable.class),
                "getDtoContent deve declarar retorno @Nullable.");
        Assertions.assertTrue(
                formatNullableObjectMethod.isAnnotationPresent(Nullable.class),
                "formatObject(Object) deve declarar retorno @Nullable para preservar mensagem sem DTO.");
        assertParameterNullable(
                formatNullableObjectMethod,
                0,
                "formatObject(Object) deve declarar entrada @Nullable.");
        assertParameterNullable(
                formatNestedObjectMethod,
                0,
                "formatObject(Object, Set) deve declarar entrada @Nullable para itens nulos em arrays/maps.");

    }

    private static void assertParameterNullable(
            Executable executable,
            int parameterIndex,
            String errorMessage) {

        boolean parameterIsNullable = Arrays.stream(executable.getParameterAnnotations()[parameterIndex])
                .anyMatch(annotation -> annotation.annotationType().equals(Nullable.class));

        Assertions.assertTrue(parameterIsNullable, errorMessage);

    }

}

class MissingDependencySampleDto {

    public String materialId = "MAT-001";
    public Integer quantity = 3;

}
