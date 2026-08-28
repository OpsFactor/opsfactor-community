package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha.InventoryPlanLinhaCompositeKey;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionLocationProduto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionProduto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.service.SupplyPlanPersistedBaselinePreflight;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory de projections de Supply Planning Community.
 *
 * <p>Esta classe materializa em memoria o subconjunto operacional usado pelo
 * motor heuristico: inventory plan, distribution plan, production plan e
 * demanda direta considerada. Custos, precos, pedidos transacionais, flows
 * otimizados, P&L, cost-to-serve e diagnosticos de solver pertencem ao
 * Enterprise.</p>
 */
@Component
public class SupplyPlanProjectionFactory {

    /**
     * Repository das linhas de distribuicao planejadas/restritas/trabalho.
     * Consultas sao feitas por lote de materiais e locations para evitar N+1
     * durante a montagem das projections.
     */
    @Autowired
    private DistributionPlanItemRepository distributionPlanItemRepository;

    /**
     * Repository das linhas de estoque projetado, estoque inicial e transito.
     */
    @Autowired
    private InventoryPlanLinhaRepository inventoryPlanLinhaRepository;

    /**
     * Repository das linhas de producao output/input usadas pela heuristica e
     * pelo plano restrito.
     */
    @Autowired
    private ProductionPlanLinhaRepository productionPlanLinhaRepository;

    /**
     * Gate escalar que impede a composicao de uma projection nova a partir de
     * valores baseline depreciados ainda efetivos na base.
     */
    @Autowired
    private SupplyPlanPersistedBaselinePreflight supplyPlanPersistedBaselinePreflight;

    /**
     * Factory da demanda direta considerada Community, derivada do Demand Plan
     * e sem pedidos transacionais Enterprise.
     */
    @Autowired
    private DemandaDiretaConsideradaProjectionFactory demandaDiretaConsideradaProjectionFactory;

    /**
     * Cria uma projection vazia para uma location, usando o calendario do Supply
     * Plan ajustado ao perfil Community.
     */
    public SupplyPlanningProjection getSupplyPlanningProjectionVazio(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            Location location,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection =
                supplyNetworkProjection.getClusterEParametrosProjection();

        Calendario calendario = supplyPlan.getCalendarioDoSupplyPlanParaLocation(clusterEParametrosProjection, location);

        SupplyPlanningProjection supplyPlanningProjection = new SupplyPlanningProjection(
                supplyPlan, perfilExecucaoSupplyPlan,
                supplyNetworkProjection, politicaEstoquesProjection,
                calendario, location,
                materialProjection, locationProjection);

        return supplyPlanningProjection;

    }

    /**
     * Cria projections vazias para todas as locations ativas do recorte
     * recebido.
     */
    public SupplyPlanningMultiplasLocationsProjection getSupplyPlanningMultiplasLocationsProjectionVazio(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection =
                supplyNetworkProjection.getClusterEParametrosProjection();

        SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection = new SupplyPlanningMultiplasLocationsProjection(
                supplyPlan, perfilExecucaoSupplyPlan,
                supplyNetworkProjection, politicaEstoquesProjection,
                materialProjection, locationProjection);

        for (Location location : locationProjection.getLocationsAtivas()) {
            Calendario calendario = supplyPlan.getCalendarioDoSupplyPlanParaLocation(clusterEParametrosProjection, location);

            SupplyPlanningProjection supplyPlanningProjection = new SupplyPlanningProjection(
                    supplyPlan, perfilExecucaoSupplyPlan,
                    supplyNetworkProjection, politicaEstoquesProjection,
                    calendario, location,
                    materialProjection, locationProjection);

            supplyPlanningMultiplasLocationsProjection.addSupplyPlanningProjection(supplyPlanningProjection);
        }

        return supplyPlanningMultiplasLocationsProjection;

    }

    /**
     * Resolve a fotografia estrutural minima para criar projections de Supply
     * Planning derivadas do calendario do plano.
     *
     * <p>A factory e chamada por services Community e por overlays Enterprise.
     * Falhar aqui deixa explicito quando a montagem do snapshot esta incompleta,
     * em vez de deixar o erro aparecer dentro de `SupplyPlan`,
     * `PerfilExecucaoSupplyPlan` ou `SupplyPlanningProjection`.</p>
     */
    /**
     * Construtor que permite o uso de um calendário com horizonte de tempo menor que do plano para limitar as buscas de dados
     * @param calendario precisa ser do mesmo tamanho de bucket que o Supply Plan
     * @return
     */
    public SupplyPlanningProjection getSupplyPlanningProjectionVazio(
            SupplyPlan supplyPlan,
            Calendario calendario,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            Location location,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection) {

        if (calendario == null
                || supplyPlan == null
                || !Objects.equals(calendario.getTamanhoBucket(), supplyPlan.getTamanhoBucket())) {
            throw getIncompatibleSupplyPlanCalendarException(supplyPlan, calendario);
        }

        SupplyPlanningProjection supplyPlanningProjection = new SupplyPlanningProjection(
                supplyPlan, perfilExecucaoSupplyPlan,
                supplyNetworkProjection, politicaEstoquesProjection,
                calendario, location,
                materialProjection, locationProjection);

        return supplyPlanningProjection;

    }

    private IllegalArgumentException getIncompatibleSupplyPlanCalendarException(
            SupplyPlan supplyPlan,
            Calendario calendario) {

        return new IllegalArgumentException(
                "SupplyPlanProjectionFactory requires the projection calendar bucket to match the Supply Plan bucket; projection bucket="
                        + (calendario == null ? "null" : calendario.getTamanhoBucket())
                        + ", supply plan bucket="
                        + (supplyPlan == null ? "null" : supplyPlan.getTamanhoBucket())
                        + ". Build the Supply Planning projection with a calendar derived from the same Supply Plan.");

    }

    /**
     * Valida linhas de Inventory Plan carregadas por repository antes de
     * indexacao paralela na projection.
     *
     * <p>Lista vazia continua snapshot operacional valido. Colecao nula, item
     * nulo ou chave composta incompleta indicam quebra da borda JPA/projection e
     * devem falhar aqui, antes de `parallelStream` ou dos mapas concorrentes da
     * `SupplyPlanningProjection`.</p>
     */
    private Collection<InventoryPlanLinha> validaInventoryPlanLinhaRepositoryResult(
            Collection<InventoryPlanLinha> inventoryPlanLinhas,
            String contextoOperacional) {

        if (inventoryPlanLinhas == null) {
            throw new IllegalStateException(
                    contextoOperacional
                            + " repository returned null Inventory Plan line collection.");
        }

        int indiceInventoryPlanLinha = 0;
        for (InventoryPlanLinha inventoryPlanLinha : inventoryPlanLinhas) {
            if (inventoryPlanLinha == null) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " repository returned null Inventory Plan line at index "
                                + indiceInventoryPlanLinha
                                + ".");
            }
            if (inventoryPlanLinha.getInventoryPlanLinhaCompositeKey() == null
                    || inventoryPlanLinha.getSupplyPlan() == null
                    || inventoryPlanLinha.getLocation() == null
                    || inventoryPlanLinha.getProduto() == null
                    || inventoryPlanLinha.getDataReferencia() == null) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " repository returned Inventory Plan line without supply plan, location, material or reference date at index "
                                + indiceInventoryPlanLinha
                                + ".");
            }
            indiceInventoryPlanLinha++;
        }

        return inventoryPlanLinhas;

    }

    /**
     * Valida transferencias planejadas carregadas por repository antes de
     * separar entradas e saidas da projection.
     */
    private Collection<DistributionPlanItem> validaDistributionPlanItemRepositoryResult(
            Collection<DistributionPlanItem> distributionPlanItems,
            String contextoOperacional) {

        if (distributionPlanItems == null) {
            throw new IllegalStateException(
                    contextoOperacional
                            + " repository returned null Distribution Plan line collection.");
        }

        int indiceDistributionPlanItem = 0;
        for (DistributionPlanItem distributionPlanItem : distributionPlanItems) {
            if (distributionPlanItem == null) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " repository returned null Distribution Plan line at index "
                                + indiceDistributionPlanItem
                                + ".");
            }
            if (distributionPlanItem.getKey() == null
                    || distributionPlanItem.getSupplyPlan() == null
                    || distributionPlanItem.getLocationOrigem() == null
                    || distributionPlanItem.getLocationDestino() == null
                    || distributionPlanItem.getProduto() == null
                    || distributionPlanItem.getDataExpedicao() == null
                    || distributionPlanItem.getDataRecebimento() == null) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " repository returned Distribution Plan line without supply plan, origin, destination, material, shipping date or receiving date at index "
                                + indiceDistributionPlanItem
                                + ".");
            }
            indiceDistributionPlanItem++;
        }

        return distributionPlanItems;

    }

    /**
     * Valida linhas de Production Plan carregadas por repository antes de
     * montar os indices de outputs e inputs produtivos.
     *
     * <p>Os repositories retornam `List` para preservar cardinalidade do
     * snapshot ate esta validation. Chave composta duplicada indica quebra de
     * consistencia antes dos mapas paralelos da projection.</p>
     */
    private Collection<ProductionPlanLinha> validaProductionPlanLinhaRepositoryResult(
            Collection<ProductionPlanLinha> productionPlanLinhas,
            String contextoOperacional) {

        if (productionPlanLinhas == null) {
            throw new IllegalStateException(
                    contextoOperacional
                            + " repository returned null Production Plan line collection.");
        }

        Set<ProductionPlanLinha.ProductionPlanLinhaCompositeKey> chavesProductionPlanLinha = new HashSet<>();
        int indiceProductionPlanLinha = 0;
        for (ProductionPlanLinha productionPlanLinha : productionPlanLinhas) {
            if (productionPlanLinha == null) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " repository returned null Production Plan line at index "
                                + indiceProductionPlanLinha
                                + ".");
            }
            if (productionPlanLinha.getProductionPlanLinhaCompositeKey() == null
                    || productionPlanLinha.getSupplyPlan() == null
                    || productionPlanLinha.getLocation() == null
                    || productionPlanLinha.getMaterialOutput() == null
                    || productionPlanLinha.getDataReferencia() == null
                    || productionPlanLinha.getRoteiro() == null
                    || productionPlanLinha.getListaTecnica() == null
                    || productionPlanLinha.getVersaoProducao() == null) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " repository returned Production Plan line without supply plan, location, output material, production version, routing, bill of materials or reference date at index "
                                + indiceProductionPlanLinha
                                + ".");
            }
            if (!chavesProductionPlanLinha.add(productionPlanLinha.getProductionPlanLinhaCompositeKey())) {
                throw new IllegalStateException(
                        contextoOperacional
                                + " repository returned duplicated Production Plan line for supply plan "
                                + productionPlanLinha.getSupplyPlan().getId()
                                + ", location "
                                + productionPlanLinha.getLocation().getId()
                                + ", production version "
                                + productionPlanLinha.getVersaoProducao().getId()
                                + ", routing "
                                + productionPlanLinha.getRoteiro().getId()
                                + ", bill of materials "
                                + productionPlanLinha.getListaTecnica().getId()
                                + " and reference date "
                                + productionPlanLinha.getDataReferencia()
                                + ".");
            }
            indiceProductionPlanLinha++;
        }

        return productionPlanLinhas;

    }

    public SupplyPlanningProjection getSupplyPlanningProjectionCompleto(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            Location location,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection) {

        SupplyPlanningProjection supplyPlanningProjection = getSupplyPlanningProjectionVazio(
                supplyPlan, perfilExecucaoSupplyPlan, location, supplyNetworkProjection, politicaEstoquesProjection,
                materialProjection, locationProjection);

        supplyPlanPersistedBaselinePreflight.assertSupplyPlanReadyForCanonicalRuntime(
                supplyPlanningProjection.getSupplyPlan().getId());

        populaSupplyPlanningProjectionComInventoryPlan(supplyPlanningProjection);
        populaSupplyPlanningProjectionComDistributionPlanInbound(supplyPlanningProjection);
        populaSupplyPlanningProjectionComDistributionPlanOutbound(supplyPlanningProjection);
        populaSupplyPlanningProjectionComProductionPlanOutput(supplyPlanningProjection);
        populaSupplyPlanningProjectionComProductionPlanInput(supplyPlanningProjection);
        populaSupplyPlanningProjectionComDemandaDiretaConsideradaProjection(supplyPlanningProjection);

        return supplyPlanningProjection;
    }



    /**
     * Popula linhas do Inventory Plan na location do planningProjection.
     * Carrega estoque inicial (periodo -1) apenas se já estiver populado na base InventoryPlanLinha
     * (carga prévia através do método carregaInventoryPlanComEstoqueInicial
     * Também carrega a linha de estoque em trânsito
     * @param planningProjection
     */
    public void populaSupplyPlanningProjectionComInventoryPlan(SupplyPlanningProjection planningProjection) {

        Collection<InventoryPlanLinha> inventoryPlanLinhas = inventoryPlanLinhaRepository.findByInventoryPlanLinhaCompositeKeySupplyPlanIdAndInventoryPlanLinhaCompositeKeyLocationAndInventoryPlanLinhaCompositeKeyProdutoIn(
                planningProjection.getSupplyPlan().getId(), planningProjection.getLocation(),
                planningProjection.getMaterialProjection().getMateriaisAtivos());

        validaInventoryPlanLinhaRepositoryResult(
                inventoryPlanLinhas,
                "Inventory Plan projection population")
                .parallelStream().forEach(inventoryPlanLinha -> {
            planningProjection.addInventoryPlanLinha(inventoryPlanLinha);
        });

    }

    /**
     * Popula linhas do Inventory Plan na location do planningProjection.
     * Carrega estoque inicial (periodo -1) apenas se já estiver populado na base InventoryPlanLinha
     * (carga prévia através do método carregaInventoryPlanComEstoqueInicial
     * Também carrega a linha de estoque em trânsito
     */
    public void populaSupplyPlanningMultiplasLocationsProjectionComInventoryPlan(SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection = supplyPlanningMultiplasLocationsProjection.getClusterEParametrosProjection();

        Collection<InventoryPlanLinha> inventoryPlanLinhas = inventoryPlanLinhaRepository.findByInventoryPlanLinhaCompositeKeySupplyPlanIdAndInventoryPlanLinhaCompositeKeyLocationInAndInventoryPlanLinhaCompositeKeyProdutoIn(
                supplyPlanningMultiplasLocationsProjection.getSupplyPlan().getId(),
                supplyPlanningMultiplasLocationsProjection.getLocationProjection().getLocationsAtivas(),
                supplyPlanningMultiplasLocationsProjection.getMaterialProjection().getMateriaisAtivos());

        validaInventoryPlanLinhaRepositoryResult(
                inventoryPlanLinhas,
                "multi-location Inventory Plan projection population")
                .parallelStream()
                .filter(inventoryPlanLinha -> clusterEParametrosProjection.isDfuAtiva(inventoryPlanLinha.getProduto(), inventoryPlanLinha.getLocation()))
                .forEach(inventoryPlanLinha -> {
                    supplyPlanningMultiplasLocationsProjection
                            .getSupplyPlanningProjectionDeLocation(inventoryPlanLinha.getLocation())
                            .addInventoryPlanLinha(inventoryPlanLinha);
                });

    }

    /**
     * Agrega os estoques dos inputs produtivos
     * Requer um mapa de production plan linhas output já populado
     * @param planningProjection
     * @param posicaoPeriodo
     */
    public void populaSupplyPlanningProjectionComInventoryPlanDeInputsProducao(SupplyPlanningProjection planningProjection, int posicaoPeriodo) {

        SupplyNetworkProjection supplyNetworkProjection = planningProjection.getSupplyNetworkProjection();

        Set<Produto> inputsProducao = planningProjection.getProductionPlanLinhaOutput(posicaoPeriodo).stream()
                .flatMap(x -> x.getMateriaisInput(supplyNetworkProjection).stream())
                .collect(Collectors.toSet());

        Collection<InventoryPlanLinha> inventoryPlanLinhas = inventoryPlanLinhaRepository.findByInventoryPlanLinhaCompositeKeySupplyPlanIdAndInventoryPlanLinhaCompositeKeyLocationAndInventoryPlanLinhaCompositeKeyProdutoIn(
                planningProjection.getSupplyPlan().getId(),
                planningProjection.getLocation(),
                inputsProducao);

        validaInventoryPlanLinhaRepositoryResult(
                inventoryPlanLinhas,
                "production input Inventory Plan projection population")
                .parallelStream().forEach(inventoryPlanLinha -> {
            planningProjection.addInventoryPlanLinha(inventoryPlanLinha);
        });

    }

    /**
     * Popula distribution plan linhas onde com destino na location do Projection (entradas)
     * @param planningProjection
     */
    public void populaSupplyPlanningProjectionComDistributionPlanInbound(SupplyPlanningProjection planningProjection) {

        Collection<DistributionPlanItem> distributionPlanItemsInbound = distributionPlanItemRepository.findByKeySupplyPlanIdAndKeyLocationDestinoAndKeyProdutoIn(
                planningProjection.getSupplyPlan().getId(),
                planningProjection.getLocation(),
                planningProjection.getMaterialProjection().getMateriaisAtivosEmLocation(planningProjection.getLocation()));

        validaDistributionPlanItemRepositoryResult(
                distributionPlanItemsInbound,
                "Distribution Plan inbound projection population")
                .parallelStream().forEach(distributionPlanItemInbound -> {
            planningProjection.addDistributionPlanItemInbound(distributionPlanItemInbound);
        });

    }

    /**
     * Popula distribution plan linhas onde com destino na location do Projection (entradas)
     */
    public void populaSupplyPlanningMultiplasLocationsProjectionComDistributionPlanInbound(SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection = supplyPlanningMultiplasLocationsProjection.getClusterEParametrosProjection();

        Collection<DistributionPlanItem> distributionPlanItemsInbound = distributionPlanItemRepository.findByKeySupplyPlanIdAndKeyLocationDestinoInAndKeyProdutoIn(
                supplyPlanningMultiplasLocationsProjection.getSupplyPlan().getId(),
                supplyPlanningMultiplasLocationsProjection.getLocationProjection().getLocationsAtivas(),
                supplyPlanningMultiplasLocationsProjection.getMaterialProjection().getMateriaisAtivos());

        validaDistributionPlanItemRepositoryResult(
                distributionPlanItemsInbound,
                "multi-location Distribution Plan inbound projection population")
                .parallelStream()
                .filter(distributionPlanItem -> clusterEParametrosProjection.isDfuAtiva(distributionPlanItem.getProduto(), distributionPlanItem.getLocationDestino()))
                .forEach(distributionPlanItem -> {
                    supplyPlanningMultiplasLocationsProjection
                            .getSupplyPlanningProjectionDeLocation(distributionPlanItem.getLocationDestino())
                            .addDistributionPlanItemInbound(distributionPlanItem);
                });

    }

    /**
     * Popula distribution plan linhas onde com destino na location do Projection (entradas)
     * @param planningProjection
     */
    public void populaSupplyPlanningProjectionComDistributionPlanInboundDeLocationOrigem(SupplyPlanningProjection planningProjection, Location locationOrigem) {

        ClusterEParametrosProjection clusterEParametrosProjection = planningProjection.getClusterEParametrosProjection();

        Collection<DistributionPlanItem> distributionPlanItemsInbound = distributionPlanItemRepository.findByKeySupplyPlanIdAndKeyLocationDestinoAndKeyLocationOrigemAndKeyProdutoIn(
                planningProjection.getSupplyPlan().getId(),
                planningProjection.getLocation(),
                locationOrigem,
                planningProjection.getMaterialProjection().getMateriaisAtivosEmLocation(planningProjection.getLocation()));

        validaDistributionPlanItemRepositoryResult(
                distributionPlanItemsInbound,
                "Distribution Plan inbound by origin projection population")
                .parallelStream().forEach(distributionPlanItemInbound -> {
            planningProjection.addDistributionPlanItemInbound(distributionPlanItemInbound);
        });

    }

    /**
     * Popula distribution plan linhas onde com origem na location do Projection (demanda indireta)
     * OBS : POR SE TRATAR DE DEMANDA INDIRETA A DATA CONSIDERADA É A DA EMISSÃO
     * E NÃO A DATA REFERÊNCIA
     * @param planningProjection
     */
    public void populaSupplyPlanningProjectionComDistributionPlanOutbound(SupplyPlanningProjection planningProjection) {

        Collection<DistributionPlanItem> distributionPlanItemsOutbound = distributionPlanItemRepository.findByKeySupplyPlanIdAndKeyLocationOrigemAndKeyProdutoIn(
                planningProjection.getSupplyPlan().getId(),
                planningProjection.getLocation(),
                planningProjection.getMaterialProjection().getMateriaisAtivosEmLocation(planningProjection.getLocation()));

        validaDistributionPlanItemRepositoryResult(
                distributionPlanItemsOutbound,
                "Distribution Plan outbound projection population")
                .parallelStream().forEach(distributionPlanItemOutbound -> {
            planningProjection.addDistributionPlanItemOutbound(distributionPlanItemOutbound);
        });

    }

    public void populaSupplyPlanningMultiplasLocationsProjectionComDistributionPlanOutbound(SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection = supplyPlanningMultiplasLocationsProjection.getClusterEParametrosProjection();

        Collection<DistributionPlanItem> distributionPlanItemsOutbound = distributionPlanItemRepository.findByKeySupplyPlanIdAndKeyLocationOrigemInAndKeyProdutoIn(
                supplyPlanningMultiplasLocationsProjection.getSupplyPlan().getId(),
                supplyPlanningMultiplasLocationsProjection.getLocationProjection().getLocationsAtivas(),
                supplyPlanningMultiplasLocationsProjection.getMaterialProjection().getMateriaisAtivos());

        validaDistributionPlanItemRepositoryResult(
                distributionPlanItemsOutbound,
                "multi-location Distribution Plan outbound projection population")
                .parallelStream()
                .filter(distributionPlanItem -> clusterEParametrosProjection.isDfuAtiva(distributionPlanItem.getProduto(), distributionPlanItem.getLocationOrigem()))
                .forEach(distributionPlanItemOutbound -> {
                    supplyPlanningMultiplasLocationsProjection
                            .getSupplyPlanningProjectionDeLocation(distributionPlanItemOutbound.getLocationOrigem())
                            .addDistributionPlanItemOutbound(distributionPlanItemOutbound);
                });

    }

    public void populaSupplyPlanningProjectionComProductionPlanOutput(SupplyPlanningProjection planningProjection) {

        Collection<ProductionPlanLinha> productionPlanLinhas = productionPlanLinhaRepository.findByProductionPlanLinhaCompositeKeySupplyPlanIdAndProductionPlanLinhaCompositeKeyLocationAndMaterialOutputIn(
                planningProjection.getSupplyPlan().getId(),
                planningProjection.getLocation(),
                planningProjection.getMaterialProjection().getMateriaisAtivosEmLocation(planningProjection.getLocation()));

        validaProductionPlanLinhaRepositoryResult(
                productionPlanLinhas,
                "Production Plan output projection population")
                .stream().forEach(productionPlanLinha -> {
            planningProjection.addProductionPlanLinhaOutput(productionPlanLinha);
        });
    }

    public void populaSupplyPlanningMultiplasLocationsProjectionComProductionPlanOutput(SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection = supplyPlanningMultiplasLocationsProjection.getClusterEParametrosProjection();

        Collection<ProductionPlanLinha> productionPlanLinhas = productionPlanLinhaRepository.findByProductionPlanLinhaCompositeKeySupplyPlanAndProductionPlanLinhaCompositeKeyLocationInAndMaterialOutputIn(
                supplyPlanningMultiplasLocationsProjection.getSupplyPlan(),
                supplyPlanningMultiplasLocationsProjection.getLocationProjection().getLocationsAtivas(),
                supplyPlanningMultiplasLocationsProjection.getMaterialProjection().getMateriaisAtivos());

        validaProductionPlanLinhaRepositoryResult(
                productionPlanLinhas,
                "multi-location Production Plan output projection population")
                .stream()
                .filter(productionPlanLinha -> clusterEParametrosProjection.isDfuAtiva(productionPlanLinha.getMaterialOutput(), productionPlanLinha.getLocation()))
                .forEach(productionPlanLinha -> {
                    SupplyPlanningProjection supplyPlanningProjection = supplyPlanningMultiplasLocationsProjection.getSupplyPlanningProjectionDeLocation(productionPlanLinha.getLocation());
                    supplyPlanningProjection.addProductionPlanLinhaOutput(productionPlanLinha);
                });
    }

    /**
     * Materializa, em uma unica leitura, os consumos de componentes usados
     * pelas projections de todas as locations de uma fotografia de Supply Plan.
     *
     * <p>O metodo espelha a selecao da variante por location, mas evita que
     * leitores agregados consultem uma lista tecnica por location. Linhas cujo
     * output ou algum componente participa do filtro de materiais sao indexadas
     * somente na projection dona da location da producao.</p>
     */
    public void populaSupplyPlanningMultiplasLocationsProjectionComProductionPlanInput(
            SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection) {

        ClusterEParametrosProjection clusterEParametrosProjection =
                supplyPlanningMultiplasLocationsProjection.getClusterEParametrosProjection();
        MaterialProjection materialProjection =
                supplyPlanningMultiplasLocationsProjection.getMaterialProjection();
        Set<Produto> filteredActiveMaterials = materialProjection.getMateriaisAtivos();

        Collection<ProductionPlanLinha> productionPlanLines = validaProductionPlanLinhaRepositoryResult(
                productionPlanLinhaRepository.customFindByProductionPlanLinhaCompositeKeySupplyPlan(
                        supplyPlanningMultiplasLocationsProjection.getSupplyPlan()),
                "multi-location Production Plan input projection population");

        for (ProductionPlanLinha productionPlanLine : productionPlanLines) {
            Location location = productionPlanLine.getLocation();
            if (!supplyPlanningMultiplasLocationsProjection.getLocationProjection()
                    .getLocationsAtivas().contains(location)
                    || !clusterEParametrosProjection.isDfuAtiva(
                            productionPlanLine.getMaterialOutput(), location)) {
                continue;
            }

            Set<Produto> inputMaterials = productionPlanLine.getMateriaisInput(
                    supplyPlanningMultiplasLocationsProjection.getSupplyNetworkProjection());
            boolean hasSelectedOutput = filteredActiveMaterials.contains(
                    productionPlanLine.getMaterialOutput());
            boolean hasSelectedInput = inputMaterials.stream().anyMatch(filteredActiveMaterials::contains);
            if (!hasSelectedOutput && !hasSelectedInput) {
                continue;
            }

            supplyPlanningMultiplasLocationsProjection
                    .getSupplyPlanningProjectionDeLocation(location)
                    .addProductionPlanLinhaInput(productionPlanLine);
        }
    }

    public void populaSupplyPlanningProjectionComProductionPlanOutput(
            SupplyPlanningProjection supplyPlanningProjection,
            RecursoProdutivo recursoProdutivo, Produto material) {

        Set<Roteiro> roteirosRecursoProdutivoMaterial = supplyPlanningProjection.getSupplyNetworkProjection()
                .getRoteiroSetByRecursoProdutivoEMaterial(recursoProdutivo, material);

        Collection<ProductionPlanLinha> productionPlanLinhas = productionPlanLinhaRepository.findByProductionPlanLinhaCompositeKeySupplyPlanIdAndProductionPlanLinhaCompositeKeyLocationAndMaterialOutputIn(
                supplyPlanningProjection.getSupplyPlan().getId(),
                supplyPlanningProjection.getLocation(),
                supplyPlanningProjection.getMaterialProjection().getMateriaisAtivosEmLocation(supplyPlanningProjection.getLocation()));

        List<ProductionPlanLinha> productionPlanLinhasFiltradas = validaProductionPlanLinhaRepositoryResult(
                productionPlanLinhas,
                "Production Plan output by resource/material projection population")
                .stream()
                .filter(x -> roteirosRecursoProdutivoMaterial.contains(x.getRoteiro()))
                .toList();

        productionPlanLinhasFiltradas.stream().forEach(productionPlanLinha -> {
            supplyPlanningProjection.addProductionPlanLinhaOutput(productionPlanLinha);
        });

    }

    /**
     * Adiciona ao mapaProductionPlanLinhaInput somente as ordens planejadas cujos inputs
     * estão na lista de DFUs do PlanningProjection
     * @param supplyPlanningProjection
     */
    public void populaSupplyPlanningProjectionComProductionPlanInput(SupplyPlanningProjection supplyPlanningProjection) {

        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjection.getSupplyNetworkProjection();
        ClusterEParametrosProjection clusterEParametrosProjection = supplyPlanningProjection.getClusterEParametrosProjection();

        Set<Produto> materiaisFiltradosAtivos = supplyPlanningProjection.getMaterialProjection().getMateriaisAtivos();

        // extrai todas as ordens planejadas da location/supply plan, sem filtros
        List<ProductionPlanLinha> productionPlanLinhas = validaProductionPlanLinhaRepositoryResult(
                productionPlanLinhaRepository.customFindByProductionPlanLinhaCompositeKeySupplyPlanAndProductionPlanLinhaCompositeKeyLocationIncluindoListaTecnicaEMateriaisInput(
                supplyPlanningProjection.getSupplyPlan(),
                supplyPlanningProjection.getLocation()),
                "Production Plan input projection population")
                .stream()
                .filter(productionPlanLinha -> clusterEParametrosProjection.isDfuAtiva(
                        productionPlanLinha.getMaterialOutput(),
                        productionPlanLinha.getLocation()))
                .collect(Collectors.toList());

        Set<Produto> materiaisInputParaOutputsNaListaProjection = productionPlanLinhas.stream()
                .filter(x -> materiaisFiltradosAtivos.contains(x.getMaterialOutput()))
                .flatMap(x -> x.getMateriaisInput(supplyNetworkProjection).stream())
                .collect(Collectors.toSet());

        Set<Produto> materiaisConsideradosComoChaveProductionPlanInputs = new HashSet<>();
        materiaisConsideradosComoChaveProductionPlanInputs.addAll(materiaisInputParaOutputsNaListaProjection);
        materiaisConsideradosComoChaveProductionPlanInputs.addAll(materiaisFiltradosAtivos);

        productionPlanLinhas.stream().forEach(productionPlanLinha -> {
            boolean consideraProductionPlanLinhaComoInput = false;
            for (Produto material : materiaisConsideradosComoChaveProductionPlanInputs) {
                if (productionPlanLinha.getMateriaisInput(supplyNetworkProjection).contains(material)) {
                    consideraProductionPlanLinhaComoInput = true;
                    break;
                }
            }

            if (consideraProductionPlanLinhaComoInput) {
                supplyPlanningProjection.addProductionPlanLinhaInput(productionPlanLinha);
            }
        });
    }
    /**
     * Carrega um InventoryPlan para ultimo segundo do periodo -1 com o valor do estoque
     * de fechamento do período anterior
     * @param supplyPlanningProjection
     * @param estoqueProjectionProduto se usa o estoque cadastrado na abertura do período 0, que no InventoryPlanLinha será salvo no como fechamento do período -1
     * @throws UnitOfMeasureConversionException
     */
    public void populaInventoryPlanComEstoqueInicial(
            SupplyPlanningProjection supplyPlanningProjection,
            // se usa o estoque cadastrado na abertura do período 0, que no InventoryPlanLinha será salvo no como fechamento do período -1
            EstoqueProjectionProduto estoqueProjectionProduto) throws UnitOfMeasureConversionException {

        Calendario calendario = supplyPlanningProjection.getCalendario();

        for (Produto material : estoqueProjectionProduto.getMateriaisComEstoque()) {

            UnidadeMedida unidadeMedidaPadraoSNP = supplyPlanningProjection.getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(
                    material,
                    supplyPlanningProjection.getLocation());

            // inventoryPlanLinha salva estoque de abertura no período -1 (fechamento do período -1)
            InventoryPlanLinha inventoryPlanLinha = new InventoryPlanLinha(new InventoryPlanLinhaCompositeKey(
                    supplyPlanningProjection.getSupplyPlan(),
                    supplyPlanningProjection.getLocation(),
                    material,
                    calendario.getUltimoSegundoPeriodo(-1)));
            inventoryPlanLinha.setUnidadeMedida(unidadeMedidaPadraoSNP);

            /*
             * Community usa apenas o estoque de abertura como ponto inicial do
             * plano. Remessas, pedidos em separacao e outros saldos
             * transacionais pertencem ao fluxo Enterprise e nao entram neste
             * projection.
             */
            double saldoEstoqueInicial = estoqueProjectionProduto.getQuantidadeEstoque(material, unidadeMedidaPadraoSNP);

            inventoryPlanLinha.setQuantidadeEstoqueProjetado(saldoEstoqueInicial, Constantes.TipoPlano.PLANO_IRRESTRITO);
            inventoryPlanLinha.setQuantidadeEstoqueProjetado(saldoEstoqueInicial, Constantes.TipoPlano.PLANO_RESTRITO);

            supplyPlanningProjection.addInventoryPlanLinha(inventoryPlanLinha);

        }

    }

    public void populaInventoryPlanComEstoqueInicial(
            SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection,
            // se usa o estoque cadastrado na abertura do período 0, que no InventoryPlanLinha será salvo no como fechamento do período -1
            EstoqueProjectionLocationProduto estoqueProjectionLocationProduto) throws UnitOfMeasureConversionException {

        Set<Location> locationsComEstoque = estoqueProjectionLocationProduto.getLocationsComEstoque();

        for (Location location : supplyPlanningMultiplasLocationsProjection.getLocationProjection().getLocationSet()) {

            if (!locationsComEstoque.contains(location)) continue;

            SupplyPlanningProjection supplyPlanningProjection = supplyPlanningMultiplasLocationsProjection.getSupplyPlanningProjectionDeLocation(location);
            Calendario calendario = supplyPlanningProjection.getCalendario();

            for (Produto material : estoqueProjectionLocationProduto.getMateriaisComEstoque()) {

                UnidadeMedida unidadeMedidaPadraoSNP = supplyPlanningMultiplasLocationsProjection.getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(
                        material,
                        supplyPlanningProjection.getLocation());

                // inventoryPlanLinha salva estoque de abertura no período -1 (fechamento do período -1)
                InventoryPlanLinha inventoryPlanLinha = new InventoryPlanLinha(new InventoryPlanLinhaCompositeKey(
                        supplyPlanningProjection.getSupplyPlan(),
                        supplyPlanningProjection.getLocation(),
                        material,
                        calendario.getUltimoSegundoPeriodo(-1)));
                inventoryPlanLinha.setUnidadeMedida(unidadeMedidaPadraoSNP);

                /*
                 * Mesma regra da projection por location unica: a edicao
                 * Community inicializa o plano apenas com o snapshot de estoque
                 * cadastrado, sem saldos transacionais Enterprise.
                 */
                double saldoEstoqueInicial = estoqueProjectionLocationProduto.getQuantidadeEstoque(location, material, unidadeMedidaPadraoSNP);

                inventoryPlanLinha.setQuantidadeEstoqueProjetado(saldoEstoqueInicial, Constantes.TipoPlano.PLANO_IRRESTRITO);
                inventoryPlanLinha.setQuantidadeEstoqueProjetado(saldoEstoqueInicial, Constantes.TipoPlano.PLANO_RESTRITO);

                supplyPlanningProjection.addInventoryPlanLinha(inventoryPlanLinha);

            }
        }

    }

    public void populaSupplyPlanningProjectionComDemandaDiretaConsideradaProjection(
            SupplyPlanningProjection supplyPlanningProjection) {

        supplyPlanningProjection.demandaDiretaConsideradaProjection = demandaDiretaConsideradaProjectionFactory.getDemandaDiretaConsideradaProjectionParaLocation(
                supplyPlanningProjection.getSupplyPlan(),
                supplyPlanningProjection.getCalendario(),
                supplyPlanningProjection.getLocation());

    }

    public void populaSupplyPlanningMultiplasLocationsProjectionComDemandaDiretaConsideradaProjection(
            SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection,
            DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjectionCompleto) {

        for (SupplyPlanningProjection supplyPlanningProjection : supplyPlanningMultiplasLocationsProjection.getTodosSupplyPlanningProjections()) {
            supplyPlanningProjection.demandaDiretaConsideradaProjection = demandaDiretaConsideradaProjectionFactory.getDemandaDiretaConsideradaProjectionParaLocation(
                    supplyPlanningProjection.getSupplyPlan(),
                    supplyPlanningProjection.getLocation(),
                    supplyPlanningProjection.getCalendario(),
                    demandaDiretaConsideradaProjectionCompleto);
        }

    }

    public void populaSupplyPlanningMultiplasLocationsProjectionComDisponibilidadeRecursos(
            SupplyPlanningMultiplasLocationsProjection supplyPlanningMultiplasLocationsProjection) {

        /*
         * Disponibilidade detalhada por recurso/turno pertence ao recorte
         * Enterprise de scheduling/AI optimizer. O fluxo Community mantém a
         * capacidade produtiva padrao em BIProjectionCapacidadeProdutiva, que é
         * passada diretamente ao heuristico e ao overlay de otimizador.
         */

    }

}
