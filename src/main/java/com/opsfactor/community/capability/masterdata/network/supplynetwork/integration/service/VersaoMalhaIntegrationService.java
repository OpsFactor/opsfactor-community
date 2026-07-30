package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.VersaoMalhaIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.VersaoMalhaIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.VersaoMalhaIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Service de integracao da versao de malha Community.
 *
 * <p>O upload mantém apenas o cadastro operacional da malha usado por Supply
 * Planning e transportation lanes. O merge busca registros persistidos pelos
 * ids do lote, e o support data de locations e carregado uma vez por request
 * para evitar consultas por linha no mapper.</p>
 */
@Service
public class VersaoMalhaIntegrationService implements IntegrationServiceInterface<
        VersaoMalhaIntegrationDataDto,
        VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO,
        VersaoMalha,
        VersaoMalhaIntegrationSupportData,
        VersaoMalhaIntegrationMapper,
        EmptyIntegrationDataFilter> {

    /**
     * Repository canonico da versao de malha.
     */
    @Autowired
    private VersaoMalhaRepository versaoMalhaRepository;

    /**
     * Repository de locations usado para resolver as origens padrao opcionais.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Mapper do contrato publico `supplynetworkversion`.
     */
    @Autowired
    private VersaoMalhaIntegrationMapper versaoMalhaIntegrationMapper;

    @Override
    public VersaoMalhaIntegrationMapper getMapper() {

        return versaoMalhaIntegrationMapper;

    }

    /**
     * Persiste as versoes de malha em lote.
     */
    @Override
    public List<VersaoMalha> saveEntityList(Collection<VersaoMalha> entityList) {

        if (!entityList.isEmpty()) {
            return validaSavedEntityCollection(
                    versaoMalhaRepository.saveAll(entityList),
                    "Supply Network Version saved collection",
                    entityList.size());
        }
        return new ArrayList<>();

    }

    /**
     * Remove versoes de malha em lote quando o arquivo/API marcar `D`.
     */
    @Override
    public void removeEntityList(Collection<VersaoMalha> entityList) {

        if (!entityList.isEmpty()) {
            versaoMalhaRepository.deleteInBatch(entityList);
        }

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Supply Network Version data uploaded";

    }

    /**
     * Carrega locations uma vez para resolver campos opcionais de origem.
     */
    @Override
    public VersaoMalhaIntegrationSupportData getSupportData() {

        VersaoMalhaIntegrationSupportData versaoMalhaIntegrationSupportData =
                new VersaoMalhaIntegrationSupportData();
        versaoMalhaIntegrationSupportData.mapaLocationPorId = getMapaPorIdObrigatorio(
                locationRepository.findAll(),
                location -> location.getId(),
                "Location snapshot");
        return versaoMalhaIntegrationSupportData;

    }

    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca somente as versoes de malha referenciadas pelo lote atual.
     */
    @Override
    public Collection<VersaoMalha> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO> dtoBatchList) {

        validaVersaoMalhaPrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        Set<String> idsInBatch = dtoBatchList.stream()
                .map(dto -> dto.supplyNetworkVersionId)
                .collect(Collectors.toSet());
        return versaoMalhaRepository.findAllById(idsInBatch);

    }

    /**
     * Retorna a base completa com origins carregadas para export.
     */
    @Override
    public Collection<VersaoMalha> getAllPersistedEntities() {

        return versaoMalhaRepository.customFindAll();

    }

    /**
     * Valida chave primaria antes de converter o lote em conjunto de ids.
     */
    private void validaVersaoMalhaPrimaryKeyCollection(
            Collection<VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Supply Network Version primary key collection is required.");
        }

        Set<String> supplyNetworkVersionIds = new HashSet<>();
        int index = 0;
        for (VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO primaryKeyDto : dtoBatchList) {
            if (primaryKeyDto == null) {
                throw new DataUploadException(
                        "Supply Network Version primary key collection item at index "
                                + index
                                + " is required.");
            }
            if (primaryKeyDto.supplyNetworkVersionId == null
                    || primaryKeyDto.supplyNetworkVersionId.isBlank()) {
                throw new DataUploadException(
                        "Supply Network Version upload primary key must include supply network version id");
            }
            if (!supplyNetworkVersionIds.add(primaryKeyDto.supplyNetworkVersionId)) {
                throw new DataUploadException(
                        "Supply Network Version primary key collection item at index "
                                + index
                                + " has duplicated key supplyNetworkVersionId "
                                + primaryKeyDto.supplyNetworkVersionId
                                + ".");
            }
            index++;
        }

    }

}
