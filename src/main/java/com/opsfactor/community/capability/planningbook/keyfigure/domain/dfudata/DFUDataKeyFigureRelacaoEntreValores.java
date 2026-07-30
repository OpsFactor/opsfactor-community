package com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Dado de uma KF derivada cuja agregacao depende de numerador e denominador.
 * O valor exibido e calculado pelo DTO, nunca persistido nesta estrutura.
 */
@Getter
@NoArgsConstructor
@SuperBuilder
public class DFUDataKeyFigureRelacaoEntreValores extends DFUDataKeyFigureAbstract {

    private double numeratorValue;
    private double denominatorValue;

}
