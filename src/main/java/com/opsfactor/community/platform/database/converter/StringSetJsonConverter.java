package com.opsfactor.community.platform.database.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Persiste IDs escalares como JSON em uma coluna, sem relação com entidades
 * privadas e sem consultas adicionais por perfil. Não altera os IDs informados.
 */
@Converter
public class StringSetJsonConverter implements AttributeConverter<Set<String>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Set<String> ids) {

        Set<String> sortedIds = new TreeSet<>();
        if (ids != null) {
            for (String id : ids) {
                validateId(id);
                sortedIds.add(id);
            }
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(sortedIds);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not serialize the explicit ID selection.", exception);
        }

    }

    @Override
    public Set<String> convertToEntityAttribute(String json) {

        if (json == null) {
            return new LinkedHashSet<>();
        }
        try {
            JsonNode values = OBJECT_MAPPER.readTree(json);
            if (values == null || !values.isArray()) {
                throw new IllegalArgumentException("Explicit ID selection must be a JSON array.");
            }
            Set<String> ids = new LinkedHashSet<>();
            for (JsonNode value : values) {
                if (!value.isTextual()) {
                    throw new IllegalArgumentException("Explicit ID selection requires textual IDs.");
                }
                String id = value.textValue();
                validateId(id);
                if (!ids.add(id)) {
                    throw new IllegalArgumentException("Explicit ID selection contains a duplicate ID: " + id);
                }
            }
            return ids;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid persisted explicit ID selection.", exception);
        }

    }

    private static void validateId(String id) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Explicit ID selection cannot contain null or blank IDs.");
        }

    }

}
