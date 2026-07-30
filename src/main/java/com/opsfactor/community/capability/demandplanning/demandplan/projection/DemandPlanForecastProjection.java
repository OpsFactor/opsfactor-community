package com.opsfactor.community.capability.demandplanning.demandplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unidade de execucao em memoria usada pelo forecast de Demand Planning.
 *
 * <p>A mesma classe abstrata representa tanto uma serie material/location
 * totalmente desagregada quanto um agregado usado para executar o modelo
 * estatistico. O subtipo define o nivel concreto: material/location ou
 * agregado. No Community a factory cria somente agregacoes derivadas de
 * cluster/material/location; no Enterprise a mesma hierarquia pode receber
 * agregacoes adicionais, como nodes de arvore ou niveis MAPE.</p>
 *
 * <p>As agregacoes sao snapshots. Depois que um agregado e criado, o fluxo que
 * alterar uma serie filha precisa chamar explicitamente a rotina de agregacao
 * novamente ou criar novo agregado. Nao ha view viva nem trigger entre pai e
 * filhos.</p>
 *
 * <p>Combinacoes padrao de agregacao material/location:</p>
 *
 * <p>BOTTOM_UP/BOTTOM_UP: a propria serie material/location e a unidade de
 * execucao; a lista de desagregados pode conter apenas ela mesma e nao ha
 * desagregacao posterior.</p>
 *
 * <p>TOP_DOWN/BOTTOM_UP: a unidade de execucao agrega materiais dentro de uma
 * location. Depois do forecast, o split volta para material/location usando o
 * modelo de desagregacao configurado.</p>
 *
 * <p>BOTTOM_UP/TOP_DOWN: a unidade de execucao agrega locations dentro de um
 * material. Depois do forecast, o split volta para material/location usando o
 * modelo de desagregacao configurado.</p>
 *
 * <p>TOP_DOWN/TOP_DOWN: a unidade de execucao agrega o cluster material/location
 * inteiro. Depois do forecast, o split volta para material/location.</p>
 *
 * <p>Foundation models Enterprise podem gerar forecast a partir do agregado ou
 * diretamente dos desagregados. Quando a saida escolhida for agregada, o fluxo
 * continua exigindo desagregacao para material/location. Quando a saida escolhida
 * ja for desagregada, a etapa de desagregacao deve ser no-op documentado pelo
 * engine Enterprise.</p>
 */
@NoArgsConstructor // necessário para builder
public abstract class DemandPlanForecastProjection {

    /*
     * Unidade de medida das series desta projection. Consumers devem ler por
     * getter para deixar claro que a UOM e metadado da unidade de forecast, nao
     * uma serie mutavel de calculo.
     */
    @Getter
    private UnidadeMedida unidadeMedida;

    /*
     * Series historicas usadas na execucao do forecast.
     *
     * demanda: venda historica observada no nivel desta projection.
     * vendaHistoricaTratamentoStockouts: serie materializada pelo workflow apos
     * a etapa de stockout treatment. No Community, stockout treatment real e
     * Enterprise, entao a serie e copia direta da venda observada.
     * vendaHistoricaTratamentoOutliers: serie materializada pelo workflow apos
     * a etapa de limpeza de outliers/eventos. No Community, normalizacao
     * historica real e Enterprise, entao a serie e copia direta da serie de
     * tratamento de stockouts.
     */
    public double[] demanda;
    public double[] vendaHistoricaTratamentoStockouts;
    public double[] vendaHistoricaTratamentoOutliers;

    /**
     * Vetores auxiliares conhecidos no passado e no horizonte futuro da
     * unidade estatistica. O Community conserva o mapa vazio; o Enterprise o
     * preenche uma vez por projection com os regressores internos aprovados.
     */
    @Getter
    @Setter
    private Map<String, double[]> forecastRegressorSeries = Map.of();

    /*
     * Series auxiliares do modelo. `fitModeloHistorico` e usado no Community
     * para inspecao/simulacao. STL, trend e bounds permanecem no contrato para
     * modelos Enterprise e ficam nulos quando o modelo Community nao os gera.
     */
    public double[] fitModeloHistorico; // fit historico do modelo estatistico, usado quando alguma etapa precisa comparar ajustado x observado
    public double[] seasonalStlHistorico;
    public double[] trendStlHistorico;

    /*
     * Series de forecast no horizonte completo do calendario.
     *
     * A posicao 0 equivale ao periodo 0 do calendario, mesmo quando esse
     * periodo esta no passado. Os metodos consumidores usam as posicoes do
     * Calendario para decidir que trechos devem ser persistidos ou exibidos.
     *
     * `forecastUplift` fica no contrato compartilhado porque a tabela fisica
     * `demand_plan_linha` tambem e compartilhada. No Community a serie nasce
     * zerada e permanece bloqueada nas bordas de configuracao/persistencia. O
     * Enterprise preenche a serie somente quando Event Uplift esta migrado e
     * explicitamente selecionado no perfil de forecast.
     */
    public double[] forecastBaseline;
    public double[] forecastUplift;
    public double[] forecastAjusteDemanda;

    // Series de suporte opcionais, preenchidas apenas por modelos que as geram.
    public double[] trend;
    public double[] seasonal;
    public double[] lowerBound;
    public double[] upperBound;

    /*
     * Pai agregado usado pelo fluxo de desagregacao. Em execucoes bottom-up o
     * campo pode ficar nulo porque a propria serie material/location e a unidade
     * de execucao estatistica.
     */
    @Getter
    @Setter
    private @Nullable DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado;

    public DemandPlanForecastProjection(
            Calendario calendario,
            UnidadeMedida unidadeMedida,
            boolean preencheHorizonteForecastComDemandaHistorica) {
        validaCalendarioInicializacao(calendario);
        if (unidadeMedida == null) {
            throw new IllegalArgumentException(
                    "Demand Plan forecast projection requires unit of measure.");
        }
        this.unidadeMedida = unidadeMedida;
        inicializaArrays(calendario, preencheHorizonteForecastComDemandaHistorica);
    }

    public DemandPlanForecastProjection inicializaArrays(Calendario calendario, boolean preencheHorizonteForecastComDemandaHistorica) {
        validaCalendarioInicializacao(calendario);
        if (preencheHorizonteForecastComDemandaHistorica) {
            demanda = new double[calendario.getNumeroPeriodosTotais()];
        } else {
            demanda = new double[calendario.getNumeroPeriodosPassados()];
        }
        vendaHistoricaTratamentoStockouts = new double[calendario.getNumeroPeriodosPassados()];
        vendaHistoricaTratamentoOutliers = new double[calendario.getNumeroPeriodosPassados()];
        fitModeloHistorico = new double[calendario.getNumeroPeriodosPassados()];

        forecastBaseline = new double[calendario.getNumeroPeriodosTotais()];
        forecastUplift = new double[calendario.getNumeroPeriodosTotais()];
        forecastAjusteDemanda = new double[calendario.getNumeroPeriodosTotais()];

        return this;
    }

    /**
     * Valida o calendario antes de dimensionar arrays de historico e forecast.
     *
     * <p>As factories Community ja fazem essa verificacao, mas a classe-base
     * tambem e usada diretamente por testes e overlays Enterprise. Manter a
     * guarda aqui evita que uma projection manualmente criada falhe depois como
     * {@link NullPointerException} ou array com tamanho inesperado dentro de
     * engines, processors ou desagregacoes.</p>
     */
    private static void validaCalendarioInicializacao(Calendario calendario) {

        if (calendario == null) {
            throw new IllegalArgumentException(
                    "Demand Plan forecast projection requires calendar.");
        }

    }

    public abstract List<DemandPlanForecastProjectionMaterialLocation> getDemandPlanForecastProjectionMaterialLocationList();

    public Set<Location> getLocations() {
        return getDemandPlanForecastProjectionMaterialLocationList()
                .stream()
                .map(DemandPlanForecastProjectionMaterialLocation::getLocation)
                .collect(Collectors.toSet());
    }

    public Set<Produto> getMateriais() {
        return getDemandPlanForecastProjectionMaterialLocationList()
                .stream()
                .map(DemandPlanForecastProjectionMaterialLocation::getMaterial)
                .collect(Collectors.toSet());
    }

    /**
     * Agrega venda historica tratada e forecast a partir das projections mais
     * desagregadas e sobe recursivamente ate o nivel desta unidade.
     */
    public abstract void agregaForecastEDemandaHistoricaDemandPlanForecastProjectionAPartirNivelDesagregado();


}
