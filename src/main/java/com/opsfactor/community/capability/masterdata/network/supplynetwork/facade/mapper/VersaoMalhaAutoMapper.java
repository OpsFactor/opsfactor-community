package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.mapper;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.VersaoMalhaDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct de versao de malha usada pelo heuristico Community.
 */
@Mapper(componentModel = "spring")
public interface VersaoMalhaAutoMapper {
    
    @Mapping(source = "descricao", target = "description")
    @Mapping(
            source = "locationOrigemPadraoMateriasPrimas.id",
            target = "defaultRawMaterialOriginLocationId")
    @Mapping(
            source = "leadTimeDiasLocationOrigemPadraoMateriasPrimas",
            target = "defaultRawMaterialOriginLeadTimeDays")
    public VersaoMalhaDTO converte(VersaoMalha versaoMalha);

    @Mapping(source = "description", target = "descricao")
    @Mapping(
            source = "defaultRawMaterialOriginLeadTimeDays",
            target = "leadTimeDiasLocationOrigemPadraoMateriasPrimas")
    @Mapping(target = "locationOrigemPadraoMateriasPrimas", ignore = true)
    @Mapping(target = "locationOrigemPadraoClientes", ignore = true)
    @Mapping(target = "linhaTransporteSet", ignore = true)
    public VersaoMalha converte(VersaoMalhaDTO versaoMalhaDTO);
        
    public List<VersaoMalhaDTO> converteListaEntidadesParaDTOs(List<VersaoMalha> versaoMalhaList);
    
    public List<VersaoMalha> converteListaDTOsParaEntidade(List<VersaoMalhaDTO> versaoMalhaDTOList);
    
}
