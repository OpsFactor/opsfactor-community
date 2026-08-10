package com.opsfactor.community.mcp.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.configuration.facade.ConfiguredViewFacade;
import com.opsfactor.community.capability.configuration.facade.PerfilExecucaoDemandPlanFacade;
import com.opsfactor.community.capability.configuration.facade.PerfilExecucaoSupplyPlanFacade;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.DemandPlanningFacade;
import com.opsfactor.community.capability.demandplanning.facade.DemandSimulationFacade;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service.UnidadeMedidaIntegrationService;
import com.opsfactor.community.capability.masterdata.product.material.integration.service.ProdutoIntegrationService;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.service.InventoryPlanIntegrationService;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.SupplyPlanFacade;
import com.opsfactor.community.capability.supplyplanning.supplyplan.integration.service.FulfilledDemandIntegrationService;
import com.opsfactor.community.platform.bi.facade.CommunityMaterialFlowsService;
import com.opsfactor.community.platform.scheduler.facade.WebControllerTaskSchedulingService;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Valida que o registry resolve os generics do service de integracao concreto
 * e o chama diretamente, sem depender do controller REST correspondente.
 */
class CommunityMcpCapabilityRegistryTest {

    @Test
    void shouldResolveAndQueryCanonicalIntegrationService() {

        ProdutoIntegrationService produtoIntegrationService = mock(ProdutoIntegrationService.class);
        when(produtoIntegrationService.getFullDTOList()).thenReturn(List.of());

        CommunityMcpCapabilityRegistry registry = new CommunityMcpCapabilityRegistry(
                new ObjectMapper(),
                List.of(produtoIntegrationService),
                mock(UnidadeMedidaIntegrationService.class),
                mock(InventoryPlanIntegrationService.class),
                mock(FulfilledDemandIntegrationService.class),
                mock(PerfilExecucaoDemandPlanFacade.class),
                mock(PerfilExecucaoSupplyPlanFacade.class),
                mock(DemandSimulationFacade.class),
                mock(CommunityMaterialFlowsService.class),
                mock(DemandPlanningFacade.class),
                mock(SupplyPlanFacade.class),
                mock(ConfiguredViewFacade.class),
                mock(AuthenticationService.class),
                mock(WebControllerTaskSchedulingService.class));

        assertThat(registry.query("data.master-data.material", null).isArray()).isTrue();
        assertThat(registry.query("data.master-data.material", null)).isEmpty();

    }

}
