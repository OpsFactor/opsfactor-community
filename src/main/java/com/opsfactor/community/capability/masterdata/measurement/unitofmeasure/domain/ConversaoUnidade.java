package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain;

import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import jakarta.annotation.Nullable;
import lombok.*;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;

/**
 * Conversões padrão. Podem ser sobrescritas por conversões 
 * produto-linha transporte ou produto-location
 */
@Getter
@Setter
@EqualsAndHashCode(of = "conversaoUnidadeCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class ConversaoUnidade {

    @EmbeddedId
    @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
    private ConversaoUnidadeCompositeKey conversaoUnidadeCompositeKey;
    
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ConversaoUnidadeCompositeKey implements Serializable {

        @ManyToOne
        @NonNull
        private UnidadeMedida unidadeMedidaOrigem;
        
        @ManyToOne
        @NonNull
        private UnidadeMedida unidadeMedidaDestino;

    }

    @Deprecated
    private Double quantidadeUnidadeDestinoPorUnidadeOrigem;

    private Double quantidadeUnidadeOrigem;
    private Double quantidadeUnidadeDestino;

    public UnidadeMedida getUnidadeMedidaOrigem() {
        return conversaoUnidadeCompositeKey.getUnidadeMedidaOrigem();
    }

    public UnidadeMedida getUnidadeMedidaDestino() {
        return conversaoUnidadeCompositeKey.getUnidadeMedidaDestino();
    }

    private Double getQuantidadeUnidadeOrigem() {
        if (quantidadeUnidadeOrigem != null) return quantidadeUnidadeOrigem;
        if (quantidadeUnidadeDestinoPorUnidadeOrigem != null) return 1.0;
        return null;
    }
    public Double getQuantidadeUnidadeOrigemCadastrado() {
        if (quantidadeUnidadeOrigem == null) {
            if (quantidadeUnidadeDestinoPorUnidadeOrigem == null) {
                return null;
            } else {
                return 1.0;
            }
        } else {
            return quantidadeUnidadeOrigem;
        }
    }

    private Double getQuantidadeUnidadeDestino() {
        if (quantidadeUnidadeDestino != null) return quantidadeUnidadeDestino;
        if (quantidadeUnidadeDestinoPorUnidadeOrigem != null) return quantidadeUnidadeDestinoPorUnidadeOrigem;
        return null;
    }
    public Double getQuantidadeUnidadeDestinoCadastrado() {
        if (quantidadeUnidadeDestino == null) {
            if (quantidadeUnidadeDestinoPorUnidadeOrigem == null) {
                return null;
            } else {
                return quantidadeUnidadeDestinoPorUnidadeOrigem;
            }
        } else {
            return quantidadeUnidadeDestino;
        }
    }

    /**
     * Retorna a razao da conversao global entre duas unidades de medida.
     *
     * <p>Conversoes globais sao carregadas diretamente na projection de UOM.
     * Linha incompleta e erro de cadastro e deve falhar com diagnostico
     * funcional, nao com NPE por divisao de quantidade ausente.</p>
     */
    public Double getQuantidadeUnidadeDestinoPorUnidadeOrigem() {

        Double quantidadeUnidadeDestinoPorUnidadeOrigemResolvida =
                getQuantidadeUnidadeDestinoPorUnidadeOrigemOuNull();

        if (quantidadeUnidadeDestinoPorUnidadeOrigemResolvida == null) {
            throw new UnitOfMeasureConversionException(
                    "Global UOM conversion has no quantity ratio from "
                            + getUnidadeMedidaOrigem().getId()
                            + " to "
                            + getUnidadeMedidaDestino().getId());
        }

        return quantidadeUnidadeDestinoPorUnidadeOrigemResolvida;

    }

    /**
     * Resolve a razao global preservando `null` como sinal interno de cadastro
     * incompleto. O metodo publico transforma esse estado em excecao funcional.
     */
    @Nullable
    private Double getQuantidadeUnidadeDestinoPorUnidadeOrigemOuNull() {

        if (getLegacyRatioState()
                == UnitOfMeasureConversionLegacyRatioState.CONFLICTING_LEGACY_AND_CANONICAL_RATIOS) {
            throw new UnitOfMeasureConversionException(
                    "Global UOM conversion has conflicting deprecated and canonical quantity ratios from "
                            + getUnidadeMedidaOrigem().getId()
                            + " to "
                            + getUnidadeMedidaDestino().getId());
        }

        if (quantidadeUnidadeDestinoPorUnidadeOrigem != null) {
            return quantidadeUnidadeDestinoPorUnidadeOrigem;
        }

        Double quantidadeUnidadeOrigemResolvida = getQuantidadeUnidadeOrigem();
        Double quantidadeUnidadeDestinoResolvida = getQuantidadeUnidadeDestino();

        if (quantidadeUnidadeOrigemResolvida == null || quantidadeUnidadeDestinoResolvida == null) {
            return null;
        }

        return quantidadeUnidadeDestinoResolvida / quantidadeUnidadeOrigemResolvida;

    }

    /**
     * Classifica a coexistencia entre a coluna direta depreciada e o par de
     * quantidades canonico sem alterar a entidade persistida.
     *
     * <p>O estado e consultado pelo preflight de cutover. O getter de calculo
     * tambem o consulta para interromper imediatamente quando ambos os formatos
     * estiverem preenchidos com razoes diferentes.</p>
     */
    public UnitOfMeasureConversionLegacyRatioState getLegacyRatioState() {

        if (quantidadeUnidadeDestinoPorUnidadeOrigem == null) {
            return UnitOfMeasureConversionLegacyRatioState.CANONICAL_ONLY;
        }
        if (quantidadeUnidadeOrigem == null && quantidadeUnidadeDestino == null) {
            return UnitOfMeasureConversionLegacyRatioState.LEGACY_ONLY;
        }
        if (quantidadeUnidadeOrigem == null || quantidadeUnidadeDestino == null) {
            return UnitOfMeasureConversionLegacyRatioState.INCOMPLETE_CANONICAL_QUANTITIES_WITH_LEGACY_RATIO;
        }

        double canonicalRatio = quantidadeUnidadeDestino / quantidadeUnidadeOrigem;
        if (!Double.isFinite(quantidadeUnidadeDestinoPorUnidadeOrigem)
                || !Double.isFinite(canonicalRatio)
                || Double.compare(quantidadeUnidadeDestinoPorUnidadeOrigem, canonicalRatio) != 0) {
            return UnitOfMeasureConversionLegacyRatioState.CONFLICTING_LEGACY_AND_CANONICAL_RATIOS;
        }

        return UnitOfMeasureConversionLegacyRatioState.REDUNDANT_LEGACY_RATIO;

    }

    public Double getQuantidadeUnidadeDestinoPorUnidadeOrigemCadastrado() {
        return quantidadeUnidadeDestinoPorUnidadeOrigem;
    }

}
