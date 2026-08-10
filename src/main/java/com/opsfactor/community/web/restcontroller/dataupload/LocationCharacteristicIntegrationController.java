package com.opsfactor.community.web.restcontroller.dataupload;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationService;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.web.bind.annotation.RestController;

/** FILE/JSON integration for Community location-characteristic definitions. */
@Tags({@Tag(name = "Master Data"), @Tag(name = "Location"), @Tag(name = "Characteristic")})
@RestController
public class LocationCharacteristicIntegrationController extends IntegrationControllerAbstract<
        CaracteristicaLocation,
        LocationCharacteristicIntegrationDataDto,
        LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO,
        EmptyIntegrationDataFilter,
        LocationCharacteristicIntegrationService,
        IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {

        return "characteristic/location";

    }

}
