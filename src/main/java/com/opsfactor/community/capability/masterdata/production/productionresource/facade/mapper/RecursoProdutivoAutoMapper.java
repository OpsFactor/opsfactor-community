package com.opsfactor.community.capability.masterdata.production.productionresource.facade.mapper;

import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionresource.facade.dto.RecursoProdutivoDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct de recurso produtivo Community.
 *
 * <p>O DTO expõe capacidade operacional por horas/eficiencia. Custos,
 * calendarios por turno e capacidade por UOM pertencem ao Enterprise.</p>
 */
// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring")
public interface RecursoProdutivoAutoMapper {
    
    @Mapping(source = "id", target = "productionResourceId")
    @Mapping(source = "location.id", target = "locationId")
    @Mapping(source = "descricao", target = "description")
    @Mapping(source = "ativo", target = "active")
    @Mapping(source = "eficiencia", target = "efficiency")
    public RecursoProdutivoDTO converte(RecursoProdutivo recursoProdutivo);
        
    public List<RecursoProdutivoDTO> converteListaEntidadeParaListaDTO(List<RecursoProdutivo> recursoProdutivoList);
        
}
