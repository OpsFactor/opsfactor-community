package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.service;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper.PoliticaEstoquesIntegrationMapper;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper.PoliticaEstoquesIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Community de data upload do cabecalho de politica de estoque.
 *
 * <p>Este cadastro define prioridade e vigencia para regras operacionais de
 * safety stock. O service nao publica filtros Enterprise nem parametros de
 * otimizacao; ele apenas persiste o cabecalho usado pelo detalhe
 * material/location.</p>
 */
@Component
public class PoliticaEstoquesIntegrationService implements IntegrationServiceInterface<PoliticaEstoquesIntegrationDataDto, PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO, PoliticaEstoques, PoliticaEstoquesIntegrationSupportData, PoliticaEstoquesIntegrationMapper, EmptyIntegrationDataFilter> {

    /**
     * Repository do cabecalho de politica de estoque.
     */
    @Autowired
    private PoliticaEstoquesRepository politicaEstoquesRepository;

    /**
     * Mapper do contrato publico de data upload do cabecalho.
     */
    @Autowired
    private PoliticaEstoquesIntegrationMapper politicaEstoquesIntegrationMapper;

    @Override
    public PoliticaEstoquesIntegrationMapper getMapper() {

        return politicaEstoquesIntegrationMapper;

    }

    /**
     * Salva o lote de cabecalhos em uma unica chamada ao repository.
     */
    @Override
    public List<PoliticaEstoques> saveEntityList(Collection<PoliticaEstoques> entityList) {

        InventoryPolicyIntegrationValidation.validaPoliticaEstoquesEntityCollection(
                entityList,
                "Inventory policy save batch");
        if (entityList.isEmpty()) {
            return new ArrayList<>();
        }

        return validaSavedEntityCollection(
                politicaEstoquesRepository.saveAll(entityList),
                "Inventory policy saved collection",
                entityList.size());

    }

    /**
     * Remove cabecalhos selecionados pela infraestrutura generica de upload.
     */
    @Override
    public void removeEntityList(Collection<PoliticaEstoques> entityList) {

        InventoryPolicyIntegrationValidation.validaPoliticaEstoquesEntityCollection(
                entityList,
                "Inventory policy remove batch");
        if (!entityList.isEmpty()) {
            politicaEstoquesRepository.deleteAll(entityList);
        }

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Inventory policies saved";

    }

    @Override
    public PoliticaEstoquesIntegrationSupportData getSupportData() {

        return new PoliticaEstoquesIntegrationSupportData();

    }

    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca somente os cabecalhos citados no batch atual.
     */
    @Override
    public Collection<PoliticaEstoques> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO> primaryKeyCollection =
                InventoryPolicyIntegrationValidation.validaPoliticaEstoquesPrimaryKeyCollection(dtoBatchList);

        if (primaryKeyCollection.isEmpty()) {
            return List.of();
        }

        Set<String> inventoryPolicyIdSet = primaryKeyCollection.stream()
                .map(primaryKey -> primaryKey.id)
                .collect(Collectors.toSet());

        return InventoryPolicyIntegrationValidation.validaPoliticaEstoquesEntityCollection(
                politicaEstoquesRepository.findAllById(inventoryPolicyIdSet),
                "Persisted inventory policy snapshot");

    }

    /**
     * Retorna todos os cabecalhos para export/DTO completo.
     */
    @Override
    public Collection<PoliticaEstoques> getAllPersistedEntities() {

        return InventoryPolicyIntegrationValidation.validaPoliticaEstoquesEntityCollection(
                politicaEstoquesRepository.findAll(),
                "Full inventory policy snapshot");

    }

}
