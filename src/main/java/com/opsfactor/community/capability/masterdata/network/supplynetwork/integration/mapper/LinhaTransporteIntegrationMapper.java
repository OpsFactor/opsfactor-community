package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte.LinhaTransporteCompositeKey;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
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
 * Mapper de malha operacional Community.
 *
 * <p>A edicao Community usa a transportation lane apenas como entrada tecnica
 * do heuristico: origem, destino, prioridade, lead time, lotes e flags basicas
 * de elegibilidade de materiais. Distancia, frete, mapa, baricentro e analises
 * de rede pertencem ao Enterprise. Por isso `distanceKm` pode existir no DTO
 * compartilhado, mas qualquer valor preenchido e rejeitado antes da persistencia
 * e nunca aparece no template/export Community.</p>
 */
@Component
public class LinhaTransporteIntegrationMapper implements IntegrationMapperInterface<LinhaTransporteIntegrationDataDto, LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO,LinhaTransporte, LinhaTransporteIntegrationSupportData> {

    /**
     * Headers publicos do template Community de transportation lanes.
     *
     * <p>A lista e imutavel para impedir que uma rotina de import/export altere
     * em runtime o contrato aberto da edicao.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
            "Supply Network Version Id",
            "Origin Location Id",
            "Destination Location Id",
            "Priority (0 = largest priority)",
            "Lead Time (Days)",
            "Enable Transportation Line for Discontinued Materials (TRUE/FALSE or 1/0)",
            "Enable Transportation Line for Materials not yet launched (TRUE/FALSE or 1/0)",
            "Enable Transportation Line for All Materials (TRUE/FALSE or 1/0)",
            "Multiple Minimum Transfer Lot Size UOM Id",
            "Minimum Transfer Lot Size",
            "Multiple Transfer",
            "Active : TRUE/FALSE or 1/0 (Default = True if empty)");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    /**
     * Converte a entidade para DTO omitindo a chave origem/destino/versao.
     */
    @Override
    public LinhaTransporteIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(LinhaTransporte entity) {

        /*
         * A entidade pode trazer distancia de bases Enterprise/transicionais,
         * mas a API Community sempre neutraliza esse campo. O export XLSX/CSV
         * abaixo tambem omite a coluna para nao publicar capacidade de mapa ou
         * frete nesta edicao.
         */
        return LinhaTransporteIntegrationDataDto.builder()
                .priority(entity.getPrioridadeCadastrada())
                .leadTimeDays(entity.getLeadTimeDiasCadastrado())
                .distanceKm(null)
                .enableDiscontinuedMaterials(entity.getHabilitadoProdutosDescontinuadosCadastrado())
                .enablePresalesMaterials(entity.getHabilitadoProdutosNaoLancadosCadastrado())
                .enableAllMaterials(entity.getHabilitadoProdutosNaoCadastradosLinhaTransporteCadastrado())
                .multipleMinimumTransferLotSizeUomId((entity.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada() == null) ? null : entity.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada().getId())
                .minimumTransferLotSize(entity.getLoteMinimoTransporteCadastrado())
                .multipleTransfer(entity.getMultiploTransporteCadastrado())
                .active(entity.getAtivoCadastrado())
                .build();
    }

    @Override
    public LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            LinhaTransporte linhaTransporte) {

        return new LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO(
                linhaTransporte.getLinhaTransporteCompositeKey().getVersaoMalha().getId(),
                linhaTransporte.getLocationOrigem().getId(),
                linhaTransporte.getLocationDestino().getId());

    }

    /**
     * Cria a lane origem/destino com dependencias resolvidas a partir dos mapas
     * de support data do batch.
     */
    @Override
    public LinhaTransporte createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO dto,
            LinhaTransporteIntegrationSupportData supportData) {
        
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
        
        return linhaTransporte;
        
    }

    /**
     * Atualiza parametros operacionais da lane Community.
     */
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            LinhaTransporte entity, 
            LinhaTransporteIntegrationDataDto dto,
            LinhaTransporteIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        entity.setPrioridade(dto.priority);
        entity.setLeadTimeDias(dto.leadTimeDays);
        /*
         * Distancia de rota alimenta recursos Enterprise como mapa, frete,
         * baricentro e analises geograficas. O Supply Planning Community usa
         * lead time/prioridade/lotes e nao deve persistir este dado.
         */
        if (dto.distanceKm != null) {
            throw new RequiresEnterpriseVersionException("Transportation lane distance");
        }
        entity.setDistanciaKm(null);
        entity.setHabilitadoProdutosDescontinuados(dto.enableDiscontinuedMaterials);
        entity.setHabilitadoProdutosNaoLancados(dto.enablePresalesMaterials);
        entity.setHabilitadoProdutosNaoCadastradosLinhaTransporte(dto.enableAllMaterials);
        
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
     * Exporta a lane para o template Community sem distancia/frete/mapa.
     */
    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(LinhaTransporte entity, LinhaTransporteIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getVersaoMalha().getId());
        linhaArquivo.addContent(entity.getLocationOrigem().getId());
        linhaArquivo.addContent(entity.getLocationDestino().getId());
        linhaArquivo.addContent(entity.getPrioridadeCadastrada());
        linhaArquivo.addContent(entity.getLeadTimeDiasCadastrado());
        linhaArquivo.addContent(entity.getHabilitadoProdutosDescontinuadosCadastrado());
        linhaArquivo.addContent(entity.getHabilitadoProdutosNaoLancadosCadastrado());
        linhaArquivo.addContent(entity.getHabilitadoProdutosNaoCadastradosLinhaTransporteCadastrado());
        linhaArquivo.addContent((entity.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada() == null) ? null : entity.getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada().getId());
        linhaArquivo.addContent(entity.getLoteMinimoTransporteCadastrado());
        linhaArquivo.addContent(entity.getMultiploTransporteCadastrado());
        linhaArquivo.addContent(entity.getAtivoCadastrado());
                
        return linhaArquivo;
        
    }

    /**
     * Le os campos nao chave da lane a partir do template Community.
     */
    @Override
    public LinhaTransporteIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, LinhaTransporteIntegrationSupportData supportData) {

        return LinhaTransporteIntegrationDataDto.builder()
                .priority(processedFileRow.getColumnValueAsInteger(3))
                .leadTimeDays(processedFileRow.getColumnValueAsDouble(4))
                .enableDiscontinuedMaterials(processedFileRow.getColumnValueAsBoolean(5))
                .enablePresalesMaterials(processedFileRow.getColumnValueAsBoolean(6))
                .enableAllMaterials(processedFileRow.getColumnValueAsBoolean(7))
                .multipleMinimumTransferLotSizeUomId(processedFileRow.getColumnValueAsString(8))
                .minimumTransferLotSize(processedFileRow.getColumnValueAsDouble(9))
                .multipleTransfer(processedFileRow.getColumnValueAsDouble(10))
                .active(processedFileRow.getColumnValueAsBoolean(11))
                .build();

    }

    /**
     * Le a chave versao/origem/destino a partir das primeiras colunas do
     * template.
     */
    @Override
    public LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, LinhaTransporteIntegrationSupportData supportData) {

        return new LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1),
                processedFileRow.getColumnValueAsString(2));

    }

}
