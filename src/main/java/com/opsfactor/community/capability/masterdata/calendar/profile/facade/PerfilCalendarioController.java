package com.opsfactor.community.capability.masterdata.calendar.profile.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.opsfactor.community.capability.masterdata.calendar.profile.dto.PerfilCalendarioDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoint de UI; em Enterprise a facade primária fornece o contrato estendido. */
@RestController
@RequestMapping("/api/secured/calendar-profiles")
public class PerfilCalendarioController {

    private final PerfilCalendarioFacade facade;

    /**
     * Recebe a facade selecionada pelo Spring, incluindo a extensão primária Enterprise.
     */
    @Autowired
    public PerfilCalendarioController(PerfilCalendarioFacade facade) {

        this.facade = facade;

    }

    /** Lista o catálogo em lote, restringindo Demand aos perfis simples quando solicitado. */
    @GetMapping
    @Secured("ROLE_ADMIN")
    public List<PerfilCalendarioDTO> listar(@RequestParam(defaultValue = "false") boolean simpleOnly) {

        return facade.listar(simpleOnly);

    }

    /** Cria ou atualiza a receita validada pela edição, preservando as cópias históricas dos planos. */
    @PostMapping
    @Secured("ROLE_ADMIN")
    public PerfilCalendarioDTO salvar(@RequestBody JsonNode payload) {

        return facade.salvar(payload);

    }

}
