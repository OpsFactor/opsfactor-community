package com.opsfactor.community.capability.configuration.integration.mapper;

import com.opsfactor.community.capability.configuration.integration.dto.ParametrosMaterialLocationIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper Community da carga de parametros material/location.
 *
 * <p>Este mapper mantém somente dados operacionais simples usados por Demand
 * Planning, Supply Planning heuristico e cadastros produtivos basicos:
 * ativacao, vigencia, lote minimo/multiplo, UOM padrao e horizonte congelado
 * de DP. Frequencia de reabastecimento, caracteristicas material-location e
 * estruturas dinamicas de DFU pertencem ao Enterprise e sao rejeitadas antes
 * de atualizar a entidade.</p>
 */
@Component
public class ParametrosMaterialLocationIntegrationMapper implements IntegrationMapperInterface<
        ParametrosMaterialLocationIntegrationDataDto,
        ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO,
        ParametrosProdutoLocation,
        ParametrosMaterialLocationIntegrationSupportData> {

    private static final List<String> PROCESSED_FILE_HEADERS = List.of(
            "Location Id",
            "Material Id",
            "Active : True/False or 1/0(if false, material is not part of lineup at location. Default = True if empty)",
            "Introduction Date",
            "Discontinuation Date",
            "Production Minimum/Multiple Unit of Measure (if empty, considers default UOM)",
            "Production Minimum Order Quantity",
            "Production Multiple Quantity",
            "Default Unit of Measure (Supply Planning)",
            "DP Frozen Horizon in Days");

    /**
     * Headers fixos Community. Nao incluir colunas Enterprise aqui sem migrar
     * tambem mapper, OpenAPI e services privados correspondentes.
     */
    public List<String> getProcessedFileHeaders() {

        return PROCESSED_FILE_HEADERS;

    }

    @Override
    public ParametrosMaterialLocationIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(ParametrosProdutoLocation entity) {
        return ParametrosMaterialLocationIntegrationDataDto.builder()
                .active(entity.getAtivoCadastrado())
                .introductionDate(entity.getDataIntroducao())
                .discontinuationDate(entity.getDataDescontinuacao())
                .reorderFrequencyDays(null)
                .productionMinimumMultipleUomId((entity.getUnidadeMedidaLoteMinimoMultiploProducaoCadastrado() == null) ? null : entity.getUnidadeMedidaLoteMinimoMultiploProducaoCadastrado().getId())
                .productionMinimumQuantity(entity.getLoteMinimoProducaoCadastrado())
                .productionMultipleQuantity(entity.getMultiploProducaoCadastrado())
                .defaultUomId((entity.getUnidadeMedidaPadraoCadastrado() == null) ? null : entity.getUnidadeMedidaPadraoCadastrado().getId())
                .frozenHorizonDpInDays(entity.getNumeroDiasHorizonteCongeladoDpCadastrado())
                .valueByCharacteristic(new HashMap<>())
                .build();
    }

    @Override
    public ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            ParametrosProdutoLocation parametrosProdutoLocation) {
        return new ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO(
                parametrosProdutoLocation.getLocation().getId(),
                parametrosProdutoLocation.getProduto().getId());
    }

    @Override
    public ParametrosProdutoLocation createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO dto, ParametrosMaterialLocationIntegrationSupportData supportData) {
        
        Produto material = supportData.mapaMaterialPorId.get(dto.materialId);
        if (material == null) throw new MissingDependencyDataUploadException("Material " + dto.materialId + " not found", dto);
        Location location = supportData.mapaLocationPorId.get(dto.locationId);
        if (location == null) throw new MissingDependencyDataUploadException("Location " + dto.locationId + " not found", dto);
        
        return new ParametrosProdutoLocation(new ParametrosProdutoLocationCompositeKey(
                material, location));
        
    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            ParametrosProdutoLocation entity, 
            ParametrosMaterialLocationIntegrationDataDto dto,
            ParametrosMaterialLocationIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        // campos simples
        entity.setAtivo(dto.active);
        entity.setDataIntroducao(dto.introductionDate);
        entity.setDataDescontinuacao(dto.discontinuationDate);
        /*
         * Frequencia de reabastecimento hoje alimenta a otimizacao de politica de
         * estoques. Safety stock operacional Community continua vindo de
         * PoliticaEstoques e dos parametros produtivos simples.
         */
        if (dto.reorderFrequencyDays != null) {
            throw new RequiresEnterpriseVersionException("Inventory policy optimization replenishment frequency");
        }
        entity.setFrequenciaReabastecimentoDias(null);
        entity.setLoteMinimoProducao(dto.productionMinimumQuantity);
        entity.setMultiploProducao(dto.productionMultipleQuantity);
        entity.setNumeroDiasHorizonteCongeladoDp(dto.frozenHorizonDpInDays);

        /*
         * Caracteristicas material-location sao usadas para filtros DFU e
         * estruturas de agregacao Enterprise. O Community preserva parametros
         * operacionais simples material/location, mas nao aceita carga de
         * atributos dinamicos por DFU.
         */
        if (dto.valueByCharacteristic != null && !dto.valueByCharacteristic.isEmpty()) {
            throw new RequiresEnterpriseVersionException("Material-location characteristics");
        }
        
        // campos entidades
        // se valor DTO = nulo, seta nulo. caso contrário, ou busca o valor no mapa em supportData
        // ou retorna exceção caso não encontre o id
        entity.setUnidadeMedidaLoteMinimoMultiploProducao(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaUnidadeMedidaPorId, 
                        dto.productionMinimumMultipleUomId,
                        false, // campo não obrigatório. pode ser nulo
                        new MissingDependencyDataUploadException("Unit of Measure " + dto.productionMinimumMultipleUomId + " not found", dto)));
        entity.setUnidadeMedidaPadrao(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaUnidadeMedidaPorId, 
                        dto.defaultUomId,
                        false, // campo não obrigatório. pode ser nulo
                        new MissingDependencyDataUploadException("Unit of Measure " + dto.defaultUomId + " not found", dto)));
        
    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(ParametrosProdutoLocation entity, ParametrosMaterialLocationIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getLocation().getId());
        linhaArquivo.addContent(entity.getProduto().getId());
        linhaArquivo.addContent(entity.getAtivoCadastrado());
        linhaArquivo.addContent(entity.getDataIntroducao());
        linhaArquivo.addContent(entity.getDataDescontinuacao());
        linhaArquivo.addContent((entity.getUnidadeMedidaLoteMinimoMultiploProducaoCadastrado() == null) ? null : entity.getUnidadeMedidaLoteMinimoMultiploProducaoCadastrado().getId());
        linhaArquivo.addContent(entity.getLoteMinimoProducaoCadastrado());
        linhaArquivo.addContent(entity.getMultiploProducaoCadastrado());
        linhaArquivo.addContent((entity.getUnidadeMedidaPadraoCadastrado() == null) ? null : entity.getUnidadeMedidaPadraoCadastrado().getId());
        linhaArquivo.addContent(entity.getNumeroDiasHorizonteCongeladoDpCadastrado());
                
        return linhaArquivo;
        
    }

    /**
     * Headers fixos da carga Community. Caracteristicas material-location/DFU
     * sao Enterprise e por isso nao geram colunas dinamicas neste mapper.
     */
    @Override
    public List<ProcessedFileRow> getFileHeaderRows(ParametrosMaterialLocationIntegrationSupportData supportData) {
        
        ProcessedFileRow processedFileRow = new ProcessedFileRow();

        // adiciona colunas-base ao header
        for (String nomeHeader : getProcessedFileHeaders()) {
            processedFileRow.addContent(nomeHeader);
        }
        
        // retorna lista de 1 só elemento (apenas 1 linha cabeçalho)
        return List.of(processedFileRow);
        
    }

    /**
     * Ultima coluna fixa considerada em arquivos de remocao Community.
     */
    @Override
    public int getDeleteProcessedFileRowPosition(ParametrosMaterialLocationIntegrationSupportData supportData) {
        return 10;
    }

    @Override
    public ParametrosMaterialLocationIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, ParametrosMaterialLocationIntegrationSupportData supportData) {
        
        ParametrosMaterialLocationIntegrationDataDto dto = ParametrosMaterialLocationIntegrationDataDto.builder()
                .active(processedFileRow.getColumnValueAsBoolean(2))
                .introductionDate(processedFileRow.getColumnValueAsLocalDateTime(3))
                .discontinuationDate(processedFileRow.getColumnValueAsLocalDateTime(4))
                .productionMinimumMultipleUomId(processedFileRow.getColumnValueAsString(5))
                .productionMinimumQuantity(processedFileRow.getColumnValueAsDouble(6))
                .productionMultipleQuantity(processedFileRow.getColumnValueAsDouble(7))
                .defaultUomId(processedFileRow.getColumnValueAsString(8))
                .frozenHorizonDpInDays(processedFileRow.getColumnValueAsInteger(9))
                .build();
        
        return dto;
        
    }

    @Override
    public ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, ParametrosMaterialLocationIntegrationSupportData supportData) {
        return new ParametrosMaterialLocationIntegrationDataDto.ParametrosMaterialLocationPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1));
    }

}
