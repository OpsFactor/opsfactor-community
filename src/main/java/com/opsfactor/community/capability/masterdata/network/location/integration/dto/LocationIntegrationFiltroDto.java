package com.opsfactor.community.capability.masterdata.network.location.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract.TipoLocation;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Filtro de integracao Community para selecao/remocao de locations.
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationIntegrationFiltroDto {

   public List<String> id;
   public List<TipoLocation> locationType;

}
