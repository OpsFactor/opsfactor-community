package com.opsfactor.community.capability.demandplanning.demandplan.facade.mapper;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanDTO;
import org.mapstruct.Context;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * Mapper Spring/MapStruct entre entidade Demand Plan e DTOs do front
 * Community.
 *
 * <p>A conversao completa inclui linhas material/location. A conversao
 * resumida omite linhas para listagens e seletores, evitando carregar payloads
 * grandes sem necessidade.</p>
 */
@Mapper(componentModel = "spring", uses={DemandPlanItemAutoMapper.class})
public interface DemandPlanAutoMapper {

    /**
     * Converte a entidade completa, incluindo linhas.
     */
    @Mapping(source = "id", target = "demandPlanId")
    @Mapping(source = "descricao", target = "description")
    @Mapping(source = "horarioGeracao", target = "timeOfExecution")
    @Mapping(source = "perfilExecucaoDemandPlan.id", target = "executionProfileId")
    @Mapping(source = "tamanhoBucket", target = "bucketSize")
    @Mapping(source = "usuarioGeradorPlano", target = "generatedBy")
    @Mapping(source = "dataInicioPlano", target = "beginsOn")
    @Mapping(source = "linhasDemandPlan", target = "demandPlanDetail")
    public DemandPlanDTO converte(
            DemandPlan demandPlan,
            @Context ParametrosGlobais parametrosGlobais);

    /**
     * Converte metadados do plano sem linhas, para listagens de versoes.
     */
    @Mapping(source = "id", target = "demandPlanId")
    @Mapping(source = "descricao", target = "description")
    @Mapping(source = "horarioGeracao", target = "timeOfExecution")
    @Mapping(source = "perfilExecucaoDemandPlan.id", target = "executionProfileId")
    @Mapping(source = "tamanhoBucket", target = "bucketSize")
    @Mapping(source = "usuarioGeradorPlano", target = "generatedBy")
    @Mapping(source = "dataInicioPlano", target = "beginsOn")
    @Named(value = "converteSemLinhas")
    public DemandPlanDTO converteSemLinhas(DemandPlan demandPlan);

    /**
     * Converte listas usando o mapeamento resumido sem linhas.
     */
    @IterableMapping(qualifiedByName = "converteSemLinhas") 
    public List<DemandPlanDTO> converteListaSemLinhas(List<DemandPlan> demandPlanList);

}
