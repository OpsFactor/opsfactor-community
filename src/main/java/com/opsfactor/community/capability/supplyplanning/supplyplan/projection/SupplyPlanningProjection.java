package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem.DistributionPlanItemKey;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha.InventoryPlanLinhaCompositeKey;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha.ProductionPlanLinhaCompositeKey;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionCompleto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.IncompatibleCalendarException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.FirmePlanejado;
import com.opsfactor.community.platform.utility.Constantes.ReferenciaPeriodo;
import com.opsfactor.community.platform.utility.Constantes.TipoPlano;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import lombok.Getter;
import org.apache.commons.compress.utils.Sets;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Indice em memoria das linhas de Supply Plan usadas pelos calculos.
 *
 * <p>No Community esta projection materializa o plano heuristico em uma unica
 * location por instancia, indexando producao, distribuicao e estoque por
 * periodo/material. O objetivo e permitir que rotinas de calculo e Planning
 * Book leiam e ajustem valores em memoria antes da persistencia em lote.</p>
 *
 * <p>A classe ainda preserva entidades transicionais como `DistributionPlanItem`
 * porque elas representam movimentacao interna do heuristico, nao o modulo
 * Enterprise de Distribution. Custos, modelos otimizados, process chain e
 * variaveis/restricoes de solver devem entrar em projections/capabilities
 * Enterprise separadas.</p>
 */
@Getter
public class SupplyPlanningProjection {

    private final SupplyPlan supplyPlan;
    
    /**
     * Perfil efetivamente usado para gerar a projection.
     *
     * <p>No Community este perfil coincide com o perfil raiz do Supply Plan.
     * O Enterprise pode reintroduzir execucoes compostas escolhendo um perfil
     * especifico para cada etapa.</p>
    */
    private final PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanConsiderado;

    /** Projection de conversoes entre unidades de medida. */
    private final UnidadeMedidaProjection conversaoUnidadeMedidaProjection;

    /** Parametros globais, materiais, locations e clusters usados no calculo. */
    private final ClusterEParametrosProjection clusterEParametrosProjection;

    /**
     * Malha operacional que define linhas inbound prioritarias, roteiros e
     * listas tecnicas usados pelos setters/getters da projection.
     */
    private final SupplyNetworkProjection supplyNetworkProjection;

    /** Politicas de estoque operacionais associadas ao perfil. */
    private final PoliticaEstoquesProjection politicaEstoquesProjection;

    /** Calendario usado para indexacao por periodo. */
    private final Calendario calendario;

    /** Location planejada por esta instancia da projection. */
    private final Location location;

    /**
     * Politicas efetivas de geracao para a location desta projection.
     *
     * <p>O Community inicializa os tres valores a partir do perfil pai. O
     * overlay Enterprise pode substituir a fotografia somente antes da rotina
     * heuristica, por meio de um contrato tipado de projection; a entidade
     * compartilhada nao recebe relacao ou estado privado por isso.</p>
     */
    private boolean generateInbound;
    private boolean generatePlannedProductionOrder;
    private boolean treatPolicyAsDrp;

    /** Materiais que podem ser planejados nesta projection. */
    MaterialProjection materialProjection;

    /** Locations de origem permitidas para consultas de inbound. */
    LocationProjection locationProjectionLocationsOrigem;

    /** Linhas inbound indexadas por periodo de recebimento e material. */
    private Map<Integer, Map<Produto, Queue<DistributionPlanItem>>> mapaDistributionPlanItemsInbound = new ConcurrentHashMap<>();

    /** Linhas outbound indexadas por periodo de expedicao e material. */
    private Map<Integer, Map<Produto, Queue<DistributionPlanItem>>> mapaDistributionPlanItemsOutbound = new ConcurrentHashMap<>();

    /*
     * Podemos ter diversas ProductionPlanLinha usando diferentes receitas. O
     * output representa onde o material e produzido; o input representa onde o
     * material e consumido. Production plan input pode se repetir varias vezes,
     * por isso parte dos mapas usa Queue.
     *
     * As versoes de producao indexadas sao persistidas ou temporarias,
     * definidas dinamicamente quando nao ha versao de producao para a linha.
     */
    private Map<Integer, Map<Produto, Map<VersaoProducao, Map<Roteiro, Map<ListaTecnica, ProductionPlanLinha>>>>> mapaProductionPlanLinhasOutput = new ConcurrentHashMap<>();
    private Map<Integer, Map<VersaoProducao, Queue<ProductionPlanLinha>>> mapaProductionPlanLinhasOutputPorVersaoProducao = new ConcurrentHashMap<>();
    private Map<Integer, Map<Produto, Map<VersaoProducao, Map<Roteiro, Map<ListaTecnica, ProductionPlanLinha>>>>> mapaProductionPlanLinhasInput = new ConcurrentHashMap<>();
    private Map<Integer, Map<VersaoProducao, Queue<ProductionPlanLinha>>> mapaProductionPlanLinhasInputPorVersaoProducao = new ConcurrentHashMap<>();

    /** Estoque projetado indexado por periodo/material. */
    private Map<Integer, Map<Produto, InventoryPlanLinha>> mapaInventoryPlanLinhas = new ConcurrentHashMap<>();

    /**
     * Demanda direta considerada pelo Supply Planning. No Community vem apenas
     * do Demand Plan e pode ser propagada para locations internas conforme o
     * perfil heuristico.
     */
    DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection;

    public DemandaDiretaConsideradaProjection getDemandaDiretaConsideradaProjection() {
        return demandaDiretaConsideradaProjection;
    }

    // CONSTRUTOR
    public SupplyPlanningProjection(SupplyPlan supplyPlan, 
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanConsiderado,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            Calendario calendario, Location location, 
            MaterialProjection materialProjection, LocationProjection locationProjectionLocationsOrigem) {

        SupplyNetworkProjection supplyNetworkProjectionObrigatoria =
                supplyNetworkProjection;
        this.supplyPlan = supplyPlan;
        this.perfilExecucaoSupplyPlanConsiderado = perfilExecucaoSupplyPlanConsiderado;
        this.conversaoUnidadeMedidaProjection =
                supplyNetworkProjectionObrigatoria.getConversaoUnidadeMedidaProjection();
        this.calendario = calendario;
        this.location = location;
        this.materialProjection = materialProjection;
        this.locationProjectionLocationsOrigem = locationProjectionLocationsOrigem;
        this.clusterEParametrosProjection =
                supplyNetworkProjectionObrigatoria.getClusterEParametrosProjection();
        this.supplyNetworkProjection = supplyNetworkProjectionObrigatoria;
        this.politicaEstoquesProjection = politicaEstoquesProjection;
        configuraPoliticaExecucaoLocationCommunity(perfilExecucaoSupplyPlanConsiderado);

    }

    /**
     * Inicializa a politica local com os defaults do perfil compartilhado.
     *
     * <p>Instancias de teste ou snapshots transitorios podem nao trazer o
     * perfil. Nesse caso, os tres defaults historicos permanecem ligados; a
     * execucao real valida o perfil antes de construir a projection.</p>
     */
    private void configuraPoliticaExecucaoLocationCommunity(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        generateInbound = perfilExecucaoSupplyPlan == null
                || perfilExecucaoSupplyPlan.getGeraRequisicoesInbound();
        generatePlannedProductionOrder = perfilExecucaoSupplyPlan == null
                || perfilExecucaoSupplyPlan.getGeraOrdensProducaoPlanejadas();
        treatPolicyAsDrp = perfilExecucaoSupplyPlan == null
                || perfilExecucaoSupplyPlan.getTrataPoliticaEstoqueComoDrp();

    }

    /**
     * Aplica a politica efetiva de geracao para esta location.
     *
     * <p>Este ponto recebe apenas os tres escalares ja resolvidos. Assim, uma
     * capability Enterprise nao atravessa entidade JPA, mapa generico ou
     * consulta de banco para dentro do loop heuristico Community.</p>
     */
    public void configuraPoliticaExecucaoLocation(
            boolean generateInbound,
            boolean generatePlannedProductionOrder,
            boolean treatPolicyAsDrp) {

        this.generateInbound = generateInbound;
        this.generatePlannedProductionOrder = generatePlannedProductionOrder;
        this.treatPolicyAsDrp = treatPolicyAsDrp;

    }

    /**
     * Valida a malha antes do construtor acessar projections derivadas.
     *
     * <p>A factory Community ja valida a malha em seus entry points. A
     * projection, porem, tambem e criada diretamente por testes e overlays.
     * Manter esta guarda na classe dona dos indices evita que um snapshot
     * manual quebre como NullPointerException antes das demais validacoes
     * especificas de calendario, UOM ou cluster.</p>
     */
            private ChavePlanningProjection getChaveFromProductionPlanLinhaOutput(ProductionPlanLinha productionPlanLinha) {
        return new ChavePlanningProjection(
                calendario.getPosicaoPeriodo(productionPlanLinha.getDataReferencia()),
                productionPlanLinha.getMaterialOutput());
    }
    
    private ChavePlanningProjection getChaveFromInventoryPlanLinha(InventoryPlanLinha inventoryPlanLinha) {
        return new ChavePlanningProjection(
                calendario.getPosicaoPeriodo(inventoryPlanLinha.getDataReferencia()),
                inventoryPlanLinha.getProduto());
    }
    
    /*
     * Indexacao incremental das linhas extraidas ou criadas pelo fluxo de
     * Supply Planning.
     *
     * Os mapas sao alimentados enquanto a projection e montada e tambem quando
     * o Planning Book cria linhas novas. A indexacao por periodo/material evita
     * varrer colecoes grandes a cada leitura de estoque, producao ou
     * distribuicao.
     */
    public void addDistributionPlanItemInbound(DistributionPlanItem distributionPlanItem) {

        validaDistributionPlanItemParaIndexacao(
                distributionPlanItem,
                "inbound Distribution Plan indexing");
        
        Queue<DistributionPlanItem> distributionPlanItemInbound = mapaDistributionPlanItemsInbound
                .computeIfAbsent(calendario.getPosicaoPeriodo(distributionPlanItem.getDataRecebimento()), x -> new ConcurrentHashMap<>())
                .computeIfAbsent(distributionPlanItem.getProduto(), x -> new ConcurrentLinkedQueue<>());
                
        distributionPlanItemInbound.add(distributionPlanItem);
        
    }
    
    /**
     * Representa demanda indireta : a data considerada desta forma é a de
     * emissão e não a data referência!
     * @param distributionPlanItem 
     */
    public void addDistributionPlanItemOutbound(DistributionPlanItem distributionPlanItem) {

        validaDistributionPlanItemParaIndexacao(
                distributionPlanItem,
                "outbound Distribution Plan indexing");
        
        Queue<DistributionPlanItem> distributionPlanItemOutbound = mapaDistributionPlanItemsOutbound
                .computeIfAbsent(calendario.getPosicaoPeriodo(distributionPlanItem.getDataExpedicao()), x -> new ConcurrentHashMap<>())
                .computeIfAbsent(distributionPlanItem.getProduto(), x -> new ConcurrentLinkedQueue<>());
                
        distributionPlanItemOutbound.add(distributionPlanItem);
        
    }

    public void addProductionPlanLinhaOutput(ProductionPlanLinha productionPlanLinha) {

        validaProductionPlanLinhaParaIndexacao(
                productionPlanLinha,
                "Production Plan output indexing");
        
        Roteiro roteiroProjetado = getRoteiroProjetado(productionPlanLinha);
        ListaTecnica listaTecnicaProjetada = getListaTecnicaProjetada(productionPlanLinha);
        // A chave usa somente os mestres canônicos já materializados pela SupplyNetworkProjection.
        VersaoProducao versaoProducaoTratada = getVersaoProducaoProjetada(
                productionPlanLinha,
                roteiroProjetado,
                listaTecnicaProjetada);
                
        FuncoesMap.adicionaElementoAoNestedMap(
                productionPlanLinha, 
                mapaProductionPlanLinhasOutput, 
                // índice do mapa
                calendario.getPosicaoPeriodo(productionPlanLinha.getDataReferencia()),
                productionPlanLinha.getMaterialOutput(), 
                versaoProducaoTratada,
                roteiroProjetado,
                listaTecnicaProjetada);
        
        mapaProductionPlanLinhasOutputPorVersaoProducao
                .computeIfAbsent(calendario.getPosicaoPeriodo(productionPlanLinha.getDataReferencia()), x -> new ConcurrentHashMap<>())
                .computeIfAbsent(versaoProducaoTratada, x -> new LinkedList<>())
                .add(productionPlanLinha);
                
    }
        
    public void addProductionPlanLinhaInput(ProductionPlanLinha productionPlanLinha) {

        validaProductionPlanLinhaParaIndexacao(
                productionPlanLinha,
                "Production Plan input indexing");

        // Uma execução múltipla possui uma linha por output, mas componentes
        // e capacidade pertencem ao pacote físico e entram uma única vez.
        if (!productionPlanLinha.representaConsumoCompartilhadoDoPacote(
                supplyNetworkProjection)) {
            return;
        }
        
        int posicaoPeriodo = calendario.getPosicaoPeriodo(productionPlanLinha.getDataReferencia());
        Roteiro roteiroProjetado = getRoteiroProjetado(productionPlanLinha);
        ListaTecnica listaTecnicaProjetada = getListaTecnicaProjetada(productionPlanLinha);
        // A versão persistida é resolvida por id; nenhum grafo JPA da linha é percorrido.
        VersaoProducao versaoProducaoTratada = getVersaoProducaoProjetada(
                productionPlanLinha,
                roteiroProjetado,
                listaTecnicaProjetada);
        
        for (Produto materialInput : productionPlanLinha.getMateriaisInput(supplyNetworkProjection)) {
            
            FuncoesMap.adicionaElementoAoNestedMap(
                    productionPlanLinha, 
                    mapaProductionPlanLinhasInput, 
                    // índice do mapa
                    posicaoPeriodo,
                    materialInput, 
                    versaoProducaoTratada,
                    roteiroProjetado,
                    listaTecnicaProjetada);
            
        }
        
        mapaProductionPlanLinhasInputPorVersaoProducao
                .computeIfAbsent(calendario.getPosicaoPeriodo(productionPlanLinha.getDataReferencia()), x -> new ConcurrentHashMap<>())
                .computeIfAbsent(versaoProducaoTratada, x -> new ConcurrentLinkedQueue<>())
                .add(productionPlanLinha);
        
    }
    
    public void addInventoryPlanLinha(InventoryPlanLinha inventoryPlanLinha) {

        validaInventoryPlanLinhaParaIndexacao(
                inventoryPlanLinha,
                "Inventory Plan indexing");
        
        int posicaoPeriodo = calendario.getPosicaoPeriodo(inventoryPlanLinha.getDataReferencia());
        Produto material = inventoryPlanLinha.getProduto();
        
        FuncoesMap.adicionaElementoAoNestedMap(
                inventoryPlanLinha, 
                mapaInventoryPlanLinhas, 
                posicaoPeriodo, material);
        
    }

    /**
     * Valida a linha de Distribution Plan antes de indexa-la nos mapas em
     * memoria.
     *
     * <p>Os caminhos publicos de escrita criam linhas completas antes de
     * chamar este metodo. A validacao explicita evita que snapshots quebrados
     * vindos de repositories ou futuras factories Enterprise sejam convertidos
     * em NullPointerException distante do ponto de entrada.</p>
     */
    private void validaDistributionPlanItemParaIndexacao(
            DistributionPlanItem distributionPlanItem,
            String contextoIndexacao) {

        if (distributionPlanItem == null) {
            throw new IllegalArgumentException(
                    "SupplyPlanningProjection cannot index null Distribution Plan line during "
                            + contextoIndexacao
                            + ".");
        }

        DistributionPlanItemKey key =
                distributionPlanItem.getKey();
        if (key == null
                || key.getSupplyPlan() == null
                || key.getLocationOrigem() == null
                || key.getLocationDestino() == null
                || key.getProduto() == null
                || key.getDataExpedicao() == null
                || key.getDataRecebimento() == null) {
            throw new IllegalArgumentException(
                    "SupplyPlanningProjection requires Distribution Plan line with supply plan, origin, destination, material, shipping date and receiving date before "
                            + contextoIndexacao
                            + ".");
        }

        validaCalendarioParaIndexacao(contextoIndexacao);

    }

    /**
     * Valida a linha de Production Plan antes de indexa-la nos mapas em
     * memoria.
     *
     * <p>A versao de producao cadastrada pode ser a sentinela de versao
     * inexistente; nesse caso a propria projection traduz para uma versao
     * temporaria por roteiro/lista tecnica. Por isso a validacao exige a
     * existencia do campo, mas nao bloqueia a versao reservada que representa ausencia.</p>
     */
    private void validaProductionPlanLinhaParaIndexacao(
            ProductionPlanLinha productionPlanLinha,
            String contextoIndexacao) {

        if (productionPlanLinha == null) {
            throw new IllegalArgumentException(
                    "SupplyPlanningProjection cannot index null Production Plan line during "
                            + contextoIndexacao
                            + ".");
        }

        ProductionPlanLinhaCompositeKey productionPlanLinhaCompositeKey =
                productionPlanLinha.getProductionPlanLinhaCompositeKey();
        if (productionPlanLinhaCompositeKey == null
                || productionPlanLinhaCompositeKey.getSupplyPlan() == null
                || productionPlanLinhaCompositeKey.getLocation() == null
                || productionPlanLinhaCompositeKey.getVersaoProducao() == null
                || productionPlanLinhaCompositeKey.getRoteiro() == null
                || productionPlanLinhaCompositeKey.getListaTecnica() == null
                || productionPlanLinhaCompositeKey.getDataReferencia() == null
                || productionPlanLinha.getMaterialOutput() == null) {
            throw new IllegalArgumentException(
                    "SupplyPlanningProjection requires Production Plan line with supply plan, location, output material, production version, routing, bill of materials and reference date before "
                            + contextoIndexacao
                            + ".");
        }

        validaCalendarioParaIndexacao(contextoIndexacao);
        validaSupplyNetworkProjectionParaIndexacao(contextoIndexacao);

    }

    /**
     * Resolve o roteiro completo no snapshot de dados mestres. A linha do
     * plano fornece apenas a identidade persistida, nunca as operações LAZY.
     */
    public Roteiro getRoteiroProjetado(ProductionPlanLinha productionPlanLinha) {

        return supplyNetworkProjection
                .getRoteiroFromId(productionPlanLinha.getRoteiro().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Routing not projected for Production Plan line: "
                                + productionPlanLinha.getRoteiro().getId()));

    }

    /** Resolve a lista técnica completa no snapshot materializado. */
    public ListaTecnica getListaTecnicaProjetada(ProductionPlanLinha productionPlanLinha) {

        return supplyNetworkProjection
                .getListaTecnicaFromId(productionPlanLinha.getListaTecnica().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Bill of Materials not projected for Production Plan line: "
                                + productionPlanLinha.getListaTecnica().getId()));

    }

    /**
     * Resolve a versão canônica por id. A sentinela histórica é convertida em
     * versão temporária sobre o roteiro e a lista técnica já projetados.
     */
    public VersaoProducao getVersaoProducaoProjetada(
            ProductionPlanLinha productionPlanLinha,
            Roteiro roteiroProjetado,
            ListaTecnica listaTecnicaProjetada) {

        VersaoProducao versaoProducaoCadastrada = productionPlanLinha.getVersaoProducao();
        if (versaoProducaoCadastrada.getId() == null
                || versaoProducaoCadastrada.isVersaoProducaoInexistente()) {
            return VersaoProducao.getVersaoProducaoAlocadaOuTemporariaSeInexistente(
                    versaoProducaoCadastrada,
                    roteiroProjetado,
                    listaTecnicaProjetada,
                    supplyNetworkProjection);
        }

        return supplyNetworkProjection
                .getVersaoProducaoFromId(versaoProducaoCadastrada.getId(), true)
                .orElseThrow(() -> new IllegalStateException(
                        "Production version not projected for Production Plan line: "
                                + versaoProducaoCadastrada.getId()));

    }

    /**
     * Valida a linha de Inventory Plan antes de indexa-la nos mapas em memoria.
     */
    private void validaInventoryPlanLinhaParaIndexacao(
            InventoryPlanLinha inventoryPlanLinha,
            String contextoIndexacao) {

        if (inventoryPlanLinha == null) {
            throw new IllegalArgumentException(
                    "SupplyPlanningProjection cannot index null Inventory Plan line during "
                            + contextoIndexacao
                            + ".");
        }

        InventoryPlanLinhaCompositeKey inventoryPlanLinhaCompositeKey =
                inventoryPlanLinha.getInventoryPlanLinhaCompositeKey();
        if (inventoryPlanLinhaCompositeKey == null
                || inventoryPlanLinhaCompositeKey.getSupplyPlan() == null
                || inventoryPlanLinhaCompositeKey.getLocation() == null
                || inventoryPlanLinhaCompositeKey.getProduto() == null
                || inventoryPlanLinhaCompositeKey.getDataReferencia() == null) {
            throw new IllegalArgumentException(
                    "SupplyPlanningProjection requires Inventory Plan line with supply plan, location, material and reference date before "
                            + contextoIndexacao
                            + ".");
        }

        validaCalendarioParaIndexacao(contextoIndexacao);

    }

    /**
     * Garante que a projection esteja completa para derivar posicoes de
     * periodo antes de indexar qualquer linha.
     */
    private void validaCalendarioParaIndexacao(String contextoIndexacao) {

        if (calendario == null) {
            throw new IllegalStateException(
                    "SupplyPlanningProjection requires calendar before "
                            + contextoIndexacao
                            + ".");
        }

    }

    /**
     * Garante que linhas de producao possam resolver versoes temporarias e
     * materiais input sem lazy loading de entidades JPA.
     */
    private void validaSupplyNetworkProjectionParaIndexacao(String contextoIndexacao) {

        if (supplyNetworkProjection == null) {
            throw new IllegalStateException(
                    "SupplyPlanningProjection requires Supply Network projection before "
                            + contextoIndexacao
                            + ".");
        }

    }

    public Queue<DistributionPlanItem> getDistributionPlanInboundQueue(ReferenciaPeriodo referenciaPeriodo, int periodoReferencia, Produto material) {

        if (referenciaPeriodo.equals(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL)) {
            
            return mapaDistributionPlanItemsInbound
                    .getOrDefault(periodoReferencia, new HashMap<>())
                    .getOrDefault(material, new LinkedList<>());
            
        } else {
            
            return mapaDistributionPlanItemsInbound
                    .values().stream()
                    .flatMap(x -> x.getOrDefault(material, new LinkedList<>()).stream())
                    .filter(x -> calendario.getPosicaoPeriodo(x.getDataExpedicao()) == periodoReferencia)
                    .collect(Collectors.toCollection(LinkedList::new));
            
        }
        
    }
    
    public Queue<DistributionPlanItem> getDistributionPlanInboundQueue(Produto material) {
                    
            return mapaDistributionPlanItemsInbound
                    .values().stream()
                    .flatMap(x -> x.getOrDefault(material, new LinkedList<>()).stream())
                    .collect(Collectors.toCollection(LinkedList::new)); 
        
    }
    
    public Queue<DistributionPlanItem> getDistributionPlanInboundQueue(ReferenciaPeriodo referenciaPeriodo, int periodoReferencia) {
        
        if (referenciaPeriodo.equals(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL)) {
            
            return mapaDistributionPlanItemsInbound.values().stream()
                    .map(x -> x.getOrDefault(periodoReferencia, new LinkedList<>()))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
            
        } else {
            
            return mapaDistributionPlanItemsInbound.values().stream()
                    .flatMap(x -> x.values().stream())
                    .flatMap(x -> x.stream())
                    .filter(x -> calendario.getPosicaoPeriodo(x.getDataExpedicao()) == periodoReferencia)
                    .collect(Collectors.toCollection(LinkedList::new));
                        
        }
    }
    
    public Queue<DistributionPlanItem> getDistributionPlanItemOutboundQueue(ReferenciaPeriodo referenciaPeriodo, int periodoReferencia, Produto material) {
        
        if (referenciaPeriodo.equals(ReferenciaPeriodo.CONSUMO_CAPACIDADE)) {
            
            return mapaDistributionPlanItemsOutbound
                    .getOrDefault(periodoReferencia, new HashMap<>())
                    .getOrDefault(material, new LinkedList<>());
            
        } else {
            
            return mapaDistributionPlanItemsOutbound
                    .values().stream()
                    .flatMap(x -> x.getOrDefault(material, new LinkedList<>()).stream())
                    .filter(x -> calendario.getPosicaoPeriodo(x.getDataExpedicao()) == periodoReferencia)
                    .collect(Collectors.toCollection(LinkedList::new));
            
        }
    }
    
    public Queue<DistributionPlanItem> getDistributionPlanItemOutboundQueue(Produto material) {
        
        return mapaDistributionPlanItemsOutbound
                .values().stream()
                .flatMap(x -> x.getOrDefault(material, new LinkedList<>()).stream())
                .collect(Collectors.toCollection(LinkedList::new));
            
    }
    
    public Queue<DistributionPlanItem> getDistributionPlanItemOutboundQueue(ReferenciaPeriodo referenciaPeriodo, int periodoReferencia) {
        
        if (referenciaPeriodo.equals(ReferenciaPeriodo.CONSUMO_CAPACIDADE)) {
            
            return mapaDistributionPlanItemsOutbound
                    .getOrDefault(periodoReferencia, new HashMap<>())
                    .values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
            
        } else {
            
            return FuncoesMap.flattenMapToStream(mapaDistributionPlanItemsOutbound, DistributionPlanItem.class)
                    .filter(x -> calendario.getPosicaoPeriodo(x.getDataExpedicao()) == periodoReferencia)
                    .collect(Collectors.toCollection(LinkedList::new));
                        
        }
    }
    
    /**
     * Retorna linhas onde location destino = CLIENTE_FINAL
     * @param referenciaPeriodo
     * @param periodoReferencia
     * @param material
     * @return 
     */
    public Queue<DistributionPlanItem> getDistributionPlanItemOutboundQueueParaClientes(ReferenciaPeriodo referenciaPeriodo, int periodoReferencia, Produto material) {
        
        return getDistributionPlanItemOutboundQueue(referenciaPeriodo, periodoReferencia, material).stream()
                .filter(x -> x.getLocationDestino().getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL) || x.getLocationDestino().getTipoLocation().equals(LocationAbstract.TipoLocation.REGIAO_COMERCIAL))
                .collect(Collectors.toCollection(LinkedList::new));
        
    }
    
    public Queue<DistributionPlanItem> getDistributionPlanItemOutboundQueueParaClientes(Produto material) {
        
        return SupplyPlanningProjection.this.getDistributionPlanItemOutboundQueue(material).stream()
                .filter(x -> x.getLocationDestino().getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL) || x.getLocationDestino().getTipoLocation().equals(LocationAbstract.TipoLocation.REGIAO_COMERCIAL))
                .collect(Collectors.toCollection(LinkedList::new));
        
    }
    
    /**
     * Retorna linhas onde location destino != CLIENTE_FINAL
     * @param referenciaPeriodo
     * @param periodoReferencia
     * @param material
     * @return 
     */
    public Queue<DistributionPlanItem> getDistributionPlanItemOutboundQueueParaLocationsInternas(ReferenciaPeriodo referenciaPeriodo, int periodoReferencia, Produto material) {
        
        return SupplyPlanningProjection.this.getDistributionPlanItemOutboundQueue(referenciaPeriodo, periodoReferencia, material).stream()
                .filter(x -> !x.getLocationDestino().getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL) && !x.getLocationDestino().getTipoLocation().equals(LocationAbstract.TipoLocation.REGIAO_COMERCIAL))
                .collect(Collectors.toCollection(LinkedList::new));
        
    }
                
    /**
     * Retorna lista de production plans associados a periodo/material output
     * @param posicaoPeriodo
     * @param material
     * @return 
     */
    public Queue<ProductionPlanLinha> getProductionPlanLinhaOutput(int posicaoPeriodo, Produto material) {
        
        // retorna um stream com pares <0: lista de elementos da chave do mapa, 1: productionPlanLinha>
        return FuncoesMap.flattenMapToStream(
                mapaProductionPlanLinhasOutput
                        .getOrDefault(posicaoPeriodo, new HashMap<>())
                        .getOrDefault(material, new HashMap<>()), 
                ProductionPlanLinha.class)
                        .collect(Collectors.toCollection(LinkedList::new));
        
    }
    
    /**
     * Retorna Optional<ProductionPlanLinha> associado a periodo/roteiro/lista técnica
     * Apenas um valor possível. Location e SupplyPlan determinados por este projection
     * Roteiro e Lista Técnica passados através dos argumentos
     * @param posicaoPeriodo
     * @param roteiro
     * @param listaTecnica
     * @return 
     */
    public Queue<ProductionPlanLinha> getProductionPlanLinhaOutput(int posicaoPeriodo, Roteiro roteiro, ListaTecnica listaTecnica) {
        
        return getProductionPlanLinhaOutput(posicaoPeriodo).stream()
                .filter(x -> x.getRoteiro().equals(roteiro) &&
                        x.getListaTecnica().equals(listaTecnica))
                .collect(Collectors.toCollection(LinkedList::new));
        
    }
    
    public Queue<ProductionPlanLinha> getProductionPlanLinhaOutput(int posicaoPeriodo, VersaoProducao versaoProducao) {
        
        validaVersaoProducaoIndexavel(versaoProducao);
        
        return mapaProductionPlanLinhasOutputPorVersaoProducao
                .getOrDefault(posicaoPeriodo, new HashMap<>())
                .getOrDefault(versaoProducao, new LinkedList<>());
        
    }
    
    public Optional<ProductionPlanLinha> getProductionPlanLinhaOutput(int posicaoPeriodo, VersaoProducao versaoProducao, Roteiro roteiro, ListaTecnica listaTecnica) {
        
        return getProductionPlanLinhaOutput(posicaoPeriodo, versaoProducao).stream()
                .filter(x -> x.getRoteiro().equals(roteiro) && x.getListaTecnica().equals(listaTecnica))
                .findAny();
        
    }

    public Queue<ProductionPlanLinha> getProductionPlanLinhaOutput(Roteiro roteiro, ListaTecnica listaTecnica) {
        
        Produto material = roteiro.getMaterialOutput();
        
        return mapaProductionPlanLinhasOutput.values().stream()
                .flatMap(x -> x
                        .getOrDefault(material, new HashMap<>())
                        .values().stream())
                .map(subMapa -> subMapa
                        .getOrDefault(roteiro, new HashMap<>())
                        .get(listaTecnica))
                .filter(x -> x != null)
                .collect(Collectors.toCollection(LinkedList::new));
                
    }
    
    /**
     * Retorna lista de production plans associados a periodo/material output
     * Lista filtrada por recurso produtivo
     * @param posicaoPeriodo
     * @param material
     * @return 
     */
    public Queue<ProductionPlanLinha> getProductionPlanLinhaOutput(int posicaoPeriodo, Produto material, RecursoProdutivo recursoProdutivo) {
        return getProductionPlanLinhaOutput(posicaoPeriodo, material).stream()
                .filter(x -> 
                        recursoProdutivo == null 
                        || supplyNetworkProjection // extrai roteiro com operações do supplyNetworkProjection para evitar erro lazy load
                                .getRoteiroFromId(x.getRoteiro().getId())
                                .orElseThrow(() -> getMissingPopulatedRoutingException(x.getRoteiro()))
                                .getRecursoProdutivoSet()
                                .contains(recursoProdutivo))
                .collect(Collectors.toCollection(LinkedList::new));
    }
    
    /**
     * Retorna lista de production plans associados a periodo/recurso produtivo
     * Lista já vem ordenada de acordo com a prioridade da receita de produção associada
     * Lista filtrada por recurso produtivo
     * @return
     */
    public Queue<ProductionPlanLinha> getProductionPlanLinhaOutput(RecursoProdutivo recursoProdutivo) {
        return getTodosProductionPlanLinhasOutput().stream()
                .filter(x -> 
                        recursoProdutivo == null 
                        || supplyNetworkProjection // extrai roteiro com operações do supplyNetworkProjection para evitar erro lazy load
                                .getRoteiroFromId(x.getRoteiro().getId())
                                .orElseThrow(() -> getMissingPopulatedRoutingException(x.getRoteiro()))
                                .getRecursoProdutivoSet()
                                .contains(recursoProdutivo))
                .collect(Collectors.toCollection(LinkedList::new));
    }
    
    /**
     * Retorna lista de production plans associados a periodo/recurso produtivo
     * Lista já vem ordenada de acordo com a prioridade da receita de produção associada
     * Lista filtrada por recurso produtivo
     * @return
     */
    public Queue<ProductionPlanLinha> getProductionPlanLinhaOutput(int posicaoPeriodo, RecursoProdutivo recursoProdutivo) {
        return getProductionPlanLinhaOutput(posicaoPeriodo).stream()
                .filter(x -> 
                        recursoProdutivo == null 
                        || supplyNetworkProjection // extrai roteiro com operações do supplyNetworkProjection para evitar erro lazy load
                                .getRoteiroFromId(x.getRoteiro().getId())
                                .orElseThrow(() -> getMissingPopulatedRoutingException(x.getRoteiro()))
                                .getRecursoProdutivoSet()
                                .contains(recursoProdutivo))
                .collect(Collectors.toCollection(LinkedList::new));
    }
    
    public Queue<ProductionPlanLinha> getProductionPlanLinhaOutput(int posicaoPeriodo, RecursoProdutivo recursoProdutivo, Produto materialOutput) {
        return getProductionPlanLinhaOutput(posicaoPeriodo, materialOutput).stream()
                .filter(x -> 
                        recursoProdutivo == null 
                        || supplyNetworkProjection // extrai roteiro com operações do supplyNetworkProjection para evitar erro lazy load
                                .getRoteiroFromId(x.getRoteiro().getId())
                                .orElseThrow(() -> getMissingPopulatedRoutingException(x.getRoteiro()))
                                .getRecursoProdutivoSet()
                                .contains(recursoProdutivo))
                .collect(Collectors.toCollection(LinkedList::new));
    }
    
    /**
     * Retorna todos os production plan linhas output para um dado período
     * @param posicaoPeriodo
     * @return 
     */
    public Queue<ProductionPlanLinha> getProductionPlanLinhaOutput(int posicaoPeriodo) {
        
        return FuncoesMap.flattenMapToStream(
                mapaProductionPlanLinhasOutput
                        .getOrDefault(posicaoPeriodo, new HashMap<>()),
                ProductionPlanLinha.class)
                .collect(Collectors.toCollection(LinkedList::new));
        
    }
    
    /**
     * Retorna todos os production plan linhas input para um dado período
     * @param posicaoPeriodo
     * @return 
     */
    public Queue<ProductionPlanLinha> getProductionPlanLinhaInput(int posicaoPeriodo) {
        
        return FuncoesMap.flattenMapToStream(
                mapaProductionPlanLinhasInput.getOrDefault(posicaoPeriodo, new HashMap<>()),
                ProductionPlanLinha.class)
                .collect(Collectors.toCollection(LinkedList::new));
        
    }
    
    public Queue<InventoryPlanLinha> getInventoryPlanLinhaPlanLinha(int posicaoPeriodo) {
        
        return mapaInventoryPlanLinhas.getOrDefault(posicaoPeriodo, new HashMap<>())
                .values().stream()
                .collect(Collectors.toCollection(LinkedList::new));
        
    }

    /**
     * Retorna lista de production plans associados a periodo/material input
     * @param posicaoPeriodo
     * @param materialInput
     * @return 
     */
    public Queue<ProductionPlanLinha> getProductionPlanLinhaInput(int posicaoPeriodo, Produto materialInput) {
        
        return FuncoesMap.flattenMapToStream(
                mapaProductionPlanLinhasInput
                        .getOrDefault(posicaoPeriodo, new ConcurrentHashMap<>())
                        .getOrDefault(materialInput, new ConcurrentHashMap<>()),
                ProductionPlanLinha.class)
                .collect(Collectors.toCollection(LinkedList::new));
                
    }
    
    public Queue<ProductionPlanLinha> getProductionPlanLinhaInput(int posicaoPeriodo, VersaoProducao versaoProducao) {
        
        validaVersaoProducaoIndexavel(versaoProducao);
        
        return mapaProductionPlanLinhasInputPorVersaoProducao
                .getOrDefault(posicaoPeriodo, new HashMap<>())
                .getOrDefault(versaoProducao, new LinkedList<>());
        
    }

    /**
     * Protege os indices por versao de producao usados pelo plano heuristico.
     *
     * <p>A versao reservada que representa ausencia e valida em algumas bordas
     * de cadastro, mas nao e chave dos indices de production plan. Chamadas que
     * chegam aqui precisam carregar uma versao persistida ou temporaria real
     * ou falhar antes de consultar os mapas.</p>
     */
    private void validaVersaoProducaoIndexavel(VersaoProducao versaoProducao) {

        if (versaoProducao == null) {
            throw getUnsupportedVersaoProducaoIndexavelException(null);
        }
        if (versaoProducao.isVersaoProducaoInexistente()) {
            throw getUnsupportedVersaoProducaoIndexavelException(versaoProducao);
        }

    }

    private IllegalArgumentException getUnsupportedVersaoProducaoIndexavelException(
            VersaoProducao versaoProducao) {

        return new IllegalArgumentException(
                "SupplyPlanningProjection indexes production plan lines only by real production versions; received "
                        + (versaoProducao == null ? "null" : versaoProducao.getClass().getSimpleName() + "(" + versaoProducao.getId() + ")")
                        + ". Sentinel or missing production versions must be resolved before querying production plan indexes.");

    }

    private IllegalStateException getMissingPopulatedRoutingException(Roteiro roteiro) {

        return new IllegalStateException(
                "SupplyPlanningProjection requires routing "
                        + getRoutingId(roteiro)
                        + " to be populated in SupplyNetworkProjection before filtering by production resource.");

    }
    
    /**
     * Retorna lista de production plans associados a periodo/material output
     * Lista já vem ordenada de acordo com a prioridade da receita de produção associada
     * @param posicaoPeriodo
     * @return
     */
    public Queue<ProductionPlanLinha> getProductionPlanLinhaInputComMaterialOutput(int posicaoPeriodo, Produto materialOutput) {
        
        return FuncoesMap.flattenMapToStream(
                mapaProductionPlanLinhasInput
                        .getOrDefault(posicaoPeriodo, new HashMap<>()),
                ProductionPlanLinha.class)
                .filter(x -> x.getMaterialOutput().equals(materialOutput))
                .collect(Collectors.toCollection(LinkedList::new));
        
    }
    
    public Optional<InventoryPlanLinha> getInventoryPlanLinha(int posicaoPeriodo, Produto material) {
        
        return Optional.ofNullable(mapaInventoryPlanLinhas
                .getOrDefault(posicaoPeriodo, new HashMap<>())
                .get(material));

    }
    public List<InventoryPlanLinha> getInventoryPlanLinhas(Produto material) {
        
        return mapaInventoryPlanLinhas.values().stream()
                .map(subMapa -> subMapa.get(material))
                .filter(inventoryPlanLinha -> inventoryPlanLinha != null)
                .collect(Collectors.toList());

    }
    public InventoryPlanLinha getOrAddInventoryPlanLinha(int posicaoPeriodo, Produto material) {
        
        return mapaInventoryPlanLinhas
                .computeIfAbsent(posicaoPeriodo, x -> new ConcurrentHashMap<>())
                .computeIfAbsent(material, x -> {
                    InventoryPlanLinha novoInventoryPlanLinha = new InventoryPlanLinha(
                            new InventoryPlanLinhaCompositeKey(
                                    getSupplyPlan(),
                                    getLocation(),
                                    material,
                                    getCalendario().getUltimaDataHorarioPeriodo(posicaoPeriodo)));
                    novoInventoryPlanLinha.setUnidadeMedida(clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(material, getLocation()));
                    return novoInventoryPlanLinha;
                });

    }
    
    /*
     * Leitura e escrita de quantidades funcionais do Supply Plan.
     *
     * Estes metodos sao a borda usada por rotinas e Planning Book para buscar
     * estoque, transito, safety stock, producao e distribuicao na unidade de
     * medida solicitada. Ausencia de linha significa ausencia real daquele
     * movimento no periodo/material e, por isso, retorna zero nas leituras
     * agregadas.
     */
    public double getQuantidadeEstoqueTransito(int posicaoPeriodo, Produto material, UnidadeMedida unidadeMedidaTarget) {
        
        Optional<InventoryPlanLinha> inventoryPlanLinhaOptional = getInventoryPlanLinha(posicaoPeriodo, material);

        // se não houver um InventoryPlanLinha retorna 0
        return inventoryPlanLinhaOptional
                .stream()
                .mapToDouble(inventoryPlanLinha -> inventoryPlanLinha.getQuantidadeEstoqueTransitoInboundNaUnidadeTarget(unidadeMedidaTarget, conversaoUnidadeMedidaProjection))
                .sum();

    }
    
    public double getQuantidadeEstoqueTransito(Produto material, UnidadeMedida unidadeMedidaTarget) {
        
        return getInventoryPlanLinhas(material)
                .stream()
                .mapToDouble(inventoryPlanLinha -> inventoryPlanLinha.getQuantidadeEstoqueTransitoInboundNaUnidadeTarget(unidadeMedidaTarget, conversaoUnidadeMedidaProjection))
                .sum();
                
    }
    
    public double getQuantidadeEstoqueProjetado(
            int posicaoPeriodo,
            Produto material,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Optional<InventoryPlanLinha> inventoryPlanLinhaOptional = getInventoryPlanLinha(posicaoPeriodo, material);

        // se não houver um InventoryPlanLinha retorna 0
        return inventoryPlanLinhaOptional
                .stream()
                .mapToDouble(inventoryPlanLinha -> inventoryPlanLinha.getQuantidadeEstoqueProjetadoNaUnidadeTarget(tipoPlano, unidadeMedidaTarget, conversaoUnidadeMedidaProjection))
                .sum();

    }
        
    public double getQuantidadeEstoqueProjetado(
            int posicaoPeriodo,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<InventoryPlanLinha> inventoryPlanLinhaQueue = getInventoryPlanLinhaPlanLinha(posicaoPeriodo);

        return inventoryPlanLinhaQueue
                .stream()
                .mapToDouble(inventoryPlanLinha -> inventoryPlanLinha.getQuantidadeEstoqueProjetadoNaUnidadeTarget(tipoPlano, unidadeMedidaTarget, conversaoUnidadeMedidaProjection))
                .sum();

    }
                
    public double getQuantidadeEstoqueSeguranca(
            int posicaoPeriodo,
            Produto material,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Optional<InventoryPlanLinha> inventoryPlanLinhaOptional = getInventoryPlanLinha(posicaoPeriodo, material);

        // se não houver um InventoryPlanLinha retorna 0
        return inventoryPlanLinhaOptional
                .stream()
                .mapToDouble(inventoryPlanLinha -> inventoryPlanLinha.getQuantidadeEstoqueSegurancaNaUnidadeTarget(tipoPlano, unidadeMedidaTarget, conversaoUnidadeMedidaProjection))
                .sum();

    }

    public double getQuantidadeEstoqueMaximo(
            int posicaoPeriodo,
            Produto material,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {

        Optional<InventoryPlanLinha> inventoryPlanLinhaOptional = getInventoryPlanLinha(posicaoPeriodo, material);

        // se não houver um InventoryPlanLinha retorna 0
        return inventoryPlanLinhaOptional
                .stream()
                .mapToDouble(inventoryPlanLinha -> inventoryPlanLinha.getQuantidadeEstoqueMaximoNaUnidadeTarget(tipoPlano, unidadeMedidaTarget, conversaoUnidadeMedidaProjection))
                .sum();

    }

    public void setQuantidadeEstoqueTransitoInbound(
            int posicaoPeriodo,
            Produto material,
            double valor,
            UnidadeMedida unidadeMedidaValor) {
                
        Optional<InventoryPlanLinha> inventoryPlanLinhaOptional = getInventoryPlanLinha(
                posicaoPeriodo,
                material);

        if (valor == 0 && inventoryPlanLinhaOptional.isEmpty()) return;

        InventoryPlanLinha inventoryPlanLinha = inventoryPlanLinhaOptional
                .orElse(getOrAddInventoryPlanLinha(posicaoPeriodo, material));

        inventoryPlanLinha.setQuantidadeEstoqueTransitoInboundEmUnidadeMedida(valor, unidadeMedidaValor, getConversaoUnidadeMedidaProjection());

    }
    
    public void setQuantidadeEstoqueProjetado(
            int posicaoPeriodo,
            Produto material,
            double valor,
            UnidadeMedida unidadeMedidaValor,
            TipoPlano tipoPlano) {

        Optional<InventoryPlanLinha> inventoryPlanLinhaOptional = getInventoryPlanLinha(
                posicaoPeriodo,
                material);

        if (valor == 0 && inventoryPlanLinhaOptional.isEmpty()) return;

        InventoryPlanLinha inventoryPlanLinha = inventoryPlanLinhaOptional
                .orElse(getOrAddInventoryPlanLinha(posicaoPeriodo, material));

        inventoryPlanLinha.setQuantidadeEstoqueProjetadoEmUnidadeMedida(valor, unidadeMedidaValor, tipoPlano, getConversaoUnidadeMedidaProjection());

    }
    
    public void setQuantidadeEstoqueSeguranca(
            int posicaoPeriodo,
            Produto material,
            double valor,
            UnidadeMedida unidadeMedidaValor,
            TipoPlano tipoPlano) {

        Optional<InventoryPlanLinha> inventoryPlanLinhaOptional = getInventoryPlanLinha(
                posicaoPeriodo,
                material);

        if (valor == 0 && inventoryPlanLinhaOptional.isEmpty()) return;

        InventoryPlanLinha inventoryPlanLinha = inventoryPlanLinhaOptional
                .orElse(getOrAddInventoryPlanLinha(posicaoPeriodo, material));

        inventoryPlanLinha.setQuantidadeEstoqueSegurancaEmUnidadeMedida(valor, unidadeMedidaValor, tipoPlano, getConversaoUnidadeMedidaProjection());

    }

    public void setQuantidadeEstoqueMaximo(
            int posicaoPeriodo,
            Produto material,
            double valor,
            UnidadeMedida unidadeMedidaValor,
            TipoPlano tipoPlano) {

        Optional<InventoryPlanLinha> inventoryPlanLinhaOptional = getInventoryPlanLinha(
                posicaoPeriodo,
                material);

        if (valor == 0 && inventoryPlanLinhaOptional.isEmpty()) return;

        InventoryPlanLinha inventoryPlanLinha = inventoryPlanLinhaOptional
                .orElse(getOrAddInventoryPlanLinha(posicaoPeriodo, material));

        inventoryPlanLinha.setQuantidadeEstoqueMaximoEmUnidadeMedida(valor, unidadeMedidaValor, tipoPlano, getConversaoUnidadeMedidaProjection());

    }

    public void adicionaQuantidadeEstoqueSeguranca(
            int posicaoPeriodo,
            Produto material,
            double valor,
            UnidadeMedida unidadeMedidaValor,
            TipoPlano tipoPlano) {
        
        setQuantidadeEstoqueSeguranca(
                posicaoPeriodo,
                material,
                getQuantidadeEstoqueSeguranca(posicaoPeriodo, material, tipoPlano, unidadeMedidaValor) + valor,
                unidadeMedidaValor,
                tipoPlano);
        
    }
    
    public double getQuantidadeDistributionPlanInbound(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<DistributionPlanItem> distributionPlanItemsInbound = getDistributionPlanInboundQueue(
                referenciaPeriodo, posicaoPeriodo);

        return distributionPlanItemsInbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado, tipoPlano, unidadeMedidaTarget, getConversaoUnidadeMedidaProjection()))
                .sum();

    }

    public double getQuantidadeDistributionPlanOutboundParaAtendimentoDemandaDireta(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            Produto material,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<DistributionPlanItem> distributionPlanItemsOutbound = getDistributionPlanItemOutboundQueue(referenciaPeriodo, posicaoPeriodo, material);

        return distributionPlanItemsOutbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeAtendimentoDemandaDiretaNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();

    }
    
    public double getQuantidadeDistributionPlanOutboundParaAtendimentoDemandaDireta(
            Produto material,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<DistributionPlanItem> distributionPlanItemsOutbound = getDistributionPlanItemOutboundQueue(material);

        return distributionPlanItemsOutbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeAtendimentoDemandaDiretaNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();

    }

    public double getQuantidadeDistributionPlanInbound(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            Produto material,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<DistributionPlanItem> distributionPlanItemsInbound = getDistributionPlanInboundQueue(
                referenciaPeriodo, posicaoPeriodo, material);

        return distributionPlanItemsInbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();

    }
    
    /**
     * ReferenciaPeriodo usado para determinar se posicaoPeriodo se refere ao periodo expedicao ou periodo recebimento
     * @return
     */
    public Queue<DistributionPlanItem> getDistributionPlanItemInboundListDeLocationsOrigemEReferenciaPeriodo(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            Produto material,
            Set<Location> locationsOrigem) {
        
        return getDistributionPlanInboundQueue(referenciaPeriodo, posicaoPeriodo, material).stream()
                .filter(x -> (locationsOrigem == null) || locationsOrigem.contains(x.getLocationOrigem()))
                .collect(Collectors.toCollection(LinkedList::new));
        
    }

    public double getQuantidadeDistributionPlanInbound(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            Produto material,
            Location locationOrigem,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {

        Queue<DistributionPlanItem> distributionPlanItemsInbound = SupplyPlanningProjection.this.getDistributionPlanInboundQueue(
                referenciaPeriodo, posicaoPeriodo, material).stream()
                    .filter(x -> x.getLocationOrigem().equals(locationOrigem))
                    .collect(Collectors.toCollection(LinkedList::new));

        return distributionPlanItemsInbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();

    }

    /**
     * ReferenciaPeriodo usado para determinar se posicaoPeriodo se refere ao periodo expedicao ou periodo recebimento
     * @return
     */
    public double getQuantidadeDistributionPlanItemInboundDeLocationsOrigem(
            Constantes.ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            Produto material,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget,
            Set<Location> locationsOrigem) {
        
        Queue<DistributionPlanItem> distributionPlanItemsInbound = getDistributionPlanItemInboundListDeLocationsOrigemEReferenciaPeriodo(
                referenciaPeriodo, posicaoPeriodo, material, locationsOrigem);

        return distributionPlanItemsInbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();

    }

    /**
     * Seta um valor (quantidade) na unidade de medida do projection
     * No caso de TipoDemanda = TOTAL, os ajustes são feitos exclusivamente em AJUSTE_SUPPLY
     * @param referenciaPeriodo ReferenciaPeriodo usado para determinar se posicaoPeriodo se refere ao periodo expedicao ou periodo recebimento
     * @param posicaoPeriodoReferencia tanto expedicao quanto recebimento ,a depender do parametro referenciaPeriodo
     */
    public void setQuantidadeDistributionPlanInbound(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodoReferencia,
            Produto material,
            Location locationOrigem,
            double valor,
            UnidadeMedida unidadeMedidaValor,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano) {
        
        if (!supplyPlan.getTamanhoBucket().equals(calendario.getTamanhoBucket())) {
            throw new IncompatibleCalendarException("Calendar time bucket different from supply plan time bucket");
        }
        

        Optional<DistributionPlanItem> optionalDistributionPlanItem = getDistributionPlanItemInboundListDeLocationsOrigemEReferenciaPeriodo(
                referenciaPeriodo, posicaoPeriodoReferencia, material, Sets.newHashSet(locationOrigem)).stream().findAny();

        // se não houver valor ou distribution plan linha existente, não criar um novo sem necessidade
        if (valor == 0 && !optionalDistributionPlanItem.isPresent()) return;

        // extrai ou cria distribution plan linha
        DistributionPlanItem distributionPlanItem = optionalDistributionPlanItem.orElseGet(() -> {

            Pair<LocalDateTime,LocalDateTime> datasExpedicaoERecebimento = DistributionPlanItem.getDatasExpedicaoERecebimentoDeReferencia(
                    referenciaPeriodo, calendario, posicaoPeriodoReferencia, 
                    getSupplyPlan().getVersaoMalha(),
                    material, locationOrigem, location, supplyNetworkProjection);
                        
            DistributionPlanItem novaDistributionPlanItem = new DistributionPlanItem(new DistributionPlanItemKey(
                    supplyPlan, getLocation(), locationOrigem, material, 
                    datasExpedicaoERecebimento.getValue0(), datasExpedicaoERecebimento.getValue1()));
            novaDistributionPlanItem.setUnidadeMedida(getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(material, getLocation()));

            addDistributionPlanItemInbound(novaDistributionPlanItem);
            return novaDistributionPlanItem;

        });

        distributionPlanItem.setQuantidadeEmUnidadeMedida(valor, unidadeMedidaValor, firmePlanejado, tipoPlano, getConversaoUnidadeMedidaProjection());

    }

    public void setQuantidadeDistributionPlanInbound(
            int posicaoPeriodoExpedicao,
            int posicaoPeriodoRecebimento,
            Produto material,
            Location locationOrigem,
            double valor,
            UnidadeMedida unidadeMedidaValor,
            FirmePlanejado firmePlanejado,
            Constantes.TipoPlano tipoPlano) {

        DistributionPlanItem distributionPlanItemInbound;
        Optional<DistributionPlanItem> optionalDistributionPlanItem = getDistributionPlanInboundQueue(
                        ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL,
                        posicaoPeriodoRecebimento,
                        material)
                .stream()
                .filter(x -> x.getLocationOrigem()
                        .equals(locationOrigem) && calendario.getPosicaoPeriodo(x.getDataExpedicao())
                        .equals(posicaoPeriodoExpedicao))
                .findAny();
        distributionPlanItemInbound = optionalDistributionPlanItem.orElseGet(() -> {

            DistributionPlanItem novaDistributionPlanItemInbound = new DistributionPlanItem(new DistributionPlanItemKey(
                    supplyPlan,
                    getLocation(),
                    locationOrigem,
                    material,
                    calendario.getPrimeiraDataHorarioPeriodo(posicaoPeriodoExpedicao),
                    calendario.getUltimoSegundoPeriodo(posicaoPeriodoRecebimento)));
            novaDistributionPlanItemInbound.setUnidadeMedida(getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(material, getLocation()));

            addDistributionPlanItemInbound(novaDistributionPlanItemInbound);
            return novaDistributionPlanItemInbound;

        });

        distributionPlanItemInbound.setQuantidadeEmUnidadeMedida(valor, unidadeMedidaValor, firmePlanejado, tipoPlano, getConversaoUnidadeMedidaProjection());

    }

    public void setQuantidadeDistributionPlanInboundParaAtendimentoDemandaDiretaEmUnidadeValor(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodoReferencia,
            Produto material,
            double valor,
            UnidadeMedida unidadeMedidaValor,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado) {
        
        Queue<DistributionPlanItem> distributionPlanItemList = getDistributionPlanInboundQueue(
                referenciaPeriodo, posicaoPeriodoReferencia, material);
        
        setQuantidadeDistributionPlanInboundParaAtendimentoDemandaDiretaEmUnidadeValor(distributionPlanItemList, valor, unidadeMedidaValor, tipoPlano, firmePlanejado);
                
    }

    private void setQuantidadeDistributionPlanInboundParaAtendimentoDemandaDiretaEmUnidadeValor(
            Collection<DistributionPlanItem> distributionPlanItemList,
            double valor,
            UnidadeMedida unidadeMedidaValor,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado) {
            
        // extratores necessários para se definir o BiConsumer setterCampoValorFinal
        Function<DistributionPlanItem,Produto> extratorMaterial = x -> x.getProduto();
        Function<DistributionPlanItem,UnidadeMedida> extratorUnidadeMedida = x -> x.getUnidadeMedida(getClusterEParametrosProjection().getParametrosGlobais());
        
        // define o extrator do valor de referência (valor total de ordens planejadas e firmes)
        ToDoubleFunction<DistributionPlanItem> extratorValorReferencia = x -> x.getQuantidade(firmePlanejado, tipoPlano);
        ToDoubleFunction<DistributionPlanItem> extratorValorReferenciaFinal = conversaoUnidadeMedidaProjection.funcaoGetQuantidadeNaUnidadeTarget(
                extratorValorReferencia, extratorMaterial, extratorUnidadeMedida, unidadeMedidaValor);
        
        // BiConsumer faz o set dos valores já convertendo para a unidade de medida do distribution plan linha,
        // através de consumerSetQuantidadeNaUnidadeTarget
        BiConsumer<DistributionPlanItem,Double> setterCampoValor = (x,valorSetado) -> x.setParcelaParaAtendimentoDemandaDireta(valorSetado, firmePlanejado, tipoPlano);
        BiConsumer<DistributionPlanItem,Double> setterCampoValorFinal = conversaoUnidadeMedidaProjection.consumerSetQuantidadeNaUnidadeTarget(
                unidadeMedidaValor, extratorMaterial, extratorUnidadeMedida, setterCampoValor);
        
        // Seta a parcela de atendimento indireto da demanda direta de maneira
        // proporcional às quantidades de referência das linhas inbound.
        MetodosUtilidade.setaValorProporcional(valor, distributionPlanItemList, extratorValorReferenciaFinal, setterCampoValorFinal);
                
    }
    
    public double getQuantidadeDistributionPlanInbound(
            int posicaoPeriodoExpedicao, int posicaoPeriodoRecebimento,
            Produto material,
            Location locationOrigem,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<DistributionPlanItem> distributionPlanItemsInbound = getDistributionPlanInboundQueue(
                ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, posicaoPeriodoRecebimento, material).stream()
                .filter(x -> x.getLocationOrigem().equals(locationOrigem) && calendario.getPosicaoPeriodo(x.getDataExpedicao()).equals(posicaoPeriodoExpedicao))
                .collect(Collectors.toCollection(LinkedList::new));

        return distributionPlanItemsInbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();

    }

    /**
     * Retorna o valor dos distribution plans com destino = CLIENTE_FINAL
     * @return
     */
    public double getQuantidadeDistributionPlanOutboundParaClientes(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            Produto material,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
 
        Queue<DistributionPlanItem> distributionPlanItemsOutbound = getDistributionPlanItemOutboundQueueParaClientes(
                referenciaPeriodo, posicaoPeriodo, material);

        return distributionPlanItemsOutbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();

    }

    public double getQuantidadeDistributionPlanOutboundParaClientes(
            Produto material, 
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
 
        Queue<DistributionPlanItem> distributionPlanItemsOutbound = getDistributionPlanItemOutboundQueueParaClientes(material);

        return distributionPlanItemsOutbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();
        
    }

    /**
     * Retorna o valor dos distribution plan linha onde location destino != CLIENTE_FINAL
     * @return
     */
    public double getQuantidadeDistributionPlanOutboundParaLocationsInternas(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            Produto material,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
 
        Queue<DistributionPlanItem> distributionPlanItemsOutbound = getDistributionPlanItemOutboundQueueParaLocationsInternas(
                referenciaPeriodo, posicaoPeriodo, material);

        return distributionPlanItemsOutbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();

    }

    /**
     * Se location destino diferente de CLIENTE_FINAL, retorna o valor dos distribution plan linha onde location destino
     * Caso contrário retorna 0
     * @return
     */
    public double getQuantidadeDistributionPlanOutboundParaLocationsInternas(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            Produto material,
            Location locationDestino,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {

        if (locationDestino.getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL) || locationDestino.getTipoLocation().equals(LocationAbstract.TipoLocation.REGIAO_COMERCIAL)) return 0;

        Queue<DistributionPlanItem> distributionPlanItemsOutbound = getDistributionPlanItemOutboundQueueParaLocationsInternas(
                referenciaPeriodo, posicaoPeriodo, material);
        
        distributionPlanItemsOutbound = distributionPlanItemsOutbound.stream()
                .filter(x -> x.getLocationDestino().equals(locationDestino))
                .collect(Collectors.toCollection(LinkedList::new));

        return distributionPlanItemsOutbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();
        
    }
    
    public double getQuantidadeDistributionPlanOutbound(
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            Produto material,
            Location locationDestino,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<DistributionPlanItem> distributionPlanItemsOutbound = SupplyPlanningProjection.this.getDistributionPlanItemOutboundQueue(referenciaPeriodo, posicaoPeriodo, material).stream()
                .filter(x -> x.getLocationDestino().equals(locationDestino))
                .collect(Collectors.toCollection(LinkedList::new));

        return distributionPlanItemsOutbound
                .stream()
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado,
                        tipoPlano,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();
        
    }
    
    public double getQuantidadeProductionPlan(
            int posicaoPeriodo,
            Produto material,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<ProductionPlanLinha> productionPlanLinhasOutput = getProductionPlanLinhaOutput(
                posicaoPeriodo, material);

        return productionPlanLinhasOutput
                .stream()
                .mapToDouble(productionPlanLinha -> productionPlanLinha.getQuantidade(
                        tipoPlano,
                        firmePlanejado,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();

    }
        
    public double getQuantidadeProductionPlan(
            int posicaoPeriodo,
            Produto material,
            VersaoProducao versaoProducao,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<ProductionPlanLinha> productionPlanLinhasOutput = getProductionPlanLinhaOutput(
                posicaoPeriodo, versaoProducao).stream()
                .filter(x -> x.getMaterialOutput().equals(material))
                .collect(Collectors.toCollection(LinkedList::new));

        return productionPlanLinhasOutput
                .stream()
                .mapToDouble(productionPlanLinha -> productionPlanLinha.getQuantidade(
                        tipoPlano,
                        firmePlanejado,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();
        
    }
    
    public double getQuantidadeProductionPlan(
            int posicaoPeriodo,
            Produto material,
            RecursoProdutivo recursoProdutivo,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<ProductionPlanLinha> productionPlanLinhasOutput = getProductionPlanLinhaOutput(
                posicaoPeriodo, material, recursoProdutivo);

        return productionPlanLinhasOutput
                .stream()
                .mapToDouble(productionPlanLinha -> productionPlanLinha.getQuantidade(
                        tipoPlano,
                        firmePlanejado,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();

    }
    
    public double getQuantidadeProductionPlan(
            int posicaoPeriodo,
            Produto material,
            Roteiro roteiro,
            Constantes.TipoDemanda tipoDemanda,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaTarget) {
        
        List<ProductionPlanLinha> productionPlanLinhasOutput = getProductionPlanLinhaOutput(posicaoPeriodo, material).stream()
                .filter(x -> x.getRoteiro().equals(roteiro))
                .collect(Collectors.toList());

        return productionPlanLinhasOutput
                .stream()
                .mapToDouble(productionPlanLinha -> productionPlanLinha.getQuantidade(
                        tipoPlano,
                        firmePlanejado,
                        unidadeMedidaTarget,
                        getConversaoUnidadeMedidaProjection()))
                .sum();
        
    }
    
    public double getQuantidadeMaterialInputConsumidoNoProductionPlan(
            int posicaoPeriodo,
            Produto materialInput,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {
        
        Queue<ProductionPlanLinha> productionPlanLinhasInput = getProductionPlanLinhaInput(posicaoPeriodo, materialInput);

        return productionPlanLinhasInput
                .stream()
                .mapToDouble(productionPlanLinha -> productionPlanLinha.getQuantidadeMaterialInputConsumido(
                        materialInput,
                        firmePlanejado,
                        tipoPlano,
                        getConversaoUnidadeMedidaProjection(),
                        getClusterEParametrosProjection(),
                        unidadeMedidaTarget))
                .sum();

    }
    
    /**
     * Extrai a quantidade produzida do material acabado através de planos de produção (mapa dos planos input). 
     * Considera apenas quantidade dependente de um material input
     * Para um tipo plano (restrito/irrestrito)
     * Converte valores para a unidade de medida do projection
     * @param posicaoPeriodo
     * @param materialOutput
     * @param tipoPlano
     * @return 
     */
    public double getQuantidadeMaterialOutputProduzidoDependenteDeMaterialInputEmProductionPlanLinhaInput (
            int posicaoPeriodo,
            Produto materialInput,
            Produto materialOutput,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano, 
            UnidadeMedida unidadeMedidaTarget) throws UnitOfMeasureConversionException {
        
        Queue<ProductionPlanLinha> productionPlanLinhasInput = getProductionPlanLinhaInput(posicaoPeriodo, materialInput).stream()
                .filter(x -> x.getMaterialOutput().equals(materialOutput))
                .collect(Collectors.toCollection(LinkedList::new));
        
        if (productionPlanLinhasInput.size() == 0) return 0;

        double quantidadeConsumoAcumulada = 0;
        for (ProductionPlanLinha productionPlanLinhaInput : productionPlanLinhasInput) {
            quantidadeConsumoAcumulada += productionPlanLinhaInput.getQuantidade(tipoPlano, firmePlanejado, unidadeMedidaTarget, getConversaoUnidadeMedidaProjection());
        }
        return quantidadeConsumoAcumulada;
    }
    
    /**
     * Seta valor de producao na unidade padrao do projection.
     *
     * <p>No Community, o ajuste manual atua sobre o único output do roteiro e
     * da lista técnica simples. A especialização Enterprise pode sobrescrever
     * esta operação para propagar a escala de um pacote múltiplo sem exigir que
     * o service chamador escolha o subtipo.</p>
     *
     * @param valor na unidade de medida padrao do projection.
     */
    public void setQuantidadeProductionPlan(
            int posicaoPeriodo,
            Produto material,
            VersaoProducao versaoProducao,
            double valor,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaValor) {
        
        // qtde de cada roteiro/lista tecnica para se atingir a producao desejada do material
        List<Triplet<Roteiro,ListaTecnica,Double>> detalhesVersaoProducao =
                supplyNetworkProjection.getDetalhePorVersaoProducao(
                        versaoProducao,
                        material,
                        unidadeMedidaValor,
                        valor);
        
        for (Triplet<Roteiro,ListaTecnica,Double> detalheVersaoProducao : detalhesVersaoProducao) {
            
            Roteiro roteiro = detalheVersaoProducao.getValue0();
            ListaTecnica listaTecnica = detalheVersaoProducao.getValue1();
            Produto materialComponenteRoteiroListaTecnica = roteiro.getMaterialOutput();
            double quantidadeComponenteRoteiroListaTecnica = detalheVersaoProducao.getValue2();
        
            // O contrato Community possui exatamente um output por combinação
            // de roteiro e lista técnica.
            if (!materialComponenteRoteiroListaTecnica.equals(material)) continue;
        
            // obtém o production plan linha ou o cria
            Optional<ProductionPlanLinha> optionalProductionPlanLinhaOutput = getProductionPlanLinhaOutput(
                    posicaoPeriodo, versaoProducao, roteiro, listaTecnica);
            if (optionalProductionPlanLinhaOutput.isEmpty() && valor == 0) return; // evita criação de novo production plan linha

            ProductionPlanLinha productionPlanLinhaOutput = optionalProductionPlanLinhaOutput.orElseGet(() -> {

                ProductionPlanLinha novoProductionPlanLinhaOutput = new ProductionPlanLinha(new ProductionPlanLinhaCompositeKey(
                        supplyPlan, 
                        getLocation(), 
                        versaoProducao,
                        roteiro, 
                        listaTecnica,
                        getCalendario().getUltimoSegundoPeriodo(posicaoPeriodo)),
                        materialComponenteRoteiroListaTecnica);
                novoProductionPlanLinhaOutput.setUnidadeMedida(getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(material, getLocation()));
                addProductionPlanLinhaOutput(novoProductionPlanLinhaOutput);
                return novoProductionPlanLinhaOutput;

            });

            productionPlanLinhaOutput.setQuantidade(
                    valor,
                    tipoPlano,
                    firmePlanejado,
                    unidadeMedidaValor,
                    getConversaoUnidadeMedidaProjection());

            // Há uma única combinação simples para o output editado no
            // Community.
            break;
            
        }
        
    }    

    /**
     * Modifica apenas a versão de produção mais prioritária que contenha o roteiro e lista técnica
     */
    public void setQuantidadeProductionPlan(
            int posicaoPeriodo,
            Roteiro roteiro,
            ListaTecnica listaTecnica,
            double valor,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaValor) {
        
        VersaoProducao versaoProducao = getSupplyNetworkProjection()
                .getVersaoProducaoViavelPrioritaria(roteiro, listaTecnica)
                .orElseThrow(() -> getMissingViableProductionVersionException(roteiro, listaTecnica));
        
        setQuantidadeProductionPlan(
                posicaoPeriodo, 
                roteiro.getMaterialOutput(), 
                versaoProducao, 
                valor, tipoPlano, firmePlanejado, unidadeMedidaValor);
        
    }

    private IllegalStateException getMissingViableProductionVersionException(
            Roteiro roteiro,
            ListaTecnica listaTecnica) {

        return new IllegalStateException(
                "SupplyPlanningProjection requires a viable production version before writing production plan "
                        + "by routing/BOM; routing="
                        + getRoutingId(roteiro)
                        + ", bom="
                        + getBomId(listaTecnica)
                        + ", location="
                        + getRoutingLocationId(roteiro)
                        + ", output material="
                        + getRoutingMaterialOutputId(roteiro)
                        + ". Community does not infer production plan lines without an explicit simple "
                        + "production version.");

    }

    private String getRoutingId(Roteiro roteiro) {

        return roteiro == null ? "null" : roteiro.getId();

    }

    private String getBomId(ListaTecnica listaTecnica) {

        return listaTecnica == null ? "null" : listaTecnica.getId();

    }

    private String getRoutingLocationId(Roteiro roteiro) {

        if (roteiro == null || roteiro.getLocation() == null) {
            return "null";
        }

        return roteiro.getLocation().getId();

    }

    private String getRoutingMaterialOutputId(Roteiro roteiro) {

        if (roteiro == null || roteiro.getMaterialOutput() == null) {
            return "null";
        }

        return roteiro.getMaterialOutput().getId();

    }
    
    public void modificaProductionPlan(
            int posicaoPeriodo, 
            Produto material, 
            VersaoProducao versaoProducao,
            double delta, 
            TipoPlano tipoPlano, FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaInput) {
        
        setQuantidadeProductionPlan(
                posicaoPeriodo, 
                material, 
                versaoProducao,
                getQuantidadeProductionPlan(
                        posicaoPeriodo, material, versaoProducao, tipoPlano, firmePlanejado, unidadeMedidaInput)
                + delta, tipoPlano, firmePlanejado, unidadeMedidaInput);
        
    }

    public Map<Produto,Double> getQuantidadeConsumidaMaterialInputPorMaterialOutput(
            int posicaoPeriodo, 
            Produto materialInput, 
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida) {

        Queue<ProductionPlanLinha> productionPlanLinhasInput = getProductionPlanLinhaInput(posicaoPeriodo, materialInput);
        
        Map<Produto,Double> mapaQuantidadeMaterialInputPorMaterialOutput = new HashMap<>();
        
        double quantidadeConsumoAcumulada = 0;
        for (ProductionPlanLinha productionPlanLinhaInput : productionPlanLinhasInput) {

            FuncoesMap.updateElementoNoNestedMap(
                    0.0,
                    valorAtual -> {
                        // soma a quantidade de consumo input deste production plan linha
                        // se valor atual = null, entao é a primeira ocorrência do material output. neste caso  
                        // usar o valor do próprio production plan linha, sem acumular com o valor atual
                        double valorAAdicionar = productionPlanLinhaInput.getQuantidadeMaterialInputConsumido(
                                materialInput, 
                                firmePlanejado, tipoPlano,
                                getConversaoUnidadeMedidaProjection(),
                                getClusterEParametrosProjection(),
                                unidadeMedida);
                        return valorAtual + valorAAdicionar;
                    },
                    Double.class,
                    mapaQuantidadeMaterialInputPorMaterialOutput, 
                    productionPlanLinhaInput.getMaterialOutput());
            
        }
        return mapaQuantidadeMaterialInputPorMaterialOutput;
        
    }

    public Queue<DistributionPlanItem> getTodosDistributionPlanItemsInboundSet() {
        return mapaDistributionPlanItemsInbound.values().stream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.stream())
                .collect(Collectors.toCollection(LinkedList::new));
    }
    
    public Queue<DistributionPlanItem> getTodosDistributionPlanItemsOutboundSet() {
        return mapaDistributionPlanItemsOutbound.values().stream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.stream())
                .collect(Collectors.toCollection(LinkedList::new));
    }
    
    public List<ProductionPlanLinha> getTodosProductionPlanLinhasOutput() {
        return FuncoesMap.flattenMapToList(mapaProductionPlanLinhasOutput, ProductionPlanLinha.class);
    }
    
    public List<InventoryPlanLinha> getTodosInventoryPlanLinhas() {
        return FuncoesMap.flattenMapToList(mapaInventoryPlanLinhas, InventoryPlanLinha.class);
    }
        
    public double getConsumoCapacidadeEmQuantidadeOuHorasProductionPlan(
            int posicaoPeriodo,
            RecursoProdutivo recursoProdutivo,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva) {
        
        switch (tipoCapacidadeProdutiva) {
            case QUANTIDADE_POR_UOM:
                return getConsumoCapacidadeEmQuantidadeNaUnidadeMedidaPadraoProductionPlan(
                        posicaoPeriodo, recursoProdutivo, tipoPlano, firmePlanejado);
            default:
                return getConsumoCapacidadeEmHorasProductionPlan(
                        posicaoPeriodo, recursoProdutivo, tipoPlano, firmePlanejado, tipoCapacidadeProdutiva);
        }

    }    
    /**
     * Método que converte produção output das ordens planejadas em horas-máquina para um 
     * dado recurso produtivo
     * @return
     */
    public double getConsumoCapacidadeEmHorasProductionPlan(
            int posicaoPeriodo,
            RecursoProdutivo recursoProdutivo,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva) {
        
        Queue<ProductionPlanLinha> productionPlanLinhasOutput = getProductionPlanLinhaOutput(posicaoPeriodo, recursoProdutivo);

        return productionPlanLinhasOutput.stream()
                .mapToDouble(x -> x.getCapacidadeConsumidaPorRecursoProdutivoEmHorasOuQuantidade(
                        tipoPlano, firmePlanejado, tipoCapacidadeProdutiva, supplyNetworkProjection)
                        .get(recursoProdutivo))
                .sum();

    }
    
    /**
     * Método que converte produção output das ordens planejadas para a quantidade
     * na unidade de capacidade do recurso produtivo
     * @return
     */
    public double getConsumoCapacidadeEmQuantidadeNaUnidadeMedidaPadraoProductionPlan(
            int posicaoPeriodo, 
            RecursoProdutivo recursoProdutivo,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado) {
        
        Queue<ProductionPlanLinha> productionPlanLinhasOutput = getProductionPlanLinhaOutput(posicaoPeriodo, recursoProdutivo);

        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        UnidadeMedida unidadeMedidaCapacidadeRecursoProdutivo = recursoProdutivo.getUnidadeMedidaCapacidadeEmUom(parametrosGlobais);
        
        return productionPlanLinhasOutput.stream()
                .mapToDouble(x -> x.getQuantidade(
                        tipoPlano,
                        firmePlanejado,
                        unidadeMedidaCapacidadeRecursoProdutivo, 
                        conversaoUnidadeMedidaProjection))
                .sum();

    }
    
    /**
     * Método que converte produção output das ordens planejadas para a quantidade
     * na unidade de capacidade do recurso produtivo
     * @return
     */
    public double getConsumoCapacidadeEmQuantidadeNaUnidadeMedidaPadraoProductionPlan(
            int posicaoPeriodo, 
            RecursoProdutivo recursoProdutivo,
            Produto material,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado) {
        
        Queue<ProductionPlanLinha> productionPlanLinhasOutput = getProductionPlanLinhaOutput(posicaoPeriodo, material, recursoProdutivo);

        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        UnidadeMedida unidadeMedidaCapacidadeRecursoProdutivo = recursoProdutivo.getUnidadeMedidaCapacidadeEmUom(parametrosGlobais);

        return productionPlanLinhasOutput.stream()
                .mapToDouble(x -> x.getQuantidade(
                        tipoPlano,
                        firmePlanejado,
                        unidadeMedidaCapacidadeRecursoProdutivo,
                        conversaoUnidadeMedidaProjection))
                .sum();

    }

    
    /**
     * Método que converte produção output das ordens planejadas em horas-máquina para um 
     * dado recurso produtivo e um dado material
     * @return
     */
    public double getConsumoCapacidadeEmHorasProductionPlan(
            int posicaoPeriodo, 
            RecursoProdutivo recursoProdutivo,
            Produto material,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado) {
                
        Queue<ProductionPlanLinha> productionPlanLinhasOutput = getProductionPlanLinhaOutput(posicaoPeriodo, recursoProdutivo, material);

        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva = getPerfilExecucaoSupplyPlanConsiderado().getTipoCapacidadeProdutiva();

        return productionPlanLinhasOutput.stream()
                .mapToDouble(x -> x.getCapacidadeConsumidaPorRecursoProdutivoEmHorasOuQuantidade(
                        tipoPlano, firmePlanejado, tipoCapacidadeProdutiva, supplyNetworkProjection)
                        .get(recursoProdutivo))
                .sum();

    }
    
    public double getConsumoCapacidadeEmQuantidadeOuHorasProductionPlan(
            int posicaoPeriodo,
            RecursoProdutivo recursoProdutivo,
            Produto material,
            TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva) {
                
        switch (tipoCapacidadeProdutiva) {
            case QUANTIDADE_POR_UOM:
                return getConsumoCapacidadeEmQuantidadeNaUnidadeMedidaPadraoProductionPlan(
                        posicaoPeriodo, recursoProdutivo, material, tipoPlano, firmePlanejado);
            default:
                return getConsumoCapacidadeEmHorasProductionPlan(
                        posicaoPeriodo, recursoProdutivo, material, tipoPlano, firmePlanejado);
        }
        
    }

    public Set<Location> getLocationsOutboundDeDistributionPlansOutbound(ReferenciaPeriodo referenciaPeriodo, int posicaoPeriodo, Produto material) {
        return SupplyPlanningProjection.this.getDistributionPlanItemOutboundQueue(referenciaPeriodo, posicaoPeriodo, material).stream()
                .map(DistributionPlanItem::getLocationDestino)
                .collect(Collectors.toSet());
    }

    public void replaceMaterialProjection(MaterialProjection materialProjection) {
        this.materialProjection = materialProjection;
    }

}
