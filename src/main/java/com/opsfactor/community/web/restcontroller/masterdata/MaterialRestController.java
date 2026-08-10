package com.opsfactor.community.web.restcontroller.masterdata;

import com.opsfactor.community.capability.cluster.facade.dto.ClusterProdutosDTO;
import com.opsfactor.community.capability.masterdata.product.material.facade.dto.MaterialStatus;
import com.opsfactor.community.capability.masterdata.product.material.facade.dto.ProdutoDTO;
import com.opsfactor.community.capability.masterdata.product.material.facade.MaterialDtoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API Community de materiais.
 *
 * <p>O contrato funcional usa "material". A superficie aberta fornece
 * listagens operacionais, clusters DP e status de ciclo de vida
 * suportados. Pricing, caracteristicas dinamicas, sucessao de materiais e
 * filtros/agregadores Enterprise nao sao publicados por este controller.</p>
 */
@RestController
public class MaterialRestController {

    /**
     * Service DTO de material.
     *
     * <p>A entidade fisica ainda usa `Produto`, enquanto a borda publica usa
     * somente a nomenclatura funcional de material.</p>
     */
    @Autowired
    private MaterialDtoService materialDtoService;

    /**
     * Lista os materiais cadastrados no Community.
     *
     * <p>O alias {@code /api/secured/product} preserva o contrato de leitura
     * consumido pelo Planning Front. Ambos os caminhos executam exatamente a
     * mesma consulta e devolvem o mesmo DTO; o contrato público preferencial
     * da edição Community continua sendo {@code /api/secured/material}.</p>
     */
    @GetMapping({"/api/secured/material", "/api/secured/product"})
    @Secured("ROLE_ADMIN")
    @ResponseBody @ResponseStatus(HttpStatus.OK)
    public List<ProdutoDTO> getMateriais() {

        return materialDtoService.getMaterialDTOList();

    }

    /**
     * Lista os clusters de materiais disponiveis para selecao no front.
     */
    @GetMapping("/api/secured/material/cluster")
    @Secured("ROLE_ADMIN")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public List<ClusterProdutosDTO> getMaterialClusterDTOList() {

        return materialDtoService.getMaterialClusterDTOList();

    }

    /**
     * Lista materiais pertencentes a um cluster de materiais.
     */
    @GetMapping("/api/secured/material/cluster/{clusterId}/materials")
    @Secured("ROLE_ADMIN")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public List<ProdutoDTO> getMaterialDTOListFromMaterialClusterId(@PathVariable("clusterId") Long clusterId) {

        return materialDtoService.getMaterialDTOListFromMaterialClusterId(clusterId);

    }

    /**
     * Lista os status de material publicados no contrato Community.
     */
    @GetMapping("/api/secured/material/status")
    @Secured("ROLE_ADMIN")
    public MaterialStatus[] getMaterialStatusList() {

        return MaterialStatus.values();

    }

}
