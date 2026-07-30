package com.opsfactor.community.web.restcontroller.configuration;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.facade.PoliticaEstoquesFacade;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.facade.dto.PoliticaEstoquesDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller Community para cadastro operacional de politicas de estoque.
 *
 * <p>Este endpoint existe para o Supply Planning heuristico resolver safety
 * stock por material/location. Ele nao representa o modulo Enterprise de
 * Inventory Policy Optimization: resultados de otimizacao, simulacoes de
 * politica, frequencia otimizada e dados economicos permanecem fora desta
 * superficie.</p>
 */
@Slf4j
@RestController
public class PoliticaEstoquesController {

    /**
     * Fachada Community de politicas operacionais de safety stock.
     *
     * <p>A injecao explicita reforca que este controller nao conhece runtime
     * Enterprise de otimizacao de politica de estoques; qualquer campo privado
     * que chegar no payload e bloqueado pela service.</p>
     */
    @Autowired
    private PoliticaEstoquesFacade politicaEstoquesFrontService;

    /**
     * Lista as politicas de estoque configuradas no Community.
     */
    @GetMapping("api/secured/configs/inventorypolicy")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<PoliticaEstoquesDTO>> getPoliticaEstoquesList() {

        try {
            return ResponseEntity.ok(politicaEstoquesFrontService.getPoliticaEstoquesDTOList());
        } catch (RuntimeException exception) {
            log.error("Error listing operational inventory policies", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        }

    }

    /**
     * Carrega uma politica de estoque pelo identificador funcional.
     */
    @GetMapping("api/secured/configs/inventorypolicy/{inventoryPolicyId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<PoliticaEstoquesDTO> getPoliticaEstoques(@PathVariable String inventoryPolicyId) {

        try {
            return ResponseEntity.ok(politicaEstoquesFrontService.getPoliticaEstoquesDTO(inventoryPolicyId));
        } catch (RuntimeException exception) {
            log.error("Error loading operational inventory policy {}", inventoryPolicyId, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        }

    }

    /**
     * Salva a politica de estoque recebida pela API Community.
     */
    @PostMapping("api/secured/configs/inventorypolicy")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> savePoliticaEstoques(@RequestBody PoliticaEstoquesDTO politicaEstoquesDTO) {

        try {
            politicaEstoquesFrontService.savePoliticaEstoquesDTO(politicaEstoquesDTO);
            return ResponseEntity.ok("Inventory policy saved");
        } catch (RuntimeException exception) {
            log.error("Error saving operational inventory policy", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        }

    }

    /**
     * Remove uma politica de estoque pelo identificador funcional.
     */
    @DeleteMapping("api/secured/configs/inventorypolicy/{inventoryPolicyId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> deletePoliticaEstoques(@PathVariable String inventoryPolicyId) {

        try {
            politicaEstoquesFrontService.deletePoliticaEstoques(inventoryPolicyId);
            return ResponseEntity.ok("Inventory policy removed");
        } catch (RuntimeException exception) {
            log.error("Error deleting operational inventory policy {}", inventoryPolicyId, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        }

    }
}
