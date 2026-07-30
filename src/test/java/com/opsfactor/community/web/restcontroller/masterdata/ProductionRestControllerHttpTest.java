package com.opsfactor.community.web.restcontroller.masterdata;

import com.opsfactor.community.capability.masterdata.production.productionresource.facade.dto.RecursoProdutivoDTO;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.ListaTecnicaFacade;
import com.opsfactor.community.capability.masterdata.production.productionresource.facade.RecursoProdutivoFacade;
import com.opsfactor.community.capability.masterdata.production.routing.facade.RoteiroFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova HTTP do cadastro operacional de recursos produtivos Community.
 *
 * <p>O teste atravessa Jackson e o controller real antes de capturar o DTO
 * entregue ao service. Assim, o contrato JSON usado pela SPA não depende de
 * uma suposição sobre o construtor gerado por Lombok.</p>
 */
class ProductionRestControllerHttpTest {

    private MockMvc mockMvc;

    private RecursoProdutivoFacade recursoProdutivoFrontService;

    @BeforeEach
    void setUp() {

        ProductionRestController productionRestController = new ProductionRestController();
        recursoProdutivoFrontService = mock(RecursoProdutivoFacade.class);
        ReflectionTestUtils.setField(
                productionRestController,
                "recursoProdutivoFrontService",
                recursoProdutivoFrontService);
        ReflectionTestUtils.setField(
                productionRestController,
                "roteiroFrontService",
                mock(RoteiroFacade.class));
        ReflectionTestUtils.setField(
                productionRestController,
                "listaTecnicaFrontService",
                mock(ListaTecnicaFacade.class));
        mockMvc = MockMvcBuilders.standaloneSetup(productionRestController).build();

    }

    @Test
    void saveProductionResourceShouldDeserializeCanonicalJsonAndDelegateToFrontService() throws Exception {

        String productionResourcePayload = """
                {
                  "productionResourceId": "resource-01",
                  "locationId": "plant-01",
                  "description": "Assembly resource",
                  "active": true,
                  "efficiency": 0.875
                }
                """;

        mockMvc.perform(post("/api/secured/production/productionresource/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productionResourcePayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Production Resource Saved"));

        ArgumentCaptor<RecursoProdutivoDTO> recursoProdutivoDTOCaptor =
                ArgumentCaptor.forClass(RecursoProdutivoDTO.class);
        verify(recursoProdutivoFrontService).saveRecursoProdutivoDTO(recursoProdutivoDTOCaptor.capture());

        RecursoProdutivoDTO recursoProdutivoDTO = recursoProdutivoDTOCaptor.getValue();
        assertEquals("resource-01", recursoProdutivoDTO.productionResourceId);
        assertEquals("plant-01", recursoProdutivoDTO.locationId);
        assertEquals("Assembly resource", recursoProdutivoDTO.description);
        assertEquals(true, recursoProdutivoDTO.active);
        assertEquals(0.875F, recursoProdutivoDTO.efficiency);

    }

}
