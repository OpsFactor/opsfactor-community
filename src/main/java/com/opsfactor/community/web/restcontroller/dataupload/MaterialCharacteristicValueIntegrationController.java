package com.opsfactor.community.web.restcontroller.dataupload;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationService;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaProduto;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller Enterprise de carga/extracao de valores de caracteristica por material.
 */
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Material"),
        @Tag(name = "Characteristic")
})
@RestController
public class MaterialCharacteristicValueIntegrationController extends IntegrationControllerAbstract<
        ValorCaracteristicaProduto,
        MaterialCharacteristicValueIntegrationDataDto,
        MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO,
        EmptyIntegrationDataFilter,
        MaterialCharacteristicValueIntegrationService,
        IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {

        return "characteristic/material/value";

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
