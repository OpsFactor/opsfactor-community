package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeProdutoIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidadeProduto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper Community para conversoes especificas de UOM por material.
 *
 * <p>Conversoes por material sao usadas pelos planning books e projections para
 * quantidade fisica. O contrato de arquivo nao inclui preco, custo, embalagem
 * logistica ou qualquer dado de distribution/visibility Enterprise.</p>
 */
@Component
public class ConversaoUnidadeProdutoIntegrationMapper implements IntegrationMapperInterface<ConversaoUnidadeProdutoIntegrationDataDto, ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO, ConversaoUnidadeProduto, ConversaoUnidadeProdutoIntegrationSupportData> {

    /**
     * Headers publicados para conversoes de UOM por material no Community.
     */
    public static final List<String> processedFileHeaders = List.of(
        "Material Id",
        "Origin Unit of Measure Id",
        "Target Unit of Measure Id",
        "Origin Quantity",
        "Target Quantity");

    /**
     * Retorna a ordem oficial das colunas processadas em arquivo.
     */
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }
    
    @Override
    public ConversaoUnidadeProdutoIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            ConversaoUnidadeProduto entity) {

        return ConversaoUnidadeProdutoIntegrationDataDto.builder()
                .originQuantity(entity.getQuantidadeUnidadeOrigemCadastrado())
                .targetQuantity(entity.getQuantidadeUnidadeDestinoCadastrado())
                .build();

    }

    @Override
    public ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(ConversaoUnidadeProduto entity) {
        return new ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO(
                entity.getProduto().getId(),
                entity.getUnidadeMedidaOrigem().getId(),
                entity.getUnidadeMedidaDestino().getId());
    }

    @Override
    public ConversaoUnidadeProduto createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO dto,
            ConversaoUnidadeProdutoIntegrationSupportData supportData) {

        Produto material = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                supportData.materialPorId,
                dto.materialId,
                true, // material é obrigatório
                new MissingDependencyDataUploadException("Material " + dto.materialId + " not found", dto));
        UnidadeMedida unidadeMedidaOrigem = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                supportData.uomPorId,
                dto.originUomId,
                true, // uom é obrigatória
                new MissingDependencyDataUploadException("Origin UOM " + dto.originUomId + " not found", dto));
        UnidadeMedida unidadeMedidaDestino = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                supportData.uomPorId,
                dto.targetUomId,
                true, // uom é obrigatória
                new MissingDependencyDataUploadException("Target UOM " + dto.targetUomId + " not found", dto));

        return new ConversaoUnidadeProduto(
                new ConversaoUnidadeProduto.ConversaoUnidadeProdutoCompositeKey(
                        material,
                        unidadeMedidaOrigem,
                        unidadeMedidaDestino));
        
    }
    
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            ConversaoUnidadeProduto entity,
            ConversaoUnidadeProdutoIntegrationDataDto dto,
            ConversaoUnidadeProdutoIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {

        entity.setQuantidadeUnidadeOrigem(dto.originQuantity);
        entity.setQuantidadeUnidadeDestino(dto.targetQuantity);
        /*
         * A integracao canonica por material tambem encerra a compatibilidade
         * por linha: o par recebido passa a ser a unica fonte da razao.
         */
        entity.setQuantidadeUnidadeDestinoPorUnidadeOrigem(null);

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(ConversaoUnidadeProduto entity, ConversaoUnidadeProdutoIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getProduto().getId());
        linhaArquivo.addContent(entity.getUnidadeMedidaOrigem().getId());
        linhaArquivo.addContent(entity.getUnidadeMedidaDestino().getId());
        linhaArquivo.addContent(entity.getQuantidadeUnidadeOrigemCadastrado());
        linhaArquivo.addContent(entity.getQuantidadeUnidadeDestinoCadastrado());
        
        return linhaArquivo;

    }

    @Override
    public ConversaoUnidadeProdutoIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, ConversaoUnidadeProdutoIntegrationSupportData supportData) {

        ConversaoUnidadeProdutoIntegrationDataDto dto = ConversaoUnidadeProdutoIntegrationDataDto.builder()
                .originQuantity(processedFileRow.getColumnValueAsDouble(3))
                .targetQuantity(processedFileRow.getColumnValueAsDouble(4))
                .build();
        return dto;

    }

    @Override
    public ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, ConversaoUnidadeProdutoIntegrationSupportData supportData) {
        return new ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1),
                processedFileRow.getColumnValueAsString(2));
    }

}
