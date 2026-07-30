package com.opsfactor.community.platform.database.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos dos converters JPA usados por arrays numericos persistidos como
 * JSON em colunas texto.
 *
 * <p>Esses converters nao sao beans Spring; o JPA instancia a classe
 * diretamente. Por isso a protecao importante aqui e garantir round-trip
 * deterministico e falha explicita quando o valor persistido no banco nao e um
 * JSON compativel com o tipo esperado.</p>
 */
class ArrayToStringConverterCommunityContractTest {

    @Test
    void doubleArrayConverterShouldRoundTripJsonTextColumn() {

        DoubleArrayToStringConverter doubleArrayToStringConverter =
                new DoubleArrayToStringConverter();
        double[] valoresOriginais = new double[]{1.25d, 0.0d, -3.5d};

        String valorPersistido = doubleArrayToStringConverter.convertToDatabaseColumn(valoresOriginais);
        double[] valoresConvertidos = doubleArrayToStringConverter.convertToEntityAttribute(valorPersistido);

        Assertions.assertArrayEquals(
                valoresOriginais,
                valoresConvertidos);

    }

    @Test
    void doubleArrayConverterShouldFailExplicitlyWhenStoredValueIsInvalidJson() {

        DoubleArrayToStringConverter doubleArrayToStringConverter =
                new DoubleArrayToStringConverter();

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> doubleArrayToStringConverter.convertToEntityAttribute("invalid-json"));

        Assertions.assertTrue(illegalStateException.getMessage().contains("double array"));

    }

    @Test
    void floatArrayConverterShouldRoundTripJsonTextColumn() {

        FloatArrayToStringConverter floatArrayToStringConverter =
                new FloatArrayToStringConverter();
        float[] valoresOriginais = new float[]{1.25f, 0.0f, -3.5f};

        String valorPersistido = floatArrayToStringConverter.convertToDatabaseColumn(valoresOriginais);
        float[] valoresConvertidos = floatArrayToStringConverter.convertToEntityAttribute(valorPersistido);

        Assertions.assertArrayEquals(
                valoresOriginais,
                valoresConvertidos);

    }

    @Test
    void floatArrayConverterShouldFailExplicitlyWhenStoredValueIsInvalidJson() {

        FloatArrayToStringConverter floatArrayToStringConverter =
                new FloatArrayToStringConverter();

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> floatArrayToStringConverter.convertToEntityAttribute("invalid-json"));

        Assertions.assertTrue(illegalStateException.getMessage().contains("float array"));

    }

}
