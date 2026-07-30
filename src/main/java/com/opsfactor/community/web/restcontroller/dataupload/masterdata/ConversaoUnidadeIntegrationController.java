package com.opsfactor.community.web.restcontroller.dataupload.masterdata;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidade;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service.ConversaoUnidadeIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community de conversoes globais entre unidades de medida.
 *
 * <p>Conversoes quantitativas sao usadas por Demand Planning, Supply Planning
 * heuristico e Planning Book. Custos, precos, impostos e valores monetarios
 * ficam fora deste contrato.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Unit of Measure")
})
@RestController
public class ConversaoUnidadeIntegrationController extends IntegrationControllerAbstract<ConversaoUnidade, ConversaoUnidadeIntegrationDataDto, ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, ConversaoUnidadeIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {
        return "unitconversion";
    }
    
}
