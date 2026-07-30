package com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.service;

import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.dto.InventoryPlanIntegrationDataDto;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.mapper.InventoryPlanIntegrationMapper;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.mapper.InventoryPlanIntegrationSupportData;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.mapper.InventoryPlanIntegrationValidation;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLinhaRepository;
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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Community read-only para exportar o Inventory Plan simples.
 *
 * <p>Esta e a unica extracao de Supply Planning Data Upload liberada no
 * Community. O service exige filtro por {@code supplyPlanId}; full export,
 * POST, delete, lotes, cobertura em dias e demais subopcoes SNP continuam
 * fora deste recorte.</p>
 */
@Service
public class InventoryPlanIntegrationService implements IntegrationServiceInterface<
        InventoryPlanIntegrationDataDto,
        InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO,
        InventoryPlanIntegrationDataDto,
        InventoryPlanIntegrationSupportData,
        InventoryPlanIntegrationMapper,
        EmptyIntegrationDataFilter> {

    /**
     * Repository dos Supply Plans usados como filtro obrigatorio.
     */
    @Autowired
    private SupplyPlanRepository supplyPlanRepository;

    /**
     * Repository das linhas de Inventory Plan materializadas pelo Supply
     * Planning Community.
     */
    @Autowired
    private InventoryPlanLinhaRepository inventoryPlanLinhaRepository;

    /** Gate de valores baseline antigos antes de qualquer exportacao. */
    @Autowired
    private SupplyPlanPersistedBaselinePreflight supplyPlanPersistedBaselinePreflight;

    /**
     * Service de parametros globais usado para resolver a UOM padrao SNP.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Factory de conversoes de unidade para colunas derivadas.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /**
     * Mapper do contrato de arquivo/API de Inventory Plan.
     */
    @Autowired
    private InventoryPlanIntegrationMapper inventoryPlanIntegrationMapper;

    @Override
    public InventoryPlanIntegrationMapper getMapper() {

        return inventoryPlanIntegrationMapper;

    }

    /**
     * Bloqueia persistencia manual: Inventory Plan e output do calculo de
     * Supply Planning.
     */
    @Override
    public List<InventoryPlanIntegrationDataDto> saveEntityList(
            Collection<InventoryPlanIntegrationDataDto> entityList) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia remocao manual por Data Upload.
     */
    @Override
    public void removeEntityList(
            Collection<InventoryPlanIntegrationDataDto> entityList) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia upload de arquivo antes de qualquer parse.
     */
    @Override
    public String saveFile(
            MultipartFile multipartFile) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia upload JSON antes de reconciliar entidades.
     */
    @Override
    public String saveDTOList(
            IntegrationDto<
                    InventoryPlanIntegrationDataDto,
                    InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO,
                    EmptyIntegrationDataFilter,
                    IntegrationOptionsDto> integrationDto) {

        throw getReadOnlyException();

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Inventory Plan data exported";

    }

    @Override
    public InventoryPlanIntegrationSupportData getSupportData() {

        return new InventoryPlanIntegrationSupportData();

    }

    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Materializa linhas persistidas por envelope de supply plans para manter o
     * contrato generico consistente, embora o recorte seja read-only.
     */
    @Override
    public Collection<InventoryPlanIntegrationDataDto> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO> primaryKeyCollection =
                InventoryPlanIntegrationValidation.validaPrimaryKeyCollection(
                        dtoBatchList,
                        "Inventory Plan primary key collection");

        if (primaryKeyCollection.isEmpty()) {
            return List.of();
        }

        Set<Long> supplyPlanIdSet = primaryKeyCollection.stream()
                .map(primaryKey -> primaryKey.supplyPlanId)
                .collect(Collectors.toSet());

        supplyPlanPersistedBaselinePreflight.assertSupplyPlanIdsReadyForCanonicalRuntime(
                supplyPlanIdSet);

        return buildDtoList(
                inventoryPlanLinhaRepository.customFindBySupplyPlanIdInForInventoryPlanExport(
                        supplyPlanIdSet),
                "Inventory Plan persisted collection");

    }

    /**
     * Full export sem filtro fica bloqueado para evitar dataset amplo e porque
     * o contrato Community aprovado exige Supply Plan no path.
     */
    @Override
    public Collection<InventoryPlanIntegrationDataDto> getAllPersistedEntities() {

        throw new UnsupportedOperationException(
                "Full Inventory Plan export requires a supplyPlanId filter.");

    }

    /**
     * Retorna DTOs de Inventory Plan de um Supply Plan especifico.
     */
    public List<InventoryPlanIntegrationDataDto> getInventoryPlanDTOList(
            Long supplyPlanId) {

        Long validatedSupplyPlanId =
                InventoryPlanIntegrationValidation.validaSupplyPlanId(supplyPlanId);

        SupplyPlan supplyPlan = supplyPlanRepository.findById(validatedSupplyPlanId)
                .orElseThrow(() -> new DataUploadException("Supply Plan Id not found"));

        supplyPlanPersistedBaselinePreflight.assertSupplyPlanReadyForCanonicalRuntime(
                validatedSupplyPlanId);

        return buildDtoList(
                inventoryPlanLinhaRepository.customFindBySupplyPlan(supplyPlan),
                "Inventory Plan persisted collection");

    }

    /**
     * Monta arquivo processado filtrado por Supply Plan.
     */
    public ProcessedFile getProcessedFileBySupplyPlanId(
            Long supplyPlanId) {

        return getMapper().convertEntityCollectionToProcessedFile(
                getInventoryPlanDTOList(supplyPlanId),
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

    private List<InventoryPlanIntegrationDataDto> buildDtoList(
            Collection<InventoryPlanLinha> inventoryPlanLinhaCollection,
            String collectionDescription) {

        if (inventoryPlanLinhaCollection == null) {
            throw new IllegalStateException(collectionDescription + " returned null.");
        }

        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        UnidadeMedida unidadeMedidaPadraoSnp =
                InventoryPlanIntegrationValidation.validaUnidadeMedidaPadraoSnp(parametrosGlobais);
        UnidadeMedidaProjection unidadeMedidaProjection =
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();

        List<InventoryPlanIntegrationDataDto> dtoList = inventoryPlanLinhaCollection.stream()
                .sorted(Comparator
                        .comparing((InventoryPlanLinha linha) -> linha.getLocation().getId())
                        .thenComparing(linha -> linha.getProduto().getId())
                        .thenComparing(InventoryPlanLinha::getDataReferencia))
                .map(inventoryPlanLinha -> buildDTO(
                        inventoryPlanLinha,
                        parametrosGlobais,
                        unidadeMedidaPadraoSnp,
                        unidadeMedidaProjection))
                .collect(Collectors.toList());

        InventoryPlanIntegrationValidation.validaDtoCollection(
                dtoList,
                collectionDescription);
        return dtoList;

    }

    private InventoryPlanIntegrationDataDto buildDTO(
            InventoryPlanLinha inventoryPlanLinha,
            ParametrosGlobais parametrosGlobais,
            UnidadeMedida unidadeMedidaPadraoSnp,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        Produto material = inventoryPlanLinha.getProduto();
        UnidadeMedida unidadeMedidaInventoryPlanLinha =
                inventoryPlanLinha.getUnidadeMedida(parametrosGlobais);

        InventoryPlanIntegrationDataDto dto =
                InventoryPlanIntegrationDataDto.builder()
                        .primaryKeyDto(new InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO(
                                inventoryPlanLinha.getSupplyPlan().getId(),
                                inventoryPlanLinha.getLocation().getId(),
                                material.getId(),
                                inventoryPlanLinha.getDataReferencia()))
                        .unitOfMeasureId(unidadeMedidaInventoryPlanLinha.getId())
                        .safetyStockQuantity(inventoryPlanLinha.getQuantidadeEstoqueSegurancaIrrestrito())
                        .maximumStockQuantity(inventoryPlanLinha.getQuantidadeEstoqueMaximoIrrestrito())
                        .projectedStockWorkingVersion(inventoryPlanLinha.getQuantidadeEstoqueProjetadoTrabalho())
                        .projectedStockUnconstrainedVersion(inventoryPlanLinha.getQuantidadeEstoqueProjetadoIrrestrito())
                        .projectedStockConstrainedVersion(inventoryPlanLinha.getQuantidadeEstoqueProjetadoRestrito())
                        .defaultSnpUnitOfMeasureId(unidadeMedidaPadraoSnp.getId())
                        .build();

        populateDefaultSnpUnitQuantities(
                dto,
                inventoryPlanLinha,
                material,
                unidadeMedidaInventoryPlanLinha,
                unidadeMedidaPadraoSnp,
                unidadeMedidaProjection);
        return dto;

    }

    private void populateDefaultSnpUnitQuantities(
            InventoryPlanIntegrationDataDto dto,
            InventoryPlanLinha inventoryPlanLinha,
            Produto material,
            UnidadeMedida unidadeMedidaInventoryPlanLinha,
            UnidadeMedida unidadeMedidaPadraoSnp,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        try {
            double conversaoParaUnidadePadraoSnp =
                    unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                            material,
                            unidadeMedidaInventoryPlanLinha,
                            unidadeMedidaPadraoSnp);

            dto.projectedStockWorkingVersionDefaultSnpUom =
                    inventoryPlanLinha.getQuantidadeEstoqueProjetadoTrabalho()
                            * conversaoParaUnidadePadraoSnp;
            dto.projectedStockUnconstrainedVersionDefaultSnpUom =
                    inventoryPlanLinha.getQuantidadeEstoqueProjetadoIrrestrito()
                            * conversaoParaUnidadePadraoSnp;
            dto.projectedStockConstrainedVersionDefaultSnpUom =
                    inventoryPlanLinha.getQuantidadeEstoqueProjetadoRestrito()
                            * conversaoParaUnidadePadraoSnp;
        } catch (UnitOfMeasureConversionException unitOfMeasureConversionException) {
            String mensagemErroConversao = "No conversion from "
                    + unidadeMedidaInventoryPlanLinha.getId()
                    + " to "
                    + unidadeMedidaPadraoSnp.getId();

            dto.projectedStockWorkingVersionDefaultSnpUom = mensagemErroConversao;
            dto.projectedStockUnconstrainedVersionDefaultSnpUom = mensagemErroConversao;
            dto.projectedStockConstrainedVersionDefaultSnpUom = mensagemErroConversao;
        }

    }

    private UnsupportedOperationException getReadOnlyException() {

        return new UnsupportedOperationException(
                "Inventory Plan Data Upload is read-only; quantities are persisted by Supply Planning execution.");

    }

}
