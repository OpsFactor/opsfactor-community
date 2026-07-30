package com.opsfactor.community.web.restcontroller.dataupload.masterdata;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidadeProduto;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service.ConversaoUnidadeProdutoIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community de conversoes material-especificas entre unidades de
 * medida.
 *
 * <p>O contrato permanece estritamente quantitativo. Conversoes ligadas a
 * precos, custos, fiscal ou parametros economicos pertencem ao Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Unit of Measure")
})
@RestController
public class ConversaoUnidadeProdutoIntegrationController extends IntegrationControllerAbstract<ConversaoUnidadeProduto, ConversaoUnidadeProdutoIntegrationDataDto, ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, ConversaoUnidadeProdutoIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {
        return "unitconversionmaterial";
    }

}
