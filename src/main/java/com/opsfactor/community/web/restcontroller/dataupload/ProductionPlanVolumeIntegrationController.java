package com.opsfactor.community.web.restcontroller.dataupload;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.web.configuration.UserRoleType;
import com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationDataDto;
import com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller Enterprise read-only de Production Plan Volume por supply plan.
 */
@Tags({
        @Tag(name = "Supply Planning"),
        @Tag(name = "Production Plan")
})
@RestController
public class ProductionPlanVolumeIntegrationController extends IntegrationControllerAbstract<
        ProductionPlanVolumeIntegrationDataDto,
        ProductionPlanVolumeIntegrationDataDto,
        ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO,
        EmptyIntegrationDataFilter,
        ProductionPlanVolumeIntegrationService,
        IntegrationOptionsDto> {

    /**
     * Service Enterprise concreto usado pelos endpoints filtrados por plano.
     */
    @Autowired
    private ProductionPlanVolumeIntegrationService productionPlanVolumeIntegrationService;

    @Override
    protected List<UserRoleType> getUserRoleTypesGet() {

        return List.of(UserRoleType.ROLE_ADMIN);

    }

    @Override
    protected List<UserRoleType> getUserRoleTypesPost() {

        return List.of();

    }

    @Override
    protected String getSubPath() {

        return "productionplan/volume";

    }

    /**
     * Baixa arquivo de Production Plan Volume de um supply plan especifico.
     */
    @GetMapping("api/secured/data/file/productionplan/volume/{supplyPlanId}")
    @Secured("ROLE_ADMIN")
    public List<List<Object>> getProductionPlanVolumeFile(
            @PathVariable("supplyPlanId") Long supplyPlanId) {

        return productionPlanVolumeIntegrationService.getFile(supplyPlanId);

    }

    /**
     * Baixa JSON de Production Plan Volume de um supply plan especifico.
     */
    @GetMapping("api/secured/data/productionplan/volume/{supplyPlanId}")
    @Secured("ROLE_ADMIN")
    public List<ProductionPlanVolumeIntegrationDataDto> getProductionPlanVolumeJson(
            @PathVariable("supplyPlanId") Long supplyPlanId) {

        return productionPlanVolumeIntegrationService.getProductionPlanVolumeDTOList(supplyPlanId);

    }

}
