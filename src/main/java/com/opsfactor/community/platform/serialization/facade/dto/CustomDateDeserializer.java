package com.opsfactor.community.platform.serialization.facade.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Desserializa datas de DTOs de integracao aceitando os formatos historicos
 * usados por templates e APIs legadas.
 *
 * <p>O deserializador e deliberadamente stateless: cada valor tenta todos os
 * formatos publicos aceitos na mesma ordem e falha somente depois de todos
 * rejeitarem o texto.</p>
 */
public class CustomDateDeserializer extends StdDeserializer<LocalDate> {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTER_LIST = List.of(
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("d/M/yyyy"));

    /**
     * Cria o deserializador usado pelo Jackson em anotacoes de campo.
     */
    public CustomDateDeserializer() {
        this(null);
    }

    /**
     * Cria o deserializador com a classe de valor informada pelo Jackson.
     *
     * @param vc classe de valor recebida pelo mecanismo de desserializacao
     */
    public CustomDateDeserializer(@Nullable Class<?> vc) {
        super(vc);
    }

    /**
     * Converte o texto JSON em {@link LocalDate}.
     *
     * <p>Valores vazios historicos continuam representando ausencia de data.
     * Formatos nao suportados preservam a ultima excecao de parse como causa
     * para diagnostico, mantendo a mensagem publica estavel.</p>
     *
     * @param jsonParser parser posicionado no campo textual de data
     * @param context contexto de desserializacao do Jackson
     * @return data desserializada ou {@code null} para os marcadores historicos
     * de ausencia
     * @throws IOException quando o parser JSON falhar
     * @throws JsonProcessingException quando o texto nao respeitar nenhum
     * formato publico aceito
     */
    @Override
    @Nullable
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        String date = jsonParser.getText().trim();
        if (date.equals("0") || date.equals("") || date.equals(" ") || date.length() == 0) return null;
        else if (date.length() < 8) {
            throw new JsonProcessingException(String.format("Wrong date [%s] format",date)) {};
        }

        DateTimeParseException ultimaDateTimeParseException = null;
        for (DateTimeFormatter dateTimeFormatter : DATE_TIME_FORMATTER_LIST) {
            try {
                return LocalDate.parse(date, dateTimeFormatter);
            } catch (DateTimeParseException dateTimeParseException) {
                // Tenta o proximo formato publico aceito pela integracao.
                ultimaDateTimeParseException = dateTimeParseException;
            }
        }

        throw new JsonProcessingException("Wrong date format", ultimaDateTimeParseException) {
        };
    }
}
