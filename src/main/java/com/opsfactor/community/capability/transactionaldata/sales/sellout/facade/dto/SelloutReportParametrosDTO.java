package com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.dto;

import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.FiltroMaterialLocationDeCombinacaoCaracteristicasDTO;
import com.opsfactor.community.web.dto.template.DTO;
import java.time.LocalDate;

/**
 * Parametros de extracao do relatorio Community de sell-out.
 *
 * <p>O filtro material/location e aplicado pelo service consumidor; este DTO
 * apenas carrega periodo e filtro solicitado pela tela.</p>
 */
public class SelloutReportParametrosDTO extends DTO {
    
    // campos do SelloutIntegrationDTO:
    public LocalDate startDate;
    public LocalDate endDate;

    public FiltroMaterialLocationDeCombinacaoCaracteristicasDTO materialLocationFilterDTO;
    
}
