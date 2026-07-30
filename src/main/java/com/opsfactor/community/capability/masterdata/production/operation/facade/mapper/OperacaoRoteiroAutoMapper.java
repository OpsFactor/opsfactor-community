package com.opsfactor.community.capability.masterdata.production.operation.facade.mapper;

import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.operation.facade.dto.OperacaoRoteiroDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct de operacao de roteiro Community.
 */
// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring")
public interface OperacaoRoteiroAutoMapper {
    
    // CONVERSÕES COM DTO ANTIGO RECEITAPRODUCAOOPERACAO ---------------------------------------------
    @Mapping(source = "operacaoRoteiroCompositeKey.roteiro.id", target = "routingId")
    @Mapping(source = "operacaoRoteiroCompositeKey.posicao", target = "operationPosition")
    @Mapping(source = "recursoProdutivo.id", target = "productionResourceId")
    @Mapping(expression = "java((operacaoRoteiro.getUnidadeMedidaCadastrado() == null) ? null : operacaoRoteiro.getUnidadeMedidaCadastrado().getId())", target = "unitOfMeasureId") // getter padrão do campo requer ParametrosGlobais
    @Mapping(source = "quantidadeBase", target = "baseQuantity")
    @Mapping(source = "horasPorQuantidadeBase", target = "hoursByBaseQuantity")
    public OperacaoRoteiroDTO converte(OperacaoRoteiro operacaoRoteiro);
    
    @Mapping(source = "routingId", target = "operacaoRoteiroCompositeKey.roteiro.id")
    @Mapping(source = "operationPosition", target = "operacaoRoteiroCompositeKey.posicao")
    @Mapping(source = "productionResourceId", target = "recursoProdutivo.id")
    @Mapping(source = "unitOfMeasureId", target = "unidadeMedida.id")
    @Mapping(source = "baseQuantity", target = "quantidadeBase")
    @Mapping(source = "hoursByBaseQuantity", target = "horasPorQuantidadeBase")
    public OperacaoRoteiro converte(OperacaoRoteiroDTO operacaoRoteiroDTO);
        
    public List<OperacaoRoteiro> converteListaDTOParaListaEntidade(List<OperacaoRoteiroDTO> operacaoRoteiroDTOList);
    public List<OperacaoRoteiroDTO> converteListaEntidadeParaListaDTO(List<OperacaoRoteiro> operacaoRoteiroList);
    
}
