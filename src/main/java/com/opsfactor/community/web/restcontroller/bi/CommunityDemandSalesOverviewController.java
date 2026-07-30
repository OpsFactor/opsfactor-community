package com.opsfactor.community.web.restcontroller.bi;

import com.opsfactor.community.platform.bi.facade.CommunityDemandSalesOverviewService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityDemandSalesOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityDemandSalesOverviewSelectionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Borda REST Community da comparação quantitativa de Sales e Demand Plan.
 *
 * <p>A rota não publica valores monetários, Custom Key Figures, filtros por
 * característica nem agregação. Esses recursos requerem capacidades
 * Enterprise que não devem vazar para uma consulta Community simples.</p>
 */
@RestController
public class CommunityDemandSalesOverviewController {

    /** Serviço read-only que materializa as séries oficiais em lote. */
    @Autowired
    private CommunityDemandSalesOverviewService communityDemandSalesOverviewService;

    /** Retorna sell-out histórico e Direct Demand irrestrita por DFU/período. */
    @PostMapping("api/secured/planning/demand/overview")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<CommunityDemandSalesOverviewDTO> getDemandSalesOverview(
            @RequestBody CommunityDemandSalesOverviewSelectionDTO selectionDTO) {

        return ResponseEntity.ok(communityDemandSalesOverviewService.getDemandSalesOverview(selectionDTO));

    }
}
