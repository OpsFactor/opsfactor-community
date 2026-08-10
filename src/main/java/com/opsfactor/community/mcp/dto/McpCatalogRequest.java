package com.opsfactor.community.mcp.dto;

/**
 * Entrada da tool de navegacao do catalogo.
 *
 * @param path caminho retornado por uma navegacao anterior; vazio lista temas
 * @param search texto opcional para buscar topicos sem conhecer o caminho
 */
public record McpCatalogRequest(String path, String search) {
}
