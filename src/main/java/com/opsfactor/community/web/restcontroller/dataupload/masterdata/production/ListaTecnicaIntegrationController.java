package com.opsfactor.community.web.restcontroller.dataupload.masterdata.production;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.service.ListaTecnicaIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community do cabecalho de BOM/lista tecnica.
 *
 * <p>A lista tecnica Community representa apenas BOM simples com material de
 * saida, quantidade, UOM, location, prioridade e status. Co-produtos, outputs
 * paralelos e custos associados a BOM pertencem ao OpsFactor Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Production")
})
@RestController
public class ListaTecnicaIntegrationController extends IntegrationControllerAbstract<ListaTecnica, ListaTecnicaIntegrationDataDto, ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, ListaTecnicaIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {

        return "bom";

    }

}
