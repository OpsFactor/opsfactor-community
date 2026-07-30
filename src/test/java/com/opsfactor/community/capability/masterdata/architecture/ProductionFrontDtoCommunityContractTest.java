package com.opsfactor.community.capability.masterdata.architecture;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.ListaTecnicaComponenteDTO;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.ListaTecnicaDTO;
import com.opsfactor.community.capability.masterdata.production.operation.facade.dto.OperacaoRoteiroDTO;
import com.opsfactor.community.capability.masterdata.production.routing.facade.dto.RoteiroDTO;
import com.opsfactor.community.capability.masterdata.production.productionresource.facade.dto.RecursoProdutivoDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrato Community dos DTOs REST de master data produtivo.
 *
 * <p>Estes DTOs alimentam telas/API de cadastro operacional usadas pelo
 * heuristico de Supply Planning. Campos de custo, setup detalhado, turnos,
 * manutencao, line scheduling ou versoes paralelas pertencem ao Enterprise e
 * nao devem voltar por copia de DTOs privados.</p>
 */
public class ProductionFrontDtoCommunityContractTest {

    @Test
    public void productionFrontDtosShouldExposeOnlyOperationalCommunityFields() {

        assertFieldNames(
                RecursoProdutivoDTO.class,
                Set.of(
                        "productionResourceId",
                        "locationId",
                        "description",
                        "active",
                        "efficiency"));
        assertFieldNames(
                RoteiroDTO.class,
                Set.of(
                        "id",
                        "description",
                        "priority",
                        "locationId",
                        "outputMaterialId",
                        "canBeUsedWithoutProductionVersion",
                        "active"));
        assertFieldNames(
                OperacaoRoteiroDTO.class,
                Set.of(
                        "routingId",
                        "operationPosition",
                        "productionResourceId",
                        "unitOfMeasureId",
                        "baseQuantity",
                        "hoursByBaseQuantity"));
        assertFieldNames(
                ListaTecnicaDTO.class,
                Set.of(
                        "id",
                        "description",
                        "outputMaterialId",
                        "outputUnitOfMeasureId",
                        "outputQuantity",
                        "active"));
        assertFieldNames(
                ListaTecnicaComponenteDTO.class,
                Set.of(
                        "billOfMaterialsId",
                        "componentMaterialId",
                        "componentMaterialUnitOfMeasureId",
                        "quantity"));

    }

    private static void assertFieldNames(Class<?> dtoClass, Set<String> expectedFieldNames) {

        Set<String> actualFieldNames = Arrays.stream(dtoClass.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(Field::getName)
                .collect(Collectors.toSet());

        Assertions.assertEquals(
                expectedFieldNames,
                actualFieldNames,
                dtoClass.getSimpleName() + " deve permanecer no contrato operacional Community");

    }

}
