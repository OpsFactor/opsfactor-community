package com.opsfactor.community.platform.database.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converte arrays primitivos de {@code float} para JSON em colunas texto.
 *
 * <p>Este converter e instanciado pelo JPA, nao como service Spring. Por isso
 * usa um {@link ObjectMapper} local e final em vez de dependencia
 * {@code @Autowired}.</p>
 */
@Converter
public class FloatArrayToStringConverter implements AttributeConverter<float[], String> {

    /**
     * Mapper local do converter JPA. Nao e bean Spring.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(float[] attribute) {

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error converting float array to JSON", e);
        }

    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {

        try {
            return objectMapper.readValue(dbData, float[].class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error converting JSON to float array", e);
        }

    }

}
