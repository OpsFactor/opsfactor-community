package com.opsfactor.community.platform.integration.mapper;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contrato comum dos mappers de data upload/download Community.
 *
 * <p>`SUPPORTDATA` concentra colecoes ja carregadas de entidades persistidas
 * para que a criacao/merge dos registros nao dispare buscas repetidas no banco
 * durante cargas grandes. Cada mapper especifico define quais mapas de apoio
 * sao obrigatorios.</p>
 *
 * <p>Implementacoes Community devem expor headers estaticos e imutaveis sempre
 * que o arquivo nao possuir colunas dinamicas. Mappers Enterprise podem
 * sobrescrever os pontos de extensao em outro modulo, mas a interface base nao
 * deve carregar conhecimento de capabilities privadas.</p>
 */
public interface IntegrationMapperInterface<DTO extends IntegrationDataDtoAbstract<DTO, PRIMARYKEYDTO, ENTITY>, PRIMARYKEYDTO extends IntegrationPrimaryKeyDTOAbstract<PRIMARYKEYDTO, ENTITY>, ENTITY, SUPPORTDATA> {

    public DTO getDtoWithoutPrimaryKeyFromEntity(ENTITY entity);
    public PRIMARYKEYDTO getPrimaryKeyDtoFromEntity(ENTITY entity);
    public DTO getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, SUPPORTDATA supportData); // SUPPORTDATA é usado para se indicar o número de colunas target do arquivo em arquivos com colunas dinâmicas (ex. produto)
    public PRIMARYKEYDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, SUPPORTDATA supportData); // SUPPORTDATA é usado para se indicar o número de colunas target do arquivo em arquivos com colunas dinâmicas (ex. produto)

    public enum MetodoAtualizacaoCampo {
        @JsonProperty("Update") 
        @JsonAlias({"update", "UPDATE"})
        UPDATE,
        @JsonProperty("Ignore") 
        @JsonAlias({"ignore", "IGNORE"})
        IGNORE
    }
    

    public default DTO convertEntityToDTO(ENTITY entity) {
        DTO dto = getDtoWithoutPrimaryKeyFromEntity(entity);
        dto.primaryKeyDto = getPrimaryKeyDtoFromEntity(entity);
        return dto;
    }
    public default DTO convertProcessedFileRowToDTO(ProcessedFileRow processedFileRow, SUPPORTDATA supportData) {
        DTO dto = getDtoWithoutPrimaryKeyFromProcessedFileRow(processedFileRow, supportData);
        dto.primaryKeyDto = getPrimaryKeyDtoFromProcessedFileRow(processedFileRow, supportData);
        dto.delete = processedFileRow.getColumnValueAsString(getDeleteProcessedFileRowPosition(supportData));
        return dto;
    }

    public default int getDeleteProcessedFileRowPosition(SUPPORTDATA supportData) {
        return getProcessedFileHeaders().size();
    }

    /**
     * Headers funcionais do `ProcessedFile`, sem considerar a coluna tecnica
     * `Delete`.
     */
    public List<String> getProcessedFileHeaders();

    /**
     * Linhas de cabecalho do arquivo processado, sem considerar a coluna
     * tecnica `Delete`.
     *
     * <p>O default cobre mappers com uma unica linha de cabecalho. Mappers com
     * colunas dinamicas ou cabecalhos multi-linha devem sobrescrever este
     * metodo e documentar quais dados de `supportData` controlam a montagem.</p>
     */
    public default List<ProcessedFileRow> getFileHeaderRows(SUPPORTDATA supportData) {

        ProcessedFileRow processedFileRow = new ProcessedFileRow();

        // adiciona colunas-base ao header
        for (String nomeHeader : getProcessedFileHeaders()) {
            processedFileRow.addContent(nomeHeader);
        }

        // retorna lista de 1 só elemento (apenas 1 linha cabeçalho)
        return List.of(processedFileRow);

    }

    public default List<DTO> convertEntityCollectionToDTOList(Collection<ENTITY> entityList) {
        return entityList.stream()
                .map(entity -> convertEntityToDTO(entity))
                .collect(Collectors.toList());
    }
    public default List<DTO> convertProcessedFileToDTOList(ProcessedFile processedFile, SUPPORTDATA supportData) {

        return processedFile.getDTOListFromProcessedFile(
                getNumberOfFileHeaderRows(supportData),
                getNumberOfFileColumns(supportData) + 1, // considera uma coluna adicional para 'delete'
                processedFileRow -> convertProcessedFileRowToDTO(processedFileRow, supportData));

    }

    /**
     * Converte o DTO para uma entidade (já persistida ou nova), realizando o merge quando houver diferença nos atributos
     * @param currentlyPersistedEntitiesByPrimaryKey mapa de todas as entidades usadas no save para não se precisar realizar
     * buscas sucessivas no banco de dados. Após operações de remove e save por batch, esse mapa será atualizado com os
     * elementos removidos ou inseridos
     */
    public default ENTITY convertDTOToEntity(
            DTO dto, 
            Map<PRIMARYKEYDTO, ENTITY> currentlyPersistedEntitiesByPrimaryKey,
            SUPPORTDATA supportData, 
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {

        PRIMARYKEYDTO primaryKeyDto = dto.primaryKeyDto;
        ENTITY entity = Optional.ofNullable(currentlyPersistedEntitiesByPrimaryKey.get(primaryKeyDto))
                .orElse(createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(primaryKeyDto, supportData));
        
        updateEntityNonPrimaryFieldsFromDTO(entity, dto, supportData, camposASobrecrever);
        
        return entity;
        
    }
    
    public ENTITY createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(PRIMARYKEYDTO primaryKeyDto, SUPPORTDATA supportData);
    
    public void updateEntityNonPrimaryFieldsFromDTO(
            ENTITY entity, 
            DTO dto, 
            SUPPORTDATA supportData, 
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever);

    public default boolean isUpdateableField(String field, @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        if (camposASobrecrever == null) return true;
        
        MetodoAtualizacaoCampo metodoAtualizacaoCampo = camposASobrecrever.get(field.toLowerCase());
        if (metodoAtualizacaoCampo == null) metodoAtualizacaoCampo = camposASobrecrever.get(field);
        
        if (metodoAtualizacaoCampo == null) return true;
        return metodoAtualizacaoCampo.equals(MetodoAtualizacaoCampo.UPDATE);
    }

    public ProcessedFileRow convertEntityToProcessedFileRow(ENTITY entity, SUPPORTDATA supportData);
    
    public default ProcessedFile convertEntityCollectionToProcessedFile(Collection<ENTITY> persistedEntityCollection, SUPPORTDATA supportData) {
        
        ProcessedFile processedFile = new ProcessedFile();
        
        processedFile.addRows(getFileHeaderRows(supportData));
        
        for (ENTITY entity : persistedEntityCollection) {
            processedFile.addRow(
                    convertEntityToProcessedFileRow(entity, supportData));
        }
        
        return processedFile;
        
    }
    
    public default int getNumberOfFileHeaderRows(SUPPORTDATA supportData) {
        return getFileHeaderRows(supportData).size();
    }
    /**
     * Número de colunas no arquivo excel, excluindo coluna 'delete'
     * @param supportData
     * @return 
     */
    public default int getNumberOfFileColumns(SUPPORTDATA supportData) {
        return getFileHeaderRows(supportData).stream()
                .mapToInt(processedFileRow -> processedFileRow.getRowSize())
                .max().orElse(0);
    }

}
