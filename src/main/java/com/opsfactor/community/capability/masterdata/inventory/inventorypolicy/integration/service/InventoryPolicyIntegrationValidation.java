package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.service;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesMaterialLocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validacoes compartilhadas pelos uploads Community de politica de estoque.
 *
 * <p>A classe mantem as regras de borda perto do recorte funcional: chaves
 * obrigatorias, snapshots persistidos integres, valores fisicos finitos e nao
 * negativos, e bloqueio explicito da frequencia de reabastecimento Enterprise.</p>
 */
final class InventoryPolicyIntegrationValidation {

    private InventoryPolicyIntegrationValidation() {

    }

    /**
     * Valida chaves de cabecalho antes de reduzir a colecao para ids.
     */
    static Collection<PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO> validaPoliticaEstoquesPrimaryKeyCollection(
            Collection<PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO> primaryKeyCollection) {

        if (primaryKeyCollection == null) {
            throw new DataUploadException("Inventory policy primary key collection is required.");
        }

        Set<String> inventoryPolicyIdSet = new HashSet<>();
        int indice = 0;
        for (PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO primaryKey : primaryKeyCollection) {
            if (primaryKey == null) {
                throw new DataUploadException("Inventory policy primary key collection item at index " + indice + " is required.");
            }
            if (primaryKey.id == null || primaryKey.id.isBlank()) {
                throw new DataUploadException("Inventory policy primary key must include id.");
            }
            if (!inventoryPolicyIdSet.add(primaryKey.id)) {
                throw new DataUploadException(
                        "Inventory policy primary key collection item at index "
                                + indice
                                + " has duplicated id "
                                + primaryKey.id
                                + ".");
            }
            indice++;
        }

        return primaryKeyCollection;

    }

    /**
     * Valida chaves de detalhe antes de buscar por envelope de politica.
     */
    static Collection<PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO> validaPoliticaEstoquesMaterialLocationPrimaryKeyCollection(
            Collection<PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO> primaryKeyCollection) {

        if (primaryKeyCollection == null) {
            throw new DataUploadException("Inventory policy detail primary key collection is required.");
        }

        Map<String, Map<String, Set<String>>> locationsPorPoliticaEMaterial =
                new HashMap<>();
        int indice = 0;
        for (PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO primaryKey : primaryKeyCollection) {
            if (primaryKey == null) {
                throw new DataUploadException("Inventory policy detail primary key collection item at index " + indice + " is required.");
            }
            if (primaryKey.inventoryPolicyId == null
                    || primaryKey.inventoryPolicyId.isBlank()
                    || primaryKey.materialId == null
                    || primaryKey.materialId.isBlank()
                    || primaryKey.locationId == null
                    || primaryKey.locationId.isBlank()) {
                throw new DataUploadException("Inventory policy detail primary key must include inventory policy, material and location.");
            }

            if (!locationsPorPoliticaEMaterial
                    .computeIfAbsent(primaryKey.inventoryPolicyId, ignored -> new HashMap<>())
                    .computeIfAbsent(primaryKey.materialId, ignored -> new HashSet<>())
                    .add(primaryKey.locationId)) {
                throw new DataUploadException(
                        "Inventory policy detail primary key collection item at index "
                                + indice
                                + " has duplicated key inventoryPolicyId "
                                + primaryKey.inventoryPolicyId
                                + " / materialId "
                                + primaryKey.materialId
                                + " / locationId "
                                + primaryKey.locationId
                                + ".");
            }
            indice++;
        }

        return primaryKeyCollection;

    }

    /**
     * Valida snapshot de cabecalho persistido antes de exportar ou atualizar o
     * mapa de entidades persistidas.
     */
    static Collection<PoliticaEstoques> validaPoliticaEstoquesEntityCollection(
            Collection<PoliticaEstoques> entityCollection,
            String snapshotDescription) {

        if (entityCollection == null) {
            throw new DataUploadException(snapshotDescription + " returned null.");
        }

        int indice = 0;
        for (PoliticaEstoques politicaEstoques : entityCollection) {
            if (politicaEstoques == null) {
                throw new DataUploadException(snapshotDescription + " returned null item at index " + indice + ".");
            }
            if (politicaEstoques.getId() == null || politicaEstoques.getId().isBlank()) {
                throw new DataUploadException(snapshotDescription + " returned item without id at index " + indice + ".");
            }
            indice++;
        }

        return entityCollection;

    }

    /**
     * Valida snapshot de detalhe e as grandezas fisicas operacionais antes de
     * salvar/exportar.
     */
    static Collection<PoliticaEstoquesMaterialLocation> validaPoliticaEstoquesMaterialLocationEntityCollection(
            Collection<PoliticaEstoquesMaterialLocation> entityCollection,
            String snapshotDescription) {

        return validaPoliticaEstoquesMaterialLocationEntityCollection(
                entityCollection,
                snapshotDescription,
                false);

    }

    /**
     * Valida a mesma fotografia de detalhe com a policy de frequencia recebida
     * pelo runtime chamador.
     *
     * <p>O Community mantem a frequencia bloqueada. O overlay Enterprise usa
     * este ponto estreito para aceitar somente valor finito e nao negativo,
     * preservando todas as demais validacoes estruturais e operacionais.</p>
     */
    static Collection<PoliticaEstoquesMaterialLocation> validaPoliticaEstoquesMaterialLocationEntityCollection(
            Collection<PoliticaEstoquesMaterialLocation> entityCollection,
            String snapshotDescription,
            boolean aceitaFrequenciaReabastecimento) {

        if (entityCollection == null) {
            throw new DataUploadException(snapshotDescription + " returned null.");
        }

        int indice = 0;
        for (PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation : entityCollection) {
            validaPoliticaEstoquesMaterialLocationEntity(
                    politicaEstoquesMaterialLocation,
                    snapshotDescription,
                    indice,
                    aceitaFrequenciaReabastecimento);
            indice++;
        }

        return entityCollection;

    }

    private static void validaPoliticaEstoquesMaterialLocationEntity(
            PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation,
            String snapshotDescription,
            int indice,
            boolean aceitaFrequenciaReabastecimento) {

        if (politicaEstoquesMaterialLocation == null) {
            throw new DataUploadException(snapshotDescription + " returned null item at index " + indice + ".");
        }
        if (politicaEstoquesMaterialLocation.getPoliticaEstoquesMaterialLocationCompositeKey() == null) {
            throw new DataUploadException(snapshotDescription + " returned item without primary key at index " + indice + ".");
        }

        PoliticaEstoques politicaEstoques = politicaEstoquesMaterialLocation.getPoliticaEstoques();
        if (politicaEstoques == null || politicaEstoques.getId() == null || politicaEstoques.getId().isBlank()) {
            throw new DataUploadException(snapshotDescription + " returned item without inventory policy id at index " + indice + ".");
        }

        Produto material = politicaEstoquesMaterialLocation.getMaterial();
        if (material == null || material.getId() == null || material.getId().isBlank()) {
            throw new DataUploadException(snapshotDescription + " returned item without material id at index " + indice + ".");
        }

        Location location = politicaEstoquesMaterialLocation.getLocation();
        if (location == null || location.getId() == null || location.getId().isBlank()) {
            throw new DataUploadException(snapshotDescription + " returned item without location id at index " + indice + ".");
        }

        validaValorNaoNegativoOuNulo(
                politicaEstoquesMaterialLocation.getEstoqueSegurancaDrpOuTargetKanbanCadastrado(),
                "safety stock / Kanban target",
                politicaEstoques.getId(),
                material.getId(),
                location.getId());
        validaValorNaoNegativoOuNulo(
                politicaEstoquesMaterialLocation.getEstoqueMaximoDrpCadastrado(),
                "maximum DRP stock",
                politicaEstoques.getId(),
                material.getId(),
                location.getId());

        Double frequenciaReabastecimentoDias =
                politicaEstoquesMaterialLocation.getFrequenciaReabastecimentoDiasCadastrado();
        if (!aceitaFrequenciaReabastecimento && frequenciaReabastecimentoDias != null) {
            throw new RequiresEnterpriseVersionException("Inventory policy optimization replenishment frequency");
        }
        if (aceitaFrequenciaReabastecimento) {
            validaValorNaoNegativoOuNulo(
                    frequenciaReabastecimentoDias,
                    "reorder frequency",
                    politicaEstoques.getId(),
                    material.getId(),
                    location.getId());
        }

    }

    private static void validaValorNaoNegativoOuNulo(
            Double valor,
            String nomeCampo,
            String inventoryPolicyId,
            String materialId,
            String locationId) {

        if (valor == null) {
            return;
        }
        if (!Double.isFinite(valor) || valor < 0.0d) {
            throw new DataUploadException(
                    "Inventory policy detail "
                            + nomeCampo
                            + " must be finite and non-negative for inventoryPolicyId "
                            + inventoryPolicyId
                            + " / materialId "
                            + materialId
                            + " / locationId "
                            + locationId
                            + ".");
        }

    }

}
