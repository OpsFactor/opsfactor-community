package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.VersaoMalhaIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper de integracao da versao de malha Community.
 *
 * <p>O contrato preserva os headers legados de `supplynetworkversion`, mas
 * mantem a superficie em dados operacionais minimos: id, descricao, origem
 * padrao para clientes e origem/lead time padrao para materias-primas.</p>
 */
@Component
public class VersaoMalhaIntegrationMapper implements IntegrationMapperInterface<
        VersaoMalhaIntegrationDataDto,
        VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO,
        VersaoMalha,
        VersaoMalhaIntegrationSupportData> {

    /**
     * Headers publicos do template de versao de malha.
     */
    public static final List<String> processedFileHeaders = List.of(
            "Supply Network Version Id",
            "Supply Network Version Description",
            "Default Origin Location for Clients",
            "Default Origin Location for Raw Materials",
            "Default Origin Location for Raw Materials Lead Time Days");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    /**
     * Converte a entidade para DTO omitindo apenas a chave primaria.
     */
    @Override
    public VersaoMalhaIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(VersaoMalha entity) {

        return VersaoMalhaIntegrationDataDto.builder()
                .description(entity.getDescricao())
                .defaultClientOriginLocationId(getLocationId(entity.getLocationOrigemPadraoClientes()))
                .defaultRawMaterialOriginLocationId(getLocationId(entity.getLocationOrigemPadraoMateriasPrimas()))
                .defaultRawMaterialOriginLeadTimeDays(entity.getLeadTimeDiasLocationOrigemPadraoMateriasPrimas())
                .build();

    }

    @Override
    public VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            VersaoMalha entity) {

        return new VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO(entity.getId());

    }

    /**
     * Cria a entidade nova apenas com a chave funcional.
     */
    @Override
    public VersaoMalha createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO primaryKeyDto,
            VersaoMalhaIntegrationSupportData supportData) {

        return new VersaoMalha(primaryKeyDto.supplyNetworkVersionId);

    }

    /**
     * Atualiza os campos nao chave da versao de malha.
     */
    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            VersaoMalha entity,
            VersaoMalhaIntegrationDataDto dto,
            VersaoMalhaIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        entity.setDescricao(dto.description);
        entity.setLocationOrigemPadraoClientes(
                getOptionalLocation(
                        normalizeOptionalLocationId(dto.defaultClientOriginLocationId),
                        supportData,
                        "Default Client Origin Location Id",
                        dto));
        entity.setLocationOrigemPadraoMateriasPrimas(
                getOptionalLocation(
                        normalizeOptionalLocationId(dto.defaultRawMaterialOriginLocationId),
                        supportData,
                        "Default Raw Material Origin Location Id",
                        dto));
        entity.setLeadTimeDiasLocationOrigemPadraoMateriasPrimas(
                validaLeadTimeDias(dto.defaultRawMaterialOriginLeadTimeDays));

    }

    /**
     * Exporta uma entidade como linha de arquivo na ordem legada.
     */
    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            VersaoMalha entity,
            VersaoMalhaIntegrationSupportData supportData) {

        ProcessedFileRow processedFileRow = new ProcessedFileRow();
        processedFileRow.addContent(entity.getId());
        processedFileRow.addContent(entity.getDescricao());
        processedFileRow.addContent(getLocationId(entity.getLocationOrigemPadraoClientes()));
        processedFileRow.addContent(getLocationId(entity.getLocationOrigemPadraoMateriasPrimas()));
        processedFileRow.addContent(entity.getLeadTimeDiasLocationOrigemPadraoMateriasPrimas());
        return processedFileRow;

    }

    /**
     * Le os campos nao chave a partir da linha de arquivo.
     */
    @Override
    public VersaoMalhaIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            VersaoMalhaIntegrationSupportData supportData) {

        return VersaoMalhaIntegrationDataDto.builder()
                .description(processedFileRow.getColumnValueAsString(1))
                .defaultClientOriginLocationId(processedFileRow.getColumnValueAsString(2))
                .defaultRawMaterialOriginLocationId(processedFileRow.getColumnValueAsString(3))
                .defaultRawMaterialOriginLeadTimeDays(processedFileRow.getColumnValueAsDouble(4))
                .build();

    }

    /**
     * Le a chave da versao de malha a partir da primeira coluna do template.
     */
    @Override
    public VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            VersaoMalhaIntegrationSupportData supportData) {

        return new VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));

    }

    /**
     * Converte a linha somente quando ela preserva as cinco colunas funcionais
     * publicadas pelo template. A coluna tecnica {@code Delete}, quando
     * presente, permanece na sexta posicao definida pelo contrato generico.
     *
     * <p>O formato anterior de quatro colunas, que interpretava {@code D} na
     * quarta posicao como exclusao, nao pertence ao contrato Community. A
     * validacao abaixo torna a incompatibilidade explicita antes de qualquer
     * tentativa de reconciliacao de origens.</p>
     */
    @Override
    public VersaoMalhaIntegrationDataDto convertProcessedFileRowToDTO(
            ProcessedFileRow processedFileRow,
            VersaoMalhaIntegrationSupportData supportData) {

        if (processedFileRow.getRowSize() < processedFileHeaders.size()) {
            throw new DataUploadException(
                    "Supply Network Version file row must provide "
                            + processedFileHeaders.size()
                            + " functional columns before the optional Delete marker.");
        }

        return IntegrationMapperInterface.super.convertProcessedFileRowToDTO(
                processedFileRow,
                supportData);

    }

    private static String getLocationId(Location location) {

        return location == null ? null : location.getId();

    }

    private static String normalizeOptionalLocationId(String locationId) {

        if (locationId == null || locationId.isBlank() || locationId.equalsIgnoreCase("null")) {
            return null;
        }
        return locationId;

    }

    private static Location getOptionalLocation(
            String locationId,
            VersaoMalhaIntegrationSupportData supportData,
            String fieldDescription,
            VersaoMalhaIntegrationDataDto dto) {

        if (locationId == null) {
            return null;
        }
        return FuncoesMap.getFromMapOrThrowExceptionIfNotFound(
                supportData.mapaLocationPorId,
                locationId,
                false,
                new MissingDependencyDataUploadException(
                        fieldDescription + " " + locationId + " not found",
                        dto));

    }

    private static Double validaLeadTimeDias(Double leadTimeDias) {

        if (leadTimeDias == null) {
            return null;
        }
        if (!Double.isFinite(leadTimeDias) || leadTimeDias < 0) {
            throw new IllegalArgumentException(
                    "Default Raw Material Origin Lead Time Days must be finite and non-negative.");
        }
        return leadTimeDias;

    }

}
