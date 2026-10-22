package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/** Resolves inherited integration DTO parameters without trusting class names in incoming files. */
final class DataFormatTypes {

    private DataFormatTypes() {

    }

    static HandlerMethod currentHandler() {

        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                && attributes.getRequest().getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE)
                instanceof HandlerMethod handler) {
            return handler;
        }
        return null;

    }

    static JavaType resolve(ObjectMapper mapper, Type declaredType, Class<?> contextClass, Object body) {

        HandlerMethod handler = currentHandler();
        Class<?> controllerClass = contextClass != null ? contextClass : handler == null ? null : handler.getBeanType();
        Class<?> integrationBase = findIntegrationBase(controllerClass);
        if (integrationBase != null) {
            ResolvableType controller = ResolvableType.forClass(controllerClass).as(integrationBase);
            JavaType recordType = mapper.constructType(controller.getGeneric(1).getType());
            if (handler != null && handler.getMethod().getName().equals("getDataRecordDtoList")) {
                return mapper.getTypeFactory().constructCollectionType(List.class, recordType);
            }
            if (declaredType != null && ResolvableType.forType(declaredType).toClass().getSimpleName().equals("IntegrationDto")) {
                Class<?> envelopeClass = ResolvableType.forType(declaredType).toClass();
                return mapper.getTypeFactory().constructParametricType(envelopeClass,
                        recordType,
                        mapper.constructType(controller.getGeneric(2).getType()),
                        mapper.constructType(controller.getGeneric(3).getType()),
                        mapper.constructType(controller.getGeneric(5).getType()));
            }
        }

        Type resolvedType = declaredType == null ? Object.class
                : controllerClass == null ? declaredType : GenericTypeResolver.resolveType(declaredType, controllerClass);
        JavaType javaType = mapper.constructType(resolvedType);
        // ResponseEntity describes HTTP status/headers, not a dataset row. Retain
        // its concrete body type even for an empty collection; runtime sampling
        // cannot recover a schema when no row exists.
        if (ResponseEntity.class.isAssignableFrom(javaType.getRawClass())) {
            javaType = javaType.containedTypeOrUnknown(0);
        }
        // Older plan-export controllers use a raw ResponseEntity. The actual collection
        // still identifies its row DTO; typed integration endpoints also cover empty lists.
        if (body instanceof Collection<?> rows && !rows.isEmpty()
                && (!javaType.isCollectionLikeType() || javaType.getContentType().isJavaLangObject())) {
            Object first = rows.iterator().next();
            if (first != null) {
                javaType = mapper.getTypeFactory().constructCollectionType(List.class, first.getClass());
            }
        }
        return javaType;

    }

    private static Class<?> findIntegrationBase(Class<?> controllerClass) {

        for (Class<?> candidate = controllerClass; candidate != null; candidate = candidate.getSuperclass()) {
            if (candidate.getSimpleName().equals("IntegrationControllerAbstract")) {
                return candidate;
            }
        }
        return null;

    }
}
