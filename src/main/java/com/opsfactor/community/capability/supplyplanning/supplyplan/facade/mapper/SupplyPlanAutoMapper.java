package com.opsfactor.community.capability.supplyplanning.supplyplan.facade.mapper;

import com.opsfactor.community.capability.demandplanning.demandplan.facade.mapper.DemandPlanAutoMapper;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanDTO;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct de cabecalho de Supply Plan para a borda front Community.
 *
 * <p>A conversao inclui Demand Plan sem linhas para evitar trafego pesado ao
 * listar planos. Detalhes de Planning Book e linhas fisicas sao carregados por
 * services/projections especificos.</p>
 */
// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring", uses={DemandPlanAutoMapper.class})
public interface SupplyPlanAutoMapper {
    
    @Mapping(source = "id", target = "supplyPlanId")
    @Mapping(source = "versaoMalha.id", target = "supplyNetworkVersionId")
    @Mapping(source = "perfilExecucaoSupplyPlan.id", target = "executionProfileId")
    @Mapping(source = "descricao", target = "description")
    @Mapping(source = "horarioGeracao", target = "timeOfExecution")
    @Mapping(source = "tamanhoBucket", target = "bucketSize")
    @Mapping(source = "usuarioGeradorPlano", target = "generatedBy")
    @Mapping(source = "dataInicioPlano", target = "beginsOn")
    @Mapping(source = "demandPlan", target = "demandPlanDTO", qualifiedByName = "converteSemLinhas") // não queremos demand plan com todas as linhas
    public SupplyPlanDTO converte(SupplyPlan supplyPlan);
    
    public List<SupplyPlanDTO> converteLista(List<SupplyPlan> supplyPlanList);

}
