package com.opsfactor.community.capability.masterdata.network.supplynetwork.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaRepository;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.RecursoProdutivoRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoRepository;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.routing.repository.RoteiroRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.service.VersaoProducaoService;
import com.opsfactor.community.platform.utility.FuncoesMap;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory da projection de malha e dados produtivos do Supply Planning.
 *
 * <p>O Community materializa somente a malha operacional necessaria ao
 * heuristico: transporte, BOM, roteiro, recursos produtivos e versoes simples
 * de producao. Recursos privados como frotas, line scheduling, paralelismo de
 * producao e custos ficam fora desta factory.</p>
 */
@Component
public class SupplyNetworkProjectionFactory {

    /**
     * Factory da projection central de parametros e master data basico.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Factory de conversoes de unidade.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /**
     * Repository de linhas de transporte.
     */
    @Autowired
    private LinhaTransporteRepository linhaTransporteRepository;

    /**
     * Repository de parametros material/linha de transporte.
     */
    @Autowired
    private LinhaTransporteProdutoRepository linhaTransporteProdutoRepository;

    /**
     * Repository de roteiros com operacoes.
     */
    @Autowired
    private RoteiroRepository roteiroRepository;

    /**
     * Repository de listas tecnicas com componentes.
     */
    @Autowired
    private ListaTecnicaRepository listaTecnicaRepository;

    /**
     * Service que fornece a versao de producao inexistente usada como sentinel.
     */
    @Autowired
    private VersaoProducaoService versaoProducaoService;

    /**
     * Repository das versoes simples de producao permitidas no Community.
     */
    @Autowired
    private VersaoProducaoRepository versaoProducaoRepository;

    /**
     * Repository de recursos produtivos.
     */
    @Autowired
    private RecursoProdutivoRepository recursoProdutivoRepository;

    /**
     * Repository das versoes de malha.
     */
    @Autowired
    private VersaoMalhaRepository versaoMalhaRepository;

    public SupplyNetworkProjection getSupplyNetworkProjectionVazio(
            UnidadeMedidaProjection conversaoUnidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        SupplyNetworkProjection supplyNetworkProjection = new SupplyNetworkProjection();
        supplyNetworkProjection.inicializaProjectionsBase(
                conversaoUnidadeMedidaProjection,
                clusterEParametrosProjection);

        return supplyNetworkProjection;

    }

    public void populaSupplyNetworkProjectionComLinhasTransporte(
            SupplyNetworkProjection supplyNetworkProjection,
            VersaoMalha versaoMalha) {

        populaSupplyNetworkProjectionComLinhasTransporte(
                supplyNetworkProjection, versaoMalha, null, null);

    }

    /**
     * Popula projection com as linhas de transporte e seus parâmetros : prioridades,
     * lead times, lotes mínimos e múltiplos.
     * São selecionadas todas as LTs onde location origem pertence a locationsOrigemFiltradas
     * OU destino pertence a locationsDestinoFiltradas (desta forma se apenas filtramos uma location origem
     * todas as possibilidades de inbound e outbound ligadas a esta location serão extraídas)
     * @param supplyNetworkProjection
     * @param versaoMalha
     * @param locationsOrigemFiltradas se nulo, não realiza filtro (usa locations de clusterEParametrosProjection
     * @param locationsDestinoFiltradas se nulo, não realiza filtro (usa locations de clusterEParametrosProjection
     */
    public void populaSupplyNetworkProjectionComLinhasTransporte(
            SupplyNetworkProjection supplyNetworkProjection,
            VersaoMalha versaoMalha,
            Set<Location> locationsOrigemFiltradas,
            Set<Location> locationsDestinoFiltradas) {

        if (supplyNetworkProjection.mapaLinhaTransporteSetPorVersaoMalha == null) supplyNetworkProjection.mapaLinhaTransporteSetPorVersaoMalha = new HashMap<>();
        if (supplyNetworkProjection.mapaLinhaTransporteInboundAtivaSetPorLocation == null) supplyNetworkProjection.mapaLinhaTransporteInboundAtivaSetPorLocation = new HashMap<>();
        if (supplyNetworkProjection.mapaLinhaTransporteOutboundAtivaSetPorLocation == null) supplyNetworkProjection.mapaLinhaTransporteOutboundAtivaSetPorLocation = new HashMap<>();
        if (supplyNetworkProjection.mapaLinhaTransporteProdutoPorLinhaTransporteEProduto == null) supplyNetworkProjection.mapaLinhaTransporteProdutoPorLinhaTransporteEProduto = new HashMap<>();

        Set<Location> locationsAtivasSet = supplyNetworkProjection.clusterEParametrosProjection.getLocationsAtivas();
        Set<Produto> materiaisAtivosSet = supplyNetworkProjection.clusterEParametrosProjection.getMateriaisAtivos();

        Set<Location> locationsOrigemFiltradasCopia = new HashSet(locationsAtivasSet);
        if ((locationsOrigemFiltradas != null && !locationsOrigemFiltradas.isEmpty())) {
            locationsOrigemFiltradasCopia.retainAll(locationsOrigemFiltradas);
        }
        Set<Location> locationsDestinoFiltradasCopia = new HashSet(locationsAtivasSet);
        if ((locationsDestinoFiltradas != null && !locationsDestinoFiltradas.isEmpty())) {
            locationsDestinoFiltradasCopia.retainAll(locationsDestinoFiltradas);
        }

        List<LinhaTransporte> linhaTransporteList =
                linhaTransporteRepository.findByLinhaTransporteCompositeKeyVersaoAndLinhaTransporteCompositeKeyLocationOrigemInAndLinhaTransporteCompositeKeyLocationDestinoIn(
                                versaoMalha,
                                locationsOrigemFiltradasCopia,
                                locationsDestinoFiltradasCopia);
        validaLinhasTransporte(linhaTransporteList);

        Set<LinhaTransporte> linhaTransporteAtivaSet = linhaTransporteList.stream()
                        .filter(LinhaTransporte::getAtivo)
                        .collect(Collectors.toSet());

        List<LinhaTransporteProduto> linhaTransporteProdutoSet =
                linhaTransporteProdutoRepository.customFindAll();
        adicionaLinhasTransporteAosMapas(
                supplyNetworkProjection,
                versaoMalha,
                linhaTransporteList,
                linhaTransporteProdutoSet);
        // verifica se há origem padrão para clientes e preenche com linhas de transporte temporárias onde não há
        // rota para abastecimento
        populaSupplyNetworkProjectionComLocationOrigemPadraoClientes(supplyNetworkProjection);

    }

    private void adicionaLinhasTransporteAosMapas(
            SupplyNetworkProjection supplyNetworkProjection,
            VersaoMalha versaoMalha,
            Collection<LinhaTransporte> linhaTransporteSetAAdicionar,
            Collection<LinhaTransporteProduto> linhaTransporteProdutoSetAAdicionar) {

        // adiciona linhas transporte ativas ou não
        supplyNetworkProjection.mapaLinhaTransporteSetPorVersaoMalha
                .computeIfAbsent(versaoMalha, vm -> new HashSet<>())
                .addAll(linhaTransporteSetAAdicionar);

        Set<LinhaTransporte> linhaTransporteAtivaSetAAdicionar = linhaTransporteSetAAdicionar
                .stream()
                .filter(linhaTransporte -> linhaTransporte.getAtivo())
                .collect(Collectors.toSet());

        for (LinhaTransporte linhaTransporte : linhaTransporteAtivaSetAAdicionar) {
            supplyNetworkProjection.mapaLinhaTransporteInboundAtivaSetPorLocation
                    .computeIfAbsent(versaoMalha, vm -> new HashMap<>())
                    .computeIfAbsent(linhaTransporte.getLocationDestino(), loc -> new HashSet<>())
                    .add(linhaTransporte);
        }

        for (LinhaTransporte linhaTransporte : linhaTransporteAtivaSetAAdicionar) {
            supplyNetworkProjection.mapaLinhaTransporteOutboundAtivaSetPorLocation
                    .computeIfAbsent(versaoMalha, vm -> new HashMap<>())
                    .computeIfAbsent(linhaTransporte.getLocationOrigem(), loc -> new HashSet<>())
                    .add(linhaTransporte);
        }

        Set<Produto> materiaisAtivosSet = supplyNetworkProjection.clusterEParametrosProjection.getMateriaisAtivos();
        Set<LinhaTransporteProduto> linhaTransporteProdutoAtivasEmLinhasTransporteAtivasSet = linhaTransporteProdutoSetAAdicionar
                .stream()
                .filter(x -> linhaTransporteAtivaSetAAdicionar == null || linhaTransporteAtivaSetAAdicionar.contains(x.getLinhaTransporte()))
                .filter(x -> materiaisAtivosSet == null || materiaisAtivosSet.contains(x.getProduto()))
                .collect(Collectors.toSet());

        for (LinhaTransporteProduto linhaTransporteProdutoAtiva : linhaTransporteProdutoAtivasEmLinhasTransporteAtivasSet) {
            supplyNetworkProjection.mapaLinhaTransporteProdutoPorLinhaTransporteEProduto
                    .computeIfAbsent(versaoMalha, vm -> new HashMap<>())
                    .computeIfAbsent(linhaTransporteProdutoAtiva.getLinhaTransporte(), loc -> new HashMap<>())
                    .put(linhaTransporteProdutoAtiva.getProduto(), linhaTransporteProdutoAtiva); // substitui
        }

    }

        public void populaSupplyNetworkProjectionComDadosMestresProducao(
            SupplyNetworkProjection supplyNetworkProjection) {

        populaSupplyNetworkProjectionComDadosMestresProducao(
                supplyNetworkProjection,
                supplyNetworkProjection.getClusterEParametrosProjection().getLocationsAtivas());

    }

    public void populaSupplyNetworkProjectionComDadosMestresProducao(
            SupplyNetworkProjection supplyNetworkProjection,
            Set<Location> locationsFiltradas) {

        Set<Location> locationsAtivasSet = supplyNetworkProjection.clusterEParametrosProjection.getLocationsAtivas();
        Set<Produto> materiaisAtivosSet = supplyNetworkProjection.clusterEParametrosProjection.getMateriaisAtivos();

        Set<Location> locationsFiltradasCopia = new HashSet(locationsAtivasSet);
        if ((locationsFiltradas != null && !locationsFiltradas.isEmpty())) {
            locationsFiltradasCopia.retainAll(locationsFiltradas);
        }

        List<RecursoProdutivo> recursoProdutivoList = recursoProdutivoRepository.customFindByLocationIn(locationsFiltradasCopia);
        validaEntidadesComId(
                recursoProdutivoList,
                RecursoProdutivo::getId,
                "Production resource repository",
                "production resource");

        Set<RecursoProdutivo> recursoProdutivoAtivoSet = recursoProdutivoList
                .stream()
                .filter(x -> x.getAtivo())
                .collect(Collectors.toSet());

        supplyNetworkProjection.mapaRecursosProdutivos = recursoProdutivoAtivoSet.stream()
                .collect(Collectors.toMap(RecursoProdutivo::getId, x -> x));
        supplyNetworkProjection.mapaRecursoProdutivoAtivoSetPorLocation = recursoProdutivoAtivoSet.stream()
                .collect(Collectors.groupingBy(RecursoProdutivo::getLocation, Collectors.toSet()));

        // LISTAS TECNICAS -------------------
        List<ListaTecnica> listaTecnicaList = listaTecnicaRepository.customFindAllByLocationInAndMaterialOutputInFetchListaTecnicaComponente(
                        locationsFiltradasCopia,
                        materiaisAtivosSet);
        validaEntidadesComId(
                listaTecnicaList,
                ListaTecnica::getId,
                "BOM repository",
                "bill of materials");
        supplyNetworkProjection.mapaListasTecnicas = listaTecnicaList.stream()
                .collect(Collectors.toMap(ListaTecnica::getId, x -> x));
        supplyNetworkProjection.mapaComponentesPorListaTecnicaId = listaTecnicaList.stream()
                .collect(Collectors.toMap(
                        ListaTecnica::getId,
                        listaTecnica -> Set.copyOf(
                                listaTecnica.getListaTecnicaComponenteSet())));
        supplyNetworkProjection.mapaMateriaisInputPorListaTecnicaId = listaTecnicaList.stream()
                .collect(Collectors.toMap(
                        ListaTecnica::getId,
                        listaTecnica -> listaTecnica.getListaTecnicaComponenteSet().stream()
                                .map(componente -> componente.getMaterialComponente())
                                .collect(Collectors.toUnmodifiableSet())));

        Set<ListaTecnica> listasTecnicasViaveisSet = listaTecnicaList.stream()
                .filter(x -> x.getAtivo())
                .filter(x -> supplyNetworkProjection.clusterEParametrosProjection.isDfuAtiva(
                        x.getMaterialOutput(), x.getLocation()))
                // todos os componentes da lista técnica devem ser ativos
                .filter(x -> x.getMateriaisInput().size() ==
                        x.getMateriaisInput().stream().filter(y ->
                                supplyNetworkProjection.clusterEParametrosProjection.isDfuAtiva(y, x.getLocation())).count())
                .collect(Collectors.toSet());
        // Map<Location,Map<Produto,Set<ListaTecnica>>>
        supplyNetworkProjection.mapaListaTecnicaSetPorLocationMaterial = listaTecnicaList.stream()
                .collect(Collectors.groupingBy(ListaTecnica::getLocation,
                        Collectors.groupingBy(ListaTecnica::getMaterialOutput, Collectors.toSet())));
        supplyNetworkProjection.mapaListaTecnicaViavelSetPorLocationMaterial = listasTecnicasViaveisSet.stream()
                .collect(Collectors.groupingBy(ListaTecnica::getLocation,
                        Collectors.groupingBy(ListaTecnica::getMaterialOutput, Collectors.toSet())));

        // ROTEIROS ------------------------------
        List<Roteiro> roteiroList = roteiroRepository.customFindAllByLocationInAndMaterialOutputInFetchOperacaoRoteiroSet(
                        locationsFiltradasCopia,
                        materiaisAtivosSet);
        validaEntidadesComId(
                roteiroList,
                Roteiro::getId,
                "Routing repository",
                "routing");
        supplyNetworkProjection.mapaRoteiros = roteiroList.stream()
                .collect(Collectors.toMap(Roteiro::getId, x -> x));
        supplyNetworkProjection.mapaOperacoesPorRoteiroId = roteiroList.stream()
                .collect(Collectors.toMap(
                        Roteiro::getId,
                        roteiro -> Set.copyOf(roteiro.getOperacaoRoteiroSet())));
        Set<Roteiro> roteirosViaveisSet = roteiroList.stream()
                .filter(x -> x.getAtivo())
                .filter(x -> !supplyNetworkProjection.getListasTecnicasViaveis(x.getLocation(), x.getMaterialOutput(), null).isEmpty())
                .filter(x -> !x.getOperacaoRoteiroSet().stream()
                        .anyMatch(y -> !recursoProdutivoAtivoSet.contains(y.getRecursoProdutivo())))
                .filter(x -> supplyNetworkProjection.clusterEParametrosProjection.isDfuAtiva(
                        x.getMaterialOutput(), x.getLocation()))
                .collect(Collectors.toSet());
        supplyNetworkProjection.mapaRoteiroSetPorRecursoProdutivoMaterial = roteiroList.stream()
                .flatMap(x -> x.getOperacaoRoteiroSet().stream())
                .collect(Collectors.groupingBy(OperacaoRoteiro::getRecursoProdutivo,
                        Collectors.groupingBy(operacaoRoteiro -> operacaoRoteiro.getRoteiro().getMaterialOutput(),
                                Collectors.mapping(OperacaoRoteiro::getRoteiro, Collectors.toSet()))));
        // Map<Location,Map<Produto,Set<Roteiro>>>
        supplyNetworkProjection.mapaRoteiroSetPorLocationMaterial = roteiroList.stream()
                .collect(Collectors.groupingBy(Roteiro::getLocation,
                        Collectors.groupingBy(Roteiro::getMaterialOutput, Collectors.toSet())));
        supplyNetworkProjection.mapaRoteiroViavelSetPorLocationMaterial = roteirosViaveisSet.stream()
                .collect(Collectors.groupingBy(Roteiro::getLocation,
                        Collectors.groupingBy(Roteiro::getMaterialOutput, Collectors.toSet())));
        // VERSAO PRODUCAO INEXISTENTE
        supplyNetworkProjection.versaoProducaoInexistente = versaoProducaoService.getOuPersisteVersaoProducaoInexistente();

        // VERSOES PRODUCAO ----------------------------------------------------
        Set<VersaoProducao> versaoProducaoAbstractSet = new HashSet<>();

        // VERSOES PRODUCAO PERSISTIDAS ---------------------------------------
        List<VersaoProducao> versaoProducaoList = versaoProducaoRepository.customFindAllByLocationInAndMaterialOutputIn(
                        locationsFiltradasCopia,
                        materiaisAtivosSet);
        validaEntidadesComId(
                versaoProducaoList,
                VersaoProducao::getId,
                "Production version repository",
                "production version");

        /*
         * Roteiros e listas tecnicas foram carregados acima, cada agregado em
         * sua consulta batch com uma unica colecao filha. Reassociamos aqui as
         * referencias canonicas da projection para evitar tanto N+1 quanto o
         * produto cartesiano que surgiria ao buscar operacoes e componentes
         * na mesma consulta da versao.
         */
        for (VersaoProducao versaoProducao : versaoProducaoList) {
            Roteiro roteiroCanonico = supplyNetworkProjection.mapaRoteiros.get(
                    versaoProducao.getRoteiro().getId());
            if (roteiroCanonico == null) {
                throw new IllegalStateException(
                        "Production version "
                                + versaoProducao.getId()
                                + " references routing outside the production master-data snapshot: "
                                + versaoProducao.getRoteiro().getId());
            }

            ListaTecnica listaTecnicaCanonica = supplyNetworkProjection.mapaListasTecnicas.get(
                    versaoProducao.getListaTecnica().getId());
            if (listaTecnicaCanonica == null) {
                throw new IllegalStateException(
                        "Production version "
                                + versaoProducao.getId()
                                + " references Bill of Materials outside the production master-data snapshot: "
                                + versaoProducao.getListaTecnica().getId());
            }

            versaoProducao.setRoteiro(roteiroCanonico);
            versaoProducao.setListaTecnica(listaTecnicaCanonica);
            versaoProducao.geraErroSeDadosInconsistentes();
        }

        versaoProducaoAbstractSet.addAll(versaoProducaoList);

        // No Community, os mestres referenciados por estas versões são simples.

        /*
         * O índice por id contém todas as versões persistidas do snapshot,
         * inclusive inativas. Relatórios e linhas históricas do plano não
         * podem depender de uma varredura restrita às versões hoje viáveis.
         */
        supplyNetworkProjection.mapaVersaoProducaoPorId = versaoProducaoList.stream()
                .collect(Collectors.toMap(VersaoProducao::getId, versaoProducao -> versaoProducao));
        supplyNetworkProjection.mapaVersaoProducaoPorId.put(
                supplyNetworkProjection.versaoProducaoInexistente.getId(),
                supplyNetworkProjection.versaoProducaoInexistente);

        // INICIALIZA MAPAS DE VERSOES PRODUCAO (VIAVEIS + PRIORITARIAS)
        supplyNetworkProjection.mapaVersaoProducaoViavelSetPorLocationMaterial = new HashMap<>();
        supplyNetworkProjection.mapaVersaoProducaoSetPorLocationMaterial = new HashMap<>();
        supplyNetworkProjection.mapaVersaoProducaoViavelPrioritariaPorLocationProduto = new HashMap<>();
        supplyNetworkProjection.mapaVersaoProducaoViavelSetPorRecursoProdutivo = new HashMap<>();

        // VERSOES PRODUCAO TEMPORARIAS (COMBINACOES ROTEIRO/LT VALIDAS MAS SEM VERSAO DEFINIDA. 1 ROTEIRO + 1 LT) -------------------
        Set<VersaoProducao> versaoProducaoTemporariaSet = new HashSet<>();
        for (Location location : locationsFiltradasCopia) {
            for (Produto material : materiaisAtivosSet) {
                List<VersaoProducao> versoesProducao = supplyNetworkProjection.getVersoesProducaoSimplesViaveis(location, material, null);

                // se consideram todos os roteiros/listas técnicas para que também se criem versões de produção inviáveis (usado por relatórios de inspeção de viabilidade de abastecimento)
                Set<Roteiro> roteiros = FuncoesMap.getElementoDeNestedMap(supplyNetworkProjection.mapaRoteiroSetPorLocationMaterial, Set.class, location, material).orElse(new HashSet<>());
                Set<ListaTecnica> listasTecnicas = FuncoesMap.getElementoDeNestedMap(supplyNetworkProjection.mapaListaTecnicaSetPorLocationMaterial, Set.class, location, material).orElse(new HashSet<>());

                for (Roteiro roteiro : roteiros) {
                    if (!roteiro.getHabilitadoParaUsoSemVersaoProducao()) continue;
                    for (ListaTecnica listaTecnica : listasTecnicas) {
                        if (!listaTecnica.getHabilitadoParaUsoSemVersaoProducao()) continue;
                        // somente se adiciona um roteiro temporário se não houver uma versão de produção com a combinação roteiro / LT
                        if (!versoesProducao.stream().anyMatch(x -> x.getRoteiro().equals(roteiro) && x.getListaTecnica().equals(listaTecnica))) {
                            VersaoProducao versaoProducaoTemporaria = new VersaoProducao(
                                    null, location,
                                    Math.max(roteiro.getPrioridade(), listaTecnica.getPrioridade()), // prioridade
                                    roteiro, listaTecnica);

                            versaoProducaoTemporariaSet.add(versaoProducaoTemporaria);
                        }
                    }
                }
            }
        }
        versaoProducaoAbstractSet.addAll(versaoProducaoTemporariaSet);

        // MAPAS COM VERSOES PRODUCAO PRODUCAO VIAVEIS
        for (VersaoProducao versaoProducaoAbstract : versaoProducaoAbstractSet) {

            Location location = versaoProducaoAbstract.getLocation();

            // sempre adiciona versoes de producao ao mapaVersaoProducaoSetPorLocationMaterial,
            // mesmo que inativas ou inviáveis
            Produto materialOutput = supplyNetworkProjection.getMaterialOutputProjetado(
                    versaoProducaoAbstract.getRoteiro(),
                    versaoProducaoAbstract.getListaTecnica());
            FuncoesMap.getOrAddElementoDeNestedMap(
                    supplyNetworkProjection.mapaVersaoProducaoSetPorLocationMaterial,
                    Set.class,
                    () -> new HashSet<>(),
                    location, materialOutput)
                    .add(versaoProducaoAbstract);

            // insere as versões de produção viáveis nos respectivos mapas
            if (!versaoProducaoAbstract.getAtivo()) continue;

            /*
             * O Community valida seus mestres simples pelas referências
             * canônicas. Especializações de múltiplos outputs pertencem ao
             * Enterprise e não alteram este contrato base.
             */
            Roteiro roteiro = supplyNetworkProjection.getRoteiroProjetado(
                    versaoProducaoAbstract);
            ListaTecnica listaTecnica = supplyNetworkProjection.getListaTecnicaProjetada(
                    versaoProducaoAbstract);
            boolean possuiRoteiroInviavel =
                    !supplyNetworkProjection.verificaSeRoteiroEViavel(roteiro);
            boolean possuiListaTecnicaInviavel =
                    !supplyNetworkProjection.verificaSeListaTecnicaEViavel(listaTecnica);

            if (possuiRoteiroInviavel || possuiListaTecnicaInviavel) continue;

            FuncoesMap.getOrAddElementoDeNestedMap(
                    supplyNetworkProjection.mapaVersaoProducaoViavelSetPorLocationMaterial,
                    Set.class,
                    () -> new HashSet<>(),
                    location, materialOutput)
                    .add(versaoProducaoAbstract);

            for (RecursoProdutivo recursoProdutivo :
                    supplyNetworkProjection.getRecursosProdutivos(roteiro)) {
                supplyNetworkProjection.mapaVersaoProducaoViavelSetPorRecursoProdutivo
                        .computeIfAbsent(recursoProdutivo, x -> new HashSet<>())
                        .add(versaoProducaoAbstract);
            }

        }

        // MAPA COM VERSAO PRODUCAO MAIS PRIORITARIA
        FuncoesMap.flattenMapToKeyListAndValueStream(supplyNetworkProjection.mapaVersaoProducaoViavelSetPorLocationMaterial)
                .forEach(x -> {
                    Location location = (Location) x.getValue0().get(0);
                    Produto produto = (Produto) x.getValue0().get(1);

                    Set<VersaoProducao> versaoProducaoAbstractSetMaterialLocation = (Set<VersaoProducao>) x.getValue1();

                    VersaoProducao versaoProducaoAbstractPrioritario = versaoProducaoAbstractSetMaterialLocation.stream()
                            .sorted(Comparator.comparing(y -> y.getPrioridade()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                    "Nenhuma versão de produção viável encontrada para "
                                            + location
                                            + " / "
                                            + produto));

                    FuncoesMap.adicionaElementoAoNestedMap(
                            versaoProducaoAbstractPrioritario,
                            supplyNetworkProjection.mapaVersaoProducaoViavelPrioritariaPorLocationProduto,
                            location, produto);
                });

    }

    /**
     * Retorna o snapshot completo de malha de suprimentos materializado em cache.
     *
     * <p>Essa projection combina unidades de medida, parametros, linhas de
     * transporte, versoes de malha e dados mestres de producao. O cache evita
     * reconstrucoes repetidas do grafo de supply network dentro de calculos que
     * compartilham a mesma base validada.</p>
     */
    @Cacheable(value = "supplyNetworkProjection", sync = true)
    public SupplyNetworkProjection getSupplyNetworkProjectionCompletoDeCache() {

        return getSupplyNetworkProjectionCompletoSemCache();

    }

    /**
     * Monta a fotografia comum sem aplicar cache.
     *
     * <p>Extensões Enterprise reutilizam esta sequência para preservar a
     * mesma malha Community e acrescentar apenas seus índices privados sob
     * um cache separado.</p>
     */
    protected SupplyNetworkProjection getSupplyNetworkProjectionCompletoSemCache() {

        UnidadeMedidaProjection unidadeMedidaProjection = unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        SupplyNetworkProjection supplyNetworkProjection = getSupplyNetworkProjectionVazio(
                unidadeMedidaProjection,
                clusterEParametrosProjection);

        // popula linhas de transporte para cada versão malha disponível
        List<VersaoMalha> versaoMalhaList = versaoMalhaRepository.customFindAll();
        validaVersaoMalhaList(versaoMalhaList);

        supplyNetworkProjection.mapaVersaoMalhaPorId = versaoMalhaList.stream()
                .collect(Collectors.toMap(x -> x.getId(), Function.identity()));

        versaoMalhaList.stream()
                .forEach(versaoMalha -> {
                    populaSupplyNetworkProjectionComLinhasTransporte(supplyNetworkProjection, versaoMalha);
                });

        populaSupplyNetworkProjectionComDadosMestresProducao(supplyNetworkProjection);
        populaSupplyNetworkProjectionComLocationOrigemPadraoMateriasPrimas(supplyNetworkProjection);

        return supplyNetworkProjection;

    }

    /**
     * Valida colecoes retornadas por repositories antes de streams/collectors.
     *
     * <p>A projection de malha e base para heuristico, plano restrito e
     * Planning Book. Um repository/stub que retorna colecao nula ou item nulo
     * deve falhar como contrato de snapshot, em vez de aparecer como NPE
     * tardio no meio da indexacao de malha.</p>
     */
    

    /**
     * Valida a identidade funcional das versoes de malha antes do mapa por id.
     *
     * <p>A lista vazia e uma fotografia operacional valida para tenants sem
     * malha cadastrada. Ja uma versao sem id ou duas instancias para o mesmo id
     * tornam ambigua a selecao de linhas de transporte e precisam falhar antes
     * do `Collectors.toMap(...)` emitir uma mensagem generica.</p>
     */
    private void validaVersaoMalhaList(List<VersaoMalha> versaoMalhaList) {

        Set<String> versaoMalhaIds = new HashSet<>();
        for (int indice = 0; indice < versaoMalhaList.size(); indice++) {
            VersaoMalha versaoMalha = versaoMalhaList.get(indice);

            if (versaoMalha.getId() == null || versaoMalha.getId().isBlank()) {
                throw new IllegalStateException(
                        "Network version repository returned network version without id at index "
                                + indice
                                + " for Supply Network Projection.");
            }
            if (!versaoMalhaIds.add(versaoMalha.getId())) {
                throw new IllegalStateException(
                        "Network version repository returned duplicate network version id "
                                + versaoMalha.getId()
                                + " for Supply Network Projection.");
            }
        }

    }

    /**
     * Valida a chave composta das linhas de transporte antes de montar os
     * mapas de inbound, outbound e parametros por material.
     *
     * <p>O repository preserva cardinalidade em `List` para que duas entidades
     * com a mesma versao de malha, origem e destino nao sejam colapsadas por
     * `Set` antes de a factory acusar a fotografia ambigua.</p>
     */
    private void validaLinhasTransporte(Collection<LinhaTransporte> linhaTransporteCollection) {

        Set<LinhaTransporte.LinhaTransporteCompositeKey> chavesLinhaTransporte =
                new HashSet<>();
        int indice = 0;
        for (LinhaTransporte linhaTransporte : linhaTransporteCollection) {
            if (linhaTransporte.getLinhaTransporteCompositeKey() == null
                    || linhaTransporte.getVersaoMalha() == null
                    || linhaTransporte.getLocationOrigem() == null
                    || linhaTransporte.getLocationDestino() == null) {
                throw new IllegalStateException(
                        "Transportation line repository returned transportation line without network version, origin or destination at index "
                                + indice
                                + " for Supply Network Projection.");
            }
            if (linhaTransporte.getVersaoMalha().getId() == null
                    || linhaTransporte.getVersaoMalha().getId().isBlank()
                    || linhaTransporte.getLocationOrigem().getId() == null
                    || linhaTransporte.getLocationOrigem().getId().isBlank()
                    || linhaTransporte.getLocationDestino().getId() == null
                    || linhaTransporte.getLocationDestino().getId().isBlank()) {
                throw new IllegalStateException(
                        "Transportation line repository returned transportation line without network version, origin or destination id at index "
                                + indice
                                + " for Supply Network Projection.");
            }

            LinhaTransporte.LinhaTransporteCompositeKey chaveLinhaTransporte =
                    linhaTransporte.getLinhaTransporteCompositeKey();
            if (!chavesLinhaTransporte.add(chaveLinhaTransporte)) {
                throw new IllegalStateException(
                        "Transportation line repository returned duplicate transportation line for network version "
                                + linhaTransporte.getVersaoMalha().getId()
                                + ", origin "
                                + linhaTransporte.getLocationOrigem().getId()
                                + " and destination "
                                + linhaTransporte.getLocationDestino().getId()
                                + " for Supply Network Projection.");
            }
            indice++;
        }

    }

    /**
     * Valida ids funcionais antes de montar mapas por id de snapshots JPA.
     *
     * <p>A colecao ja foi validada contra nulo/item nulo por
     * {@link #getRepositoryCollectionObrigatoria(Collection, String)}. Esta
     * segunda camada fecha a chave funcional usada por mapas como
     * `mapaRecursosProdutivos`, evitando que id ausente ou repetido vire
     * resultado dependente de ordem de repository.</p>
     */
    private <T> void validaEntidadesComId(
            Collection<T> entityCollection,
            Function<T, String> idFunction,
            String repositoryDescription,
            String entityDescription) {

        Set<String> entityIds = new HashSet<>();
        int indice = 0;
        for (T entity : entityCollection) {
            String entityId = idFunction.apply(entity);
            if (entityId == null || entityId.isBlank()) {
                throw new IllegalStateException(
                        repositoryDescription
                                + " returned "
                                + entityDescription
                                + " without id at index "
                                + indice
                                + " for Supply Network Projection.");
            }
            if (!entityIds.add(entityId)) {
                throw new IllegalStateException(
                        repositoryDescription
                                + " returned duplicate "
                                + entityDescription
                                + " id "
                                + entityId
                                + " for Supply Network Projection.");
            }
            indice++;
        }

    }

    /**
     * Valida a projection de UOM usada como base da malha cacheada.
     *
     * <p>A Supply Network Projection e consumida por heuristico, Planning Book
     * e plano restrito. Se a factory de UOM retornar snapshot nulo ou sem
     * parametros globais, a falha precisa ocorrer antes de carregar repositories
     * de malha/producao, evitando uma projection parcialmente materializada.</p>
     */
    /**
     * Valida a projection central de parametros/master data usada para indexar
     * locations, materiais e defaults da malha.
     */
    public void populaSupplyNetworkProjectionComLocationOrigemPadraoClientes(
            SupplyNetworkProjection supplyNetworkProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        Set<Location> locationsClienteERegiaoComercialAtivas = clusterEParametrosProjection.getLocationsAtivas()
                .parallelStream()
                .filter(location -> location.getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL)
                        || location.getTipoLocation().equals(LocationAbstract.TipoLocation.REGIAO_COMERCIAL))
                .collect(Collectors.toSet());

        for (VersaoMalha versaoMalha : supplyNetworkProjection.getTodasVersoesMalha()) {
            if (versaoMalha.getLocationOrigemPadraoClientes() == null) continue;

            List<LinhaTransporte> linhasTransporteAAdicionar = new ArrayList<>();
            List<LinhaTransporteProduto> linhasTransporteProdutoAAdicionar = new ArrayList<>();

            Location locationOrigemPadraoClientes = clusterEParametrosProjection.getLocationPersistida(versaoMalha.getLocationOrigemPadraoClientes().getId());
            boolean locationOrigemPadraoClientesIsInterna = locationOrigemPadraoClientes.getTipoLocation().equals(LocationAbstract.TipoLocation.INTERNA);

            for (Location locationClienteOuRegiaoComercial : locationsClienteERegiaoComercialAtivas) {
                List<LinhaTransporte> linhasTransporteInbound = supplyNetworkProjection.getLinhasTransporteAtivasInboundOrdenadasPorPrioridade(versaoMalha, locationClienteOuRegiaoComercial, null);
                boolean regiaoComercialSemLinhaInboundInterna = locationClienteOuRegiaoComercial.getTipoLocation().equals(LocationAbstract.TipoLocation.REGIAO_COMERCIAL)
                        && locationOrigemPadraoClientesIsInterna
                        && linhasTransporteInbound.stream().noneMatch(linhaTransporte -> linhaTransporte.getLocationOrigem().getTipoLocation().equals(LocationAbstract.TipoLocation.INTERNA));

                // 1a hipotese : não há nenhuma linha de transporte : criar uma habilitada para todos os materiais
                if (linhasTransporteInbound.isEmpty()) {
                    LinhaTransporte linhaTransporteTemporaria = new LinhaTransporte(new LinhaTransporte.LinhaTransporteCompositeKey(
                            versaoMalha,
                            locationOrigemPadraoClientes,
                            locationClienteOuRegiaoComercial));
                    linhaTransporteTemporaria.setHabilitadoProdutosNaoCadastradosLinhaTransporte(true);
                    linhasTransporteAAdicionar.add(linhaTransporteTemporaria);
                }

                // 2a hipotese : já temos 1 linha de transporte inbound habilitada para todos os SKUs. não temos problema de atendimento no cliente/regiao comercial
                boolean contemAoMenosUmaLinhaInboundHabilitadaTodosMateriais = linhasTransporteInbound
                        .stream()
                        .anyMatch(linhaTransporteInbound -> linhaTransporteInbound.getHabilitadoProdutosNaoCadastradosLinhaTransporte());
                boolean naoHaProdutosDesativadosEmLinhasInbound = supplyNetworkProjection
                        .getLinhasTransporteProdutoAtivasInbound(versaoMalha, locationClienteOuRegiaoComercial, null)
                        .stream()
                        .allMatch(linhaTransporteProduto -> linhaTransporteProduto.getAtivo());
                if (contemAoMenosUmaLinhaInboundHabilitadaTodosMateriais
                        && naoHaProdutosDesativadosEmLinhasInbound
                        && !regiaoComercialSemLinhaInboundInterna) continue;

                // 3a hipotese : temos linhas de transporte ativas para apenas alguns produtos.
                // 1 : criar linha de transporte da origem primaria (se não houver)
                // 2 : habilitar produtos que não estejam mapeados em outras linhas de transporte
                LinhaTransporte linhaTransporteDeOrigemPadraoClientes = linhasTransporteInbound
                        .stream()
                        .filter(linhaTransporte -> linhaTransporte.getLocationOrigem().equals(locationOrigemPadraoClientes))
                        .findFirst()
                        .orElseGet(() -> {
                            LinhaTransporte linhaTransporteTemporaria = new LinhaTransporte(new LinhaTransporte.LinhaTransporteCompositeKey(
                                    versaoMalha,
                                    locationOrigemPadraoClientes,
                                    locationClienteOuRegiaoComercial));
                            linhaTransporteTemporaria.setHabilitadoProdutosNaoCadastradosLinhaTransporte(false); // só se habilitarão alguns materiais
                            linhaTransporteTemporaria.setAtivo(true);
                            linhasTransporteAAdicionar.add(linhaTransporteTemporaria);
                            return linhaTransporteTemporaria;
                        });

                Map<Produto, Boolean> produtoTemAlgumaLinhaTransporteProdutoAtivaInbound =
                        supplyNetworkProjection
                                .getLinhasTransporteProdutoAtivasInbound(versaoMalha, locationClienteOuRegiaoComercial, null)
                                .stream()
                                .filter(LinhaTransporteProduto::getAtivo)
                                .collect(Collectors.toMap(
                                        LinhaTransporteProduto::getProduto,
                                        ltp -> ltp.getAtivo(),
                                        (a, b) -> a || b // merge: se ao menos 1 produto ativo em linha transporte produto, mantém true
                                ));
                /*
                 * A linha padrao para clientes deve cobrir os produtos ainda
                 * sem nenhum inbound ativo, mas representar isso criando um
                 * LinhaTransporteProduto por material/location explode a malha
                 * para dezenas de milhoes de pares em bases grandes.
                 *
                 * Usamos a propria semantica da linha de transporte:
                 * produtos nao cadastrados ficam habilitados na linha padrao,
                 * e produtos que ja possuem outro inbound ativo recebem uma
                 * excecao explicita inativa nessa linha. Assim preservamos a
                 * regra "somente nao mapeados" sem materializar todos os nao
                 * mapeados.
                 */
                linhaTransporteDeOrigemPadraoClientes.setHabilitadoProdutosNaoCadastradosLinhaTransporte(true);

                Map<Produto, LinhaTransporteProduto> linhaTransporteProdutoPorProdutoNaOrigemPadrao =
                        supplyNetworkProjection.mapaLinhaTransporteProdutoPorLinhaTransporteEProduto
                                .getOrDefault(versaoMalha, new HashMap<>())
                                .getOrDefault(linhaTransporteDeOrigemPadraoClientes, new HashMap<>());

                linhaTransporteProdutoPorProdutoNaOrigemPadrao
                        .values()
                        .stream()
                        .filter(linhaTransporteProduto -> !produtoTemAlgumaLinhaTransporteProdutoAtivaInbound
                                .containsKey(linhaTransporteProduto.getProduto()))
                        .forEach(linhaTransporteProduto -> linhaTransporteProduto.setAtivo(true));

                for (Produto produtoJaMapeadoEmAlgumaLinhaInbound : produtoTemAlgumaLinhaTransporteProdutoAtivaInbound.keySet()) {
                    if (linhaTransporteProdutoPorProdutoNaOrigemPadrao.containsKey(produtoJaMapeadoEmAlgumaLinhaInbound)) {
                        continue;
                    }

                    LinhaTransporteProduto linhaTransporteProdutoBloqueioOrigemPadrao = new LinhaTransporteProduto(
                            new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                                    linhaTransporteDeOrigemPadraoClientes,
                                    produtoJaMapeadoEmAlgumaLinhaInbound));
                    linhaTransporteProdutoBloqueioOrigemPadrao.setAtivo(false);
                    linhasTransporteProdutoAAdicionar.add(linhaTransporteProdutoBloqueioOrigemPadrao);
                }
            }

            // adiciona as linhas transporte temporárias aos mapas do projection
            adicionaLinhasTransporteAosMapas(
                    supplyNetworkProjection,
                    versaoMalha,
                    linhasTransporteAAdicionar,
                    linhasTransporteProdutoAAdicionar);
        }
    }

    public void populaSupplyNetworkProjectionComLocationOrigemPadraoMateriasPrimas(
            SupplyNetworkProjection supplyNetworkProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        Set<Location> locationsInternasAtivas = clusterEParametrosProjection.getLocationsAtivas()
                .stream()
                .filter(location -> location.getTipoLocation().equals(LocationAbstract.TipoLocation.INTERNA))
                .collect(Collectors.toSet());
        Set<Produto> materiaisComCadastroProdutivoEmLocationsInternas =
                getMateriaisComCadastroProdutivoEmLocationsInternas(
                        supplyNetworkProjection,
                        locationsInternasAtivas);

        for (VersaoMalha versaoMalha : supplyNetworkProjection.getTodasVersoesMalha()) {
            if (versaoMalha.getLocationOrigemPadraoMateriasPrimas() == null) continue;

            List<LinhaTransporte> linhasTransporteAAdicionar = new ArrayList<>();
            List<LinhaTransporteProduto> linhasTransporteProdutoAAdicionar = new ArrayList<>();
            Map<Location, LinhaTransporte> linhaTransporteOrigemPadraoPorLocationInterna = new HashMap<>();

            Location locationOrigemPadraoMateriasPrimas = clusterEParametrosProjection.getLocationPersistida(
                    versaoMalha.getLocationOrigemPadraoMateriasPrimas().getId());

            for (Location locationInterna : locationsInternasAtivas) {
                Set<Produto> materiasPrimasSemOrigem = clusterEParametrosProjection
                        .getMateriaisAtivosEmLocation(locationInterna)
                        .stream()
                        .filter(material -> !materiaisComCadastroProdutivoEmLocationsInternas.contains(material))
                        .filter(material -> supplyNetworkProjection
                                .getLinhaTransporteInboundViavelListOrdenadaPorPrioridade(
                                        versaoMalha, locationInterna, material, null, null)
                                .isEmpty())
                        .collect(Collectors.toSet());

                if (materiasPrimasSemOrigem.isEmpty()) continue;

                LinhaTransporte linhaTransporteOrigemPadraoMateriasPrimas =
                        linhaTransporteOrigemPadraoPorLocationInterna.computeIfAbsent(
                                locationInterna,
                                location -> getOuCriaLinhaTransporteOrigemPadraoMateriasPrimas(
                                        supplyNetworkProjection,
                                        versaoMalha,
                                        locationOrigemPadraoMateriasPrimas,
                                        location,
                                        linhasTransporteAAdicionar));

                for (Produto materiaPrimaSemOrigem : materiasPrimasSemOrigem) {
                    LinhaTransporteProduto linhaTransporteProduto = supplyNetworkProjection
                            .getLinhaTransporteMaterial(
                                    linhaTransporteOrigemPadraoMateriasPrimas,
                                    materiaPrimaSemOrigem)
                            .orElseGet(() -> {
                                LinhaTransporteProduto linhaTransporteProdutoNova = new LinhaTransporteProduto(
                                        new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                                                linhaTransporteOrigemPadraoMateriasPrimas,
                                                materiaPrimaSemOrigem));
                                linhasTransporteProdutoAAdicionar.add(linhaTransporteProdutoNova);
                                return linhaTransporteProdutoNova;
                            });

                    linhaTransporteProduto.setAtivo(true);

                    Integer leadTimeDiasInteiroLocationOrigemPadraoMateriasPrimas =
                            getLeadTimeDiasInteiroLocationOrigemPadraoMateriasPrimas(versaoMalha);
                    if (leadTimeDiasInteiroLocationOrigemPadraoMateriasPrimas != null) {
                        linhaTransporteProduto.setLeadTimeDias(leadTimeDiasInteiroLocationOrigemPadraoMateriasPrimas);
                    }
                }
            }

            adicionaLinhasTransporteAosMapas(
                    supplyNetworkProjection,
                    versaoMalha,
                    linhasTransporteAAdicionar,
                    linhasTransporteProdutoAAdicionar);
        }
    }

    private LinhaTransporte getOuCriaLinhaTransporteOrigemPadraoMateriasPrimas(
            SupplyNetworkProjection supplyNetworkProjection,
            VersaoMalha versaoMalha,
            Location locationOrigemPadraoMateriasPrimas,
            Location locationInterna,
            List<LinhaTransporte> linhasTransporteAAdicionar) {

        Optional<LinhaTransporte> linhaTransporteExistente = supplyNetworkProjection
                .getLinhaTransporteEntreOrigemEDestino(
                        versaoMalha,
                        locationOrigemPadraoMateriasPrimas,
                        locationInterna);

        return linhaTransporteExistente
                .map(linhaTransporte -> {

                    linhasTransporteAAdicionar.add(linhaTransporte);
                    return linhaTransporte;

                })
                .orElseGet(() -> {

                    LinhaTransporte linhaTransporteTemporaria = new LinhaTransporte(
                            new LinhaTransporte.LinhaTransporteCompositeKey(
                                    versaoMalha,
                                    locationOrigemPadraoMateriasPrimas,
                                    locationInterna));
                    linhaTransporteTemporaria.setAtivo(true);
                    linhaTransporteTemporaria.setHabilitadoProdutosNaoCadastradosLinhaTransporte(false);
                    linhaTransporteTemporaria.setLeadTimeDias(
                            versaoMalha.getLeadTimeDiasLocationOrigemPadraoMateriasPrimas());
                    linhasTransporteAAdicionar.add(linhaTransporteTemporaria);
                    return linhaTransporteTemporaria;

                });
    }

    private Set<Produto> getMateriaisComCadastroProdutivoEmLocationsInternas(
            SupplyNetworkProjection supplyNetworkProjection,
            Set<Location> locationsInternasAtivas) {

        Set<Produto> materiaisComCadastroProdutivoEmLocationsInternas = new HashSet<>();

        for (Location locationInterna : locationsInternasAtivas) {
            materiaisComCadastroProdutivoEmLocationsInternas.addAll(
                    supplyNetworkProjection.mapaRoteiroSetPorLocationMaterial
                            .getOrDefault(locationInterna, new HashMap<>())
                            .keySet());
            materiaisComCadastroProdutivoEmLocationsInternas.addAll(
                    supplyNetworkProjection.mapaListaTecnicaSetPorLocationMaterial
                            .getOrDefault(locationInterna, new HashMap<>())
                            .keySet());
            materiaisComCadastroProdutivoEmLocationsInternas.addAll(
                    supplyNetworkProjection.mapaVersaoProducaoSetPorLocationMaterial
                            .getOrDefault(locationInterna, new HashMap<>())
                            .keySet());
        }

        return materiaisComCadastroProdutivoEmLocationsInternas;
    }

    @Nullable
    private Integer getLeadTimeDiasInteiroLocationOrigemPadraoMateriasPrimas(VersaoMalha versaoMalha) {

        Double leadTimeDiasLocationOrigemPadraoMateriasPrimas =
                versaoMalha.getLeadTimeDiasLocationOrigemPadraoMateriasPrimas();
        if (leadTimeDiasLocationOrigemPadraoMateriasPrimas == null) return null;
        if (!Double.isFinite(leadTimeDiasLocationOrigemPadraoMateriasPrimas)
                || leadTimeDiasLocationOrigemPadraoMateriasPrimas < 0.0d) {
            throw new IllegalStateException(
                    "Default raw material source lead time must be finite and non-negative for network version "
                            + versaoMalha.getId()
                            + ": "
                            + leadTimeDiasLocationOrigemPadraoMateriasPrimas
                            + ".");
        }

        return (int) Math.ceil(leadTimeDiasLocationOrigemPadraoMateriasPrimas);
    }

}
