package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendarioSimples;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionLocationProduto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.configuration.repository.PerfilExecucaoSupplyPlanRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto.UnidadeConversaoFaltanteDTO;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifica o alerta público com demanda somente no último período do horizonte. */
class UnidadeMedidaFacadeSupplyCalendarTest {

    @Test
    void detectsMissingConversionInSingleYearlyBucket() {

        assertLastBucketConversionIsReported(1);

    }

    @Test
    void detectsMissingConversionInLastBucketOfLongerHorizon() {

        assertLastBucketConversionIsReported(2);

    }

    private void assertLastBucketConversionIsReported(int numberOfPeriods) {

        LocalDateTime reference = LocalDateTime.of(2035, 1, 1, 0, 0);
        CalendarioSimples calendar = CalendarioSimples.criaCalendarioDeOffsetsPeriodos(
                Constantes.TamanhoBucket.ANUAL, reference, 0, 0, numberOfPeriods, 0);
        ParametrosGlobais parameters = new ParametrosGlobais();
        Location location = mock(Location.class);
        Location transferDestination = new Location("TARGET_SITE");
        UnidadeMedida transferLotUnit = new UnidadeMedida("LOT");
        Produto material = mock(Produto.class);
        UnidadeMedida origin = new UnidadeMedida("BOX");
        UnidadeMedida target = new UnidadeMedida("KG");
        DemandPlanItem demandLine = mock(DemandPlanItem.class);
        DemandPlan demandPlan = mock(DemandPlan.class);
        VersaoMalha networkVersion = mock(VersaoMalha.class);
        PerfilExecucaoSupplyPlan profile = mock(PerfilExecucaoSupplyPlan.class);
        ClusterEParametrosProjection cluster = mock(ClusterEParametrosProjection.class);
        SupplyNetworkProjection network = mock(SupplyNetworkProjection.class);
        DemandPlanningProjection demand = mock(DemandPlanningProjection.class);
        UnidadeMedidaProjection units = mock(UnidadeMedidaProjection.class);
        EstoqueProjectionLocationProduto stock = mock(EstoqueProjectionLocationProduto.class);

        when(networkVersion.getId()).thenReturn("NETWORK");
        when(demandPlan.getId()).thenReturn(1L);
        when(profile.getId()).thenReturn("PROFILE");
        when(location.getId()).thenReturn("SITE");
        when(material.getId()).thenReturn("MATERIAL");
        when(profile.getPerfilCalendario()).thenReturn(
                new PerfilCalendarioSimples("CALENDAR", Constantes.TamanhoBucket.ANUAL, numberOfPeriods));
        parameters.setUnidadeMedidaPadraoSNP(target);
        when(cluster.getParametrosGlobais()).thenReturn(parameters);
        when(cluster.getLocationSet()).thenReturn(Set.of(location));
        when(cluster.getMaterialSet()).thenReturn(Set.of(material));
        when(cluster.getMateriaisAtivosEmLocation(location)).thenReturn(Set.of(material));
        when(cluster.isDfuAtiva(material, location)).thenReturn(true);
        when(cluster.getSNPUnidadeMedidaPadrao(material, location)).thenReturn(target);
        when(network.getRoteirosViaveis(location, material)).thenReturn(Collections.emptySet());
        when(network.getListasTecnicasViaveis(location, material, null)).thenReturn(Collections.emptySet());
        // Transfer multiples are expressed in their own lane UOM; the report must name that need.
        when(network.getLocationDestinoViavelSet(networkVersion, location, material, reference, null))
                .thenReturn(Set.of(transferDestination));
        when(cluster.getSNPUnidadeMedidaPadrao(material, transferDestination)).thenReturn(target);
        when(network.getUnidadeMedidaLoteMinimoMultiploTransporte(networkVersion, location, transferDestination, material, reference))
                .thenReturn(transferLotUnit);
        when(units.contemConversaoParaUnidadeDestino(material, target, target)).thenReturn(true);
        when(stock.getMateriaisComEstoqueNaLocation(location)).thenReturn(Collections.emptySet());
        when(demand.getCalendario()).thenReturn(calendar);
        when(demand.getDemandPlanItem(location, material, calendar.getPosicaoPeriodoFinalFuturo()))
                .thenReturn(demandLine);
        when(demandLine.getUnidadeMedida(parameters)).thenReturn(origin);
        when(units.contemConversaoParaUnidadeDestino(material, origin, target)).thenReturn(false);

        UnidadeMedidaFacade service = new UnidadeMedidaFacade();
        VersaoMalhaRepository versaoMalhaRepository = mock(VersaoMalhaRepository.class);
        ReflectionTestUtils.setField(service, "versaoMalhaRepository", versaoMalhaRepository);
        DemandPlanRepository demandPlanRepository = mock(DemandPlanRepository.class);
        ReflectionTestUtils.setField(service, "demandPlanRepository", demandPlanRepository);
        PerfilExecucaoSupplyPlanRepository perfilExecucaoSupplyPlanRepository = mock(PerfilExecucaoSupplyPlanRepository.class);
        ReflectionTestUtils.setField(service, "perfilExecucaoSupplyPlanRepository", perfilExecucaoSupplyPlanRepository);
        ParametrosGlobaisService parametrosGlobaisService = mock(ParametrosGlobaisService.class);
        ReflectionTestUtils.setField(service, "parametrosGlobaisService", parametrosGlobaisService);
        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory = mock(UnidadeMedidaProjectionFactory.class);
        ReflectionTestUtils.setField(service, "unidadeMedidaProjectionFactory", unidadeMedidaProjectionFactory);
        ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory = mock(ClusterEParametrosProjectionFactory.class);
        ReflectionTestUtils.setField(service, "clusterEParametrosProjectionFactory", clusterEParametrosProjectionFactory);
        SupplyNetworkProjectionFactory supplyNetworkProjectionFactory = mock(SupplyNetworkProjectionFactory.class);
        ReflectionTestUtils.setField(service, "supplyNetworkProjectionFactory", supplyNetworkProjectionFactory);
        DemandPlanProjectionFactory demandPlanProjectionFactory = mock(DemandPlanProjectionFactory.class);
        ReflectionTestUtils.setField(service, "demandPlanProjectionFactory", demandPlanProjectionFactory);
        EstoqueProjectionFactory estoqueProjectionFactory = mock(EstoqueProjectionFactory.class);
        ReflectionTestUtils.setField(service, "estoqueProjectionFactory", estoqueProjectionFactory);
        when(versaoMalhaRepository.findById("NETWORK")).thenReturn(Optional.of(networkVersion));
        when(demandPlanRepository.customFindByIdComPerfilExecucao(1L)).thenReturn(Optional.of(demandPlan));
        when(perfilExecucaoSupplyPlanRepository.findById("PROFILE")).thenReturn(Optional.of(profile));
        when(parametrosGlobaisService.getParametrosGlobais()).thenReturn(parameters);
        when(unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache()).thenReturn(units);
        when(clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache()).thenReturn(cluster);
        when(supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache()).thenReturn(network);
        when(demandPlanProjectionFactory.getDemandPlanningProjectionCompleto(
                any(DemandPlan.class), any(Set.class), any(Set.class), anyBoolean())).thenReturn(demand);
        when(estoqueProjectionFactory.getEstoqueProjectionLocationProduto(
                any(LocalDateTime.class), any(Set.class), any(Set.class),
                any(UnidadeMedidaProjection.class), any(ClusterEParametrosProjection.class), any(UnidadeMedida.class)))
                .thenReturn(stock);

        Set<UnidadeConversaoFaltanteDTO> gaps = service.getUnidadeConversaoFaltanteSNPListDTO(
                reference, Constantes.TamanhoBucket.ANUAL, "NETWORK", "PROFILE", 1L);

        assertEquals(2, gaps.size());
        UnidadeConversaoFaltanteDTO gap = gaps.stream().filter(row -> "Demand Plan UOM".equals(row.getReason())).findFirst().orElseThrow();
        assertEquals("BOX", gap.getOriginUnitOfMeasure());
        assertEquals("KG", gap.getTargetUnitOfMeasure());
        org.junit.jupiter.api.Assertions.assertTrue(gap.getAdditionalInformation().contains("Location SITE"));
        assertEquals("Demand Plan UOM", gap.getReason());
        UnidadeConversaoFaltanteDTO transferLotGap = gaps.stream()
                .filter(row -> "LOT".equals(row.getTargetUnitOfMeasure())).findFirst().orElseThrow();
        assertEquals("Transfer lot size / multiple", transferLotGap.getReason());
        assertEquals("MATERIAL", transferLotGap.getMaterialId());
        assertEquals("KG", transferLotGap.getOriginUnitOfMeasure());
        org.junit.jupiter.api.Assertions.assertTrue(transferLotGap.getAdditionalInformation().contains("Transportation Line"));
        org.junit.jupiter.api.Assertions.assertTrue(transferLotGap.getAdditionalInformation().contains("TARGET_SITE"));


    }

}
