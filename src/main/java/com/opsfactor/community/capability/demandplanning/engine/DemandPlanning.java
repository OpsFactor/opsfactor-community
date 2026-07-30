package com.opsfactor.community.capability.demandplanning.engine;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.HistoricoDemandPlanItem;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosGeraisDemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionAgregado;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.capability.demandplanning.forecast.disaggregation.engine.DemandForecastDisaggregationSpi;
import com.opsfactor.community.capability.demandplanning.forecast.disaggregation.engine.HistoricalSalesForecastDisaggregation;
import com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine.ArimaForecastEngine;
import com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine.DemandForecastStatisticalEngineSpi;
import com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine.ExponentialSmoothingForecastEngine;
import com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine.HoltWintersForecastEngine;
import com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine.MovingAverageForecastEngine;
import com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine.RollingMovingAverageForecastEngine;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.StatusProduto;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Rotinas puras de Demand Planning usadas pelo workflow Community.
 *
 * <p>Esta classe permanece estatica por compatibilidade com o legado migrado,
 * mas a orquestracao nova fica em `DemandForecastWorkflowService`. Por isso,
 * ela nao deve guardar estado de rodada, registrar beans ou escolher
 * comportamento por edicao. A selecao aqui e apenas uma fachada transicional
 * para os modelos estatisticos e o split Historical Sales disponiveis no
 * OpsFactor Community.</p>
 *
 * <p>O Enterprise deve substituir o workflow Spring com `@Primary` quando
 * precisar de foundation models, splits compostos ou engines privadas, sem
 * transformar esta rotina pura em registry global.</p>
 */
@Slf4j
public class DemandPlanning {

    /**
     * Executa o modelo estatistico Community sobre a unidade de forecast recebida.
     *
     * <p>Apesar do nome historico do metodo ainda falar em "Agregado", a unidade
     * pode ser uma projection agregada top-down ou uma projection
     * material/location bottom-up. A engine escreve diretamente na projection;
     * a etapa posterior do workflow decide se precisa desagregar.</p>
     */
    public static void geraForecastAgregadoNoDemandPlanForecastProjection(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            DemandPlanForecastProjection demandPlanForecastProjection) {

        DemandForecastStatisticalEngineSpi demandForecastStatisticalEngine =
                getCommunityDemandForecastStatisticalEngine(parametrosForecastProjection.getDpModeloEstatistico());
        demandForecastStatisticalEngine.executaForecast(
                calendario,
                parametrosForecastProjection,
                demandPlanForecastProjection);

    }

    /**
     * Indica se a engine estatistica Community precisa abrir o resultado
     * agregado ate material/location.
     *
     * <p>O workflow Spring usa este metodo antes de desagregar para que a
     * decisao venha do contrato da engine, nao apenas do tipo concreto da
     * projection. A selecao da engine continua nesta fachada estatica
     * transicional para evitar registry global ou beans vazios na camada de
     * routines Community.</p>
     */
    public static boolean requerDesagregacaoForecast(
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast) {

        DemandForecastStatisticalEngineSpi demandForecastStatisticalEngine =
                getCommunityDemandForecastStatisticalEngine(parametrosForecastProjection.getDpModeloEstatistico());
        return demandForecastStatisticalEngine.requerDesagregacao(
                parametrosForecastProjection,
                parametrosAgregacaoForecast);

    }

    /**
     * Seleciona a engine estatistica Community sem usar registry global.
     *
     * <p>A selecao fica aqui porque `DemandPlanning` ainda e uma fachada
     * estatica transicional chamada pelo workflow Spring Community. Quando o
     * Enterprise trouxer implementacoes reais de foundation model ou modelos
     * estatisticos Enterprise, ele podera substituir o workflow com `@Primary` sem
     * transformar esta rotina pura em registry global.</p>
     */
    private static DemandForecastStatisticalEngineSpi getCommunityDemandForecastStatisticalEngine(
            Constantes.DPModeloEstatistico dpModeloEstatistico) {

        if (dpModeloEstatistico == null) {
            throw new IllegalArgumentException(
                    "Demand Planning Forecast Model is required for Community statistical forecast execution.");
        }

        return switch (dpModeloEstatistico) {
            case MM -> new MovingAverageForecastEngine();
            case RMM -> new RollingMovingAverageForecastEngine();
            case ARIMA -> new ArimaForecastEngine();
            case HOLT_WINTERS -> new HoltWintersForecastEngine();
            case ES -> new ExponentialSmoothingForecastEngine();
            case SNAIVE, STL, PROPHET, ETS, TBATS, BUDGET_DECOMPOSITION, CHRONOS, PRICING_ML ->
                    throw new RequiresEnterpriseVersionException("Demand Planning Forecast Model " + dpModeloEstatistico);
        };

    }

    public static void desagregaForecast(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        desagregaForecast(
                calendario,
                parametrosForecastProjection.getDpModeloSplit(),
                parametrosForecastProjection.getNumeroDiasSplitTopDown(),
                demandPlanForecastProjectionAgregado,
                clusterEParametrosProjection);

    }


    /**
     * Desagrega forecast agregado usando a estrategia Community selecionada.
     *
     * <p>No Community, a unica estrategia permitida e Historical Sales. O metodo
     * recebe explicitamente o split e a janela porque o workflow Enterprise pode
     * reaproveitar a assinatura quando chamar rotinas de apoio sem carregar o
     * objeto completo de parametros estatisticos.</p>
     */
    public static void desagregaForecast(
            Calendario calendario,
            Constantes.DPModeloSplit dpModeloSplit,
            int numeroDiasSplitTopDown,
            DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        DemandForecastDisaggregationSpi demandForecastDisaggregation =
                getCommunityDemandForecastDisaggregation(dpModeloSplit);
        demandForecastDisaggregation.desagregaForecast(
                calendario,
                numeroDiasSplitTopDown,
                demandPlanForecastProjectionAgregado,
                clusterEParametrosProjection);

    }

    /**
     * Seleciona a desagregacao Community sem usar registry global.
     *
     * <p>Historical Sales e a unica estrategia de split do Community. HTS,
     * Forecast Proportion e variantes compostas devem ser adicionadas pelo
     * Enterprise em classes proprias quando o workflow Enterprise substituir o
     * workflow Community com `@Primary`.</p>
     */
    private static DemandForecastDisaggregationSpi getCommunityDemandForecastDisaggregation(
            Constantes.DPModeloSplit dpModeloSplit) {

        if (dpModeloSplit == null) {
            throw new IllegalArgumentException(
                    "Demand Planning Split Model is required for Community forecast disaggregation.");
        }

        return switch (dpModeloSplit) {
            case HISTORICAL_SALES -> new HistoricalSalesForecastDisaggregation();
            case FORECAST_PROPORTION, HTS ->
                    throw new RequiresEnterpriseVersionException("Demand Planning Split Model " + dpModeloSplit);
        };

    }

    /**
     * Cria as series material/location usadas como folhas do forecast de demanda.
     *
     * <p>Este metodo nao puxa Demand Plan existente e nao executa tratamento de
     * stockout/outliers. Ele apenas materializa uma
     * `DemandPlanForecastProjectionMaterialLocation` por combinacao material/location
     * permitida e popula a serie observada `demanda` com historico sell-out. As
     * series tratadas sao criadas depois pelo workflow sobre a projection de
     * execucao, porque a limpeza historica deve acontecer no nivel agregado da
     * rodada quando o forecast for top-down.</p>
     *
     * <p>O calendario do Demand Plan deve ter o mesmo tamanho de bucket do
     * calendario da projection de vendas; caso contrario a conversao temporal do
     * historico para os indices do forecast fica ambigua e o fluxo deve falhar.</p>
     */
    public static List<DemandPlanForecastProjectionMaterialLocation> geraDemandPlanForecastProjectionMaterialLocationListComDemandaHistoricaPopuladaCommunity(
            Calendario calendario,
            LocationProjection locationProjection,
            MaterialProjection materialProjection,
            UnidadeMedida unidadeMedidaPadraoDp,
            boolean usaHistoricoDemandaInativos,
            SalesProjectionLocationMaterialData salesProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            boolean preencheHorizonteForecastComDemandaHistorica) {

        if (calendario == null
                || salesProjection == null
                || salesProjection.getCalendario() == null
                || !Objects.equals(calendario.getTamanhoBucket(), salesProjection.getCalendario().getTamanhoBucket())) {
            throw getIncompatibleDemandPlanningSalesProjectionCalendarException(calendario, salesProjection);
        }

        validaInputsGeracaoForecastMaterialLocationCommunity(
                locationProjection,
                materialProjection,
                unidadeMedidaPadraoDp,
                clusterEParametrosProjection);

        if (locationProjection.getLocationSet().isEmpty()) {
            /*
             * Um recorte sem locations e um resultado funcional valido: nao
             * existe DFU Community a materializar para a rodada. Retornar lista
             * vazia aqui tambem evita acionar queries internas do projection de
             * vendas com uma clausula IN vazia, que alguns bancos rejeitam antes
             * de o fluxo chegar ao filtro de negocio.
             */
            return new ArrayList<>();
        }

        List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationList = new ArrayList<>();

        for (Produto material : salesProjection.getMateriaisComSales()) {
            for (Location location : salesProjection.getLocationsComSales(material)) {

                if (!materialProjection.getMaterialSet().contains(material)
                        || !locationProjection.getLocationSet().contains(location)) {
                    /*
                     * A projection de vendas pode ter sido construida para um
                     * universo mais amplo que o recorte final do Demand Plan.
                     * O Community mantem a unidade de execucao restrita ao
                     * material/location explicitamente selecionado pelo perfil.
                     */
                    continue;
                }

                if (!usaHistoricoDemandaInativos && !clusterEParametrosProjection.isDfuAtiva(material, location)) {
                    /*
                     * Historico de DFUs inativas so entra quando o parametro
                     * funcional permite. Esta decisao acontece antes da criacao
                     * da projection leaf para que agregados posteriores nao
                     * carreguem series que o Community nao deve forecastar.
                     */
                    continue;
                }

                DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation = new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedidaPadraoDp,
                        location,
                        material,
                        preencheHorizonteForecastComDemandaHistorica);
                demandPlanForecastProjectionMaterialLocationList.add(demandPlanForecastProjectionMaterialLocation);

                for (int i=calendario.getPosicaoPeriodoInicialPassado();
                     (preencheHorizonteForecastComDemandaHistorica) ?
                             i <= calendario.getPosicaoPeriodoFinalFuturo() // períodos futuros recebem venda observada (por ex. para avaliação posterior mape/bias)
                             : i < calendario.getPosicaoPeriodoInicialFuturo(); // periodos futuros não recebem venda observada (menor número de buscas no sales projection)
                    i++) {
                    /*
                     * Puxa a venda usando como referencia de periodo o calendario
                     * passado como argumento. Ele pode divergir do calendario da
                     * sales projection, entao a conversao temporal fica dentro
                     * do proprio objeto de sales.
                     */
                    demandPlanForecastProjectionMaterialLocation.demanda[i] += salesProjection.getQuantidadeSales(
                            material, location, calendario, i, unidadeMedidaPadraoDp);
                }

            }
        }
        return demandPlanForecastProjectionMaterialLocationList;
    }

    /**
     * Valida os snapshots estruturais usados para transformar vendas historicas
     * em series material/location.
     *
     * <p>O service Community valida a mesma borda antes de chegar aqui, mas
     * esta rotina estatica ainda e chamada diretamente por testes e por pontos
     * transicionais migrados do legado. Por isso ela tambem precisa falhar com
     * mensagens funcionais quando um snapshot obrigatorio nao foi carregado,
     * em vez de deixar um `NullPointerException` surgir dentro de loops de
     * forecast sem contexto de negocio.</p>
     */
    private static void validaInputsGeracaoForecastMaterialLocationCommunity(
            LocationProjection locationProjection,
            MaterialProjection materialProjection,
            UnidadeMedida unidadeMedidaPadraoDp,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (locationProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning location projection is required for material/location forecast series generation.");
        }
        if (materialProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning material projection is required for material/location forecast series generation.");
        }
        if (unidadeMedidaPadraoDp == null) {
            throw new IllegalArgumentException(
                    "Demand Planning default UOM is required for material/location forecast series generation.");
        }
        if (clusterEParametrosProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning cluster and parameters projection is required for material/location forecast series generation.");
        }
        if (locationProjection.getLocationSet() == null) {
            throw new IllegalStateException(
                    "LocationProjection returned null location set for material/location forecast series generation.");
        }
        if (materialProjection.getMaterialSet() == null) {
            throw new IllegalStateException(
                    "MaterialProjection returned null material set for material/location forecast series generation.");
        }

    }

    public static List<? extends DemandPlanForecastProjection> geraDemandPlanForecastProjectionsExecucaoComDemandaHistoricaPopuladaCommunity(
            List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationList,
            Calendario calendario,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
            boolean preencheHorizonteForecastComDemandaHistorica) {

        UnidadeMedida unidadeMedidaPadraoDp = parametrosGeraisDemandPlanningProjection.getUnidadeMedidaDP();

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        materialProjection,
                        locationProjection,
                        !parametrosGeraisDemandPlanningProjection.isDpUsaHistoricoDemandaInativos(),
                        demandPlanForecastProjectionMaterialLocationList,
                        parametrosGeraisDemandPlanningProjection.parametrosAgregacaoForecast.getMaterialAggregationType(),
                        parametrosGeraisDemandPlanningProjection.parametrosAgregacaoForecast.getLocationAggregationType(),
                        unidadeMedidaPadraoDp,
                        preencheHorizonteForecastComDemandaHistorica);

        return demandPlanForecastProjectionsExecucao;

    }

    public static void redistribuiForecastBaselineTrendSeasonalEntreMateriaisAtivosCommunity(
            List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao,
            Calendario calendario,
            ClusterEParametrosProjection clusterEParametrosProjection,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection) {

        boolean geraForecastParaDescontinuados = parametrosGeraisDemandPlanningProjection
                .isDpGeraForecastParaDescontinuados();

        for (DemandPlanForecastProjection demandPlanForecastProjectionExecucao : demandPlanForecastProjectionsExecucao) {

            // RE-EQUILIBRA FORECAST BASELINE ENTRE MATERIAIS ATIVOS -------------------------------
            for (int i = calendario.getPosicaoPeriodoPresente(); i <= calendario.getPosicaoPeriodoFinalFuturo(); i++) {
                // adiciona dados da chave composta da linha
                LocalDate dataReferencia = calendario.getUltimaDataPeriodo(i); // ex: ultima data semana ou mês
                
                double demandaBaselineTotalPeriodo = 0;
                double demandaBaselineAtivosTotalPeriodo = 0;
                double demandaUpliftTotalPeriodo = 0;
                double demandaUpliftAtivosTotalPeriodo = 0;
                double trendTotalPeriodo = 0;
                double trendAtivosTotalPeriodo = 0;
                double seasonalTotalPeriodo = 0;
                double seasonalAtivosTotalPeriodo = 0;

                for (DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionDesagregado : demandPlanForecastProjectionExecucao.getDemandPlanForecastProjectionMaterialLocationList()) {

                    Location location = demandPlanForecastProjectionDesagregado.getLocation();
                    Produto material = demandPlanForecastProjectionDesagregado.getMaterial();

                    boolean dfuAtiva = clusterEParametrosProjection.isDfuAtiva(material, location);
                    boolean materialDescontinuadoNaLocation = clusterEParametrosProjection.getStatusProduto(material, location, calendario.getPrimeiraDataHorarioPeriodo(i))
                            .equals(StatusProduto.DESCONTINUADO);
                    
                    
                    if (dfuAtiva && 
                            (geraForecastParaDescontinuados ||
                            !materialDescontinuadoNaLocation)) {
                        demandaBaselineAtivosTotalPeriodo += demandPlanForecastProjectionDesagregado.forecastBaseline[i];
                        demandaUpliftAtivosTotalPeriodo += demandPlanForecastProjectionDesagregado.forecastUplift[i];
                        if (demandPlanForecastProjectionDesagregado.trend != null) {
                            trendAtivosTotalPeriodo += demandPlanForecastProjectionDesagregado.trend[i];
                        }
                        if (demandPlanForecastProjectionDesagregado.seasonal != null) {
                            seasonalAtivosTotalPeriodo += demandPlanForecastProjectionDesagregado.seasonal[i];
                        }
                    }
                    demandaBaselineTotalPeriodo += demandPlanForecastProjectionDesagregado.forecastBaseline[i];
                    demandaUpliftTotalPeriodo += demandPlanForecastProjectionDesagregado.forecastUplift[i];
                    if (demandPlanForecastProjectionDesagregado.trend != null) {
                        trendTotalPeriodo += demandPlanForecastProjectionDesagregado.trend[i];
                    }
                    if (demandPlanForecastProjectionDesagregado.seasonal != null) {
                        seasonalTotalPeriodo += demandPlanForecastProjectionDesagregado.seasonal[i];
                    }
                }
                
                for (DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionDesagregado : demandPlanForecastProjectionExecucao.getDemandPlanForecastProjectionMaterialLocationList()) {

                    Location location = demandPlanForecastProjectionDesagregado.getLocation();
                    Produto material = demandPlanForecastProjectionDesagregado.getMaterial();

                    boolean dfuAtiva = clusterEParametrosProjection.isDfuAtiva(material, location);
                    boolean materialDescontinuadoNaLocation = clusterEParametrosProjection.getStatusProduto(material, location, calendario.getPrimeiraDataHorarioPeriodo(i))
                            .equals(StatusProduto.DESCONTINUADO);
                    
                    if (dfuAtiva &&
                            (geraForecastParaDescontinuados ||
                            !materialDescontinuadoNaLocation)) {
                        if (demandaBaselineAtivosTotalPeriodo > 0) {
                            demandPlanForecastProjectionDesagregado.forecastBaseline[i] = demandPlanForecastProjectionDesagregado.forecastBaseline[i] * demandaBaselineTotalPeriodo / demandaBaselineAtivosTotalPeriodo;
                        }
                        if (demandaUpliftAtivosTotalPeriodo > 0) {
                            demandPlanForecastProjectionDesagregado.forecastUplift[i] = demandPlanForecastProjectionDesagregado.forecastUplift[i] * demandaUpliftTotalPeriodo / demandaUpliftAtivosTotalPeriodo;
                        }
                        if (trendAtivosTotalPeriodo > 0) {
                            demandPlanForecastProjectionDesagregado.trend[i] = demandPlanForecastProjectionDesagregado.trend[i] * trendTotalPeriodo / trendAtivosTotalPeriodo;
                        }
                        if (seasonalAtivosTotalPeriodo > 0) {
                            demandPlanForecastProjectionDesagregado.seasonal[i] = demandPlanForecastProjectionDesagregado.seasonal[i] * seasonalTotalPeriodo / seasonalAtivosTotalPeriodo;
                        }
                    } else {
                        demandPlanForecastProjectionDesagregado.forecastBaseline[i] = 0;
                        demandPlanForecastProjectionDesagregado.forecastUplift[i] = 0;
                        if (demandPlanForecastProjectionDesagregado.trend != null) {
                            demandPlanForecastProjectionDesagregado.trend[i] = 0;
                        }
                        if (demandPlanForecastProjectionDesagregado.seasonal != null) {
                            demandPlanForecastProjectionDesagregado.seasonal[i] = 0;
                        }
                    }
                }
            }
            // FIM RE-EQUILIBRA FORECAST BASELINE ENTRE MATERIAIS ATIVOS -------------------------------
        }
    }   
    
    public static void arredondaDemandPlanItemsParaUnidadeVenda(
            Collection<DemandPlanItem> demandPlanItems, 
            Constantes.ModificacaoAgregadaPlano modificacaoAgregadaPlano,
            Constantes.TipoPlano tipoPlano,
            ClusterEParametrosProjection clusterEParametrosProjection, 
            UnidadeMedidaProjection unidadeMedidaProjection) {
        
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        
        for (DemandPlanItem demandPlanItem : demandPlanItems) {
            
            Produto material = demandPlanItem.getProduto();
            UnidadeMedida unidadeMedidaDemandPlanItem = demandPlanItem.getUnidadeMedida(parametrosGlobais);
            UnidadeMedida unidadeMedidaVendas = material.getUnidadeMedidaVendas(parametrosGlobais);
            
            double valorDemandPlanItem = getQuantidadeTotalCommunityDemandPlanItem(demandPlanItem, tipoPlano);
            
            // ex: 1 saco = 30kg
            double multiploNaUnidadeDemandPlanItem = unidadeMedidaProjection.getConversaoParaUnidadeDestino(material, unidadeMedidaVendas, unidadeMedidaDemandPlanItem);

            double quantidadeNaUnidadeVendas = valorDemandPlanItem / multiploNaUnidadeDemandPlanItem;
            quantidadeNaUnidadeVendas = Math.round(quantidadeNaUnidadeVendas);

            double novoValorDemandPlanItem = quantidadeNaUnidadeVendas * multiploNaUnidadeDemandPlanItem;
            demandPlanItem.setQuantidadeTotal(valorDemandPlanItem, tipoPlano, Constantes.ModificacaoAgregadaPlano.PROPORCIONAL_OU_BASELINE);
            
            demandPlanItem.setQuantidadeTotal(novoValorDemandPlanItem, tipoPlano, modificacaoAgregadaPlano);
            
        }
        
    }

    private static IllegalArgumentException getIncompatibleDemandPlanningSalesProjectionCalendarException(
            Calendario calendario,
            SalesProjectionLocationMaterialData salesProjection) {

        return new IllegalArgumentException(
                "DemandPlanning requires the Demand Plan calendar bucket to match the Sales Projection calendar bucket; demand bucket="
                        + getTamanhoBucket(calendario)
                        + ", sales bucket="
                        + getTamanhoBucket(salesProjection)
                        + ". Build the sales projection with the same bucket used by the forecast execution.");

    }

    private static Constantes.TamanhoBucket getTamanhoBucket(Calendario calendario) {

        return calendario == null ? null : calendario.getTamanhoBucket();

    }

    private static Constantes.TamanhoBucket getTamanhoBucket(SalesProjectionLocationMaterialData salesProjection) {

        return salesProjection == null ? null : getTamanhoBucket(salesProjection.getCalendario());

    }

    /**
     * Total funcional do Demand Plan Community para rotinas internas.
     *
     * <p>`DemandPlanItem` ainda possui campos transicionais de Uplift e New
     * Products, e o total generico da entidade preserva esses campos por
     * compatibilidade. Rotinas Community de arredondamento, redistribuicao e
     * persistencia devem usar apenas Baseline + Demand Adjustment.</p>
     */
    private static double getQuantidadeTotalCommunityDemandPlanItem(
            DemandPlanItem demandPlanItem,
            Constantes.TipoPlano tipoPlano) {

        return switch (tipoPlano) {
            case PLANO_IRRESTRITO -> demandPlanItem.getQuantidadeBaseline()
                    + demandPlanItem.getQuantidadeAjusteDemanda();
            case PLANO_RESTRITO, PLANO_TRABALHO -> demandPlanItem.getQuantidadeBaselineAtendida()
                    + demandPlanItem.getQuantidadeAjusteDemandaAtendida();
            default -> throw new IllegalArgumentException("Unsupported Demand Plan type for Community total: " + tipoPlano);
        };

    }
    
    public static boolean verificaSeAjusteDentroHorizonteCongelado(
            LocalDate dataAjuste,
            Collection<Location> locations, Collection<Produto> materiais, 
            Calendario calendarioDemandPlan, 
            ClusterEParametrosProjection clusterEParametrosProjection) {
        
        Optional<Integer> leadTimeEmPeriodosOptional = clusterEParametrosProjection.getDPHorizonteCongeladoEmPeriodos(locations, materiais, calendarioDemandPlan);
        
        if (leadTimeEmPeriodosOptional.isEmpty()) return true;
        
        int leadTimeEmPeriodos = leadTimeEmPeriodosOptional
                .orElseThrow(() -> new NoSuchElementException(
                        "Lead time congelado deveria estar presente depois da checagem de Optional.empty()."));
        int posicaoPeriodo = calendarioDemandPlan.getPosicaoPeriodo(dataAjuste);
        
        if (calendarioDemandPlan.getPosicaoPeriodoPresente() + leadTimeEmPeriodos > posicaoPeriodo) return false;
        return true;
        
    }

    public static List<DemandPlanItem> geraDemandPlanItemListDeDemandPlanForecastProjectionsExecucao(
            DemandPlan demandPlan,
            List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao,
            Calendario calendario,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        // Extrai demand plans do BD se existirem e os cria se não existirem
        // guarda tudo num mapa para reuso mais à frente
        List<DemandPlanItem> demandPlanItemList = new ArrayList<>();

        for (DemandPlanForecastProjection demandPlanForecastProjectionExecucao : demandPlanForecastProjectionsExecucao) {

            // varre cada elemento desagregado : cria DP linha e anexa ao demand plan
            // se a projection de execucao ja for material/location, retorna lista com ela propria
            for (DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationDesagregado : demandPlanForecastProjectionExecucao.getDemandPlanForecastProjectionMaterialLocationList()) {
                Location location = demandPlanForecastProjectionMaterialLocationDesagregado.getLocation();
                Produto material = demandPlanForecastProjectionMaterialLocationDesagregado.getMaterial();

                // Community salva somente materiais ativos no nivel material/location.
                if (clusterEParametrosProjection.isDfuAtiva(material, location)) {
                        // salva valores do projection no DemandPlan e faz update com valores do
                    // plano anterior (apenas ajuste demanda no Community)
                    for (int i = calendario.getPosicaoPeriodoPresente(); i <= calendario.getPosicaoPeriodoFinalFuturo(); i++) {

                        // adiciona dados da chave composta da linha
                        LocalDateTime dataReferencia = calendario.getUltimaDataHorarioPeriodo(i); // ex: ultima data semana ou mês

                        if (!parametrosGeraisDemandPlanningProjection.isDpGeraForecastParaDescontinuados()
                                && clusterEParametrosProjection.getStatusProduto(material, location, dataReferencia).equals(StatusProduto.DESCONTINUADO)) {
                            /*
                             * A projection pode existir para preservar o
                             * historico e os agregados da rodada, mas a linha
                             * fisica do plano futuro nao deve ser persistida
                             * quando o material/location esta descontinuado e
                             * o parametro global proibe forecast futuro para
                             * descontinuados.
                             */
                            continue;
                        }

                        DemandPlanItem demandPlanItem = new DemandPlanItem(new DemandPlanItem.DemandPlanItemKey(
                                demandPlan, location, material, dataReferencia));

                        demandPlanItem.setUnidadeMedida(demandPlanForecastProjectionExecucao.getUnidadeMedida());
                        // adiciona dados do forecast baseline
                        demandPlanItem.setQuantidadeBaseline((double) demandPlanForecastProjectionMaterialLocationDesagregado.forecastBaseline[i]);
                        demandPlanItem.setQuantidadeUplift((double) demandPlanForecastProjectionMaterialLocationDesagregado.forecastUplift[i]);
                        if (demandPlanForecastProjectionMaterialLocationDesagregado.trend != null && demandPlanForecastProjectionMaterialLocationDesagregado.trend[i] != 0) {
                            demandPlanItem.setQuantidadeBaselineTrend((double) demandPlanForecastProjectionMaterialLocationDesagregado.trend[i]);
                        }
                        if (demandPlanForecastProjectionMaterialLocationDesagregado.seasonal != null && demandPlanForecastProjectionMaterialLocationDesagregado.seasonal[i] != 0) {
                            demandPlanItem.setQuantidadeBaselineSeasonal((double) demandPlanForecastProjectionMaterialLocationDesagregado.seasonal[i]);
                        }

                        // Community neutraliza Uplift logo antes do save. O
                        // valor e copiado da projection aqui para que overlays
                        // Enterprise possam preservar a KF calculada sem
                        // duplicar toda a conversao projection -> entidade.
                        if ((demandPlanItem.getQuantidadeAjusteDemanda() != 0)
                                || (demandPlanItem.getQuantidadeUplift() > 0)
                                || (demandPlanItem.getQuantidadeBaseline() > 0)) {
                            demandPlanItemList.add(demandPlanItem);
                        }
                    } // FIM DO FOR DE PERIODOS
                } // FIM DO IF MATERIAIS ATIVOS
            } // FIM DO FOR DE PROJECTIONS MATERIAL/LOCATION DA UNIDADE DE EXECUCAO
        }
        return demandPlanItemList;
    }

    public static List<HistoricoDemandPlanItem> geraHistoricoDemandPlanItemListDeDemandPlanForecastProjectionsExecucao(
            DemandPlan demandPlan,
            List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao,
            Calendario calendario,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        // Extrai demand plans do BD se existirem e os cria se não existirem
        // guarda tudo num mapa para reuso mais à frente
        List<HistoricoDemandPlanItem> historicoDemandPlanItemList = new ArrayList<>();

        for (DemandPlanForecastProjection demandPlanForecastProjectionExecucao : demandPlanForecastProjectionsExecucao) {

            // varre cada elemento desagregado : cria DP linha e anexa ao demand plan
            // se a projection de execucao ja for material/location, retorna lista com ela propria
            for (DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationDesagregado : demandPlanForecastProjectionExecucao.getDemandPlanForecastProjectionMaterialLocationList()) {
                Location location = demandPlanForecastProjectionMaterialLocationDesagregado.getLocation();
                Produto material = demandPlanForecastProjectionMaterialLocationDesagregado.getMaterial();

                // Community salva historico somente para materiais ativos no nivel material/location.
                if (clusterEParametrosProjection.isDfuAtiva(material, location)) {

                    for (int i = 0; i <= calendario.getPosicaoPeriodoFinalPassado(); i++) {
                        // adiciona dados da chave composta da linha
                        LocalDateTime dataReferencia = calendario.getUltimaDataHorarioPeriodo(i); // ex: ultima data semana ou mês
                        HistoricoDemandPlanItem historicoDemandPlanItem = new HistoricoDemandPlanItem(new HistoricoDemandPlanItem.HistoricoDemandPlanItemKey(
                                demandPlan, location, material, dataReferencia));

                        historicoDemandPlanItem.setUnidadeMedida(demandPlanForecastProjectionExecucao.getUnidadeMedida());

                        historicoDemandPlanItem.setVendaHistoricaTratamentoStockouts(
                                demandPlanForecastProjectionMaterialLocationDesagregado.vendaHistoricaTratamentoStockouts[i]);
                        historicoDemandPlanItem.setVendaHistoricaTratamentoOutliers(
                                demandPlanForecastProjectionMaterialLocationDesagregado.vendaHistoricaTratamentoOutliers[i]);

                        if (demandPlanForecastProjectionMaterialLocationDesagregado.trend != null && demandPlanForecastProjectionMaterialLocationDesagregado.trend[i] != 0) {
                            historicoDemandPlanItem.setQuantidadeTrend(demandPlanForecastProjectionMaterialLocationDesagregado.trend[i]);
                        }
                        if (demandPlanForecastProjectionMaterialLocationDesagregado.seasonal != null && demandPlanForecastProjectionMaterialLocationDesagregado.seasonal[i] != 0) {
                            historicoDemandPlanItem.setQuantidadeSeasonal(demandPlanForecastProjectionMaterialLocationDesagregado.seasonal[i]);
                        }

                        // adiciona linha ao plano
                        if ((historicoDemandPlanItem.getQuantidadeTrendCadastrada() != null && historicoDemandPlanItem.getQuantidadeTrend() != 0)
                                || (historicoDemandPlanItem.getQuantidadeSeasonalCadastrada() != null && historicoDemandPlanItem.getQuantidadeSeasonal() > 0)
                                || (historicoDemandPlanItem.getVendaHistoricaTratamentoStockoutsCadastrada() != null
                                && historicoDemandPlanItem.getVendaHistoricaTratamentoStockouts() > 0)
                                || (historicoDemandPlanItem.getVendaHistoricaTratamentoOutliersCadastrada() != null
                                && historicoDemandPlanItem.getVendaHistoricaTratamentoOutliers() != 0)) {
                            historicoDemandPlanItemList.add(historicoDemandPlanItem);
                        }
                    } // FIM DO FOR DE PERIODOS

                } // FIM DO IF MATERIAIS ATIVOS
            } // FIM DO FOR DE PROJECTIONS MATERIAL/LOCATION DA UNIDADE DE EXECUCAO
        }
        return historicoDemandPlanItemList;
    }

    public static Calendario getCalendarioDemandPlanComPeriodosPassadosEFuturos(
            ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection,
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            LocalDateTime dataHorarioPeriodoInicioHorizonteForecast) {

        int diasHistoricosForecastEstatistico = parametrosDemandPlanNivelClusterProjection
                .getParametrosGeraisDemandPlanningProjection()
                .diasHistoricosForecastEstatistico;
        int diasFuturosHorizonteForecast = perfilExecucaoDemandPlan.getNumeroDiasHorizontePlanejamento(
                dataHorarioPeriodoInicioHorizonteForecast);

        return getCalendarioDemandPlanComPeriodosPassadosEFuturosComInputsEmDias(
                perfilExecucaoDemandPlan.getTamanhoBucket(),
                diasHistoricosForecastEstatistico,
                diasFuturosHorizonteForecast,
                dataHorarioPeriodoInicioHorizonteForecast);

    }

    public static Calendario getCalendarioDemandPlanComPeriodosPassadosEFuturosComInputsEmDias(
            Constantes.TamanhoBucket tamanhoBucket,
            int diasHistoricosForecastEstatistico,
            int diasFuturosHorizonteForecast,
            LocalDateTime dataHorarioPeriodoInicioHorizonteForecast) {

        // Entrada em dias: este helper converte a janela para o calendario operacional do Demand Planning.
        // Granularidade menor que diaria depende de projections de sales especificas.
        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                tamanhoBucket,
                dataHorarioPeriodoInicioHorizonteForecast,
                0, diasHistoricosForecastEstatistico,
                diasFuturosHorizonteForecast, 0);

        return calendario;

    }

    public static Calendario getCalendarioDemandPlanComPeriodosPassadosEFuturosComInputsEmPeriodos(
            Constantes.TamanhoBucket tamanhoBucket,
            int periodosHistoricosForecastEstatistico,
            int periodosFuturosHorizonteForecast,
            LocalDateTime dataHorarioPeriodoInicioHorizonteForecast) {

        // Entrada em periodos: usado quando a propria configuracao ja foi traduzida para buckets.
        // Granularidade menor que diaria depende de projections de sales especificas.
        Calendario calendario = Calendario.criaCalendarioDeOffsetsPeriodos(
                tamanhoBucket,
                dataHorarioPeriodoInicioHorizonteForecast,
                0, periodosHistoricosForecastEstatistico,
                periodosFuturosHorizonteForecast, 0);

        return calendario;

    }


}
