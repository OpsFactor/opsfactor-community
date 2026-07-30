package com.opsfactor.community.web.restcontroller.dataupload.masterdata.malha;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service.LinhaTransporteIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community da transportation lane origem/destino.
 *
 * <p>O controller reabre o subpath legado `transportationlane` para a malha
 * operacional consumida pelo Supply Planning heuristico. Dados privados de
 * distancia, mapa, frete, frota e custo continuam bloqueados pelo DTO/mapper e
 * permanecem no overlay Enterprise.</p>
 */
@RestController
public class LinhaTransporteIntegrationController extends IntegrationControllerAbstract<
        LinhaTransporte,
        LinhaTransporteIntegrationDataDto,
        LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO,
        EmptyIntegrationDataFilter,
        LinhaTransporteIntegrationService,
        IntegrationOptionsDto> {

    /**
     * Service concreto do contrato `transportationlane`.
     */
    @Autowired
    private LinhaTransporteIntegrationService linhaTransporteIntegrationService;

    @Override
    protected String getSubPath() {

        return "transportationlane";

    }

}
