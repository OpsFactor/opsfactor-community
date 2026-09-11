package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDateImpl;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.repository.SelloutRepository;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SalesAnnualCalendarTest {

    @Test
    void consultaMesesUmaVezESomaNoAnoSemMisturarAnoSeguinte() throws Exception {

        var calendario = CalendarioSimples.criaCalendarioDeOffsetsPeriodos(Constantes.TamanhoBucket.ANUAL,
                LocalDate.of(2023, 1, 1).atStartOfDay(), 0, 0, 2, 0);
        var produto = new Produto("PRODUTO");
        var location = new Location("LOCAL");
        var unidade = mock(UnidadeMedida.class);
        var conversao = mock(UnidadeMedidaProjection.class);
        when(conversao.getConversaoParaUnidadeDestino(produto, unidade, unidade)).thenReturn(1d);
        List<AggregatedByMaterialUOMDate> meses = List.of(
                mes(produto, unidade, "2023-01-31", 300d), mes(produto, unidade, "2023-02-28", 200d),
                mes(produto, unidade, "2024-01-31", 100d), mes(produto, unidade, "2024-02-29", 100d));
        var repository = mock(SelloutRepository.class);
        when(repository.consolidatedSelloutByMaterialUOMMonthForMaterialLocationIds(any(), any(), any(), any()))
                .thenReturn(meses);
        var source = new SelloutHistoricalSalesSource();
        ReflectionTestUtils.setField(source, "selloutRepository", repository);
        var factory = new SalesProjectionFactory();
        ReflectionTestUtils.setField(factory, "selloutHistoricalSalesSource", source);
        var projection = factory.getSalesProjectionMaterialData(Constantes.TipoDocumentoVenda.SELLOUT, calendario,
                Set.of(location), Set.of(produto), conversao, mock(ClusterEParametrosProjection.class), unidade);

        assertEquals(500f, projection.getQuantidadeSales(produto, 0, unidade));
        assertEquals(200f, projection.getQuantidadeSales(produto, 1, unidade));
        verify(repository).consolidatedSelloutByMaterialUOMMonthForMaterialLocationIds(
                calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal(), Set.of("LOCAL"), Set.of("PRODUTO"));
        verifyNoMoreInteractions(repository);

    }

    private AggregatedByMaterialUOMDate mes(Produto produto, UnidadeMedida unidade, String data, double quantidade) {

        return AggregatedByMaterialUOMDateImpl.builder().material(produto).uom(unidade)
                .referenceDate(LocalDate.parse(data)).totalQuantity(quantidade).build();

    }

}
