package com.opsfactor.community.web.restcontroller.masterdata;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.VersaoMalhaDTO;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.LinhaTransporteFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Endpoints Community de cadastro de malha/transportation lanes.
 *
 * <p>O path historico usa `supplynetwork`, mas aqui ele representa somente a
 * malha operacional necessaria ao heuristico de Supply Planning: versao de
 * malha, origem, destino, material, prioridade e lead time. Supply Network
 * Flows visual, mapa, distancias geograficas, frete e analises de rede sao
 * capacidades Enterprise e permanecem bloqueadas nos DTOs/mappers.</p>
 */
@RestController
public class LinhaTransporteController {

    /**
     * Service front que concentra conversao e persistencia da malha simples
     * Community.
     */
    @Autowired
    private LinhaTransporteFacade linhaTransporteFrontService;

    /**
     * Lista transportation lanes origem/destino da versao de malha informada.
     */
    @GetMapping("api/secured/supplynetwork/transportationline/get/{versaoMalhaId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<LinhaTransporteIntegrationDataDto>> getLinhasTransporte(@PathVariable String versaoMalhaId) {

        try {
            return ResponseEntity.ok(linhaTransporteFrontService.getLinhaTransporteIntegrationDataDtoList(versaoMalhaId));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Cria ou atualiza uma transportation lane origem/destino.
     */
    @PostMapping("api/secured/supplynetwork/transportationline/update")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> updateLinhasTransporte(@RequestBody LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDTO) {

        try {
            linhaTransporteFrontService.saveLinhaTransporteIntegrationDataDto(linhaTransporteIntegrationDTO);
            return ResponseEntity.ok("Transportation Line Successfully Saved");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Remove transportation lanes origem/destino selecionadas pela tela.
     */
    @DeleteMapping("api/secured/supplynetwork/transportationline/delete")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> removeLinhasTransporte(@RequestBody List<LinhaTransporteIntegrationDataDto> linhaTransporteIntegrationDTOList) {

        try {
            linhaTransporteFrontService.removeLinhaTransporteIntegrationDataDtoList(linhaTransporteIntegrationDTOList);
            return ResponseEntity.ok("Transportation Line Successfully Removed");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista transportation lanes material-especificas da versao de malha.
     */
    @GetMapping("api/secured/supplynetwork/transportationlinematerial/get/{versaoMalhaId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<LinhaTransporteProdutoIntegrationDataDto>> getLinhasTransporteProduto(@PathVariable String versaoMalhaId) {

        try {
            return ResponseEntity.ok(linhaTransporteFrontService.getLinhaTransporteProdutoIntegrationDataDtoList(versaoMalhaId));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);   
        }

    }

    /**
     * Cria ou atualiza uma transportation lane por material.
     */
    @PostMapping("api/secured/supplynetwork/transportationlinematerial/update")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> updateLinhasTransporteProduto(@RequestBody LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDTO) {

        try {
            linhaTransporteFrontService.saveLinhaTransporteProdutoIntegrationDataDto(linhaTransporteProdutoIntegrationDTO);
            return ResponseEntity.ok("Transportation Line - Material Successfully Saved");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Remove transportation lanes material-especificas selecionadas pela tela.
     */
    @DeleteMapping("api/secured/supplynetwork/transportationlinematerial/delete")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> removeLinhasTransporteProduto(@RequestBody List<LinhaTransporteProdutoIntegrationDataDto> linhaTransporteProdutoIntegrationDTOList) {

        try {
            linhaTransporteFrontService.removeLinhaTransporteProdutoIntegrationDataDtoList(linhaTransporteProdutoIntegrationDTOList);
            return ResponseEntity.ok("Transportation Line - Material Selection Successfully Removed");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista versoes de malha Community.
     */
    @GetMapping("api/secured/supplynetwork/version")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<VersaoMalhaDTO>> getVersoesMalha() {

        try {
            return ResponseEntity.ok(linhaTransporteFrontService.getVersaoMalhaDTOList());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);   
        }

    }

    /**
     * Cria ou atualiza uma versao de malha.
     */
    @PostMapping("api/secured/supplynetwork/version")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> updateVersoesMalha(@RequestBody VersaoMalhaDTO versaoMalhaDTO) {

        try {
            linhaTransporteFrontService.saveVersaoMalhaDTO(versaoMalhaDTO);
            return ResponseEntity.ok("Supply Network Version Successfully Saved");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

}
