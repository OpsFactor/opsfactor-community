package com.opsfactor.community.platform.bi.service;

import com.opsfactor.community.capability.configuration.projection.parametros.CadastroEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningMultiplasLocationsProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.DemandaDiretaConsideradaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.utility.Constantes;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

/** A soma mista consome valores das linhas e não precisa de projection de conversão. */
class CommunitySupplyOverviewBaseFactoryMixedUomTest {

    @Test
    void sumsStoredQuantitiesAndExposesDerivedDemandAsUnavailable() {

        var calendar = Mockito.mock(CalendarioSimples.class);
        Mockito.when(calendar.getListaDatasHorarios()).thenReturn(List.of(LocalDateTime.of(2026, 1, 31, 23, 59)));
        Mockito.when(calendar.getNumeroPeriodosFuturos()).thenReturn(1);
        Mockito.when(calendar.getPosicaoPeriodoPresente()).thenReturn(0);
        Mockito.when(calendar.getPosicaoPeriodoFinalFuturo()).thenReturn(0);
        var location = Mockito.mock(Location.class);
        var material = Mockito.mock(Produto.class);
        Mockito.when(location.getId()).thenReturn("L");
        Mockito.when(material.getId()).thenReturn("P");
        var materialProjection = Mockito.mock(MaterialProjection.class);
        Mockito.when(materialProjection.getMateriaisAtivos()).thenReturn(Set.of(material));
        var parameters = Mockito.mock(CadastroEParametrosProjection.class);
        Mockito.when(parameters.isDfuAtiva(material, location)).thenReturn(true);
        var multi = Mockito.mock(SupplyPlanningMultiplasLocationsProjection.class);
        var projection = Mockito.mock(SupplyPlanningProjection.class);
        Mockito.when(multi.getSupplyPlanningProjectionDeLocation(location)).thenReturn(projection);
        Mockito.when(projection.getCalendario()).thenReturn(calendar);
        Mockito.when(projection.getLocation()).thenReturn(location);
        var directProjection = Mockito.mock(DemandaDiretaConsideradaProjection.class);
        var directLine = Mockito.mock(DemandaDiretaConsideradaLinha.class);
        Mockito.when(projection.getDemandaDiretaConsideradaProjection()).thenReturn(directProjection);
        Mockito.when(directProjection.getDemandaDiretaConsideradaLinha(location, material, 0)).thenReturn(Optional.of(directLine));
        Mockito.when(directLine.getQuantidadeDemandaDiretaConsideradaSegregada(
                DemandaDiretaConsideradaLinha.TipoDemandaDireta.PLANO_DEMANDA, Constantes.TipoPlano.PLANO_RESTRITO)).thenReturn(11.0);
        var customerOutbound = Mockito.mock(DistributionPlanItem.class);
        Mockito.when(customerOutbound.getQuantidade(Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO)).thenReturn(3.0);
        Mockito.when(projection.getDistributionPlanItemOutboundQueueParaClientes(Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, 0, material))
                .thenReturn(new LinkedList<>(List.of(customerOutbound)));
        var inventory = Mockito.mock(InventoryPlanLinha.class);
        Mockito.when(inventory.getQuantidadeEstoqueProjetado(Constantes.TipoPlano.PLANO_RESTRITO)).thenReturn(5.0);
        Mockito.when(projection.getInventoryPlanLinha(0, material)).thenReturn(Optional.of(inventory));
        var production = Mockito.mock(ProductionPlanLinha.class);
        Mockito.when(production.getQuantidade(Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.TOTAL)).thenReturn(7.0);
        Mockito.when(projection.getProductionPlanLinhaOutput(0, material)).thenReturn(new LinkedList<>(List.of(production)));
        var inbound = Mockito.mock(DistributionPlanItem.class);
        Mockito.when(inbound.getQuantidade(Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO)).thenReturn(9.0);
        Mockito.when(projection.getDistributionPlanInboundQueue(Constantes.ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, 0, material))
                .thenReturn(new LinkedList<>(List.of(inbound)));

        var result = new CommunitySupplyOverviewBaseFactory().create(calendar, List.of(location), materialProjection,
                parameters, null, multi);

        var stock = result.stockAndProductionByLocationAndMaterialGrouping.getFirst();
        assertEquals("Multiple UOMs", stock.quantityUomId);
        assertEquals(5.0f, stock.constrainedInventory[0]);
        assertEquals(7.0f, stock.constrainedProduction[0]);
        assertEquals(9.0f, stock.constrainedInbound[0]);
        assertEquals(14.0f, result.directAndIndirectDemandByLocationAndMaterialGrouping.getFirst().constrainedDirectDemand[0]);
        assertNull(result.directAndIndirectDemandByLocationAndMaterialGrouping.getFirst().constrainedIndirectDemand);
        Mockito.verify(projection, Mockito.never()).getConversaoUnidadeMedidaProjection();
    }
}
