package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import jakarta.annotation.Nullable;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;

/**
 * Conversão por produto : sobrepõem a configuração padrão
 */
@Getter
@Setter
@EqualsAndHashCode(of = "conversaoUnidadeProdutoCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class ConversaoUnidadeProduto {

    @EmbeddedId
    @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
    private ConversaoUnidadeProdutoCompositeKey conversaoUnidadeProdutoCompositeKey;
    
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ConversaoUnidadeProdutoCompositeKey implements Serializable {

        @ManyToOne(fetch = FetchType.LAZY)
        @NonNull
        private Produto produto;

        @ManyToOne(fetch = FetchType.LAZY)
        @NonNull
        private UnidadeMedida unidadeMedidaOrigem;

        @ManyToOne(fetch = FetchType.LAZY)
        @NonNull
        private UnidadeMedida unidadeMedidaDestino;

    }

    @Deprecated
    private Double quantidadeUnidadeDestinoPorUnidadeOrigem;

    private Double quantidadeUnidadeOrigem;
    private Double quantidadeUnidadeDestino;
    
    public Produto getProduto() {
        return conversaoUnidadeProdutoCompositeKey.getProduto();
    }
    
    public UnidadeMedida getUnidadeMedidaOrigem() {
        return conversaoUnidadeProdutoCompositeKey.getUnidadeMedidaOrigem();
    }
    
    public UnidadeMedida getUnidadeMedidaDestino() {
        return conversaoUnidadeProdutoCompositeKey.getUnidadeMedidaDestino();
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
     * Retorna a conversao especifica por material sem aplicar fallback global.
     *
     * <p>A projection de UOM usa este metodo ao materializar conversoes por
     * material. Se a linha especifica existir mas nao trouxer razao nem par
     * origem/destino, o cadastro esta incompleto e deve falhar com erro
     * funcional explicito, nao com NPE durante a divisao.</p>
     */
    public Double getQuantidadeUnidadeDestinoPorUnidadeOrigem() {

        Double quantidadeUnidadeDestinoPorUnidadeOrigemResolvida =
                getQuantidadeUnidadeDestinoPorUnidadeOrigemPropriaOuNull();

        if (quantidadeUnidadeDestinoPorUnidadeOrigemResolvida == null) {
            throw new UnitOfMeasureConversionException(
                    "Material-level UOM conversion has no quantity ratio from "
                            + getUnidadeMedidaOrigem().getId()
                            + " to "
                            + getUnidadeMedidaDestino().getId()
                            + " for material "
                            + getProduto().getId());
        }

        return quantidadeUnidadeDestinoPorUnidadeOrigemResolvida;

    }

    public Double getQuantidadeUnidadeDestinoPorUnidadeOrigem(Collection<ConversaoUnidade> listaConversoesPadrao) {

        Double quantidadeUnidadeDestinoPorUnidadeOrigemResolvida =
                getQuantidadeUnidadeDestinoPorUnidadeOrigemPropriaOuNull();

        if (quantidadeUnidadeDestinoPorUnidadeOrigemResolvida == null) {
            if (listaConversoesPadrao == null) {
                throw new IllegalArgumentException(
                        "Default UOM conversions are required when material-level conversion has no own ratio");
            }

            Optional<ConversaoUnidade> optionalConversaoUnidadePadrao = listaConversoesPadrao.stream()
                    .filter(x -> x.getUnidadeMedidaOrigem().equals(getUnidadeMedidaOrigem())
                            && x.getUnidadeMedidaDestino().equals(getUnidadeMedidaDestino()))
                    .findAny();
            // se houver conversão padrão, retorna ela. senão retorna 1
            return optionalConversaoUnidadePadrao
                    .map(ConversaoUnidade::getQuantidadeUnidadeDestinoPorUnidadeOrigem)
                    .orElse(1.0);
        } else {
            return quantidadeUnidadeDestinoPorUnidadeOrigemResolvida;
        }
    }

    /**
     * Resolve somente a conversao cadastrada na propria linha material/UOM.
     *
     * <p>Retornar `null` aqui e intencional: o overload que recebe conversoes
     * padrao usa esse sinal para aplicar fallback global. O metodo publico sem
     * fallback transforma o mesmo `null` em erro funcional.</p>
     */
    @Nullable
    private Double getQuantidadeUnidadeDestinoPorUnidadeOrigemPropriaOuNull() {

        if (getLegacyRatioState()
                == UnitOfMeasureConversionLegacyRatioState.CONFLICTING_LEGACY_AND_CANONICAL_RATIOS) {
            throw new UnitOfMeasureConversionException(
                    "Material-level UOM conversion has conflicting deprecated and canonical quantity ratios from "
                            + getUnidadeMedidaOrigem().getId()
                            + " to "
                            + getUnidadeMedidaDestino().getId()
                            + " for material "
                            + getProduto().getId());
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
     * Classifica a coexistencia entre o fator depreciado e as quantidades
     * canonicas da conversao especifica por material, sem persistir mudanca.
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
    
    /**
     * 
     * @param produto
     * @param unidadeMedidaOrigem
     * @param unidadeMedidaDestino
     * @param conversoesProduto
     * @param conversoesPadrao
     * @return conversão (float) ou nulo, caso não haja conversão disponível
     */
    @Nullable
    public static Double getQuantidadeUnidadeDestinoPorUnidadeOrigem(
            Produto produto, UnidadeMedida unidadeMedidaOrigem, UnidadeMedida unidadeMedidaDestino,
            Collection<ConversaoUnidadeProduto> conversoesProduto, 
            Collection<ConversaoUnidade> conversoesPadrao) {
        
        if (unidadeMedidaOrigem.getId().equals(unidadeMedidaDestino.getId())) {
            return 1.0;
        }
        
        Optional<ConversaoUnidadeProduto> optionalConversaoUnidadeProduto = conversoesProduto.stream()
                .filter(x -> x.getUnidadeMedidaOrigem().equals(unidadeMedidaOrigem)
                            && x.getUnidadeMedidaDestino().equals(unidadeMedidaDestino)
                            && x.getProduto().equals(produto))
                .findAny();

        return optionalConversaoUnidadeProduto
                .map(conversaoUnidadeProduto ->
                        conversaoUnidadeProduto.getQuantidadeUnidadeDestinoPorUnidadeOrigem(conversoesPadrao))
                .orElseGet(() -> {

                    Optional<ConversaoUnidade> optionalConversaoUnidade = conversoesPadrao.stream()
                            .filter(x -> x.getUnidadeMedidaOrigem().equals(unidadeMedidaOrigem)
                                    && x.getUnidadeMedidaDestino().equals(unidadeMedidaDestino))
                            .findAny();

                    return optionalConversaoUnidade
                            .map(ConversaoUnidade::getQuantidadeUnidadeDestinoPorUnidadeOrigem)
                            .orElse(null);

                });
    }
    
}
