package com.opsfactor.community.capability.demandplanning.demandplan.projection;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Projection agregada de forecast.
 *
 * <p>O agregado contem filhos que podem ser series material/location ou outros
 * agregados. Isso permite representar top-down em material, location ou ambos,
 * e tambem deixa o caminho aberto para estruturas Enterprise mais profundas
 * sem mudar o contrato dos consumidores Community.</p>
 */
@Getter
public class DemandPlanForecastProjectionAgregado extends DemandPlanForecastProjection {

    /*
     * Filhos usados para consolidar snapshots de historico e forecast.
     *
     * A lista continua mutavel porque a factory monta a hierarquia em etapas:
     * cria o agregado, adiciona seus filhos e depois recalcula as series
     * agregadas. A referencia, contudo, nao deve ser substituida fora da classe.
     */
    private final List<DemandPlanForecastProjection> demandPlanForecastProjectionDesagregados = new ArrayList<>();

    public DemandPlanForecastProjectionAgregado(
            Calendario calendario,
            UnidadeMedida unidadeMedida,
            boolean preencheHorizonteForecastComDemandaHistorica) {
        super(calendario, unidadeMedida, preencheHorizonteForecastComDemandaHistorica);
    }

    @Override
    public List<DemandPlanForecastProjectionMaterialLocation> getDemandPlanForecastProjectionMaterialLocationList() {

        /*
         * A lista de filhos e mutavel porque a factory monta a hierarquia em
         * etapas. Justamente por isso, a projection agregada valida cada item na
         * hora de navegar a arvore, transformando snapshot corrompido em erro de
         * contrato em vez de NullPointerException ou ClassCastException.
         */
        List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationList =
                new ArrayList<>();
        for (DemandPlanForecastProjection demandPlanForecastProjectionDesagregado :
                demandPlanForecastProjectionDesagregados) {
            if (demandPlanForecastProjectionDesagregado instanceof DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation) {
                demandPlanForecastProjectionMaterialLocationList.add(demandPlanForecastProjectionMaterialLocation);
            } else if (demandPlanForecastProjectionDesagregado instanceof DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado) {
                demandPlanForecastProjectionMaterialLocationList.addAll(
                        demandPlanForecastProjectionAgregado.getDemandPlanForecastProjectionMaterialLocationList());
            } else {
                throw new IllegalStateException(
                        "DemandPlanForecastProjectionAgregado recebeu filho de tipo nao suportado "
                                + demandPlanForecastProjectionDesagregado.getClass().getName()
                                + " ao listar folhas material/location.");
            }
        }
        return demandPlanForecastProjectionMaterialLocationList;

    }

    /**
     * Agrega venda historica tratada e forecast a partir das projections mais
     * desagregadas e sobe recursivamente ate este agregado.
     */
    @Override
    public void agregaForecastEDemandaHistoricaDemandPlanForecastProjectionAPartirNivelDesagregado() {

        /*
         * A agregacao e snapshot, nao view viva. Antes de recalcular a partir
         * dos filhos, zeramos as series do agregado para que uma segunda chamada
         * explicita nao duplique historico ou forecast ja consolidados.
         */
        zeraSeriesAgregadasAntesDeRecalcular();

        // faz a consolidação do nível N+1 no nível N
        for (DemandPlanForecastProjection demandPlanForecastProjectionDesagregado :
                demandPlanForecastProjectionDesagregados) {
            // consolida níveis N+3 em N+2, N+2 em N+1, etc
            demandPlanForecastProjectionDesagregado.agregaForecastEDemandaHistoricaDemandPlanForecastProjectionAPartirNivelDesagregado();

            for (int periodo = 0; periodo < demanda.length; periodo++) {
                demanda[periodo] += demandPlanForecastProjectionDesagregado.demanda[periodo];
            }

            for (int periodo = 0; periodo < vendaHistoricaTratamentoStockouts.length; periodo++) {
                vendaHistoricaTratamentoStockouts[periodo] +=
                        demandPlanForecastProjectionDesagregado.vendaHistoricaTratamentoStockouts[periodo];
                vendaHistoricaTratamentoOutliers[periodo] +=
                        demandPlanForecastProjectionDesagregado.vendaHistoricaTratamentoOutliers[periodo];
            }

            for (int periodo = 0; periodo < forecastBaseline.length; periodo++) {
                forecastBaseline[periodo] +=
                        demandPlanForecastProjectionDesagregado.forecastBaseline[periodo];
                forecastUplift[periodo] +=
                        demandPlanForecastProjectionDesagregado.forecastUplift[periodo];
                forecastAjusteDemanda[periodo] +=
                        demandPlanForecastProjectionDesagregado.forecastAjusteDemanda[periodo];
            }
        }
    }

    private void zeraSeriesAgregadasAntesDeRecalcular() {


        Arrays.fill(demanda, 0.0d);
        Arrays.fill(vendaHistoricaTratamentoStockouts, 0.0d);
        Arrays.fill(vendaHistoricaTratamentoOutliers, 0.0d);
        Arrays.fill(forecastBaseline, 0.0d);
        Arrays.fill(forecastUplift, 0.0d);
        Arrays.fill(forecastAjusteDemanda, 0.0d);

    }

}
