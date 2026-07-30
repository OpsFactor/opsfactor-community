package com.opsfactor.community.capability.masterdata.network.location.facade.mapper;

import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring")
public abstract class LocationMapper {
    
    @Named(value = "convertSemCaracteristicasLocation")
    public static LocationDTO convertSemCaracteristicasLocation(Location location){
        
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId(location.getId());
        locationDTO.setDescription(location.getDescricao());
        locationDTO.setLocationType(location.getTipoLocation());
        /*
         * O contrato REST publica o valor efetivo para que cadastros legados
         * sem coluna configurada continuem aparecendo como ativos.
         */
        locationDTO.setActive(location.getAtivo());
        locationDTO.setCountry(location.getPais());
        locationDTO.setState(location.getEstado());
        locationDTO.setCity(location.getCidade());
        /*
         * Coordenadas geograficas alimentam visualizacoes de mapa e baricentro,
         * que pertencem ao Enterprise. O DTO compartilhado preserva os campos,
         * mas a API Community nao os popula.
         */
        locationDTO.setLatitude(null);
        locationDTO.setLongitude(null);
        locationDTO.setShowInSupplyPlanningBook(location.getPlanejaSupply());
        locationDTO.setShowInProductionPlanningBook(location.getPlanejaProducao());
        /*
         * Deployment e restricoes logisticas gerais sao Enterprise. A
         * restricao inbound, porem, e consumida pelo heuristico Community: a
         * resposta publica seu valor efetivo (o default do dominio e true).
         */
        locationDTO.setApplyInboundConstraints(location.getConsideraRestricaoLinhaInbound());
        /*
         * Diferente da restricao inbound, este campo nao possui default local.
         * Publicamos exclusivamente o cadastro para a tela poder distinguir
         * uma escolha explicita da heranca do parametro global.
         */
        locationDTO.setSafetyStockConsiderIndirectDemand(
                location.getIncluiDemandaIndiretaNoSafetyStockCadastrado());
        locationDTO.setApplyProductionConstraints(location.getConsideraRestricaoProducao());
        
        return locationDTO;
        
    }

    /**
     * Converte a location para o contrato administrativo sem caracteristicas.
     *
     * <p>O metodo de instancia e o ponto de extensao dos overlays. A versao
     * Community delega para a conversao estatica historica, enquanto o
     * Enterprise pode reabrir apenas os escalares que pertencem a uma
     * capability privada sem duplicar os campos compartilhados.</p>
     */
    public LocationDTO convertLocationDtoWithoutCharacteristics(Location location) {

        return convertSemCaracteristicasLocation(location);

    }
    
    public Location convert(LocationDTO locationDTO){
        Location location = new Location();
        location.setId(locationDTO.getId());
        location.setDescricao(locationDTO.getDescription());
        location.setTipoLocation(locationDTO.getLocationType());
        location.setAtivo(locationDTO.getActive());
        location.setConsideraRestricaoLinhaInbound(locationDTO.getApplyInboundConstraints());
        location.setIncluiDemandaIndiretaNoSafetyStock(
                locationDTO.getSafetyStockConsiderIndirectDemand());
        return location;
    }
    
    /**
     * Precisa ser implementado pois há ambiguidade em qual conversao de locations (com ou sem caracteristicas) será usada para converter uma coleção de locations
     * Por padrão se usa a conversão sem características para evitar o N+1
     * @param locationList
     * @return 
     */
    @Named("converteListaEntidadeParaListaDTOSemCaracteristicasLocation")
    public List<LocationDTO> converteListaEntidadeParaListaDTOSemCaracteristicasLocation(Collection<Location> locationList) {
        return locationList.stream().map(this::convertLocationDtoWithoutCharacteristics)
                .collect(Collectors.toList());
    }

}
