package com.opsfactor.community.mcp.catalog;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Catalogo hierarquico e estavel das capacidades MCP abertas no Community.
 *
 * <p>A arvore segue a linguagem da tela Data Operations nos quatro primeiros
 * temas e acrescenta Report, Planning Book, Process Execution e Admin. Cada
 * resposta materializa apenas os filhos imediatos. Assim, o agente percorre
 * theme, group, section e topic sem receber centenas de contratos de uma vez.</p>
 */
@Component
public class CommunityMcpCatalog {

    private final McpCatalogNode root = createCatalog();

    /**
     * Navega por caminho ou pesquisa topicos por texto.
     *
     * @param path caminho de um item previamente retornado
     * @param search texto opcional para pesquisa transversal de folhas
     * @return no solicitado com somente um nivel de filhos
     */
    public McpCatalogNode navigate(String path, String search) {

        if (search != null && !search.isBlank()) {
            return searchTopics(search);
        }

        String normalizedPath = path == null ? "" : path.trim();
        McpCatalogNode selectedNode = normalizedPath.isEmpty()
                ? root
                : findByPath(normalizedPath);

        if (selectedNode == null) {
            throw new IllegalArgumentException("Unknown MCP catalog path: " + normalizedPath);
        }

        return navigationView(selectedNode);

    }

    /**
     * Resolve uma capacidade declarada no catalogo para validacao da borda
     * executora antes de chegar ao registry de handlers.
     */
    public McpCatalogNode getCapability(String capabilityId) {

        if (capabilityId == null || capabilityId.isBlank()) {
            throw new IllegalArgumentException("MCP capability id is required.");
        }

        ArrayDeque<McpCatalogNode> pendingNodes = new ArrayDeque<>();
        pendingNodes.add(root);
        while (!pendingNodes.isEmpty()) {
            McpCatalogNode currentNode = pendingNodes.removeFirst();
            if (Objects.equals(capabilityId, currentNode.capabilityId())) {
                return currentNode;
            }
            pendingNodes.addAll(currentNode.children());
        }

        throw new IllegalArgumentException("Unknown MCP capability: " + capabilityId);

    }

    private McpCatalogNode findByPath(String path) {

        ArrayDeque<McpCatalogNode> pendingNodes = new ArrayDeque<>();
        pendingNodes.add(root);
        while (!pendingNodes.isEmpty()) {
            McpCatalogNode currentNode = pendingNodes.removeFirst();
            if (currentNode.path().equals(path)) {
                return currentNode;
            }
            pendingNodes.addAll(currentNode.children());
        }

        return null;

    }

    private McpCatalogNode searchTopics(String search) {

        String normalizedSearch = search.trim().toLowerCase(Locale.ROOT);
        List<McpCatalogNode> matchingTopics = new ArrayList<>();
        ArrayDeque<McpCatalogNode> pendingNodes = new ArrayDeque<>();
        pendingNodes.add(root);
        while (!pendingNodes.isEmpty()) {
            McpCatalogNode currentNode = pendingNodes.removeFirst();
            String searchableText = String.join(
                    " ",
                    currentNode.id(),
                    currentNode.title(),
                    currentNode.description(),
                    currentNode.capabilityId() == null ? "" : currentNode.capabilityId())
                    .toLowerCase(Locale.ROOT);
            if (currentNode.type() == McpCatalogNodeType.TOPIC
                    && searchableText.contains(normalizedSearch)) {
                matchingTopics.add(summaryView(currentNode));
            }
            pendingNodes.addAll(currentNode.children());
        }

        return new McpCatalogNode(
                "search",
                "Catalog search",
                "Topics matching: " + search,
                McpCatalogNodeType.ROOT,
                "",
                matchingTopics,
                null,
                List.of(),
                null,
                null,
                false);

    }

    private McpCatalogNode navigationView(McpCatalogNode node) {

        return new McpCatalogNode(
                node.id(),
                node.title(),
                node.description(),
                node.type(),
                node.path(),
                node.children().stream().map(this::summaryView).toList(),
                node.capabilityId(),
                node.operations(),
                node.inputTypeHint(),
                node.outputTypeHint(),
                node.confirmationRequired());

    }

    private McpCatalogNode summaryView(McpCatalogNode node) {

        return new McpCatalogNode(
                node.id(),
                node.title(),
                node.description(),
                node.type(),
                node.path(),
                List.of(),
                node.capabilityId(),
                node.operations(),
                node.inputTypeHint(),
                node.outputTypeHint(),
                node.confirmationRequired());

    }

    private McpCatalogNode createCatalog() {

        McpCatalogNode masterData = branch("master-data", "Master Data",
                "Core planning master data.", McpCatalogNodeType.THEME, "",
                branch("supply-network", "Supply Network", "Network topology and transportation.", McpCatalogNodeType.GROUP, "master-data",
                        branch("structure", "Structure", "Versions and lanes.", McpCatalogNodeType.SECTION, "master-data/supply-network",
                                integrationLeaf("supply-network-version", "Supply Network Version", "data.master-data.supply-network-version", "VersaoMalhaIntegrationDataDto"),
                                integrationLeaf("transportation-lane", "Transportation Lane", "data.master-data.transportation-lane", "LinhaTransporteIntegrationDataDto"),
                                integrationLeaf("transportation-lane-material", "Transportation Lane Material", "data.master-data.transportation-lane-material", "LinhaTransporteProdutoIntegrationDataDto"))),
                branch("material-location", "Material and Location", "Material, location and measurement records.", McpCatalogNodeType.GROUP, "master-data",
                        branch("records", "Records", "Primary master records.", McpCatalogNodeType.SECTION, "master-data/material-location",
                                integrationLeaf("material", "Material", "data.master-data.material", "ProdutoIntegrationDataDto"),
                                integrationLeaf("location", "Location", "data.master-data.location", "LocationIntegrationDataDto"),
                                leaf("unit-of-measure", "Unit of Measure", "Measurement units available to planning.", "data.master-data.unit-of-measure", List.of(McpCapabilityOperation.QUERY, McpCapabilityOperation.UPDATE), "List<UnidadeMedidaDataUploadDTO>", "List<UnidadeMedidaDataUploadDTO>", true),
                                integrationLeaf("unit-conversion", "Unit Conversion", "data.master-data.unit-conversion", "ConversaoUnidadeIntegrationDataDto"),
                                integrationLeaf("unit-conversion-material", "Unit Conversion by Material", "data.master-data.unit-conversion-material", "ConversaoUnidadeProdutoIntegrationDataDto"))),
                branch("production", "Production", "Basic production structure for heuristic planning.", McpCatalogNodeType.GROUP, "master-data",
                        branch("structure", "Structure", "Resources, routings and bills of material.", McpCatalogNodeType.SECTION, "master-data/production",
                                integrationLeaf("production-resource", "Production Resource", "data.master-data.production-resource", "RecursoProdutivoIntegrationDataDto"),
                                integrationLeaf("production-resource-availability", "Production Resource Availability", "data.master-data.production-resource-availability", "DisponibilidadeRecursoProdutivoIntegrationDataDto"),
                                integrationLeaf("production-routing", "Production Routing", "data.master-data.production-routing", "RoteiroIntegrationDataDto"),
                                integrationLeaf("bill-of-material", "Bill of Material", "data.master-data.bill-of-material", "ListaTecnicaIntegrationDataDto"),
                                integrationLeaf("bill-of-material-component", "Bill of Material Component", "data.master-data.bill-of-material-component", "ListaTecnicaComponenteIntegrationDataDto"),
                                integrationLeaf("simple-production-version", "Simple Production Version", "data.master-data.simple-production-version", "VersaoProducaoSimplesIntegrationDataDto"))),
                branch("inventory", "Inventory", "Inventory policy master data.", McpCatalogNodeType.GROUP, "master-data",
                        branch("policy", "Policy", "Policy headers and material/location details.", McpCatalogNodeType.SECTION, "master-data/inventory",
                                integrationLeaf("inventory-policy", "Inventory Policy", "data.master-data.inventory-policy", "PoliticaEstoquesIntegrationDataDto"),
                                integrationLeaf("inventory-policy-detail", "Inventory Policy Detail", "data.master-data.inventory-policy-detail", "PoliticaEstoquesMaterialLocationIntegrationDataDto"))));

        McpCatalogNode transactionalData = branch("transactional-data", "Transactional Data",
                "Operational facts consumed by planning.", McpCatalogNodeType.THEME, "",
                branch("inventory", "Inventory", "Current inventory position.", McpCatalogNodeType.GROUP, "transactional-data",
                        branch("position", "Position", "Initial or current stock.", McpCatalogNodeType.SECTION, "transactional-data/inventory",
                                integrationLeaf("stock", "Stock", "data.transactional-data.stock", "EstoqueIntegrationDataDto"))),
                branch("demand", "Demand", "Observed demand history.", McpCatalogNodeType.GROUP, "transactional-data",
                        branch("history", "History", "Sales or sell-out history.", McpCatalogNodeType.SECTION, "transactional-data/demand",
                                integrationLeaf("sellout", "Sell-out", "data.transactional-data.sellout", "SelloutIntegrationDataDto"))));

        McpCatalogNode configuration = branch("configuration", "Configuration",
                "Planning parameters, including settings outside Data Operations.", McpCatalogNodeType.THEME, "",
                branch("material-location", "Material/Location", "Parameters and attributes by planning object.", McpCatalogNodeType.GROUP, "configuration",
                        branch("parameters", "Parameters and Attributes", "Configuration at material/location level.", McpCatalogNodeType.SECTION, "configuration/material-location",
                                integrationLeaf("material-location-parameters", "Material/Location Parameters", "data.configuration.material-location-parameters", "ParametrosMaterialLocationIntegrationDataDto"))),
                branch("demand-planning", "Demand Planning", "Forecast execution and cluster-level settings.", McpCatalogNodeType.GROUP, "configuration",
                        branch("forecast", "Forecast", "Profiles and configuration by cluster.", McpCatalogNodeType.SECTION, "configuration/demand-planning",
                                leaf("execution-profile", "Demand Planning Execution Profile", "Profiles allowed by Community Demand Planning.", "configuration.demand.execution-profile", List.of(McpCapabilityOperation.QUERY, McpCapabilityOperation.UPDATE), "PerfilExecucaoDemandPlanDTO", "List<PerfilExecucaoDemandPlanDTO>", true),
                                leaf("cluster-level", "Demand Planning Cluster Configuration", "Forecast parameters by execution profile, location cluster and material cluster.", "configuration.demand.cluster-level", List.of(McpCapabilityOperation.QUERY, McpCapabilityOperation.UPDATE), "DemandPlanningClusterLevelConfigurationDTO or key object", "DemandPlanningClusterLevelConfigurationDTO", true))),
                branch("supply-planning", "Supply Planning", "Supply execution configuration.", McpCatalogNodeType.GROUP, "configuration",
                        branch("execution", "Execution", "Profiles used to run a Supply Plan.", McpCatalogNodeType.SECTION, "configuration/supply-planning",
                                leaf("execution-profile", "Supply Planning Execution Profile", "Profiles allowed by Community Supply Planning.", "configuration.supply.execution-profile", List.of(McpCapabilityOperation.QUERY, McpCapabilityOperation.UPDATE), "PerfilExecucaoSupplyPlanDTO", "Set<PerfilExecucaoSupplyPlanDTO>", true))));

        McpCatalogNode planningData = branch("planning-data", "Planning Data",
                "Persisted outputs produced by planning processes.", McpCatalogNodeType.THEME, "",
                branch("supply-planning", "Supply Planning", "Supply Plan output datasets.", McpCatalogNodeType.GROUP, "planning-data",
                        branch("outputs", "Outputs", "Read-only physical planning results.", McpCatalogNodeType.SECTION, "planning-data/supply-planning",
                                leaf("inventory-plan", "Inventory Plan", "Inventory projection for a Supply Plan.", "data.planning.inventory-plan", List.of(McpCapabilityOperation.QUERY), "{supplyPlanId}", "List<InventoryPlanIntegrationDataDto>", false),
                                leaf("fulfilled-demand", "Fulfilled Demand", "Demand fulfilled by a Supply Plan and unit of measure.", "data.planning.fulfilled-demand", List.of(McpCapabilityOperation.QUERY), "{supplyPlanId, unitOfMeasureId}", "List<FulfilledDemandIntegrationDataDto>", false))));

        McpCatalogNode report = branch("report", "Report", "Community analytical reports.", McpCatalogNodeType.THEME, "",
                branch("supply-planning", "Supply Planning", "Supply analysis outputs.", McpCatalogNodeType.GROUP, "report",
                        branch("flow", "Flow", "Physical network-flow reports.", McpCatalogNodeType.SECTION, "report/supply-planning",
                                leaf("material-flows", "Material Flows", "Physical transfer matrix for a Supply Plan.", "report.supply.material-flows", List.of(McpCapabilityOperation.QUERY), "{supplyPlanId}", "CommunityMaterialFlowsDTO", false))));

        McpCatalogNode planningBook = branch("planning-book", "Planning Book", "Interactive Community planning books.", McpCatalogNodeType.THEME, "",
                branch("demand-planning", "Demand Planning", "Demand planning grid.", McpCatalogNodeType.GROUP, "planning-book",
                        branch("book", "Book", "Read and update material/location values.", McpCatalogNodeType.SECTION, "planning-book/demand-planning",
                                leaf("demand", "Demand Planning Book", "Loads a configured view or applies selected-cell adjustments.", "planning-book.demand", List.of(McpCapabilityOperation.QUERY, McpCapabilityOperation.UPDATE), "ConfiguredViewSelectionDTO or List<SelectedPlanningBookCellDTO>", "PlanningBookDTO", true))),
                branch("supply-planning", "Supply Planning", "Supply planning grid.", McpCatalogNodeType.GROUP, "planning-book",
                        branch("book", "Book", "Read and update material/location values.", McpCatalogNodeType.SECTION, "planning-book/supply-planning",
                                leaf("supply", "Supply Planning Book", "Loads a configured view or applies selected-cell adjustments.", "planning-book.supply", List.of(McpCapabilityOperation.QUERY, McpCapabilityOperation.UPDATE), "ConfiguredViewSelectionDTO or List<SelectedPlanningBookCellDTO>", "PlanningBookDTO", true))));

        McpCatalogNode processExecution = branch("process-execution", "Process Execution", "Synchronous Community planning processes.", McpCatalogNodeType.THEME, "",
                branch("demand-planning", "Demand Planning", "Forecast generation process.", McpCatalogNodeType.GROUP, "process-execution",
                        branch("planning", "Planning", "Run planning from a version payload.", McpCatalogNodeType.SECTION, "process-execution/demand-planning",
                                leaf("execute", "Execute Demand Planning", "Runs Demand Planning synchronously.", "process.demand.execute", List.of(McpCapabilityOperation.RUN), "VersaoDemandPlanDTO", "ResponseDTO", true))),
                branch("supply-planning", "Supply Planning", "Heuristic supply process.", McpCatalogNodeType.GROUP, "process-execution",
                        branch("planning", "Planning", "Run planning from a version payload.", McpCatalogNodeType.SECTION, "process-execution/supply-planning",
                                leaf("execute", "Execute Supply Planning", "Runs heuristic Supply Planning synchronously.", "process.supply.execute", List.of(McpCapabilityOperation.RUN), "VersaoSupplyPlanDTO", "ResponseDTO", true))));

        McpCatalogNode admin = branch("admin", "Admin", "Community administration limited to user views.", McpCatalogNodeType.THEME, "",
                branch("user-views", "User Views", "Planning Book views owned by Community users.", McpCatalogNodeType.GROUP, "admin",
                        branch("planning-book", "Planning Book", "Demand and Supply view definitions.", McpCatalogNodeType.SECTION, "admin/user-views",
                                leaf("demand", "Demand Planning User Views", "Lists or saves Demand Planning Book views for the authenticated Community user.", "admin.user-view.demand", List.of(McpCapabilityOperation.QUERY, McpCapabilityOperation.UPDATE), "ConfiguredViewDTO", "List<ConfiguredViewDTO>", true),
                                leaf("supply", "Supply Planning User Views", "Lists or saves Supply Planning Book views for the authenticated Community user.", "admin.user-view.supply", List.of(McpCapabilityOperation.QUERY, McpCapabilityOperation.UPDATE), "ConfiguredViewDTO", "List<ConfiguredViewDTO>", true))));

        return new McpCatalogNode(
                "root",
                "OpsFactor Community",
                "Navigate one level at a time and execute only topic capability ids.",
                McpCatalogNodeType.ROOT,
                "",
                List.of(masterData, transactionalData, configuration, planningData, report, planningBook, processExecution, admin),
                null,
                List.of(),
                null,
                null,
                false);

    }

    private McpCatalogNode integrationLeaf(
            String id,
            String title,
            String capabilityId,
            String dataDtoType) {

        return leaf(
                id,
                title,
                "Queries or imports the canonical Community integration DTO.",
                capabilityId,
                List.of(McpCapabilityOperation.QUERY, McpCapabilityOperation.UPDATE),
                "IntegrationDto<" + dataDtoType + "> or JSON array",
                "List<" + dataDtoType + ">",
                true);

    }

    private McpCatalogNode leaf(
            String id,
            String title,
            String description,
            String capabilityId,
            List<McpCapabilityOperation> operations,
            String inputTypeHint,
            String outputTypeHint,
            boolean confirmationRequired) {

        return new McpCatalogNode(
                id,
                title,
                description,
                McpCatalogNodeType.TOPIC,
                "",
                List.of(),
                capabilityId,
                operations,
                inputTypeHint,
                outputTypeHint,
                confirmationRequired);

    }

    private McpCatalogNode branch(
            String id,
            String title,
            String description,
            McpCatalogNodeType type,
            String parentPath,
            McpCatalogNode... children) {

        String path = parentPath == null || parentPath.isBlank()
                ? id
                : parentPath + "/" + id;
        List<McpCatalogNode> childrenWithPaths = Arrays.stream(children)
                .map(child -> withPath(child, path + "/" + child.id()))
                .toList();

        return new McpCatalogNode(
                id,
                title,
                description,
                type,
                path,
                childrenWithPaths,
                null,
                List.of(),
                null,
                null,
                false);

    }

    private McpCatalogNode withPath(McpCatalogNode node, String path) {

        List<McpCatalogNode> childrenWithPaths = node.children().stream()
                .map(child -> withPath(child, path + "/" + child.id()))
                .toList();

        return new McpCatalogNode(
                node.id(),
                node.title(),
                node.description(),
                node.type(),
                path,
                childrenWithPaths,
                node.capabilityId(),
                node.operations(),
                node.inputTypeHint(),
                node.outputTypeHint(),
                node.confirmationRequired());

    }

}
