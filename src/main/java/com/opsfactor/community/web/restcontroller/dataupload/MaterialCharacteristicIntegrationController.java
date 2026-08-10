package com.opsfactor.community.web.restcontroller.dataupload;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationService;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.web.bind.annotation.RestController;

/** FILE/JSON integration for Community material-characteristic definitions. */
@Tags({@Tag(name = "Master Data"), @Tag(name = "Material"), @Tag(name = "Characteristic")})
@RestController
public class MaterialCharacteristicIntegrationController extends IntegrationControllerAbstract<
        CaracteristicaProduto,
        MaterialCharacteristicIntegrationDataDto,
        MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO,
        EmptyIntegrationDataFilter,
        MaterialCharacteristicIntegrationService,
        IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {

        return "characteristic/material";

    }

}
