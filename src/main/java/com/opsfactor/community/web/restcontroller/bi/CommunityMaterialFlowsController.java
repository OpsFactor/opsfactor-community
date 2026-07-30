package com.opsfactor.community.web.restcontroller.bi;

import com.opsfactor.community.platform.bi.facade.CommunityMaterialFlowsService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityMaterialFlowsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Community que preserva a rota legada da visualização de material flows.
 *
 * <p>O legado confiava na cadeia autenticada global de {@code api/secured},
 * sem uma anotação {@code @Secured} no handler.</p>
 */
@RestController
public class CommunityMaterialFlowsController {

    /** Consulta a matriz física de transferências do Supply Plan selecionado. */
    @Autowired
    private CommunityMaterialFlowsService communityMaterialFlowsService;

    /** Retorna a matriz de transferências irrestritas de um Supply Plan. */
    @GetMapping("api/secured/bi/planning/supply/materialflows/{supplyPlanId}")
    public ResponseEntity<CommunityMaterialFlowsDTO> getMaterialFlows(
            @PathVariable Long supplyPlanId) {

        return ResponseEntity.ok(communityMaterialFlowsService.getMaterialFlows(supplyPlanId));

    }
}
