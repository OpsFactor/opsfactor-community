package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.service;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesMaterialLocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper.PoliticaEstoquesMaterialLocationIntegrationMapper;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper.PoliticaEstoquesMaterialLocationIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesMaterialLocationRepository;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesRepository;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Service Community de data upload do detalhe material/location de politica de estoque.
 *
 * <p>O service materializa politica, material e location uma vez por batch e
 * usa consultas com fetch join para exportar o detalhe sem N+1. Campos de
 * Inventory Policy Optimization Enterprise, como frequencia de
 * reabastecimento, sao rejeitados antes da persistencia.</p>
 */
@Component
public class PoliticaEstoquesMaterialLocationIntegrationService implements IntegrationServiceInterface<PoliticaEstoquesMaterialLocationIntegrationDataDto, PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO, PoliticaEstoquesMaterialLocation, PoliticaEstoquesMaterialLocationIntegrationSupportData, PoliticaEstoquesMaterialLocationIntegrationMapper, EmptyIntegrationDataFilter> {

    /**
     * Repository do detalhe material/location de politica de estoque.
     */
    @Autowired
    private PoliticaEstoquesMaterialLocationRepository politicaEstoquesMaterialLocationRepository;

    /**
     * Repository do cabecalho de politica usado para resolver a chave do detalhe.
     */
    @Autowired
    private PoliticaEstoquesRepository politicaEstoquesRepository;

    /**
     * Repository de materiais usados na chave operacional.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Repository de locations usadas na chave operacional.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Mapper do contrato publico do detalhe material/location.
     */
    @Autowired
    private PoliticaEstoquesMaterialLocationIntegrationMapper politicaEstoquesMaterialLocationIntegrationMapper;

    @Override
    public PoliticaEstoquesMaterialLocationIntegrationMapper getMapper() {

        return politicaEstoquesMaterialLocationIntegrationMapper;

    }

    /**
     * Salva o lote de detalhes em uma unica chamada ao repository.
     */
    @Override
    public List<PoliticaEstoquesMaterialLocation> saveEntityList(
            Collection<PoliticaEstoquesMaterialLocation> entityList) {

        validaPoliticaEstoquesMaterialLocationEntityCollection(
                entityList,
                "Inventory policy detail save batch");
        if (entityList.isEmpty()) {
            return new ArrayList<>();
        }

        return validaSavedEntityCollection(
                politicaEstoquesMaterialLocationRepository.saveAll(entityList),
                "Inventory policy detail saved collection",
                entityList.size());

    }

    /**
     * Remove detalhes selecionados pela infraestrutura generica de upload.
     */
    @Override
    public void removeEntityList(Collection<PoliticaEstoquesMaterialLocation> entityList) {

        validaPoliticaEstoquesMaterialLocationEntityCollection(
                entityList,
                "Inventory policy detail remove batch");
        if (!entityList.isEmpty()) {
            politicaEstoquesMaterialLocationRepository.deleteAll(entityList);
        }

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Inventory policy details saved";

    }

    /**
     * Carrega lookups obrigatorios do batch para resolver referencias no mapper.
     */
    @Override
    public PoliticaEstoquesMaterialLocationIntegrationSupportData getSupportData() {

        PoliticaEstoquesMaterialLocationIntegrationSupportData supportData =
                new PoliticaEstoquesMaterialLocationIntegrationSupportData();
        supportData.mapaPoliticaEstoquesPorId = getMapaPorIdObrigatorio(
                politicaEstoquesRepository.findAll(),
                politicaEstoques -> politicaEstoques.getId(),
                "Inventory Policy snapshot");
        supportData.mapaMaterialPorId = getMapaPorIdObrigatorio(
                produtoRepository.findAll(),
                material -> material.getId(),
                "Material snapshot");
        supportData.mapaLocationPorId = getMapaPorIdObrigatorio(
                locationRepository.findAll(),
                location -> location.getId(),
                "Location snapshot");

        return supportData;

    }

    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca detalhes persistidos para as politicas citadas no batch atual.
     *
     * <p>A consulta usa o envelope por politica para evitar uma query por DFU.
     * A reconciliacao final por chave completa continua no mapa generico da
     * infraestrutura de upload.</p>
     */
    @Override
    public Collection<PoliticaEstoquesMaterialLocation> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO> primaryKeyCollection =
                InventoryPolicyIntegrationValidation.validaPoliticaEstoquesMaterialLocationPrimaryKeyCollection(dtoBatchList);

        if (primaryKeyCollection.isEmpty()) {
            return List.of();
        }

        Set<String> inventoryPolicyIdSet = primaryKeyCollection.stream()
                .map(primaryKey -> primaryKey.inventoryPolicyId)
                .collect(Collectors.toSet());

        return validaPoliticaEstoquesMaterialLocationEntityCollection(
                politicaEstoquesMaterialLocationRepository.customFindByPoliticaEstoquesIdIn(inventoryPolicyIdSet),
                "Persisted inventory policy detail snapshot");

    }

    /**
     * Retorna todos os detalhes com fetch dos many-to-one usados por mapper/export.
     */
    @Override
    public Collection<PoliticaEstoquesMaterialLocation> getAllPersistedEntities() {

        return validaPoliticaEstoquesMaterialLocationEntityCollection(
                politicaEstoquesMaterialLocationRepository.customFindAll(),
                "Full inventory policy detail snapshot");

    }

    /**
     * Aplica as validacoes compartilhadas do snapshot com a policy de campo
     * definida pelo runtime Community ou Enterprise.
     */
    protected Collection<PoliticaEstoquesMaterialLocation>
            validaPoliticaEstoquesMaterialLocationEntityCollection(
                    Collection<PoliticaEstoquesMaterialLocation> entityCollection,
                    String snapshotDescription) {

        return InventoryPolicyIntegrationValidation
                .validaPoliticaEstoquesMaterialLocationEntityCollection(
                        entityCollection,
                        snapshotDescription,
                        aceitaFrequenciaReabastecimento());

    }

    /**
     * Community nao aceita a frequencia reservada a Inventory Policy
     * Optimization. O Enterprise a libera sem afrouxar os demais gates.
     */
    protected boolean aceitaFrequenciaReabastecimento() {

        return false;

    }

}
