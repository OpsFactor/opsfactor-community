package com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

/**
 * Filtro de extracao/remocao do historico de vendas Community.
 *
 * <p>O periodo e obrigatorio nos services para evitar operacoes amplas sobre
 * dado transacional. O tipo de location apenas restringe o mesmo historico de
 * vendas observado, sem alternar para sell-in ou pedidos.</p>
 */
@Builder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SelloutIntegrationFiltroDto {

    public LocalDate startDate;
    public LocalDate endDate;
    public List<LocationAbstract.TipoLocation> locationType;
    
}
