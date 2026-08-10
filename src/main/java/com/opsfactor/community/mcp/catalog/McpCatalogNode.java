package com.opsfactor.community.mcp.catalog;

import java.util.List;

/**
 * Descritor imutavel de um nivel do catalogo MCP.
 *
 * @param id identificador estavel e unico dentro do catalogo
 * @param title rotulo curto apresentado ao agente
 * @param description explicacao funcional usada na escolha do caminho
 * @param type nivel estrutural do item
 * @param path caminho completo que pode ser enviado novamente para navegacao
 * @param children filhos imediatos; descendentes nao sao materializados nesta resposta
 * @param capabilityId identificador executavel quando o item e um topico
 * @param operations executores MCP aceitos pela folha
 * @param inputTypeHint DTO ou formato de entrada esperado pela capacidade
 * @param outputTypeHint DTO ou formato do resultado JSON
 * @param confirmationRequired indica que a operacao mutavel exige confirmacao explicita
 */
public record McpCatalogNode(
        String id,
        String title,
        String description,
        McpCatalogNodeType type,
        String path,
        List<McpCatalogNode> children,
        String capabilityId,
        List<McpCapabilityOperation> operations,
        String inputTypeHint,
        String outputTypeHint,
        boolean confirmationRequired) {

    /**
     * Garante colecoes imutaveis para que uma navegacao nao altere o catalogo
     * compartilhado entre sessoes MCP.
     */
    public McpCatalogNode {

        children = children == null ? List.of() : List.copyOf(children);
        operations = operations == null ? List.of() : List.copyOf(operations);

    }

}
