package com.opsfactor.community.web.restcontroller.masterdata;

import com.opsfactor.community.capability.cluster.facade.dto.ClusterLocationsDTO;
import com.opsfactor.community.capability.cluster.facade.dto.ClusterProdutosDTO;
import com.opsfactor.community.capability.cluster.facade.dto.allocation.AlocacaoClusterLocationDTO;
import com.opsfactor.community.capability.cluster.facade.dto.allocation.AlocacaoClusterMaterialDTO;
import com.opsfactor.community.capability.cluster.facade.ClusteringFacade;
import com.opsfactor.community.capability.cluster.facade.dto.ClusterRuleDTO;
import com.opsfactor.community.capability.masterdata.demand.dfu.facade.dto.DFUDTO;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * API Community de clusters de material e location.
 *
 * <p>Clusters continuam no Community porque Demand Planning precisa executar
 * forecasts por combinacoes cluster material/location. O recorte aberto aceita
 * apenas clusters de material para processo DP, regras de material por status
 * permitido e regras de location por pais/estado ou tipo de location. Regras
 * por caracteristicas dinamicas, clusters de Pricing e agregadores genericos
 * permanecem Enterprise e sao bloqueados na service layer.</p>
 *
 * <p>Os paths publicos usam `materialclustering`, alinhado ao vocabulario
 * Community/Enterprise.</p>
 */
@Slf4j
@RestController
public class ClusteringRestController {

    /**
     * Service de configuracao dos clusters Community.
     *
     * <p>O bean centraliza os bloqueios de regras Enterprise, como
     * caracteristicas dinamicas, clusters de Pricing e status de material novo
     * fora do recorte aberto.</p>
     */
    @Autowired
    private ClusteringFacade clusteringFrontService;

    /**
     * Salva um cluster de materiais pelo contrato Community.
     */
    @PostMapping("api/secured/materialclustering/save")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> saveMaterialCluster(@RequestBody ClusterProdutosDTO materialClusterDTO) {

        try {
            clusteringFrontService.saveClusterProdutosDTO(materialClusterDTO);
            return ResponseEntity.ok("OK");
        } catch (RuntimeException e) {
            log.error("Error saving material cluster", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Salva um cluster de locations pelo contrato Community.
     */
    @PostMapping("api/secured/locationclustering/save")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> saveLocationCluster(@RequestBody ClusterLocationsDTO clusterLocationsDTO) {

        try {
            clusteringFrontService.saveClusterLocationsDTO(clusterLocationsDTO);
            return ResponseEntity.ok("OK");
        } catch (RuntimeException e) {
            log.error("Error saving location cluster", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista clusters de materiais configurados no Community.
     */
    @GetMapping("api/secured/materialclustering")
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public List<ClusterProdutosDTO> getMaterialClusterDTOList() {

        return clusteringFrontService.getListaTodosClusterProdutosDTOExcetoPadrao();

    }

    /**
     * Lista clusters de locations configurados no Community.
     */
    @GetMapping("api/secured/locationclustering")
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public List<ClusterLocationsDTO> getClusterLocationsDTOList() {

        return clusteringFrontService.getListaTodosClusterLocationsDTOExcetoPadrao();

    }

    /**
     * Carrega um cluster de materiais para o processo solicitado.
     */
    @GetMapping("api/secured/materialclustering/{id}/{process}")
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public ClusterProdutosDTO getMaterialClusterDTOByIdAndProcess(@PathVariable String id, @PathVariable String process) {

        return clusteringFrontService.getClusterProdutosDTO(id,process);

    }

    /**
     * Carrega um cluster de locations pelo identificador funcional.
     */
    @GetMapping("api/secured/locationclustering/{id}")
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public ClusterLocationsDTO getClusterLocationsDTODeId(@PathVariable String id) {

        return clusteringFrontService.getClusterLocationsDTO(id);

    }

    /**
     * Lista DFUs de material-location para a configuracao de clustering.
     */
    @GetMapping("api/secured/materialclustering/DFU")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<DFUDTO>> getMaterialLocationDfuList(
            @RequestParam(value = "materialClusterId", required = false) Long materialClusterId,
            @RequestParam Long locationClusterId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate referenceDate) {

        if (referenceDate == null) referenceDate = LocalDate.now();

        if (materialClusterId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "materialClusterId is required");
        }

        try {
            List<DFUDTO> dfuDTOList = clusteringFrontService.getDFUsDeClusterProdutosEmClusterLocations(
                    materialClusterId,
                    locationClusterId,
                    referenceDate.atStartOfDay());
            return ResponseEntity.ok(dfuDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing DFUs for material/location clusters", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Remove um cluster de materiais conforme a regra informada.
     */
    @DeleteMapping("api/secured/materialclustering/delete")
    @Secured("ROLE_ADMIN")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public String deleteMaterialCluster(@RequestBody ClusterRuleDTO clusterRuleDTO) {

        clusteringFrontService.deleteClusterProdutos(clusterRuleDTO);
        return "Cluster deleted.";

    }

    /**
     * Remove um cluster de locations conforme a regra informada.
     */
    @DeleteMapping("api/secured/locationclustering/delete")
    @Secured("ROLE_ADMIN")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public String deleteLocationCluster(@RequestBody ClusterRuleDTO clusterRuleDTO) {

        clusteringFrontService.deleteClusterLocations(clusterRuleDTO);
        return "Cluster deleted.";

    }


    /**
     * Lista os criterios de alocacao permitidos para clusters de materiais.
     */
    @GetMapping("api/secured/clustering/material/criteria")
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public Constantes.RegraAlocacaoClusterProdutosTipo[] materialClusteringCriteriaList() {

        return new Constantes.RegraAlocacaoClusterProdutosTipo[] {
                Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO
        };

    }

    /**
     * Lista os criterios de alocacao permitidos para clusters de locations.
     */
    @GetMapping("api/secured/clustering/location/criteria")
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public Constantes.RegraAlocacaoClusterLocationsTipo[] locationClusteringCriteriaList() {

        return new Constantes.RegraAlocacaoClusterLocationsTipo[] {
                Constantes.RegraAlocacaoClusterLocationsTipo.PAIS_ESTADO,
                Constantes.RegraAlocacaoClusterLocationsTipo.TIPO_LOCATION
        };

    }

    /**
     * Lista a alocacao atual de materiais em clusters.
     */
    @GetMapping("api/secured/clustering/material/allocation")
    @Secured("ROLE_ADMIN")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public List<AlocacaoClusterMaterialDTO> getAlocacaoMateriaisEmClusters() {

        return clusteringFrontService.getAlocacaoMateriaisEmClusters();

    }

    /**
     * Lista a alocacao atual de locations em clusters.
     */
    @GetMapping("api/secured/clustering/location/allocation")
    @Secured("ROLE_ADMIN")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public List<AlocacaoClusterLocationDTO> getAlocacaoLocationsEmClusters() {

        return clusteringFrontService.getAlocacaoLocationsEmClusters();

    }

    /**
     * Remove uma regra de alocacao de cluster de materiais.
     */
    @DeleteMapping("api/secured/clustering/material/criteria")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> deleteRegraAlocacaoClusterProdutosBy(@RequestParam("id") String id) {

        Long regraId = Long.parseLong(id);

        try {
            clusteringFrontService.deleteRegraAlocacaoClusterProdutosBy(regraId);
            return ResponseEntity.ok("Success");
        } catch (RuntimeException e) {
            log.error("Error deleting material cluster allocation rule {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Remove uma regra de alocacao de cluster de locations.
     */
    @DeleteMapping("api/secured/clustering/location/criteria")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> deleteRegraAlocacaoClusterLocationsBy(@RequestParam("id") String id) {

        Long regraId = Long.parseLong(id);

        try {
            clusteringFrontService.deleteRegraAlocacaoClusterLocationsBy(regraId);
            return ResponseEntity.ok("Success");
        } catch (RuntimeException e) {
            log.error("Error deleting location cluster allocation rule {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Valida ids textuais recebidos pelos endpoints legados de remocao de
     * criterios.
     *
     * <p>O parse fica fora do bloco `try/catch` dos repositories para que erro
     * de request seja reportado como 400, e nao como falha interna do
     * controller.</p>
     */
    


}
