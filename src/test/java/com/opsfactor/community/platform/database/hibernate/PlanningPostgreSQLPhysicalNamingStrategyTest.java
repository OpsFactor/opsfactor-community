package com.opsfactor.community.platform.database.hibernate;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica nomes físicos sem abrir conexão ou modificar o schema de uma base. */
class PlanningPostgreSQLPhysicalNamingStrategyTest {

    private final PlanningPostgreSQLPhysicalNamingStrategy strategy = new PlanningPostgreSQLPhysicalNamingStrategy();

    @Test
    void matchesExistingTruncatedColumnInsteadOfAddingItAgain() {

        String fullName = "custo_impostos_plano_demanda_original_propagada_location_interna";
        String storedName = "custo_impostos_plano_demanda_original_propagada_location_intern";
        Identifier physicalName = strategy.toPhysicalColumnName(new Identifier(fullName, true), null);

        assertEquals(storedName, physicalName.getText());
        assertEquals(63, physicalName.getText().getBytes(StandardCharsets.UTF_8).length);
        assertTrue(physicalName.isQuoted());
        assertEquals(physicalName, strategy.toPhysicalColumnName(physicalName, null));

    }

    @Test
    void preservesExistingCamelCaseUnderscoreAndQuoteConventions() {

        var defaultStrategy = new CamelCaseToUnderscoresNamingStrategy();
        for (boolean quoted : new boolean[]{false, true}) {
            for (String name : new String[]{"perfilCalendario", "SupplyPlan", "user", "schema.Table", "açãoPlanejada"}) {
                Identifier logicalName = new Identifier(name, quoted);
                Identifier expected = defaultStrategy.toPhysicalColumnName(logicalName, null);
                Identifier actual = strategy.toPhysicalColumnName(logicalName, null);
                assertEquals(expected.getText(), actual.getText());
                assertEquals(expected.isQuoted(), actual.isQuoted());
            }
        }

    }

    @Test
    void truncatesAfterCamelCaseExpansion() {

        Identifier physicalName = strategy.toPhysicalColumnName(
                new Identifier("a".repeat(60) + "Beta", false), null);

        assertEquals("a".repeat(60) + "_be", physicalName.getText());
        assertFalse(physicalName.isQuoted());

    }

    @Test
    void leavesExactly63BytesUnchanged() {

        for (String name : new String[]{"a".repeat(63), "a".repeat(61) + "é"}) {
            assertEquals(name, strategy.toPhysicalColumnName(new Identifier(name, false), null).getText());
        }

    }

    @Test
    void neverSplitsUtf8CharactersAtTheBoundary() {

        for (String character : new String[]{"é", "界", "😀"}) {
            int characterBytes = character.getBytes(StandardCharsets.UTF_8).length;
            for (int overflowingBytes = 1; overflowingBytes < characterBytes; overflowingBytes++) {
                String prefix = "a".repeat(63 - overflowingBytes);
                Identifier physicalName = strategy.toPhysicalColumnName(
                        new Identifier(prefix + character + "z", true), null);
                assertEquals(prefix, physicalName.getText());
                assertTrue(physicalName.isQuoted());
            }
        }

    }

    @Test
    void keepsWholeMultibyteCharacterThatEndsAtTheLimit() {

        for (String character : new String[]{"é", "界", "😀"}) {
            String prefix = "a".repeat(63 - character.getBytes(StandardCharsets.UTF_8).length) + character;
            Identifier physicalName = strategy.toPhysicalColumnName(new Identifier(prefix + "z", false), null);
            assertEquals(prefix, physicalName.getText());
        }

    }

    @Test
    void appliesTheSameServerLimitToAllPhysicalIdentifierKinds() {

        Identifier logicalName = new Identifier("a".repeat(64), true);
        for (Identifier physicalName : new Identifier[]{
                strategy.toPhysicalCatalogName(logicalName, null),
                strategy.toPhysicalSchemaName(logicalName, null),
                strategy.toPhysicalTableName(logicalName, null),
                strategy.toPhysicalSequenceName(logicalName, null),
                strategy.toPhysicalColumnName(logicalName, null)}) {
            assertEquals("a".repeat(63), physicalName.getText());
            assertTrue(physicalName.isQuoted());
        }

    }

    @Test
    void preservesNullOptionalIdentifiers() {

        assertNull(strategy.toPhysicalCatalogName(null, null));
        assertNull(strategy.toPhysicalSchemaName(null, null));
        assertNull(strategy.toPhysicalTableName(null, null));
        assertNull(strategy.toPhysicalSequenceName(null, null));
        assertNull(strategy.toPhysicalColumnName(null, null));

    }
}
