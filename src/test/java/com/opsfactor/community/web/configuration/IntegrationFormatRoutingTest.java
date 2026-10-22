package com.opsfactor.community.web.configuration;

import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import com.opsfactor.community.web.restcontroller.dataupload.transactionaldata.EstoqueIntegrationController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * Verifica o despacho real do Spring para uma integracao generica: os
 * formatos de planilha conservam o template e os demais usam o DTO tipado.
 * Nao inicia servidor, banco ou servicos de dominio.
 */
class IntegrationFormatRoutingTest {

    @Test
    void shouldPreserveTypedDefaultAndChooseTemplateOnlyForSpreadsheetFormats() throws Exception {

        try (StaticWebApplicationContext context = new StaticWebApplicationContext()) {
            RequestMappingHandlerMapping mapping = createMappings(context);

            Assertions.assertEquals("getDataRecordDtoList", handler(mapping, "GET", "/api/secured/data/stock", null));
            for (String format : List.of("json", "arrow", "parquet")) {
                Assertions.assertEquals("getDataRecordDtoList", handler(mapping, "GET", "/api/secured/data/stock", format));
            }
            for (String format : List.of("xlsx", "csv")) {
                Assertions.assertEquals("getFile", handler(mapping, "GET", "/api/secured/data/stock", format));
            }
            Assertions.assertEquals("getFile", handler(mapping, "GET", "/api/secured/data/file/stock", null));
            for (String contentType : List.of("application/json", "application/vnd.apache.arrow.stream", "application/vnd.apache.parquet")) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/secured/data/stock");
                request.setContentType(contentType);
                Assertions.assertEquals("saveIntegrationDto", resolveHandler(mapping, request));
            }
            Assertions.assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/api/secured/dataupload/json/stock")));
        }

    }

    @Test
    void shouldDocumentFormatsOnCanonicalGetWithoutChangingSaveConfirmation() throws Exception {

        try (StaticWebApplicationContext context = new StaticWebApplicationContext()) {
            RequestMappingHandlerMapping mapping = createMappings(context);
            OpenAPI openApi = new OpenAPI();
            new IntegrationOpenApiConfiguration().integrationControllersOpenApiCustomizer(mapping).customise(openApi);

            Operation get = openApi.getPaths().get("/api/secured/data/stock").getGet();
            Assertions.assertEquals(
                    Set.of("application/json", "application/vnd.apache.arrow.stream", "application/vnd.apache.parquet",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv"),
                    get.getResponses().get("200").getContent().keySet());
            Assertions.assertEquals(List.of("format", "fieldSeparator", "decimalSeparator"),
                    get.getParameters().stream().map(parameter -> parameter.getName()).toList());
            Assertions.assertEquals("json", get.getParameters().get(0).getSchema().getDefault());
            Assertions.assertEquals(List.of("json", "arrow", "parquet", "xlsx", "csv"),
                    get.getParameters().get(0).getSchema().getEnum());

            Operation post = openApi.getPaths().get("/api/secured/data/stock").getPost();
            Assertions.assertEquals(
                    Set.of("application/json", "application/vnd.apache.arrow.stream", "application/vnd.apache.parquet"),
                    post.getRequestBody().getContent().keySet());
            Assertions.assertEquals(Set.of("application/json"), post.getResponses().get("200").getContent().keySet());
            Assertions.assertTrue(post.getParameters() == null || post.getParameters().isEmpty());
            Assertions.assertEquals(Set.of("application/json"),
                    openApi.getPaths().get("/api/secured/data/file/stock").getGet()
                            .getResponses().get("200").getContent().keySet());
        }

    }

    private RequestMappingHandlerMapping createMappings(StaticWebApplicationContext context) throws Exception {

        RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
        mapping.setApplicationContext(context);
        mapping.afterPropertiesSet();

        EstoqueIntegrationController controller = new EstoqueIntegrationController();
        Field handlerMappingField = IntegrationControllerAbstract.class.getDeclaredField("handlerMapping");
        handlerMappingField.setAccessible(true);
        handlerMappingField.set(controller, mapping);
        controller.configureMappings();
        return mapping;

    }

    private String handler(RequestMappingHandlerMapping mapping, String method, String path, String format) throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (format != null) {
            request.addParameter("format", format);
        }
        return resolveHandler(mapping, request);

    }

    private String resolveHandler(RequestMappingHandlerMapping mapping, MockHttpServletRequest request) throws Exception {

        HandlerExecutionChain chain = mapping.getHandler(request);
        Assertions.assertNotNull(chain, "A rota existente deve continuar registrada");
        return ((HandlerMethod) chain.getHandler()).getMethod().getName();

    }

}
