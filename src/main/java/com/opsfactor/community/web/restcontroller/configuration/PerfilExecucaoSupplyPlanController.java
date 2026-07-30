package com.opsfactor.community.web.restcontroller.configuration;

import com.opsfactor.community.capability.supplyplanning.configuration.facade.dto.PerfilExecucaoSupplyPlanDTO;
import com.opsfactor.community.capability.configuration.facade.PerfilExecucaoSupplyPlanFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * API de perfil de execucao Supply Planning Community.
 *
 * <p>A service desta borda restringe o perfil ao motor heuristico e bloqueia
 * configuracoes Enterprise como optimizer/process chain, custos, P&L,
 * priorizacao, line scheduling, frotas, filtros de material, pedidos
 * transacionais e restricoes logisticas.</p>
 */
@Slf4j
@RestController
public class PerfilExecucaoSupplyPlanController {

    /**
     * Fachada Community que valida o DTO compartilhado com o front e bloqueia
     * qualquer configuracao Enterprise antes de persistir o perfil.
     */
    @Autowired
    private PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFrontService;

    /**
     * Salva perfil de execucao Supply Planning Community.
     *
     * <p>O DTO pode conter campos Enterprise por compatibilidade de contrato,
     * mas o service falha explicitamente se algum deles estiver habilitado ou
     * preenchido.</p>
     */
    @PostMapping("api/secured/supplyplanexecutionprofile")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> postPerfilExecucaoSupplyPlan(@RequestBody PerfilExecucaoSupplyPlanDTO perfilExecucaoSupplyPlanDTO) {

        try {
            perfilExecucaoSupplyPlanFrontService.savePerfilExecucaoSupplyPlanDTO(perfilExecucaoSupplyPlanDTO);
            return ResponseEntity.ok("Profile saved");
        } catch (RuntimeException e) {
            log.error("Error saving Supply Planning execution profile", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista os perfis de execucao Supply Planning ja persistidos no recorte
     * Community.
     */
    @GetMapping("api/secured/supplyplanexecutionprofile")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Set<PerfilExecucaoSupplyPlanDTO>> getPerfilExecucaoSupplyPlanList() {

        try {
            Set<PerfilExecucaoSupplyPlanDTO> perfilExecucaoSupplyPlanDTOList = perfilExecucaoSupplyPlanFrontService.getPerfilExecucaoSupplyPlanDTOSet();
            return ResponseEntity.ok(perfilExecucaoSupplyPlanDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing Supply Planning execution profiles", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

}
