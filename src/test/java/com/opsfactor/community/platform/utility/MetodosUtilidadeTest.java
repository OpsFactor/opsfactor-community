package com.opsfactor.community.platform.utility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.utility.statistical.NormalDistributionCustom;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Testes de utilitarios matematicos usados por rotinas Community.
 */
public class MetodosUtilidadeTest {

    @Test
    public void testGetPercentilDaObservacao() {

        Assertions.assertNull(MetodosUtilidade.getPercentilDaObservacao(null, 3f));
        Assertions.assertNull(MetodosUtilidade.getPercentilDaObservacao(new ArrayList<>(), 3f));

        List<Float> listaObservacoes = new ArrayList<>();
        listaObservacoes.add(2f);
        listaObservacoes.add(1f);
        listaObservacoes.add(4f);
        listaObservacoes.add(5f);
        listaObservacoes.add(3f);
        Assertions.assertEquals(0f, MetodosUtilidade.getPercentilDaObservacao(listaObservacoes, 0f)); // percentil 0
        Assertions.assertEquals(0.5f, MetodosUtilidade.getPercentilDaObservacao(listaObservacoes, 3f)); // percentil 0.5
        Assertions.assertEquals(1f, MetodosUtilidade.getPercentilDaObservacao(listaObservacoes, 5f)); // percentil 1

        listaObservacoes.add(6f); // 1,2,3,4,5,6 : numero impar de observacoes
        Assertions.assertEquals(0.5f, MetodosUtilidade.getPercentilDaObservacao(listaObservacoes, 3.5f)); // percentil 0.5

        listaObservacoes.remove(5);
        listaObservacoes.add(3f); // 1,2,3,3,4,5 : repetição de valor
        Assertions.assertEquals(0.6f, MetodosUtilidade.getPercentilDaObservacao(listaObservacoes, 3f), 0.0001f); // percentil 0.6

        listaObservacoes.clear();
        listaObservacoes.add(3.5f);
        listaObservacoes.add(4.6f);
        listaObservacoes.add(6.2f);
        Assertions.assertEquals(
                0.875f,
                MetodosUtilidade.getPercentilDaObservacao(listaObservacoes, 5.8f),
                0.0001f); // entre 0.5 e 1.0
    }

    @Test
    public void fatorialShouldAcceptZeroAndRejectNegativeInput() {

        Assertions.assertEquals(1, MetodosUtilidade.fatorial(0));
        Assertions.assertEquals(1, MetodosUtilidade.fatorial(1));
        Assertions.assertEquals(120, MetodosUtilidade.fatorial(5));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MetodosUtilidade.fatorial(-1));

    }

    @Test
    public void converteStringParaBooleanShouldAcceptOnlyExplicitBooleanValues() {

        Assertions.assertTrue(MetodosUtilidade.converteStringParaBoolean("1"));
        Assertions.assertTrue(MetodosUtilidade.converteStringParaBoolean(" true "));
        Assertions.assertFalse(MetodosUtilidade.converteStringParaBoolean("0"));
        Assertions.assertFalse(MetodosUtilidade.converteStringParaBoolean(" false "));

        IllegalArgumentException missingValueException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MetodosUtilidade.converteStringParaBoolean(null));
        Assertions.assertEquals(
                "Boolean value must be provided as 0/1 or true/false.",
                missingValueException.getMessage());

        IllegalArgumentException blankValueException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MetodosUtilidade.converteStringParaBoolean(" "));
        Assertions.assertEquals(
                "Boolean value must be provided as 0/1 or true/false.",
                blankValueException.getMessage());

        IllegalArgumentException invalidValueException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MetodosUtilidade.converteStringParaBoolean("yes"));
        Assertions.assertEquals(
                "String yes is not a boolean value: should be 0/1 or true/false",
                invalidValueException.getMessage());

    }

    @Test
    public void normalDistributionCustomShouldRejectNegativeStandardDeviationAsInvalidArgument() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new NormalDistributionCustom(10.0, -0.1));

        Assertions.assertEquals(
                "Standard deviation must be >= 0",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getValorEnumDeJsonPropertyShouldRejectUnknownValueAsInvalidArgument() {

        Assertions.assertEquals(
                JsonPropertyEnumTest.VALUE_A,
                MetodosUtilidade.getValorEnumDeJsonProperty(JsonPropertyEnumTest.class, "Value A"));
        Assertions.assertEquals(
                JsonPropertyEnumTest.VALUE_B,
                MetodosUtilidade.getValorEnumDeJsonProperty(JsonPropertyEnumTest.class, "VALUE_B"));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> MetodosUtilidade.getValorEnumDeJsonProperty(JsonPropertyEnumTest.class, "Value C"));

        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains("Invalid value Value C"));

    }
    
    @Test
    public void testaRepetibilidadeNormalDistributionCustom() {
        NormalDistributionCustom distribuicaoNormal = new NormalDistributionCustom(30.4, 12.3);
        
        double sample1 = distribuicaoNormal.sample();
        double sample2 = distribuicaoNormal.sample();
        double sample3 = distribuicaoNormal.sample();
        
        distribuicaoNormal.resetRandomGenerator();
        
        double sample4 = distribuicaoNormal.sample();
        double sample5 = distribuicaoNormal.sample();
        double sample6 = distribuicaoNormal.sample();
        
        Assertions.assertEquals(sample1, sample4, 0.00001);
        Assertions.assertEquals(sample2, sample5, 0.00001);
        Assertions.assertEquals(sample3, sample6, 0.00001);
        
        // seed arbitrário
        distribuicaoNormal.reseedRandomGenerator(2381234);
        
        double sample7 = distribuicaoNormal.sample();
        double sample8 = distribuicaoNormal.sample();
        double sample9 = distribuicaoNormal.sample();
        
        Assertions.assertNotEquals(sample1, sample7);
        Assertions.assertNotEquals(sample2, sample8);
        Assertions.assertNotEquals(sample3, sample9);

    }

    private enum JsonPropertyEnumTest {

        @JsonProperty("Value A")
        VALUE_A,
        VALUE_B

    }

}

