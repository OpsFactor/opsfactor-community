package com.opsfactor.community.web.restcontroller.dataupload;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationService;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaLocation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller Enterprise de carga/extracao de valores de caracteristica por location.
 */
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Location"),
        @Tag(name = "Characteristic")
})
@RestController
public class LocationCharacteristicValueIntegrationController extends IntegrationControllerAbstract<
        ValorCaracteristicaLocation,
        LocationCharacteristicValueIntegrationDataDto,
        LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO,
        EmptyIntegrationDataFilter,
        LocationCharacteristicValueIntegrationService,
        IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {

        return "characteristic/location/value";

    }

    @Override
    public List<String> getRootFilePaths() {

        return List.of("api/secured/data/file/");

    }

    @Override
    public List<String> getRootJsonPaths() {

        return List.of("api/secured/data/");

    }

}
