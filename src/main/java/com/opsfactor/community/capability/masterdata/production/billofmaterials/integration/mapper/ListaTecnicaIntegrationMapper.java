package com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper de listas tecnicas operacionais Community.
 *
 * <p>Exporta/importa apenas o cabecalho da BOM simples usado pelo Supply
 * Planning heuristico. Estruturas de co-produto, outputs paralelos e
 * informacoes economicas sao recortes Enterprise.</p>
 */
@Component
public class ListaTecnicaIntegrationMapper implements IntegrationMapperInterface<ListaTecnicaIntegrationDataDto, ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO,ListaTecnica, ListaTecnicaIntegrationSupportData> {

    /**
     * Headers publicados para a BOM simples Community.
     *
     * <p>A lista fica imutavel para que exportacao, importacao e documentacao
     * OpenAPI conversem sobre a mesma superficie operacional minima.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
        "Bill of Materials Id",
        "Description",
        "Location Id",
        "Output Material Id",
        "Output Quantity",
        "Output Unit of Measure Id",
        "Priority",
        "Active (true/false or 1/0)",
        "Bill of Materials can be used without production version");

    /**
     * Retorna a ordem oficial das colunas processadas em arquivo.
     */
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public ListaTecnicaIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(ListaTecnica entity) {
        
        return ListaTecnicaIntegrationDataDto.builder()
                .description(entity.getDescricao())
                .locationId(entity.getLocation().getId())
                .outputMaterialId(entity.getMaterialOutput().getId())
                .outputUomId((entity.getUnidadeMedidaMaterialOutputCadastrada() == null) ? null : entity.getUnidadeMedidaMaterialOutputCadastrada().getId())
                .outputQuantity(entity.getQuantidade())
                .priority(entity.getPrioridadeCadastrada())
                .active(entity.getAtivoCadastrado())
                .canBeUsedWithoutProductionVersion(entity.getHabilitadoParaUsoSemVersaoProducaoCadastrado())
                .build();
        
    }

    @Override
    public ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(ListaTecnica listaTecnica) {
        return new ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO(
                listaTecnica.getId());
    }

    @Override
    public ListaTecnica createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO dto,
            ListaTecnicaIntegrationSupportData supportData) {
        
        ListaTecnica entity = new ListaTecnica();
        entity.setId(dto.id);
        
        return entity;
        
    }
    
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            ListaTecnica entity,
            ListaTecnicaIntegrationDataDto dto,
            ListaTecnicaIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        // seta campos simples
        entity.setDescricao(dto.description);
        entity.setAtivo(dto.active);
        entity.setQuantidade(dto.outputQuantity);
        entity.setPrioridade(dto.priority);
        entity.setHabilitadoParaUsoSemVersaoProducao(dto.canBeUsedWithoutProductionVersion);
        
        entity.setLocation(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaLocationPorId, 
                        dto.locationId, 
                        true, // location é obrigatória
                        new MissingDependencyDataUploadException("Location " + dto.locationId + " not found", dto)));
        entity.setMaterialOutput(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaMaterialPorId, 
                        dto.outputMaterialId, 
                        true, // material output é obrigatório
                        new MissingDependencyDataUploadException("Material " + dto.outputMaterialId + " not found", dto)));
        entity.setUnidadeMedidaMaterialOutput(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaUnidadeMedidaPorId, 
                        dto.outputUomId, 
                        false, // unidade de medida não é obrigatória
                        new MissingDependencyDataUploadException("Unit of Measure " + dto.outputUomId + " not found", dto)));
                
    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(ListaTecnica entity, ListaTecnicaIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getId());
        linhaArquivo.addContent(entity.getDescricao());
        linhaArquivo.addContent(entity.getLocation().getId());
        linhaArquivo.addContent(entity.getMaterialOutput().getId());
        linhaArquivo.addContent(entity.getQuantidade());
        linhaArquivo.addContent((entity.getUnidadeMedidaMaterialOutputCadastrada() == null) ? null : entity.getUnidadeMedidaMaterialOutputCadastrada().getId());
        linhaArquivo.addContent(entity.getPrioridadeCadastrada());
        linhaArquivo.addContent(entity.getAtivoCadastrado());
        linhaArquivo.addContent(entity.getHabilitadoParaUsoSemVersaoProducaoCadastrado());
                
        return linhaArquivo;

    }

    @Override
    public ListaTecnicaIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, ListaTecnicaIntegrationSupportData supportData) {
        
        return ListaTecnicaIntegrationDataDto.builder()
                .description(processedFileRow.getColumnValueAsString(1))
                .locationId(processedFileRow.getColumnValueAsString(2))
                .outputMaterialId(processedFileRow.getColumnValueAsString(3))
                .outputQuantity(processedFileRow.getColumnValueAsDouble(4))
                .outputUomId(processedFileRow.getColumnValueAsString(5))
                .priority(processedFileRow.getColumnValueAsInteger(6))
                .active(processedFileRow.getColumnValueAsBoolean(7))
                /*
                 * A coluna nova fica no fim para que arquivos Community com
                 * oito colunas preservem Active na posicao original.
                 */
                .canBeUsedWithoutProductionVersion(
                        processedFileRow.getRowSize() > 8
                                ? processedFileRow.getColumnValueAsBoolean(8)
                                : null)
                .build();
        
    }

    @Override
    public ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, ListaTecnicaIntegrationSupportData supportData) {
        return new ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));
    }

}
