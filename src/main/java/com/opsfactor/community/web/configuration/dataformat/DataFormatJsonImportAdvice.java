package com.opsfactor.community.web.configuration.dataformat;

import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.SequenceInputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Accepts an unchanged JSON export list at an existing integration-envelope import.
 * Jackson still resolves the concrete inherited DTO and validates its normal schema.
 */
@ControllerAdvice(assignableTypes = IntegrationControllerAbstract.class)
public final class DataFormatJsonImportAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {

        return IntegrationControllerAbstract.class.isAssignableFrom(parameter.getContainingClass())
                && IntegrationDto.class.isAssignableFrom(parameter.getParameterType())
                && MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);

    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage input, MethodParameter parameter,
            Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {

        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return input;
        var request = attributes.getRequest();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!(path.startsWith("/api/secured/data/") || path.startsWith("/api/secured/dataupload/"))) return input;
        MediaType contentType = input.getHeaders().getContentType();
        if (contentType == null || !(MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                || contentType.getSubtype().endsWith("+json"))) return input;
        // JSON exchanged by the file workflow is UTF-8. Leave other declared
        // encodings to Jackson's existing reader instead of inserting UTF-8 bytes.
        if (contentType.getCharset() != null && !StandardCharsets.UTF_8.equals(contentType.getCharset())) return input;

        PushbackInputStream body = new PushbackInputStream(input.getBody(), 3);
        byte[] probe = body.readNBytes(3);
        boolean bom = probe.length == 3 && (probe[0] & 0xff) == 0xef
                && (probe[1] & 0xff) == 0xbb && (probe[2] & 0xff) == 0xbf;
        if (!bom) body.unread(probe);
        int first;
        boolean skippedWhitespace = false;
        do {
            first = body.read();
            if (first == ' ' || first == '\t' || first == '\r' || first == '\n') skippedWhitespace = true;
            else break;
        } while (true);
        if (first != -1) body.unread(first);

        InputStream adapted = body;
        if (first == '[') {
            // Constant memory: the original array streams through untouched.
            adapted = new SequenceInputStream(Collections.enumeration(List.of(
                    new ByteArrayInputStream("{\"data\":".getBytes(StandardCharsets.UTF_8)),
                    body,
                    new ByteArrayInputStream("}".getBytes(StandardCharsets.UTF_8)))));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(input.getHeaders());
        // A wrapper or stripped BOM/JSON whitespace changes the byte count.
        // Unknown length is truthful and keeps the original headers unmodified.
        if (first == '[' || bom || skippedWhitespace) headers.remove(HttpHeaders.CONTENT_LENGTH);
        InputStream result = adapted;
        return new HttpInputMessage() {
            @Override public InputStream getBody() { return result; }
            @Override public HttpHeaders getHeaders() { return headers; }
        };

    }
}
