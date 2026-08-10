package com.opsfactor.community.capability.supplyplanning.productionplan.integration;

import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoInexistente;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
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
 * Service Enterprise read-only para exportacao do Production Plan no nivel de
 * volume por roteiro/BOM.
 */
@Service
public class ProductionPlanVolumeIntegrationService implements IntegrationServiceInterface<
        ProductionPlanVolumeIntegrationDataDto,
        ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO,
        ProductionPlanVolumeIntegrationDataDto,
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationSupportData,
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationMapper,
        EmptyIntegrationDataFilter> {

    /**
     * Repository Community dos supply plans usados como filtro obrigatorio.
     */
    @Autowired
    private SupplyPlanRepository supplyPlanRepository;

    /**
     * Repository Community das linhas de Production Plan persistidas pelo
     * calculo de Supply Planning.
     */
    @Autowired
    private ProductionPlanLinhaRepository productionPlanLinhaRepository;

    /** Gate Community compartilhado das series baseline persistidas antigas. */
    @Autowired
    private SupplyPlanPersistedBaselinePreflight supplyPlanPersistedBaselinePreflight;

    /**
     * Service Community de parametros globais para resolver a UOM padrao SNP.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Factory Community da projection de conversoes de unidade.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /**
     * Mapper Enterprise do contrato de arquivo/API de Production Plan Volume.
     */
    @Autowired
    private com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationMapper productionPlanVolumeIntegrationMapper;

    @Override
    public com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationMapper getMapper() {

        return productionPlanVolumeIntegrationMapper;

    }

    /**
     * Bloqueia persistencia manual: Production Plan Volume e output do motor
     * de Supply Planning.
     */
    @Override
    public List<ProductionPlanVolumeIntegrationDataDto> saveEntityList(
            Collection<ProductionPlanVolumeIntegrationDataDto> entityList) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia remocao manual pelo Data Upload generico.
     */
    @Override
    public void removeEntityList(
            Collection<ProductionPlanVolumeIntegrationDataDto> entityList) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia upload por arquivo antes de qualquer parse de dados.
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
                    ProductionPlanVolumeIntegrationDataDto,
                    ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO,
                    EmptyIntegrationDataFilter,
                    IntegrationOptionsDto> integrationDto) {

        throw getReadOnlyException();

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Production Plan Volume data exported";

    }

    @Override
    public com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationSupportData getSupportData() {

        return new com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationSupportData();

    }

    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Reconciliacao por envelope de supply plans, usada apenas se a superficie
     * generica tentar materializar chaves deste recorte read-only.
     */
    @Override
    public Collection<ProductionPlanVolumeIntegrationDataDto> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO> primaryKeyCollection =
                com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationValidation.validaPrimaryKeyCollection(
                        dtoBatchList,
                        "Production Plan Volume primary key collection");

        if (primaryKeyCollection.isEmpty()) {
            return List.of();
        }

        Set<Long> supplyPlanIds = primaryKeyCollection.stream()
                .map(primaryKey -> primaryKey.supplyPlanId)
                .collect(Collectors.toSet());

        supplyPlanPersistedBaselinePreflight.assertSupplyPlanIdsReadyForCanonicalRuntime(
                supplyPlanIds);

        return buildDtoList(
                productionPlanLinhaRepository.customFindBySupplyPlanIdInForProductionPlanVolumeExport(
                        supplyPlanIds),
                "Production Plan Volume persisted collection");

    }

    /**
     * Full export sem filtro e bloqueado para evitar dataset amplo demais.
     */
    @Override
    public Collection<ProductionPlanVolumeIntegrationDataDto> getAllPersistedEntities() {

        throw new UnsupportedOperationException(
                "Full Production Plan Volume export requires a supplyPlanId filter.");

    }

    /**
     * Extrai o Production Plan Volume de um supply plan especifico.
     */
    public List<ProductionPlanVolumeIntegrationDataDto> getProductionPlanVolumeDTOList(
            Long supplyPlanId) {

        Long validatedSupplyPlanId =
                com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationValidation.validaSupplyPlanId(supplyPlanId);

        supplyPlanRepository.findById(validatedSupplyPlanId)
                .orElseThrow(() -> new DataUploadException("Supply Plan Id not found"));

        supplyPlanPersistedBaselinePreflight.assertSupplyPlanReadyForCanonicalRuntime(
                validatedSupplyPlanId);

        return buildDtoList(
                productionPlanLinhaRepository.customFindBySupplyPlanIdForProductionPlanVolumeExport(
                        validatedSupplyPlanId),
                "Production Plan Volume persisted collection");

    }

    /**
     * Gera arquivo processado filtrado por supply plan.
     */
    public ProcessedFile getProcessedFileBySupplyPlanId(
            Long supplyPlanId) {

        return getMapper().convertEntityCollectionToProcessedFile(
                getProductionPlanVolumeDTOList(supplyPlanId),
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

    private List<ProductionPlanVolumeIntegrationDataDto> buildDtoList(
            Collection<ProductionPlanLinha> productionPlanLinhas,
            String collectionDescription) {

        if (productionPlanLinhas == null) {
            throw new IllegalStateException(collectionDescription + " returned null.");
        }

        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        UnidadeMedidaProjection unidadeMedidaProjection =
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();

        List<ProductionPlanVolumeIntegrationDataDto> dtoList = productionPlanLinhas.stream()
                .sorted(Comparator
                        .comparing((ProductionPlanLinha linha) -> linha.getLocation().getId())
                        .thenComparing(linha -> linha.getMaterialOutput().getId())
                        .thenComparing(ProductionPlanLinha::getDataReferencia)
                        .thenComparing(linha -> linha.getRoteiro().getId())
                        .thenComparing(linha -> linha.getListaTecnica().getId()))
                .map(productionPlanLinha -> buildDTO(
                        productionPlanLinha,
                        parametrosGlobais,
                        unidadeMedidaProjection))
                .collect(Collectors.toList());

        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanVolumeIntegrationValidation.validaDtoCollection(
                dtoList,
                collectionDescription);
        return dtoList;

    }

    private ProductionPlanVolumeIntegrationDataDto buildDTO(
            ProductionPlanLinha productionPlanLinha,
            ParametrosGlobais parametrosGlobais,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        Produto materialOutput = productionPlanLinha.getMaterialOutput();
        UnidadeMedida unidadeMedidaProductionPlanLinha =
                productionPlanLinha.getUnidadeMedida(parametrosGlobais);
        VersaoProducao versaoProducao =
                productionPlanLinha.getVersaoProducaoCadastrada();
        ProductionPlanVolumeIntegrationDataDto dto =
                ProductionPlanVolumeIntegrationDataDto.builder()
                        .primaryKeyDto(new ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO(
                                productionPlanLinha.getSupplyPlan().getId(),
                                productionPlanLinha.getLocation().getId(),
                                materialOutput.getId(),
                                productionPlanLinha.getDataReferencia(),
                                (versaoProducao instanceof VersaoProducaoInexistente)
                                        ? null
                                        : versaoProducao.getId(),
                                productionPlanLinha.getRoteiro().getId(),
                                productionPlanLinha.getListaTecnica().getId()))
                        .unitOfMeasureId(unidadeMedidaProductionPlanLinha.getId())
                        .unconstrainedPlannedOrderQuantity(
                                productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoIrrestrita())
                        .unconstrainedFirmOrderQuantity(
                                productionPlanLinha.getQuantidadeOrdemFirmeProducaoIrrestrita())
                        .constrainedPlannedOrderQuantity(
                                productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoRestrita())
                        .constrainedFirmOrderQuantity(
                                productionPlanLinha.getQuantidadeOrdemFirmeProducaoRestrita())
                        .defaultSnpUnitOfMeasureId(
                                parametrosGlobais.getUnidadeMedidaPadraoSNP().getId())
                        .build();

        populateDefaultSnpUnitQuantities(
                dto,
                productionPlanLinha,
                materialOutput,
                unidadeMedidaProductionPlanLinha,
                parametrosGlobais,
                unidadeMedidaProjection);
        return dto;

    }

    private void populateDefaultSnpUnitQuantities(
            ProductionPlanVolumeIntegrationDataDto dto,
            ProductionPlanLinha productionPlanLinha,
            Produto materialOutput,
            UnidadeMedida unidadeMedidaProductionPlanLinha,
            ParametrosGlobais parametrosGlobais,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        try {
            double conversaoParaUnidadePadraoSNP =
                    unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                            materialOutput,
                            unidadeMedidaProductionPlanLinha,
                            parametrosGlobais.getUnidadeMedidaPadraoSNP());

            dto.unconstrainedPlannedOrderQuantityDefaultSnpUom =
                    productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoIrrestrita()
                            * conversaoParaUnidadePadraoSNP;
            dto.unconstrainedFirmOrderQuantityDefaultSnpUom =
                    productionPlanLinha.getQuantidadeOrdemFirmeProducaoIrrestrita()
                            * conversaoParaUnidadePadraoSNP;
            dto.constrainedPlannedOrderQuantityDefaultSnpUom =
                    productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoRestrita()
                            * conversaoParaUnidadePadraoSNP;
            dto.constrainedFirmOrderQuantityDefaultSnpUom =
                    productionPlanLinha.getQuantidadeOrdemFirmeProducaoRestrita()
                            * conversaoParaUnidadePadraoSNP;
        } catch (UnitOfMeasureConversionException unitOfMeasureConversionException) {
            String mensagemErroConversao = "No conversion from "
                    + unidadeMedidaProductionPlanLinha.getId()
                    + " to "
                    + parametrosGlobais.getUnidadeMedidaPadraoSNP().getId();

            dto.unconstrainedPlannedOrderQuantityDefaultSnpUom = mensagemErroConversao;
            dto.unconstrainedFirmOrderQuantityDefaultSnpUom = mensagemErroConversao;
            dto.constrainedPlannedOrderQuantityDefaultSnpUom = mensagemErroConversao;
            dto.constrainedFirmOrderQuantityDefaultSnpUom = mensagemErroConversao;
        }

    }

    private UnsupportedOperationException getReadOnlyException() {

        return new UnsupportedOperationException(
                "Production Plan Volume Data Upload is read-only; quantities are persisted by Supply Planning execution.");

    }

}
