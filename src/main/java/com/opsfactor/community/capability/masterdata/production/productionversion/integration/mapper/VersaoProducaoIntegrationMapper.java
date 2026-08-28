package com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.productionversion.integration.dto.VersaoProducaoIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper da entidade única de versão de produção.
 *
 * <p>Roteiro e lista técnica são referenciados por seus tipos gerais. A
 * especialização dos mestres é resolvida pelo JPA, sem contratos concorrentes
 * na camada de integração.</p>
 */
@Component
public class VersaoProducaoIntegrationMapper implements IntegrationMapperInterface<VersaoProducaoIntegrationDataDto, VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO, VersaoProducao, VersaoProducaoIntegrationSupportData>{

    /**
     * Headers publicados para versão de produção.
     *
     * <p>A coluna de material valida o output derivado dos mestres e não cria
     * uma segunda associação de material na versão.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
            "Id",
            "Location Id",
            "Priority",
            "Output Material Id",
            "Routing Id",
            "Bill of Materials Id",
            "Active");

    /**
     * Retorna a ordem oficial das colunas processadas em arquivo.
     */
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public VersaoProducaoIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(VersaoProducao entity) {
        
        return VersaoProducaoIntegrationDataDto.builder()
                .locationId(entity.getLocation().getId())
                .priority(entity.getPrioridadeCadastrada())
                .outputMaterialId(entity.getMaterialOutput().getId())
                .routingId(entity.getRoteiro().getId())
                .billOfMaterialsId(entity.getListaTecnica().getId())
                .active(entity.getAtivo())
                .build();
        
    }

    @Override
    public VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(VersaoProducao versaoProducao) {
        return new VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO(
                versaoProducao.getId());
    }

    @Override
    public VersaoProducao createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO dto,
            VersaoProducaoIntegrationSupportData supportData) {
        
        VersaoProducao versaoProducao = new VersaoProducao();
        versaoProducao.setId(dto.id);
        
        return versaoProducao;
        
    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            VersaoProducao entity,
            VersaoProducaoIntegrationDataDto dto,
            VersaoProducaoIntegrationSupportData supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever) {
        
        // seta campos simples
        entity.setAtivo(dto.active);
        entity.setPrioridade(dto.priority);
        
        // se valor DTO = nulo, seta nulo. caso contrário, ou busca o valor no mapa em supportData
        // ou retorna exceção caso não encontre o id
        entity.setLocation(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(supportData.mapaLocationPorId, 
                        dto.locationId,
                        true, // campo obrigatório. não pode ser nulo
                        new MissingDependencyDataUploadException("Location " + dto.locationId + " not found", dto)));
        entity.setRoteiro(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaRoteiroPorId, 
                        dto.routingId,
                        true, // campo obrigatório. não pode ser nulo
                        new MissingDependencyDataUploadException("Routing " + dto.routingId + " not found", dto)));
        entity.setListaTecnica(
                FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                        supportData.mapaListaTecnicaPorId, 
                        dto.billOfMaterialsId,
                        true, // campo obrigatório. não pode ser nulo
                        new MissingDependencyDataUploadException("Bills of Materials " + dto.billOfMaterialsId + " not found", dto)));

        /*
         * A coluna de material continua no arquivo por compatibilidade, mas
         * nao e uma dependencia persistida da versao. Ela valida o output
         * derivado do roteiro e da lista tecnica, evitando duas fontes de
         * verdade e um setter artificial na entidade.
         */
        entity.geraErroSeDadosInconsistentes();
        if (dto.outputMaterialId == null
                || !dto.outputMaterialId.equals(entity.getMaterialOutput().getId())) {
            throw new DataUploadException(
                    "Output Material "
                            + dto.outputMaterialId
                            + " does not match routing and Bill of Materials output "
                            + entity.getMaterialOutput().getId());
        }
        
    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            VersaoProducao entity,
            VersaoProducaoIntegrationSupportData supportData) {
        
        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getId());
        linhaArquivo.addContent(entity.getLocation().getId());
        linhaArquivo.addContent(entity.getPrioridadeCadastrada());
        linhaArquivo.addContent(entity.getMaterialOutput().getId());
        linhaArquivo.addContent(entity.getRoteiro().getId());
        linhaArquivo.addContent(entity.getListaTecnica().getId());
        linhaArquivo.addContent(entity.getAtivo());
        
        return linhaArquivo;
        
    }

    @Override
    public VersaoProducaoIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, VersaoProducaoIntegrationSupportData supportData) {
        return VersaoProducaoIntegrationDataDto.builder()
                .locationId(processedFileRow.getColumnValueAsString(1))
                .priority(processedFileRow.getColumnValueAsInteger(2))
                .outputMaterialId(processedFileRow.getColumnValueAsString(3))
                .routingId(processedFileRow.getColumnValueAsString(4))
                .billOfMaterialsId(processedFileRow.getColumnValueAsString(5))
                .active(processedFileRow.getColumnValueAsBoolean(6))
                .build();
    }

    @Override
    public VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, VersaoProducaoIntegrationSupportData supportData) {
        return new VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));
    }

}
