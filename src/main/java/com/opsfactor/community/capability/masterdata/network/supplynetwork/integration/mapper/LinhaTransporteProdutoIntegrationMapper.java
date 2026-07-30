package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte.LinhaTransporteCompositeKey;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper de override de malha por material no Community.
 *
 * <p>Este mapper complementa a transportation lane geral com parametros
 * especificos de material usados pelo heuristico: prioridade, lead time,
 * lotes, multiplo e status. Distancia por material alimenta capacidades
 * Enterprise de mapa/frete/analise de rede; por isso o campo permanece apenas
 * no DTO compartilhado para rejeicao defensiva e nao aparece em headers nem em
 * exports Community.</p>
 */
@Component
public class LinhaTransporteProdutoIntegrationMapper implements IntegrationMapperInterface<LinhaTransporteProdutoIntegrationDataDto, LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO,LinhaTransporteProduto, LinhaTransporteProdutoIntegrationSupportData> {

    /**
     * Headers publicos do template Community de transportation lane/material.
     * A lista e imutavel para preservar o contrato aberto da edicao.
     */
    public static final List<String> processedFileHeaders = List.of(
            "Supply Network Version Id",
            "Origin Location Id",
            "Destination Location Id",
            "Material Id",
            "Priority (0 = largest priority)",
            "Lead Time (Days)",
            "Multiple Minimum Transfer Lot Size UOM Id",
            "Minimum Transfer Lot Size",
            "Multiple Transfer",
            "Active : TRUE/FALSE or 1/0 (Default = True if empty)");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    /**
     * Converte a entidade para DTO omitindo a chave composta da lane/material.
     */
    @Override
    public LinhaTransporteProdutoIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(LinhaTransporteProduto entity) {

        /*
         * Nao transportar `distanceKm` da entidade para o DTO Community. A
         * coluna pode existir fisicamente em bases transicionais, mas sua
         * semantica e Enterprise e nao deve vazar para o template publico.
         */
        return LinhaTransporteProdutoIntegrationDataDto.builder()
                .priority(entity.getPrioridadeCadastrada())
                .leadTimeDays(entity.getLeadTimeDiasCadastrado())
                .multipleMinimumTransferLotSizeUomId((entity.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada() == null) ? null : entity.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada().getId())
                .minimumTransferLotSize(entity.getLoteMinimoTransporteCadastrado())
                .multipleTransfer(entity.getMultiploTransporteCadastrado())
                .active(entity.getAtivoCadastrado())
                .build();
    }

    @Override
    public LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            LinhaTransporteProduto linhaTransporteProduto) {

        return new LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO(
                linhaTransporteProduto.getLinhaTransporte().getLinhaTransporteCompositeKey().getVersaoMalha().getId(),
                linhaTransporteProduto.getLinhaTransporte().getLocationOrigem().getId(),
                linhaTransporteProduto.getLinhaTransporte().getLocationDestino().getId(),
                linhaTransporteProduto.getProduto().getId());

    }

    /**
     * Cria a entidade lane/material com dependencias resolvidas no support data
     * do batch.
     */
    @Override
    public LinhaTransporteProduto createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO dto, LinhaTransporteProdutoIntegrationSupportData supportData) {
        
        VersaoMalha versaoMalha = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(supportData.mapaVersaoMalhaPorId, 
                dto.supplyNetworkVersionId, 
                false, 
                new MissingDependencyDataUploadException("Supply Network Version " + dto.supplyNetworkVersionId + " not found", dto));
        
        Location locationOrigem = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(supportData.mapaLocationOrigemPorId, 
                dto.originLocationId, 
                false, 
                new MissingDependencyDataUploadException("Origin Location " + dto.originLocationId + " not found", dto));
        
        Location locationDestino = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(supportData.mapaLocationDestinoPorId, 
                dto.destinationLocationId, 
                false, 
                new MissingDependencyDataUploadException("Destination Location " + dto.destinationLocationId + " not found", dto));
        
        LinhaTransporteCompositeKey linhaTransporteCompositeKey = new LinhaTransporte.LinhaTransporteCompositeKey(versaoMalha, locationOrigem, locationDestino);
        LinhaTransporte linhaTransporte = new LinhaTransporte(linhaTransporteCompositeKey);
        
        Produto produto = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(supportData.mapaProdutoPorId, 
                dto.materialId, 
                false, 
                new MissingDependencyDataUploadException("Material " + dto.materialId + " not found", dto));
        
        LinhaTransporteProdutoCompositeKey linhaTransporteProdutoCompositeKey = new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(linhaTransporte, produto);
        LinhaTransporteProduto linhaTransporteProduto = new LinhaTransporteProduto(linhaTransporteProdutoCompositeKey);
        
        return linhaTransporteProduto;
        
    }

    /**
     * Atualiza parametros operacionais da lane/material Community.
     */
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            LinhaTransporteProduto entity, 
            LinhaTransporteProdutoIntegrationDataDto dto,
            LinhaTransporteProdutoIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        entity.setPrioridade(dto.priority);
        entity.setLeadTimeDias(dto.leadTimeDays);
        /*
         * Distancia por material e Enterprise. Mantemos o campo no DTO para
         * compatibilidade de contrato, mas qualquer valor preenchido deve
         * falhar no Community em vez de ser ignorado silenciosamente.
         */
        if (dto.distanceKm != null) {
            throw new RequiresEnterpriseVersionException("Transportation lane material distance");
        }
        
        if (dto.multipleMinimumTransferLotSizeUomId != null) {
            if (dto.multipleMinimumTransferLotSizeUomId.equalsIgnoreCase("null")) {
                dto.multipleMinimumTransferLotSizeUomId = null;
            }
        }
        UnidadeMedida unidadeMedidaLoteMinimoMultiploTransporte = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(supportData.mapaUomLoteMinimoMultiploTransportePorId, 
                dto.multipleMinimumTransferLotSizeUomId, 
                false, 
                new MissingDependencyDataUploadException("Multiple Minimum Transfer Lot Size UoM " + dto.multipleMinimumTransferLotSizeUomId + " not found", dto));
        entity.setUnidadeMedidaLoteMinimoMultiploTransporte(unidadeMedidaLoteMinimoMultiploTransporte);
        
        entity.setLoteMinimoTransporte(dto.minimumTransferLotSize);
        entity.setMultiploTransporte(dto.multipleTransfer);
        
        entity.setAtivo(dto.active);
        
    }

    /**
     * Exporta a lane/material para o template Community.
     */
    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(LinhaTransporteProduto entity, LinhaTransporteProdutoIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getVersaoMalha().getId());
        linhaArquivo.addContent(entity.getLocationOrigem().getId());
        linhaArquivo.addContent(entity.getLocationDestino().getId());
        linhaArquivo.addContent(entity.getProduto().getId());
        linhaArquivo.addContent(entity.getPrioridadeCadastrada());
        linhaArquivo.addContent(entity.getLeadTimeDiasCadastrado());
        linhaArquivo.addContent((entity.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada() == null) ? null : entity.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada().getId());
        linhaArquivo.addContent(entity.getLoteMinimoTransporteCadastrado());
        linhaArquivo.addContent(entity.getMultiploTransporteCadastrado());
        linhaArquivo.addContent(entity.getAtivoCadastrado());
                
        return linhaArquivo;
        
    }

    /**
     * Le os campos nao chave da lane/material a partir do template.
     */
    @Override
    public LinhaTransporteProdutoIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, LinhaTransporteProdutoIntegrationSupportData supportData) {

        return LinhaTransporteProdutoIntegrationDataDto.builder()
                .priority(processedFileRow.getColumnValueAsInteger(4))
                .leadTimeDays(processedFileRow.getColumnValueAsInteger(5))
                .multipleMinimumTransferLotSizeUomId(processedFileRow.getColumnValueAsString(6))
                .minimumTransferLotSize(processedFileRow.getColumnValueAsDouble(7))
                .multipleTransfer(processedFileRow.getColumnValueAsDouble(8))
                .active(processedFileRow.getColumnValueAsBoolean(9))
                .build();

    }

    /**
     * Le a chave versao/origem/destino/material a partir das primeiras colunas
     * do template.
     */
    @Override
    public LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, LinhaTransporteProdutoIntegrationSupportData supportData) {

        return new LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1),
                processedFileRow.getColumnValueAsString(2),
                processedFileRow.getColumnValueAsString(3));

    }

}
