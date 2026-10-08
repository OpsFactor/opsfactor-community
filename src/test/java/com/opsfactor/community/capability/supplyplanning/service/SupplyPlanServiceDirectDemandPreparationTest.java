package com.opsfactor.community.capability.supplyplanning.service;

import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.DemandaDiretaConsideradaProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.calendar.PerfilCalendarioSimplesSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionPorDfu;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;

/** Verifies the read-only preparation and the edition persistence hook remain independent. */
class SupplyPlanServiceDirectDemandPreparationTest {

    @Test
    void transientPlanCanBePreparedWithoutPersistence() {

        SupplyPlanService service = spy(new SupplyPlanService());
        SupplyPlan plan = new SupplyPlan();
        PerfilExecucaoSupplyPlan profile = new PerfilExecucaoSupplyPlan();
        plan.setPerfilExecucaoSupplyPlan(profile);
        plan.setDataInicioPlano(LocalDateTime.of(2027, 1, 1, 0, 0));
        plan.setPerfilCalendarioSupplyPlan(new PerfilCalendarioSimplesSupplyPlan(
                plan, Constantes.TamanhoBucket.ANUAL, 1));
        ClusterEParametrosProjection parameters = mock(ClusterEParametrosProjection.class);
        when(parameters.getParametrosGlobais()).thenReturn(new ParametrosGlobais());
        when(parameters.getLocationsAtivas()).thenReturn(Set.of());
        SupplyNetworkProjection network = mock(SupplyNetworkProjection.class);
        when(network.getClusterEParametrosProjection()).thenReturn(parameters);
        LocationProjection locations = mock(LocationProjection.class);
        MaterialProjection materials = mock(MaterialProjection.class);
        try (var locationFactory = mockStatic(LocationProjectionFactory.class);
             var materialFactory = mockStatic(MaterialProjectionFactory.class)) {
            locationFactory.when(() -> LocationProjectionFactory.getLocationProjectionDePerfilExecucaoSupplyPlan(profile, parameters))
                    .thenReturn(locations);
            materialFactory.when(() -> MaterialProjectionFactory.getMaterialProjectionDePerfilExecucaoSupplyPlan(profile, parameters))
                    .thenReturn(materials);
            DemandaDiretaConsideradaProjection prepared = service.preparaDemandaDiretaConsiderada(
                    plan, profile, mock(DemandPlanningProjection.class), mock(SplitTemporalProjectionPorDfu.class),
                    mock(PoliticaEstoquesProjection.class), network, new UnidadeMedidaProjection());
            assertNull(plan.getId());
            assertTrue(prepared.getAllDemandaDiretaConsideradaLinha().isEmpty());
            assertEquals(LocalDateTime.of(2027, 12, 31, 23, 59, 59), prepared.getCalendario().getDataHorarioFinal());
            verify(service, never()).saveDemandaDiretaConsideradaLinhaCollection(any(), anyBoolean());
        }

    }

    @Test
    void persistedExecutionUsesPreparationAndEditionBatchHook() {

        SupplyPlanService service = mock(SupplyPlanService.class, CALLS_REAL_METHODS);
        SupplyPlan plan = new SupplyPlan();
        plan.setId(42L);
        DemandaDiretaConsideradaProjection prepared = mock(DemandaDiretaConsideradaProjection.class);
        List<DemandaDiretaConsideradaLinha> rows = List.of(mock(DemandaDiretaConsideradaLinha.class));
        when(prepared.getAllDemandaDiretaConsideradaLinha()).thenReturn(rows);
        doReturn(prepared).when(service).preparaDemandaDiretaConsiderada(plan, null, null, null, null, null, null);
        doNothing().when(service).saveDemandaDiretaConsideradaLinhaCollection(rows, false);
        service.atualizaSupplyPlanComDemandaDiretaConsiderada(plan, null, null, null, null, null, null);
        verify(service).saveDemandaDiretaConsideradaLinhaCollection(rows, false);
        plan.setId(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.atualizaSupplyPlanComDemandaDiretaConsiderada(plan, null, null, null, null, null, null));
        verify(service, times(1)).preparaDemandaDiretaConsiderada(plan, null, null, null, null, null, null);

    }

}
