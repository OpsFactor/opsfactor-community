package com.opsfactor.community.web.restcontroller.masterdata;

import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;
import com.opsfactor.community.capability.cluster.facade.dto.ClusterLocationsDTO;
import com.opsfactor.community.capability.cluster.facade.ClusterLocationDtoService;
import com.opsfactor.community.capability.masterdata.network.location.facade.LocationDtoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * API Community de locations e clusters de location para telas operacionais.
 *
 * <p>Este controller expõe listas simples usadas por cadastro, Demand Planning,
 * Supply Planning heuristico e Planning Book. Visualizacao em mapa, GIS,
 * last-mile, warehouses e filtros/agregadores dinamicos de location pertencem
 * ao Enterprise e nao sao publicados aqui.</p>
 */
@Slf4j
@RestController
public class LocationRestController {

    /**
     * Service DTO de clusters de locations usados por Demand Planning,
     * Planning Book e Supply Planning heuristico.
     */
    @Autowired
    private ClusterLocationDtoService clusterLocationDtoService;

    /**
     * Service DTO de locations operacionais. Recursos Enterprise como mapa,
     * warehouses e filtros dinamicos nao sao expostos por esta fachada.
     */
    @Autowired
    private LocationDtoService locationFrontService;

    /**
     * Lista todas as locations publicadas para o front Community.
     */
    @GetMapping("api/secured/location")
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public List<LocationDTO> getLocationDTOList() {

        return locationFrontService.getListaLocationDTO();

    }

    /**
     * Lista locations internas usadas pelos fluxos de planejamento.
     */
    @GetMapping("api/secured/location/internal")
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public List<LocationDTO> getLocationsInternasDTOList() {

        return locationFrontService.getListaLocationDTODeLocationsInternas();

    }

    /**
     * Lista locations internas e fornecedores aceitos nos fluxos Community.
     */
    @GetMapping("api/secured/location/internalandsupplier")
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public List<LocationDTO> getLocationsInternasEFornecedoresDTOList() {

        return locationFrontService.getListaLocationDTODeLocationsInternasEFornecedores();

    }

    /**
     * Salva uma location pelo contrato administrativo Community.
     */
    @PostMapping("api/secured/location")
    @Secured("ROLE_ADMIN")
    public void saveLocation(@RequestBody LocationDTO locationDTO) {

        locationFrontService.saveLocationDTO(locationDTO);

    }

    /**
     * Carrega uma location pelo identificador funcional.
     */
    @GetMapping("api/secured/location/{id}")
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public LocationDTO getOne(@PathVariable String id) {

        return locationFrontService.getListaLocationDTO().stream()
                .filter(x -> x.id.equals(id))
                .findAny().orElse(null);

    }

    /**
     * Lista clusters de locations disponiveis para filtros e parametros.
     */
    @GetMapping("api/secured/location/cluster")
    @Secured("ROLE_ADMIN")
    public List<ClusterLocationsDTO> clusterLocationsDTOList() {

        return clusterLocationDtoService.getListaClusterLocationDTO();

    }

    /**
     * Lista locations pertencentes a um cluster de locations.
     */
    @GetMapping("api/secured/location/cluster/{clusterLocationsId}/locations")
    @Secured("ROLE_ADMIN")
    public List<LocationDTO> getLocationDTOListFromClusterLocationsId(
            @PathVariable("clusterLocationsId") Long clusterLocationsId) {

        return locationFrontService.getLocationsDeClusterLocationsId(clusterLocationsId);

    }


    /**
     * Lista locations elegiveis ao Planning Book de Supply Planning.
     */
    @GetMapping("api/secured/location/supplyplanning")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<LocationDTO>> getLocationsParaSupplyPlanningBook() {

        try {
            List<LocationDTO> locationDTOList = locationFrontService.getLocationDTOListParaPlanningBookSupply();
            return ResponseEntity.ok(locationDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing locations for Supply Planning Book", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }
}
