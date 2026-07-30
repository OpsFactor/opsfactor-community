package com.opsfactor.community.web.restcontroller.planning;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningClusterLevelConfigurationDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningPreviaForecastRequestDTO;
import com.opsfactor.community.capability.demandplanning.facade.dto.SimulatedDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.facade.DemandSimulationFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller Community para configuracao e simulacao de forecast por cluster.
 *
 * <p>O nome historico "Demand Analysis" nao significa Demand Accuracy,
 * Auto-fit, analytics Enterprise ou Change Log. No Community esta superficie apenas
 * carrega parametros cluster-level de Demand Planning, simula forecast com os
 * modelos estatisticos permitidos e salva a configuracao operacional.</p>
 */
@Slf4j
@RestController
public class DemandAnalysisRestController {

    /**
     * Service de configuracao/simulacao cluster-level. Ele valida campos
     * Enterprise antes de repositories e devolve simulacao somente
     * material/location.
     */
    @Autowired
    private DemandSimulationFacade demandSimulationFrontService;

    /**
     * Busca parametros cluster-level existentes ou defaults para a simulacao.
     */
    @GetMapping("api/secured/demandPlanConfiguration/get/{executionProfileId}/{locationClusterId}/{materialClusterId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<DemandPlanningClusterLevelConfigurationDTO> getDemandPlanParameters(
            @PathVariable("executionProfileId") String executionProfileId,
            @PathVariable("locationClusterId") Long locationClusterId,
            @PathVariable("materialClusterId") Long materialClusterId) {
        
        try {
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO = demandSimulationFrontService.getDemandPlanningConfigurationDTO(
                    executionProfileId, locationClusterId, materialClusterId);
            return ResponseEntity.ok(demandPlanningClusterLevelConfigurationDTO);
        } catch (RuntimeException e) {
            log.error("Error loading Demand Planning cluster-level configuration", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Executa uma simulacao de forecast Community com os modelos estatisticos e
     * split liberados para a edicao aberta.
     */
    @PostMapping("api/secured/demandPlanConfiguration/simulate")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<SimulatedDemandPlanDTO> getSimulatedDemandPlan(
            @RequestBody DemandPlanningPreviaForecastRequestDTO demandPlanningPreviaForecastRequestDTO) {
        
        try {
            SimulatedDemandPlanDTO simulatedDemandPlanDTO = demandSimulationFrontService.getSimulatedDemandPlanDTO(
                    demandPlanningPreviaForecastRequestDTO);
            return ResponseEntity.ok(simulatedDemandPlanDTO);
        } catch (RuntimeException e) {
            log.error("Error simulating Demand Planning forecast", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Persiste parametros cluster-level apos validacao de recorte Community.
     */
    @PostMapping("api/secured/demandPlanConfiguration/save")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> saveDemandPlanParameters(
            @RequestBody DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {
        
        try {
            demandSimulationFrontService.saveParametrosDemandPlanning(demandPlanningClusterLevelConfigurationDTO);
            return ResponseEntity.ok("Demand Plan Parameters Saved");
        } catch (RuntimeException e) {
            log.error("Error saving Demand Planning cluster-level configuration", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
        
    }

}
