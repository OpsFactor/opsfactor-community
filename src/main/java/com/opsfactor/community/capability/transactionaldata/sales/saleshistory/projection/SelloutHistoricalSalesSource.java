package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.repository.SelloutRepository;
import com.opsfactor.community.platform.calendar.Calendario;
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
            Calendario calendario,
            Set<String> locationIds,
            Set<String> materialIds) {

        return switch (calendario.getTamanhoBucket()) {
            case MENSAL -> selloutRepository.consolidatedSelloutByMaterialUOMMonthForMaterialLocationIds(
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
            Calendario calendario,
            Set<String> locationIds,
            Set<String> materialIds) {

        return switch (calendario.getTamanhoBucket()) {
            case MENSAL -> (locationIds != null && materialIds != null)
                    ? selloutRepository.consolidatedSelloutByLocationMaterialUOMMonthForMaterialLocationIds(
                    calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal(), locationIds, materialIds)
                    : selloutRepository.consolidatedSelloutByLocationMaterialUOMMonth(
                    calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal());
            case SEMANAL -> (locationIds != null && materialIds != null)
                    ? selloutRepository.consolidatedSelloutByLocationMaterialUOMWeekForMaterialLocationIds(
                    calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal(), locationIds, materialIds)
                    : selloutRepository.consolidatedSelloutByLocationMaterialUOMWeek(
                    calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal());
            case DIARIO -> (locationIds != null && materialIds != null)
                    ? selloutRepository.consolidatedSelloutByLocationMaterialUOMDayForMaterialLocationIds(
                    calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal(), locationIds, materialIds)
                    : selloutRepository.consolidatedSelloutByLocationMaterialUOMDay(
                    calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal());
            default -> throw getUnsupportedBucketException(calendario);
        };

    }

    @Override
    public Collection<AggregatedByMaterialUOM> getAggregatedByMaterialUom(
            Calendario calendario,
            Set<String> locationIds,
            Set<String> materialIds) {

        return selloutRepository.consolidatedSelloutByMaterialUOMAtLocationIds(
                calendario.getDataHorarioInicial(), calendario.getDataHorarioFinal(), locationIds, materialIds);

    }

    @Override
    public Collection<AggregatedByLocationMaterialUOM> getAggregatedByLocationMaterialUom(
            Calendario calendario,
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

    private IllegalArgumentException getUnsupportedBucketException(Calendario calendario) {

        return new IllegalArgumentException(
                "Historical sales source does not support bucket size "
                        + calendario.getTamanhoBucket());

    }

}
