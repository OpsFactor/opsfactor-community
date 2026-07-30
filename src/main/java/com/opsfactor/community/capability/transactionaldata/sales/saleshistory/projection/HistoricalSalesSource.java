package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDate;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;

import java.util.Collection;
import java.util.Set;

/**
 * Fonte batch de uma modalidade de venda histórica.
 *
 * <p>A fonte conhece somente a entidade persistida, a semântica da location e
 * as queries por bucket. A montagem das projections, validações dos agregados,
 * conversão de UOM e consolidação de demanda pertencem exclusivamente à
 * {@link SalesProjectionFactory}.</p>
 */
public interface HistoricalSalesSource {

    /**
     * Documento histórico atendido por esta fonte.
     */
    Constantes.TipoDocumentoVenda getTipoDocumentoVenda();

    /**
     * Carrega agregados por material, UOM e data para filtros obrigatórios.
     */
    Collection<AggregatedByMaterialUOMDate> getAggregatedByMaterialUomDate(
            Calendario calendario,
            Set<String> locationIds,
            Set<String> materialIds);

    /**
     * Carrega agregados por location, material, UOM e data, com filtros opcionais.
     */
    Collection<AggregatedByLocationMaterialUOMDate> getAggregatedByLocationMaterialUomDate(
            Calendario calendario,
            Set<String> locationIds,
            Set<String> materialIds);

    /**
     * Carrega agregados por material e UOM para filtros obrigatórios.
     */
    Collection<AggregatedByMaterialUOM> getAggregatedByMaterialUom(
            Calendario calendario,
            Set<String> locationIds,
            Set<String> materialIds);

    /**
     * Carrega agregados por location, material e UOM para filtros obrigatórios.
     */
    Collection<AggregatedByLocationMaterialUOM> getAggregatedByLocationMaterialUom(
            Calendario calendario,
            Set<String> locationIds,
            Set<String> materialIds);

    /**
     * Carrega as três granularidades de primeira e última venda.
     */
    Collection<FirstLastByMaterialLocation> getFirstLastByMaterialLocation();

    Collection<FirstLastByLocation> getFirstLastByLocation();

    Collection<FirstLastByMaterial> getFirstLastByMaterial();

}
