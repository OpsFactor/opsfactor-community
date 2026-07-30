package com.opsfactor.community.web.restcontroller.configuration;

import com.opsfactor.community.capability.configuration.facade.dto.ParametroClusterLocationDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ParametrosMaterialDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ParametrosMaterialLocationDTO;
import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;
import com.opsfactor.community.capability.configuration.facade.ParametrosFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.annotation.Secured;

/**
 * Endpoints de configuracao simples de parametros operacionais.
 *
 * <p>No Community, a parte material-location desta API permite apenas manutencao
 * de informacoes basicas usadas no Planning Book e no Supply Planning
 * heuristico, como ativacao/inativacao, minimo e multiplo de producao.
 * Caracteristicas
 * material-location, filtros DFU, estruturas agregadoras, auto-fit, pricing e
 * qualquer semantica de otimizacao ficam fora deste controller.</p>
 *
 * <p>Os paths publicos usam a nomenclatura funcional de material.</p>
 */
@RestController
public class ParametroMaterialLocationController {

    /**
     * Fachada de parametros operacionais simples.
     *
     * <p>Este bean salva parametros de material, location e material/location
     * permitidos no Community. Filtros/agregadores, caracteristicas
     * material-location e otimizacao de politica permanecem fora desta borda.</p>
     */
    @Autowired
    private ParametrosFacade parametrosFrontService;

    /**
     * Lista parametros agregados por cluster de locations.
     */
    @GetMapping("api/secured/configs/parametros/clusterLocation")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ParametroClusterLocationDTO>> pullParametrosClusterLocation() {

        List<ParametroClusterLocationDTO> parametroClusterLocationDTOList = parametrosFrontService.getParametroClusterLocationDTO();
        if (parametroClusterLocationDTOList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(parametroClusterLocationDTOList);
        } else {
            return ResponseEntity.ok(parametroClusterLocationDTOList);
        }

    }

    @ResponseBody
    /**
     * Salva parametros agregados por cluster de locations.
     */
    @PostMapping("api/secured/configs/parametros/clusterLocation")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Boolean> pushParametrosClusterLocation(@RequestBody ParametroClusterLocationDTO parametroClusterLocationDTO) {

        if (parametrosFrontService.saveParametroClusterLocationDTO(parametroClusterLocationDTO)) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }

    }

    /**
     * Lista parametros de materiais usados nas configuracoes Community.
     */
    @GetMapping("api/secured/configs/parametros/material")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ParametrosMaterialDTO>> pullParametrosMaterial() {

        List<ParametrosMaterialDTO> parametrosMaterialDTOList = parametrosFrontService.getParametrosMaterialDTO();
        if (parametrosMaterialDTOList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(parametrosMaterialDTOList);
        }
        return ResponseEntity.ok(parametrosMaterialDTOList);

    }

    @ResponseBody
    /**
     * Salva parametros de material usados nas configuracoes Community.
     */
    @PostMapping("api/secured/configs/parametros/material")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Boolean> pushParametrosMaterial(@RequestBody ParametrosMaterialDTO parametrosMaterialDTO) {

        if (parametrosFrontService.saveParametrosMaterialDTO(parametrosMaterialDTO)) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }

    }

    /**
     * Lista parametros material-location para uma location especifica.
     */
    @GetMapping("api/secured/configs/parametros/materialLocation/{location}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ParametrosMaterialLocationDTO>> pullParametrosMaterialLocation(@PathVariable String location) {

        List<ParametrosMaterialLocationDTO> parametrosMaterialLocationDTOList;
        if (location == null || location.isEmpty()) {
            parametrosMaterialLocationDTOList = parametrosFrontService.getParametrosMaterialLocation();
        } else {
            parametrosMaterialLocationDTOList = parametrosFrontService.getParametrosMaterialLocation(location);
        }

        if (parametrosMaterialLocationDTOList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(parametrosMaterialLocationDTOList);
        }

        return ResponseEntity.ok(parametrosMaterialLocationDTOList);

    }

    @ResponseBody
    /**
     * Salva parametros material-location usados pelo planejamento Community.
     */
    @PostMapping("api/secured/configs/parametros/materialLocation")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Boolean> pushParametrosMaterialLocation(@RequestBody ParametrosMaterialLocationDTO parametrosMaterialLocationDTO) {

        /*
         * Rejeita configuracao invalida na borda HTTP antes de acionar a
         * service ou qualquer repository. A service repete a verificacao para
         * consumidores internos que nao passam por este controller.
         */
        if (parametrosMaterialLocationDTO != null
                && parametrosMaterialLocationDTO.getFrozenHorizonDpInDays() != null
                && parametrosMaterialLocationDTO.getFrozenHorizonDpInDays() < 0) {
            return ResponseEntity.badRequest().body(false);
        }
        if (parametrosMaterialLocationDTO != null
                && parametrosMaterialLocationDTO.getProductionMinimumQuantity() != null
                && (!Double.isFinite(parametrosMaterialLocationDTO.getProductionMinimumQuantity())
                || parametrosMaterialLocationDTO.getProductionMinimumQuantity() < 0.0d)) {
            return ResponseEntity.badRequest().body(false);
        }
        if (parametrosMaterialLocationDTO != null
                && parametrosMaterialLocationDTO.getProductionMultipleQuantity() != null
                && (!Double.isFinite(parametrosMaterialLocationDTO.getProductionMultipleQuantity())
                || parametrosMaterialLocationDTO.getProductionMultipleQuantity() <= 0.0d)) {
            return ResponseEntity.badRequest().body(false);
        }

        if (parametrosFrontService.saveParametrosMaterialLocationDTO(parametrosMaterialLocationDTO)) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }

    }

    /**
     * Lista locations disponiveis para configuracao de parametros.
     */
    @GetMapping("api/secured/configs/parametros/locationList")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<LocationDTO>> pullLocations() {

        List<LocationDTO> locationDTOList = parametrosFrontService.getLocationsDTO();
        if (locationDTOList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(locationDTOList);
        } else {
            return ResponseEntity.ok(locationDTOList);
        }

    }

}
