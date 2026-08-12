package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.platform.bi.service.CommunityInventoryOverviewCoverageCalculator;
import com.opsfactor.community.platform.bi.service.CommunityInventoryOverviewProjectionLoader;
import com.opsfactor.community.platform.bi.service.CommunityInventoryOverviewProjectionLoader.CommunityInventoryOverviewProjectionContext;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewMaterialLocationDetailDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewPeriodDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewSelectionDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Publica a visão física do estoque projetado e seu esgotamento agregado.
 *
 * <p>Não há cálculo por SKU de cobertura nem leitura adicional durante o
 * loop: a série é agregada na UOM solicitada a partir da projection única do
 * Supply Plan e então convertida para Days of Supply.</p>
 */
@Service
public class CommunityInventoryOverviewService {

    /** Materializa o recorte físico indexado antes de calcular a visão de estoque. */
    @Autowired
    private CommunityInventoryOverviewProjectionLoader inventoryOverviewProjectionLoader;

    /** Materializa as séries restrita e irrestrita do recorte físico Community. */
    public CommunityInventoryOverviewDTO getInventoryOverview(CommunityInventoryOverviewSelectionDTO selectionDTO) {

        return getInventoryOverviewPhysicalRead(selectionDTO).inventoryOverview();

    }

    /**
     * Expõe a fotografia física já calculada para overlays Enterprise do mesmo
     * request, sem repetir a carga batch de Supply Planning.
     *
     * <p>O tipo retornado contém somente o recorte Community e seus índices
     * transitórios. Ele não introduz dependência inversa nem dados privados no
     * Community.</p>
     */
    public CommunityInventoryOverviewPhysicalRead getInventoryOverviewPhysicalRead(
            CommunityInventoryOverviewSelectionDTO selectionDTO) {

        CommunityInventoryOverviewProjectionContext projectionContext = inventoryOverviewProjectionLoader.load(selectionDTO);
        Calendario calendar = projectionContext.calendar();
        int firstFuturePeriod = calendar.getPosicaoPeriodoPresente();
        int finalFuturePeriod = calendar.getPosicaoPeriodoFinalFuturo();
        int numberOfDisplayedPeriods = finalFuturePeriod - firstFuturePeriod + 1;
        double[] constrainedStock = new double[numberOfDisplayedPeriods];
        double[] unconstrainedStock = new double[numberOfDisplayedPeriods];
        double[] constrainedConsumption = new double[numberOfDisplayedPeriods];
        double[] unconstrainedConsumption = new double[numberOfDisplayedPeriods];
        double[] daysByPeriod = new double[numberOfDisplayedPeriods];
        List<Double> daysInPeriod = new ArrayList<>();
        List<CommunityInventoryOverviewMaterialLocationDetailDTO> materialLocationDetails = new ArrayList<>();

        for (int period = firstFuturePeriod; period <= finalFuturePeriod; period++) {

            double days = calendar.getNumeroDiasNoPeriodo(period);
            daysByPeriod[period - firstFuturePeriod] = days;
            daysInPeriod.add(days);
        }

        for (Location location : projectionContext.eligibleLocations()) {

            SupplyPlanningProjection locationProjection = projectionContext.supplyPlanningBiProjection()
                    .getSupplyPlanningProjection(location, projectionContext.materialProjection());
            ClusterEParametrosProjection clusterAndParametersProjection = locationProjection
                    .getSupplyNetworkProjection()
                    .getClusterEParametrosProjection();
            for (Produto material : projectionContext.materialProjection().getMateriaisAtivos()) {

                if (!clusterAndParametersProjection.isDfuAtiva(material, location)) {
                    continue;
                }
                CommunityInventoryOverviewMaterialLocationDetailDTO materialLocationDetail = getMaterialLocationDetail(
                        locationProjection,
                        material,
                        projectionContext,
                        firstFuturePeriod,
                        finalFuturePeriod);
                materialLocationDetails.add(materialLocationDetail);
                addToAggregate(constrainedStock, materialLocationDetail.constrainedProjectedStock());
                addToAggregate(unconstrainedStock, materialLocationDetail.unconstrainedProjectedStock());
                addToAggregate(constrainedConsumption, materialLocationDetail.constrainedConsumption());
                addToAggregate(unconstrainedConsumption, materialLocationDetail.unconstrainedConsumption());
            }
        }

        double[] constrainedCoverageDays = CommunityInventoryOverviewCoverageCalculator.calculateCoverageDays(
                constrainedStock,
                constrainedConsumption,
                daysByPeriod,
                selectionDTO.postHorizonPolicy());
        double[] unconstrainedCoverageDays = CommunityInventoryOverviewCoverageCalculator.calculateCoverageDays(
                unconstrainedStock,
                unconstrainedConsumption,
                daysByPeriod,
                selectionDTO.postHorizonPolicy());
        CommunityInventoryOverviewDTO inventoryOverview = new CommunityInventoryOverviewDTO(
                projectionContext.targetUnitOfMeasure().getId(),
                getPeriodDTOs(
                        calendar,
                        firstFuturePeriod,
                        constrainedStock,
                        unconstrainedStock,
                        constrainedCoverageDays,
                        unconstrainedCoverageDays),
                daysInPeriod,
                materialLocationDetails);
        return new CommunityInventoryOverviewPhysicalRead(inventoryOverview, projectionContext);

    }

    /** Soma estoque fechado e somente os dois fluxos físicos que o consomem. */
    private CommunityInventoryOverviewMaterialLocationDetailDTO getMaterialLocationDetail(
            SupplyPlanningProjection locationProjection,
            Produto material,
            CommunityInventoryOverviewProjectionContext projectionContext,
            int firstFuturePeriod,
            int finalFuturePeriod) {

        int numberOfDisplayedPeriods = finalFuturePeriod - firstFuturePeriod + 1;
        double[] constrainedStock = new double[numberOfDisplayedPeriods];
        double[] unconstrainedStock = new double[numberOfDisplayedPeriods];
        double[] constrainedConsumption = new double[numberOfDisplayedPeriods];
        double[] unconstrainedConsumption = new double[numberOfDisplayedPeriods];

        for (int period = firstFuturePeriod; period <= finalFuturePeriod; period++) {

            int displayedPeriodIndex = period - firstFuturePeriod;
            constrainedStock[displayedPeriodIndex] += locationProjection.getQuantidadeEstoqueProjetado(
                    period,
                    material,
                    Constantes.TipoPlano.PLANO_RESTRITO,
                    projectionContext.targetUnitOfMeasure());
            unconstrainedStock[displayedPeriodIndex] += locationProjection.getQuantidadeEstoqueProjetado(
                    period,
                    material,
                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                    projectionContext.targetUnitOfMeasure());
            constrainedConsumption[displayedPeriodIndex] += getStockConsumption(
                    locationProjection,
                    period,
                    material,
                    Constantes.TipoPlano.PLANO_RESTRITO,
                    projectionContext);
            unconstrainedConsumption[displayedPeriodIndex] += getStockConsumption(
                    locationProjection,
                    period,
                    material,
                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                    projectionContext);
        }

        return new CommunityInventoryOverviewMaterialLocationDetailDTO(
                locationProjection.getLocation().getId(),
                locationProjection.getLocation().getDescricao(),
                material.getId(),
                material.getDescricao(),
                getLocationCharacteristicValues(locationProjection.getLocation(), projectionContext),
                getMaterialCharacteristicValues(material, projectionContext),
                constrainedStock,
                unconstrainedStock,
                constrainedConsumption,
                unconstrainedConsumption);

    }

    /** Adds one already-materialized physical series to the report aggregate without another projection read. */
    private void addToAggregate(double[] aggregate, double[] materialLocationSeries) {

        for (int periodIndex = 0; periodIndex < aggregate.length; periodIndex++) {

            aggregate[periodIndex] += materialLocationSeries[periodIndex];
        }

    }

    /** Includes only assigned values; a characteristic can legitimately not apply to a given location. */
    private Map<String, String> getLocationCharacteristicValues(
            Location location,
            CommunityInventoryOverviewProjectionContext projectionContext) {

        Map<String, String> values = new LinkedHashMap<>();
        projectionContext.locationCharacteristics().forEach((characteristicId, characteristic) ->
                characteristic.findValorCaracteristicaDeLocation(location)
                        .ifPresent(value -> values.put(characteristicId, value)));
        return values;

    }

    /** Includes only assigned values; a characteristic can legitimately not apply to a given material. */
    private Map<String, String> getMaterialCharacteristicValues(
            Produto material,
            CommunityInventoryOverviewProjectionContext projectionContext) {

        Map<String, String> values = new LinkedHashMap<>();
        projectionContext.materialCharacteristics().forEach((characteristicId, characteristic) ->
                characteristic.findValorCaracteristicaDeProduto(material)
                        .ifPresent(value -> values.put(characteristicId, value)));
        return values;

    }

    /**
     * Exclui deliberadamente demanda indireta e transferências: ambas não são
     * consumo final do saldo da location observada.
     */
    private double getStockConsumption(
            SupplyPlanningProjection locationProjection,
            int period,
            Produto material,
            Constantes.TipoPlano planType,
            CommunityInventoryOverviewProjectionContext projectionContext) {

        return SupplyPlanning.getDemandaDiretaConsideradaParaEstoqueProjetado(
                locationProjection,
                period,
                material,
                planType,
                projectionContext.targetUnitOfMeasure())
                + locationProjection.getQuantidadeMaterialInputConsumidoNoProductionPlan(
                        period,
                        material,
                        Constantes.FirmePlanejado.TOTAL,
                        planType,
                        projectionContext.targetUnitOfMeasure());

    }

    /** Empacota cada bucket completo, preservando períodos zerados para a leitura temporal. */
    private List<CommunityInventoryOverviewPeriodDTO> getPeriodDTOs(
            Calendario calendar,
            int firstFuturePeriod,
            double[] constrainedStock,
            double[] unconstrainedStock,
            double[] constrainedCoverageDays,
            double[] unconstrainedCoverageDays) {

        List<CommunityInventoryOverviewPeriodDTO> periods = new ArrayList<>();
        for (int displayedPeriodIndex = 0; displayedPeriodIndex < constrainedStock.length; displayedPeriodIndex++) {

            int calendarPeriod = firstFuturePeriod + displayedPeriodIndex;
            periods.add(new CommunityInventoryOverviewPeriodDTO(
                    calendar.getListaDatasHorarios().get(calendarPeriod),
                    constrainedStock[displayedPeriodIndex],
                    unconstrainedStock[displayedPeriodIndex],
                    constrainedCoverageDays[displayedPeriodIndex],
                    unconstrainedCoverageDays[displayedPeriodIndex]));
        }
        return periods;

    }

    /** Resultado físico e contexto transitório reutilizável apenas no overlay da mesma requisição. */
    public record CommunityInventoryOverviewPhysicalRead(
            CommunityInventoryOverviewDTO inventoryOverview,
            CommunityInventoryOverviewProjectionContext projectionContext) {
    }

}
