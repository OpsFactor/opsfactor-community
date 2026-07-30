package com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto.DisponibilidadeRecursoProdutivoIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo.DisponibilidadeRecursoProdutivoCompositeKey;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Mapper da disponibilidade diaria de recurso produtivo Community.
 *
 * <p>A superficie publica inclui somente horas disponiveis por dia. O campo de
 * quantidade por UOM do schema legado e bloqueado aqui, porque esse modo de
 * capacidade produtiva depende do perfil Supply Enterprise.</p>
 */
@Component
public class DisponibilidadeRecursoProdutivoIntegrationMapper implements IntegrationMapperInterface<DisponibilidadeRecursoProdutivoIntegrationDataDto, DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO, DisponibilidadeRecursoProdutivo, DisponibilidadeRecursoProdutivoIntegrationSupportData> {

    /**
     * Headers publicados para upload/download Community.
     *
     * <p>A lista e imutavel para impedir que um service de data upload ajuste
     * dinamicamente a superficie publica e exponha colunas Enterprise, como
     * capacidade produtiva por quantidade/UOM.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
            "Production Resource Id",
            "Reference Date",
            "Available Hours");

    /**
     * Retorna a ordem oficial das colunas processadas em arquivo.
     */
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public DisponibilidadeRecursoProdutivoIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            DisponibilidadeRecursoProdutivo entity) {

        return DisponibilidadeRecursoProdutivoIntegrationDataDto.builder()
                .availableHours(entity.getHorasDisponiveisCadastrado())
                .capacityInQuantity(null)
                .capacityInQuantityUomId(null)
                .build();

    }

    @Override
    public DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivo) {

        return new DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO(
                disponibilidadeRecursoProdutivo.getRecursoProdutivo().getId(),
                disponibilidadeRecursoProdutivo.getDataReferencia());

    }

    @Override
    public DisponibilidadeRecursoProdutivo createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO dto,
            DisponibilidadeRecursoProdutivoIntegrationSupportData supportData) {

        RecursoProdutivo recursoProdutivo = supportData.mapaRecursoProdutivoPorId.get(dto.productionResourceId);
        if (recursoProdutivo == null) {
            throw new MissingDependencyDataUploadException("Production resource " + dto.productionResourceId + " not found", dto);
        }

        return new DisponibilidadeRecursoProdutivo(
                new DisponibilidadeRecursoProdutivoCompositeKey(
                        recursoProdutivo,
                        dto.referenceDate));

    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            DisponibilidadeRecursoProdutivo entity,
            DisponibilidadeRecursoProdutivoIntegrationDataDto dto,
            DisponibilidadeRecursoProdutivoIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        validaCamposEnterpriseCommunity(dto);

        /*
         * Community persiste apenas horas disponiveis. Capacidade em
         * quantidade fica sempre nula para nao deixar dados Enterprise
         * transicionais influenciarem o fluxo heuristico.
         */
        entity.setHorasDisponiveis(dto.availableHours);
        entity.setCapacidadeEmQuantidade(null);

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            DisponibilidadeRecursoProdutivo entity,
            DisponibilidadeRecursoProdutivoIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getRecursoProdutivo().getId());
        linhaArquivo.addContent(entity.getDataReferencia());
        linhaArquivo.addContent(entity.getHorasDisponiveisCadastrado());

        return linhaArquivo;

    }

    @Override
    public DisponibilidadeRecursoProdutivoIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            DisponibilidadeRecursoProdutivoIntegrationSupportData supportData) {

        return DisponibilidadeRecursoProdutivoIntegrationDataDto.builder()
                .availableHours(processedFileRow.getColumnValueAsFloat(2))
                .build();

    }

    @Override
    public DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            DisponibilidadeRecursoProdutivoIntegrationSupportData supportData) {

        return new DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsLocalDate(1));

    }

    private void validaCamposEnterpriseCommunity(DisponibilidadeRecursoProdutivoIntegrationDataDto dto) {

        if (dto.capacityInQuantity != null) {
            throw new RequiresEnterpriseVersionException("Quantity-based production capacity availability");
        }
        if (dto.capacityInQuantityUomId != null
                && !dto.capacityInQuantityUomId.isBlank()) {
            throw new RequiresEnterpriseVersionException("Quantity-based production capacity availability");
        }
        if (dto.delete != null
                && !dto.delete.isBlank()
                && !dto.delete.trim().equalsIgnoreCase("D")) {
            throw new RequiresEnterpriseVersionException("Quantity-based production capacity availability");
        }

    }

}
