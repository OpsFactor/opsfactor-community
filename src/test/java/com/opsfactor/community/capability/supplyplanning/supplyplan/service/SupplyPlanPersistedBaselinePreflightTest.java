package com.opsfactor.community.capability.supplyplanning.supplyplan.service;

import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanLegacyBaselineRequirement;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLegacyBaselineRequirement;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Contrato do gate que separa definitivamente series baseline persistidas das
 * colunas canonicas de Supply Planning.
 */
class SupplyPlanPersistedBaselinePreflightTest {

    @Test
    void canonicalSupplyPlanShouldUseOneScalarReadPerSeries() {

        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        DistributionPlanItemRepository distributionPlanItemRepository =
                Mockito.mock(DistributionPlanItemRepository.class);
        ProductionPlanLinhaRepository productionPlanLinhaRepository =
                Mockito.mock(ProductionPlanLinhaRepository.class);
        stubEmptyRequirements(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

        SupplyPlanPersistedBaselinePreflight preflight = getPreflight(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

        Assertions.assertDoesNotThrow(
                () -> preflight.assertSupplyPlanReadyForCanonicalRuntime(41L));

        ArgumentCaptor<Set<Long>> supplyPlanIdCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(inventoryPlanLinhaRepository)
                .findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        supplyPlanIdCaptor.capture(),
                        pageableCaptor.capture());
        Mockito.verify(distributionPlanItemRepository)
                .findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.eq(Set.of(41L)),
                        Mockito.any(Pageable.class));
        Mockito.verify(productionPlanLinhaRepository)
                .findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.eq(Set.of(41L)),
                        Mockito.any(Pageable.class));
        Mockito.verify(inventoryPlanLinhaRepository)
                .countLegacyBaselineRequirementsBySupplyPlanIdIn(Set.of(41L));
        Mockito.verify(distributionPlanItemRepository)
                .countLegacyBaselineRequirementsBySupplyPlanIdIn(Set.of(41L));
        Mockito.verify(productionPlanLinhaRepository)
                .countLegacyBaselineRequirementsBySupplyPlanIdIn(Set.of(41L));
        Assertions.assertEquals(Set.of(41L), supplyPlanIdCaptor.getValue());
        Assertions.assertEquals(0, pageableCaptor.getValue().getPageNumber());
        Assertions.assertEquals(25, pageableCaptor.getValue().getPageSize());

    }

    @Test
    void preflightReportShouldPreserveTotalCountWhenItsInventoryEvidenceIsTruncated() {

        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        DistributionPlanItemRepository distributionPlanItemRepository =
                Mockito.mock(DistributionPlanItemRepository.class);
        ProductionPlanLinhaRepository productionPlanLinhaRepository =
                Mockito.mock(ProductionPlanLinhaRepository.class);
        stubEmptyRequirements(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

        InventoryPlanLegacyBaselineRequirement inventoryRequirement =
                Mockito.mock(InventoryPlanLegacyBaselineRequirement.class);
        Mockito.when(inventoryRequirement.getSupplyPlanId()).thenReturn(41L);
        Mockito.when(inventoryRequirement.getLocationId()).thenReturn("PLANT");
        Mockito.when(inventoryRequirement.getMaterialId()).thenReturn("MATERIAL");
        Mockito.when(inventoryRequirement.getReferenceDate())
                .thenReturn(LocalDateTime.of(2026, 7, 31, 23, 59));
        Mockito.when(inventoryRequirement.getSafetyStockBaselineUnconstrained()).thenReturn(4.0f);
        Mockito.when(inventoryPlanLinhaRepository.findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.eq(Set.of(41L)),
                        Mockito.any(Pageable.class)))
                .thenReturn(List.of(inventoryRequirement));
        Mockito.when(inventoryPlanLinhaRepository
                        .countLegacyBaselineRequirementsBySupplyPlanIdIn(Set.of(41L)))
                .thenReturn(2L);

        SupplyPlanPersistedBaselinePreflight preflight = getPreflight(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

        SupplyPlanPersistedBaselinePreflight.SupplyPlanPersistedBaselinePreflightReport report =
                preflight.inspectSupplyPlanIds(Set.of(41L));

        Assertions.assertEquals(Set.of(41L), report.supplyPlanIdSet());
        Assertions.assertTrue(report.hasBlockingIssues());
        Assertions.assertEquals(2, report.inventoryPlanReport().totalIssueCount());
        Assertions.assertEquals(1, report.inventoryPlanReport().limitedIssueList().size());
        Assertions.assertTrue(report.inventoryPlanReport().truncated());
        Assertions.assertEquals(0, report.distributionPlanReport().totalIssueCount());
        Assertions.assertFalse(report.distributionPlanReport().truncated());
        Assertions.assertEquals(0, report.productionPlanReport().totalIssueCount());
        Assertions.assertFalse(report.productionPlanReport().truncated());

    }

    @Test
    void effectiveLegacyInventoryBaselineShouldBlockWithActionableKeyAndField() {

        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        DistributionPlanItemRepository distributionPlanItemRepository =
                Mockito.mock(DistributionPlanItemRepository.class);
        ProductionPlanLinhaRepository productionPlanLinhaRepository =
                Mockito.mock(ProductionPlanLinhaRepository.class);
        stubEmptyRequirements(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

        InventoryPlanLegacyBaselineRequirement inventoryRequirement =
                Mockito.mock(InventoryPlanLegacyBaselineRequirement.class);
        Mockito.when(inventoryRequirement.getSupplyPlanId()).thenReturn(41L);
        Mockito.when(inventoryRequirement.getLocationId()).thenReturn("PLANT");
        Mockito.when(inventoryRequirement.getMaterialId()).thenReturn("MATERIAL");
        Mockito.when(inventoryRequirement.getReferenceDate())
                .thenReturn(LocalDateTime.of(2026, 7, 31, 23, 59));
        Mockito.when(inventoryRequirement.getProjectedStockBaselineUnconstrained())
                .thenReturn(12.0f);
        Mockito.when(inventoryPlanLinhaRepository.findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.eq(Set.of(41L)),
                        Mockito.any(Pageable.class)))
                .thenReturn(List.of(inventoryRequirement));
        Mockito.when(inventoryPlanLinhaRepository
                        .countLegacyBaselineRequirementsBySupplyPlanIdIn(Set.of(41L)))
                .thenReturn(1L);

        SupplyPlanPersistedBaselinePreflight preflight = getPreflight(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> preflight.assertSupplyPlanReadyForCanonicalRuntime(41L));

        Assertions.assertTrue(exception.getMessage().contains("Inventory Plan"));
        Assertions.assertTrue(exception.getMessage().contains("supplyPlan=41/location=PLANT/material=MATERIAL"));
        Assertions.assertTrue(exception.getMessage().contains("referenceDate=2026-07-31T23:59"));
        Assertions.assertTrue(exception.getMessage().contains("quantidadeEstoqueBaseline"));
        Assertions.assertTrue(exception.getMessage().contains("Unconstrained/Constrained/Working"));

    }

    @Test
    void zeroLegacyColumnsShouldNotBlockBecauseTheyCannotAffectCanonicalSeries() {

        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        DistributionPlanItemRepository distributionPlanItemRepository =
                Mockito.mock(DistributionPlanItemRepository.class);
        ProductionPlanLinhaRepository productionPlanLinhaRepository =
                Mockito.mock(ProductionPlanLinhaRepository.class);
        stubEmptyRequirements(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

        DistributionPlanLegacyBaselineRequirement distributionRequirement =
                Mockito.mock(DistributionPlanLegacyBaselineRequirement.class);
        Mockito.when(distributionRequirement.getPlannedOrderBaselineUnconstrained())
                .thenReturn(0.0f);
        Mockito.when(distributionPlanItemRepository.findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.eq(Set.of(41L)),
                        Mockito.any(Pageable.class)))
                .thenReturn(List.of(distributionRequirement));

        SupplyPlanPersistedBaselinePreflight preflight = getPreflight(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

        Assertions.assertDoesNotThrow(
                () -> preflight.assertSupplyPlanReadyForCanonicalRuntime(41L));

    }

    @Test
    void invalidSupplyPlanIdShouldFailBeforeRepositories() {

        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        DistributionPlanItemRepository distributionPlanItemRepository =
                Mockito.mock(DistributionPlanItemRepository.class);
        ProductionPlanLinhaRepository productionPlanLinhaRepository =
                Mockito.mock(ProductionPlanLinhaRepository.class);
        SupplyPlanPersistedBaselinePreflight preflight = getPreflight(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> preflight.assertSupplyPlanReadyForCanonicalRuntime(null));

        Assertions.assertEquals(
                "Supply Plan persisted baseline preflight requires a positive Supply Plan id.",
                exception.getMessage());
        Mockito.verifyNoInteractions(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

    }

    @Test
    void selectedPreviewShouldReadAndReportOnlyTheRequestedSupplySeries() {

        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        DistributionPlanItemRepository distributionPlanItemRepository =
                Mockito.mock(DistributionPlanItemRepository.class);
        ProductionPlanLinhaRepository productionPlanLinhaRepository =
                Mockito.mock(ProductionPlanLinhaRepository.class);
        stubEmptyRequirements(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);
        SupplyPlanPersistedBaselinePreflight preflight = getPreflight(
                inventoryPlanLinhaRepository,
                distributionPlanItemRepository,
                productionPlanLinhaRepository);

        SupplyPlanPersistedBaselinePreflight.SupplyPlanPersistedBaselinePreflightReport report = preflight
                .inspectSupplyPlanIdsForSeries(
                        Set.of(41L),
                        Set.of(SupplyPlanPersistedBaselineSeries.DISTRIBUTION));

        Assertions.assertEquals(0, report.inventoryPlanReport().totalIssueCount());
        Assertions.assertEquals(0, report.distributionPlanReport().totalIssueCount());
        Assertions.assertEquals(0, report.productionPlanReport().totalIssueCount());
        Mockito.verify(distributionPlanItemRepository)
                .findLegacyBaselineRequirementsBySupplyPlanIdIn(Set.of(41L), Pageable.ofSize(25));
        Mockito.verify(distributionPlanItemRepository)
                .countLegacyBaselineRequirementsBySupplyPlanIdIn(Set.of(41L));
        Mockito.verifyNoInteractions(inventoryPlanLinhaRepository, productionPlanLinhaRepository);

    }

    private static void stubEmptyRequirements(
            InventoryPlanLinhaRepository inventoryPlanLinhaRepository,
            DistributionPlanItemRepository distributionPlanItemRepository,
            ProductionPlanLinhaRepository productionPlanLinhaRepository) {

        Mockito.when(inventoryPlanLinhaRepository.findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.anySet(),
                        Mockito.any(Pageable.class)))
                .thenReturn(List.of());
        Mockito.when(distributionPlanItemRepository.findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.anySet(),
                        Mockito.any(Pageable.class)))
                .thenReturn(List.of());
        Mockito.when(productionPlanLinhaRepository.findLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.anySet(),
                        Mockito.any(Pageable.class)))
                .thenReturn(List.of());
        Mockito.when(inventoryPlanLinhaRepository.countLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.anySet()))
                .thenReturn(0L);
        Mockito.when(distributionPlanItemRepository.countLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.anySet()))
                .thenReturn(0L);
        Mockito.when(productionPlanLinhaRepository.countLegacyBaselineRequirementsBySupplyPlanIdIn(
                        Mockito.anySet()))
                .thenReturn(0L);

    }

    private static SupplyPlanPersistedBaselinePreflight getPreflight(
            InventoryPlanLinhaRepository inventoryPlanLinhaRepository,
            DistributionPlanItemRepository distributionPlanItemRepository,
            ProductionPlanLinhaRepository productionPlanLinhaRepository) {

        SupplyPlanPersistedBaselinePreflight preflight =
                new SupplyPlanPersistedBaselinePreflight();
        ReflectionTestUtils.setField(
                preflight,
                "inventoryPlanLinhaRepository",
                inventoryPlanLinhaRepository);
        ReflectionTestUtils.setField(
                preflight,
                "distributionPlanItemRepository",
                distributionPlanItemRepository);
        ReflectionTestUtils.setField(
                preflight,
                "productionPlanLinhaRepository",
                productionPlanLinhaRepository);
        return preflight;

    }

}
