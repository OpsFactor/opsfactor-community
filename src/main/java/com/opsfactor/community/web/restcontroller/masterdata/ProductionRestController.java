package com.opsfactor.community.web.restcontroller.masterdata;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.ListaTecnicaComponenteDTO;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.ListaTecnicaDTO;
import com.opsfactor.community.capability.masterdata.production.operation.facade.dto.OperacaoRoteiroDTO;
import com.opsfactor.community.capability.masterdata.production.routing.facade.dto.RoteiroDTO;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.InconsistenciaReceitaProducaoDTO;
import com.opsfactor.community.capability.masterdata.production.productionresource.facade.dto.RecursoProdutivoDTO;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.ListaTecnicaFacade;
import com.opsfactor.community.capability.masterdata.production.productionresource.facade.RecursoProdutivoFacade;
import com.opsfactor.community.capability.masterdata.production.routing.facade.RoteiroFacade;
import java.util.List;
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

/**
 * Superficie REST de master data produtivo operacional do Community.
 *
 * <p>Estes endpoints atendem o cadastro basico usado pelo heuristico de Supply
 * Planning: roteiros, operacoes, listas tecnicas e recursos produtivos. Line
 * scheduling, turnos, manutencao, setup detalhado, custos de recurso e versoes
 * paralelas pertencem ao OpsFactor Enterprise.</p>
 */
@Slf4j
@RestController
public class ProductionRestController {

    /**
     * Service front de roteiros e operacoes produtivas Community.
     */
    @Autowired
    private RoteiroFacade roteiroFrontService;

    /**
     * Service front de BOMs e componentes Community.
     */
    @Autowired
    private ListaTecnicaFacade listaTecnicaFrontService;

    /**
     * Service front de recursos produtivos Community.
     */
    @Autowired
    private RecursoProdutivoFacade recursoProdutivoFrontService;

    /**
     * Lista roteiros produtivos operacionais.
     */
    @GetMapping("api/secured/production/routing")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<RoteiroDTO>> getRoteiroDTOList() {

        try {
            return ResponseEntity.ok(roteiroFrontService.getRoteiroDTOList());
        } catch (RuntimeException e) {
            log.error("Error listing routings", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista operacoes dos roteiros produtivos.
     */
    @GetMapping("api/secured/production/routingoperation")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<OperacaoRoteiroDTO>> getOperacaoRoteiroDTOList() {

        try {
            return ResponseEntity.ok(roteiroFrontService.getOperacaoRoteiroDTOList());
        } catch (RuntimeException e) {
            log.error("Error listing routing operations", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista BOMs operacionais.
     */
    @GetMapping("api/secured/production/billofmaterials")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ListaTecnicaDTO>> getListaTecnicaDTOList() {

        try {
            return ResponseEntity.ok(listaTecnicaFrontService.getListaTecnicaDTOList());
        } catch (RuntimeException e) {
            log.error("Error listing bills of materials", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista componentes de BOM.
     */
    @GetMapping("api/secured/production/billofmaterialscomponents")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ListaTecnicaComponenteDTO>> getListaTecnicaComponenteDTOList() {

        try {
            return ResponseEntity.ok(listaTecnicaFrontService.getListaTecnicaComponenteDTOList());
        } catch (RuntimeException e) {
            log.error("Error listing bill of materials components", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista inconsistencias simples de roteiro/BOM expostas ao Community.
     */
    @GetMapping("api/secured/production/routing/inconsistencies")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<InconsistenciaReceitaProducaoDTO>> getInconsistenciaRoteiroDTOList() {

        try {
            return ResponseEntity.ok(roteiroFrontService.getInconsistenciaReceitaProducaoDTOList());
        } catch (RuntimeException e) {
            log.error("Error listing routing inconsistencies", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista recursos produtivos.
     */
    @GetMapping("api/secured/production/productionresource")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<RecursoProdutivoDTO>> getRecursoProdutivoDTOList() {

        try {
            return ResponseEntity.ok(recursoProdutivoFrontService.getRecursoProdutivoDTOList());
        } catch (RuntimeException e) {
            log.error("Error listing production resources", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Cria ou atualiza recurso produtivo pelo contrato operacional Community.
     */
    @PostMapping("api/secured/production/productionresource/save")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> saveRecursoProdutivoDTO(@RequestBody RecursoProdutivoDTO recursoProdutivoDTO) {

        try {
            recursoProdutivoFrontService.saveRecursoProdutivoDTO(recursoProdutivoDTO);
            return ResponseEntity.ok("Production Resource Saved");
        } catch (RuntimeException e) {
            log.error("Error saving production resource", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

}
