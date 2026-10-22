package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Compares old and negotiated HTTP pipelines without contacting an integration or database. */
class DataFormatCompatibilityTest {

    @Test
    void noFormatPreservesPostStatusHeadersAndBodyForAllObservedInboundPaths() throws Exception {

        MockMvc original = mvc(false);
        MockMvc negotiated = mvc(true);
        for (String path : paths()) {
            for (String accept : List.of("", "*/*", "application/json")) {
                var oldRequest = post(path).contentType(MediaType.APPLICATION_JSON).content("{\"data\":[{\"id\":\"000123\"}]}");
                var newRequest = post(path).contentType(MediaType.APPLICATION_JSON).content("{\"data\":[{\"id\":\"000123\"}]}");
                if (!accept.isEmpty()) {
                    oldRequest.header("Accept", accept);
                    newRequest.header("Accept", accept);
                }
                MockHttpServletResponse before = original.perform(oldRequest).andReturn().getResponse();
                MockHttpServletResponse after = negotiated.perform(newRequest).andReturn().getResponse();
                assertEquals(before.getStatus(), after.getStatus(), path);
                assertEquals(before.getContentType(), after.getContentType(), path + " Accept=" + accept);
                assertArrayEquals(before.getContentAsByteArray(), after.getContentAsByteArray(), path);
                assertEquals(before.getHeader("X-Import-Result"), after.getHeader("X-Import-Result"), path);
            }
        }

    }

    @Test
    void noFormatPreservesJsonExtractionShape() throws Exception {

        MockMvc original = mvc(false);
        MockMvc negotiated = mvc(true);
        for (String path : paths()) {
            MockHttpServletResponse before = original.perform(get(path)).andReturn().getResponse();
            MockHttpServletResponse after = negotiated.perform(get(path)).andReturn().getResponse();
            assertEquals(before.getContentType(), after.getContentType(), path);
            assertArrayEquals(before.getContentAsByteArray(), after.getContentAsByteArray(), path);
        }

    }

    private MockMvc mvc(boolean negotiated) {

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var builder = MockMvcBuilders.standaloneSetup(new ContractFixture());
        if (negotiated) {
            builder.setContentNegotiationManager(new ContentNegotiationManager(
                    new DataFormatNegotiationStrategy(), new HeaderContentNegotiationStrategy()));
            builder.setMessageConverters(new DataFormatHttpMessageConverter(mapper),
                    new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter(mapper));
        } else {
            // Matches Spring's standard relative converter order: String precedes Jackson.
            builder.setMessageConverters(new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter(mapper));
        }
        return builder.build();

    }

    private static List<String> paths() {

        return List.of("/api/secured/dataupload/json/material", "/api/secured/dataupload/json/location",
                "/api/secured/data/location", "/api/secured/data/sellin",
                "/api/secured/data/transportationlanematerial", "/api/secured/data/inventorypolicydetail",
                "/api/secured/data/materiallocationparameters", "/api/secured/data/productsuccession");

    }

    @RestController
    static class ContractFixture {

        @PostMapping({"/api/secured/dataupload/json/material", "/api/secured/dataupload/json/location",
                "/api/secured/data/location", "/api/secured/data/sellin",
                "/api/secured/data/transportationlanematerial", "/api/secured/data/inventorypolicydetail",
                "/api/secured/data/materiallocationparameters", "/api/secured/data/productsuccession"})
        public ResponseEntity<String> save(@RequestBody Map<String, Object> request) {

            assertTrue(request.containsKey("data"));
            return ResponseEntity.accepted().header("X-Import-Result", "preserved").body("Data saved successfully");

        }

        @GetMapping({"/api/secured/dataupload/json/material", "/api/secured/dataupload/json/location",
                "/api/secured/data/location", "/api/secured/data/sellin",
                "/api/secured/data/transportationlanematerial", "/api/secured/data/inventorypolicydetail",
                "/api/secured/data/materiallocationparameters", "/api/secured/data/productsuccession"})
        public List<Map<String, Object>> query() {

            return List.of(Map.of("id", "000123", "quantity", 5));

        }
    }
}
