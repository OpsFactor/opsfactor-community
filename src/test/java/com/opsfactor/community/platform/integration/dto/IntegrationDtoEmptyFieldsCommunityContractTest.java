package com.opsfactor.community.platform.integration.dto;

import com.opsfactor.community.platform.integration.validation.IntegrationInputContext;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato de deteccao de DTOs de integracao vazios.
 *
 * <p>O filtro generico de data upload remove DTOs totalmente vazios antes de
 * criar, atualizar ou apagar entidades. A chave primaria vazia e a coluna
 * tecnica `delete` nao devem, por si so, tornar a linha funcionalmente
 * preenchida.</p>
 */
class IntegrationDtoEmptyFieldsCommunityContractTest {

    /** Input location cannot turn a business-empty DTO into a record to persist. */
    @Test
    void sourceContextDoesNotPopulateEmptyDtoOrPrimaryKey() {

        TestIntegrationDataDto dto = new TestIntegrationDataDto();
        dto.primaryKeyDto = new TestPrimaryKeyDto();
        var context = new IntegrationInputContext(true, 181, List.of("Material Id"));
        dto.setIntegrationInputContext(context);
        dto.primaryKeyDto.setIntegrationInputContext(context);
        Assertions.assertTrue(dto.allFieldsAreEmpty());
        Assertions.assertTrue(dto.primaryKeyDto.allFieldsAreEmpty());

    }

    @Test
    void dataDtoShouldTreatEmptyPrimaryKeyDtoAndDeleteFlagAsEmpty() {

        TestIntegrationDataDto testIntegrationDataDto = new TestIntegrationDataDto();
        testIntegrationDataDto.primaryKeyDto = new TestPrimaryKeyDto();
        testIntegrationDataDto.delete = "x";

        Assertions.assertTrue(testIntegrationDataDto.allFieldsAreEmpty());

    }

    @Test
    void dataDtoShouldNotTreatPrimaryKeyValueAsEmpty() {

        TestIntegrationDataDto testIntegrationDataDto = new TestIntegrationDataDto();
        testIntegrationDataDto.primaryKeyDto = new TestPrimaryKeyDto();
        testIntegrationDataDto.primaryKeyDto.id = "MAT-001";

        Assertions.assertFalse(testIntegrationDataDto.allFieldsAreEmpty());

    }

    @Test
    void dataDtoShouldNotTreatDataValueAsEmpty() {

        TestIntegrationDataDto testIntegrationDataDto = new TestIntegrationDataDto();
        testIntegrationDataDto.primaryKeyDto = new TestPrimaryKeyDto();
        testIntegrationDataDto.description = "Material 001";

        Assertions.assertFalse(testIntegrationDataDto.allFieldsAreEmpty());

    }

    private static class TestIntegrationDataDto
            extends IntegrationDataDtoAbstract<TestIntegrationDataDto, TestPrimaryKeyDto, Object> {

        public String description;

    }

    private static class TestPrimaryKeyDto
            extends IntegrationPrimaryKeyDTOAbstract<TestPrimaryKeyDto, Object> {

        public String id;

        @Override
        public boolean hasSameKeyAsEntity(Object entity) {

            return false;

        }

    }

}
