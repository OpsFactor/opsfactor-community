package com.opsfactor.community.capability.masterdata.production.routing.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.routing.integration.dto.RoteiroIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.integration.validation.IntegrationFieldValidation;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper de roteiros operacionais Community.
 *
 * <p>As colunas representam somente o roteiro basico consumido pelo heuristico:
 * id, descricao, location, material de saida, prioridade, status e uso sem
 * versao de producao. Setup detalhado, turnos, manutencao, custos e line
 * scheduling nao entram neste mapper.</p>
 */
@Component
public class RoteiroIntegrationMapper implements IntegrationMapperInterface<RoteiroIntegrationDataDto, RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO,Roteiro, RoteiroIntegrationSupportData> {

    /**
     * Headers publicados para roteiros Community.
     *
     * <p>A lista evita colunas de setup, manutencao, turnos ou scheduling para
     * manter o cadastro alinhado ao heuristico Community.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
        "Routing Id",
        "Description",
        "Location Id",
        "Output Material Id",
        "Base Quantity",
        "Base Quantity UOM",
        "Routing can be used without production version",
        "Priority",
        "Active (true/false or 1/0)");

    /**
     * Retorna a ordem oficial das colunas processadas em arquivo.
     */
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public RoteiroIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(Roteiro entity) {
        
        return RoteiroIntegrationDataDto.builder()
                .description(entity.getDescricao())
                .locationId(entity.getLocation().getId())
                .outputMaterialId(entity.getMaterialOutput().getId())
                .baseQuantity(entity.getQuantidadeBaseCadastrada())
                .baseQuantityUomId(entity.getUnidadeMedidaQuantidadeBaseCadastrada() == null
                        ? null
                        : entity.getUnidadeMedidaQuantidadeBaseCadastrada().getId())
                .canBeUsedWithoutProductionVersion(entity.getHabilitadoParaUsoSemVersaoProducaoCadastrado())
                .priority(entity.getPrioridadeCadastrada())
                .active(entity.getAtivoCadastrado())
                .build();
        
    }

    @Override
    public RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(Roteiro roteiro) {
        return new RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO(
                roteiro.getId());
    }

    @Override
    public Roteiro createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO dto,
            RoteiroIntegrationSupportData supportData) {
        
        Roteiro roteiro = new Roteiro();
        roteiro.setId(dto.id);
        
        return roteiro;
        
    }
    
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            Roteiro entity,
            RoteiroIntegrationDataDto dto,
            RoteiroIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        // Deletion consumes only the key and must permit removing invalid historical records.
        if (dto.delete != null && dto.delete.trim().equalsIgnoreCase("D")) {
            return;
        }

        // An omitted quantity keeps the unitary calculation default; a supplied value must be valid.
        if (isUpdateableField("baseQuantity", camposASobrecrever)
                && dto.baseQuantity != null
                && (!Double.isFinite(dto.baseQuantity) || dto.baseQuantity <= 0d)) {
            throw new DataUploadException(
                    "Routing " + entity.getId()
                            + " field '" + IntegrationFieldValidation.label(dto, "baseQuantity",
                                    getIntegrationFileColumn(dto, "baseQuantity"))
                            + "' must be finite and positive; received " + dto.baseQuantity
                            + IntegrationFieldValidation.location(dto));
        }

        // seta campos simples
        entity.setDescricao(dto.description);
        entity.setAtivo(dto.active);
        entity.setHabilitadoParaUsoSemVersaoProducao(dto.canBeUsedWithoutProductionVersion);
        entity.setPrioridade(dto.priority);
        if (isUpdateableField("baseQuantity", camposASobrecrever)) {
            entity.setQuantidadeBase(dto.baseQuantity);
        }

        /*
         * O identificador e compartilhado apenas para o overlay Enterprise
         * religar o catalogo privado de clusters de roteiros. O Community nao
         * pode aceitar uma configuracao de line scheduling que ele nao
         * executa; falhar aqui evita persistir um escalar sem efeito publico.
         */
        if (isUpdateableField("routingClusterId", camposASobrecrever)
                && dto.routingClusterId != null
                && !dto.routingClusterId.isBlank()) {
            throw new RequiresEnterpriseVersionException("Routing cluster");
        }
        
        entity.setLocation(
                resolveReference(supportData.mapaLocationPorId, dto, "locationId", true));
        entity.setMaterialOutput(
                resolveReference(supportData.mapaMaterialPorId, dto, "outputMaterialId", true));
        entity.setUnidadeMedidaQuantidadeBase(
                resolveReference(supportData.mapaUnidadeMedidaPorId, dto, "baseQuantityUomId", false));
                
    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(Roteiro entity, RoteiroIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getId());
        linhaArquivo.addContent(entity.getDescricao());
        linhaArquivo.addContent(entity.getLocation().getId());
        linhaArquivo.addContent(entity.getMaterialOutput().getId());
        linhaArquivo.addContent(entity.getQuantidadeBaseCadastrada());
        linhaArquivo.addContent(entity.getUnidadeMedidaQuantidadeBaseCadastrada() == null
                ? null
                : entity.getUnidadeMedidaQuantidadeBaseCadastrada().getId());
        linhaArquivo.addContent(entity.getHabilitadoParaUsoSemVersaoProducaoCadastrado());
        linhaArquivo.addContent(entity.getPrioridadeCadastrada());
        linhaArquivo.addContent(entity.getAtivoCadastrado());
                
        return linhaArquivo;

    }

    @Override
    public RoteiroIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, RoteiroIntegrationSupportData supportData) {
        return RoteiroIntegrationDataDto.builder()
                .description(processedFileRow.getColumnValueAsString(1))
                .locationId(processedFileRow.getColumnValueAsString(2))
                .outputMaterialId(processedFileRow.getColumnValueAsString(3))
                .baseQuantity(processedFileRow.getColumnValueAsDouble(4))
                .baseQuantityUomId(processedFileRow.getColumnValueAsString(5))
                .canBeUsedWithoutProductionVersion(processedFileRow.getColumnValueAsBoolean(6))
                .priority(processedFileRow.getColumnValueAsInteger(7))
                .active(processedFileRow.getColumnValueAsBoolean(8))
                .build();
    }

    @Override
    public RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, RoteiroIntegrationSupportData supportData) {
        return new RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));
    }

}
