package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds binary/table representations to Spring's normal DTO pipeline. Jackson stays
 * responsible for JSON, status messages and error envelopes. Multipart uploads keep
 * their existing argument resolver and template mapper.
 */
public final class DataFormatHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

    private final ObjectMapper objectMapper;
    private final DataFormatCodec codec;
    private final String defaultFieldSeparator;

    public DataFormatHttpMessageConverter(ObjectMapper objectMapper) {

        this(objectMapper, ",");

    }

    public DataFormatHttpMessageConverter(ObjectMapper objectMapper, String defaultFieldSeparator) {

        super(MediaType.parseMediaType("application/vnd.apache.arrow.stream"),
                MediaType.parseMediaType("application/vnd.apache.parquet"),
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                new MediaType("text", "csv", StandardCharsets.UTF_8));
        this.objectMapper = objectMapper;
        this.codec = new DataFormatCodec(objectMapper);
        this.defaultFieldSeparator = defaultFieldSeparator;

    }

    @Override
    protected boolean supports(Class<?> clazz) {

        return Collection.class.isAssignableFrom(clazz) || clazz.getSimpleName().equals("IntegrationDto")
                || DataFormatGridAdapter.supports(clazz)
                || String.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)
                || clazz.getSimpleName().equals("ResponseDTO");

    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {

        HttpServletRequest request = currentRequest();
        // Explicit opt-in preserves legacy matrix/JSON responses even if an old
        // downloader sent Accept: application/vnd...sheet before requesting JSON.
        if (request == null || request.getParameter("format") == null || !supports(clazz)
                || !DataFormatNegotiationStrategy.isDataPath(request.getRequestURI())) {
            return false;
        }
        return super.canWrite(clazz, mediaType);

    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {

        return canWrite(clazz, mediaType);

    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {

        return (Collection.class.isAssignableFrom(clazz) || clazz.getSimpleName().equals("IntegrationDto")) && isBinaryInput(mediaType);

    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {

        if (!isBinaryInput(mediaType)) {
            return false;
        }
        JavaType javaType = DataFormatTypes.resolve(objectMapper, type, contextClass, null);
        return Collection.class.isAssignableFrom(javaType.getRawClass()) || javaType.getRawClass().getSimpleName().equals("IntegrationDto");

    }

    @Override
    public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException {

        JavaType javaType = DataFormatTypes.resolve(objectMapper, type, contextClass, null);
        try {
            return codec.read(inputMessage.getBody(), javaType, formatFor(inputMessage.getHeaders().getContentType()));
        } catch (IllegalArgumentException exception) {
            throw new HttpMessageNotReadableException("The integration payload does not match its declared DTO schema.", exception, inputMessage);
        }

    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {

        return read(clazz, null, inputMessage);

    }

    @Override
    protected void writeInternal(Object body, Type type, HttpOutputMessage outputMessage) throws IOException {

        // Save confirmations and error DTOs are not tabular datasets. Keep those
        // replies JSON even when the caller selected a binary extraction format.
        if (!(body instanceof Collection<?>) && !body.getClass().getSimpleName().equals("IntegrationDto")
                && !DataFormatGridAdapter.supports(body.getClass())) {
            outputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            objectMapper.writeValue(outputMessage.getBody(), body);
            return;
        }
        DataFormat format = formatFor(outputMessage.getHeaders().getContentType());
        JavaType javaType = DataFormatTypes.resolve(objectMapper, type, null, body);
        if (DataFormatGridAdapter.supports(body.getClass())) {
            body = DataFormatGridAdapter.matrix(body, objectMapper);
            javaType = objectMapper.getTypeFactory().constructCollectionType(List.class,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class));
        }
        HttpServletRequest request = currentRequest();
        Map<String, String> options = new LinkedHashMap<>();
        if (request != null) {
            for (String name : new String[]{"fieldSeparator", "decimalSeparator", "csvDelimiter", "csvDecimalSeparator",
                    "csvLineEnding", "csvIncludeHeaders"}) {
                String value = request.getParameter(name);
                if (value != null) {
                    options.put(name, value);
                }
            }
        }
        options.putIfAbsent("fieldSeparator", defaultFieldSeparator);
        // Preparation rejects hierarchical flat-file exports before a response body
        // or attachment header exists, allowing the normal Spring error pipeline.
        DataFormatCodec.PreparedWrite prepared = codec.prepareWrite(body, javaType, format, options);
        String resourceName = request == null ? "data" : request.getRequestURI().replaceAll("[^a-zA-Z0-9_-]+", "-");
        outputMessage.getHeaders().setContentDisposition(ContentDisposition.attachment()
                .filename(resourceName + "." + format.extension(), StandardCharsets.UTF_8).build());
        try {
            prepared.writeTo(outputMessage.getBody());
        } catch (IllegalArgumentException exception) {
            throw new HttpMessageNotWritableException("Cannot export the selected integration schema as " + format + ".", exception);
        }

    }

    private static boolean isBinaryInput(MediaType mediaType) {

        return mediaType != null && (mediaType.isCompatibleWith(MediaType.parseMediaType("application/vnd.apache.arrow.stream"))
                || mediaType.isCompatibleWith(MediaType.parseMediaType("application/vnd.apache.parquet")));

    }

    private static DataFormat formatFor(MediaType mediaType) {

        for (DataFormat format : DataFormat.values()) {
            if (mediaType != null && mediaType.isCompatibleWith(MediaType.parseMediaType(format.mediaType()))) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unsupported integration media type: " + mediaType);

    }

    private static HttpServletRequest currentRequest() {

        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;

    }
}
