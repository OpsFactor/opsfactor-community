package com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract.TipoLocation;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Filtro de extracao/remocao para o estoque inicial Community.
 *
 * <p>O filtro sempre deve delimitar periodo. O tipo de location e apenas um
 * refinamento opcional sobre o mesmo snapshot simples de estoque, sem
 * granularidade por lote/batch.</p>
 */
@Builder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EstoqueIntegrationFiltroDto {

    public LocalDate startDate;
    public LocalDate endDate;
    public List<TipoLocation> locationType;

}
