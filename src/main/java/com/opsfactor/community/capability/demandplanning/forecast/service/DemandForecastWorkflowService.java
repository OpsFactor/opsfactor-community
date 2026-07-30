package com.opsfactor.community.capability.demandplanning.forecast.service;

import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionAgregado;
import com.opsfactor.community.capability.demandplanning.engine.DemandPlanning;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastHistoryCleaningProcessor;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastStockoutContext;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastStockoutTreatmentProcessor;
import com.opsfactor.community.platform.calendar.Calendario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Borda Spring do workflow de forecast de Demand Planning Community.
 *
 * <p>O service coordena apenas a execucao atomica sobre uma
 * {@link DemandPlanForecastProjection}: materializa as series historicas
 * tratadas da unidade de execucao, chama a engine estatistica adequada e,
 * quando a factory entregou uma unidade de execucao agregada, chama a
 * desagregacao configurada para popular as series material/location finais.</p>
 *
 * <p>Hoje as engines, processors e desagregacoes ainda sao fachadas/rotinas
 * puras em `com.opsfactor.community.platform.routine`. Este service existe para estabilizar o ponto
 * de extensao Spring: o Enterprise podera substituir esta classe com `@Primary`
 * quando trouxer foundation models, HTS/STL, Forecast Proportion ou workflows
 * compostos.</p>
 *
 * <p>A classe deve permanecer stateless. O estado da rodada fica nas projections
 * recebidas, preservando a execucao paralela por cluster feita a montante.</p>
 */
@Service
public class DemandForecastWorkflowService {

    /**
     * Etapa Community de tratamento de stockouts.
     *
     * <p>No Community esta implementacao e no-op por copia, mas ela e um bean
     * explicito para deixar claro que o workflow possui uma etapa substituivel
     * pelo Enterprise via {@code @Primary}, sem guardar estado entre clusters
     * processados em paralelo.</p>
     */
    @Autowired
    private DemandForecastStockoutTreatmentProcessor demandForecastStockoutTreatmentProcessor;

    /**
     * Etapa Community de limpeza historica de outliers/eventos.
     *
     * <p>No Community esta implementacao tambem e no-op por copia. A injecao
     * explicita evita instanciacao manual dentro do service e documenta a ordem
     * fixa do pipeline: primeiro stockout treatment, depois limpeza historica.</p>
     */
    @Autowired
    private DemandForecastHistoryCleaningProcessor demandForecastHistoryCleaningProcessor;

    /**
     * Executa forecast e desagregacao, mutando a projection recebida.
     */
    public void executaForecastEDesagregacao(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast,
            DemandPlanForecastProjection demandPlanForecastProjection,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        executaForecastEDesagregacao(
                calendario,
                parametrosForecastProjection,
                parametrosAgregacaoForecast,
                demandPlanForecastProjection,
                clusterEParametrosProjection,
                null);

    }

    /**
     * Executa o workflow com contexto historico opcional de stockout.
     *
     * <p>O Community sempre entrega {@code null}, pois seu processor e neutro.
     * O contexto existe para que o Enterprise reutilize o mesmo pipeline sem
     * estado em bean e sem consultar estoque dentro de cada serie.</p>
     */
    public void executaForecastEDesagregacao(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast,
            DemandPlanForecastProjection demandPlanForecastProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            DemandForecastStockoutContext demandForecastStockoutContext) {

        validaParametrosBasicosForecastWorkflow(
                calendario,
                parametrosForecastProjection,
                parametrosAgregacaoForecast,
                demandPlanForecastProjection);

        processaHistoricoForecast(
                calendario,
                demandPlanForecastProjection,
                parametrosForecastProjection,
                demandForecastStockoutContext);

        DemandPlanning.geraForecastAgregadoNoDemandPlanForecastProjection(
                calendario,
                parametrosForecastProjection,
                demandPlanForecastProjection);

        desagregaForecastSeNecessario(
                calendario,
                parametrosForecastProjection,
                parametrosAgregacaoForecast,
                demandPlanForecastProjection,
                clusterEParametrosProjection);

    }

    /**
     * Valida os argumentos obrigatorios antes de qualquer processor, engine ou
     * desagregacao.
     *
     * <p>O metodo e protegido para que overlays Enterprise reutilizem a mesma
     * borda de diagnostico, preservando mensagens funcionais e evitando que
     * parametros ausentes aparecam como {@link NullPointerException} no meio de
     * uma engine privada. A ordem dos checks preserva a compatibilidade das
     * mensagens historicas para payloads completamente vazios.</p>
     */
    protected void validaParametrosBasicosForecastWorkflow(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast,
            DemandPlanForecastProjection demandPlanForecastProjection) {

        if (parametrosForecastProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning forecast parameters are required");
        }
        if (parametrosAgregacaoForecast == null) {
            throw new IllegalArgumentException(
                    "Demand Planning forecast aggregation parameters are required");
        }
        if (parametrosForecastProjection.getDpModeloEstatistico() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning statistical forecast model is required");
        }
        if (parametrosForecastProjection.getDpModeloSplit() == null) {
            throw new IllegalArgumentException(
                    "Demand Planning forecast split model is required");
        }
        if (calendario == null) {
            throw new IllegalArgumentException(
                    "Demand Planning calendar is required");
        }
        if (demandPlanForecastProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning forecast projection is required");
        }

    }

    /**
     * Materializa as series historicas tratadas da unidade de execucao.
     *
     * <p>A limpeza historica roda na unidade de execucao do forecast. Para
     * top-down, isso significa tratar o agregado e desagregar depois. Para
     * bottom-up, a propria projection material/location e a unidade. O metodo
     * existe como hook protegido para o Enterprise reutilizar exatamente a mesma
     * preparacao antes de executar engines privadas, sem copiar processors nem
     * guardar estado entre clusters paralelos.</p>
     */
    protected void processaHistoricoForecast(
            Calendario calendario,
            DemandPlanForecastProjection demandPlanForecastProjection,
            ParametrosForecastProjection parametrosForecastProjection) {

        processaHistoricoForecast(
                calendario,
                demandPlanForecastProjection,
                parametrosForecastProjection,
                null);

    }

    /**
     * Materializa series historicas usando o contexto opcional da rodada.
     */
    protected void processaHistoricoForecast(
            Calendario calendario,
            DemandPlanForecastProjection demandPlanForecastProjection,
            ParametrosForecastProjection parametrosForecastProjection,
            DemandForecastStockoutContext demandForecastStockoutContext) {

        demandForecastStockoutTreatmentProcessor.processa(
                calendario,
                demandPlanForecastProjection,
                parametrosForecastProjection,
                demandForecastStockoutContext);
        demandForecastHistoryCleaningProcessor.processa(
                calendario,
                demandPlanForecastProjection,
                parametrosForecastProjection);

    }

    /**
     * Executa a desagregacao configurada somente quando a unidade de forecast e
     * agregada.
     *
     * <p>No Community, a necessidade de desagregacao e definida pela forma da
     * projection criada pela factory. Bottom-up material/location retorna a
     * propria serie leaf como unidade de execucao; top-down retorna um agregado
     * que precisa redistribuir forecast para as folhas. O Enterprise deve
     * reutilizar este hook enquanto seus modelos privados seguirem o mesmo
     * contrato agregado + split Historical Sales.</p>
     */
    protected void desagregaForecastSeNecessario(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast,
            DemandPlanForecastProjection demandPlanForecastProjection,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        /*
         * A engine decide se sua saida precisa ser aberta ate material/location
         * a partir da configuracao material/location. A projection concreta
         * continua sendo validada aqui para capturar cedo qualquer divergencia
         * entre factory e parametros de agregacao.
         */
        if (!requerDesagregacao(parametrosForecastProjection, parametrosAgregacaoForecast)) {
            validaProjectionNaoAgregadaQuandoDesagregacaoNaoEhNecessaria(demandPlanForecastProjection);
            return;
        }

        if (!(demandPlanForecastProjection instanceof DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado)) {
            throw new IllegalStateException(
                    "Configuracao de forecast requer desagregacao, mas a factory retornou projection nao agregada: "
                            + demandPlanForecastProjection.getClass().getSimpleName());
        }
        validaClusterEParametrosProjectionParaDesagregacao(clusterEParametrosProjection);

        DemandPlanning.desagregaForecast(
                calendario,
                parametrosForecastProjection,
                demandPlanForecastProjectionAgregado,
                clusterEParametrosProjection);

    }

    /**
     * Valida o snapshot estrutural usado por splits top-down.
     *
     * <p>Bottom-up material/location nao precisa consultar DFU ativa nem
     * caracteristicas do cluster. Quando existe desagregacao, entretanto, o
     * split precisa dessa projection para decidir quais folhas podem receber
     * forecast e para preservar o mesmo contrato nos overlays Enterprise de
     * Forecast Proportion e HTS.</p>
     */
    protected void validaClusterEParametrosProjectionParaDesagregacao(
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (clusterEParametrosProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Planning cluster parameters projection is required for forecast disaggregation");
        }

    }

    /**
     * Valida a simetria entre configuracao bottom-up e projection de execucao.
     *
     * <p>No Community, quando a engine declara que nao precisa de desagregacao,
     * a unidade de execucao deve ser uma serie material/location. Receber uma
     * projection agregada nesse caminho indica divergencia entre factory e
     * parametros de agregacao: o forecast ficaria calculado no agregado sem
     * nenhuma etapa posterior para popular as folhas material/location que serao
     * persistidas no Demand Plan.</p>
     *
     * <p>O hook e protegido porque o Enterprise tem uma excecao documentada:
     * foundation models como Chronos podem receber um agregado e escrever
     * diretamente nas folhas, recalculando o snapshot superior sem split.</p>
     */
    protected void validaProjectionNaoAgregadaQuandoDesagregacaoNaoEhNecessaria(
            DemandPlanForecastProjection demandPlanForecastProjection) {

        if (demandPlanForecastProjection instanceof DemandPlanForecastProjectionAgregado) {
            throw new IllegalStateException(
                    "Configuracao de forecast nao requer desagregacao, mas a factory retornou projection agregada: "
                            + demandPlanForecastProjection.getClass().getSimpleName());
        }

    }

    /**
     * Hook de decisao da desagregacao.
     *
     * <p>O Community delega para a engine estatistica selecionada pela fachada
     * transicional `DemandPlanning`. O Enterprise sobrescreve este hook para
     * modelos privados ja migrados, preservando a mesma assinatura e evitando
     * comparacao por edicao ou registry generico.</p>
     */
    protected boolean requerDesagregacao(
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast) {

        return DemandPlanning.requerDesagregacaoForecast(
                parametrosForecastProjection,
                parametrosAgregacaoForecast);

    }

}
