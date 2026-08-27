package com.opsfactor.community.web.restcontroller.bi;

import com.opsfactor.community.platform.bi.facade.CommunityProductionOverviewResourceDetailService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewResourceDetailResponseDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewResourceDetailSelectionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Superfície Community da abertura detalhada do Production Overview.
 *
 * <p>O legado não declarava {@code @Secured} neste endpoint; esta classe
 * preserva esse mesmo contrato de RBAC e deixa a política de segurança global
 * da aplicação cuidar da rota {@code /api/secured}.</p>
 */
@RestController
public class CommunityProductionOverviewResourceDetailController {

    private static final String PRODUCTION_OVERVIEW_DETAILS_PATH =
            "api/secured/bi/planning/supply/productionoverview/"
                    + "{supplyPlanId}/{productionResourceId}/{periodIndex}/details";

    private static final String VOLUMES_AND_CAPACITIES_DETAILS_PATH =
            "api/secured/bi/planning/supply/volumesandcapacities/"
                    + "{supplyPlanId}/{productionResourceId}/{periodIndex}/details";

    /** Abre o detalhe da célula selecionada usando projections Community. */
    @Autowired
    private CommunityProductionOverviewResourceDetailService communityProductionOverviewResourceDetailService;

    /** Abre a célula sem o filtro opcional de materiais. */
    @GetMapping({
            PRODUCTION_OVERVIEW_DETAILS_PATH,
            VOLUMES_AND_CAPACITIES_DETAILS_PATH
    })
    public ResponseEntity<CommunityProductionOverviewResourceDetailResponseDTO> getResourceDetail(
            @PathVariable Long supplyPlanId,
            @PathVariable String productionResourceId,
            @PathVariable Integer periodIndex) {

        return ResponseEntity.ok(communityProductionOverviewResourceDetailService.getResourceDetail(
                supplyPlanId,
                productionResourceId,
                periodIndex));

    }

    /** Abre a célula aplicando apenas o filtro de características de material do POST. */
    @PostMapping({
            PRODUCTION_OVERVIEW_DETAILS_PATH,
            VOLUMES_AND_CAPACITIES_DETAILS_PATH
    })
    public ResponseEntity<CommunityProductionOverviewResourceDetailResponseDTO> getResourceDetail(
            @PathVariable Long supplyPlanId,
            @PathVariable String productionResourceId,
            @PathVariable Integer periodIndex,
            @RequestBody CommunityProductionOverviewResourceDetailSelectionDTO selectionDTO) {

        return ResponseEntity.ok(communityProductionOverviewResourceDetailService.getResourceDetail(
                supplyPlanId,
                productionResourceId,
                periodIndex,
                selectionDTO));

    }
}
