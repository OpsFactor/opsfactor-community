package com.opsfactor.community.platform.integration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contexto transversal de observabilidade das integracoes genericas.
 * O controller cria o contexto da request e os services comuns acumulam os
 * contadores de registros persistidos, removidos e ignorados.
 */
public class IntegrationLoggingContext {

    private static final Logger log = LoggerFactory.getLogger(IntegrationLoggingContext.class);
    private static final ThreadLocal<IntegrationLoggingContext> CONTEXTO_ATUAL = new ThreadLocal<>();

    private final boolean lifecycleLoggingEnabled;
    private final boolean ignoredErrorsLoggingEnabled;
    private final String apiPath;
    private final String httpMethod;
    private final String username;
    private final String ipAddress;
    private final String entityClassName;
    private final String operationName;
    private final Integer totalRecords;

    private final AtomicInteger savedRecords = new AtomicInteger();
    private final AtomicInteger removedRecords = new AtomicInteger();
    private final AtomicInteger ignoredRecords = new AtomicInteger();

    private Instant startedAt;
    private Instant finishedAt;
    private String status = "RUNNING";
    private String failureMessage;

    public IntegrationLoggingContext(
            boolean lifecycleLoggingEnabled,
            boolean ignoredErrorsLoggingEnabled,
            String apiPath,
            String httpMethod,
            String username,
            String ipAddress,
            String entityClassName,
            String operationName,
            Integer totalRecords) {

        this.lifecycleLoggingEnabled = lifecycleLoggingEnabled;
        this.ignoredErrorsLoggingEnabled = ignoredErrorsLoggingEnabled;
        this.apiPath = apiPath;
        this.httpMethod = httpMethod;
        this.username = username;
        this.ipAddress = ipAddress;
        this.entityClassName = entityClassName;
        this.operationName = operationName;
        this.totalRecords = totalRecords;

    }

    public static void setCurrent(IntegrationLoggingContext integrationLoggingContext) {

        CONTEXTO_ATUAL.set(integrationLoggingContext);

    }

    public static IntegrationLoggingContext getCurrent() {

        return CONTEXTO_ATUAL.get();

    }

    public static void clearCurrent() {

        CONTEXTO_ATUAL.remove();

    }

    public void markStarted() {

        startedAt = Instant.now();
        status = "RUNNING";

    }

    public void markSuccess() {

        status = "SUCCESS";

    }

    public void markFailure(Throwable throwable) {

        status = "FAILED";
        failureMessage = throwable == null ? null : throwable.getMessage();

    }

    public void markFinished() {

        finishedAt = Instant.now();

    }

    public void addSavedRecords(int records) {

        savedRecords.addAndGet(records);

    }

    public void addRemovedRecords(int records) {

        removedRecords.addAndGet(records);

    }

    public void recordIgnoredError(int lineNumber, Exception exception, Object dto) {

        ignoredRecords.incrementAndGet();

        if (ignoredErrorsLoggingEnabled) {
            log.warn(
                    "DATA_INTEGRATION_IGNORED_ERROR timestamp={} api={} method={} user={} ip={} entity={} operation={} line={} reason={} dto={}",
                    Instant.now(),
                    apiPath,
                    httpMethod,
                    username,
                    ipAddress,
                    entityClassName,
                    operationName,
                    lineNumber,
                    exception == null ? null : exception.getMessage(),
                    dto);
        }

    }

    public boolean isLifecycleLoggingEnabled() {

        return lifecycleLoggingEnabled;

    }

    public String getApiPath() {

        return apiPath;

    }

    public String getHttpMethod() {

        return httpMethod;

    }

    public String getUsername() {

        return username;

    }

    public String getIpAddress() {

        return ipAddress;

    }

    public String getEntityClassName() {

        return entityClassName;

    }

    public String getOperationName() {

        return operationName;

    }

    public Integer getTotalRecords() {

        return totalRecords;

    }

    public int getSavedRecords() {

        return savedRecords.get();

    }

    public int getRemovedRecords() {

        return removedRecords.get();

    }

    public int getIgnoredRecords() {

        return ignoredRecords.get();

    }

    public String getStatus() {

        return status;

    }

    public String getFailureMessage() {

        return failureMessage;

    }

    public long getDurationMs() {

        if (startedAt == null || finishedAt == null) {
            return 0L;
        }
        return Duration.between(startedAt, finishedAt).toMillis();

    }

}
