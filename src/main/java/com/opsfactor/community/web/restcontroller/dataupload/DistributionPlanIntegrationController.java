package com.opsfactor.community.web.restcontroller.dataupload;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.web.configuration.UserRoleType;
import com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationDataDto;
import com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller compartilhado read-only do Distribution Plan por Supply Plan.
 */
@Tags({
        @Tag(name = "Supply Planning"),
        @Tag(name = "Distribution Plan")
})
@RestController
public class DistributionPlanIntegrationController extends IntegrationControllerAbstract<
        DistributionPlanIntegrationDataDto,
        DistributionPlanIntegrationDataDto,
        DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO,
        EmptyIntegrationDataFilter,
        DistributionPlanIntegrationService,
        IntegrationOptionsDto> {

    /**
     * Service Enterprise concreto usado pelos endpoints filtrados por plano.
     */
    @Autowired
    private DistributionPlanIntegrationService distributionPlanIntegrationService;

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

        return "distributionplan";

    }

    /**
     * Baixa arquivo de Distribution Plan de um Supply Plan especifico.
     */
    @GetMapping("api/secured/data/file/distributionplan/{supplyPlanId}")
    @Secured("ROLE_ADMIN")
    public List<List<Object>> getDistributionPlanFile(
            @PathVariable("supplyPlanId") Long supplyPlanId) {

        return distributionPlanIntegrationService.getFile(supplyPlanId);

    }

    /**
     * Baixa JSON de Distribution Plan de um Supply Plan especifico.
     */
    @GetMapping("api/secured/data/distributionplan/{supplyPlanId}")
    @Secured("ROLE_ADMIN")
    public List<DistributionPlanIntegrationDataDto> getDistributionPlanJson(
            @PathVariable("supplyPlanId") Long supplyPlanId) {

        return distributionPlanIntegrationService.getDistributionPlanDTOList(supplyPlanId);

    }

}
