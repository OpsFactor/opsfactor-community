package com.opsfactor.community.capability.demandplanning.facade.mapper;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningClusterLevelConfigurationDTO;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.capability.demandplanning.facade.dto.SimulatedDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.facade.dto.SimulatedDemandPlanMaterialLocationDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Gera um DemandAnalysisDTO com historico de vendas, serie historica tratada
 * e forecast para todas as combinacoes material/location/periodo de dado
 * cluster de locations e cluster de materiais.
 *
 * <p>O tipo Java de material ainda e {@link Produto} porque a entidade JPA
 * permanece transicionalmente no dominio `produto`. O payload Community,
 * entretanto, deve falar sempre em material.</p>
 *
 * <p>No Community, a serie tratada e igual a venda historica observada porque
 * stockout, outlier/event cleaning e uplift sao Enterprise. O DTO expoe as duas
 * etapas historicas de forma explicita para que o front novo consiga mostrar o
 * efeito de cada tratamento quando o overlay Enterprise existir.</p>
 */
@Service
public class DemandAnalysisMapper {
    
    /**
     * Monta as series material/location do DTO de simulacao a partir das
     * projections de forecast e da projection diaria de vendas.
     *
     * <p>O Community usa buckets de sales diarios nesta borda. Uma granularidade
     * menor deve trocar o tipo de projection recebido pelo metodo, preservando
     * este mapper como adaptador do contrato de tela.</p>
     */
    public SimulatedDemandPlanDTO demandPlanProjectionToDemandModelSetupDTO(
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO,
            Calendario calendario,
            List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao,
            SalesProjectionLocationMaterialData salesProjection) {

        validaEntradasDemandAnalysisMapperCommunity(
                demandPlanningClusterLevelConfigurationDTO,
                calendario,
                demandPlanForecastProjectionsExecucao,
                salesProjection);

        UnidadeMedidaProjection unidadeMedidaProjection = salesProjection.getConversaoUnidadeMedidaProjection();

        /*
         * A UOM configurada precisa existir na mesma projection usada para
         * materializar o historico. O mapper nao copia essa unidade para cada
         * linha do DTO, mas falhar aqui evita publicar uma simulacao com
         * configuracao incoerente.
         */
        unidadeMedidaProjection.getUnidadeMedidaFromId(demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.uomId);
        
        // objeto que será retornado
        SimulatedDemandPlanDTO simulatedDemandPlanDTO = new SimulatedDemandPlanDTO();
        
        List<SimulatedDemandPlanMaterialLocationDTO> simulatedDemandPlanMaterialLocationDTOList = new ArrayList<>();
        
        // referência de posição período -> data
        for (int i = 0; i <= calendario.getPosicaoPeriodoFinalFuturo(); i++) {
            simulatedDemandPlanDTO.periodos.add(calendario.getUltimaDataPeriodo(i));
        }

        /*
         * A factory de forecast pode retornar mais de uma projection de
         * execucao, por exemplo quando o overlay Enterprise trabalhar com
         * arvore ou outros escopos agregados. O DTO Community ignora qualquer
         * detalhe agregado e publica somente as series desagregadas
         * material/location contidas em cada projection.
         */
        for (int projectionIndex = 0;
             projectionIndex < demandPlanForecastProjectionsExecucao.size();
             projectionIndex++) {
            DemandPlanForecastProjection demandPlanForecastProjectionExecucao =
                    demandPlanForecastProjectionsExecucao.get(projectionIndex);
            List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationList =
                    demandPlanForecastProjectionExecucao.getDemandPlanForecastProjectionMaterialLocationList();

            for (int materialLocationIndex = 0;
                 materialLocationIndex < demandPlanForecastProjectionMaterialLocationList.size();
                 materialLocationIndex++) {
                DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationDesagregado =
                        demandPlanForecastProjectionMaterialLocationList.get(materialLocationIndex);
                validaDemandPlanForecastProjectionMaterialLocationSimulacaoCommunity(
                        demandPlanForecastProjectionMaterialLocationDesagregado,
                        projectionIndex,
                        materialLocationIndex,
                        calendario);

                Location location = demandPlanForecastProjectionMaterialLocationDesagregado.getLocation();
                Produto material = demandPlanForecastProjectionMaterialLocationDesagregado.getMaterial();

                SimulatedDemandPlanMaterialLocationDTO simulatedDemandPlanMaterialLocationDTO = new SimulatedDemandPlanMaterialLocationDTO();
                simulatedDemandPlanMaterialLocationDTO.setLocationId(location.getId());
                /*
                 * A projection usa Produto por causa do dominio JPA legado; o
                 * DTO exposto ao front novo usa materialId em todo o contrato
                 * Community.
                 */
                simulatedDemandPlanMaterialLocationDTO.setMaterialId(material.getId());

                simulatedDemandPlanMaterialLocationDTOList.add(simulatedDemandPlanMaterialLocationDTO);
                /*
                 * A venda historica observada e copiada no tamanho total do
                 * calendario para manter alinhamento com forecast/residuos no
                 * front. Periodos futuros permanecem zerados quando a projection
                 * nao materializa demanda observada neles.
                 */
                simulatedDemandPlanMaterialLocationDTO.historicalSales = new double[calendario.getNumeroPeriodosTotais()];
                copiaArray(demandPlanForecastProjectionMaterialLocationDesagregado.demanda, simulatedDemandPlanMaterialLocationDTO.historicalSales, 3);
                // Community nao aplica tratamento de stockout Enterprise; a
                // serie permanece igual a venda observada, mas o campo explicito
                // preserva o contrato por etapa para o front novo.
                simulatedDemandPlanMaterialLocationDTO.historicalSalesAfterStockoutTreatment = MetodosUtilidade.roundArray(
                        demandPlanForecastProjectionMaterialLocationDesagregado.vendaHistoricaTratamentoStockouts, 3);
                // Community tambem nao aplica limpeza de outliers/eventos; no
                // Enterprise este sera o ultimo historico antes da engine estatistica.
                simulatedDemandPlanMaterialLocationDTO.historicalSalesAfterOutlierTreatment = MetodosUtilidade.roundArray(
                        demandPlanForecastProjectionMaterialLocationDesagregado.vendaHistoricaTratamentoOutliers, 3);
                // Popula dados de forecast para as series material/location já
                // filtradas na construção da projection de forecast.
                simulatedDemandPlanMaterialLocationDTO.baselineForecast = MetodosUtilidade.roundArray(demandPlanForecastProjectionMaterialLocationDesagregado.forecastBaseline, 3);
                if (demandPlanForecastProjectionMaterialLocationDesagregado.trend != null) {
                    simulatedDemandPlanMaterialLocationDTO.trend = MetodosUtilidade.roundArray(demandPlanForecastProjectionMaterialLocationDesagregado.trend, 3);
                }
                if (demandPlanForecastProjectionMaterialLocationDesagregado.seasonal != null) {
                    simulatedDemandPlanMaterialLocationDTO.seasonal = MetodosUtilidade.roundArray(demandPlanForecastProjectionMaterialLocationDesagregado.seasonal, 3);
                }
                if (demandPlanForecastProjectionMaterialLocationDesagregado.lowerBound != null && demandPlanForecastProjectionMaterialLocationDesagregado.upperBound != null) {
                    simulatedDemandPlanMaterialLocationDTO.lowerBound = MetodosUtilidade.roundArray(demandPlanForecastProjectionMaterialLocationDesagregado.lowerBound, 3);
                    simulatedDemandPlanMaterialLocationDTO.upperBound = MetodosUtilidade.roundArray(demandPlanForecastProjectionMaterialLocationDesagregado.upperBound, 3);
                }
            }
        }
            
        for (SimulatedDemandPlanMaterialLocationDTO simulatedDemandPlanMaterialLocationDTO : simulatedDemandPlanMaterialLocationDTOList) {
            simulatedDemandPlanMaterialLocationDTO.residual = new double[calendario.getNumeroPeriodosTotais()];
            simulatedDemandPlanMaterialLocationDTO.absoluteResidual = new double[calendario.getNumeroPeriodosTotais()];
            for (int i = 0, vhLength = simulatedDemandPlanMaterialLocationDTO.historicalSales.length; i < vhLength; i++) {
                if (simulatedDemandPlanMaterialLocationDTO.baselineForecast == null) continue;
                simulatedDemandPlanMaterialLocationDTO.residual[i] = simulatedDemandPlanMaterialLocationDTO.baselineForecast[i] - simulatedDemandPlanMaterialLocationDTO.historicalSales[i];
                simulatedDemandPlanMaterialLocationDTO.absoluteResidual[i] = simulatedDemandPlanMaterialLocationDTO.residual[i] >= 0 ? simulatedDemandPlanMaterialLocationDTO.residual[i] : simulatedDemandPlanMaterialLocationDTO.residual[i]*(-1);
            }
        }

        simulatedDemandPlanDTO.setMaterialLocationData(simulatedDemandPlanMaterialLocationDTOList);

        return simulatedDemandPlanDTO;
    }

    /**
     * Valida os insumos estruturais do mapper de simulacao.
     *
     * <p>O front service normal valida essas estruturas antes de chegar aqui,
     * mas o mapper e um bean proprio e pode ser chamado diretamente por testes,
     * ferramentas ou overlays Enterprise. Falhar nesta borda evita que uma
     * projection quebrada vaze como `NullPointerException`, `IndexOutOfBounds`
     * ou DTO parcialmente preenchido.</p>
     */
    private void validaEntradasDemandAnalysisMapperCommunity(
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO,
            Calendario calendario,
            List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao,
            SalesProjectionLocationMaterialData salesProjection) {

        if (demandPlanningClusterLevelConfigurationDTO == null) {
            throw new IllegalArgumentException(
                    "Demand Planning simulation configuration is required.");
        }
        if (demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters == null) {
            throw new IllegalArgumentException(
                    "Demand Planning simulation general parameters are required.");
        }
        if (demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.uomId == null
                || demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.uomId.isBlank()) {
            throw new IllegalArgumentException(
                    "Demand Planning simulation unit of measure is required.");
        }
        if (calendario == null) {
            throw new IllegalArgumentException(
                    "Demand Planning simulation calendar is required.");
        }
        if (demandPlanForecastProjectionsExecucao == null) {
            throw new IllegalStateException(
                    "Demand Planning simulation forecast projection list is required.");
        }
        if (salesProjection == null) {
            throw new IllegalStateException(
                    "Demand Planning simulation sales projection is required.");
        }
        if (salesProjection.getConversaoUnidadeMedidaProjection() == null) {
            throw new IllegalStateException(
                    "Demand Planning simulation unit conversion projection is required.");
        }

    }

    /**
     * Obtem as series material/location de uma unidade de forecast validando a
     * colecao retornada pela projection.
     */
    /**
     * Valida a leaf material/location que sera publicada no DTO de simulacao.
     */
    private void validaDemandPlanForecastProjectionMaterialLocationSimulacaoCommunity(
            DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation,
            int projectionIndex,
            int materialLocationIndex,
            Calendario calendario) {

        String contextoSerie =
                "projection "
                        + projectionIndex
                        + ", material/location "
                        + materialLocationIndex;

        if (demandPlanForecastProjectionMaterialLocation == null) {
            throw new IllegalStateException(
                    "Demand Planning simulation forecast " + contextoSerie + " is required.");
        }
        validaLocationMaterialSimulacaoCommunity(
                demandPlanForecastProjectionMaterialLocation,
                contextoSerie);
        validaSeriesSimulacaoCommunity(
                demandPlanForecastProjectionMaterialLocation,
                contextoSerie,
                calendario.getNumeroPeriodosTotais());

    }

    /**
     * Valida a identidade material/location usada como chave da linha no front.
     */
    private void validaLocationMaterialSimulacaoCommunity(
            DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation,
            String contextoSerie) {

        Location location = demandPlanForecastProjectionMaterialLocation.getLocation();
        Produto material = demandPlanForecastProjectionMaterialLocation.getMaterial();

        if (location == null) {
            throw new IllegalStateException(
                    "Demand Planning simulation forecast " + contextoSerie + " has no location.");
        }
        if (location.getId() == null || location.getId().isBlank()) {
            throw new IllegalStateException(
                    "Demand Planning simulation forecast " + contextoSerie + " has location without id.");
        }
        if (material == null) {
            throw new IllegalStateException(
                    "Demand Planning simulation forecast " + contextoSerie + " has no material.");
        }
        if (material.getId() == null || material.getId().isBlank()) {
            throw new IllegalStateException(
                    "Demand Planning simulation forecast " + contextoSerie + " has material without id.");
        }

    }

    /**
     * Valida tamanhos e valores das series publicadas na simulacao.
     *
     * <p>Historicos podem ter apenas a janela passada, mas nunca podem exceder
     * o calendario da tela. Series de forecast e componentes opcionais precisam
     * acompanhar exatamente o horizonte total porque o front usa a mesma lista
     * de periodos para baseline, residuos e faixas.</p>
     */
    private void validaSeriesSimulacaoCommunity(
            DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation,
            String contextoSerie,
            int numeroPeriodosTotais) {

        validaSerieHistoricaSimulacaoCommunity(
                demandPlanForecastProjectionMaterialLocation.demanda,
                "observed historical sales",
                contextoSerie,
                numeroPeriodosTotais);
        validaSerieHistoricaSimulacaoCommunity(
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts,
                "stockout-treated historical sales",
                contextoSerie,
                numeroPeriodosTotais);
        validaSerieHistoricaSimulacaoCommunity(
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers,
                "outlier-treated historical sales",
                contextoSerie,
                numeroPeriodosTotais);

        validaSerieForecastSimulacaoCommunity(
                demandPlanForecastProjectionMaterialLocation.forecastBaseline,
                "baseline forecast",
                contextoSerie,
                numeroPeriodosTotais);
        validaSerieForecastOpcionalSimulacaoCommunity(
                demandPlanForecastProjectionMaterialLocation.trend,
                "trend",
                contextoSerie,
                numeroPeriodosTotais);
        validaSerieForecastOpcionalSimulacaoCommunity(
                demandPlanForecastProjectionMaterialLocation.seasonal,
                "seasonal",
                contextoSerie,
                numeroPeriodosTotais);

        if ((demandPlanForecastProjectionMaterialLocation.lowerBound == null)
                != (demandPlanForecastProjectionMaterialLocation.upperBound == null)) {
            throw new IllegalStateException(
                    "Demand Planning simulation forecast "
                            + contextoSerie
                            + " must provide lower and upper bounds together.");
        }
        validaSerieForecastOpcionalSimulacaoCommunity(
                demandPlanForecastProjectionMaterialLocation.lowerBound,
                "lower bound",
                contextoSerie,
                numeroPeriodosTotais);
        validaSerieForecastOpcionalSimulacaoCommunity(
                demandPlanForecastProjectionMaterialLocation.upperBound,
                "upper bound",
                contextoSerie,
                numeroPeriodosTotais);

    }

    private void validaSerieHistoricaSimulacaoCommunity(
            double[] serie,
            String nomeSerie,
            String contextoSerie,
            int numeroPeriodosTotais) {

        if (serie.length > numeroPeriodosTotais) {
            throw new IllegalStateException(
                    "Demand Planning simulation "
                            + nomeSerie
                            + " series for "
                            + contextoSerie
                            + " cannot be longer than the simulation calendar.");
        }
        validaSerieFinitaSimulacaoCommunity(
                serie,
                nomeSerie,
                contextoSerie);

    }

    private void validaSerieForecastSimulacaoCommunity(
            double[] serie,
            String nomeSerie,
            String contextoSerie,
            int numeroPeriodosTotais) {

        validaSerieForecastOpcionalSimulacaoCommunity(
                serie,
                nomeSerie,
                contextoSerie,
                numeroPeriodosTotais);

    }

    private void validaSerieForecastOpcionalSimulacaoCommunity(
            double[] serie,
            String nomeSerie,
            String contextoSerie,
            int numeroPeriodosTotais) {

        if (serie == null) {
            return;
        }
        if (serie.length != numeroPeriodosTotais) {
            throw new IllegalStateException(
                    "Demand Planning simulation "
                            + nomeSerie
                            + " series for "
                            + contextoSerie
                            + " must match the simulation calendar.");
        }
        validaSerieFinitaSimulacaoCommunity(
                serie,
                nomeSerie,
                contextoSerie);

    }

    private void validaSerieFinitaSimulacaoCommunity(
            double[] serie,
            String nomeSerie,
            String contextoSerie) {

        for (int periodo = 0; periodo < serie.length; periodo++) {
            if (!Double.isFinite(serie[periodo])) {
                throw new IllegalStateException(
                        "Demand Planning simulation "
                                + nomeSerie
                                + " series for "
                                + contextoSerie
                                + " must contain only finite values. Invalid value at period "
                                + periodo
                                + ".");
            }
        }

    }
    
    private void copiaArray(float[] copiarEste, float[] colarNeste, int decimais) {
        for (int i = 0, copiarEsteLength = copiarEste.length; i < copiarEsteLength; i++) {
            float value = copiarEste[i];
            if (decimais > 0) colarNeste[i] = MetodosUtilidade.round(value, decimais);
            else colarNeste[i] = value;
        }
    }

    private void copiaArray(double[] copiarEste, double[] colarNeste, int decimais) {
        for (int i = 0, copiarEsteLength = copiarEste.length; i < copiarEsteLength; i++) {
            double value = copiarEste[i];
            if (decimais > 0) colarNeste[i] = MetodosUtilidade.round(value, decimais);
            else colarNeste[i] = value;
        }
    }

}
