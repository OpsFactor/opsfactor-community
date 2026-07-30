package com.opsfactor.community.capability.planningbook.facade.dto;

import com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure.KeyFigureDTOPadrao;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrato de shape dos DTOs de Planning Book expostos no Community.
 *
 * <p>Regras funcionais como bloqueio de reference plan, key figures Enterprise
 * e agrupamentos configuraveis ficam nos services. Este teste congela a
 * superficie publica dos DTOs para que novos campos de workflow, agrupamento,
 * caracteristicas, upload ou selecao livre de KFs exijam decisao explicita de
 * recorte antes de entrar no Community.</p>
 */
public class PlanningBookDtoCommunityContractTest {

    @Test
    public void planningBookDtoShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "viewName",
                        "viewType",
                        "autoSubmitChanges",
                        "keyFigures",
                        "columnDefs",
                        "groups",
                        "additionalParameters",
                        "periodList",
                        "bucketSize",
                        "uom",
                        "errorMessage"),
                getDeclaredFieldNames(PlanningBookDTO.class));

    }

    @Test
    public void selectedPlanningBookCellDtoShouldExposeOnlyCommunityFieldsAndReferencePlanGuardField() {

        /*
         * `referencePlanId` permanece no DTO por compatibilidade com o front
         * compartilhado, mas e campo Enterprise no Community e deve continuar
         * bloqueado pelos services antes de qualquer alteracao de plano.
         */
        Assertions.assertEquals(
                Set.of(
                        "planId",
                        "referencePlanId",
                        "viewType",
                        "viewName",
                        "locationId",
                        "locationDescriptionCols",
                        "materialDescriptionCols",
                        "keyFigure",
                        "period",
                        "uom",
                        "oldValue",
                        "newValue",
                        "parentSelection"),
                getDeclaredFieldNames(SelectedPlanningBookCellDTO.class));

    }

    @Test
    public void selectedPlanningBookCellDtoAccessorsShouldHandleMissingDescriptionMapsWithoutTechnicalError() {

        SelectedPlanningBookCellDTO selectedPlanningBookCellDTO = SelectedPlanningBookCellDTO.builder()
                .planId(1L)
                .viewName("Community View")
                .keyFigure("AJUSTE_DEMANDA")
                .build();

        /*
         * Mapas ausentes representam payload incompleto. O DTO deve apenas
         * retornar ausencia de material/location; a decisao funcional de
         * bloquear ajuste agregado pertence aos services de Planning Book.
         */
        Assertions.assertFalse(selectedPlanningBookCellDTO.isMaterialLevel());
        Assertions.assertFalse(selectedPlanningBookCellDTO.isLocationLevel());
        Assertions.assertNull(selectedPlanningBookCellDTO.getMaterialId());
        Assertions.assertNull(selectedPlanningBookCellDTO.getLocationId());
        Assertions.assertDoesNotThrow(selectedPlanningBookCellDTO::toString);

        selectedPlanningBookCellDTO.materialDescriptionCols = Map.of("materialId", "MAT-1");
        selectedPlanningBookCellDTO.locationDescriptionCols = Map.of("locationId", "LOC-1");

        Assertions.assertEquals("MAT-1", selectedPlanningBookCellDTO.getMaterialId());
        Assertions.assertEquals("LOC-1", selectedPlanningBookCellDTO.getLocationId());

    }

    @Test
    public void cellDetailsDtoShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "planId",
                        "viewName",
                        "materialId",
                        "locationId",
                        "keyFigure",
                        "tipoPlano",
                        "period",
                        "detailLines",
                        "columnDefs"),
                getDeclaredFieldNames(CellDetailsDTO.class));

    }

    @Test
    public void columnDefDtoShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "field",
                        "name",
                        "dataColumn",
                        "cellClass",
                        "dimension",
                        "width",
                        "enableCellEdit",
                        "enableFiltering",
                        "enableSorting",
                        "enableHiding",
                        "enablePinning",
                        "pinnedLeft"),
                getDeclaredFieldNames(ColumnDefDTO.class));

    }

    @Test
    public void groupDtoShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "locationDescriptionCols",
                        "materialDescriptionCols",
                        "keyFigures",
                        "subGroups",
                        "parentSelection"),
                getDeclaredFieldNames(GroupDTO.class));

    }

    @Test
    public void keyFigureDtoAbstractShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "keyFigure",
                        "values",
                        "unavailableReasons",
                        "toolTips",
                        "additionalClasses",
                        "editMode"),
                getDeclaredFieldNames(KeyFigureDTOAbstract.class));

    }

    @Test
    public void specializedKeyFigureDtosShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(Set.of(), getDeclaredFieldNames(KeyFigureDTOPadrao.class));

    }

    @Test
    public void keyFigureDtoAggregationShouldRejectIncompatibleKeyFigureTypeAsInvalidArgument() {

        KeyFigureDTOPadrao demandPlanKeyFigureDTO = new KeyFigureDTOPadrao(
                "Demand Plan",
                EditMode.CELLEDIT);
        KeyFigureDTOPadrao stockKeyFigureDTO = new KeyFigureDTOPadrao(
                "Stock",
                EditMode.CELLEDIT);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanKeyFigureDTO.incorporaValoresTooltipsEClassesAdicionaisDeKeyFigure(stockKeyFigureDTO));

        Assertions.assertEquals(
                "Only key figures of the same type can be aggregated",
                illegalArgumentException.getMessage());

    }

    @Test
    public void keyFigureDtoAggregationShouldRejectIncompatibleEditModeAsInvalidArgument() {

        KeyFigureDTOPadrao editableKeyFigureDTO = new KeyFigureDTOPadrao(
                "Demand Plan",
                EditMode.CELLEDIT);
        KeyFigureDTOPadrao readOnlyKeyFigureDTO = new KeyFigureDTOPadrao(
                "Demand Plan",
                EditMode.NOEDIT);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> editableKeyFigureDTO.incorporaValoresTooltipsEClassesAdicionaisDeKeyFigure(readOnlyKeyFigureDTO));

        Assertions.assertEquals(
                "Only key figures with the same Edit Mode can be aggregated",
                illegalArgumentException.getMessage());

    }

    @Test
    public void keyFigureDtoAggregationShouldPropagateFirstUnavailableReasonAndRemovePartialValue() {

        String period = "2026-01-31T23:59:59";
        KeyFigureDTOPadrao aggregatedKeyFigureDTO = new KeyFigureDTOPadrao(
                "Gross Sales",
                EditMode.NOEDIT);
        KeyFigureDTOPadrao pricedLeafKeyFigureDTO = new KeyFigureDTOPadrao(
                "Gross Sales",
                EditMode.NOEDIT);
        pricedLeafKeyFigureDTO.values = Map.of(period, 125.0d);
        KeyFigureDTOPadrao unavailableLeafKeyFigureDTO = new KeyFigureDTOPadrao(
                "Gross Sales",
                EditMode.NOEDIT);
        unavailableLeafKeyFigureDTO.unavailableReasons = Map.of(
                period,
                "MISSING_APPLICABLE_PRICE");
        KeyFigureDTOPadrao secondUnavailableLeafKeyFigureDTO = new KeyFigureDTOPadrao(
                "Gross Sales",
                EditMode.NOEDIT);
        secondUnavailableLeafKeyFigureDTO.unavailableReasons = Map.of(
                period,
                "MISSING_UNIT_CONVERSION");

        aggregatedKeyFigureDTO.incorporaValoresTooltipsEClassesAdicionaisDeKeyFigure(
                pricedLeafKeyFigureDTO);
        aggregatedKeyFigureDTO.incorporaValoresTooltipsEClassesAdicionaisDeKeyFigure(
                unavailableLeafKeyFigureDTO);
        aggregatedKeyFigureDTO.incorporaValoresTooltipsEClassesAdicionaisDeKeyFigure(
                secondUnavailableLeafKeyFigureDTO);

        Assertions.assertEquals(
                "MISSING_APPLICABLE_PRICE",
                aggregatedKeyFigureDTO.unavailableReasons.get(period));
        Assertions.assertFalse(aggregatedKeyFigureDTO.values.containsKey(period));

    }

    @Test
    public void keyFigureDtoWithoutUnavailableReasonShouldKeepSidecarNullForCommunityPayloads() {

        KeyFigureDTOPadrao keyFigureDTO = new KeyFigureDTOPadrao(
                "Baseline",
                EditMode.NOEDIT);

        Assertions.assertNull(keyFigureDTO.unavailableReasons);

    }

    private static Set<String> getDeclaredFieldNames(Class<?> dtoClass) {

        return Arrays.stream(dtoClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

    }

}
