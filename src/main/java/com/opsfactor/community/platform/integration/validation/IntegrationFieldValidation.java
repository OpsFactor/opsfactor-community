package com.opsfactor.community.platform.integration.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/**
 * Builds field diagnostics from input provenance and declarative column links.
 *
 * <p>Field reflection is cached per DTO class. No names, mandatory rules or
 * foreign-key semantics are inferred from a value, a field suffix or field
 * declaration order. References are resolved against the mapper's batch
 * snapshot, preserving its existing optional/gated behavior and round trips.</p>
 */
public final class IntegrationFieldValidation {

    /** Includes CR/LF, CRLF and Unicode line separators pasted from spreadsheets. */
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    private static final ClassValue<Map<String, Field>> FIELD_CACHE = new ClassValue<>() {

        @Override
        protected Map<String, Field> computeValue(Class<?> recordClass) {

            Map<String, Field> fields = new LinkedHashMap<>();
            Arrays.stream(recordClass.getFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .forEach(field -> {
                        // Public DTO fields may be declared by a non-public nested DTO class.
                        if (!field.trySetAccessible()) throw new IllegalStateException("Cannot inspect integration field " + field);
                        fields.put(field.getName(), field);
                    });
            return Map.copyOf(fields);

        }

    };

    private IntegrationFieldValidation() {

    }

    /** Returns a declared column, or -1 for properties supplied outside the file. */
    public static int fileColumn(Object owner, String fieldName) {

        IntegrationFileColumn column = field(owner, fieldName).getAnnotation(IntegrationFileColumn.class);
        return column == null ? -1 : column.value();

    }

    /** Reads the exact property referenced by a mapper; missing metadata is a programming error. */
    public static Object value(Object owner, String fieldName) {

        try {
            return field(owner, fieldName).get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read integration field " + fieldName, exception);
        }

    }

    /** Selects Excel's published header, or Jackson's external JSON property name. */
    public static String label(Object owner, String fieldName, int fileColumn) {

        Field field = field(owner, fieldName);
        IntegrationInputContext context = context(owner);
        if (context != null && context.file() && fileColumn >= 0) {
            if (fileColumn >= context.fileHeaders().size()) {
                throw new IllegalStateException("Integration column outside header contract for " + fieldName);
            }
            String header = context.fileHeaders().get(fileColumn);
            if (header == null || header.isBlank()) {
                throw new IllegalStateException("Integration column has no header for " + fieldName);
            }
            return header;
        }
        JsonProperty property = field.getAnnotation(JsonProperty.class);
        return property == null || property.value().isEmpty() ? fieldName : property.value();

    }

    /** Resolves a foreign key at the mapper's original validation point. */
    public static <V> V resolveReference(Map<?, V> snapshot, Object owner,
                                           String fieldName, Object lookupValue,
                                           boolean required, int fileColumn) {

        String label = label(owner, fieldName, fileColumn);
        if (lookupValue == null) {
            if (!required) return null;
            String role = primaryKey(owner) ? "Primary key reference" : "Required foreign key";
            throw new DataUploadException(role + " field '" + label + "' is missing");
        }
        V referencedRecord = snapshot.get(lookupValue);
        if (referencedRecord == null) {
            String role = primaryKey(owner) ? "Primary key reference" : "Foreign key";
            throw new MissingDependencyDataUploadException(
                    role + " field '" + label + "' references an unknown value '" + lookupValue + "'", owner);
        }
        return referencedRecord;

    }

    /**
     * Checks only explicitly declared required scalars/key fields. This must
     * not turn optional references or ignored update fields into requirements.
     */
    public static void validateRequiredFields(Object owner) {

        validateRequiredFields(owner, fieldName -> true, fieldName -> fileColumn(owner, fieldName));

    }

    /** Allows the mapper to preserve IGNORE/delete gates and dynamic file positions. */
    public static void validateRequiredFields(Object owner, Predicate<String> includedField, ToIntFunction<String> columnResolver) {

        if (owner == null) throw new DataUploadException("Primary key is missing");
        for (Field field : FIELD_CACHE.get(owner.getClass()).values()) {
            IntegrationFileColumn column = field.getAnnotation(IntegrationFileColumn.class);
            if (column == null || !column.required() || !includedField.test(field.getName())) continue;
            Object value = value(owner, field.getName());
            if (value == null || value instanceof String text && text.isBlank()) {
                String role = primaryKey(owner) ? "Primary key" : "Required";
                throw new DataUploadException(role + " field '"
                        + label(owner, field.getName(), columnResolver.applyAsInt(field.getName())) + "' is missing");
            }
        }

    }

    /**
     * Rejects malformed textual keys only at the upload boundary, before text
     * normalization can remove trailing Enter characters. Never apply this to
     * keys extracted from persisted entities, exports or calculation snapshots.
     * All textual components of a composite primary key follow the same rule;
     * descriptions and other non-key text remain outside this validation.
     */
    public static void validateInputPrimaryKeyText(Object owner) {

        if (owner == null) return; // Missing keys retain the existing required-field diagnostic.
        for (Field field : FIELD_CACHE.get(owner.getClass()).values()) {
            if (!String.class.equals(field.getType())) continue;
            String text = (String) value(owner, field.getName());
            if (text == null || text.isEmpty()) continue;
            String label = label(owner, field.getName(), fileColumn(owner, field.getName()));
            validateInputIdentifier(text, label);
        }

    }

    /** Validates a raw identifier in importers that do not use the generic key DTO. */
    public static void validateInputIdentifier(String text, String label) {

        if (text == null || text.isEmpty()) return;
        // A formula marker is invalid in an uploaded key; never silently strip or evaluate it.
        if (text.startsWith("=")) {
            throw new DataUploadException("Primary key field '" + label
                    + "': Excel formulas are not accepted");
        }
        // Check line breaks first so a trailing Enter is reported explicitly.
        if (LINE_BREAK_PATTERN.matcher(text).find()) {
            throw new DataUploadException("Primary key field '" + label
                    + "' contains a line break (Enter); remove it before uploading");
        }
        int firstCharacter = text.codePointAt(0);
        int lastCharacter = text.codePointBefore(text.length());
        if (Character.isWhitespace(firstCharacter) || Character.isSpaceChar(firstCharacter)
                || Character.isWhitespace(lastCharacter) || Character.isSpaceChar(lastCharacter)) {
            throw new DataUploadException("Primary key field '" + label
                    + "' has leading or trailing whitespace; remove it before uploading");
        }

    }

    /** Context is optional for direct mapper calls that did not originate in an upload. */
    public static IntegrationInputContext context(Object owner) {

        return owner instanceof IntegrationInputAware aware ? aware.getIntegrationInputContext() : null;

    }

    public static String location(Object owner) {

        IntegrationInputContext context = context(owner);
        return context == null ? "" : context.location();

    }

    private static boolean primaryKey(Object owner) {

        return owner instanceof IntegrationInputAware aware && aware.isIntegrationPrimaryKey();

    }

    private static Field field(Object owner, String fieldName) {

        if (owner == null) throw new IllegalArgumentException("Integration field owner is null");
        Field field = FIELD_CACHE.get(owner.getClass()).get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Unknown integration field " + owner.getClass().getName() + "." + fieldName);
        }
        return field;

    }

}
