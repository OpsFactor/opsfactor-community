package com.opsfactor.community.capability.supplyplanning.supplyplan.domain;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.optimizer.presetconstraint.RestricaoPredefinidaGrupo;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato da referência unidirecional de preset constraints no Supply Plan.
 *
 * <p>O Community conserva a FK para o header compartilhado, mas não mantém a
 * ponte transitória genérica nem navega para regras privadas do optimizer.</p>
 */
class SupplyPlanPresetConstraintCommunityContractTest {

    @Test
    void supplyPlanShouldPersistALazyUnidirectionalReferenceToTheSharedHeader() throws Exception {

        Field presetConstraintGroupField = SupplyPlan.class.getDeclaredField("presetConstraintGroup");
        ManyToOne manyToOne = presetConstraintGroupField.getAnnotation(ManyToOne.class);

        Assertions.assertEquals(RestricaoPredefinidaGrupo.class, presetConstraintGroupField.getType());
        Assertions.assertNotNull(manyToOne);
        Assertions.assertEquals(FetchType.LAZY, manyToOne.fetch());
        Assertions.assertNull(presetConstraintGroupField.getAnnotation(JoinColumn.class));

        RestricaoPredefinidaGrupo presetConstraintGroup = new RestricaoPredefinidaGrupo();
        presetConstraintGroup.setId("PRESET-GROUP");
        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setPresetConstraintGroup(presetConstraintGroup);

        Assertions.assertSame(presetConstraintGroup, supplyPlan.getPresetConstraintGroup());

    }

    @Test
    void supplyPlanShouldNotKeepTheGenericTransientEnterpriseBridge() {

        Assertions.assertFalse(Arrays.stream(SupplyPlan.class.getDeclaredFields())
                .anyMatch(field -> Object.class.equals(field.getType())
                        || field.getName().equals("restricaoPredefinidaGrupo")));
        Assertions.assertFalse(Arrays.stream(SupplyPlan.class.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(methodName -> methodName.equals("getRestricaoPredefinidaGrupo")
                        || methodName.equals("setRestricaoPredefinidaGrupo")));

    }

}
