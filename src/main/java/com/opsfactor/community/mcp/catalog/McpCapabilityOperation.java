package com.opsfactor.community.mcp.catalog;

/**
 * Operacoes estaveis que uma folha do catalogo MCP pode delegar.
 *
 * <p>Os valores desta enum nao se transformam em novas tools MCP. Eles apontam
 * para uma das tools executoras fixas publicadas por
 * {@code CommunityMcpTools}, mantendo pequeno o catalogo entregue ao agente.</p>
 */
public enum McpCapabilityOperation {

    QUERY,
    UPDATE,
    RUN

}
