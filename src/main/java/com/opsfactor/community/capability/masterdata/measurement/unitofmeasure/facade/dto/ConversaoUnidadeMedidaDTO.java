package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO de conversao entre unidades de medida.
 *
 * <p>Conversoes globais e especificas por material ficam no Community porque
 * sao necessarias para Demand/Supply quantitativo. Custos, frotas e pricing
 * nao sao representados por este DTO.</p>
 */
@Data
@ToString @NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversaoUnidadeMedidaDTO {
    
    public String materialId;
    public String originUomId;
    public String targetUomId;
    public Double conversionCoefficient;
    public String stepByStep;

}
