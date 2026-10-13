package com.opsfactor.community.platform.integration.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Regressions for shared diagnostics, independent of a particular business mapper. */
class IntegrationFieldValidationTest {

    @Test
    void identifiesTheActualMissingReferenceEvenWhenTwoFieldsAreNull() {

        TestRecord record = new TestRecord(false);
        record.setIntegrationInputContext(new IntegrationInputContext(true, 181, List.of("Material Id", "Location Id")));
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> IntegrationFieldValidation.resolveReference(Map.of(), record, "materialId", null, true, 0));
        assertEquals("Required foreign key field 'Material Id' is missing", exception.getMessage());

    }

    @Test
    void usesJsonPropertyNameAndAllowsOptionalMissingReferences() {

        TestRecord record = new TestRecord(false);
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> IntegrationFieldValidation.resolveReference(Map.of(), record, "materialId", null, true, 0));
        assertEquals("Required foreign key field 'externalMaterialId' is missing", exception.getMessage());
        assertNull(IntegrationFieldValidation.resolveReference(Map.of(), record, "materialId", null, false, 0));

    }

    @Test
    void differentiatesMissingPrimaryKeyScalarAndUnresolvedReference() {

        TestRecord record = new TestRecord(true);
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> IntegrationFieldValidation.validateRequiredFields(record));
        assertEquals("Primary key field 'externalMaterialId' is missing", exception.getMessage());
        record.materialId = "UNKNOWN";
        MissingDependencyDataUploadException unresolved = assertThrows(MissingDependencyDataUploadException.class,
                () -> IntegrationFieldValidation.resolveReference(Map.of(), record, "materialId", record.materialId, true, 0));
        assertTrue(unresolved.getMessage().startsWith("Primary key reference field 'externalMaterialId' references an unknown value 'UNKNOWN'"));
        assertSame(record, unresolved.getDto());

    }

    @Test
    void requiredScalarHasItsOwnMessageAndMetadataIsNeverSerialized() throws Exception {

        TestRecord record = new TestRecord(false);
        assertEquals("Required field 'externalMaterialId' is missing", assertThrows(DataUploadException.class,
                () -> IntegrationFieldValidation.validateRequiredFields(record)).getMessage());
        record.materialId = "MAT";
        record.setIntegrationInputContext(new IntegrationInputContext(true, 181, List.of("Material Id")));
        String json = new ObjectMapper().writeValueAsString(record);
        assertFalse(json.contains("integrationInputContext"));
        assertFalse(json.contains("integrationPrimaryKey"));
        assertTrue(json.contains("externalMaterialId"));

    }

    public static class TestRecord implements IntegrationInputAware {

        @IntegrationFileColumn(value = 0, required = true)
        @JsonProperty("externalMaterialId")
        public String materialId;
        @IntegrationFileColumn(1)
        public String locationId;
        private final boolean primaryKey;
        private IntegrationInputContext context;

        TestRecord(boolean primaryKey) { this.primaryKey = primaryKey; }

        @Override
        public IntegrationInputContext getIntegrationInputContext() { return context; }
        @Override
        public void setIntegrationInputContext(IntegrationInputContext context) { this.context = context; }
        @Override
        public boolean isIntegrationPrimaryKey() { return primaryKey; }

    }

}
