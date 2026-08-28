package com.opsfactor.community.web.restcontroller.dataupload.masterdata.production;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.dto.VersaoProducaoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.service.VersaoProducaoIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community de versoes simples de producao.
 *
 * <p>Uma versao simples aponta uma unica combinacao de location, material de
 * saida, roteiro e BOM para o Supply Planning heuristico. Parallel routing,
 * multiplos outputs e escolha avancada de versoes sao capacidades Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Production")
})
@RestController
public class VersaoProducaoIntegrationController extends IntegrationControllerAbstract<VersaoProducao, VersaoProducaoIntegrationDataDto, VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, VersaoProducaoIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {

        return "productionversion";

    }

}
