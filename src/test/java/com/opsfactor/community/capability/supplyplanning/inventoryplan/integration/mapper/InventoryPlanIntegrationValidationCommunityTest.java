package com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.mapper;

import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * Contrato dos validators Community de Inventory Plan.
 *
 * <p>Ausencia operacional de quantidade no template de Inventory Plan deve
 * permanecer como {@code null}. A anotacao explicita evita que consumers
 * futuros tratem ausencia opcional como erro de cadastro.</p>
 */
public class InventoryPlanIntegrationValidationCommunityTest {

    @Test
    public void numericValidatorsShouldDeclareNullableQuantityContract() throws Exception {

        assertNullableQuantityContract("validaNumeroFinitoOuNulo");
        assertNullableQuantityContract("validaNumeroNaoNegativoOuNulo");

        Assertions.assertNull(
                InventoryPlanIntegrationValidation.validaNumeroFinitoOuNulo(
                        null,
                        "projected stock working version"));
        Assertions.assertNull(
                InventoryPlanIntegrationValidation.validaNumeroNaoNegativoOuNulo(
                        null,
                        "safety stock quantity"));

    }

    private static void assertNullableQuantityContract(
            String methodName) throws NoSuchMethodException {

        Method validationMethod = InventoryPlanIntegrationValidation.class.getDeclaredMethod(
                methodName,
                Double.class,
                String.class);

        Assertions.assertTrue(
                validationMethod.isAnnotationPresent(Nullable.class),
                methodName + " deve declarar retorno @Nullable.");
        Assertions.assertTrue(
                validationMethod.getParameters()[0].isAnnotationPresent(Nullable.class),
                methodName + " deve declarar quantidade de entrada @Nullable.");

    }

}
