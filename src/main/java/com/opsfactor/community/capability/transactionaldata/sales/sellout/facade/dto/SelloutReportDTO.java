package com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.dto;

import com.opsfactor.community.web.dto.template.DTO;
import java.time.LocalDateTime;
import lombok.experimental.SuperBuilder;

/**
 * Linha do relatorio Community de sell-out com conversoes para UOM padrao.
 *
 * <p>Sell-in, sales orders e valores financeiros pertencem ao Enterprise; este
 * DTO permanece quantitativo e baseado apenas em venda observada.</p>
 */
@SuperBuilder
public class SelloutReportDTO extends DTO {
    
    // campos do SelloutIntegrationDTO:
    public String documentId;
    public LocalDateTime referenceDate;
    public String originLocationId;
    public String materialId;
    public String uomId;
    public Double quantity;
    
    // campos adicionais:
    public String defaultDpUomId;
    public Double quantityInDefaultDpUom;
    
    public String defaultSnpUomId;
    public Double quantityInDefaultSnpUom;

}
