package com.opsfactor.community.web.restcontroller.configuration;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.PerfilExecucaoDemandPlanDTO;
import com.opsfactor.community.capability.configuration.facade.PerfilExecucaoDemandPlanFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * API de perfil de execucao Demand Planning Community.
 *
 * <p>O Community usa apenas sell-out como documento historico e bloqueia
 * agregacao MAPE configuravel, auto-fit, regression tree, sell-in e sales
 * orders. A service tambem fixa a persistencia para o subconjunto permitido
 * pelo recorte Community.</p>
 */
@Slf4j
@RestController
public class PerfilExecucaoDemandPlanController {

    /**
     * Fachada do perfil de execucao Demand Planning. A validacao de sell-out,
     * MAPE, auto-fit e regression tree fica centralizada neste service.
     */
    @Autowired
    private PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFrontService;

    /**
     * Salva perfil de execucao com o subconjunto Community permitido.
     */
    @PostMapping("api/secured/demandplanexecutionprofile")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> savePerfilExecucaoDemandPlan(@RequestBody PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO) {

        try {
            perfilExecucaoDemandPlanFrontService.savePerfilExecucaoDemandPlanDTO(perfilExecucaoDemandPlanDTO);
            return ResponseEntity.ok("Profile saved");
        } catch (RuntimeException e) {
            log.error("Error saving Demand Planning execution profile", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista perfis de execucao ja normalizados para o DTO Community.
     */
    @GetMapping("api/secured/demandplanexecutionprofile")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<PerfilExecucaoDemandPlanDTO>> getPerfilExecucaoDemandPlanList() {

        try {
            List<PerfilExecucaoDemandPlanDTO> perfilExecucaoDemandPlanDTOList = perfilExecucaoDemandPlanFrontService.getPerfilExecucaoDemandPlanDTOSet();
            return ResponseEntity.ok(perfilExecucaoDemandPlanDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing Demand Planning execution profiles", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

}
