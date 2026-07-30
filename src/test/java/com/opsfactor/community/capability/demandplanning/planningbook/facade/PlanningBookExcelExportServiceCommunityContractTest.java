package com.opsfactor.community.capability.demandplanning.planningbook.facade;

import com.opsfactor.community.capability.planningbook.facade.dto.ColumnDefDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.GroupDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure.KeyFigureDTOPadrao;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.DemandPlanningFacade;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Protege a borda Community do export XLSX do Demand Planning Book.
 *
 * <p>O export deve ser apenas uma representacao tabular da mesma visao que o
 * front recebeu pelo Planning Book. Por isso ele precisa passar pelo
 * {@link DemandPlanningFacade}: ali ficam os bloqueios de reference plan,
 * key figures Enterprise e agrupamentos nao suportados no Community.</p>
 */
class PlanningBookExcelExportServiceCommunityContractTest {

    @Test
    void writePlanningBookExcelToOutputStreamShouldReusePlanningBookValidationBeforeCreatingWorkbook() {

        PlanningBookExcelExportService planningBookExcelExportService = new PlanningBookExcelExportService();
        ReflectionTestUtils.setField(
                planningBookExcelExportService,
                "demandPlanningFrontService",
                new DemandPlanningFacade());

        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.planId = "1";
        configuredViewSelectionDTO.referencePlanId = "reference-plan";

        /*
         * Nenhum collaborator do DemandPlanningFrontService foi injetado de
         * proposito. Se o XLSX tentar montar dados por fora do service de tela,
         * este teste deixa de receber RequiresEnterpriseVersionException e passa
         * a falhar por NPE, workbook vazio ou outra excecao operacional.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> planningBookExcelExportService.writePlanningBookExcelToOutputStream(
                        configuredViewSelectionDTO,
                        "admin",
                        new ByteArrayOutputStream()));

    }

    @Test
    void parseWidthShouldIgnoreOnlyInvalidOptionalWidthText() {

        PlanningBookExcelExportService planningBookExcelExportService = new PlanningBookExcelExportService();

        /*
         * Width de coluna e metadado visual opcional recebido pelo DTO. Quando
         * vier invalido, o exportador deve usar largura padrao sem abortar a
         * geracao do XLSX.
         */
        Integer validWidth = ReflectionTestUtils.invokeMethod(
                planningBookExcelExportService,
                "parseWidth",
                "128.5");
        Integer invalidWidth = ReflectionTestUtils.invokeMethod(
                planningBookExcelExportService,
                "parseWidth",
                "wide");

        Assertions.assertEquals(128, validWidth);
        Assertions.assertNull(invalidWidth);

    }

    @Test
    void exportPlanningBookWorkbookShouldAcceptMinimalCommunityPlanningBookDTO() throws Exception {

        PlanningBookExcelExportService planningBookExcelExportService = new PlanningBookExcelExportService();

        /*
         * O DTO minimo contem somente colunas tecnicas, uma linha material /
         * location e um periodo. Isso protege o export contra validacoes
         * excessivas que bloqueariam uma Planning Book Community simples.
         */
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        ReflectionTestUtils.invokeMethod(
                planningBookExcelExportService,
                "writePlanningBookExcelToOutputStream",
                planningBookDTOValido(),
                byteArrayOutputStream);

        Assertions.assertTrue(byteArrayOutputStream.size() > 0);

    }

    @Test
    void exportPlanningBookWorkbookShouldRejectBrokenStructuralDTOBeforeCreatingWorkbook() {

        PlanningBookExcelExportService planningBookExcelExportService = new PlanningBookExcelExportService();

        assertInvalidExport(
                planningBookExcelExportService,
                PlanningBookDTO.builder().build(),
                "columnDefs");

        assertInvalidExport(
                planningBookExcelExportService,
                planningBookDTOValidoComColumnDefs(Collections.singletonList(null)),
                "columnDefs[0]");

        assertInvalidExport(
                planningBookExcelExportService,
                planningBookDTOValidoComColumnDefs(List.of(ColumnDefDTO.builder().field(" ").build())),
                "field is required");

        assertInvalidExport(
                planningBookExcelExportService,
                planningBookDTOValidoComPeriodList(List.of(" ")),
                "periodList[0]");

        assertInvalidExport(
                planningBookExcelExportService,
                planningBookDTOValidoComGroups(Collections.singletonList(null)),
                "groups[0]");

        assertInvalidExport(
                planningBookExcelExportService,
                planningBookDTOValidoComGroups(List.of(GroupDTO.builder().keyFigures(Collections.singletonList(null)).build())),
                "keyFigures[0]");

        assertInvalidExport(
                planningBookExcelExportService,
                planningBookDTOValidoComKeyFigure(
                        KeyFigureDTOPadrao.builder()
                                .keyFigure("Demand")
                                .values(Map.of("2026-01-01T00:00", Double.NaN))
                                .build()),
                "must be finite");

    }

    private static void assertInvalidExport(
            PlanningBookExcelExportService planningBookExcelExportService,
            PlanningBookDTO planningBookDTO,
            String expectedMessageFragment) {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        planningBookExcelExportService,
                        "exportPlanningBookWorkbook",
                        planningBookDTO));

        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains(expectedMessageFragment),
                illegalArgumentException.getMessage());

    }

    private static PlanningBookDTO planningBookDTOValido() {

        return planningBookDTOValidoComKeyFigure(
                KeyFigureDTOPadrao.builder()
                        .keyFigure("Demand")
                        .values(Map.of("2026-01-01T00:00", 10.0))
                        .build());

    }

    private static PlanningBookDTO planningBookDTOValidoComKeyFigure(KeyFigureDTOPadrao keyFigureDTOPadrao) {

        GroupDTO groupDTO = GroupDTO.builder()
                .locationDescriptionCols(Map.of("locationId", "LOC-1"))
                .materialDescriptionCols(Map.of("materialId", "MAT-1"))
                .keyFigures(List.of(keyFigureDTOPadrao))
                .build();

        return planningBookDTOValidoComGroups(List.of(groupDTO));

    }

    private static PlanningBookDTO planningBookDTOValidoComGroups(List<GroupDTO> groupDTOList) {

        return PlanningBookDTO.builder()
                .viewName("PlanningBook Community")
                .columnDefs(List.of(
                        ColumnDefDTO.builder().field("materialId").dimension("material").name("Material").build(),
                        ColumnDefDTO.builder().field("locationId").dimension("location").name("Location").build(),
                        ColumnDefDTO.builder().field("keyFigure").name("Key Figure").build(),
                        ColumnDefDTO.builder().field("2026-01-01").name("2026-01-01").dataColumn(true).build()))
                .periodList(List.of("2026-01-01T00:00"))
                .bucketSize("DIARIO")
                .uom("UN")
                .groups(groupDTOList)
                .build();

    }

    private static PlanningBookDTO planningBookDTOValidoComColumnDefs(List<ColumnDefDTO> columnDefDTOList) {

        PlanningBookDTO planningBookDTO = planningBookDTOValido();
        planningBookDTO.columnDefs = columnDefDTOList;
        return planningBookDTO;

    }

    private static PlanningBookDTO planningBookDTOValidoComPeriodList(List<String> periodList) {

        PlanningBookDTO planningBookDTO = planningBookDTOValido();
        planningBookDTO.periodList = periodList;
        return planningBookDTO;

    }

}
