package com.opsfactor.community.capability.demandplanning.demandplan.facade.mapper;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanItemDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper do contrato material/location do Demand Planning Book.
 *
 * <p>MapStruct lê a chave JPA `produto`, mas o DTO publico deve expor
 * `materialId`. Esse mapeamento explicito evita que a nomenclatura fisica da
 * entidade volte para a API.</p>
 */
// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring") 
public interface DemandPlanItemAutoMapper {
    
    @Mapping(source = "demandPlanItem.key.location.id", target = "locationId")
    @Mapping(source = "demandPlanItem.key.produto.id", target = "materialId")
    @Mapping(source = "demandPlanItem.key.dataReferencia", target = "referenceDate")
    @Mapping(expression = "java(demandPlanItem.getUnidadeMedida(parametrosGlobais).getId())", target = "uomId")
    @Mapping(source = "demandPlanItem.quantidadeBaseline", target = "baselineQtyUnconstrained")
    @Mapping(source = "demandPlanItem.quantidadeAjusteDemanda", target = "demandAdjustmentQtyUnconstrained")
    @Mapping(expression = "java(demandPlanItem.getQuantidadeBaseline() + demandPlanItem.getQuantidadeAjusteDemanda())", target = "totalQtyUnconstrained")
    @Mapping(source = "demandPlanItem.quantidadeBaselineAtendida", target = "baselineQtyConstrained")
    @Mapping(source = "demandPlanItem.quantidadeAjusteDemandaAtendida", target = "demandAdjustmentQtyConstrained")
    @Mapping(expression = "java(demandPlanItem.getQuantidadeBaselineAtendida() + demandPlanItem.getQuantidadeAjusteDemandaAtendida())", target = "totalQtyConstrained")
    public DemandPlanItemDTO converte(
            DemandPlanItem demandPlanItem,
            @Context ParametrosGlobais parametrosGlobais);

    default public List<DemandPlanItemDTO> converte(
            List<DemandPlanItem> listaDemandPlanItems,
            @Context ParametrosGlobais parametrosGlobais) {
        return listaDemandPlanItems.stream().map(x -> converte(x, parametrosGlobais)).collect(Collectors.toList());
    }
    
}
