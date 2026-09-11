package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.repository.SelloutRepository;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.calendar.IntervaloExtracaoCalendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * Fonte Community de sell-out histórico.
 *
 * <p>As queries permanecem batch e selecionam somente o bucket suportado. A
 * fonte não valida nem materializa projections para que esse comportamento seja
 * único para todas as modalidades de documento.</p>
 */
@Component
public class SelloutHistoricalSalesSource implements HistoricalSalesSource {

    /** Repository de sell-out consultado em lote para montar cada recorte histórico. */
    @Autowired
    private SelloutRepository selloutRepository;

    @Override
    public Constantes.TipoDocumentoVenda getTipoDocumentoVenda() {

        return Constantes.TipoDocumentoVenda.SELLOUT;

    }

    @Override
    public Collection<AggregatedByMaterialUOMDate> getAggregatedByMaterialUomDate(
            CalendarioSimples calendario,
            Set<String> locationIds,
            Set<String> materialIds) {

        // Sem query anual específica: os meses são preservados na extração e
        // consolidados pela factory no fim do ano, sem perder meses de mesmo valor.
        return switch (calendario.getTamanhoBucket()) {
            case MENSAL, ANUAL -> selloutRepository.consolidatedSelloutByMaterialUOMMonthForMaterialLocationIds(
                    calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal(), locationIds, materialIds);
            case SEMANAL -> selloutRepository.consolidatedSelloutByMaterialUOMWeekForMaterialLocationIds(
                    calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal(), locationIds, materialIds);
            case DIARIO -> selloutRepository.consolidatedSelloutByMaterialUOMDayForMaterialLocationIds(
                    calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal(), locationIds, materialIds);
            default -> throw getUnsupportedBucketException(calendario);
        };

    }

    @Override
    public Collection<AggregatedByLocationMaterialUOMDate> getAggregatedByLocationMaterialUomDate(
            IntervaloExtracaoCalendario intervaloExtracao,
            Set<String> locationIds,
            Set<String> materialIds) {

        return switch (intervaloExtracao.tamanhoBucket()) {
            case MENSAL -> (locationIds != null && materialIds != null)
                    ? selloutRepository.consolidatedSelloutByLocationMaterialUOMMonthForMaterialLocationIds(
                    intervaloExtracao.dataHorarioInicial(), intervaloExtracao.dataHorarioFinal(), locationIds, materialIds)
                    : selloutRepository.consolidatedSelloutByLocationMaterialUOMMonth(
                    intervaloExtracao.dataHorarioInicial(), intervaloExtracao.dataHorarioFinal());
            case SEMANAL -> (locationIds != null && materialIds != null)
                    ? selloutRepository.consolidatedSelloutByLocationMaterialUOMWeekForMaterialLocationIds(
                    intervaloExtracao.dataHorarioInicial(), intervaloExtracao.dataHorarioFinal(), locationIds, materialIds)
                    : selloutRepository.consolidatedSelloutByLocationMaterialUOMWeek(
                    intervaloExtracao.dataHorarioInicial(), intervaloExtracao.dataHorarioFinal());
            case DIARIO -> (locationIds != null && materialIds != null)
                    ? selloutRepository.consolidatedSelloutByLocationMaterialUOMDayForMaterialLocationIds(
                    intervaloExtracao.dataHorarioInicial(), intervaloExtracao.dataHorarioFinal(), locationIds, materialIds)
                    : selloutRepository.consolidatedSelloutByLocationMaterialUOMDay(
                    intervaloExtracao.dataHorarioInicial(), intervaloExtracao.dataHorarioFinal());
            default -> throw new IllegalArgumentException("Historical sales source does not support bucket size " + intervaloExtracao.tamanhoBucket());
        };

    }

    @Override
    public Collection<AggregatedByMaterialUOM> getAggregatedByMaterialUom(
            CalendarioSimples calendario,
            Set<String> locationIds,
            Set<String> materialIds) {

        return selloutRepository.consolidatedSelloutByMaterialUOMAtLocationIds(
                calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal(), locationIds, materialIds);

    }

    @Override
    public Collection<AggregatedByLocationMaterialUOM> getAggregatedByLocationMaterialUom(
            CalendarioSimples calendario,
            Set<String> locationIds,
            Set<String> materialIds) {

        return selloutRepository.consolidatedSelloutByLocationMaterialUOMAtLocationIds(
                calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal(), locationIds, materialIds);

    }

    @Override
    public Collection<FirstLastByMaterialLocation> getFirstLastByMaterialLocation() {

        return selloutRepository.findFirstLastSelloutPorMaterialLocation();

    }

    @Override
    public Collection<FirstLastByLocation> getFirstLastByLocation() {

        return selloutRepository.findFirstLastSelloutPorLocation();

    }

    @Override
    public Collection<FirstLastByMaterial> getFirstLastByMaterial() {

        return selloutRepository.findFirstLastSelloutPorMaterial();

    }

    private IllegalArgumentException getUnsupportedBucketException(CalendarioSimples calendario) {

        return new IllegalArgumentException(
                "Historical sales source does not support bucket size "
                        + calendario.getTamanhoBucket());

    }

}
