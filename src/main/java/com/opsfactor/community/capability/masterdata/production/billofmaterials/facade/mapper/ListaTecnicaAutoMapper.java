package com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.mapper;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.ListaTecnicaDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct da lista tecnica Community.
 */
// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring")
public interface ListaTecnicaAutoMapper {
    
    // CONVERSÕES COM DTO ANTIGO RECEITAPRODUCAOOPERACAO ---------------------------------------------
    @Mapping(source = "listaTecnica.id", target = "id")
    @Mapping(source = "listaTecnica.descricao", target = "description")
    @Mapping(source = "listaTecnica.materialOutput.id", target = "outputMaterialId")
    @Mapping(expression = "java(listaTecnica.getUnidadeMedidaMaterialOutput(parametrosGlobais).getId())", target = "outputUnitOfMeasureId")
    @Mapping(source = "listaTecnica.quantidade", target = "outputQuantity")
    @Mapping(source = "listaTecnica.ativo", target = "active")
    public ListaTecnicaDTO converte(ListaTecnica listaTecnica, ParametrosGlobais parametrosGlobais);
        
    default public List<ListaTecnicaDTO> converteListaEntidadeParaListaDTO(List<ListaTecnica> listaTecnicaList, ParametrosGlobais parametrosGlobais) {
        return listaTecnicaList.stream().map(x -> converte(x, parametrosGlobais)).collect(Collectors.toList());
    }
        
}
