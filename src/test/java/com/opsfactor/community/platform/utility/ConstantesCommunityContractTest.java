package com.opsfactor.community.platform.utility;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos dos enums compartilhados usados nas bordas Community/Enterprise.
 */
class ConstantesCommunityContractTest {

    @Test
    void tamanhoBucketShouldExposeStableAggregationOrderAndNames() {

        Assertions.assertEquals(1, Constantes.TamanhoBucket.SEGUNDO.getNivelAgregacao());
        Assertions.assertEquals(2, Constantes.TamanhoBucket.MINUTO.getNivelAgregacao());
        Assertions.assertEquals(3, Constantes.TamanhoBucket.SEXTO_HORA.getNivelAgregacao());
        Assertions.assertEquals(4, Constantes.TamanhoBucket.QUARTO_HORA.getNivelAgregacao());
        Assertions.assertEquals(5, Constantes.TamanhoBucket.MEIA_HORA.getNivelAgregacao());
        Assertions.assertEquals(6, Constantes.TamanhoBucket.HORARIO.getNivelAgregacao());
        Assertions.assertEquals(7, Constantes.TamanhoBucket.TURNO.getNivelAgregacao());
        Assertions.assertEquals(8, Constantes.TamanhoBucket.DIARIO.getNivelAgregacao());
        Assertions.assertEquals(9, Constantes.TamanhoBucket.SEMANAL.getNivelAgregacao());
        Assertions.assertEquals(10, Constantes.TamanhoBucket.MENSAL.getNivelAgregacao());
        Assertions.assertEquals(11, Constantes.TamanhoBucket.ANUAL.getNivelAgregacao());

        Assertions.assertEquals("Day", Constantes.TamanhoBucket.DIARIO.getNomePeriodoSingular());
        Assertions.assertEquals("Weeks", Constantes.TamanhoBucket.SEMANAL.getNomePeriodoPlural());

    }

    @Test
    void tipoQuantidadeValorShouldConvertOnlyMonetaryMeasuresToTipoValor() {

        Assertions.assertEquals(
                Constantes.TipoValor.GROSS,
                Constantes.TipoQuantidadeValor.GROSS.converteParaTipoValor());
        Assertions.assertEquals(
                Constantes.TipoValor.NET,
                Constantes.TipoQuantidadeValor.NET.converteParaTipoValor());
        Assertions.assertEquals(
                Constantes.TipoValor.COGS,
                Constantes.TipoQuantidadeValor.COGS.converteParaTipoValor());
        Assertions.assertEquals(
                Constantes.TipoValor.MARGEM,
                Constantes.TipoQuantidadeValor.MARGEM.converteParaTipoValor());

    }

    @Test
    void tipoQuantidadeValorShouldFailClearlyWhenQuantityIsConvertedToTipoValor() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Constantes.TipoQuantidadeValor.QUANTIDADE.converteParaTipoValor());

        Assertions.assertEquals(
                "Impossivel converter QUANTITY para TipoValor",
                illegalArgumentException.getMessage());

    }

}
