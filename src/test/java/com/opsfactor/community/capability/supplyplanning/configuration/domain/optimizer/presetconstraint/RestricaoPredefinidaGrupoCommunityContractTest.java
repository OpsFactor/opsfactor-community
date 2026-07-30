package com.opsfactor.community.capability.supplyplanning.configuration.domain.optimizer.presetconstraint;

import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato do cabeçalho compartilhado de preset constraints no Community.
 *
 * <p>O header é persistido para manter a FK única do Supply Plan. As regras e
 * seus filhos são recursos Enterprise e não podem voltar como coleções
 * inversas no aggregate aberto.</p>
 */
class RestricaoPredefinidaGrupoCommunityContractTest {

    @Test
    void sharedHeaderShouldPersistOnlyItsIdentityAndDescription() throws Exception {

        Field idField = RestricaoPredefinidaGrupo.class.getDeclaredField("id");
        Field descriptionField = RestricaoPredefinidaGrupo.class.getDeclaredField("description");

        Assertions.assertNotNull(RestricaoPredefinidaGrupo.class.getAnnotation(Entity.class));
        Assertions.assertEquals(String.class, idField.getType());
        Assertions.assertNotNull(idField.getAnnotation(Id.class));
        Assertions.assertNull(idField.getAnnotation(Transient.class));
        Assertions.assertEquals(String.class, descriptionField.getType());
        Assertions.assertNull(descriptionField.getAnnotation(Transient.class));

        RestricaoPredefinidaGrupo presetConstraintGroup = new RestricaoPredefinidaGrupo();
        presetConstraintGroup.setId("PRESET-GROUP");
        presetConstraintGroup.setDescription("Shared preset constraint group");

        Assertions.assertEquals("PRESET-GROUP", presetConstraintGroup.getId());
        Assertions.assertEquals(
                "Shared preset constraint group",
                presetConstraintGroup.getDescription());

    }

    @Test
    void sharedHeaderShouldNotExposeEnterpriseChildrenOrReverseSupplyPlans() {

        Assertions.assertFalse(Arrays.stream(RestricaoPredefinidaGrupo.class.getDeclaredFields())
                .anyMatch(field -> field.getAnnotation(OneToMany.class) != null
                        || field.getAnnotation(ManyToMany.class) != null
                        || Collection.class.isAssignableFrom(field.getType())
                        || Map.class.isAssignableFrom(field.getType())
                        || SupplyPlan.class.equals(field.getType())
                        || field.getType().getPackageName().startsWith("com.opsfactor.enterprise")));

    }

}
