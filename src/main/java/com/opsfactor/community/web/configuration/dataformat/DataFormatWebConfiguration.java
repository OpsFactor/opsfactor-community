package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Registers shared codecs once; individual integration methods need no format annotations. */
@Configuration
public class DataFormatWebConfiguration implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;
    private final String fieldSeparator;

    public DataFormatWebConfiguration(ObjectMapper objectMapper,
            @Value("${opsfactor.data-integration.csv.field-separator:,}") String fieldSeparator) {

        this.objectMapper = objectMapper;
        this.fieldSeparator = fieldSeparator;

    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {

        configurer.strategies(List.of(new DataFormatNegotiationStrategy(), new HeaderContentNegotiationStrategy()));

    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {

        // Keep all standard JSON, String, Resource and multipart converters.
        converters.add(0, new DataFormatHttpMessageConverter(objectMapper, fieldSeparator));

    }
}
