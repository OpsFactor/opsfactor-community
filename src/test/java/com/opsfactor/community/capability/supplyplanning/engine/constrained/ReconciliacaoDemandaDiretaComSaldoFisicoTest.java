package com.opsfactor.community.capability.supplyplanning.engine.constrained;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanHeuristicDemandRestrictionSpi;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.DemandaDiretaConsideradaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

import static com.opsfactor.community.platform.utility.Constantes.FirmePlanejado.PLANEJADO;
import static com.opsfactor.community.platform.utility.Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE;
import static com.opsfactor.community.platform.utility.Constantes.TipoPlano.PLANO_RESTRITO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReconciliacaoDemandaDiretaComSaldoFisicoTest {

    private final SupplyPlanningProjection projection = mock(SupplyPlanningProjection.class);
    private final UnidadeMedidaProjection conversoes = mock(UnidadeMedidaProjection.class);
    private final Location location = mock(Location.class);
    private final Produto material = mock(Produto.class);
    private final UnidadeMedida unidadePadrao = mock(UnidadeMedida.class);
    private final DemandaDiretaConsideradaLinha linha = new DemandaDiretaConsideradaLinha();
    private final DistributionPlanItem expedicao = new DistributionPlanItem();

    ReconciliacaoDemandaDiretaComSaldoFisicoTest() {

        UnidadeMedida unidadeLinha = mock(UnidadeMedida.class);
        var chaveDemanda = new DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey();
        chaveDemanda.setMaterial(material);
        linha.setDemandaDiretaConsideradaLinhaCompositeKey(chaveDemanda);
        linha.setUnidadeMedida(unidadeLinha);
        linha.setQuantidadeDemandaDiretaPlanoDemandaIrrestrita(20.0);
        linha.setQuantidadeDemandaDiretaPlanoDemandaRestrita(20.0);
        var chaveExpedicao = new DistributionPlanItem.DistributionPlanItemKey();
        chaveExpedicao.setProduto(material);
        expedicao.setKey(chaveExpedicao);
        expedicao.setUnidadeMedida(unidadeLinha);
        expedicao.setQuantidade(10.0, PLANEJADO, PLANO_RESTRITO);
        DemandaDiretaConsideradaProjection demanda = mock(DemandaDiretaConsideradaProjection.class);

        when(projection.getLocation()).thenReturn(location);
        when(projection.getDemandaDiretaConsideradaProjection()).thenReturn(demanda);
        when(projection.getConversaoUnidadeMedidaProjection()).thenReturn(conversoes);
        when(demanda.getUnidadeMedidaProjection()).thenReturn(conversoes);
        when(demanda.getDemandaDiretaConsideradaLinha(location, material, 1)).thenReturn(Optional.of(linha));
        when(conversoes.getConversaoParaUnidadeDestino(material, unidadeLinha, unidadePadrao)).thenReturn(2.0);
        when(projection.getDistributionPlanItemOutboundQueueParaLocationsInternas(CONSUMO_CAPACIDADE, 1, material))
                .thenReturn(new ArrayDeque<>());
        when(projection.getDistributionPlanItemOutboundQueueParaClientes(CONSUMO_CAPACIDADE, 1, material))
                .thenReturn(new ArrayDeque<>(List.of(expedicao)));

    }

    @Test
    void rateiaPrevisaoLocalEExpedicaoNaMesmaProporcaoEUsaSaldoAtualNaSegundaPassagem() {

        assertEquals(0, restringe(15), 0.000001);
        assertEquals(15, linha.getQuantidadeDemandaDiretaPlanoDemandaRestrita(), 0.000001);
        assertEquals(7.5, expedicao.getQuantidade(PLANEJADO, PLANO_RESTRITO), 0.000001);

        assertEquals(0, restringe(15), 0.000001);
        assertEquals(10, linha.getQuantidadeDemandaDiretaPlanoDemandaRestrita(), 0.000001);
        assertEquals(5, expedicao.getQuantidade(PLANEJADO, PLANO_RESTRITO), 0.000001);
        assertEquals(20, linha.getQuantidadeDemandaDiretaPlanoDemandaIrrestrita(), 0.000001);
        assertEquals(0, linha.getQuantidadeDemandaDiretaCarteiraRestrita(), 0.000001);

    }

    @Test
    void usaConsumoAdicionalParaCalcularDeficitEProtegeEsseGrupoAteEsgotarForecast() {

        SupplyPlanHeuristicDemandRestrictionSpi extensao = mock(SupplyPlanHeuristicDemandRestrictionSpi.class);
        when(projection.getRestricaoDemandaAdicional()).thenReturn(extensao);
        when(projection.getQuantidadeDistributionPlanOutboundParaClientes(
                CONSUMO_CAPACIDADE, 1, material, PLANEJADO, PLANO_RESTRITO, unidadePadrao))
                .thenAnswer(call -> 2 * expedicao.getQuantidade(PLANEJADO, PLANO_RESTRITO));
        double[] consumo = {40};
        when(extensao.getConsumoAdicional(projection, 1, material, PLANO_RESTRITO, unidadePadrao))
                .thenAnswer(call -> consumo[0]);
        when(extensao.getParcelasDemandaAdicional(projection, 1, material, unidadePadrao))
                .thenAnswer(call -> List.of(new CorteProporcional.Parcela(consumo[0],
                        fracao -> consumo[0] *= 1 - fracao)));

        assertEquals(-100, saldo(), 0.000001);
        assertEquals(0, restringe(30), 0.000001);
        assertEquals(40, consumo[0], 0.000001);
        assertEquals(-70, saldo(), 0.000001);

        assertEquals(0, restringe(40), 0.000001);
        assertEquals(0, linha.getQuantidadeDemandaDiretaPlanoDemandaRestrita(), 0.000001);
        assertEquals(0, expedicao.getQuantidade(PLANEJADO, PLANO_RESTRITO), 0.000001);
        assertEquals(30, consumo[0], 0.000001);
        assertEquals(-30, saldo(), 0.000001);

    }

    private double restringe(double deficit) {

        return ConstrainedPlanningHeuristicoRotinas.restringeDemandaPorDisponibilidade(
                projection, 1, material, unidadePadrao, deficit);

    }

    private double saldo() {

        return SupplyPlanning.getSaldoEntradasSaidas(
                projection, 1, material, PLANO_RESTRITO, unidadePadrao, true, true);

    }
}
