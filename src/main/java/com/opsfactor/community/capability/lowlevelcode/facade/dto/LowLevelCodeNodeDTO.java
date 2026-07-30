package com.opsfactor.community.capability.lowlevelcode.facade.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Node do grafo tecnico de low level code.
 *
 * <p>O tipo e uma categoria visual simples, como Location, Material,
 * Production Routing Operation, Production Resource ou Bill of Materials. A
 * resposta Community nao transporta metadados de custo, capacidade otimizada
 * ou line scheduling.</p>
 */
@Data
@Builder
@EqualsAndHashCode(of = "id")
public class LowLevelCodeNodeDTO {

    /** Categoria visual do node. */
    public String tipo;

    /** Id tecnico unico dentro do grafo. */
    public String id;

    /** Rotulo exibido no front. */
    public String label;

    /** Nivel calculado a partir das edges do grafo. */
    public Integer level;

}
