package com.opsfactor.community.platform.serialization.facade.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * Contrato do deserializador de datas usado em DTOs de integracao.
 *
 * <p>O parser aceita os formatos historicos dos templates e deve ser
 * stateless: desserializar um formato nao pode alterar a tentativa seguinte.</p>
 */
class CustomDateDeserializerCommunityContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializeShouldAcceptSupportedDateFormatsWithoutRetainingPreviousFormatState() throws Exception {

        Assertions.assertEquals(
                LocalDate.of(2026, 6, 25),
                readPayload("{\"date\":\"2026-6-25\"}").date);
        Assertions.assertEquals(
                LocalDate.of(2026, 6, 25),
                readPayload("{\"date\":\"25-6-2026\"}").date);
        Assertions.assertEquals(
                LocalDate.of(2026, 6, 25),
                readPayload("{\"date\":\"2026/6/25\"}").date);
        Assertions.assertEquals(
                LocalDate.of(2026, 6, 25),
                readPayload("{\"date\":\"25/6/2026\"}").date);
        Assertions.assertEquals(
                LocalDate.of(2026, 6, 25),
                readPayload("{\"date\":\"2026-6-25\"}").date);

    }

    @Test
    void deserializeShouldPreserveHistoricalBlankMarkersAsNull() throws Exception {

        Assertions.assertNull(readPayload("{\"date\":\"0\"}").date);
        Assertions.assertNull(readPayload("{\"date\":\"\"}").date);
        Assertions.assertNull(readPayload("{\"date\":\" \"}").date);

    }

    @Test
    void deserializeShouldRejectUnsupportedDateFormat() {

        JsonMappingException jsonMappingException = Assertions.assertThrows(
                JsonMappingException.class,
                () -> readPayload("{\"date\":\"2026.06.25\"}"));

        Assertions.assertTrue(jsonMappingException.getMessage().contains("Wrong date format"));
        Assertions.assertTrue(temCausaDateTimeParseException(jsonMappingException));

    }

    @Test
    void nullableContractShouldBeDeclaredExplicitly() throws ReflectiveOperationException {

        Constructor<CustomDateDeserializer> customDateDeserializerConstructor =
                CustomDateDeserializer.class.getConstructor(Class.class);
        Method deserializeMethod = CustomDateDeserializer.class.getDeclaredMethod(
                "deserialize",
                JsonParser.class,
                DeserializationContext.class);

        assertParameterNullable(
                customDateDeserializerConstructor,
                0,
                "O construtor deve declarar classe de valor @Nullable porque o construtor padrao delega null.");
        Assertions.assertTrue(
                deserializeMethod.isAnnotationPresent(Nullable.class),
                "deserialize deve declarar retorno @Nullable para marcadores historicos de ausencia.");

    }

    private boolean temCausaDateTimeParseException(Throwable throwable) {

        /*
         * O ObjectMapper pode acrescentar camadas de JsonMappingException ao
         * erro original do deserializador. O contrato relevante e a causa
         * tecnica de parse sobreviver em alguma parte da cadeia.
         */
        Throwable causaAtual = throwable;
        while (causaAtual != null) {
            if (causaAtual instanceof DateTimeParseException) {
                return true;
            }
            causaAtual = causaAtual.getCause();
        }
        return false;

    }

    private static void assertParameterNullable(
            Executable executable,
            int parameterIndex,
            String errorMessage) {

        boolean parameterIsNullable = Arrays.stream(executable.getParameterAnnotations()[parameterIndex])
                .anyMatch(annotation -> annotation.annotationType().equals(Nullable.class));

        Assertions.assertTrue(parameterIsNullable, errorMessage);

    }

    private DatePayload readPayload(String payload) throws Exception {

        return objectMapper.readValue(payload, DatePayload.class);

    }

    private static class DatePayload {

        @JsonDeserialize(using = CustomDateDeserializer.class)
        @Nullable
        public LocalDate date;

    }

}
