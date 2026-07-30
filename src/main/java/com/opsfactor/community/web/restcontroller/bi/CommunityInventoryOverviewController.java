package com.opsfactor.community.web.restcontroller.bi;

import com.opsfactor.community.platform.bi.facade.CommunityInventoryOverviewService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewSelectionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Borda REST do Inventory Overview físico do modelo novo Community. */
@RestController
public class CommunityInventoryOverviewController {

    /** Executa a leitura física do estoque e Days of Supply solicitada pela API Community. */
    @Autowired
    private CommunityInventoryOverviewService communityInventoryOverviewService;

    /** Retorna estoque físico fechado e Days of Supply sem capacidades financeiras Enterprise. */
    @PostMapping("api/secured/bi/supply/inventory-overview")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<CommunityInventoryOverviewDTO> getInventoryOverview(
            @RequestBody CommunityInventoryOverviewSelectionDTO selectionDTO) {

        return ResponseEntity.ok(communityInventoryOverviewService.getInventoryOverview(selectionDTO));

    }

}
