package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One Jackson-derived schema shared by the binary writers. No Java class names enter files. */
final class ColumnarSchema {
    enum Kind { BOOLEAN, LONG, DOUBLE, STRING, DECIMAL, STRUCT, LIST, MAP }

    static final class Field {
        final String name;
        Kind kind;
        final List<Field> children = new ArrayList<>();
        int scale;
        int integerDigits;
        boolean decimalObserved;

        Field(String name, Kind kind) { this.name = name; this.kind = kind; }
        int precision() { return Math.max(1, integerDigits + scale); }

        void observe(JsonNode node) throws IOException {
            if (node == null || node.isNull()) return;
            switch (kind) {
                case STRUCT -> {
                    if (!node.isObject()) throw unsupported("Serializer changed object shape at " + name);
                    Set<String> expected = new HashSet<>();
                    for (Field child : children) {
                        expected.add(child.name);
                        child.observe(node.get(child.name));
                    }
                    var names = node.fieldNames();
                    while (names.hasNext()) {
                        String field = names.next();
                        if (!expected.contains(field)) throw unsupported("Unmapped serialized property " + field);
                    }
                }
                case LIST -> {
                    if (!node.isArray()) throw unsupported("Expected a list at " + name);
                    for (JsonNode element : node) children.getFirst().observe(element);
                }
                case MAP -> {
                    if (!node.isObject()) throw unsupported("Expected a string-keyed map at " + name);
                    for (JsonNode element : node) children.getFirst().observe(element);
                }
                case DECIMAL -> {
                    if (!node.isNumber()) throw unsupported("Expected a decimal at " + name);
                    BigDecimal number = node.decimalValue();
                    int actualScale = Math.max(0, number.scale());
                    scale = Math.max(scale, actualScale);
                    integerDigits = Math.max(integerDigits, number.precision() - number.scale());
                    decimalObserved = true;
                    if (precision() > 38) throw unsupported("Decimal exceeds supported precision 38 at " + name);
                }
                case LONG -> {
                    if (!node.isIntegralNumber() || !node.canConvertToLong())
                        throw unsupported("Expected a signed 64-bit integer at " + name);
                }
                case DOUBLE -> {
                    if (!node.isNumber() || !Double.isFinite(node.doubleValue()))
                        throw unsupported("Expected a finite floating point number at " + name);
                }
                case BOOLEAN -> {
                    if (!node.isBoolean()) throw unsupported("Expected a boolean at " + name);
                }
                case STRING -> {
                    if (!node.isTextual() && !node.isBinary())
                        throw unsupported("Expected a string at " + name + "; custom date/enum serializers must emit strings");
                }
            }
        }
    }

    static Field of(ObjectMapper mapper, JavaType type) throws IOException {
        Field schema = of(mapper, "row", type, new HashSet<>(), 0);
        if (schema.kind != Kind.STRUCT || schema.children.isEmpty())
            throw unsupported("Columnar data requires a DTO with at least one serializable property");
        return schema;
    }

    private static Field of(ObjectMapper mapper, String name, JavaType type, Set<JavaType> stack,
                            int depth) throws IOException {
        if (type == null || type.hasRawClass(Object.class))
            throw unsupported("Missing concrete Java type for " + name);
        if (depth > 32 || stack.contains(type)) throw unsupported("Recursive schema at " + name);
        Class<?> raw = type.getRawClass();
        if (raw == boolean.class || raw == Boolean.class) return new Field(name, Kind.BOOLEAN);
        if (raw == BigDecimal.class || raw == BigInteger.class) return new Field(name, Kind.DECIMAL);
        if (raw == float.class || raw == Float.class || raw == double.class || raw == Double.class)
            return new Field(name, Kind.DOUBLE);
        if (raw.isPrimitive() && raw != char.class || Number.class.isAssignableFrom(raw))
            return new Field(name, Kind.LONG);
        if (type.isEnumType()) {
            Object[] constants = raw.getEnumConstants();
            JsonNode sample = constants.length == 0 ? null : mapper.valueToTree(constants[0]);
            if (sample != null && sample.isIntegralNumber()) return new Field(name, Kind.LONG);
            if (sample != null && !sample.isTextual()) throw unsupported("Unsupported enum serializer at " + name);
            return new Field(name, Kind.STRING);
        }
        if (Date.class.isAssignableFrom(raw))
            return new Field(name, mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) ? Kind.LONG : Kind.STRING);
        if (TemporalAccessor.class.isAssignableFrom(raw) && mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
            if (raw == java.time.LocalDate.class || raw == java.time.LocalTime.class || raw == java.time.LocalDateTime.class
                    || raw == java.time.OffsetTime.class) {
                Field result = new Field(name, Kind.LIST);
                result.children.add(new Field("element", Kind.LONG));
                // OffsetTime adds a textual offset; reject instead of coercing it into an integer.
                if (raw == java.time.OffsetTime.class) throw unsupported("OffsetTime requires string date serialization at " + name);
                return result;
            }
            if (raw == java.time.Instant.class || raw == java.time.OffsetDateTime.class || raw == java.time.ZonedDateTime.class)
                return new Field(name, mapper.isEnabled(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS) ? Kind.DECIMAL : Kind.LONG);
        }
        if (raw == String.class || raw == Character.class || raw == char.class || raw == java.util.UUID.class
                || raw == byte[].class || TemporalAccessor.class.isAssignableFrom(raw)) return new Field(name, Kind.STRING);
        if (type.isCollectionLikeType() || type.isArrayType()) {
            Field result = new Field(name, Kind.LIST);
            result.children.add(of(mapper, "element", type.getContentType(), stack, depth + 1));
            return result;
        }
        if (type.isMapLikeType()) {
            if (!type.getKeyType().hasRawClass(String.class)) throw unsupported("Map keys must be strings at " + name);
            Field result = new Field(name, Kind.MAP);
            result.children.add(of(mapper, "value", type.getContentType(), stack, depth + 1));
            return result;
        }
        Field result = new Field(name, Kind.STRUCT);
        Set<JavaType> nested = new HashSet<>(stack);
        nested.add(type);
        for (BeanPropertyDefinition property : mapper.getSerializationConfig().introspect(type).findProperties()) {
            if (!property.couldSerialize() || property.getAccessor() == null) continue;
            var member = property.getPrimaryMember();
            JsonUnwrapped unwrapped = member == null ? null : member.getAnnotation(JsonUnwrapped.class);
            Field child = of(mapper, property.getName(), property.getPrimaryType(), nested, depth + 1);
            if (unwrapped != null && unwrapped.enabled()) {
                if (child.kind != Kind.STRUCT) throw unsupported("Only DTO structs can be unwrapped at " + name);
                for (Field field : child.children) {
                    Field renamed = new Field(unwrapped.prefix() + field.name + unwrapped.suffix(), field.kind);
                    renamed.children.addAll(field.children);
                    result.children.add(renamed);
                }
            } else result.children.add(child);
        }
        Set<String> unique = new HashSet<>();
        for (Field child : result.children)
            if (!unique.add(child.name)) throw unsupported("Duplicate serialized property: " + child.name);
        if (result.children.isEmpty()) throw unsupported("No supported DTO properties at " + name);
        return result;
    }

    static IOException unsupported(String message) { return new IOException(message); }
}
