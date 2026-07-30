package com.opsfactor.community.platform.utility.statistical;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.NotANumberException;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import java.util.Collection;

/**
 * Wrapper de {@link NormalDistribution} que aceita desvio padrao zero.
 *
 * <p>Rotinas Community usam esta classe quando uma distribuicao degenerada
 * precisa continuar sendo amostravel sem desviar da media. Apenas o subconjunto
 * de metodos usado pelas rotinas de planejamento e exposto aqui.</p>
 */
public class NormalDistributionCustom {
    
    private NormalDistribution normalDistribution;
    private Long startingSeed;
    RandomGenerator randomGenerator;
    
    private double mean;
    private double sd;
    
    /**
     * Cria uma distribuicao normal ou degenerada.
     *
     * <p>Media NaN segue o contrato do Apache Commons Math. Desvio padrao
     * negativo e argumento invalido do caller e falha como
     * {@link IllegalArgumentException}, nao como capability nao suportada.</p>
     */
    public NormalDistributionCustom(double mean, double sd) {

        if (Double.isNaN(mean)) throw new NotANumberException();
        if (sd < 0) throw new IllegalArgumentException("Standard deviation must be >= 0");
        this.mean = mean;
        this.sd = sd;
        
        if (sd > 0) {
            randomGenerator = new Well19937c();
            startingSeed = randomGenerator.nextLong();
            normalDistribution = new NormalDistribution(randomGenerator, mean, sd);
            resetRandomGenerator();
        }
    }
    
    public double sample() {
        if (sd <= 0) return mean;
        return normalDistribution.sample();
    }
    
    public double getMean() {
        return mean;
    }
    public double getStandardDeviation() {
        return sd;
    }
    
    public void resetRandomGenerator() {
        if (sd <= 0) return;
        normalDistribution.reseedRandomGenerator(startingSeed);
    }
    public void reseedRandomGenerator(long seed) {
        if (sd <= 0) return;
        startingSeed = seed;
        resetRandomGenerator();
    }

    /**
     * Referência: https://www.statisticshowto.com/pooled-standard-deviation/
     * Assume que o número de amostras usadas para gerar cada distribuição normal foi igual em cada caso
     * @param normalDistributionCustomCollection
     * @return
     */
    public static NormalDistributionCustom getNormalDistributionCustomDeDistribuicoesIndependentes(
            Collection<NormalDistributionCustom> normalDistributionCustomCollection) {

        double mean = normalDistributionCustomCollection.stream()
                .mapToDouble(NormalDistributionCustom::getMean)
                .average()
                .getAsDouble();

        double stDev =
                Math.sqrt(
                        normalDistributionCustomCollection
                                .stream()
                                .mapToDouble(normalDistribution -> Math.pow(normalDistribution.getStandardDeviation(), 2))
                                .sum()
                        / normalDistributionCustomCollection.size());

        return new NormalDistributionCustom(mean, stDev);

    }
        
}
