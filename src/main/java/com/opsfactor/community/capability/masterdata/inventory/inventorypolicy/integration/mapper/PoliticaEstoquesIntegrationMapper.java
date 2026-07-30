package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper Community do cabecalho de politica operacional de estoque.
 *
 * <p>Este mapper expoe apenas prioridade e vigencia. A relacao
 * material/location fica em {@link PoliticaEstoquesMaterialLocationIntegrationMapper},
 * mantendo o cabecalho pequeno e sem dependencias de support data.</p>
 */
@Component
public class PoliticaEstoquesIntegrationMapper implements IntegrationMapperInterface<
        PoliticaEstoquesIntegrationDataDto,
        PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO,
        PoliticaEstoques,
        PoliticaEstoquesIntegrationSupportData> {

    private static final List<String> PROCESSED_FILE_HEADERS = List.of(
            "Id",
            "Priority",
            "Start Date",
            "End Date");

    /**
     * Headers fixos do cabecalho de politica de estoque.
     */
    @Override
    public List<String> getProcessedFileHeaders() {

        return PROCESSED_FILE_HEADERS;

    }

    /**
     * Converte entidade em DTO sem chave primaria, preservando somente os
     * campos publicos Community.
     */
    @Override
    public PoliticaEstoquesIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(PoliticaEstoques entity) {

        return PoliticaEstoquesIntegrationDataDto.builder()
                .priority(entity.getPrioridadeCadastrada())
                .startDateTime(entity.getDataHorarioInicio())
                .endDateTime(entity.getDataHorarioFim())
                .build();

    }

    /**
     * Extrai a chave funcional do cabecalho persistido.
     */
    @Override
    public PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            PoliticaEstoques entity) {

        return new PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO(
                entity.getId());

    }

    /**
     * Cria uma politica nova apenas com sua chave funcional.
     */
    @Override
    public PoliticaEstoques createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO primaryKeyDto,
            PoliticaEstoquesIntegrationSupportData supportData) {

        PoliticaEstoques politicaEstoques = new PoliticaEstoques();
        politicaEstoques.setId(primaryKeyDto.id);
        return politicaEstoques;

    }

    /**
     * Atualiza os campos simples do cabecalho.
     */
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            PoliticaEstoques entity,
            PoliticaEstoquesIntegrationDataDto dto,
            PoliticaEstoquesIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        entity.setPrioridade(dto.priority);
        entity.setDataHorarioInicio(dto.startDateTime);
        entity.setDataHorarioFim(dto.endDateTime);

    }

    /**
     * Exporta uma linha do arquivo processado na mesma ordem dos headers.
     */
    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            PoliticaEstoques entity,
            PoliticaEstoquesIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getId());
        linhaArquivo.addContent(entity.getPrioridadeCadastrada());
        linhaArquivo.addContent(entity.getDataHorarioInicio());
        linhaArquivo.addContent(entity.getDataHorarioFim());
        return linhaArquivo;

    }

    /**
     * Le campos nao primarios do arquivo processado.
     */
    @Override
    public PoliticaEstoquesIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            PoliticaEstoquesIntegrationSupportData supportData) {

        return PoliticaEstoquesIntegrationDataDto.builder()
                .priority(processedFileRow.getColumnValueAsInteger(1))
                .startDateTime(processedFileRow.getColumnValueAsLocalDateTime(2))
                .endDateTime(processedFileRow.getColumnValueAsLocalDateTime(3))
                .build();

    }

    /**
     * Le a chave publica do arquivo processado.
     */
    @Override
    public PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            PoliticaEstoquesIntegrationSupportData supportData) {

        return new PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));

    }

}
