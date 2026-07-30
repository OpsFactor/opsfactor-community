package com.opsfactor.community.web.restcontroller.dataupload.masterdata;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationFiltroDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.integration.service.LocationIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Data upload Community de locations.
 *
 * <p>O layout aceita somente atributos operacionais usados por Demand
 * Planning, Supply Planning heuristico e Planning Book. Coordenadas
 * geograficas, UOM de expedicao, prazo de atendimento, caracteristicas
 * dinamicas, mapa/GIS, warehouses e last-mile sao Enterprise e sao rejeitados
 * no mapper quando aparecem preenchidos.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Location")
})
@RestController
public class LocationIntegrationController extends IntegrationControllerAbstract<Location, LocationIntegrationDataDto, LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO, LocationIntegrationFiltroDto, LocationIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {
        return "location";
    }

    /**
     * Service de integracao de locations usado pelo endpoint manual de
     * desativacao por filtro. As rotas FILE/JSON padrao continuam herdadas de
     * {@link IntegrationControllerAbstract}.
     */
    @Autowired
    private LocationIntegrationService locationIntegrationService;

    /**
     * Desativa locations do filtro informado sem expor recursos Enterprise de
     * mapa/GIS, warehouses ou last-mile.
     */
    @PostMapping("api/secured/data/location/deactivate")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> desativaLocations(@RequestBody(required = false) LocationIntegrationFiltroDto filtroLocationsADesativar) {

        try {
            locationIntegrationService.desativaLocationsDeFiltro(filtroLocationsADesativar);
            return ResponseEntity.ok("Filtered Locations Deactivated");
        } catch (RuntimeException e) {
            log.error("Error deactivating filtered locations", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

}
