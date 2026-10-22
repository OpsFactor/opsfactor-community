package com.opsfactor.community.web.configuration.dataformat;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Reports an unsupported tabular shape as a readable JSON error, not a corrupt download. */
@RestControllerAdvice
public final class DataFormatExceptionHandler {

    @ExceptionHandler(TabularExportException.class)
    public ResponseEntity<Map<String, Object>> unsupportedHierarchy(TabularExportException exception) {

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", HttpStatus.NOT_ACCEPTABLE.value(),
                        "code", "HIERARCHICAL_EXPORT_NOT_SUPPORTED", "message", exception.getMessage()));

    }
}
