package com.opsfactor.community.platform.projection.inmemorybi;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos basicos do BI em memoria usado pelas projections Community.
 */
public class BIEmMemoriaCommunityContractTest {

    @Test
    public void getChavesIndiceLongShouldUseLongIndex() {

        BIEmMemoria<TestItem> biEmMemoria = new BIEmMemoria<>(TestItem.class);
        biEmMemoria.addLongAttribute("idLongo", TestItem::getIdLongo, true);
        biEmMemoria.addElementoNoBI(new TestItem(42L, "A", new TestDimension("D1")));

        Assertions.assertEquals(Set.of(42L), biEmMemoria.getChavesIndiceLong("idLongo"));

    }

    @Test
    public void getChavesIndiceShouldFailAsInvalidBiStateWhenIndexWasNotCreated() {

        BIEmMemoria<TestItem> biEmMemoria = new BIEmMemoria<>(TestItem.class);
        biEmMemoria.addStringAttribute("codigo", TestItem::getCodigo, false);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> biEmMemoria.getChavesIndiceString("codigo"));

        Assertions.assertEquals(
                "Attribute codigo does not have a string index",
                illegalStateException.getMessage());

    }

    @Test
    public void getWhereEqualsStringUniqueIndexShouldFailAsInvalidBiStateWhenUniqueIndexWasNotCreated() {

        BIEmMemoria<TestItem> biEmMemoria = new BIEmMemoria<>(TestItem.class);
        biEmMemoria.addStringAttribute("codigo", TestItem::getCodigo, true);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> biEmMemoria.getWhereEqualsStringUniqueIndex("codigo", "A"));

        Assertions.assertEquals(
                "Attribute codigo does not have a string unique index",
                illegalStateException.getMessage());

    }

    @Test
    public void getChavesIndiceObjectShouldRejectIncompatibleClassAsInvalidArgument() {

        BIEmMemoria<TestItem> biEmMemoria = new BIEmMemoria<>(TestItem.class);
        biEmMemoria.addObjectAttribute("dimensao", TestDimension.class, TestItem::getTestDimension, true);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> biEmMemoria.getChavesIndiceObject("dimensao", String.class));

        Assertions.assertEquals(
                "Class String does not match object index class TestDimension",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getWhereEqualsShouldUseLocalDateTimeValueEquality() {

        LocalDateTime referenceDate = LocalDateTime.of(2026, 7, 31, 23, 59, 59);
        DateItem dateItem = new DateItem(referenceDate);
        BIEmMemoria<DateItem> biEmMemoria = new BIEmMemoria<>(DateItem.class);
        biEmMemoria.addLocalDateTimeAttribute(
                "dataReferencia",
                DateItem::referenceDate,
                true);
        biEmMemoria.addElementoNoBI(dateItem);

        Assertions.assertEquals(
                List.of(dateItem),
                biEmMemoria.getWhereEquals(
                                BIEmMemoria.FiltroDimensao.with(
                                        "dataReferencia",
                                        LocalDateTime.of(2026, 7, 31, 23, 59, 59)))
                        .stream()
                        .toList());

    }

    private static class TestItem {

        private final Long idLongo;
        private final String codigo;
        private final TestDimension testDimension;

        private TestItem(Long idLongo, String codigo, TestDimension testDimension) {

            this.idLongo = idLongo;
            this.codigo = codigo;
            this.testDimension = testDimension;

        }

        private Long getIdLongo() {

            return idLongo;

        }

        private String getCodigo() {

            return codigo;

        }

        private TestDimension getTestDimension() {

            return testDimension;

        }

    }

    private record DateItem(LocalDateTime referenceDate) {
    }

    private static class TestDimension {

        private final String id;

        private TestDimension(String id) {

            this.id = id;

        }

    }

}
