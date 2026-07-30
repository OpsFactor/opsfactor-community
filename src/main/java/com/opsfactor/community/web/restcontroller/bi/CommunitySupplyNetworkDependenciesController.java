package com.opsfactor.community.web.restcontroller.bi;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.SupplyNetworkDependencyExplorerService;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.SupplyNetworkDependencyDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Community que preserva o explorador legado de dependências da malha.
 *
 * <p>A rota historicamente dependia somente da cadeia autenticada global de
 * {@code api/secured}; por isso não declara {@code @Secured} local.</p>
 */
@RestController
public class CommunitySupplyNetworkDependenciesController {

    /** Serviço que materializa a árvore limitada de dependências da malha. */
    private final SupplyNetworkDependencyExplorerService supplyNetworkDependencyExplorerService;

    /**
     * Fixa o explorador de malha publicado pela rota Community somente leitura.
     */
    @Autowired
    public CommunitySupplyNetworkDependenciesController(
            SupplyNetworkDependencyExplorerService supplyNetworkDependencyExplorerService) {

        this.supplyNetworkDependencyExplorerService = supplyNetworkDependencyExplorerService;

    }

    /** Returns the bounded dependency tree for one location and optionally one material. */
    @GetMapping("api/secured/supplynetwork/dependencies")
    public ResponseEntity<List<SupplyNetworkDependencyDTO>> getSupplyNetworkDependencies(
            @RequestParam String supplyNetworkId,
            @RequestParam String locationId,
            @RequestParam(required = false) String materialId,
            @RequestParam Integer maximumTreeDepth) {

        return ResponseEntity.ok(supplyNetworkDependencyExplorerService.getDependencies(
                supplyNetworkId,
                locationId,
                materialId,
                maximumTreeDepth));

    }
}
