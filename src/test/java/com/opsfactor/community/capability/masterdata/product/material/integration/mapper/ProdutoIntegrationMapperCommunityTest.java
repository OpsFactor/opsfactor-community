package com.opsfactor.community.capability.masterdata.product.material.integration.mapper;

import com.opsfactor.community.capability.masterdata.product.material.integration.dto.ProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Valida o contrato Community do data upload de materiais.
 *
 * <p>O Community aceita o cadastro operacional do material e suas unidades de
 * medida basicas, mas caracteristicas dinamicas de material pertencem ao
 * Enterprise.</p>
 */
public class ProdutoIntegrationMapperCommunityTest {

    private static final Set<String> COMMUNITY_ACCEPTED_FIELD_NAMES = Set.of(
            "description",
            "active",
            "lifecycleStage",
            "operationalModel",
            "introductionDate",
            "discontinuationDate",
            "defaultUomId",
            "salesUomId",
            "transferUomId");

    @Test
    public void materialShouldRejectCharacteristicsCommunity() {

        ProdutoIntegrationMapper produtoIntegrationMapper = new ProdutoIntegrationMapper();
        ProdutoIntegrationDataDto produtoIntegrationDataDto =
                ProdutoIntegrationDataDto.builder()
                        .valueByCharacteristic(new HashMap<>())
                        .build();
        produtoIntegrationDataDto.valueByCharacteristic.put("BRAND", "A");

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> produtoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        new Produto("MAT_01"),
                        produtoIntegrationDataDto,
                        getProdutoIntegrationSupportData(),
                        null));

    }

    @Test
    public void materialShouldRejectEveryNonCommunityField() throws IllegalAccessException {

        ProdutoIntegrationMapper produtoIntegrationMapper = new ProdutoIntegrationMapper();

        /*
         * Allowlist reflexiva do DTO de material Community. Hoje somente
         * caracteristicas dinamicas ficam fora do contrato publico, mas o teste
         * protege o recorte contra futuros campos Enterprise adicionados ao DTO
         * compartilhado sem validacao explicita no mapper.
         */
        for (Field field : ProdutoIntegrationDataDto.class.getDeclaredFields()) {
            if (COMMUNITY_ACCEPTED_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            ProdutoIntegrationDataDto produtoIntegrationDataDto =
                    ProdutoIntegrationDataDto.builder()
                            .build();
            field.setAccessible(true);
            field.set(produtoIntegrationDataDto, getEnterpriseFieldValue(field));

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> produtoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                            new Produto("MAT_01"),
                            produtoIntegrationDataDto,
                            getProdutoIntegrationSupportData(),
                            null));
        }

    }

    @Test
    public void materialTemplateShouldNotExposeDynamicCharacteristicsCommunity() {

        ProdutoIntegrationMapper produtoIntegrationMapper = new ProdutoIntegrationMapper();
        List<String> processedFileHeaders = produtoIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertTrue(ProdutoIntegrationMapper.class.isAnnotationPresent(Component.class));
        Assertions.assertFalse(processedFileHeaders.contains("BRAND"));
        Assertions.assertEquals(10, processedFileHeaders.size());
        Assertions.assertTrue(processedFileHeaders.stream().anyMatch(
                header -> header.contains("Operational Model")));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("BRAND"));

    }

    @Test
    public void materialIntegrationShouldKeepEnterpriseEconomicFieldsDefensivelyHidden() {

        ProdutoIntegrationMapper produtoIntegrationMapper = new ProdutoIntegrationMapper();
        List<String> processedFileHeaders = produtoIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertTrue(Arrays.stream(ProdutoIntegrationDataDto.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("unitCogs")));
        Assertions.assertTrue(Arrays.stream(ProdutoIntegrationDataDto.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("unitCogsUnitOfMeasureId")));
        Assertions.assertFalse(processedFileHeaders.stream()
                .anyMatch(header -> header.contains("COGS")
                        || header.contains("Price")));

    }

    @Test
    public void materialShouldPreserveOperationalModelConfigurationAndFallback() {

        ProdutoIntegrationMapper produtoIntegrationMapper = new ProdutoIntegrationMapper();
        Produto material = new Produto("MAT_01");

        produtoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                material,
                ProdutoIntegrationDataDto.builder()
                        .operationalModel(Constantes.SNPModeloOperacional.MTO)
                        .build(),
                getProdutoIntegrationSupportData(),
                null);

        Assertions.assertEquals(
                Constantes.SNPModeloOperacional.MTO,
                material.getModeloOperacionalCadastrado());
        Assertions.assertEquals(
                Constantes.SNPModeloOperacional.MTO,
                produtoIntegrationMapper.getDtoWithoutPrimaryKeyFromEntity(material).operationalModel);

        produtoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                material,
                ProdutoIntegrationDataDto.builder().build(),
                getProdutoIntegrationSupportData(),
                null);

        Assertions.assertNull(material.getModeloOperacionalCadastrado());
        Assertions.assertEquals(Constantes.SNPModeloOperacional.MTS, material.getModeloOperacional());
        Assertions.assertNull(
                produtoIntegrationMapper.getDtoWithoutPrimaryKeyFromEntity(material).operationalModel);

    }

    @Test
    public void materialFileShouldRoundTripOperationalModelAtDedicatedColumn() {

        ProdutoIntegrationMapper produtoIntegrationMapper = new ProdutoIntegrationMapper();
        Produto material = new Produto("MAT_01");
        material.setModeloOperacional(Constantes.SNPModeloOperacional.MTO);

        ProcessedFileRow processedFileRow = produtoIntegrationMapper.convertEntityToProcessedFileRow(
                material,
                getProdutoIntegrationSupportData());
        ProdutoIntegrationDataDto produtoIntegrationDataDto =
                produtoIntegrationMapper.getDtoWithoutPrimaryKeyFromProcessedFileRow(
                        processedFileRow,
                        getProdutoIntegrationSupportData());

        Assertions.assertEquals(10, processedFileRow.getRowSize());
        Assertions.assertEquals("MTO", processedFileRow.getColumnValue(9));
        Assertions.assertEquals(
                Constantes.SNPModeloOperacional.MTO,
                produtoIntegrationDataDto.operationalModel);

    }

    private ProdutoIntegrationSupportData getProdutoIntegrationSupportData() {

        return ProdutoIntegrationSupportData.builder()
                .unidadeMedidaMap(new HashMap<>())
                .build();

    }

    private Object getEnterpriseFieldValue(Field field) {

        if (Map.class.isAssignableFrom(field.getType())) {
            return Map.of("EnterpriseCharacteristic", "Value");
        }
        if (Double.class.equals(field.getType())) {
            return 1.0d;
        }
        if (String.class.equals(field.getType())) {
            return "ENTERPRISE_UOM";
        }

        throw new IllegalArgumentException("Unsupported Enterprise field type in test: " + field);

    }

}
