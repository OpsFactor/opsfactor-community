package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.integration.mapper;

import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationDataDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationFiltroDto;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto.SelloutIntegrationDataDto;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto.SelloutIntegrationFiltroDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.mapper.EstoqueIntegrationMapper;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.mapper.SelloutIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valida o contrato Community das cargas transacionais basicas.
 *
 * <p>Sell-out e estoque inicial sao superficies abertas. Este teste impede que
 * headers ou exports de valores, precos, pedidos, sell-in, campanhas/eventos,
 * lote ou validade voltem para o template Community por acidente.</p>
 */
public class HistoricalDataIntegrationMapperCommunityTest {

    @Test
    public void historicalDataMappersShouldBeSpringComponents() {

        Assertions.assertTrue(SelloutIntegrationMapper.class.isAnnotationPresent(Component.class));
        Assertions.assertTrue(EstoqueIntegrationMapper.class.isAnnotationPresent(Component.class));

    }

    @Test
    public void selloutDtoShouldExposeOnlyCommunityFields() {

        /*
         * Sell-out e a unica venda historica carregavel no Community. Como nao
         * existem campos Enterprise transicionais neste DTO, travamos o shape
         * publico inteiro para impedir reintroducao silenciosa de valor, preco,
         * sell-in, pedidos, campanhas ou eventos.
         */
        Assertions.assertEquals(
                Set.of(
                        "referenceDate",
                        "originLocationId",
                        "materialId",
                        "uomId",
                        "quantity"),
                getDeclaredFieldNames(SelloutIntegrationDataDto.class));

    }

    @Test
    public void selloutFilterShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "startDate",
                        "endDate",
                        "locationType"),
                getDeclaredFieldNames(SelloutIntegrationFiltroDto.class));

    }

    @Test
    public void stockDtoShouldExposeOnlyCommunityFields() {

        /*
         * Estoque Community e snapshot inicial simples. Lote, aging, validade,
         * batch de producao e campos de valor pertencem ao Enterprise e nao
         * devem reaparecer no DTO publico.
         */
        Assertions.assertEquals(
                Set.of(
                        "uomId",
                        "quantity",
                        "delete"),
                getDeclaredFieldNames(EstoqueIntegrationDataDto.class));

    }

    @Test
    public void stockFilterShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "startDate",
                        "endDate",
                        "locationType"),
                getDeclaredFieldNames(EstoqueIntegrationFiltroDto.class));

    }

    @Test
    public void selloutTemplateShouldExposeOnlyQuantitativeHistoricalSalesCommunity() {

        SelloutIntegrationMapper selloutIntegrationMapper = new SelloutIntegrationMapper();
        List<String> processedFileHeaders = selloutIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertEquals(
                List.of(
                        "Document Id",
                        "Reference Date",
                        "Origin Location Id",
                        "Material Id",
                        "Unit of Measure Id",
                        "Quantity"),
                processedFileHeaders);
        assertNoEnterpriseHeader(processedFileHeaders);
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Gross Value"));

    }

    @Test
    public void selloutExportShouldExposeOnlyQuantitativeHistoricalSalesCommunity() {

        SelloutIntegrationMapper selloutIntegrationMapper = new SelloutIntegrationMapper();
        Sellout sellout = new Sellout(
                new Location("LOC_01"),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                new Produto("MAT_01"));
        sellout.setId("DOC_01");
        sellout.setUnidadeMedida(new UnidadeMedida("UN"));
        sellout.setQuantidade(12.5d);

        ProcessedFileRow processedFileRow = selloutIntegrationMapper.convertEntityToProcessedFileRow(sellout, null);

        Assertions.assertEquals(6, processedFileRow.row().size());
        Assertions.assertEquals("DOC_01", processedFileRow.getColumnValue(0));
        Assertions.assertEquals(12.5d, processedFileRow.getColumnValue(5));

    }

    @Test
    public void stockTemplateShouldExposeOnlyInitialInventorySnapshotCommunity() {

        EstoqueIntegrationMapper estoqueIntegrationMapper = new EstoqueIntegrationMapper();
        List<String> processedFileHeaders = estoqueIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertEquals(
                List.of(
                        "Location Id",
                        "Material Id",
                        "Reference Date (stock at the start of the reference date)",
                        "Unit of Measure Id",
                        "Quantity"),
                processedFileHeaders);
        assertNoEnterpriseHeader(processedFileHeaders);
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Batch Id"));

    }

    @Test
    public void stockExportShouldExposeOnlyInitialInventorySnapshotCommunity() {

        EstoqueIntegrationMapper estoqueIntegrationMapper = new EstoqueIntegrationMapper();
        LocalDateTime referenceDate = LocalDateTime.of(2026, 1, 1, 0, 0);
        Estoque estoque = new Estoque(
                new Estoque.EstoqueCompositeKey(
                        new Location("LOC_01"),
                        new Produto("MAT_01"),
                        referenceDate),
                20.0d);
        estoque.setUnidadeMedida(new UnidadeMedida("UN"));

        ProcessedFileRow processedFileRow = estoqueIntegrationMapper.convertEntityToProcessedFileRow(estoque, null);

        Assertions.assertEquals(5, processedFileRow.row().size());
        Assertions.assertEquals("LOC_01", processedFileRow.getColumnValue(0));
        Assertions.assertEquals("MAT_01", processedFileRow.getColumnValue(1));
        Assertions.assertEquals(referenceDate.toString(), processedFileRow.getColumnValue(2));
        Assertions.assertEquals(20.0d, processedFileRow.getColumnValue(4));

    }

    private static void assertNoEnterpriseHeader(List<String> processedFileHeaders) {

        String joinedHeaders = String.join(" | ", processedFileHeaders).toLowerCase();
        Assertions.assertFalse(joinedHeaders.contains("price"));
        Assertions.assertFalse(joinedHeaders.contains("cost"));
        Assertions.assertFalse(joinedHeaders.contains("sell-in"));
        Assertions.assertFalse(joinedHeaders.contains("sales order"));
        Assertions.assertFalse(joinedHeaders.contains("campaign"));
        Assertions.assertFalse(joinedHeaders.contains("event"));
        Assertions.assertFalse(joinedHeaders.contains("batch"));
        Assertions.assertFalse(joinedHeaders.contains("expiration"));

    }

    private static Set<String> getDeclaredFieldNames(Class<?> dtoClass) {

        return Arrays.stream(dtoClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

    }

}
