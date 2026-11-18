package com.opsfactor.community.platform.database.converter;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Profile selection must never turn malformed persisted data into an implicit unrestricted selection. */
class StringSetJsonConverterTest {

    private final StringSetJsonConverter converter = new StringSetJsonConverter();

    @Test
    void preservesExplicitIdsAndSerializesDeterministically() {

        assertEquals("[\"A\",\"B\"]", converter.convertToDatabaseColumn(Set.of("B", "A")));
        assertEquals(Set.of("A", "B"), converter.convertToEntityAttribute("[\"B\",\"A\"]"));
        assertEquals(Set.of(), converter.convertToEntityAttribute(null));
        assertEquals("[]", converter.convertToDatabaseColumn(Set.of()));

    }

    @Test
    void rejectsInvalidAndDuplicateIds() {

        for (String json : new String[] {"null", "{}", "[1]", "[\"\"]", "[\"A\",\"A\"]", "invalid"}) {
            assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute(json));
        }
        assertThrows(IllegalArgumentException.class, () -> converter.convertToDatabaseColumn(Set.of(" ")));

    }

}
