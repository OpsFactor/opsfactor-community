package com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Componentes de cobertura de estoque de uma DFU em um período.
 *
 * <p>O valor exibido não é armazenado aqui: para cada período inicial, o DTO
 * soma estoque e fluxo líquido futuro dos filhos e só então calcula o ponto
 * de esgotamento. Isso preserva a semântica quando a linha pai representa
 * mais de um material na location selecionada.</p>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@SuperBuilder
public class DFUDataKeyFigureCoberturaEstoque extends DFUDataKeyFigureAbstract {

    /** Estoque projetado no fechamento do período de referência. */
    private double quantidadeEstoqueProjetado;

    /** Entradas menos demanda a consumir no período de referência. */
    private double saldoEntradasSaidas;

}
