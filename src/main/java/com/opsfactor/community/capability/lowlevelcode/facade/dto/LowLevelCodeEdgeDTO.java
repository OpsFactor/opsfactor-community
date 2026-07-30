package com.opsfactor.community.capability.lowlevelcode.facade.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * Edge direcionada do grafo tecnico de low level code.
 *
 * <p>Edges ligam materiais, locations, roteiros, recursos produtivos e listas
 * tecnicas. Labels podem trazer informacoes operacionais como lead time ou
 * consumo de componente, mas nao custos, fretes ou variaveis de otimizador.</p>
 */
@Builder
@EqualsAndHashCode(of = {"from", "to"})
public class LowLevelCodeEdgeDTO {

    /** Id do node de origem. */
    public String from;

    /** Id do node de destino. */
    public String to;

    /** Texto opcional exibido na aresta. */
    public String label;

}
