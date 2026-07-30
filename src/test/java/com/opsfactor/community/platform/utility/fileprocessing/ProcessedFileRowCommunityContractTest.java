package com.opsfactor.community.platform.utility.fileprocessing;

import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeParseException;

/**
 * Contratos dos conversores tipados de {@link ProcessedFileRow}.
 *
 * <p>Os mappers de integracao dependem desta classe para traduzir valores de
 * arquivo para tipos de dominio. Falhas de conversao precisam preservar o
 * contexto da coluna para que o usuario corrija o template enviado.</p>
 */
class ProcessedFileRowCommunityContractTest {

    @Test
    void getColumnValueAsBooleanShouldAcceptExplicitBinaryAndTextValues() {

        ProcessedFileRow processedFileRow = new ProcessedFileRow();
        processedFileRow.addContent("1");
        processedFileRow.addContent("0");
        processedFileRow.addContent(" true ");
        processedFileRow.addContent(" false ");

        Assertions.assertTrue(processedFileRow.getColumnValueAsBoolean(0));
        Assertions.assertFalse(processedFileRow.getColumnValueAsBoolean(1));
        Assertions.assertTrue(processedFileRow.getColumnValueAsBoolean(2));
        Assertions.assertFalse(processedFileRow.getColumnValueAsBoolean(3));

    }

    @Test
    void getColumnValueAsBooleanShouldTranslateInvalidValueToDataUploadException() {

        ProcessedFileRow processedFileRow = new ProcessedFileRow();
        processedFileRow.addContent("material-01");
        processedFileRow.addContent("yes");

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> processedFileRow.getColumnValueAsBoolean(1));

        Assertions.assertEquals(
                "Incompatible type for column 2 : should be a binary number (0/1 or true/false) but found yes",
                dataUploadException.getMessage());
        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                dataUploadException.getCause());

    }

    @Test
    void getColumnValueAsDateAndTimeShouldTranslateInvalidValueToDataUploadException() {

        ProcessedFileRow processedFileRow = new ProcessedFileRow();
        processedFileRow.addContent("not-a-date-time");
        processedFileRow.addContent("not-a-date");
        processedFileRow.addContent("not-a-time");

        DataUploadException localDateTimeDataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> processedFileRow.getColumnValueAsLocalDateTime(0));
        DataUploadException localDateDataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> processedFileRow.getColumnValueAsLocalDate(1));
        DataUploadException localTimeDataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> processedFileRow.getColumnValueAsLocalTime(2));

        Assertions.assertEquals(
                "Incompatible type for column 1 : should be date/time but found not-a-date-time",
                localDateTimeDataUploadException.getMessage());
        Assertions.assertEquals(
                "Incompatible type for column 2 : should be date/time but found not-a-date",
                localDateDataUploadException.getMessage());
        Assertions.assertEquals(
                "Incompatible type for column 3 : should be time but found not-a-time",
                localTimeDataUploadException.getMessage());
        Assertions.assertInstanceOf(
                DateTimeParseException.class,
                localDateTimeDataUploadException.getCause());
        Assertions.assertInstanceOf(
                DateTimeParseException.class,
                localDateDataUploadException.getCause());
        Assertions.assertInstanceOf(
                DateTimeParseException.class,
                localTimeDataUploadException.getCause());

    }

    @Test
    void getColumnValueAsNumberShouldTranslateInvalidValueAndPreserveParseCause() {

        ProcessedFileRow processedFileRow = new ProcessedFileRow();
        processedFileRow.addContent("not-a-float");
        processedFileRow.addContent("not-a-long");
        processedFileRow.addContent("not-an-integer");
        processedFileRow.addContent("not-a-double");

        DataUploadException floatDataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> processedFileRow.getColumnValueAsFloat(0));
        DataUploadException longDataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> processedFileRow.getColumnValueAsLong(1));
        DataUploadException integerDataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> processedFileRow.getColumnValueAsInteger(2));
        DataUploadException doubleDataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> processedFileRow.getColumnValueAsDouble(3));

        Assertions.assertEquals(
                "Incompatible type for column 1 : should be a decimal number (with . decimal separator) but found not-a-float",
                floatDataUploadException.getMessage());
        Assertions.assertEquals(
                "Incompatible type for column 2 : should be a long number (no decimal separator) but found not-a-long",
                longDataUploadException.getMessage());
        Assertions.assertEquals(
                "Incompatible type for column 3 : should be an integer number (no decimal separator) but found not-an-integer",
                integerDataUploadException.getMessage());
        Assertions.assertEquals(
                "Incompatible type for column 4 : should be a decimal number (with . decimal separator) but found not-a-double",
                doubleDataUploadException.getMessage());
        Assertions.assertInstanceOf(
                NumberFormatException.class,
                floatDataUploadException.getCause());
        Assertions.assertInstanceOf(
                NumberFormatException.class,
                longDataUploadException.getCause());
        Assertions.assertInstanceOf(
                NumberFormatException.class,
                integerDataUploadException.getCause());
        Assertions.assertInstanceOf(
                NumberFormatException.class,
                doubleDataUploadException.getCause());

    }

}
