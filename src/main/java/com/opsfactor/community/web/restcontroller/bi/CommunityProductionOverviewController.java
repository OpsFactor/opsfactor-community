package com.opsfactor.community.web.restcontroller.bi;

import com.opsfactor.community.platform.bi.facade.CommunityProductionOverviewService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewSelectionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Superfície Community do agregado Production Overview.
 *
 * <p>O legado não declarava RBAC por método para esta leitura. O controller
 * preserva essa característica e deixa a autenticação global da rota secured
 * atuar sem ampliar nem restringir silenciosamente o contrato original.</p>
 */
@RestController
public class CommunityProductionOverviewController {

    private static final String PRODUCTION_OVERVIEW_PATH =
            "api/secured/bi/planning/supply/productionoverview";

    /** Executa a montagem indexada das séries publicadas pelo Production Overview. */
    @Autowired
    private CommunityProductionOverviewService communityProductionOverviewService;

    /** Retorna o JSON achatado do Production Overview. */
    @PostMapping({
            PRODUCTION_OVERVIEW_PATH,
            "api/secured/bi/planning/supply/volumesandcapacities"
    })
    public ResponseEntity<CommunityProductionOverviewDTO> getProductionOverview(
            @RequestBody CommunityProductionOverviewSelectionDTO selectionDTO) {

        return ResponseEntity.ok(
                communityProductionOverviewService.getProductionOverview(selectionDTO));

    }
}
