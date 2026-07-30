package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.platform.bi.service.CommunityInventoryOverviewCoverageCalculator;
import com.opsfactor.community.platform.bi.service.CommunityInventoryOverviewSnapshotFactory;
import com.opsfactor.community.platform.bi.service.CommunityInventoryOverviewSnapshotFactory.CommunityInventoryOverviewSnapshot;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewPeriodDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewSelectionDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
    private CommunityInventoryOverviewSnapshotFactory inventoryOverviewSnapshotFactory;

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

        CommunityInventoryOverviewSnapshot snapshot = inventoryOverviewSnapshotFactory.createSnapshot(selectionDTO);
        Calendario calendar = snapshot.calendar();
        int firstFuturePeriod = calendar.getPosicaoPeriodoPresente();
        int finalFuturePeriod = calendar.getPosicaoPeriodoFinalFuturo();
        int numberOfDisplayedPeriods = finalFuturePeriod - firstFuturePeriod + 1;
        double[] constrainedStock = new double[numberOfDisplayedPeriods];
        double[] unconstrainedStock = new double[numberOfDisplayedPeriods];
        double[] constrainedConsumption = new double[numberOfDisplayedPeriods];
        double[] unconstrainedConsumption = new double[numberOfDisplayedPeriods];
        double[] daysByPeriod = new double[numberOfDisplayedPeriods];

        for (int period = firstFuturePeriod; period <= finalFuturePeriod; period++) {

            daysByPeriod[period - firstFuturePeriod] = calendar.getNumeroDiasNoPeriodo(period);
        }

        for (Location location : snapshot.eligibleLocations()) {

            SupplyPlanningProjection locationProjection = snapshot.supplyPlanningBiProjection()
                    .getSupplyPlanningProjection(location, snapshot.materialProjection());
            ClusterEParametrosProjection clusterAndParametersProjection = locationProjection
                    .getSupplyNetworkProjection()
                    .getClusterEParametrosProjection();
            for (Produto material : snapshot.materialProjection().getMateriaisAtivos()) {

                if (!clusterAndParametersProjection.isDfuAtiva(material, location)) {
                    continue;
                }
                addMaterialLocationSeries(
                        locationProjection,
                        material,
                        snapshot,
                        firstFuturePeriod,
                        finalFuturePeriod,
                        constrainedStock,
                        unconstrainedStock,
                        constrainedConsumption,
                        unconstrainedConsumption);
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
                snapshot.targetUnitOfMeasure().getId(),
                getPeriodDTOs(
                        calendar,
                        firstFuturePeriod,
                        constrainedStock,
                        unconstrainedStock,
                        constrainedCoverageDays,
                        unconstrainedCoverageDays));
        return new CommunityInventoryOverviewPhysicalRead(inventoryOverview, snapshot);

    }

    /** Soma estoque fechado e somente os dois fluxos físicos que o consomem. */
    private void addMaterialLocationSeries(
            SupplyPlanningProjection locationProjection,
            Produto material,
            CommunityInventoryOverviewSnapshot snapshot,
            int firstFuturePeriod,
            int finalFuturePeriod,
            double[] constrainedStock,
            double[] unconstrainedStock,
            double[] constrainedConsumption,
            double[] unconstrainedConsumption) {

        for (int period = firstFuturePeriod; period <= finalFuturePeriod; period++) {

            int displayedPeriodIndex = period - firstFuturePeriod;
            constrainedStock[displayedPeriodIndex] += locationProjection.getQuantidadeEstoqueProjetado(
                    period,
                    material,
                    Constantes.TipoPlano.PLANO_RESTRITO,
                    snapshot.targetUnitOfMeasure());
            unconstrainedStock[displayedPeriodIndex] += locationProjection.getQuantidadeEstoqueProjetado(
                    period,
                    material,
                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                    snapshot.targetUnitOfMeasure());
            constrainedConsumption[displayedPeriodIndex] += getStockConsumption(
                    locationProjection,
                    period,
                    material,
                    Constantes.TipoPlano.PLANO_RESTRITO,
                    snapshot);
            unconstrainedConsumption[displayedPeriodIndex] += getStockConsumption(
                    locationProjection,
                    period,
                    material,
                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                    snapshot);
        }

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
            CommunityInventoryOverviewSnapshot snapshot) {

        return SupplyPlanning.getDemandaDiretaConsideradaParaEstoqueProjetado(
                locationProjection,
                period,
                material,
                planType,
                snapshot.targetUnitOfMeasure())
                + locationProjection.getQuantidadeMaterialInputConsumidoNoProductionPlan(
                        period,
                        material,
                        Constantes.FirmePlanejado.TOTAL,
                        planType,
                        snapshot.targetUnitOfMeasure());

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
            CommunityInventoryOverviewSnapshot snapshot) {
    }

}
