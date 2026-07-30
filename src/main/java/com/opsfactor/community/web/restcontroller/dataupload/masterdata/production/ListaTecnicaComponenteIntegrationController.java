package com.opsfactor.community.web.restcontroller.dataupload.masterdata.production;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaComponenteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.service.ListaTecnicaComponenteIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community dos componentes de BOM/lista tecnica.
 *
 * <p>O contrato Community persiste somente material componente, quantidade e
 * UOM de consumo. Substituicoes, custos, perdas economicas e regras avancadas
 * de line scheduling pertencem ao OpsFactor Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Production")
})
@RestController
public class ListaTecnicaComponenteIntegrationController extends IntegrationControllerAbstract<ListaTecnicaComponente, ListaTecnicaComponenteIntegrationDataDto, ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, ListaTecnicaComponenteIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {

        return "bomcomponents";

    }

}
