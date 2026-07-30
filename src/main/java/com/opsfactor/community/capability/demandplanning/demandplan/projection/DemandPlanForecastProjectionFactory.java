package com.opsfactor.community.capability.demandplanning.demandplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory pura das unidades de execucao do forecast de Demand Planning.
 *
 * <p>O Community sempre parte de series material/location ja populadas com
 * venda historica sell-out. Esta factory apenas reorganiza essas series em
 * unidades de execucao bottom-up ou top-down, sem consultar banco, sem conhecer
 * MAPE/HTS/STL e sem manter estado entre chamadas. Agregacoes Enterprise mais
 * profundas devem entrar por factory/servico Enterprise, preservando este
 * contrato simples para o fluxo aberto.</p>
 *
 * <p>Uma unidade agregada so e criada quando existe pelo menos uma serie
 * material/location filha. Clusters, materiais ou locations vazios geram lista
 * menor ou vazia, e o service principal simplesmente nao itera esses casos.</p>
 */
public abstract class DemandPlanForecastProjectionFactory {

    /**
     * Gera a lista de unidades de execucao do forecast a partir das series
     * material/location desagregadas, respeitando a combinacao de agregacao
     * definida para material e location.
     *
     * BOTTOM_UP/BOTTOM_UP: retorna a própria lista material/location.
     * TOP_DOWN/BOTTOM_UP: retorna um agregado por location.
     * BOTTOM_UP/TOP_DOWN: retorna um agregado por material.
     * TOP_DOWN/TOP_DOWN: retorna um agregado único para o cluster material/location.
     */
    public static List<? extends DemandPlanForecastProjection> getDemandPlanForecastProjectionsExecucao(
            Calendario calendario,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            boolean somenteDfusAtivos,
            List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationDesagregadosList,
            Constantes.DPNivelAgregacao materialAggregationType,
            Constantes.DPNivelAgregacao locationAggregationType,
            UnidadeMedida unidadeMedida,
            boolean preencheHorizonteForecastComDemandaHistorica) {

        validaDemandPlanForecastProjectionMaterialLocationDesagregadosList(
                demandPlanForecastProjectionMaterialLocationDesagregadosList);

        /*
         * A factory tambem e chamada por testes e helpers puros, nao apenas pelo
         * service que ja recebe ParametrosAgregacaoForecast. Reaproveitar o
         * value object aqui garante que nulos transicionais caiam no mesmo
         * fallback conservador TOP_DOWN/TOP_DOWN.
         */
        ParametrosAgregacaoForecast parametrosAgregacaoForecast =
                new ParametrosAgregacaoForecast(
                        locationAggregationType,
                        materialAggregationType);
        locationAggregationType = parametrosAgregacaoForecast.getLocationAggregationType();
        materialAggregationType = parametrosAgregacaoForecast.getMaterialAggregationType();

        List<DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao = new ArrayList<>();

        switch (locationAggregationType) {
            case BOTTOM_UP:
                switch (materialAggregationType) {
                    case BOTTOM_UP:
                        return demandPlanForecastProjectionMaterialLocationDesagregadosList;
                    case TOP_DOWN:
                        Set<Location> locations = getLocationsParaAgregacaoPorLocation(
                                locationProjection,
                                somenteDfusAtivos);
                        // Um DemandPlanForecastProjection agregado por location, consolidando todos os materiais da unidade.
                        for (Location location : locations) {

                            if (somenteDfusAtivos && !location.getAtivo()) continue;

                            // Adiciona ao agregado apenas as séries material/location existentes para a location.
                            // Se a combinação não tiver DFUs, a projection agregada não entra na lista de execução.
                            List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationDaLocationList = demandPlanForecastProjectionMaterialLocationDesagregadosList
                                    .stream()
                                    .filter(demandPlanForecastProjectionMaterialLocationDesagregado ->
                                            demandPlanForecastProjectionMaterialLocationDesagregado.getLocation().equals(location))
                                    .collect(Collectors.toList());

                            if (demandPlanForecastProjectionMaterialLocationDaLocationList.isEmpty()) continue;

                            // Cria projection agregada apenas quando ha filhos reais para evitar placeholders vazios.
                            DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                                    criaDemandPlanForecastProjectionAgregado(
                                            calendario,
                                            unidadeMedida,
                                            preencheHorizonteForecastComDemandaHistorica);

                            adicionaProjectionAgregadaComDesagregados(
                                    demandPlanForecastProjectionsExecucao,
                                    demandPlanForecastProjectionAgregado,
                                    demandPlanForecastProjectionMaterialLocationDaLocationList);
                        }
                        return demandPlanForecastProjectionsExecucao;
                }
            case TOP_DOWN:
                switch (materialAggregationType) {
                    case BOTTOM_UP:
                        Set<Produto> materiais = getMateriaisParaAgregacaoPorMaterial(
                                materialProjection,
                                somenteDfusAtivos);
                        // Um DemandPlanForecastProjection agregado por material, consolidando todas as locations da unidade.
                        for (Produto material : materiais) {

                            // Defesa simetrica ao ramo por location: uma projection quebrada nao deve reintroduzir material inativo em execucao active-only.
                            if (somenteDfusAtivos && !material.getAtivo()) continue;

                            // Adiciona ao agregado apenas as séries material/location existentes para o material.
                            // Se a combinação não tiver DFUs, a projection agregada não entra na lista de execução.
                            List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationDoMaterialList = demandPlanForecastProjectionMaterialLocationDesagregadosList
                                    .stream()
                                    .filter(demandPlanForecastProjectionMaterialLocationDesagregado ->
                                            demandPlanForecastProjectionMaterialLocationDesagregado.getMaterial().equals(material))
                                    .collect(Collectors.toList());

                            if (demandPlanForecastProjectionMaterialLocationDoMaterialList.isEmpty()) continue;

                            // Cria projection agregada apenas quando ha filhos reais para evitar placeholders vazios.
                            DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                                    criaDemandPlanForecastProjectionAgregado(
                                            calendario,
                                            unidadeMedida,
                                            preencheHorizonteForecastComDemandaHistorica);

                            adicionaProjectionAgregadaComDesagregados(
                                    demandPlanForecastProjectionsExecucao,
                                    demandPlanForecastProjectionAgregado,
                                    demandPlanForecastProjectionMaterialLocationDoMaterialList);
                        }
                        return demandPlanForecastProjectionsExecucao;
                    case TOP_DOWN:
                        // DemandPlanForecastProjection único, agregando cluster de materiais e cluster de locations.

                        if (demandPlanForecastProjectionMaterialLocationDesagregadosList.isEmpty()) return demandPlanForecastProjectionsExecucao;

                        // Cria projection agregada inicialmente sem dados.
                        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                                criaDemandPlanForecastProjectionAgregado(
                                        calendario,
                                        unidadeMedida,
                                        preencheHorizonteForecastComDemandaHistorica);

                        adicionaProjectionAgregadaComDesagregados(
                                demandPlanForecastProjectionsExecucao,
                                demandPlanForecastProjectionAgregado,
                                demandPlanForecastProjectionMaterialLocationDesagregadosList);
                        return demandPlanForecastProjectionsExecucao;
                }
        }
        throw new IllegalStateException(
                "Unsupported Demand Planning aggregation levels. Material: "
                + materialAggregationType
                + ", Location: "
                + locationAggregationType);
    }

    /**
     * Valida o snapshot material/location antes de qualquer combinacao de
     * agregacao.
     *
     * <p>Mesmo em BOTTOM_UP/BOTTOM_UP, quando a factory devolve a propria lista
     * recebida, cada item precisa trazer identidade material/location. Isso
     * impede que o erro apareca mais tarde em engines, desagregacao ou
     * persistencia do Planning Book, onde a causa original ficaria menos
     * rastreavel.</p>
     */
    private static void validaDemandPlanForecastProjectionMaterialLocationDesagregadosList(
            List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationDesagregadosList) {

        if (demandPlanForecastProjectionMaterialLocationDesagregadosList == null) {
            throw new IllegalArgumentException(
                    "demandPlanForecastProjectionMaterialLocationDesagregadosList e obrigatoria para forecast");
        }

        for (int index = 0; index < demandPlanForecastProjectionMaterialLocationDesagregadosList.size(); index++) {
            DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                    demandPlanForecastProjectionMaterialLocationDesagregadosList.get(index);

            if (demandPlanForecastProjectionMaterialLocation == null) {
                throw new IllegalStateException(
                        "demandPlanForecastProjectionMaterialLocationDesagregadosList contem item nulo no indice "
                                + index);
            }
            if (demandPlanForecastProjectionMaterialLocation.getLocation() == null) {
                throw new IllegalStateException(
                        "DemandPlanForecastProjectionMaterialLocation sem location no indice "
                                + index);
            }
            if (demandPlanForecastProjectionMaterialLocation.getMaterial() == null) {
                throw new IllegalStateException(
                        "DemandPlanForecastProjectionMaterialLocation sem material no indice "
                                + index);
            }
        }

    }

    /**
     * Cria uma projection agregada validando os metadados que so sao
     * obrigatorios quando ha agregado real para materializar.
     */
    private static DemandPlanForecastProjectionAgregado criaDemandPlanForecastProjectionAgregado(
            Calendario calendario,
            UnidadeMedida unidadeMedida,
            boolean preencheHorizonteForecastComDemandaHistorica) {

        if (calendario == null) {
            throw new IllegalArgumentException(
                    "calendario e obrigatorio para criar agregado de forecast");
        }
        if (unidadeMedida == null) {
            throw new IllegalArgumentException(
                    "unidadeMedida e obrigatoria para criar agregado de forecast");
        }

        return new DemandPlanForecastProjectionAgregado(
                calendario,
                unidadeMedida,
                preencheHorizonteForecastComDemandaHistorica);

    }

    /**
     * Conecta um agregado aos seus filhos material/location e materializa o
     * snapshot inicial das series agregadas.
     *
     * <p>Todo agregado criado pela factory precisa cumprir as mesmas tres
     * etapas: guardar a lista de filhos, apontar cada filho para o agregado pai
     * e consolidar venda/forecast no snapshot inicial. Manter isso em um unico
     * helper evita pequenas divergencias entre os ramos por location, por
     * material e por cluster inteiro, o que seria especialmente perigoso para
     * overlays Enterprise que criem agregados auxiliares MAPE/HTS a partir do
     * mesmo contrato.</p>
     */
    private static void adicionaProjectionAgregadaComDesagregados(
            List<DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao,
            DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado,
            List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationDesagregadosList) {

        demandPlanForecastProjectionAgregado
                .getDemandPlanForecastProjectionDesagregados()
                .addAll(demandPlanForecastProjectionMaterialLocationDesagregadosList);
        demandPlanForecastProjectionMaterialLocationDesagregadosList.forEach(
                demandPlanForecastProjectionMaterialLocationDesagregado ->
                        demandPlanForecastProjectionMaterialLocationDesagregado.setDemandPlanForecastProjectionAgregado(
                                demandPlanForecastProjectionAgregado));

        demandPlanForecastProjectionAgregado.agregaForecastEDemandaHistoricaDemandPlanForecastProjectionAPartirNivelDesagregado();
        demandPlanForecastProjectionsExecucao.add(demandPlanForecastProjectionAgregado);

    }

    private static Set<Location> getLocationsParaAgregacaoPorLocation(
            LocationProjection locationProjection,
            boolean somenteDfusAtivos) {

        /*
         * A projection de locations so e obrigatoria quando a combinacao de
         * agregacao cria uma unidade por location. Bottom-up puro e agregado
         * cluster/cluster nao precisam dela, por isso a validacao fica
         * confinada neste ramo em vez de validar todos os parametros no inicio.
         */
        if (locationProjection == null) {
            throw new IllegalArgumentException(
                    "locationProjection e obrigatorio para forecast TOP_DOWN/BOTTOM_UP");
        }

        Set<Location> locations =
                somenteDfusAtivos ? locationProjection.getLocationsAtivas() : locationProjection.getLocationSet();
        validaLocationsParaAgregacaoForecast(
                locations,
                "TOP_DOWN/BOTTOM_UP");
        return locations;

    }

    private static Set<Produto> getMateriaisParaAgregacaoPorMaterial(
            MaterialProjection materialProjection,
            boolean somenteDfusAtivos) {

        /*
         * A projection de materiais so e obrigatoria quando a combinacao de
         * agregacao cria uma unidade por material. Mantemos essa checagem aqui
         * para que callers que nao usam materialProjection continuem simples e
         * para que falhas de contrato aparecam com mensagem funcional.
         */
        if (materialProjection == null) {
            throw new IllegalArgumentException(
                    "materialProjection e obrigatorio para forecast BOTTOM_UP/TOP_DOWN");
        }

        Set<Produto> materiais =
                somenteDfusAtivos ? materialProjection.getMateriaisAtivos() : materialProjection.getMaterialSet();
        validaMateriaisParaAgregacaoForecast(
                materiais,
                "BOTTOM_UP/TOP_DOWN");
        return materiais;

    }

    /**
     * Valida o snapshot de locations entregue pela projection de escopo.
     *
     * <p>Colecao vazia continua sendo recorte operacional valido. Colecao
     * nula ou item nulo, por outro lado, indicam projection corrompida e devem
     * falhar antes de o loop de agregacao tentar acessar status/identidade da
     * location, especialmente porque esse fluxo roda por clusters em paralelo
     * no service de Demand Planning.</p>
     */
    private static void validaLocationsParaAgregacaoForecast(
            Collection<Location> locations,
            String combinacaoAgregacao) {

        if (locations == null) {
            throw new IllegalStateException(
                    "LocationProjection retornou colecao nula para forecast "
                            + combinacaoAgregacao);
        }

        int index = 0;
        for (Location location : locations) {
            if (location == null) {
                throw new IllegalStateException(
                        "LocationProjection retornou location nula no indice "
                                + index
                                + " para forecast "
                                + combinacaoAgregacao);
            }
            index++;
        }

    }

    /**
     * Valida o snapshot de materiais entregue pela projection de escopo.
     *
     * <p>Assim como no eixo de location, o conjunto vazio e um recorte tecnico
     * legitimo. A validacao existe apenas para transformar snapshot nulo ou
     * item nulo em erro funcional antes da criacao de agregados, mantendo
     * factories Enterprise futuras alinhadas ao mesmo contrato.</p>
     */
    private static void validaMateriaisParaAgregacaoForecast(
            Collection<Produto> materiais,
            String combinacaoAgregacao) {

        if (materiais == null) {
            throw new IllegalStateException(
                    "MaterialProjection retornou colecao nula para forecast "
                            + combinacaoAgregacao);
        }

        int index = 0;
        for (Produto material : materiais) {
            if (material == null) {
                throw new IllegalStateException(
                        "MaterialProjection retornou material nulo no indice "
                                + index
                                + " para forecast "
                                + combinacaoAgregacao);
            }
            index++;
        }

    }

}
