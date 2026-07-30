package com.opsfactor.community.web.restcontroller.dataupload.masterdata.production;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto.RecursoProdutivoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.service.RecursoProdutivoIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community de recursos produtivos operacionais.
 *
 * <p>Este controller publica apenas o cadastro usado pelo Supply Planning
 * heuristico com capacidade produtiva em horas totais por dia. Capacidade por
 * UOM, turnos, manutencao, custos de recurso, line scheduling e qualquer dado
 * economico permanecem no OpsFactor Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Production")
})
@RestController
public class RecursoProdutivoIntegrationController extends IntegrationControllerAbstract<RecursoProdutivo, RecursoProdutivoIntegrationDataDto, RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, RecursoProdutivoIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {

        return "productionresource";

    }

}
