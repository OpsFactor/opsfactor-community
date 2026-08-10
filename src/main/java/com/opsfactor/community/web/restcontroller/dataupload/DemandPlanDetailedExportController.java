package com.opsfactor.community.web.restcontroller.dataupload;

import com.opsfactor.community.capability.demandplanning.export.DemandPlanDetailedExportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Superficie compartilhada somente leitura para o arquivo detalhado de Demand Plan. */
@Tags({
        @Tag(name = "Demand Planning"),
        @Tag(name = "Data Upload")
})
@RestController
public class DemandPlanDetailedExportController {

    @Autowired
    private DemandPlanDetailedExportService demandPlanDetailedExportService;

    /** Baixa a fotografia detalhada integral do Demand Plan solicitado. */
    @GetMapping("api/secured/data/file/demandplan/{demandPlanId}")
    @Secured("ROLE_ADMIN")
    public List<List<Object>> getDemandPlanDetailedFile(
            @PathVariable("demandPlanId") Long demandPlanId) {

        return demandPlanDetailedExportService.getFile(demandPlanId);

    }

    /** Baixa somente o bucket operacional que contem a data solicitada. */
    @GetMapping("api/secured/data/file/demandplan/{demandPlanId}/period/{referenceDate}")
    @Secured("ROLE_ADMIN")
    public List<List<Object>> getDemandPlanDetailedPeriodFile(
            @PathVariable("demandPlanId") Long demandPlanId,
            @PathVariable("referenceDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {

        return demandPlanDetailedExportService.getFileByPeriod(demandPlanId, referenceDate);

    }

}
