package com.opsfactor.community.capability.supplyplanning.engine.constrained;

import java.util.List;
import java.util.Objects;
import java.util.function.DoubleConsumer;

/** Aplica um corte proporcional aos volumes atuais de um grupo na mesma unidade de medida. */
public final class CorteProporcional {

    private CorteProporcional() {
    }

    /** A quantidade serve como peso; a ação recebe a fração a cortar, entre zero e um. */
    public record Parcela(double quantidade, DoubleConsumer aplicaFracaoCortada) {
        public Parcela {
            if (!Double.isFinite(quantidade) || quantidade < 0) {
                throw new IllegalArgumentException("Invalid quantity for proportional cut: " + quantidade);
            }
            Objects.requireNonNull(aplicaFracaoCortada);
        }
    }

    /** Retorna a parte do déficit que o grupo não conseguiu absorver. */
    public static double aplica(double deficit, List<Parcela> parcelas) {

        if (!Double.isFinite(deficit) || deficit < 0) {
            throw new IllegalArgumentException("Invalid deficit for proportional cut: " + deficit);
        }
        if (deficit == 0) {
            return 0;
        }

        double quantidadeTotal = parcelas.stream().mapToDouble(Parcela::quantidade).sum();
        if (!Double.isFinite(quantidadeTotal)) {
            throw new IllegalArgumentException("Invalid total quantity for proportional cut: " + quantidadeTotal);
        }
        if (quantidadeTotal == 0) {
            return deficit;
        }

        double corte = Math.min(deficit, quantidadeTotal);
        double fracaoCortada = corte / quantidadeTotal;
        for (Parcela parcela : parcelas) {
            if (parcela.quantidade() > 0) {
                parcela.aplicaFracaoCortada().accept(fracaoCortada);
            }
        }
        return deficit - corte;

    }
}
