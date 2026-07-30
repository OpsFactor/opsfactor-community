package com.opsfactor.community.capability.masterdata.production.routing.facade.mapper;

import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.routing.facade.dto.RoteiroDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct de roteiro produtivo Community.
 */
// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring")
public interface RoteiroAutoMapper {
    
    // CONVERSÕES COM DTO ANTIGO RECEITAPRODUCAOOPERACAO ---------------------------------------------
    @Mapping(source = "descricao", target = "description")
    @Mapping(source = "prioridade", target = "priority")
    @Mapping(source = "location.id", target = "locationId")
    @Mapping(source = "materialOutput.id", target = "outputMaterialId")
    @Mapping(
            source = "habilitadoParaUsoSemVersaoProducaoCadastrado",
            target = "canBeUsedWithoutProductionVersion")
    @Mapping(source = "ativo", target = "active")
    public RoteiroDTO converte(Roteiro roteiro);
        
    @Mapping(source = "description", target = "descricao")
    @Mapping(source = "priority", target = "prioridade")
    @Mapping(source = "locationId", target = "location.id")
    @Mapping(source = "outputMaterialId", target = "materialOutput.id")
    @Mapping(
            source = "canBeUsedWithoutProductionVersion",
            target = "habilitadoParaUsoSemVersaoProducao")
    @Mapping(source = "active", target = "ativo")
    public Roteiro converte(RoteiroDTO roteiroDTO);
    
    public List<RoteiroDTO> converteListaEntidadeParaListaDTO(List<Roteiro> roteiroList);
    public List<Roteiro> converteListaDTOParaListaEntidade(List<RoteiroDTO> roteiroDTOList);
        
}
