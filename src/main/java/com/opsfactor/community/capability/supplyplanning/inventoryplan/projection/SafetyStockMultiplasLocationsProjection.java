package com.opsfactor.community.capability.supplyplanning.inventoryplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningMultiplasLocationsProjection;
import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Agrega as projections de safety stock por location em um contrato
 * multi-location compartilhado.
 * <p>
 * O papel desta classe espelha o de {@link SupplyPlanningMultiplasLocationsProjection}:
 * ela organiza um contrato multi-location explícito que pode ser injetado como
 * input opcional de motores Enterprise sem carregar uma projection unitária
 * bruta para dentro dos loaders e mappers.
 */
@Getter
public class SafetyStockMultiplasLocationsProjection {

    /**
     * Índice principal por location.
     */
    private final Map<Location, SafetyStockProjection> mapaSafetyStockProjectionPorLocation = new HashMap<>();

    public SafetyStockMultiplasLocationsProjection(Collection<SafetyStockProjection> safetyStockProjections) {

        if (safetyStockProjections == null) {
            return;
        }

        int indiceSafetyStockProjection = 0;
        for (SafetyStockProjection safetyStockProjection : safetyStockProjections) {
            validaSafetyStockProjection(
                    safetyStockProjection,
                    indiceSafetyStockProjection);

            /*
             * O snapshot multi-location possui uma única projection de safety
             * stock por location. Sobrescrever silenciosamente faria loaders
             * Community/Enterprise dependerem da ordem da coleção recebida.
             */
            SafetyStockProjection safetyStockProjectionAnterior =
                    mapaSafetyStockProjectionPorLocation.putIfAbsent(
                            safetyStockProjection.getLocation(),
                            safetyStockProjection);

            if (safetyStockProjectionAnterior != null
                    && safetyStockProjectionAnterior != safetyStockProjection) {
                throw new IllegalArgumentException(
                        "SafetyStockMultiplasLocationsProjection received duplicated safety stock projection for location "
                                + safetyStockProjection.getLocation().getId()
                                + ".");
            }

            indiceSafetyStockProjection++;
        }

    }

    public SafetyStockProjection getSafetyStockProjectionDeLocation(Location location) {
        return mapaSafetyStockProjectionPorLocation.get(location);
    }

    /**
     * Valida cada projection unitária antes de indexar por location.
     *
     * <p>Coleção nula representa ausência de safety stock e segue como
     * snapshot vazio. Item nulo ou projection sem location, por outro lado,
     * indicam quebra do contrato de montagem do input de Supply Planning.</p>
     */
    private void validaSafetyStockProjection(
            SafetyStockProjection safetyStockProjection,
            int indiceSafetyStockProjection) {

        if (safetyStockProjection == null) {
            throw new IllegalArgumentException(
                    "SafetyStockMultiplasLocationsProjection received null safety stock projection at index "
                            + indiceSafetyStockProjection
                            + ".");
        }
        if (safetyStockProjection.getLocation() == null) {
            throw new IllegalArgumentException(
                    "SafetyStockMultiplasLocationsProjection received safety stock projection without location at index "
                            + indiceSafetyStockProjection
                            + ".");
        }

    }

    /**
     * Resume se existe alguma política de safety stock utilizável no cenário.
     */
    public boolean verificaSeHaPoliticaEstoquesMaterialLocationCadastrada() {
        return mapaSafetyStockProjectionPorLocation.values()
                .stream()
                .anyMatch(SafetyStockProjection::verificaSeHaPoliticaEstoquesMaterialLocationCadastrada);
    }
}
