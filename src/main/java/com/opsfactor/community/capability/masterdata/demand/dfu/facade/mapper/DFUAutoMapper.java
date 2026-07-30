package com.opsfactor.community.capability.masterdata.demand.dfu.facade.mapper;

import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.facade.dto.DFUDTO;

import java.util.Map;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct de DFU material/location para DTOs de front.
 */
// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring") 
public interface DFUAutoMapper {
    
    @Mapping(source = "produto.id", target = "materialId")
    @Mapping(source = "location.id", target = "locationId")
    public DFUDTO converte(DFU dfu);
        
    public Set<DFUDTO> converteDFUListParaDFUDTOSet(Set<DFU> dfus);
    
    public Map<Integer,Set<DFUDTO>> converteMapaDFUParaMapaDTO(Map<Integer,Set<DFU>> mapaDFUs);
    
}
