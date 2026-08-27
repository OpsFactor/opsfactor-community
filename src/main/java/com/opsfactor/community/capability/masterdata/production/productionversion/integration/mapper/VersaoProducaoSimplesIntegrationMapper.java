package com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.productionversion.integration.dto.VersaoProducaoSimplesIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoSimples;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper da versao de producao simples Community.
 *
 * <p>A versao simples liga um roteiro e uma BOM a um material/location. Qualquer
 * modelo de parallel routing/output deve passar por mapper/service Enterprise e
 * nao por este contrato.</p>
 */
@Component
public class VersaoProducaoSimplesIntegrationMapper implements IntegrationMapperInterface<VersaoProducaoSimplesIntegrationDataDto, VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO, VersaoProducaoSimples, VersaoProducaoSimplesIntegrationSupportData>{

    /**
     * Headers publicados para versao de producao simples Community.
     *
     * <p>O contrato liga um unico roteiro e uma unica BOM a um material/location.
     * Parallel routing e versoes com multiplos outputs devem ficar em mappers
     * Enterprise separados.</p>
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
    public VersaoProducaoSimplesIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(VersaoProducaoSimples entity) {
        
        return VersaoProducaoSimplesIntegrationDataDto.builder()
                .locationId(entity.getLocation().getId())
                .priority(entity.getPrioridadeCadastrada())
                .outputMaterialId(entity.getMaterialOutput().getId())
                .routingId(entity.getRoteiro().getId())
                .billOfMaterialsId(entity.getListaTecnica().getId())
                .active(entity.getAtivo())
                .build();
        
    }

    @Override
    public VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(VersaoProducaoSimples versaoProducaoSimples) {
        return new VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO(
                versaoProducaoSimples.getId());
    }

    @Override
    public VersaoProducaoSimples createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO dto,
            VersaoProducaoSimplesIntegrationSupportData supportData) {
        
        VersaoProducaoSimples versaoProducaoSimples = new VersaoProducaoSimples();
        versaoProducaoSimples.setId(dto.id);
        
        return versaoProducaoSimples;
        
    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            VersaoProducaoSimples entity, 
            VersaoProducaoSimplesIntegrationDataDto dto,
            VersaoProducaoSimplesIntegrationSupportData supportData,
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
            VersaoProducaoSimples entity, 
            VersaoProducaoSimplesIntegrationSupportData supportData) {
        
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
    public VersaoProducaoSimplesIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow processedFileRow, VersaoProducaoSimplesIntegrationSupportData supportData) {
        return VersaoProducaoSimplesIntegrationDataDto.builder()
                .locationId(processedFileRow.getColumnValueAsString(1))
                .priority(processedFileRow.getColumnValueAsInteger(2))
                .outputMaterialId(processedFileRow.getColumnValueAsString(3))
                .routingId(processedFileRow.getColumnValueAsString(4))
                .billOfMaterialsId(processedFileRow.getColumnValueAsString(5))
                .active(processedFileRow.getColumnValueAsBoolean(6))
                .build();
    }

    @Override
    public VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow processedFileRow, VersaoProducaoSimplesIntegrationSupportData supportData) {
        return new VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));
    }

}
