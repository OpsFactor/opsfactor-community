package com.opsfactor.community.web.configuration.dataformat;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Makes existing tabular exports available on the canonical data namespace with
 * explicit format. Source aliases retain their original no-format JSON contract.
 * This includes plan exports outside the generic integration controller.
 */
@Component
public final class DataFormatLegacyExportAliases implements SmartInitializingSingleton {

    private final ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider;
    private final DataFormatPolicy policy;

    public DataFormatLegacyExportAliases(
            @Qualifier("requestMappingHandlerMapping") ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider) {

        this(handlerMappingProvider, new DataFormatPolicy());

    }

    @Autowired
    public DataFormatLegacyExportAliases(
            @Qualifier("requestMappingHandlerMapping") ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider,
            DataFormatPolicy policy) {

        this.handlerMappingProvider = handlerMappingProvider;
        this.policy = policy;

    }

    @Override
    public void afterSingletonsInstantiated() {

        registerAliases(handlerMappingProvider.getObject(), policy);

    }

    static void registerAliases(RequestMappingHandlerMapping handlerMapping) {

        registerAliases(handlerMapping, new DataFormatPolicy());

    }

    static void registerAliases(RequestMappingHandlerMapping handlerMapping, DataFormatPolicy policy) {

        List<Map.Entry<RequestMappingInfo, HandlerMethod>> originalMappings = new ArrayList<>(handlerMapping.getHandlerMethods().entrySet());
        // Older concrete controllers predate the abstract base. Publish their DTO
        // methods under the canonical namespace, retaining the exact body contract,
        // authorization annotations, HTTP verbs and old paths.
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : originalMappings) {
            for (String path : entry.getKey().getPatternValues()) {
                String normalized = path.startsWith("/") ? path : "/" + path;
                if (!normalized.startsWith("/api/secured/dataupload/json/")) {
                    continue;
                }
                String canonical = normalized.replace("/api/secured/dataupload/json/", "/api/secured/data/");
                RequestMappingInfo alias = entry.getKey().mutate().paths(canonical).build();
                if (!hasEquivalentMapping(handlerMapping, alias, canonical)) {
                    handlerMapping.registerMapping(alias, entry.getValue().getBean(), entry.getValue().getMethod());
                }
            }
        }
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : originalMappings) {
            RequestMappingInfo source = entry.getKey();
            HandlerMethod handler = entry.getValue();
            if (!source.getMethodsCondition().getMethods().contains(RequestMethod.GET)
                    || !source.getParamsCondition().isEmpty()
                    || !isTabularExport(handler)) {
                continue;
            }
            for (String sourcePath : source.getPatternValues()) {
                String canonicalPath = canonicalExportPath(sourcePath);
                if (canonicalPath == null || canonicalPath.equals(sourcePath)) {
                    continue;
                }
                // If a typed DTO handler already exists, it owns JSON/Arrow/Parquet.
                // Only spreadsheet representations reuse the mapper-owned template.
                boolean hasCanonicalHandler = handlerMapping.getHandlerMethods().keySet().stream().anyMatch(candidate ->
                        candidate.getPatternValues().contains(canonicalPath)
                                && candidate.getMethodsCondition().getMethods().contains(RequestMethod.GET)
                                && candidate.getParamsCondition().isEmpty());
                List<String> formats = hasCanonicalHandler ? List.of("xlsx", "csv")
                        : List.of("json", "arrow", "parquet", "xlsx", "csv");
                if (!hasCanonicalHandler) {
                    RequestMappingInfo defaultJson = RequestMappingInfo.paths(canonicalPath)
                            .methods(RequestMethod.GET).options(handlerMapping.getBuilderConfiguration()).build();
                    if (!hasEquivalentMapping(handlerMapping, defaultJson, canonicalPath)) {
                        handlerMapping.registerMapping(defaultJson, handler.getBean(), handler.getMethod());
                    }
                }
                for (String format : formats) {
                    if (!policy.supports(DataFormat.fromParameter(format))) continue;
                    RequestMappingInfo alias = RequestMappingInfo.paths(canonicalPath)
                            .methods(RequestMethod.GET).params("format=" + format)
                            .options(handlerMapping.getBuilderConfiguration()).build();
                    if (!hasEquivalentMapping(handlerMapping, alias, canonicalPath)) {
                        handlerMapping.registerMapping(alias, handler.getBean(), handler.getMethod());
                    }
                }
            }
        }

        // Async receipts must win over format-only aliases. Preserve the async
        // condition when normalizing file routes, then register the two-parameter
        // combinations so format=xlsx cannot select the direct extraction handler.
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : originalMappings) {
            RequestMappingInfo source = entry.getKey();
            if (!source.getMethodsCondition().getMethods().contains(RequestMethod.GET)
                    || source.getParamsCondition().getExpressions().stream()
                    .noneMatch(expression -> expression.toString().equals("threadSync=async"))) {
                continue;
            }
            for (String sourcePath : source.getPatternValues()) {
                String canonicalPath = canonicalExportPath(sourcePath);
                if (canonicalPath == null && sourcePath.startsWith("/api/secured/data/")) canonicalPath = sourcePath;
                if (canonicalPath == null) continue;
                RequestMappingInfo background = source.mutate().paths(canonicalPath).params("threadSync=async").build();
                if (!hasEquivalentMapping(handlerMapping, background, canonicalPath)) {
                    handlerMapping.registerMapping(background, entry.getValue().getBean(), entry.getValue().getMethod());
                }
                for (DataFormat format : DataFormat.values()) {
                    if (!policy.supports(format)) continue;
                    RequestMappingInfo alias = source.mutate().paths(canonicalPath)
                            .params("threadSync=async", "format=" + format.extension()).build();
                    if (!hasEquivalentMapping(handlerMapping, alias, canonicalPath)) {
                        handlerMapping.registerMapping(alias, entry.getValue().getBean(), entry.getValue().getMethod());
                    }
                }
            }
        }

    }

    private static boolean hasEquivalentMapping(RequestMappingHandlerMapping mapping,
            RequestMappingInfo proposed, String path) {

        // A mapping can contain multiple URL patterns. Comparing the entire map key
        // would miss an already registered single path inside a multi-path handler.
        return mapping.getHandlerMethods().keySet().stream().anyMatch(existing ->
                existing.getPatternValues().contains(path)
                        && existing.getMethodsCondition().equals(proposed.getMethodsCondition())
                        && existing.getParamsCondition().equals(proposed.getParamsCondition())
                        && existing.getHeadersCondition().equals(proposed.getHeadersCondition())
                        && existing.getConsumesCondition().equals(proposed.getConsumesCondition())
                        && existing.getProducesCondition().equals(proposed.getProducesCondition()));

    }

    private static boolean isTabularExport(HandlerMethod handler) {

        String methodName = handler.getMethod().getName().toLowerCase(java.util.Locale.ROOT);
        // Legacy endpoints with raw ResponseEntity are recognized by their explicit
        // File export method name, never by every arbitrary Data GET response.
        Class<?> returnedClass = handler.getMethod().getReturnType();
        return (java.util.Collection.class.isAssignableFrom(returnedClass)
                || org.springframework.http.ResponseEntity.class.isAssignableFrom(returnedClass))
                && (methodName.contains("file") || methodName.contains("tabular"));

    }

    static String canonicalExportPath(String sourcePath) {

        String normalized = sourcePath.startsWith("/") ? sourcePath : "/" + sourcePath;
        String suffix;
        if (normalized.startsWith("/api/secured/data/file/")) {
            suffix = normalized.substring("/api/secured/data/file/".length());
        } else if (normalized.startsWith("/api/secured/dataupload/")
                && !normalized.startsWith("/api/secured/dataupload/json/")) {
            suffix = normalized.substring("/api/secured/dataupload/".length());
        } else {
            return null;
        }
        suffix = suffix.replace("/new/file/", "/");
        return "/api/secured/data/" + suffix;

    }
}
