package com.opsfactor.community.mcp;

import com.opsfactor.community.mcp.catalog.CommunityMcpCatalog;
import com.opsfactor.community.mcp.catalog.McpCapabilityOperation;
import com.opsfactor.community.mcp.catalog.McpCatalogNode;
import com.opsfactor.community.mcp.dto.McpCapabilityRequest;
import com.opsfactor.community.mcp.dto.McpCatalogRequest;
import com.opsfactor.community.mcp.dto.McpExecutionResponse;
import com.opsfactor.community.mcp.execution.CommunityMcpCapabilityRegistry;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Superficie MCP pequena e estavel do OpsFactor Community.
 *
 * <p>O protocolo sempre publica estas quatro tools. Theme, group, section e
 * topic sao dados devolvidos por {@link #catalog(McpCatalogRequest)}, nao novas
 * tools registradas dinamicamente. Uma folha fornece o capability id que deve
 * ser enviado a query, update ou run.</p>
 */
@Component
public class CommunityMcpTools {

    private final CommunityMcpCatalog communityMcpCatalog;
    private final CommunityMcpCapabilityRegistry communityMcpCapabilityRegistry;

    /**
     * Conecta a borda anotada ao catalogo imutavel e ao registry funcional de
     * capacidades permitidas no Community.
     */
    @Autowired
    public CommunityMcpTools(
            CommunityMcpCatalog communityMcpCatalog,
            CommunityMcpCapabilityRegistry communityMcpCapabilityRegistry) {

        this.communityMcpCatalog = communityMcpCatalog;
        this.communityMcpCapabilityRegistry = communityMcpCapabilityRegistry;

    }

    /**
     * Lista o primeiro nivel, abre um caminho retornado anteriormente ou busca
     * topicos. Somente os filhos imediatos sao materializados.
     */
    @McpTool(
            name = "opsfactor_catalog",
            title = "Navigate OpsFactor Community capabilities",
            description = "Navigate theme/group/section/topic one level at a time. Use an empty path for root themes, reuse a returned path for the next level, or provide search text.")
    public McpCatalogNode catalog(
            @McpToolParam(
                    description = "Navigation request with optional path and optional topic search text.")
            McpCatalogRequest request) {

        McpCatalogRequest effectiveRequest = request == null
                ? new McpCatalogRequest(null, null)
                : request;
        return communityMcpCatalog.navigate(effectiveRequest.path(), effectiveRequest.search());

    }

    /**
     * Consulta uma folha read-only e devolve seu DTO como JSON estruturado.
     */
    @McpTool(
            name = "opsfactor_query",
            title = "Query an OpsFactor Community capability",
            description = "Execute QUERY on a topic capability id returned by opsfactor_catalog. The payload must follow the topic inputTypeHint. Returns structured JSON DTO data.")
    public McpExecutionResponse query(
            @McpToolParam(
                    description = "Capability id and precise JSON payload returned or described by the selected catalog topic.")
            McpCapabilityRequest request) {

        validateRequest(request, McpCapabilityOperation.QUERY);
        return new McpExecutionResponse(
                request.capabilityId(),
                McpCapabilityOperation.QUERY.name(),
                communityMcpCapabilityRegistry.query(request.capabilityId(), request.payload()));

    }

    /**
     * Atualiza dados ou configuracoes somente apos confirmacao explicita.
     */
    @McpTool(
            name = "opsfactor_update",
            title = "Update an OpsFactor Community capability",
            description = "Execute UPDATE on a topic capability id returned by opsfactor_catalog. Mutations require confirmed=true and use the same DTO contracts and services as the REST API.")
    public McpExecutionResponse update(
            @McpToolParam(
                    description = "Capability id, exact JSON DTO payload, and confirmed=true after user confirmation.")
            McpCapabilityRequest request) {

        validateRequest(request, McpCapabilityOperation.UPDATE);
        requireConfirmation(request);
        return new McpExecutionResponse(
                request.capabilityId(),
                McpCapabilityOperation.UPDATE.name(),
                communityMcpCapabilityRegistry.update(request.capabilityId(), request.payload()));

    }

    /**
     * Dispara um processo funcional Community somente apos confirmacao.
     */
    @McpTool(
            name = "opsfactor_run",
            title = "Run an OpsFactor Community process",
            description = "Execute RUN on a process topic capability id returned by opsfactor_catalog. Processes require confirmed=true and run through the Community synchronous task service.")
    public McpExecutionResponse run(
            @McpToolParam(
                    description = "Process capability id, exact execution DTO payload, and confirmed=true after user confirmation.")
            McpCapabilityRequest request) {

        validateRequest(request, McpCapabilityOperation.RUN);
        requireConfirmation(request);
        return new McpExecutionResponse(
                request.capabilityId(),
                McpCapabilityOperation.RUN.name(),
                communityMcpCapabilityRegistry.run(request.capabilityId(), request.payload()));

    }

    private void validateRequest(
            McpCapabilityRequest request,
            McpCapabilityOperation requestedOperation) {

        if (request == null) {
            throw new IllegalArgumentException("MCP capability request is required.");
        }

        McpCatalogNode capabilityNode = communityMcpCatalog.getCapability(request.capabilityId());
        if (!capabilityNode.operations().contains(requestedOperation)) {
            throw new IllegalArgumentException(
                    "MCP capability "
                            + request.capabilityId()
                            + " does not support "
                            + requestedOperation.name());
        }

    }

    private void requireConfirmation(McpCapabilityRequest request) {

        if (!Boolean.TRUE.equals(request.confirmed())) {
            throw new IllegalArgumentException(
                    "Explicit confirmed=true is required for MCP mutations and process execution.");
        }

    }

}
