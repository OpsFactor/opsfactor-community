package com.opsfactor.community.capability.demandplanning.configuration.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosArima;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosChronos;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosExponentialSmoothing;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosHoltWinters;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosLimpezaHistoricoForecast;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosMediaMovel;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosProphet;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Getter;
import lombok.Setter;

/**
 * Projection de parametros de forecast usada pelo workflow Community e pelos
 * overlays Enterprise.
 *
 * <p>O Community executa apenas os modelos estatisticos aprovados no catalogo
 * aberto, mas a projection tambem carrega contêineres transicionais de
 * parametros Enterprise, como Prophet e Chronos. Esses objetos existem para que
 * entidades compartilhadas e overlays {@code @Primary} do Enterprise possam
 * reutilizar o mesmo contrato de dados. No Community puro, qualquer tentativa
 * de salvar ou executar um modelo Enterprise deve falhar antes, nos
 * mappers/services de borda, com {@code RequiresEnterpriseVersionException}.</p>
 */
@Getter
@Setter
public class ParametrosForecastProjection {

    /**
     * Modelo estatistico/foundation model selecionado para a rodada.
     */
    public Constantes.DPModeloEstatistico dpModeloEstatistico;

    /**
     * Parametros de Moving Average, modelo Community.
     */
    public ParametrosMediaMovel parametrosMediaMovel;

    /**
     * Parametros de Exponential Smoothing, modelo Community.
     */
    public ParametrosExponentialSmoothing parametrosExponentialSmoothing;

    /**
     * Parametros de Holt-Winters, modelo Community.
     */
    public ParametrosHoltWinters parametrosHoltWinters;

    /**
     * Parametros de ARIMA, modelo Community.
     */
    public ParametrosArima parametrosArima;

    /**
     * Parametros Prophet carregados apenas como contrato compartilhado com o
     * Enterprise. O Community nao executa Prophet.
     */
    public ParametrosProphet parametrosProphet;

    /**
     * Parametros Chronos/foundation model carregados apenas como contrato
     * compartilhado com o Enterprise. O Community nao executa Chronos.
     */
    public ParametrosChronos parametrosChronos;

    /**
     * Parametros de tratamento historico. No Community ficam neutros; o
     * Enterprise pode aplicar percentis/campanhas por overlay.
     */
    public ParametrosLimpezaHistoricoForecast parametrosLimpezaHistoricoForecast;

    /**
     * Modelo de split ate DFU. No Community apenas Historical Sales executa.
     */
    public Constantes.DPModeloSplit dpModeloSplit;

    /**
     * Modelo de uplift pos-forecast.
     *
     * <p>O Community carrega apenas {@code DESATIVADO}. O campo fica nesta
     * projection compartilhada porque o Enterprise calcula Event Uplift depois
     * do forecast/desagregacao usando a mesma unidade de execucao.</p>
     */
    public Constantes.DPModeloUplift modeloUplift;

    /**
     * Janela historica usada pelo split top-down por Historical Sales.
     *
     * <p>{@code null} nos construtores significa usar o default publico. Valor
     * preenchido precisa ser positivo; zero dias transformaria o split em uma
     * distribuicao uniforme acidental, escondendo configuracao invalida antes
     * da desagregacao.</p>
     */
    public int numeroDiasSplitTopDown;

    /*
     * IDs Enterprise de nivel de agregacao MAPE vindos do perfil de execucao.
     *
     * O Community carrega estes campos apenas como metadados transicionais da
     * projection compartilhada. A borda Community nao permite salvar valores e
     * nenhum split Community le esses ids. O Enterprise usa os ids para carregar
     * NivelAgregacaoMaterial/Location privados antes de preparar HTS/MAPE.
     */
    private String nivelAgregacaoMaterialMapeId;
    private String nivelAgregacaoLocationMapeId;

    public ParametrosForecastProjection(
            Constantes.DPModeloEstatistico dpModeloEstatistico,
            ParametrosMediaMovel parametrosMediaMovel,
            ParametrosExponentialSmoothing parametrosExponentialSmoothing,
            ParametrosHoltWinters parametrosHoltWinters,
            ParametrosArima parametrosArima,
            Constantes.DPModeloSplit dpModeloSplit,
            Integer numeroDiasSplitTopDown) {

        this(
                dpModeloEstatistico,
                parametrosMediaMovel,
                parametrosExponentialSmoothing,
                parametrosHoltWinters,
                parametrosArima,
                null,
                null,
                null,
                dpModeloSplit,
                numeroDiasSplitTopDown);

    }

    public ParametrosForecastProjection(
            Constantes.DPModeloEstatistico dpModeloEstatistico,
            ParametrosMediaMovel parametrosMediaMovel,
            ParametrosExponentialSmoothing parametrosExponentialSmoothing,
            ParametrosHoltWinters parametrosHoltWinters,
            ParametrosArima parametrosArima,
            ParametrosProphet parametrosProphet,
            Constantes.DPModeloSplit dpModeloSplit,
            Integer numeroDiasSplitTopDown) {

        this(
                dpModeloEstatistico,
                parametrosMediaMovel,
                parametrosExponentialSmoothing,
                parametrosHoltWinters,
                parametrosArima,
                parametrosProphet,
                null,
                null,
                dpModeloSplit,
                numeroDiasSplitTopDown);

    }

    public ParametrosForecastProjection(
            Constantes.DPModeloEstatistico dpModeloEstatistico,
            ParametrosMediaMovel parametrosMediaMovel,
            ParametrosExponentialSmoothing parametrosExponentialSmoothing,
            ParametrosHoltWinters parametrosHoltWinters,
            ParametrosArima parametrosArima,
            ParametrosProphet parametrosProphet,
            ParametrosChronos parametrosChronos,
            Constantes.DPModeloSplit dpModeloSplit,
            Integer numeroDiasSplitTopDown) {

        this(
                dpModeloEstatistico,
                parametrosMediaMovel,
                parametrosExponentialSmoothing,
                parametrosHoltWinters,
                parametrosArima,
                parametrosProphet,
                parametrosChronos,
                null,
                dpModeloSplit,
                numeroDiasSplitTopDown);

    }

    public ParametrosForecastProjection(
            Constantes.DPModeloEstatistico dpModeloEstatistico,
            ParametrosMediaMovel parametrosMediaMovel,
            ParametrosExponentialSmoothing parametrosExponentialSmoothing,
            ParametrosHoltWinters parametrosHoltWinters,
            ParametrosArima parametrosArima,
            ParametrosProphet parametrosProphet,
            ParametrosChronos parametrosChronos,
            ParametrosLimpezaHistoricoForecast parametrosLimpezaHistoricoForecast,
            Constantes.DPModeloSplit dpModeloSplit,
            Integer numeroDiasSplitTopDown) {

        this(
                dpModeloEstatistico,
                parametrosMediaMovel,
                parametrosExponentialSmoothing,
                parametrosHoltWinters,
                parametrosArima,
                parametrosProphet,
                parametrosChronos,
                parametrosLimpezaHistoricoForecast,
                null,
                dpModeloSplit,
                numeroDiasSplitTopDown);

    }

    public ParametrosForecastProjection(
            Constantes.DPModeloEstatistico dpModeloEstatistico,
            ParametrosMediaMovel parametrosMediaMovel,
            ParametrosExponentialSmoothing parametrosExponentialSmoothing,
            ParametrosHoltWinters parametrosHoltWinters,
            ParametrosArima parametrosArima,
            ParametrosProphet parametrosProphet,
            ParametrosChronos parametrosChronos,
            ParametrosLimpezaHistoricoForecast parametrosLimpezaHistoricoForecast,
            Constantes.DPModeloUplift modeloUplift,
            Constantes.DPModeloSplit dpModeloSplit,
            Integer numeroDiasSplitTopDown) {

        this.dpModeloEstatistico = (dpModeloEstatistico == null) ? Constantes.DPModeloEstatistico.MM : dpModeloEstatistico;
        this.parametrosMediaMovel = parametrosMediaMovel;
        this.parametrosExponentialSmoothing = parametrosExponentialSmoothing;
        this.parametrosHoltWinters = parametrosHoltWinters;
        this.parametrosArima = parametrosArima;
        this.parametrosProphet = new ParametrosProphet(parametrosProphet);
        this.parametrosChronos = new ParametrosChronos(parametrosChronos);
        this.parametrosLimpezaHistoricoForecast =
                new ParametrosLimpezaHistoricoForecast(
                        parametrosLimpezaHistoricoForecast,
                        new ParametrosGlobais());
        this.modeloUplift = (modeloUplift == null) ? Constantes.DPModeloUplift.DESATIVADO : modeloUplift;
        this.dpModeloSplit = (dpModeloSplit == null) ? Constantes.DPModeloSplit.HISTORICAL_SALES : dpModeloSplit;
        this.numeroDiasSplitTopDown =
                getNumeroDiasSplitTopDown(numeroDiasSplitTopDown);

    }

    /**
     * Construtor usado na execucao de forecast Community em nivel cluster.
     *
     * @param parametrosModeloEstatisticoAbstract configuracao estatistica
     *                                           cadastrada para o cluster
     * @param parametrosGlobais parametros globais usados em regras derivadas,
     *                          como limpeza historica
     */
    public ParametrosForecastProjection(
            ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract,
            ParametrosGlobais parametrosGlobais) {

        this(
                parametrosModeloEstatisticoAbstract.getDpModeloEstatistico(),
                new ParametrosMediaMovel(parametrosModeloEstatisticoAbstract),
                new ParametrosExponentialSmoothing(parametrosModeloEstatisticoAbstract),
                new ParametrosHoltWinters(parametrosModeloEstatisticoAbstract),
                new ParametrosArima(),
                new ParametrosProphet(parametrosModeloEstatisticoAbstract),
                new ParametrosChronos(parametrosModeloEstatisticoAbstract),
                new ParametrosLimpezaHistoricoForecast(
                        parametrosModeloEstatisticoAbstract,
                        parametrosGlobais),
                parametrosModeloEstatisticoAbstract.getModeloUplift(),
                parametrosModeloEstatisticoAbstract.getDpModeloSplit(),
                parametrosModeloEstatisticoAbstract.getNumeroDiasSplitTopDown());

    }

    public ParametrosForecastProjection(
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosGlobais parametrosGlobais) {

        this(
                parametrosForecastProjection.getDpModeloEstatistico(),
                (parametrosForecastProjection.getParametrosMediaMovel() == null) ? null : new ParametrosMediaMovel(parametrosForecastProjection.getParametrosMediaMovel()),
                (parametrosForecastProjection.getParametrosExponentialSmoothing() == null) ? null : new ParametrosExponentialSmoothing(parametrosForecastProjection.getParametrosExponentialSmoothing()),
                (parametrosForecastProjection.getParametrosHoltWinters() == null) ? null : new ParametrosHoltWinters(parametrosForecastProjection.getParametrosHoltWinters()),
                (parametrosForecastProjection.getParametrosArima() == null) ? null : parametrosForecastProjection.getParametrosArima(),
                (parametrosForecastProjection.getParametrosProphet() == null) ? null : new ParametrosProphet(parametrosForecastProjection.getParametrosProphet()),
                (parametrosForecastProjection.getParametrosChronos() == null) ? null : new ParametrosChronos(parametrosForecastProjection.getParametrosChronos()),
                parametrosForecastProjection.getParametrosLimpezaHistoricoForecast() == null
                        ? null
                        : new ParametrosLimpezaHistoricoForecast(
                        parametrosForecastProjection.getParametrosLimpezaHistoricoForecast(),
                        parametrosGlobais),
                parametrosForecastProjection.getModeloUplift(),
                parametrosForecastProjection.getDpModeloSplit(),
                parametrosForecastProjection.getNumeroDiasSplitTopDown()
        );
        this.nivelAgregacaoMaterialMapeId =
                parametrosForecastProjection.getNivelAgregacaoMaterialMapeId();
        this.nivelAgregacaoLocationMapeId =
                parametrosForecastProjection.getNivelAgregacaoLocationMapeId();

    }

    private static int getNumeroDiasSplitTopDown(
            Integer numeroDiasSplitTopDown) {

        if (numeroDiasSplitTopDown == null) {
            return Constantes.DP_PADRAO_DIAS_HISTORICOS_TOP_DOWN;
        }
        if (numeroDiasSplitTopDown <= 0) {
            throw new IllegalArgumentException(
                    "Historical Sales split reference window must be positive.");
        }

        return numeroDiasSplitTopDown;

    }

}
