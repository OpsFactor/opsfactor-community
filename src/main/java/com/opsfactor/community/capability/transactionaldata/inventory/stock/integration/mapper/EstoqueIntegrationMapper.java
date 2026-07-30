package com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.mapper;

import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper do arquivo/API de estoque inicial Community.
 *
 * <p>O contrato aceita somente location, material, data de referencia,
 * unidade de medida e quantidade. Campos de lote, validade, aging, writeoff ou
 * producao por batch devem ser modelados apenas no Enterprise.</p>
 */
@Component
public class EstoqueIntegrationMapper implements IntegrationMapperInterface<EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, Estoque, EstoqueIntegrationSupportData> {

    /**
     * Headers publicos do template de estoque Community. A lista e imutavel
     * para que o contrato de import/export nao mude em runtime.
     */
    public static final List<String> processedFileHeaders = List.of(
            "Location Id",
            "Material Id",
            "Reference Date (stock at the start of the reference date)",
            "Unit of Measure Id",
            "Quantity");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    /**
     * Converte entidade de estoque para DTO sem chave primaria.
     */
    @Override
    public EstoqueIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(Estoque entity) {
        
        return EstoqueIntegrationDataDto.builder()
                .uomId((entity.getUnidadeMedidaCadastrada() == null) ? null : entity.getUnidadeMedidaCadastrada().getId())
                .quantity(entity.getQuantidade())
                .build();
        
    }

    /**
     * Extrai a chave publica location/material/data do snapshot.
     */
    @Override
    public EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(Estoque entity) {

        return new EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO(
                entity.getLocation().getId(),
                entity.getProduto().getId(),
                entity.getDataReferencia());

    }

    /**
     * Cria novo snapshot de estoque com chave resolvida em support data.
     */
    @Override
    public Estoque createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO primaryKeyDto,
            EstoqueIntegrationSupportData supportData) {

        Location location = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                supportData.mapaLocationPorId,
                primaryKeyDto.locationId,
                true,
                new MissingDependencyDataUploadException("No Location found with id = " + primaryKeyDto.locationId, primaryKeyDto));
        Produto material = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                supportData.mapaProdutoPorId,
                primaryKeyDto.materialId,
                true,
                new MissingDependencyDataUploadException("No Material found with id = " + primaryKeyDto.materialId, primaryKeyDto));
        if (primaryKeyDto.referenceDate == null) throw new DataUploadException("Reference date is empty");

        return new Estoque(
                new Estoque.EstoqueCompositeKey(
                        location,
                        material,
                        primaryKeyDto.referenceDate),
                0.0);
        
    }

    /**
     * Atualiza UOM opcional e quantidade do snapshot.
     */
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            Estoque entity,
            EstoqueIntegrationDataDto dto,
            EstoqueIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {

        UnidadeMedida unidadeMedida = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                supportData.mapaUomPorId,
                dto.uomId,
                false,
                new MissingDependencyDataUploadException("No Unit of Measure found with id = " + dto.uomId, dto));

        entity.setUnidadeMedida(unidadeMedida);
        entity.setQuantidade(dto.quantity);
                
    }

    /**
     * Exporta o snapshot para a linha do template Community.
     */
    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(Estoque entity, EstoqueIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getLocation().getId());
        linhaArquivo.addContent(entity.getProduto().getId());
        linhaArquivo.addContent(entity.getDataReferencia());
        linhaArquivo.addContent((entity.getUnidadeMedidaCadastrada() == null) ? null : entity.getUnidadeMedidaCadastrada().getId());
        linhaArquivo.addContent(entity.getQuantidade());

        return linhaArquivo;

    }

    /**
     * Le a parte nao chave do DTO a partir das colunas do template.
     */
    @Override
    public EstoqueIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, EstoqueIntegrationSupportData supportData) {

        return EstoqueIntegrationDataDto.builder()
                .uomId(processedFileRow.getColumnValueAsString(3))
                .quantity(processedFileRow.getColumnValueAsDouble(4))
                .build();
        
    }

    /**
     * Le a chave location/material/data a partir das primeiras colunas do
     * template.
     */
    @Override
    public EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, EstoqueIntegrationSupportData supportData) {

        return new EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1),
                processedFileRow.getColumnValueAsLocalDateTime(2));

    }

}
