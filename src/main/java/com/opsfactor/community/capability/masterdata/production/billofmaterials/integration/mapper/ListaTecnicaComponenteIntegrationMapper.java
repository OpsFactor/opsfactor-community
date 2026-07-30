package com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto.ListaTecnicaComponenteIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente.ListaTecnicaComponenteCompositeKey;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper dos componentes de lista tecnica Community.
 *
 * <p>O contrato possui apenas BOM, material componente, UOM e quantidade. Regras
 * de substituicao, perdas economicas, custos e line scheduling nao fazem parte
 * da carga Community.</p>
 */
@Component
public class ListaTecnicaComponenteIntegrationMapper implements IntegrationMapperInterface<ListaTecnicaComponenteIntegrationDataDto, ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO,ListaTecnicaComponente, ListaTecnicaComponenteIntegrationSupportData> {

    /**
     * Headers publicados para componentes da BOM Community.
     *
     * <p>O contrato contem apenas o material componente, sua UOM e quantidade.
     * Colunas de substituicao, perdas, custos ou uso em line scheduling devem
     * ser adicionadas somente em mappers Enterprise.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
        "Bill of Materials Id",
        "Component Material Id",
        "Component Material Quantity Unit of Measure Id",
        "Component Material Quantity");

    /**
     * Retorna a ordem oficial das colunas processadas em arquivo.
     */
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public ListaTecnicaComponenteIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(ListaTecnicaComponente entity) {
        
        return ListaTecnicaComponenteIntegrationDataDto.builder()
                .componentMaterialQuantityUomId((entity.getUnidadeMedidaMaterialComponenteCadastrada() == null) ? null : entity.getUnidadeMedidaMaterialComponenteCadastrada().getId())
                .componentMaterialQuantity(entity.getQuantidade())
                .build();
        
    }

    @Override
    public ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            ListaTecnicaComponente listaTecnicaComponente) {
        return new ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO(
                listaTecnicaComponente.getListaTecnica().getId(),
                listaTecnicaComponente.getMaterialComponente().getId());
    }

    @Override
    public ListaTecnicaComponente createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO dto,
            ListaTecnicaComponenteIntegrationSupportData supportData) {
        
        ListaTecnicaComponente entity = new ListaTecnicaComponente();
        entity.setListaTecnicaComponenteCompositeKey(new ListaTecnicaComponenteCompositeKey(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaListaTecnicaPorId, 
                        dto.bomId, 
                        true, // bom é obrigatória
                        new MissingDependencyDataUploadException("Bill of Materials " + dto.bomId + " not found", dto)),
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaMaterialPorId, 
                        dto.componentMaterialId, 
                        true, // material é obrigatório
                        new MissingDependencyDataUploadException("Material " + dto.componentMaterialId + " not found", dto))
        ));
        
        return entity;
        
    }
    
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            ListaTecnicaComponente entity,
            ListaTecnicaComponenteIntegrationDataDto dto,
            ListaTecnicaComponenteIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        // seta campos simples
        entity.setQuantidade(dto.componentMaterialQuantity);
        
        entity.setUnidadeMedidaMaterialComponente(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaUnidadeMedidaPorId, 
                        dto.componentMaterialQuantityUomId, 
                        false, // unidade de medida não é obrigatória
                        new MissingDependencyDataUploadException("Unit of Measure " + dto.componentMaterialQuantityUomId + " not found", dto)));
                
    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(ListaTecnicaComponente entity, ListaTecnicaComponenteIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getListaTecnica().getId());
        linhaArquivo.addContent(entity.getMaterialComponente().getId());
        linhaArquivo.addContent((entity.getUnidadeMedidaMaterialComponenteCadastrada() == null) ? null : entity.getUnidadeMedidaMaterialComponenteCadastrada().getId());
        linhaArquivo.addContent(entity.getQuantidade());
                
        return linhaArquivo;

    }

    @Override
    public ListaTecnicaComponenteIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, ListaTecnicaComponenteIntegrationSupportData supportData) {
        return ListaTecnicaComponenteIntegrationDataDto.builder()
                .componentMaterialQuantityUomId(processedFileRow.getColumnValueAsString(2))
                .componentMaterialQuantity(processedFileRow.getColumnValueAsFloat(3))
                .build();
    }

    @Override
    public ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, ListaTecnicaComponenteIntegrationSupportData supportData) {
        return new ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1));
    }

}
