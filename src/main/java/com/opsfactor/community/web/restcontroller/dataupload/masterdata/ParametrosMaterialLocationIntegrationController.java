package com.opsfactor.community.web.restcontroller.dataupload.masterdata;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.configuration.integration.dto.ParametrosMaterialLocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationFiltroDto;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.configuration.integration.service.ParametrosMaterialLocationIntegrationService;
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

import java.util.List;

/**
 * Endpoints de carga dos parametros operacionais material-location no
 * Community.
 *
 * <p>Este controller publica apenas os campos simples aceitos pelo mapper
 * Community: ativacao, datas, UOM, lote minimo/multiplo e horizonte congelado
 * de DP. Frequencia de reabastecimento, caracteristicas material-location,
 * filtros DFU e estruturas de agregacao permanecem no Enterprise e sao
 * rejeitados antes da persistencia.</p>
 *
 */
@Slf4j
@Tags({
        @Tag(name = "Configuration"),
        @Tag(name = "Material / Location")
})
@RestController
public class ParametrosMaterialLocationIntegrationController extends IntegrationControllerAbstract<ParametrosProdutoLocation, ParametrosMaterialLocationIntegrationDataDto, ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO, LocationIntegrationFiltroDto, ParametrosMaterialLocationIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {
        return "materiallocationparameters";
    }

    /**
     * Parametros material-location publicam somente a raiz FILE canonica. A
     * abstracao continua com aliases apenas para controllers fora deste lote.
     */
    @Override
    public List<String> getRootFilePaths() {

        return List.of("api/secured/data/file/");

    }

    /**
     * Publica somente a raiz JSON canonica para evitar o registro dinamico de
     * dataupload/productlocationparameters.
     */
    @Override
    public List<String> getRootJsonPaths() {

        return List.of("api/secured/data/");

    }

    /**
     * Service concreto usado pelos endpoints de parametros material-location.
     */
    @Autowired
    private ParametrosMaterialLocationIntegrationService parametrosMaterialLocationIntegrationService;

    /**
     * Desativa parametros material-location pelo filtro de location aprovado no Community.
     */
    @PostMapping("api/secured/data/materiallocationparameters/deactivate")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> desativaParametrosMaterialLocation(@RequestBody LocationIntegrationFiltroDto filtroLocationsADesativar) {

        try {
            parametrosMaterialLocationIntegrationService.desativaParametrosMaterialLocationDeFiltroLocations(filtroLocationsADesativar);
            return ResponseEntity.ok("Filtered Locations Deactivated");
        } catch (RuntimeException e) {
            log.error("Error deactivating material-location parameters for filtered locations", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Remove todos os parametros material-location pelo endpoint operacional aprovado.
     */
    @DeleteMapping("api/secured/data/materiallocationparameters")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseDTO> deleteParametrosMaterialLocation() {

        return executaComandoPersonalizado(
                parametrosMaterialLocationIntegrationService -> parametrosMaterialLocationIntegrationService.removeAll(),
                "DeleteMaterialLocationParameters",
                "All Material-Location Properties were Deleted");

    }

}
