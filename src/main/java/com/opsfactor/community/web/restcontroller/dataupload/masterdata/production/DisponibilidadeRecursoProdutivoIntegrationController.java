package com.opsfactor.community.web.restcontroller.dataupload.masterdata.production;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto.DisponibilidadeRecursoProdutivoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.service.DisponibilidadeRecursoProdutivoIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community da disponibilidade diaria de recursos produtivos.
 *
 * <p>Este endpoint carrega apenas horas disponiveis por recurso/dia, entrada
 * necessaria para o plano restrito heuristico. Capacidade em quantidade/UOM,
 * disponibilidade por turno, calendarios produtivos detalhados e line
 * scheduling permanecem no OpsFactor Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Production")
})
@RestController
public class DisponibilidadeRecursoProdutivoIntegrationController extends IntegrationControllerAbstract<DisponibilidadeRecursoProdutivo, DisponibilidadeRecursoProdutivoIntegrationDataDto, DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, DisponibilidadeRecursoProdutivoIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {

        return "productionresourceavailability";

    }

}
