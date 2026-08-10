package com.opsfactor.community.mcp.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resposta JSON uniforme das tools executoras Community.
 *
 * @param capabilityId capacidade efetivamente chamada
 * @param operation operacao executada
 * @param data DTO, colecao de DTOs ou resultado funcional serializado
 */
public record McpExecutionResponse(
        String capabilityId,
        String operation,
        JsonNode data) {
}
