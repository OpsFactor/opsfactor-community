package com.opsfactor.community.web.restcontroller.dataupload.masterdata.malha;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service.LinhaTransporteProdutoIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Data upload Community de transportation lane por material.
 *
 * <p>Este controller carrega apenas a malha operacional usada pelo heuristico:
 * versao de malha, origem/destino, material, lead time e prioridade. Distancia,
 * mapa, frete, Supply Network Flows e analises visuais/economicas da rede
 * pertencem ao Enterprise e sao bloqueados nos DTOs/mappers.</p>
 */
@Slf4j
@RestController
public class LinhaTransporteMaterialIntegrationController extends IntegrationControllerAbstract<LinhaTransporteProduto, LinhaTransporteProdutoIntegrationDataDto, LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, LinhaTransporteProdutoIntegrationService, LinhaTransporteProdutoIntegrationOptionsDto> {

    @Override
    protected String getSubPath() {
        return "transportationlanematerial";
    }

    /**
     * A malha por material publica somente a raiz FILE canonica. Outros
     * controllers ainda em migracao preservam os aliases na abstracao base.
     */
    @Override
    public List<String> getRootFilePaths() {

        return List.of("api/secured/data/file/");

    }

    /**
     * A raiz JSON canonica impede o registro dinamico de dataupload para a
     * malha por material.
     */
    @Override
    public List<String> getRootJsonPaths() {

        return List.of("api/secured/data/");

    }

    /**
     * Service concreto usado pelos endpoints de malha material-especifica.
     */
    @Autowired
    private LinhaTransporteProdutoIntegrationService linhaTransporteMaterialIntegrationService;



    /**
     * Desativa associacoes material-linha de transporte pelo filtro Community.
     */
    @PostMapping("api/secured/data/transportationlanematerial/deactivate")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> desativaLinhasTransporteMaterial(@RequestBody LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoDeactivationFilterIntegrationDTO filtroLinhasTransporteMaterialADesativar) {

        try {
            linhaTransporteMaterialIntegrationService.desativaLinhaTransporteProdutoDeFiltro(filtroLinhasTransporteMaterialADesativar);
            return ResponseEntity.ok("Filtered Transportation Lane / Materials Deactivated");
        } catch (RuntimeException e) {
            log.error("Error deactivating filtered Transportation Lane / Materials", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

}

