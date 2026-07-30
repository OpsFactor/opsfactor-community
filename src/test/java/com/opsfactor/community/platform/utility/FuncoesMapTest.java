package com.opsfactor.community.platform.utility;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Testes dos helpers de mapa usados por projections Community em memoria.
 */
public class FuncoesMapTest {

    @Test
    @SuppressWarnings("unchecked")
    public void getNestedMapDeCollectionComGroupingShouldKeepGroupingContract() {

        List<String> valores = List.of("AA", "AB", "BA");

        Map<String, Set<String>> mapaPorPrimeiraLetra = (Map<String, Set<String>>)
                FuncoesMap.getNestedMapDeCollectionComGrouping(
                        valores,
                        valor -> valor.substring(0, 1));

        Assertions.assertEquals(Set.of("AA", "AB"), mapaPorPrimeiraLetra.get("A"));
        Assertions.assertEquals(Set.of("BA"), mapaPorPrimeiraLetra.get("B"));

    }

    @Test
    public void nestedMapHelpersShouldRejectMissingExtractionFunctionsAsInvalidArgument() {

        IllegalArgumentException groupingException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> FuncoesMap.getNestedMapDeCollectionComGrouping(List.of("A")));

        IllegalArgumentException reduceException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> FuncoesMap.getNestedMapDeCollectionComReduce(
                        List.of("A"),
                        (valorA, valorB) -> valorA));

        IllegalArgumentException totalizadorException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> FuncoesMap.getNestedMapDeCollectionComTotalizadorDouble(
                        List.of("A"),
                        valor -> 1.0));

        Assertions.assertEquals(
                "At least one nested map extraction function must be provided",
                groupingException.getMessage());
        Assertions.assertEquals(groupingException.getMessage(), reduceException.getMessage());
        Assertions.assertEquals(groupingException.getMessage(), totalizadorException.getMessage());

    }

    @Test
    public void getFromMapOrThrowExceptionIfNotFoundShouldRejectRequiredNullKeyAsInvalidArgument() {

        Map<String, Integer> mapa = Map.of("A", 1);

        Assertions.assertNull(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        mapa,
                        null,
                        false,
                        new IllegalStateException("Value not found")));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        mapa,
                        null,
                        true,
                        new IllegalStateException("Value not found")));

        Assertions.assertEquals(
                "Key must not be null when throwExceptionIfKeyIsNull = true",
                illegalArgumentException.getMessage());

    }

}
