package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaMaterialRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.EnterpriseValorCaracteristicaMaterialRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaProduto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service Enterprise de integracao de valores de caracteristica por material.
 */
@Service
public class MaterialCharacteristicValueIntegrationService implements IntegrationServiceInterface<
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto,
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO,
        ValorCaracteristicaProduto,
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationSupportData,
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationMapper,
        EmptyIntegrationDataFilter> {

    /**
     * Repository Enterprise dos valores por material/caracteristica.
     */
    @Autowired
    private EnterpriseValorCaracteristicaMaterialRepository enterpriseValorCaracteristicaMaterialRepository;

    /**
     * Repository Community de materiais usados como chave da carga.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Repository Enterprise do catalogo de caracteristicas de material.
     */
    @Autowired
    private CaracteristicaMaterialRepository caracteristicaMaterialRepository;

    /**
     * Mapper Enterprise de arquivo/API.
     */
    @Autowired
    private com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationMapper materialCharacteristicValueIntegrationMapper;

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationMapper getMapper() {

        return materialCharacteristicValueIntegrationMapper;

    }

    @Override
    public List<ValorCaracteristicaProduto> saveEntityList(
            Collection<ValorCaracteristicaProduto> entityList) {

        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.validaEntityCollection(
                entityList,
                "Material Characteristic Value entity collection");

        if (entityList.isEmpty()) {
            return new ArrayList<>();
        }

        return validaSavedEntityCollection(
                enterpriseValorCaracteristicaMaterialRepository.saveAll(entityList),
                "Material Characteristic Value saved collection",
                entityList.size());

    }

    @Override
    public void removeEntityList(Collection<ValorCaracteristicaProduto> entityList) {

        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.validaEntityCollection(
                entityList,
                "Material Characteristic Value entity collection");

        if (!entityList.isEmpty()) {
            enterpriseValorCaracteristicaMaterialRepository.deleteAll(entityList);
        }

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Material Characteristic Value data saved";

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationSupportData getSupportData() {

        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationSupportData supportData =
                new com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationSupportData();
        supportData.materialPorId = getMaterialPorId();
        supportData.materialCharacteristicById = getMaterialCharacteristicById();
        return supportData;

    }

    @Override
    public int getBatchSize() {

        return 1000;

    }

    @Override
    public Collection<ValorCaracteristicaProduto> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO> materialCharacteristicValuePrimaryKeyDtoCollection =
                com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.validaPrimaryKeyDtoCollection(
                        dtoBatchList,
                        "Material Characteristic Value primary key collection",
                        (primaryKeyDto, index) -> {
                            String materialId = com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.getRequiredPrimaryKeyField(
                                    primaryKeyDto.materialId,
                                    "materialId",
                                    "Material Characteristic Value primary key collection",
                                    index);
                            String materialCharacteristicId =
                                    com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.getRequiredPrimaryKeyField(
                                            primaryKeyDto.materialCharacteristicId,
                                            "materialCharacteristicId",
                                            "Material Characteristic Value primary key collection",
                                            index);
                            return "material "
                                    + materialId
                                    + " / materialCharacteristic "
                                    + materialCharacteristicId;
                        });

        if (materialCharacteristicValuePrimaryKeyDtoCollection.isEmpty()) {
            return List.of();
        }

        Set<com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO> primaryKeys =
                new HashSet<>(materialCharacteristicValuePrimaryKeyDtoCollection);

        return getAllPersistedEntities()
                .stream()
                .filter(valorCaracteristicaProduto -> primaryKeys.contains(
                        materialCharacteristicValueIntegrationMapper.getPrimaryKeyDtoFromEntity(
                                valorCaracteristicaProduto)))
                .toList();

    }

    @Override
    public Collection<ValorCaracteristicaProduto> getAllPersistedEntities() {

        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.validaPersistedEntityCollection(
                        enterpriseValorCaracteristicaMaterialRepository.customFindAllComCaracteristicaEMaterial(),
                        "Material Characteristic Value persisted collection")
                .stream()
                .sorted(Comparator
                        .comparing((ValorCaracteristicaProduto valorCaracteristicaProduto) ->
                                valorCaracteristicaProduto.getProduto().getId())
                        .thenComparing(valorCaracteristicaProduto ->
                                valorCaracteristicaProduto.getCaracteristicaProduto().getId()))
                .toList();

    }

    private java.util.Map<String, Produto> getMaterialPorId() {

        List<Produto> materiais = produtoRepository.findAll();
        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.getSupportEntityByIdMap(
                materiais,
                "Material support collection",
                Produto::getId,
                null);

    }

    private java.util.Map<String, CaracteristicaProduto> getMaterialCharacteristicById() {

        List<CaracteristicaProduto> caracteristicasProduto =
                caracteristicaMaterialRepository.findAll();
        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.getSupportEntityByIdMap(
                caracteristicasProduto,
                "Material Characteristic support collection",
                CaracteristicaProduto::getId,
                caracteristicaProduto -> {
                    if (caracteristicaProduto == null
                            || caracteristicaProduto.getTipoCaracteristica() == null) {
                        throw new IllegalStateException(
                                "Material Characteristic support collection returned invalid characteristic.");
                    }
                });

    }

}
