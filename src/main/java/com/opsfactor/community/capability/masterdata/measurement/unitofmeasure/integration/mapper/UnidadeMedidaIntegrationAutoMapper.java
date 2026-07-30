package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.UnidadeMedidaDataUploadDTO;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring")
public interface UnidadeMedidaIntegrationAutoMapper {
    
    @Mapping(source = "id", target = "id")
    @Mapping(source = "descricao", target = "description")
    public UnidadeMedidaDataUploadDTO converteEntidadeParaDTO(UnidadeMedida unidadeMedida);
    
    public List<UnidadeMedidaDataUploadDTO> converteColecaoEntidadesParaListaDTO(Collection<UnidadeMedida> unidadeMedidaCollection);
    
    /**
     * Converte uma linha de um arquivo para o DTO
     * Somente usado para carga manual de arquivos, e não para integração via JSON
     * LinhaArquivo já deve ter o número total de colunas necessárias para o DTO
     * com valores null caso coluna esteja vazia
     * @param linhaArquivo
     * @return 
     */
    public default UnidadeMedidaDataUploadDTO converteLinhaArquivoParaDTO(List<String> linhaArquivo) {
        String optionalDescription = null;
        String optionalDelete = null;
        
        switch(linhaArquivo.size()){
            case 3:
                optionalDescription = (String) linhaArquivo.get(1);
                optionalDelete = (String) linhaArquivo.get(2);
                
            case 2:
                optionalDescription = (String) linhaArquivo.get(1).trim();
        
        }
        
 
        return UnidadeMedidaDataUploadDTO.builder()
                .id((String) linhaArquivo.get(0).trim())
                .description(optionalDescription)
                .delete(optionalDelete) // delete é sempre última coluna
                .build();
        
    }
    
    /**
     * Converte uma coleção de entidades para um arquivo
     * Inclui um cabeçalho no arquivo
     * @param colecaoEntidades
     * @return 
     */
    public default List<List<Object>> converteColecaoEntidadesParaArquivo(Collection<UnidadeMedida> colecaoEntidades) {
        
        List<List<Object>> linhasArquivo = new ArrayList<>();
              
        // inclui o cabeçalho
        List<Object> linhaHeader = new ArrayList<>();
        linhaHeader.add("Unit of Measure Id");
        linhaHeader.add("Description");
        
        linhasArquivo.add(linhaHeader);
        
        // insere as demais linhas com dados
        for (UnidadeMedida unidadeMedida : colecaoEntidades) {
            List<Object> linhaArquivo = converteEntidadeParaLinhaArquivo(unidadeMedida);
            linhasArquivo.add(linhaArquivo);
        }
        
        return linhasArquivo;
        
    }
    
    /** 
     * Converte uma só entidade para uma só linha do arquivo (.xlsx ou .csv)
     * @param unidadeMedida
     * @return 
     */
    public default List<Object> converteEntidadeParaLinhaArquivo(UnidadeMedida unidadeMedida) {
                
        List<Object> linhaArquivo = new ArrayList<>();
        linhaArquivo.add(unidadeMedida.getId());
        linhaArquivo.add(unidadeMedida.getDescricao());
            
        return linhaArquivo;
        
    }

    
    
}
