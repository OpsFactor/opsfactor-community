package com.opsfactor.community.platform.database.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converte arrays primitivos de {@code double} para JSON em colunas texto.
 *
 * <p>Este converter e instanciado pelo JPA, nao como service Spring. Por isso
 * usa um {@link ObjectMapper} local e final em vez de dependencia
 * {@code @Autowired}.</p>
 */
@Converter
public class DoubleArrayToStringConverter implements AttributeConverter<double[], String> {

    /**
     * Mapper local do converter JPA. Nao e bean Spring.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(double[] attribute) {

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error converting double array to JSON", e);
        }

    }

    @Override
    public double[] convertToEntityAttribute(String dbData) {

        try {
            return objectMapper.readValue(dbData, double[].class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error converting JSON to double array", e);
        }

    }

}
