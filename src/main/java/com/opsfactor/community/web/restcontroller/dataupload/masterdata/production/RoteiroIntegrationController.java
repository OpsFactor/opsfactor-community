package com.opsfactor.community.web.restcontroller.dataupload.masterdata.production;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.production.routing.integration.dto.RoteiroIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.production.routing.integration.service.RoteiroIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community de roteiros de producao.
 *
 * <p>O roteiro Community descreve cabecalho operacional: location, material de
 * saida, prioridade, status e uso sem versao de producao. Setup detalhado,
 * manutencao, turnos, custos, roteiros paralelos e line scheduling pertencem ao
 * OpsFactor Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Production")
})
@RestController
public class RoteiroIntegrationController extends IntegrationControllerAbstract<Roteiro, RoteiroIntegrationDataDto, RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, RoteiroIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {

        return "productionrouting";

    }

}
