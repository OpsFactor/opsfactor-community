package com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.mapper;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.ListaTecnicaComponenteDTO;

import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct dos componentes da lista tecnica Community.
 */
// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring")
public interface ListaTecnicaComponenteAutoMapper {
    
    // CONVERSÕES COM DTO ANTIGO RECEITAPRODUCAOOPERACAO ---------------------------------------------
    @Mapping(source = "listaTecnicaComponente.listaTecnicaComponenteCompositeKey.listaTecnica.id", target = "billOfMaterialsId")
    @Mapping(source = "listaTecnicaComponente.listaTecnicaComponenteCompositeKey.materialComponente.id", target = "componentMaterialId")
    @Mapping(expression = "java(listaTecnicaComponente.getUnidadeMedidaMaterialComponente(parametrosGlobais).getId())", target = "componentMaterialUnitOfMeasureId")
    @Mapping(source = "listaTecnicaComponente.quantidade", target = "quantity")
    public ListaTecnicaComponenteDTO converte(ListaTecnicaComponente listaTecnicaComponente, ParametrosGlobais parametrosGlobais);
        
    default public List<ListaTecnicaComponenteDTO> converteListaEntidadeParaListaDTO(List<ListaTecnicaComponente> listaTecnicaComponenteList, ParametrosGlobais parametrosGlobais) {
        return listaTecnicaComponenteList.stream().map(x -> converte(x, parametrosGlobais)).collect(Collectors.toList());
    }
        
}
