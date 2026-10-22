package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.capability.masterdata.product.material.integration.dto.ProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.UnidadeMedidaDataUploadDTO;
import com.opsfactor.community.capability.masterdata.product.material.integration.service.ProdutoIntegrationService;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service.UnidadeMedidaIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.MaterialIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.UnidadeMedidaIntegrationController;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import com.opsfactor.community.platform.scheduler.facade.WebControllerTaskSchedulingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real inherited Material and concrete UOM HTTP contracts; no server or database. */
class DataFormatJsonImportAdviceTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final AtomicReference<IntegrationDto<?, ?, ?, ?>> received = new AtomicReference<>();
    private MockMvc mvc;
    private MaterialIntegrationController controller;
    private ProdutoIntegrationService service;
    private WebControllerTaskSchedulingService scheduler;
    private static final String ROW = "{\"id\":\"000123\",\"description\":\"Bobina São Paulo\",\"defaultUomId\":\"TON\",\"active\":true}";

    @BeforeEach
    void configureHttp() throws Exception {
        controller = new MaterialIntegrationController();
        service = mock(ProdutoIntegrationService.class);
        AuthenticationService authentication = mock(AuthenticationService.class);
        when(authentication.currentUserHasAnyRole(anyList())).thenReturn(true);
        scheduler = mock(WebControllerTaskSchedulingService.class);
        when(scheduler.runImediatoSync(any(), anyString())).thenAnswer(invocation -> {
            ((Supplier<?>) invocation.getArgument(0)).get();
            return ResponseEntity.ok().build();
        });
        when(service.saveDTOList(any(IntegrationDto.class))).thenAnswer(invocation -> {
            received.set(invocation.getArgument(0));
            return "saved";
        });
        when(service.getFullDTOList()).thenReturn(List.of(mapper.readValue(ROW, ProdutoIntegrationDataDto.class)));
        ReflectionTestUtils.setField(controller, "authenticationService", authentication);
        ReflectionTestUtils.setField(controller, "integrationService", service);
        ReflectionTestUtils.setField(controller, "webControllerTaskSchedulingService", scheduler);
        ReflectionTestUtils.setField(controller, "objectMapper", mapper);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new DataFormatJsonImportAdvice())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
        RequestMappingHandlerMapping mapping = mvc.getDispatcherServlet().getWebApplicationContext()
                .getBean(RequestMappingHandlerMapping.class);
        ReflectionTestUtils.setField(controller, "handlerMapping", mapping);
        controller.configureMappings();
        mapping.registerMapping(RequestMappingInfo.paths("/outside/material").methods(RequestMethod.POST).build(),
                controller, IntegrationControllerAbstract.class.getMethod("saveIntegrationDto", IntegrationDto.class));
    }

    @Test
    void downloadedJsonCanBePostedUnchangedAndInheritedGenericsStayConcrete() throws Exception {
        byte[] downloaded = mvc.perform(get("/api/secured/data/material"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        for (String path : List.of("/api/secured/data/material")) {
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(downloaded)).andExpect(status().isOk());
            assertMaterial(received.get());
            assertNull(received.get().options);
            assertNull(received.get().threadSync);
        }
    }

    @Test
    void bomWhitespaceAndEmptyArraysUseTheSameNativeJacksonDtoBinding() throws Exception {
        mvc.perform(post("/api/secured/data/material").contentType(MediaType.APPLICATION_JSON)
                .content(("\uFEFF \r\n\t[" + ROW + "]").getBytes(StandardCharsets.UTF_8))).andExpect(status().isOk());
        assertMaterial(received.get());
        mvc.perform(post("/api/secured/data/material").contentType(MediaType.APPLICATION_JSON).content(" \n[]\t"))
                .andExpect(status().isOk());
        assertTrue(received.get().data.isEmpty());
    }

    @Test
    void existingEnvelopePreservesOptionsFieldPoliciesFiltersAndSyncMode() throws Exception {
        String envelope = "{\"data\":[" + ROW + "],\"options\":{\"skipRecordsWithUnresolvedDependencies\":true},"
                + "\"dataFilter\":{},\"dataFieldConfiguration\":{\"description\":\"Ignore\"},\"threadSync\":\"sync\"}";
        mvc.perform(post("/api/secured/data/material").contentType(MediaType.APPLICATION_JSON)
                .content(("\uFEFF \t" + envelope).getBytes(StandardCharsets.UTF_8))).andExpect(status().isOk());
        assertMaterial(received.get());
        assertTrue(mapper.valueToTree(received.get().options).get("skipRecordsWithUnresolvedDependencies").asBoolean());
        assertNotNull(received.get().dataFilter);
        assertEquals("IGNORE", received.get().dataFieldConfiguration.get("description").name());
        assertEquals(IntegrationDto.ThreadSync.SYNC, received.get().threadSync);
    }

    @Test
    void actualUnitOfMeasureListEndpointIsNotWrappedOrChanged() throws Exception {
        UnidadeMedidaIntegrationService uomService = mock(UnidadeMedidaIntegrationService.class);
        AtomicReference<List<UnidadeMedidaDataUploadDTO>> uoms = new AtomicReference<>();
        when(uomService.saveDTOList(anyList())).thenAnswer(invocation -> {
            uoms.set(invocation.getArgument(0));
            return "saved";
        });
        UnidadeMedidaIntegrationController uomController = new UnidadeMedidaIntegrationController();
        ReflectionTestUtils.setField(uomController, "unidadeMedidaIntegrationService", uomService);
        ReflectionTestUtils.setField(uomController, "webControllerTaskSchedulingService", scheduler);
        MockMvc uomMvc = MockMvcBuilders.standaloneSetup(uomController)
                .setControllerAdvice(new DataFormatJsonImportAdvice())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
        uomMvc.perform(post("/api/secured/data/unitofmeasure").contentType(MediaType.APPLICATION_JSON)
                .content("[{\"id\":\"TON\",\"description\":\"Tonelada\",\"delete\":\"D\"}]"))
                .andExpect(status().isOk());
        assertEquals("TON", uoms.get().get(0).id);
        assertEquals("D", uoms.get().get(0).delete);
    }

    @Test
    void malformedJsonAndWrongDtoTypesStillReturnNativeBadRequest() throws Exception {
        for (String malformed : List.of("[{", "[{\"id\":{\"invalid\":1}}]", "\"not a list\"")) {
            mvc.perform(post("/api/secured/data/material").contentType(MediaType.APPLICATION_JSON).content(malformed))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertInstanceOf(HttpMessageNotReadableException.class, result.getResolvedException()));
        }
        verify(service, never()).saveDTOList(any(IntegrationDto.class));
        mvc.perform(post("/outside/material").contentType(MediaType.APPLICATION_JSON).content("[" + ROW + "]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrappingIsStreamingAndDoesNotLeaveAnIncorrectContentLength() throws Exception {
        byte[] payload = ("[" + ROW.replace("Bobina São Paulo", "x".repeat(1_000_000)) + "]").getBytes(StandardCharsets.UTF_8);
        AtomicInteger readBytes = new AtomicInteger();
        ByteArrayInputStream source = new ByteArrayInputStream(payload) {
            @Override public synchronized int read() {
                int value = super.read();
                if (value != -1) readBytes.incrementAndGet();
                return value;
            }
            @Override public synchronized int read(byte[] buffer, int offset, int length) {
                int count = super.read(buffer, offset, length);
                if (count > 0) readBytes.addAndGet(count);
                return count;
            }
        };
        MockHttpInputMessage input = new MockHttpInputMessage(source);
        input.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        input.getHeaders().setContentLength(payload.length);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/secured/data/material");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            MethodParameter parameter = new MethodParameter(IntegrationControllerAbstract.class
                    .getMethod("saveIntegrationDto", IntegrationDto.class), 0).withContainingClass(MaterialIntegrationController.class);
            var advice = new DataFormatJsonImportAdvice();
            assertTrue(advice.supports(parameter, parameter.getGenericParameterType(), MappingJackson2HttpMessageConverter.class));
            var adapted = advice.beforeBodyRead(input, parameter, parameter.getGenericParameterType(), MappingJackson2HttpMessageConverter.class);
            assertTrue(readBytes.get() <= 4, "Advice must peek only the root, not buffer the JSON document");
            assertNull(adapted.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH));
            assertEquals(payload.length, input.getHeaders().getContentLength());
            byte[] prefix = adapted.getBody().readNBytes(8);
            assertEquals("{\"data\":", new String(prefix, StandardCharsets.UTF_8));
            assertTrue(readBytes.get() <= 4);
            var decoded = mapper.readTree(new SequenceInputStream(new ByteArrayInputStream(prefix), adapted.getBody()));
            assertTrue(decoded.get("data").isArray());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private void assertMaterial(IntegrationDto<?, ?, ?, ?> body) {
        assertNotNull(body);
        assertEquals(1, body.data.size());
        ProdutoIntegrationDataDto row = assertInstanceOf(ProdutoIntegrationDataDto.class, body.data.get(0));
        assertEquals("000123", row.primaryKeyDto.id);
        assertEquals("Bobina São Paulo", row.description);
        assertEquals("TON", row.defaultUomId);
        assertTrue(row.active);
    }
}
