package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.*;
import java.util.Date;
import java.util.Map;

/** Retains declared temporal values without guessing dates from textual identifiers. */
final class DataFormatTemporalValues {

    private DataFormatTemporalValues() { }

    static boolean isDate(Object value) {

        return value instanceof LocalDate || value instanceof LocalDateTime
                || value instanceof LocalTime || value instanceof Date;

    }

    /** Resolve Jackson names, unwrapped keys and report paths against the original row. */
    static Object value(ObjectMapper mapper, Object source, String field) throws IOException {

        return value(mapper, source, field, 0);

    }

    private static Object value(ObjectMapper mapper, Object source, String field, int depth) throws IOException {

        if (source == null || depth > 32) return null;
        if (source instanceof Map<?, ?> map) {
            if (map.containsKey(field)) return map.get(field);
            int dot = field.indexOf('.');
            return dot < 0 ? null : value(mapper, map.get(field.substring(0, dot)), field.substring(dot + 1), depth + 1);
        }
        JavaType type = mapper.constructType(source.getClass());
        for (var property : mapper.getSerializationConfig().introspect(type).findProperties()) {
            if (!property.couldSerialize() || property.getAccessor() == null) continue;
            var accessor = property.getAccessor();
            JsonUnwrapped unwrapped = accessor.getAnnotation(JsonUnwrapped.class);
            String nested = null;
            if (property.getName().equals(field)) nested = "";
            else if (field.startsWith(property.getName() + ".")) nested = field.substring(property.getName().length() + 1);
            else if (unwrapped != null && unwrapped.enabled()
                    && field.length() >= unwrapped.prefix().length() + unwrapped.suffix().length()
                    && field.startsWith(unwrapped.prefix())
                    && field.endsWith(unwrapped.suffix())) {
                nested = field.substring(unwrapped.prefix().length(), field.length() - unwrapped.suffix().length());
            }
            if (nested == null) continue;
            try {
                accessor.fixAccess(true);
                Object result = accessor.getValue(source);
                result = nested.isEmpty() ? result : value(mapper, result, nested, depth + 1);
                if (result != null) return result;
            } catch (RuntimeException exception) {
                throw new IOException("Cannot read spreadsheet property " + field, exception);
            }
        }
        return null;

    }

    /** The target DTO type, rather than the cell's display text, selects date versus time. */
    static JavaType propertyType(ObjectMapper mapper, JavaType type, String field) {

        for (var property : mapper.getSerializationConfig().introspect(type).findProperties()) {
            if (!property.couldSerialize() || property.getAccessor() == null) continue;
            if (property.getName().equals(field)) return property.getPrimaryType();
            JsonUnwrapped unwrapped = property.getAccessor().getAnnotation(JsonUnwrapped.class);
            if (unwrapped != null && unwrapped.enabled()
                    && field.length() >= unwrapped.prefix().length() + unwrapped.suffix().length()
                    && field.startsWith(unwrapped.prefix()) && field.endsWith(unwrapped.suffix())) {
                String nested = field.substring(unwrapped.prefix().length(), field.length() - unwrapped.suffix().length());
                JavaType result = propertyType(mapper, property.getPrimaryType(), nested);
                if (result != null) return result;
            }
        }
        return null;

    }

    static Object imported(ObjectMapper mapper, LocalDateTime value, JavaType target) throws IOException {

        if (target == null || target.hasRawClass(LocalDateTime.class)) return value;
        if (target.hasRawClass(LocalDate.class)) return value.toLocalDate();
        if (target.hasRawClass(LocalTime.class)) return value.toLocalTime();
        if (target.hasRawClass(java.sql.Time.class)) return java.sql.Time.valueOf(value.toLocalTime());
        // SQL Date uses the JVM's local calendar. Passing its ISO string through
        // Jackson's timezone would shift the day; epoch milliseconds preserve that date.
        if (target.hasRawClass(java.sql.Date.class)) return java.sql.Date.valueOf(value.toLocalDate()).getTime();
        if (target.hasRawClass(java.sql.Timestamp.class)) return java.sql.Timestamp.valueOf(value);
        if (target.hasRawClass(Date.class)) return Date.from(value.atZone(mapper.getSerializationConfig().getTimeZone().toZoneId()).toInstant());
        throw new IOException("An Excel date cell requires a date/time target, found " + target);

    }
}
