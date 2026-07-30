package com.opsfactor.community.platform.exception;

import jakarta.annotation.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Excecao especifica para sinalizar que uma dependencia obrigatoria do DTO nao
 * foi encontrada durante a conversao para entidade.
 */
public class MissingDependencyDataUploadException extends DataUploadException {

    /**
     * DTO original recebido no Data Upload. O valor fica nulo quando a falha nao
     * possui linha/payload de origem anexado.
     */
    @Nullable
    private final transient Object dto;

    /**
     * Representacao textual do DTO usada para diagnostico. Permanece nula
     * quando a excecao foi criada apenas com a mensagem funcional.
     */
    @Nullable
    private final String dtoContent;

    public MissingDependencyDataUploadException(String errorMessage) {
        this(errorMessage, null);
    }

    public MissingDependencyDataUploadException(String errorMessage, @Nullable Object dto) {
        super(buildMessage(errorMessage, dto));
        this.dto = dto;
        this.dtoContent = formatObject(dto);
    }

    /**
     * Retorna o DTO original quando a camada de integracao conseguiu anexar o
     * payload problematico a excecao.
     */
    @Nullable
    public Object getDto() {
        return dto;
    }

    /**
     * Retorna a representacao textual do DTO anexado, ou nulo quando nao ha DTO
     * associado a falha funcional.
     */
    @Nullable
    public String getDtoContent() {
        return dtoContent;
    }

    private static String buildMessage(String errorMessage, @Nullable Object dto) {
        String dtoContent = formatObject(dto);
        if (dtoContent == null) {
            return errorMessage;
        }
        return errorMessage + " | DTO: " + dtoContent;
    }

    @Nullable
    private static String formatObject(@Nullable Object object) {
        if (object == null) {
            return null;
        }
        return formatObject(object, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static String formatObject(@Nullable Object object, Set<Object> visitedObjects) {
        if (object == null) {
            return "null";
        }
        if (isSimpleValue(object)) {
            return String.valueOf(object);
        }
        if (visitedObjects.contains(object)) {
            return "<recursion>";
        }

        visitedObjects.add(object);
        try {
            if (object.getClass().isArray()) {
                return formatArray(object, visitedObjects);
            }
            if (object instanceof Collection<?> collection) {
                return collection.stream()
                        .map(item -> formatObject(item, visitedObjects))
                        .collect(Collectors.joining(", ", "[", "]"));
            }
            if (object instanceof Map<?, ?> map) {
                return map.entrySet().stream()
                        .map(entry -> formatObject(entry.getKey(), visitedObjects) + "=" + formatObject(entry.getValue(), visitedObjects))
                        .collect(Collectors.joining(", ", "{", "}"));
            }

            Field[] publicFields = Arrays.stream(object.getClass().getFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .toArray(Field[]::new);
            if (publicFields.length == 0) {
                return String.valueOf(object);
            }

            String fieldContents = Arrays.stream(publicFields)
                    .map(field -> field.getName() + "=" + getFieldValueAsString(field, object, visitedObjects))
                    .collect(Collectors.joining(", "));
            return object.getClass().getSimpleName() + "{" + fieldContents + "}";
        } finally {
            visitedObjects.remove(object);
        }
    }

    private static String formatArray(Object array, Set<Object> visitedObjects) {
        int arrayLength = Array.getLength(array);
        StringBuilder stringBuilder = new StringBuilder("[");
        for (int i = 0; i < arrayLength; i++) {
            if (i > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(formatObject(Array.get(array, i), visitedObjects));
        }
        return stringBuilder.append("]").toString();
    }

    private static String getFieldValueAsString(Field field, Object object, Set<Object> visitedObjects) {
        try {
            return formatObject(field.get(object), visitedObjects);
        } catch (IllegalAccessException illegalAccessException) {
            return "<inaccessible>";
        }
    }

    private static boolean isSimpleValue(Object object) {
        Class<?> objectClass = object.getClass();
        Package objectPackage = objectClass.getPackage();
        String packageName = objectPackage == null ? "" : objectPackage.getName();

        return objectClass.isPrimitive()
                || object instanceof Number
                || object instanceof CharSequence
                || object instanceof Boolean
                || object instanceof Enum<?>
                || object instanceof Class<?>
                || packageName.startsWith("java.time");
    }

}
