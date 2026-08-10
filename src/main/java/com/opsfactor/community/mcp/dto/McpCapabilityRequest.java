package com.opsfactor.community.mcp.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Envelope comum para consulta, atualizacao e execucao de uma folha MCP.
 *
 * @param capabilityId id devolvido por uma folha do catalogo
 * @param payload parametros JSON precisos esperados pelo DTO da capacidade
 * @param confirmed confirmacao explicita exigida para escrita ou processamento
 */
public record McpCapabilityRequest(
        String capabilityId,
        JsonNode payload,
        Boolean confirmed) {
}
