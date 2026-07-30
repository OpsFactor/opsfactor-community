package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesMaterialLocationIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation.PoliticaEstoquesMaterialLocationCompositeKey;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper Community da regra material/location de politica de estoque.
 *
 * <p>O arquivo publica apenas parametros operacionais do heuristico:
 * modelos DRP/Kanban/MTS/MTO e valores de safety stock/estoque maximo. A
 * frequencia de reabastecimento e mantida fora dos headers porque pertence a
 * Inventory Policy Optimization Enterprise.</p>
 */
@Component
public class PoliticaEstoquesMaterialLocationIntegrationMapper implements IntegrationMapperInterface<
        PoliticaEstoquesMaterialLocationIntegrationDataDto,
        PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO,
        PoliticaEstoquesMaterialLocation,
        PoliticaEstoquesMaterialLocationIntegrationSupportData> {

    private static final List<String> PROCESSED_FILE_HEADERS = List.of(
            "Inventory Policy Id",
            "Material Id",
            "Location Id",
            "Operational Model : MTS / MTO (default = MTS if empty)",
            "Reorder Model : DRP / KANBAN (default = DRP if empty)",
            "Safety Stock Type : DAYS / QUANTITY (default = DAYS if empty)",
            "DRP Safety Stock or Kanban Target Stock (# days or quantity)",
            "DRP Maximum Stock (# days or quantity)");

    /**
     * Headers Community sem colunas de otimizacao Enterprise.
     */
    @Override
    public List<String> getProcessedFileHeaders() {

        return PROCESSED_FILE_HEADERS;

    }

    /**
     * Converte entidade em DTO sem publicar frequencia de reabastecimento.
     */
    @Override
    public PoliticaEstoquesMaterialLocationIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            PoliticaEstoquesMaterialLocation entity) {

        return PoliticaEstoquesMaterialLocationIntegrationDataDto.builder()
                .operationalModel(entity.getModeloOperacionalCadastrado())
                .reorderModel(entity.getModeloReabastecimentoCadastrado())
                .safetyStockType(entity.getCalculoSafetyStockCadastrado())
                .drpSafetyStockOrKanbanTargetStockValue(entity.getEstoqueSegurancaDrpOuTargetKanbanCadastrado())
                .drpMaximumStockValue(entity.getEstoqueMaximoDrpCadastrado())
                .reorderFrequencyDays(null)
                .build();

    }

    /**
     * Extrai a chave publica da regra material/location.
     */
    @Override
    public PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            PoliticaEstoquesMaterialLocation entity) {

        return new PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO(
                entity.getPoliticaEstoques().getId(),
                entity.getMaterial().getId(),
                entity.getLocation().getId());

    }

    /**
     * Cria entidade nova resolvendo politica, material e location pelos mapas
     * pre-carregados do service.
     */
    @Override
    public PoliticaEstoquesMaterialLocation createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO primaryKeyDto,
            PoliticaEstoquesMaterialLocationIntegrationSupportData supportData) {

        PoliticaEstoques politicaEstoques = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                supportData.mapaPoliticaEstoquesPorId,
                primaryKeyDto.inventoryPolicyId,
                true,
                new MissingDependencyDataUploadException(
                        "Inventory Policy " + primaryKeyDto.inventoryPolicyId + " not found",
                        primaryKeyDto));
        Produto material = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                supportData.mapaMaterialPorId,
                primaryKeyDto.materialId,
                true,
                new MissingDependencyDataUploadException(
                        "Material " + primaryKeyDto.materialId + " not found",
                        primaryKeyDto));
        Location location = FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                supportData.mapaLocationPorId,
                primaryKeyDto.locationId,
                true,
                new MissingDependencyDataUploadException(
                        "Location " + primaryKeyDto.locationId + " not found",
                        primaryKeyDto));

        return new PoliticaEstoquesMaterialLocation(
                new PoliticaEstoquesMaterialLocationCompositeKey(
                        politicaEstoques,
                        material,
                        location));

    }

    /**
     * Atualiza somente campos operacionais Community e bloqueia frequencia
     * Enterprise quando enviada via JSON ou configuracao de campos.
     */
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            PoliticaEstoquesMaterialLocation entity,
            PoliticaEstoquesMaterialLocationIntegrationDataDto dto,
            PoliticaEstoquesMaterialLocationIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        entity.setModeloOperacional(dto.operationalModel);
        entity.setModeloReabastecimento(dto.reorderModel);
        entity.setCalculoSafetyStock(dto.safetyStockType);
        entity.setEstoqueSegurancaDrpOuTargetKanban(dto.drpSafetyStockOrKanbanTargetStockValue);
        entity.setEstoqueMaximoDrp(dto.drpMaximumStockValue);

        /*
         * A frequencia foi historicamente compartilhada no schema, mas no
         * Community nao deve voltar por upload nem permanecer em entidade
         * atualizada por este mapper.
         */
        if (dto.reorderFrequencyDays != null) {
            throw new RequiresEnterpriseVersionException("Inventory policy optimization replenishment frequency");
        }
        entity.setFrequenciaReabastecimentoDias(null);

    }

    /**
     * Exporta uma linha do arquivo processado sem a coluna Enterprise de
     * frequencia de reabastecimento.
     */
    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            PoliticaEstoquesMaterialLocation entity,
            PoliticaEstoquesMaterialLocationIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getPoliticaEstoques().getId());
        linhaArquivo.addContent(entity.getMaterial().getId());
        linhaArquivo.addContent(entity.getLocation().getId());
        linhaArquivo.addContent(entity.getModeloOperacionalCadastrado());
        linhaArquivo.addContent(entity.getModeloReabastecimentoCadastrado());
        linhaArquivo.addContent(entity.getCalculoSafetyStockCadastrado());
        linhaArquivo.addContent(entity.getEstoqueSegurancaDrpOuTargetKanbanCadastrado());
        linhaArquivo.addContent(entity.getEstoqueMaximoDrpCadastrado());
        return linhaArquivo;

    }

    /**
     * Le campos operacionais do arquivo processado.
     */
    @Override
    public PoliticaEstoquesMaterialLocationIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            PoliticaEstoquesMaterialLocationIntegrationSupportData supportData) {

        return PoliticaEstoquesMaterialLocationIntegrationDataDto.builder()
                .operationalModel(MetodosUtilidade.getValorEnumDeJsonProperty(
                        Constantes.SNPModeloOperacional.class,
                        processedFileRow.getColumnValueAsString(3)))
                .reorderModel(MetodosUtilidade.getValorEnumDeJsonProperty(
                        Constantes.SNPModeloReabastecimento.class,
                        processedFileRow.getColumnValueAsString(4)))
                .safetyStockType(MetodosUtilidade.getValorEnumDeJsonProperty(
                        Constantes.SNPCalculoSafetyStock.class,
                        processedFileRow.getColumnValueAsString(5)))
                .drpSafetyStockOrKanbanTargetStockValue(processedFileRow.getColumnValueAsDouble(6))
                .drpMaximumStockValue(processedFileRow.getColumnValueAsDouble(7))
                .build();

    }

    /**
     * Le a chave publica do arquivo processado.
     */
    @Override
    public PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            PoliticaEstoquesMaterialLocationIntegrationSupportData supportData) {

        return new PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1),
                processedFileRow.getColumnValueAsString(2));

    }

}
