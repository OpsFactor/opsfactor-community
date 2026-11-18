package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import com.opsfactor.community.web.restcontroller.dataupload.transactionaldata.EstoqueIntegrationController;
import com.opsfactor.community.web.configuration.IntegrationOpenApiConfiguration;
import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Runs the production Community MVC configuration without an Enterprise policy or Azure context. */
class CommunityEditionHttpBoundaryTest {

    @Test
    void publishedOpenApiAdvertisesOnlyTheFormatsAcceptedByTheCommunityRuntime() throws Exception {

        var controller = new EstoqueIntegrationController();
        var mapping = mock(RequestMappingHandlerMapping.class);
        String path = "/api/secured/data/stock";
        when(mapping.getHandlerMethods()).thenReturn(Map.of(
                RequestMappingInfo.paths(path).methods(RequestMethod.GET).build(),
                new HandlerMethod(controller, controller.getClass().getMethod("getDataRecordDtoList")),
                RequestMappingInfo.paths(path).methods(RequestMethod.POST).build(),
                new HandlerMethod(controller, controller.getClass().getMethod("saveIntegrationDto", IntegrationDto.class))));
        var openApi = new OpenAPI();
        new IntegrationOpenApiConfiguration().integrationControllersOpenApiCustomizer(mapping).customise(openApi);
        var query = openApi.getPaths().get(path).getGet();
        var format = query.getParameters().stream().filter(parameter -> parameter.getName().equals("format")).findFirst().orElseThrow();
        assertEquals(Set.of("json", "csv", "xlsx"), Set.copyOf(format.getSchema().getEnum()));
        var responses = query.getResponses().get("200").getContent();
        assertFalse(responses.containsKey(DataFormat.ARROW.mediaType()));
        assertFalse(responses.containsKey(DataFormat.PARQUET.mediaType()));
        assertEquals(Set.of("application/json"), openApi.getPaths().get(path).getPost().getRequestBody().getContent().keySet());

    }

    @Test
    void communityAllowsDirectCsvXlsxAndNormalJsonButRejectsColumnarDownloadsAndUploads() throws Exception {

        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(CommunityMvc.class);
            context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            for (String format : List.of("csv", "xlsx")) {
                mvc.perform(get("/api/secured/data/edition").param("format", format))
                        .andExpect(status().isOk()).andExpect(header().exists("Content-Disposition"));
            }
            mvc.perform(get("/api/secured/data/edition"))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(header().doesNotExist("Content-Disposition"));
            mvc.perform(post("/api/secured/data/edition").contentType("application/json").content("[{\"id\":\"001\"}]"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("001"));
            for (String format : List.of("arrow", "parquet")) {
                mvc.perform(get("/api/secured/data/edition").param("format", format)).andExpect(status().isNotAcceptable());
                mvc.perform(get("/api/secured/data/file/edition").param("format", format)).andExpect(status().isNotAcceptable());
                mvc.perform(post("/api/secured/data/edition").contentType(DataFormat.fromParameter(format).mediaType()).content(new byte[]{1, 2, 3}))
                        .andExpect(status().isUnsupportedMediaType());
            }
            var error = mvc.perform(get("/api/secured/data/hierarchical").param("format", "xlsx"))
                    .andExpect(status().isNotAcceptable()).andExpect(jsonPath("$.code").value("HIERARCHICAL_EXPORT_NOT_SUPPORTED"))
                    .andReturn().getResponse().getContentAsString();
            assertFalse(error.toLowerCase(Locale.ROOT).contains("arrow"));
            assertFalse(error.toLowerCase(Locale.ROOT).contains("parquet"));
        }

    }

    @Test
    void multipartFileBoundaryRejectsNonCsvXlsxBeforeTheFallbackParserOrScheduler() {

        var authentication = mock(AuthenticationService.class);
        when(authentication.currentUserHasAnyRole(anyCollection())).thenReturn(true);
        var controller = new EstoqueIntegrationController();
        ReflectionTestUtils.setField(controller, "authenticationService", authentication);
        for (String extension : List.of("arrow", "parquet", "json", "xls", "xlsm")) {
            var file = new MockMultipartFile("file", "input." + extension, "application/octet-stream", new byte[]{1});
            assertEquals(415, assertThrows(ResponseStatusException.class, () -> controller.saveFile(file)).getStatusCode().value());
        }

    }

    @Test
    void communityComponentInventoryHasNoAtpOrPersistedReportExecutionControllers() {

        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        var names = scanner.findCandidateComponents("com.opsfactor.community").stream()
                .map(bean -> bean.getBeanClassName().toLowerCase(Locale.ROOT)).toList();
        assertFalse(names.isEmpty());
        assertTrue(names.stream().noneMatch(name -> name.contains("atp") || name.contains("relatorioexecucao")
                || name.contains("dataexportscheduling") || name.contains("savedreport")));
        assertFalse(org.springframework.util.ClassUtils.isPresent(
                "com.opsfactor.enterprise.platform.reports.RelatorioBlobStorage", getClass().getClassLoader()));

    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({DataFormatPolicy.class, DataFormatWebConfiguration.class, DataFormatLegacyExportAliases.class, DataFormatExceptionHandler.class})
    static class CommunityMvc {
        @Bean ObjectMapper mapper() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean FixtureController fixture() { return new FixtureController(); }
    }

    @RestController
    static class FixtureController {
        @GetMapping("/api/secured/data/file/edition")
        public List<Row> getFile() { return List.of(new Row("001")); }
        @PostMapping("/api/secured/data/edition")
        public List<Row> importData(@RequestBody List<Row> rows) { return rows; }
        @GetMapping("/api/secured/data/hierarchical")
        public List<Nested> hierarchy() { return List.of(new Nested(new Row("001"))); }
    }

    record Row(String id) { }
    record Nested(Row child) { }

}
