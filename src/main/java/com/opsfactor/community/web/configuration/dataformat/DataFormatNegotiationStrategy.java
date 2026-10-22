package com.opsfactor.community.web.configuration.dataformat;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ResolvableType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;

/**
 * Opt-in output negotiation for Data APIs only. Missing format preserves JSON DTOs
 * and legacy JSON matrices; explicitly binary Planning Book routes remain native.
 */
public final class DataFormatNegotiationStrategy implements ContentNegotiationStrategy {

    @Override
    public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) throws HttpMediaTypeNotAcceptableException {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null || !isDataPath(request.getRequestURI())) {
            return MEDIA_TYPE_ALL_LIST;
        }
        HandlerMethod handler = DataFormatTypes.currentHandler();
        // Native file-returning methods have their own produces/content type. They
        // do not participate in DTO conversion, including the batch workbook POST.
        if (handler != null && isNativeFileHandler(handler)) {
            return MEDIA_TYPE_ALL_LIST;
        }
        String parameter = request.getParameter("format");
        if (parameter == null || parameter.isBlank()) {
            // Existing importers also depend on the confirmation contract. A POST
            // returning String historically uses Spring's normal String converter
            // (often text/plain). Do not relabel or reserialize it as JSON.
            if (!request.getMethod().equals("GET")) {
                return MEDIA_TYPE_ALL_LIST;
            }
            // During route matching no handler is selected yet. Avoid excluding
            // native produces=xlsx mappings before Spring has selected their method.
            return handler == null ? MEDIA_TYPE_ALL_LIST : List.of(MediaType.APPLICATION_JSON);
        }
        try {
            DataFormat format = DataFormat.fromParameter(parameter);
            if (!parameter.equals(format.extension())) {
                throw new IllegalArgumentException("Format names must use their documented lowercase spelling.");
            }
            return List.of(MediaType.parseMediaType(format.mediaType()));
        } catch (IllegalArgumentException exception) {
            throw new HttpMediaTypeNotAcceptableException(Arrays.stream(DataFormat.values())
                    .map(format -> MediaType.parseMediaType(format.mediaType())).toList());
        }

    }

    static boolean isDataPath(String path) {

        return path.startsWith("/api/secured/data/") || path.startsWith("/api/secured/dataupload/")
                || path.startsWith("/api/secured/deployment/detailedreport/")
                || path.startsWith("/api/secured/planning/demand/changelog/");

    }

    private static boolean isNativeFileHandler(HandlerMethod handler) {

        ResolvableType returnType = ResolvableType.forMethodReturnType(handler.getMethod(), handler.getBeanType());
        if (ResponseEntity.class.isAssignableFrom(returnType.toClass())) {
            returnType = returnType.getGeneric(0);
        }
        Class<?> returnedClass = returnType.toClass();
        return returnedClass == Void.TYPE || returnedClass == byte[].class
                || org.springframework.core.io.Resource.class.isAssignableFrom(returnedClass);

    }
}
