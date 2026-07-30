package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain;

/**
 * Estado de transicao entre o fator legado direto e o par de quantidades
 * canonico de uma conversao de unidade de medida.
 *
 * <p>O fator legado representa {@code target / origin} em uma unica coluna.
 * O par canonico preserva os dois valores cadastrados. Este enum e usado pelo
 * preflight somente-leitura e pelos getters de calculo para que uma linha com
 * valores divergentes nao tenha sua semantica escolhida silenciosamente.</p>
 */
public enum UnitOfMeasureConversionLegacyRatioState {

    /** A linha nao possui mais o fator direto depreciado. */
    CANONICAL_ONLY,

    /** A linha ainda depende exclusivamente do fator direto depreciado. */
    LEGACY_ONLY,

    /** O fator direto e o par canonico completo representam a mesma razao. */
    REDUNDANT_LEGACY_RATIO,

    /** Ha somente uma das quantidades canonicas junto ao fator legado. */
    INCOMPLETE_CANONICAL_QUANTITIES_WITH_LEGACY_RATIO,

    /** O fator direto e o par canonico completo representam razoes distintas. */
    CONFLICTING_LEGACY_AND_CANONICAL_RATIOS

}
