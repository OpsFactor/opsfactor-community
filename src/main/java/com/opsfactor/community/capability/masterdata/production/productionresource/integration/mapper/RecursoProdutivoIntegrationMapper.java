package com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto.RecursoProdutivoIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper dos recursos produtivos Community.
 *
 * <p>O Community aceita apenas eficiencia, location, status e capacidade em
 * horas por dia. O campo legado de UOM de capacidade por quantidade permanece
 * para rejeitar payloads Enterprise explicitamente.</p>
 */
@Component
public class RecursoProdutivoIntegrationMapper implements IntegrationMapperInterface<RecursoProdutivoIntegrationDataDto, RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO,RecursoProdutivo, RecursoProdutivoIntegrationSupportData> {

    /**
     * Headers publicados para recursos produtivos Community.
     *
     * <p>Capacidade por quantidade/UOM permanece fora da lista porque depende
     * de configuracao e validacao Enterprise. Payloads JSON com esse campo
     * continuam sendo rejeitados explicitamente pelo mapper.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
        "Production Resource Id",
        "Description",
        "Location Id",
        "Efficiency (1.0 if empty)",
        "Active (true/false or 1/0)");

    /**
     * Retorna a ordem oficial das colunas processadas em arquivo.
     */
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public RecursoProdutivoIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(RecursoProdutivo entity) {
        
        return RecursoProdutivoIntegrationDataDto.builder()
                .description(entity.getDescricao())
                .locationId(entity.getLocation().getId())
                .efficiency(entity.getEficienciaCadastrado())
                .capacityInQuantityUomId(null)
                .active(entity.getAtivoCadastrado())
                .build();
        
    }

    @Override
    public RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(RecursoProdutivo recursoProdutivo) {
        return new RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO(
                recursoProdutivo.getId());
    }

    @Override
    public RecursoProdutivo createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO dto,
            RecursoProdutivoIntegrationSupportData supportData) {
        
        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        recursoProdutivo.setId(dto.id);
        
        return recursoProdutivo;
        
    }
    
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            RecursoProdutivo entity,
            RecursoProdutivoIntegrationDataDto dto,
            RecursoProdutivoIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        // seta campos simples
        entity.setDescricao(dto.description);
        entity.setAtivo(dto.active);
        entity.setEficiencia(dto.efficiency);

        /*
         * Community executa Supply Planning com capacidade produtiva em horas
         * por dia. Capacidade por quantidade/UOM e a configuracao associada no
         * recurso produtivo pertencem ao Enterprise; se vierem preenchidas por
         * JSON ou arquivo legado, falhamos antes de persistir qualquer coisa.
         */
        if (dto.capacityInQuantityUomId != null
                && !dto.capacityInQuantityUomId.isBlank()) {
            throw new RequiresEnterpriseVersionException("Production capacity in quantity UOM");
        }

        entity.setLocation(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaLocationPorId, 
                        dto.locationId, 
                        true, // location é obrigatória
                        new MissingDependencyDataUploadException("Location " + dto.locationId + " not found", dto)));
        entity.setUnidadeMedidaCapacidadeEmUom(null);
                
    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(RecursoProdutivo entity, RecursoProdutivoIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getId());
        linhaArquivo.addContent(entity.getDescricao());
        linhaArquivo.addContent(entity.getLocation().getId());
        linhaArquivo.addContent(entity.getEficienciaCadastrado());
        linhaArquivo.addContent(entity.getAtivoCadastrado());
                
        return linhaArquivo;

    }

    @Override
    public RecursoProdutivoIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, RecursoProdutivoIntegrationSupportData supportData) {
        return RecursoProdutivoIntegrationDataDto.builder()
                .description(processedFileRow.getColumnValueAsString(1))
                .locationId(processedFileRow.getColumnValueAsString(2))
                .efficiency(processedFileRow.getColumnValueAsFloat(3))
                .active(processedFileRow.getColumnValueAsBoolean(4))
                .build();
    }

    @Override
    public RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, RecursoProdutivoIntegrationSupportData supportData) {
        return new RecursoProdutivoIntegrationDataDto.RecursoProdutivoPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));
    }

}
