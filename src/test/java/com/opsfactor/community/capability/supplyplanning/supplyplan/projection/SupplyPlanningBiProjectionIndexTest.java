package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.supplyplanning.distributionplan.projection.DistributionPlanItemBiProjection;
import com.opsfactor.community.capability.supplyplanning.productionplan.projection.ProductionPlanLinhaBiProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifica que os índices centrais preservam uma instância física por linha. */
class SupplyPlanningBiProjectionIndexTest {

    @Test
    void distributionPlanItemDeveSerUnicaEVisivelPorOrigemEDestino() {

        Calendario calendario = mock(Calendario.class);
        when(calendario.getPosicaoPeriodo(org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(0);
        DistributionPlanItemBiProjection projection = new DistributionPlanItemBiProjection(
                calendario, mock(UnidadeMedidaProjection.class));
        SupplyPlan supplyPlan = new SupplyPlan();
        Location origem = new Location("ORIGEM");
        Location destino = new Location("DESTINO");
        Produto material = new Produto("MATERIAL");
        DistributionPlanItem distributionPlanItem = new DistributionPlanItem(
                new DistributionPlanItem.DistributionPlanItemKey(
                        supplyPlan, destino, origem, material,
                        LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0)));

        projection.addDadoAoBI(distributionPlanItem);
        projection.addDadoAoBI(distributionPlanItem);

        assertEquals(1, projection.getStreamTodosDistributionPlanItems().count());
        assertSame(distributionPlanItem, projection.getDistributionPlanItemsPorOrigem(
                supplyPlan, origem, Set.of(material)).iterator().next());
        assertSame(distributionPlanItem, projection.getDistributionPlanItemsPorDestino(
                supplyPlan, destino, Set.of(material)).iterator().next());

    }

    @Test
    void productionPlanLinhaDeveSerUnicaMesmoComMultiplosInputs() {

        SupplyNetworkProjection supplyNetworkProjection = mock(SupplyNetworkProjection.class);
        ProductionPlanLinhaBiProjection projection = new ProductionPlanLinhaBiProjection(supplyNetworkProjection);
        ProductionPlanLinha productionPlanLinha = mock(ProductionPlanLinha.class);
        Location location = new Location("PLANTA");
        Produto output = new Produto("OUTPUT");
        Produto inputA = new Produto("INPUT-A");
        Produto inputB = new Produto("INPUT-B");
        when(productionPlanLinha.getLocation()).thenReturn(location);
        when(productionPlanLinha.getMaterialOutput()).thenReturn(output);
        when(productionPlanLinha.getDataReferencia()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0));
        when(productionPlanLinha.getMateriaisInput(supplyNetworkProjection)).thenReturn(Set.of(inputA, inputB));

        projection.addDadoAoBI(productionPlanLinha);
        projection.addDadoAoBI(productionPlanLinha);

        assertEquals(1, projection.getTodosProductionPlanLinhas().size());
        assertSame(productionPlanLinha, projection.getProductionPlanLinhasComInput(location, Set.of(inputA)).iterator().next());
        assertSame(productionPlanLinha, projection.getProductionPlanLinhasComInput(location, Set.of(inputB)).iterator().next());

    }

    @Test
    void distributionIndexDeveIsolarLinhasDeSupplyPlansDistintos() {

        Calendario calendario = mock(Calendario.class);
        when(calendario.getPosicaoPeriodo(org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(0);
        DistributionPlanItemBiProjection projection = new DistributionPlanItemBiProjection(
                calendario, mock(UnidadeMedidaProjection.class));
        SupplyPlan primeiroPlano = new SupplyPlan();
        SupplyPlan segundoPlano = new SupplyPlan();
        Location origem = new Location("ORIGEM");
        Location destino = new Location("DESTINO");
        Produto material = new Produto("MATERIAL");
        LocalDateTime expedicao = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime recebimento = LocalDateTime.of(2026, 7, 2, 0, 0);
        projection.addDadoAoBI(new DistributionPlanItem(new DistributionPlanItem.DistributionPlanItemKey(
                primeiroPlano, destino, origem, material, expedicao, recebimento)));
        projection.addDadoAoBI(new DistributionPlanItem(new DistributionPlanItem.DistributionPlanItemKey(
                segundoPlano, destino, origem, material, expedicao, recebimento)));

        assertEquals(1, projection.getDistributionPlanItemsPorOrigem(
                primeiroPlano, origem, Set.of(material)).size());
        assertEquals(1, projection.getDistributionPlanItemsPorOrigem(
                segundoPlano, origem, Set.of(material)).size());

    }

}
