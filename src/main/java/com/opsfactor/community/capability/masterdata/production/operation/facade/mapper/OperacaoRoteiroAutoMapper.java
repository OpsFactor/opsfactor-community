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
    @Mapping(source = "tempoPorQuantidadeBase", target = "operationDuration")
    @Mapping(expression = "java(operacaoRoteiro.getUnidadeTempoOperacao().getCodigo())", target = "timeUnit")
    public OperacaoRoteiroDTO converte(OperacaoRoteiro operacaoRoteiro);
    
    @Mapping(source = "routingId", target = "operacaoRoteiroCompositeKey.roteiro.id")
    @Mapping(source = "operationPosition", target = "operacaoRoteiroCompositeKey.posicao")
    @Mapping(source = "productionResourceId", target = "recursoProdutivo.id")
    @Mapping(source = "operationDuration", target = "tempoPorQuantidadeBase")
    @Mapping(target = "unidadeTempoOperacao", expression = "java(com.opsfactor.community.capability.masterdata.production.operation.domain.UnidadeTempoOperacao.deCodigoOuPadrao(operacaoRoteiroDTO.getTimeUnit()))")
    public OperacaoRoteiro converte(OperacaoRoteiroDTO operacaoRoteiroDTO);
        
    public List<OperacaoRoteiro> converteListaDTOParaListaEntidade(List<OperacaoRoteiroDTO> operacaoRoteiroDTOList);
    public List<OperacaoRoteiroDTO> converteListaEntidadeParaListaDTO(List<OperacaoRoteiro> operacaoRoteiroList);
    
}
