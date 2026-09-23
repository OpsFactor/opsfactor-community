package com.opsfactor.community.capability.supplyplanning.engine.constrained;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.DemandaDiretaConsideradaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** Confere que falta física reduz atendimento restrito na UOM da linha. */
class ReconciliacaoDemandaDiretaComSaldoFisicoTest {

    @Test
    void deveReduzirSomenteDemandaRestritaPelaFaltaNaUnidadeDaLinha() {

        SupplyPlanningProjection projection = mock(SupplyPlanningProjection.class);
        DemandaDiretaConsideradaProjection demandaProjection = mock(DemandaDiretaConsideradaProjection.class);
        UnidadeMedidaProjection unidadeProjection = mock(UnidadeMedidaProjection.class);
        Location location = mock(Location.class);
        Produto material = mock(Produto.class);
        UnidadeMedida unidadeLinha = mock(UnidadeMedida.class);
        UnidadeMedida unidadePadrao = mock(UnidadeMedida.class);
        DemandaDiretaConsideradaLinha linha = new DemandaDiretaConsideradaLinha();
        linha.setUnidadeMedida(unidadeLinha);
        linha.setQuantidadeDemandaDiretaPlanoDemandaIrrestrita(50.0);
        linha.setQuantidadeDemandaDiretaPlanoDemandaRestrita(50.0);
        linha.setQuantidadeDemandaDiretaCarteiraRestrita(0.0);

        when(projection.getLocation()).thenReturn(location);
        when(projection.getDemandaDiretaConsideradaProjection()).thenReturn(demandaProjection);
        when(projection.getConversaoUnidadeMedidaProjection()).thenReturn(unidadeProjection);
        when(demandaProjection.getDemandaDiretaConsideradaLinha(location, material, 1))
                .thenReturn(Optional.of(linha));
        when(unidadeProjection.getConversaoParaUnidadeDestino(material, unidadeLinha, unidadePadrao))
                .thenReturn(2.0);

        try (MockedStatic<SupplyPlanning> supplyPlanning = mockStatic(SupplyPlanning.class)) {
            supplyPlanning.when(() -> SupplyPlanning.getEstoqueProjetado(
                    eq(projection), eq(0), eq(1), eq(material),
                    eq(Constantes.TipoPlano.PLANO_RESTRITO), eq(unidadePadrao),
                    eq(true), eq(true), eq(false), eq(true)))
                    .thenReturn(-35.0);

            ConstrainedPlanningHeuristicoRotinas.reconciliaDemandaDiretaComSaldoFisico(
                    projection, 1, material, unidadePadrao);
        }

        assertEquals(32.5, linha.getQuantidadeDemandaDiretaPlanoDemandaRestrita(), 0.000001);
        assertEquals(50.0, linha.getQuantidadeDemandaDiretaPlanoDemandaIrrestrita(), 0.000001);

    }

    @Test
    void deveConsumirCarteiraSomenteDepoisDoPlanoDeDemanda() {

        SupplyPlanningProjection projection = mock(SupplyPlanningProjection.class);
        DemandaDiretaConsideradaProjection demandaProjection = mock(DemandaDiretaConsideradaProjection.class);
        UnidadeMedidaProjection unidadeProjection = mock(UnidadeMedidaProjection.class);
        Location location = mock(Location.class);
        Produto material = mock(Produto.class);
        UnidadeMedida unidade = mock(UnidadeMedida.class);
        DemandaDiretaConsideradaLinha linha = new DemandaDiretaConsideradaLinha();
        linha.setUnidadeMedida(unidade);
        linha.setQuantidadeDemandaDiretaPlanoDemandaRestrita(10.0);
        linha.setQuantidadeDemandaDiretaCarteiraRestrita(8.0);

        when(projection.getLocation()).thenReturn(location);
        when(projection.getDemandaDiretaConsideradaProjection()).thenReturn(demandaProjection);
        when(projection.getConversaoUnidadeMedidaProjection()).thenReturn(unidadeProjection);
        when(demandaProjection.getDemandaDiretaConsideradaLinha(location, material, 1))
                .thenReturn(Optional.of(linha));
        when(unidadeProjection.getConversaoParaUnidadeDestino(material, unidade, unidade))
                .thenReturn(1.0);

        try (MockedStatic<SupplyPlanning> supplyPlanning = mockStatic(SupplyPlanning.class)) {
            supplyPlanning.when(() -> SupplyPlanning.getEstoqueProjetado(
                    eq(projection), eq(0), eq(1), eq(material),
                    eq(Constantes.TipoPlano.PLANO_RESTRITO), eq(unidade),
                    eq(true), eq(true), eq(false), eq(true)))
                    .thenReturn(-13.0);

            ConstrainedPlanningHeuristicoRotinas.reconciliaDemandaDiretaComSaldoFisico(
                    projection, 1, material, unidade);
        }

        assertEquals(0.0, linha.getQuantidadeDemandaDiretaPlanoDemandaRestrita(), 0.000001);
        assertEquals(5.0, linha.getQuantidadeDemandaDiretaCarteiraRestrita(), 0.000001);

    }

}
