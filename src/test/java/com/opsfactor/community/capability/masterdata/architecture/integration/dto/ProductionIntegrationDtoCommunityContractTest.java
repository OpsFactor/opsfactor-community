package com.opsfactor.community.capability.masterdata.architecture.integration.dto;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaComponenteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.routing.integration.dto.RoteiroIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.dto.VersaoProducaoSimplesIntegrationDataDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrato de shape dos DTOs de data upload produtivo Community.
 *
 * <p>Os headers dos templates ja sao cobertos pelos mappers. Este teste fecha
 * tambem o payload JSON/CSV desserializado, evitando que campos Enterprise como
 * custo, setup detalhado, manutencao, turnos, line scheduling ou parallel
 * routing/output voltem como atributos publicos silenciosos.</p>
 */
public class ProductionIntegrationDtoCommunityContractTest {

    @Test
    public void billOfMaterialsDtoShouldExposeOnlySimpleCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "description",
                        "locationId",
                        "outputMaterialId",
                        "outputQuantity",
                        "outputUomId",
                        "priority",
                        "active",
                        "canBeUsedWithoutProductionVersion"),
                getDeclaredFieldNames(ListaTecnicaIntegrationDataDto.class));
        Assertions.assertEquals(
                Set.of("id"),
                getDeclaredFieldNames(ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO.class));

    }

    @Test
    public void billOfMaterialsComponentDtoShouldExposeOnlySimpleCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "componentMaterialQuantityUomId",
                        "componentMaterialQuantity"),
                getDeclaredFieldNames(ListaTecnicaComponenteIntegrationDataDto.class));
        Assertions.assertEquals(
                Set.of(
                        "bomId",
                        "componentMaterialId"),
                getDeclaredFieldNames(
                        ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO.class));

    }

    @Test
    public void routingDtoShouldExposeOnlySimpleCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "locationId",
                        "description",
                        "active",
                        "outputMaterialId",
                        "routingClusterId",
                        "canBeUsedWithoutProductionVersion",
                        "priority"),
                getDeclaredFieldNames(RoteiroIntegrationDataDto.class));
        Assertions.assertEquals(
                Set.of("id"),
                getDeclaredFieldNames(RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO.class));

    }

    @Test
    public void simpleProductionVersionDtoShouldExposeOnlySingleOutputCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "locationId",
                        "priority",
                        "outputMaterialId",
                        "routingId",
                        "billOfMaterialsId",
                        "active"),
                getDeclaredFieldNames(VersaoProducaoSimplesIntegrationDataDto.class));
        Assertions.assertEquals(
                Set.of("id"),
                getDeclaredFieldNames(
                        VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO.class));

    }

    private static Set<String> getDeclaredFieldNames(Class<?> dtoClass) {

        return Arrays
                .stream(dtoClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

    }

}
