package com.opsfactor.community.web.restcontroller.dataupload.masterdata;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.product.material.integration.dto.ProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.product.material.integration.service.ProdutoIntegrationService;
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
 * Data upload Community de materiais.
 *
 * <p>O layout aceita somente atributos operacionais do material e UOMs
 * associadas. Caracteristicas dinamicas, sucessao de materiais, custos, precos
 * e filtros/agregadores Enterprise sao bloqueados no mapper ou permanecem fora
 * deste controller.</p>
 *
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Material")
})
@RestController
public class MaterialIntegrationController extends IntegrationControllerAbstract<Produto, ProdutoIntegrationDataDto, ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, ProdutoIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {
        return "material";
    }

    /**
     * Material publica somente as raizes canonicas de arquivo e JSON. A
     * abstracao ainda conserva aliases transitorios para outros controllers
     * Community fora deste recorte.
     */
    @Override
    public List<String> getRootFilePaths() {

        return List.of("api/secured/data/file/");

    }

    /**
     * A raiz JSON canonica impede o registro dinamico do alias historico de
     * data upload para materiais.
     */
    @Override
    public List<String> getRootJsonPaths() {

        return List.of("api/secured/data/");

    }

    /**
     * Service concreto usado pelos endpoints de material.
     */
    @Autowired
    private ProdutoIntegrationService materialIntegrationService;

    /**
     * Desativa materiais pelo filtro de integracao aprovado no Community.
     */
    @PostMapping("api/secured/data/material/deactivate")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<String> desativaMateriais(@RequestBody(required = false) ProdutoIntegrationDataDto.MaterialDeactivationFilterIntegrationDTO filtroMateriaisADesativar) {

        try {
            materialIntegrationService.desativaMateriaisDeFiltro(filtroMateriaisADesativar);
            return ResponseEntity.ok("Filtered Materials Deactivated");
        } catch (RuntimeException e) {
            log.error("Error deactivating filtered materials", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

}
