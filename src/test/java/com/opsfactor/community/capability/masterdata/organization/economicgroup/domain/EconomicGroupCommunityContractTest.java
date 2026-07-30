package com.opsfactor.community.capability.masterdata.organization.economicgroup.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Contrato do cabeçalho Community de grupo econômico e da sua referência em
 * location.
 *
 * <p>O recorte preserva uma única FK no aggregate operacional. O grupo não
 * mantém coleção inversa: consumidores Enterprise fazem a leitura em lote
 * necessária para cada projection, sem transformar o cabeçalho em novo ponto
 * de carregamento lazy.</p>
 */
class EconomicGroupCommunityContractTest {

    @Test
    void economicGroupShouldBeACommunityEntityWithOnlyItsSharedHeader() throws NoSuchFieldException {

        Assertions.assertTrue(EconomicGroup.class.isAnnotationPresent(Entity.class));

        Field idField = EconomicGroup.class.getDeclaredField("id");
        Field descriptionField = EconomicGroup.class.getDeclaredField("description");
        Column idColumn = idField.getAnnotation(Column.class);

        Assertions.assertTrue(idField.isAnnotationPresent(Id.class));
        Assertions.assertNotNull(idColumn);
        Assertions.assertEquals(100, idColumn.length());
        Assertions.assertEquals(String.class, descriptionField.getType());
        Assertions.assertEquals(
                2,
                EconomicGroup.class.getDeclaredFields().length,
                "O header Community nao deve receber colecoes ou dados fiscais Enterprise.");

    }

    @Test
    void locationShouldKeepOnlyLazyUnidirectionalEconomicGroupForeignKey() throws ReflectiveOperationException {

        Field economicGroupField = Location.class.getDeclaredField("economicGroup");
        ManyToOne manyToOne = economicGroupField.getAnnotation(ManyToOne.class);
        Method getter = Location.class.getMethod("getEconomicGroup");
        Method setter = Location.class.getMethod("setEconomicGroup", EconomicGroup.class);

        Assertions.assertEquals(EconomicGroup.class, economicGroupField.getType());
        Assertions.assertNotNull(manyToOne);
        Assertions.assertEquals(FetchType.LAZY, manyToOne.fetch());
        Assertions.assertNull(
                economicGroupField.getAnnotation(JoinColumn.class),
                "A coluna deve seguir a convencao limpa do Hibernate, sem nome legado explicito.");
        Assertions.assertEquals(EconomicGroup.class, getter.getReturnType());
        Assertions.assertEquals(void.class, setter.getReturnType());
        Assertions.assertFalse(
                Arrays.stream(EconomicGroup.class.getDeclaredFields())
                        .anyMatch(field -> field.isAnnotationPresent(OneToMany.class)),
                "EconomicGroup nao deve reintroduzir navegacao inversa para Location.");

    }

}
