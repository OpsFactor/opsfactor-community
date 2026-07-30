package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidade;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper Community para conversoes globais entre unidades de medida.
 *
 * <p>Este cadastro e operacional e fica no Community porque Demand/Supply
 * Planning precisam converter quantidades fisicas sem depender de capabilities
 * Enterprise. A carga contem apenas UOM origem, UOM destino e a razao de
 * conversao.</p>
 */
@Component
public class ConversaoUnidadeIntegrationMapper implements IntegrationMapperInterface<ConversaoUnidadeIntegrationDataDto, ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO, ConversaoUnidade, ConversaoUnidadeIntegrationSupportData> {

    /**
     * Headers publicados para conversoes globais de UOM no Community.
     */
    public static final List<String> processedFileHeaders = List.of(
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
    public ConversaoUnidadeIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            ConversaoUnidade entity) {
        return ConversaoUnidadeIntegrationDataDto.builder()
                .originQuantity(entity.getQuantidadeUnidadeOrigemCadastrado())
                .targetQuantity(entity.getQuantidadeUnidadeDestinoCadastrado())
                .build();
    }

    @Override
    public ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(ConversaoUnidade entity) {
        return new ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO(
                entity.getUnidadeMedidaOrigem().getId(),
                entity.getUnidadeMedidaDestino().getId());
    }

    @Override
    public ConversaoUnidade createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO dto,
            ConversaoUnidadeIntegrationSupportData supportData) {

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

        return new ConversaoUnidade(
                new ConversaoUnidade.ConversaoUnidadeCompositeKey(
                        unidadeMedidaOrigem,
                        unidadeMedidaDestino));
        
    }
    
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            ConversaoUnidade entity,
            ConversaoUnidadeIntegrationDataDto dto,
            ConversaoUnidadeIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {

        entity.setQuantidadeUnidadeOrigem(dto.originQuantity);
        entity.setQuantidadeUnidadeDestino(dto.targetQuantity);
        /*
         * O upload Community publica somente o par canonico. Limpar a coluna
         * depreciada impede que uma alteracao posterior escolha silenciosamente
         * uma razao antiga em vez dos valores que acabaram de ser enviados.
         */
        entity.setQuantidadeUnidadeDestinoPorUnidadeOrigem(null);

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(ConversaoUnidade entity, ConversaoUnidadeIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getUnidadeMedidaOrigem().getId());
        linhaArquivo.addContent(entity.getUnidadeMedidaDestino().getId());
        linhaArquivo.addContent(entity.getQuantidadeUnidadeOrigemCadastrado());
        linhaArquivo.addContent(entity.getQuantidadeUnidadeDestinoCadastrado());
        
        return linhaArquivo;

    }

    @Override
    public ConversaoUnidadeIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, ConversaoUnidadeIntegrationSupportData supportData) {

        ConversaoUnidadeIntegrationDataDto dto = ConversaoUnidadeIntegrationDataDto.builder()
                .originQuantity(processedFileRow.getColumnValueAsDouble(2))
                .targetQuantity(processedFileRow.getColumnValueAsDouble(3))
                .build();
        return dto;

    }

    @Override
    public ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, ConversaoUnidadeIntegrationSupportData supportData) {
        return new ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1));
    }

}
