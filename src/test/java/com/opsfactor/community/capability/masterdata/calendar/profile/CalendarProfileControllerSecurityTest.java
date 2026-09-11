package com.opsfactor.community.capability.masterdata.calendar.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.opsfactor.community.capability.masterdata.calendar.profile.facade.PerfilCalendarioController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.annotation.Secured;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Mantém o catálogo novo sob a mesma proteção dos cadastros operacionais. */
class CalendarProfileControllerSecurityTest {

    @Test
    void leituraEGravacaoExigemAdministradorNasDuasEdicoes() throws Exception {

        for (var method : new Method[]{
                PerfilCalendarioController.class.getMethod("listar", boolean.class),
                PerfilCalendarioController.class.getMethod("salvar", JsonNode.class)}) {
            Secured secured = method.getAnnotation(Secured.class);
            assertNotNull(secured, "O endpoint deve declarar sua autorização explicitamente");
            assertArrayEquals(new String[]{"ROLE_ADMIN"}, secured.value());
        }

    }

}
