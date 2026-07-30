package com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.mapper;

import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto.SelloutIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper do arquivo/API de vendas historicas Community.
 *
 * <p>As colunas exportadas/importadas sao intencionalmente minimas: documento,
 * data, location, material, UOM e quantidade. O mapper nao conhece valores,
 * precos, sell-in, pedidos ou eventos.</p>
 */
@Component
public class SelloutIntegrationMapper implements IntegrationMapperInterface<SelloutIntegrationDataDto, SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO,Sellout, SelloutIntegrationSupportData> {

    /**
     * Headers publicos do template sell-out Community. A lista e imutavel para
     * evitar que um fluxo de upload altere o contrato compartilhado da edicao.
     */
    public static final List<String> processedFileHeaders = List.of(
            "Document Id",
            "Reference Date",
            "Origin Location Id",
            "Material Id",
            "Unit of Measure Id",
            "Quantity");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    /**
     * Converte entidade sell-out para DTO sem a chave primaria do documento.
     */
    @Override
    public SelloutIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(Sellout entity) {
        
        return SelloutIntegrationDataDto.builder()
                .referenceDate(entity.getDataVenda())
                .originLocationId(entity.getLocationOrigem().getId())
                .materialId(entity.getProduto().getId())
                .uomId((entity.getUnidadeMedidaCadastrada() == null) ? null : entity.getUnidadeMedidaCadastrada().getId())
                .quantity(entity.getQuantidade())
                .build();
        
    }

    /**
     * Extrai a chave publica do documento sell-out.
     */
    @Override
    public SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(Sellout sellout) {

        return new SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO(
                sellout.getId());

    }

    /**
     * Cria entidade vazia com a chave do documento. Os campos restantes sao
     * preenchidos em {@link #updateEntityNonPrimaryFieldsFromDTO}.
     */
    @Override
    public Sellout createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO dto,
            SelloutIntegrationSupportData supportData) {
        
        Sellout sellout = new Sellout();
        sellout.setId(dto.documentId);
        
        return sellout;
        
    }

    /**
     * Atualiza campos nao chave do sell-out.
     *
     * <p>Location e material sao obrigatorios; UOM e opcional para manter
     * compatibilidade com cargas historicas cuja unidade pode ser inferida
     * depois por conversoes de material.</p>
     */
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            Sellout entity,
            SelloutIntegrationDataDto dto,
            SelloutIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        entity.setDataVenda(dto.referenceDate);
        entity.setQuantidade(dto.quantity);
        
        /*
         * Se o valor do DTO for nulo em campo opcional, o helper devolve nulo;
         * se for obrigatorio ou informado e inexistente, falha com a excecao de
         * dependencia ausente contendo o proprio DTO.
         */
        entity.setLocationOrigem(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaLocationPorId, 
                        dto.originLocationId,
                        true, // campo obrigatório. não pode ser nulo
                        new MissingDependencyDataUploadException("Location " + dto.originLocationId + " not found", dto)));
        entity.setProduto(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaMaterialPorId, 
                        dto.materialId,
                        true, // campo obrigatório. não pode ser nulo
                        new MissingDependencyDataUploadException("Material " + dto.materialId + " not found", dto)));
        entity.setUnidadeMedida(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaUnidadeMedidaPorId, 
                        dto.uomId,
                        false, // campo não obrigatório. pode ser nulo
                        new MissingDependencyDataUploadException("Unit of Measure " + dto.uomId + " not found", dto)));
                        
    }

    /**
     * Exporta a entidade para a linha do template Community.
     */
    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(Sellout entity, SelloutIntegrationSupportData supportData) {
        
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getId());
        linhaArquivo.addContent(entity.getDataVenda());
        linhaArquivo.addContent(entity.getLocationOrigem().getId());
        linhaArquivo.addContent(entity.getProduto().getId());
        linhaArquivo.addContent((entity.getUnidadeMedidaCadastrada() == null) ? null : entity.getUnidadeMedidaCadastrada().getId());
        linhaArquivo.addContent(entity.getQuantidade());
                
        return linhaArquivo;

    }

    /**
     * Le a parte nao chave do DTO a partir das colunas do template.
     */
    @Override
    public SelloutIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, SelloutIntegrationSupportData supportData) {

        return SelloutIntegrationDataDto.builder()
                .referenceDate(processedFileRow.getColumnValueAsLocalDateTime(1))
                .originLocationId(processedFileRow.getColumnValueAsString(2))
                .materialId(processedFileRow.getColumnValueAsString(3))
                .uomId(processedFileRow.getColumnValueAsString(4))
                .quantity(processedFileRow.getColumnValueAsDouble(5))
                .build();

    }

    /**
     * Le a chave do documento sell-out a partir da primeira coluna do template.
     */
    @Override
    public SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, SelloutIntegrationSupportData supportData) {

        return new SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));

    }

}
