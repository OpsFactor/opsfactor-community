package com.opsfactor.community.platform.utility.fileprocessing;

import com.opsfactor.community.platform.exception.DataUploadException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Numeric file columns must not silently truncate fractions, wrap or saturate integer values. */
class ProcessedFileRowNumericImportTest {

    @Test
    void exactIntegerValuesAndBinaryBoundaryValuesAreAccepted() {

        assertEquals(Long.MIN_VALUE, row(Long.MIN_VALUE).getColumnValueAsLong(0));
        assertEquals(Long.MAX_VALUE, row(Long.MAX_VALUE).getColumnValueAsLong(0));
        assertEquals(Long.MIN_VALUE, row(-0x1p63).getColumnValueAsLong(0));
        assertEquals((long) Math.nextDown(0x1p63), row(Math.nextDown(0x1p63)).getColumnValueAsLong(0));
        assertEquals(Integer.MIN_VALUE, row((double) Integer.MIN_VALUE).getColumnValueAsInteger(0));
        assertEquals(Integer.MIN_VALUE, row((float) Integer.MIN_VALUE).getColumnValueAsInteger(0));
        assertEquals(Integer.MAX_VALUE, row((double) Integer.MAX_VALUE).getColumnValueAsInteger(0));
        assertEquals(123, row(123L).getColumnValueAsInteger(0));
        assertEquals(123L, row(123f).getColumnValueAsLong(0));
        assertEquals(123, row(new BigDecimal("123.000")).getColumnValueAsInteger(0));
        assertEquals(Long.MAX_VALUE, row(BigInteger.valueOf(Long.MAX_VALUE)).getColumnValueAsLong(0));

    }

    @Test
    void integerGettersRejectFractionsOverflowAndNonFiniteValuesWithColumnContext() {

        for (Object value : List.of(1.9d, -1.9f, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                0x1p63, Math.nextDown(-0x1p63), new BigDecimal("1.5"),
                BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE))) {
            DataUploadException error = assertThrows(DataUploadException.class,
                    () -> row(value).getColumnValueAsLong(0), value.toString());
            assertTrue(error.getMessage().contains("column 1"));
            assertNotNull(error.getCause());
        }
        for (Object value : List.of(2147483648L, -2147483649L, 1.9d, -1.9f,
                Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                (float) Integer.MAX_VALUE, new BigDecimal("1.5"), Long.MAX_VALUE)) {
            DataUploadException error = assertThrows(DataUploadException.class,
                    () -> row(value).getColumnValueAsInteger(0), value.toString());
            assertTrue(error.getMessage().contains("column 1"));
            assertNotNull(error.getCause());
        }

    }

    @Test
    void textIntegerSyntaxAndDecimalFloatDoubleConversionsKeepTheirExistingContracts() {

        assertEquals(123, row("00123").getColumnValueAsInteger(0));
        assertEquals(Long.MAX_VALUE, row(Long.toString(Long.MAX_VALUE)).getColumnValueAsLong(0));
        for (String decimal : List.of("1.0", "1,0", "1e3")) {
            assertThrows(DataUploadException.class, () -> row(decimal).getColumnValueAsInteger(0));
            assertThrows(DataUploadException.class, () -> row(decimal).getColumnValueAsLong(0));
        }
        for (Object value : List.of(1.9d, "1.9", "1,9")) {
            assertEquals(1.9d, row(value).getColumnValueAsDouble(0));
            assertEquals(1.9f, row(value).getColumnValueAsFloat(0));
        }
        assertEquals("00123", row("00123").getColumnValueAsString(0));
        assertNull(row(null).getColumnValueAsInteger(0));
        assertNull(row(null).getColumnValueAsLong(0));

    }

    /** Preserve each runtime Number type so both integral and floating-point paths are exercised. */
    private static ProcessedFileRow row(Object value) {

        return new ProcessedFileRow().row(Arrays.asList(value));

    }
}
