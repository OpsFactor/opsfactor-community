package com.opsfactor.community.capability.supplyplanning.distributionplan.integration;

import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.supplyplanning.supplyplan.service.SupplyPlanPersistedBaselinePreflight;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service compartilhado read-only para exportar Distribution Plan por Supply Plan.
 */
@Service
public class DistributionPlanIntegrationService implements IntegrationServiceInterface<
        DistributionPlanIntegrationDataDto,
        DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO,
        DistributionPlanIntegrationDataDto,
        com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationSupportData,
        com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationMapper,
        EmptyIntegrationDataFilter> {

    /**
     * Repository Community dos supply plans usados como filtro obrigatorio.
     */
    @Autowired
    private SupplyPlanRepository supplyPlanRepository;

    /**
     * Repository Community das linhas de Distribution Plan persistidas pelo
     * Supply Planning.
     */
    @Autowired
    private DistributionPlanItemRepository distributionPlanItemRepository;

    /** Gate Community compartilhado das series baseline persistidas antigas. */
    @Autowired
    private SupplyPlanPersistedBaselinePreflight supplyPlanPersistedBaselinePreflight;

    /**
     * Service de parametros globais usado para resolver a UOM padrao SNP
     * quando a linha nao tem UOM cadastrada.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Mapper Enterprise do contrato de arquivo/API de Distribution Plan.
     */
    @Autowired
    private com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationMapper distributionPlanIntegrationMapper;

    @Override
    public com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationMapper getMapper() {

        return distributionPlanIntegrationMapper;

    }

    /**
     * Bloqueia persistencia manual: Distribution Plan e output do Supply
     * Planning.
     */
    @Override
    public List<DistributionPlanIntegrationDataDto> saveEntityList(
            Collection<DistributionPlanIntegrationDataDto> entityList) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia remocao manual pelo Data Upload generico.
     */
    @Override
    public void removeEntityList(
            Collection<DistributionPlanIntegrationDataDto> entityList) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia upload por arquivo antes de qualquer parse.
     */
    @Override
    public String saveFile(
            MultipartFile multipartFile) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia upload JSON antes de reconciliar linhas persistidas.
     */
    @Override
    public String saveDTOList(
            IntegrationDto<
                    DistributionPlanIntegrationDataDto,
                    DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO,
                    EmptyIntegrationDataFilter,
                    IntegrationOptionsDto> integrationDto) {

        throw getReadOnlyException();

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Distribution Plan data exported";

    }

    @Override
    public com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationSupportData getSupportData() {

        return new com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationSupportData();

    }

    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Materializa linhas persistidas por envelope de supply plans para manter o
     * contrato generico consistente, sem abrir full scan.
     */
    @Override
    public Collection<DistributionPlanIntegrationDataDto> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO> primaryKeyCollection =
                com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationValidation.validaPrimaryKeyCollection(
                        dtoBatchList,
                        "Distribution Plan primary key collection");

        if (primaryKeyCollection.isEmpty()) {
            return List.of();
        }

        Set<Long> supplyPlanIdSet = primaryKeyCollection.stream()
                .map(primaryKey -> primaryKey.supplyPlanId)
                .collect(Collectors.toSet());

        supplyPlanPersistedBaselinePreflight.assertSupplyPlanIdsReadyForCanonicalRuntime(
                supplyPlanIdSet);

        return buildDtoList(
                distributionPlanItemRepository.customFindBySupplyPlanIdInForDistributionPlanExport(
                        supplyPlanIdSet),
                "Distribution Plan persisted collection");

    }

    /**
     * Full export sem filtro fica bloqueado para evitar dataset amplo.
     */
    @Override
    public Collection<DistributionPlanIntegrationDataDto> getAllPersistedEntities() {

        throw new UnsupportedOperationException(
                "Full Distribution Plan export requires a supplyPlanId filter.");

    }

    /**
     * Retorna DTOs de Distribution Plan de um Supply Plan especifico.
     */
    public List<DistributionPlanIntegrationDataDto> getDistributionPlanDTOList(
            Long supplyPlanId) {

        Long validatedSupplyPlanId =
                com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationValidation.validaSupplyPlanId(supplyPlanId);

        SupplyPlan supplyPlan = supplyPlanRepository.findById(validatedSupplyPlanId)
                .orElseThrow(() -> new DataUploadException("Supply Plan Id not found"));

        supplyPlanPersistedBaselinePreflight.assertSupplyPlanReadyForCanonicalRuntime(
                validatedSupplyPlanId);

        return buildDtoList(
                distributionPlanItemRepository.customFindBySupplyPlan(supplyPlan),
                "Distribution Plan persisted collection");

    }

    /**
     * Monta arquivo processado filtrado por Supply Plan.
     */
    public ProcessedFile getProcessedFileBySupplyPlanId(
            Long supplyPlanId) {

        return getMapper().convertEntityCollectionToProcessedFile(
                getDistributionPlanDTOList(supplyPlanId),
                getSupportData());

    }

    /**
     * Compatibilidade com o nome usado pelo controller legado.
     */
    public List<List<Object>> getFile(
            Long supplyPlanId) {

        return getProcessedFileBySupplyPlanId(supplyPlanId)
                .getFileContentsAsObjects();

    }

    private List<DistributionPlanIntegrationDataDto> buildDtoList(
            Collection<DistributionPlanItem> distributionPlanItemCollection,
            String collectionDescription) {

        if (distributionPlanItemCollection == null) {
            throw new IllegalStateException(collectionDescription + " returned null.");
        }

        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationValidation.validaUnidadeMedidaPadraoSnp(parametrosGlobais);

        List<DistributionPlanIntegrationDataDto> dtoList = new ArrayList<>();
        int indice = 0;
        for (DistributionPlanItem distributionPlanItem : distributionPlanItemCollection) {
            com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationValidation.validaDistributionPlanItem(
                    distributionPlanItem,
                    collectionDescription,
                    indice);
            dtoList.add(buildDTO(
                    distributionPlanItem,
                    parametrosGlobais));
            indice++;
        }

        com.opsfactor.community.capability.supplyplanning.distributionplan.integration.DistributionPlanIntegrationValidation.validaDtoCollection(
                dtoList,
                collectionDescription);
        return dtoList.stream()
                .sorted(Comparator
                        .comparing((DistributionPlanIntegrationDataDto dto) -> dto.primaryKeyDto.originLocationId)
                        .thenComparing(dto -> dto.primaryKeyDto.destinationLocationId)
                        .thenComparing(dto -> dto.primaryKeyDto.materialId)
                        .thenComparing(dto -> dto.primaryKeyDto.plannedDeliveryDate)
                        .thenComparing(dto -> dto.primaryKeyDto.suggestedOrderEmissionDate))
                .toList();

    }

    private DistributionPlanIntegrationDataDto buildDTO(
            DistributionPlanItem distributionPlanItem,
            ParametrosGlobais parametrosGlobais) {

        UnidadeMedida unidadeMedidaDistributionPlanItem =
                distributionPlanItem.getUnidadeMedida(parametrosGlobais);

        return DistributionPlanIntegrationDataDto.builder()
                .primaryKeyDto(new DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO(
                        distributionPlanItem.getSupplyPlan().getId(),
                        distributionPlanItem.getLocationOrigem().getId(),
                        distributionPlanItem.getLocationDestino().getId(),
                        distributionPlanItem.getProduto().getId(),
                        distributionPlanItem.getDataRecebimento(),
                        distributionPlanItem.getDataExpedicao()))
                .unitOfMeasureId(unidadeMedidaDistributionPlanItem.getId())
                .unconstrainedPlannedOrderQuantity(distributionPlanItem.getQuantidadeOrdemPlanejadaIrrestrita())
                .unconstrainedFirmOrderQuantity(distributionPlanItem.getQuantidadeOrdemFirmeIrrestrita())
                .constrainedPlannedOrderQuantity(distributionPlanItem.getQuantidadeOrdemPlanejadaRestrita())
                .constrainedFirmOrderQuantity(distributionPlanItem.getQuantidadeOrdemFirmeRestrita())
                .build();

    }

    private UnsupportedOperationException getReadOnlyException() {

        return new UnsupportedOperationException(
                "Distribution Plan Data Upload is read-only; quantities are persisted by Supply Planning execution.");

    }

}
