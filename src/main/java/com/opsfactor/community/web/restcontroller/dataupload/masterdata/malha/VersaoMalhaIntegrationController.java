package com.opsfactor.community.web.restcontroller.dataupload.masterdata.malha;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.VersaoMalhaIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service.VersaoMalhaIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community da versao de malha.
 *
 * <p>O controller reabre o subpath legado `supplynetworkversion` usando a
 * infraestrutura generica FILE/JSON. A carga permanece no Community porque
 * transportation lanes e Supply Plans precisam referenciar a versao de rede
 * operacional mesmo sem capacidades privadas de mapa, frete ou frota.</p>
 */
@RestController
public class VersaoMalhaIntegrationController extends IntegrationControllerAbstract<
        VersaoMalha,
        VersaoMalhaIntegrationDataDto,
        VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO,
        EmptyIntegrationDataFilter,
        VersaoMalhaIntegrationService,
        IntegrationOptionsDto> {

    /**
     * Service concreto do contrato `supplynetworkversion`.
     */
    @Autowired
    private VersaoMalhaIntegrationService versaoMalhaIntegrationService;

    @Override
    protected String getSubPath() {

        return "supplynetworkversion";

    }

}
