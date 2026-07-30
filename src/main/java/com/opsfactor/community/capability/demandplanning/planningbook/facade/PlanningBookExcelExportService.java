package com.opsfactor.community.capability.demandplanning.planningbook.facade;

import com.opsfactor.community.capability.planningbook.facade.dto.ColumnDefDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.GroupDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.KeyFigureDTOAbstract;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.DemandPlanningFacade;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exportador somente leitura do Demand Planning Book para XLSX.
 *
 * <p>O OpsFactor Community permite ajustes pela tela do Planning Book, mas nao
 * possui upload/importacao de ajustes por arquivo. Este service existe apenas
 * para gerar uma copia tabular da visao atual para download.</p>
 */
@Slf4j
@Service
public class PlanningBookExcelExportService {

    /**
     * Fachada do Planning Book de demanda usada para montar a mesma visao que
     * o usuario abriu na tela antes de exportar o XLSX somente leitura.
     */
    @Autowired
    private DemandPlanningFacade demandPlanningFrontService;

    public void writePlanningBookExcelToOutputStream(
            ConfiguredViewSelectionDTO configuredViewSelectionDTO,
            String userId,
            OutputStream outputStream) throws IOException {
        if (configuredViewSelectionDTO == null) {
            throw new IllegalArgumentException("configuredViewSelectionDTO is required");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream is required");
        }

        PlanningBookDTO planningBookDTO = demandPlanningFrontService.getPlanningBookDTO(configuredViewSelectionDTO, userId);
        writePlanningBookExcelToOutputStream(planningBookDTO, outputStream);
    }

    private void writePlanningBookExcelToOutputStream(
            PlanningBookDTO dto,
            OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream is required");
        }

        Workbook workbook = exportPlanningBookWorkbook(dto);
        try {
            workbook.write(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            log.error("Erro ao escrever Planning Book Community em XLSX", e);
            throw e;
        } finally {
            try {
                workbook.close();
            } catch (IOException e2) {
                log.error("Erro ao fechar workbook XLSX do Planning Book Community", e2);
            } finally {
                if (workbook instanceof SXSSFWorkbook workbookSxssf) {
                    workbookSxssf.dispose(); // limpa os temp files
                }
            }
        }
    }

    /**
     * Gera um XLSX em streaming (SXSSF). Quem chamar deve:
     *  - workbook.write(outputStream)
     *  - workbook.close()
     *  - se for SXSSFWorkbook: ((SXSSFWorkbook)workbook).dispose()
     */
    private Workbook exportPlanningBookWorkbook(PlanningBookDTO dto) {
        validaPlanningBookDTOParaExport(dto);

        // janela de 200 linhas em memória (ajuste conforme seu caso)
        SXSSFWorkbook wb = new SXSSFWorkbook(200);
        wb.setCompressTempFiles(true);

        String safeSheetName = WorkbookUtil.createSafeSheetName(
                dto.getViewName() != null ? dto.getViewName() : "PlanningBook"
        );
        Sheet sheet = wb.createSheet(safeSheetName);

        // estilos (cacheados)
        Styles styles = new Styles(wb);

        // 1) preparar colunas/export periods
        List<ColumnDefDTO> cols = dto.getColumnDefs() != null ? dto.getColumnDefs() : List.of();

        // Mapa: field da coluna (já “resumido”) -> chave original em periodList (que pode ter hora)
        // Ex.: "2026-01-01" -> "2026-01-01T00:00:00"
        Map<String, String> summarizedToOriginalPeriod = buildSummarizedToOriginalPeriod(dto);

        // 2) header
        Row header = sheet.createRow(0);
        header.setHeightInPoints(18);

        int freezeCol = 0;
        for (int c = 0; c < cols.size(); c++) {
            ColumnDefDTO col = cols.get(c);
            String headerName = (col.getName() != null && !col.getName().isBlank())
                    ? col.getName()
                    : col.getField();

            Cell cell = header.createCell(c, CellType.STRING);
            cell.setCellValue(headerName);
            cell.setCellStyle(styles.header);

            // largura (se vier)
            Integer width = parseWidth(col.getWidth());
            if (width != null) {
                // POI usa 1/256 de caractere
                sheet.setColumnWidth(c, Math.min(255 * 256, Math.max(8 * 256, width * 256)));
            }

            // freeze até antes das colunas dataColumn=true
            if (Boolean.TRUE.equals(col.getDataColumn()) && freezeCol == 0) {
                freezeCol = c;
            }
        }
        if (freezeCol > 0) {
            sheet.createFreezePane(freezeCol, 1);
        }

        // 3) dados (streaming row-a-row, sem “montar uiGridData” em memória)
        AtomicInteger rowNum = new AtomicInteger(1);

        if (dto.getGroups() != null) {
            for (GroupDTO g : dto.getGroups()) {
                writeGroupRecursive(
                        sheet,
                        dto,
                        cols,
                        summarizedToOriginalPeriod,
                        g,
                        new LinkedHashMap<>(),
                        new LinkedHashMap<>(),
                        -1,
                        rowNum,
                        styles
                );
            }
        }

        // outline (colapsável) fica melhor com isso
        sheet.setRowSumsBelow(false);

        return wb;
    }

    /**
     * Valida o contrato estrutural minimo do DTO antes de criar objetos POI.
     *
     * <p>O export Community e somente leitura e deve representar exatamente a
     * mesma Planning Book entregue ao front. Campos opcionais de exibicao podem
     * ficar vazios, mas a estrutura de linhas/colunas/key figures precisa estar
     * coerente para que erros de contrato aparecam aqui, e nao como NPEs dentro
     * da biblioteca de planilha.</p>
     */
    private void validaPlanningBookDTOParaExport(PlanningBookDTO planningBookDTO) {
        if (planningBookDTO == null) {
            throw new IllegalArgumentException("planningBookDTO is required");
        }

        validaColumnDefsParaExport(planningBookDTO.getColumnDefs());
        validaPeriodListParaExport(planningBookDTO.getPeriodList());
        validaGroupsParaExport(planningBookDTO.getGroups(), "groups");
    }

    /**
     * Garante que cada coluna tenha o identificador tecnico usado para buscar
     * dimensoes, periodos ou metadados de key figure durante a escrita.
     */
    private void validaColumnDefsParaExport(List<ColumnDefDTO> columnDefDTOList) {
        if (columnDefDTOList == null) {
            throw new IllegalArgumentException("Planning Book export requires columnDefs");
        }

        for (int columnIndex = 0; columnIndex < columnDefDTOList.size(); columnIndex++) {
            ColumnDefDTO columnDefDTO = columnDefDTOList.get(columnIndex);
            if (columnDefDTO == null) {
                throw new IllegalArgumentException("Planning Book export columnDefs[" + columnIndex + "] is null");
            }
            if (columnDefDTO.getField() == null || columnDefDTO.getField().isBlank()) {
                throw new IllegalArgumentException("Planning Book export columnDefs[" + columnIndex + "].field is required");
            }
        }
    }

    /**
     * Valida as chaves de periodo usadas como ponte entre o header resumido e o
     * mapa de valores das key figures.
     */
    private void validaPeriodListParaExport(List<String> periodList) {
        if (periodList == null) {
            throw new IllegalArgumentException("Planning Book export requires periodList");
        }

        for (int periodIndex = 0; periodIndex < periodList.size(); periodIndex++) {
            String period = periodList.get(periodIndex);
            if (period == null || period.isBlank()) {
                throw new IllegalArgumentException("Planning Book export periodList[" + periodIndex + "] is required");
            }
        }
    }

    /**
     * Percorre a arvore de grupos recebida do Planning Book preservando o nome
     * do caminho para mensagens de erro acionaveis durante a migracao.
     */
    private void validaGroupsParaExport(List<GroupDTO> groupDTOList, String caminho) {
        if (groupDTOList == null) {
            throw new IllegalArgumentException("Planning Book export requires " + caminho);
        }

        for (int groupIndex = 0; groupIndex < groupDTOList.size(); groupIndex++) {
            GroupDTO groupDTO = groupDTOList.get(groupIndex);
            String caminhoGroupDTO = caminho + "[" + groupIndex + "]";

            if (groupDTO == null) {
                throw new IllegalArgumentException("Planning Book export " + caminhoGroupDTO + " is null");
            }

            validaKeyFiguresParaExport(groupDTO.getKeyFigures(), caminhoGroupDTO + ".keyFigures");

            if (groupDTO.getSubGroups() != null) {
                validaGroupsParaExport(groupDTO.getSubGroups(), caminhoGroupDTO + ".subGroups");
            }
        }
    }

    /**
     * Valida os pontos que o XLSX efetivamente consome de cada key figure:
     * identificador da linha e mapa numerico de valores por periodo.
     */
    private void validaKeyFiguresParaExport(List<KeyFigureDTOAbstract> keyFigureDTOAbstractList, String caminho) {
        if (keyFigureDTOAbstractList == null) {
            throw new IllegalArgumentException("Planning Book export requires " + caminho);
        }

        for (int keyFigureIndex = 0; keyFigureIndex < keyFigureDTOAbstractList.size(); keyFigureIndex++) {
            KeyFigureDTOAbstract keyFigureDTOAbstract = keyFigureDTOAbstractList.get(keyFigureIndex);
            String caminhoKeyFigureDTOAbstract = caminho + "[" + keyFigureIndex + "]";

            if (keyFigureDTOAbstract == null) {
                throw new IllegalArgumentException("Planning Book export " + caminhoKeyFigureDTOAbstract + " is null");
            }
            if (keyFigureDTOAbstract.getKeyFigure() == null || keyFigureDTOAbstract.getKeyFigure().isBlank()) {
                throw new IllegalArgumentException("Planning Book export " + caminhoKeyFigureDTOAbstract + ".keyFigure is required");
            }

            validaValoresKeyFigureParaExport(
                    keyFigureDTOAbstract.getValues(),
                    caminhoKeyFigureDTOAbstract + ".values");
        }
    }

    /**
     * Valores nulos ou infinitos/NaN nao sao mascarados no Community porque
     * representam dado inconsistente e poderiam produzir uma planilha enganosa.
     */
    private void validaValoresKeyFigureParaExport(Map<String, Double> values, String caminho) {
        if (values == null) {
            return;
        }

        for (Map.Entry<String, Double> entryPeriodAndValue : values.entrySet()) {
            if (entryPeriodAndValue.getKey() == null || entryPeriodAndValue.getKey().isBlank()) {
                throw new IllegalArgumentException("Planning Book export " + caminho + " contains blank period");
            }
            if (entryPeriodAndValue.getValue() == null || !Double.isFinite(entryPeriodAndValue.getValue())) {
                throw new IllegalArgumentException("Planning Book export " + caminho + "[" + entryPeriodAndValue.getKey() + "] must be finite");
            }
        }
    }

    private void writeGroupRecursive(
            Sheet sheet,
            PlanningBookDTO dto,
            List<ColumnDefDTO> cols,
            Map<String, String> summarizedToOriginalPeriod,
            GroupDTO group,
            Map<String, String> inheritedLoc,
            Map<String, String> inheritedMat,
            int parentLevel,
            AtomicInteger rowNum,
            Styles styles) {

        int groupRowStart = rowNum.get(); // marca início do grupo

        // ===== herança (igual JS) =====
        Map<String, String> loc = new LinkedHashMap<>(inheritedLoc);
        if (group.getLocationDescriptionCols() != null) loc.putAll(group.getLocationDescriptionCols());

        Map<String, String> mat = new LinkedHashMap<>(inheritedMat);
        if (group.getMaterialDescriptionCols() != null) mat.putAll(group.getMaterialDescriptionCols());

        // ===== linhas das key figures =====
        List<KeyFigureDTOAbstract> kfs =
                group.getKeyFigures() != null ? group.getKeyFigures() : List.of();

        for (KeyFigureDTOAbstract kf : kfs) {
            Row row = sheet.createRow(rowNum.getAndIncrement());
            writeRow(dto, cols, summarizedToOriginalPeriod, row, loc, mat, kf, styles);
        }

        // ===== filhos =====
        if (group.getSubGroups() != null) {
            for (GroupDTO child : group.getSubGroups()) {
                writeGroupRecursive(
                        sheet,
                        dto,
                        cols,
                        summarizedToOriginalPeriod,
                        child,
                        loc,
                        mat,
                        parentLevel + 1,
                        rowNum,
                        styles
                );
            }
        }

        int groupRowEnd = rowNum.get() - 1;

        // ===== aplica outline =====
        if (groupRowEnd > groupRowStart) {
            sheet.groupRow(groupRowStart + 1, groupRowEnd);
        }
    }

    private void writeRow(
            PlanningBookDTO dto,
            List<ColumnDefDTO> cols,
            Map<String, String> summarizedToOriginalPeriod,
            Row row,
            Map<String, String> loc,
            Map<String, String> mat,
            KeyFigureDTOAbstract kf,
            Styles styles
    ) {
        Map<String, Double> values = kf.getValues() != null ? kf.getValues() : Collections.emptyMap();

        for (int c = 0; c < cols.size(); c++) {
            ColumnDefDTO col = cols.get(c);
            String field = col.getField();

            Cell cell = row.createCell(c);

            // 1) colunas “dimensionais”
            if ("keyFigure".equals(field)) {
                cell.setCellType(CellType.STRING);
                cell.setCellValue(kf.getKeyFigure() != null ? kf.getKeyFigure() : "");
                cell.setCellStyle(styles.text);
                continue;
            }
            if ("uom".equals(field)) {
                cell.setCellType(CellType.STRING);
                cell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
                cell.setCellStyle(styles.text);
                continue;
            }

            // Colunas de dimensao material/location usam os mapas especificos
            // do GroupDTO em vez de procurar valores em key figures.
            if ("material".equalsIgnoreCase(col.getDimension())) {
                String v = mat.get(field);
                cell.setCellType(CellType.STRING);
                cell.setCellValue(v != null ? v : "");
                cell.setCellStyle(styles.text);
                continue;
            }
            if ("location".equalsIgnoreCase(col.getDimension())) {
                String v = loc.get(field);
                cell.setCellType(CellType.STRING);
                cell.setCellValue(v != null ? v : "");
                cell.setCellStyle(styles.text);
                continue;
            }

            // 2) colunas de periodo (dataColumn=true no ColumnDefDTO)
            if (Boolean.TRUE.equals(col.getDataColumn())) {
                String originalPeriodKey = summarizedToOriginalPeriod.getOrDefault(field, field);
                Double v = values.get(originalPeriodKey);
                double num = (v != null ? v : 0d);

                cell.setCellType(CellType.NUMERIC);
                cell.setCellValue(num);

                // Marcador semantico de periodo historico recebido do contrato
                // do Planning Book.
                if (col.getCellClass() != null && col.getCellClass().contains("pastPeriods")) {
                    cell.setCellStyle(styles.numberPast);
                } else {
                    cell.setCellStyle(styles.number);
                }

                // O export aplica apenas o estado operacional mais critico:
                // celulas travadas pelo horizonte congelado.
                if (hasCrosshatch(kf, originalPeriodKey)) {
                    cell.setCellStyle(styles.numberCrosshatch);
                }

                continue;
            }

            // 3) fallback: tenta achar em loc/mat pelo field; senão vazio
            String v = mat.getOrDefault(field, loc.get(field));
            if (v != null) {
                cell.setCellType(CellType.STRING);
                cell.setCellValue(v);
                cell.setCellStyle(styles.text);
            } else {
                cell.setBlank();
            }
        }
    }

    /**
     * Cria o de-para entre a chave resumida exibida em colunas de buckets
     * diario/semanal/mensal e a chave original usada nos mapas de valores.
     */
    private Map<String, String> buildSummarizedToOriginalPeriod(PlanningBookDTO dto) {
        Map<String, String> map = new LinkedHashMap<>();
        if (dto.getPeriodList() == null) return map;

        String bucket = dto.getBucketSize();

        for (String p : dto.getPeriodList()) {
            if (p == null) continue;

            String summarized = p;
            if (!"Average Historical Sales".equals(p)
                    && ( "DIARIO".equalsIgnoreCase(bucket)
                    || "SEMANAL".equalsIgnoreCase(bucket)
                    || "MENSAL".equalsIgnoreCase(bucket))) {
                summarized = p.length() >= 10 ? p.substring(0, 10) : p;
            }
            // se houver colisão (não deveria), mantém o primeiro
            map.putIfAbsent(summarized, p);
        }
        return map;
    }

    private boolean hasCrosshatch(KeyFigureDTOAbstract kf, String originalPeriodKey) {
        if (kf.getAdditionalClasses() == null || kf.getAdditionalClasses().isEmpty()) return false;

        LocalDateTime dt = tryParseLocalDateTime(originalPeriodKey);
        if (dt == null) return false;

        Set<String> classes = (Set<String>) kf.getAdditionalClasses().get(dt);
        if (classes == null) return false;
        return classes.contains("crosshatch");
    }

    private LocalDateTime tryParseLocalDateTime(String s) {
        if (s == null) return null;
        try {
            // Aceita tanto LocalDateTime ISO quanto LocalDate ISO.
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(s).atStartOfDay();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private Integer parseWidth(String width) {
        if (width == null) return null;
        try {
            return (int) Double.parseDouble(width.trim());
        } catch (NumberFormatException numberFormatException) {
            /*
             * Largura vinda do DTO e opcional. Valor textual invalido nao deve
             * impedir a exportacao; apenas voltamos ao width padrao da coluna.
             */
            return null;
        }
    }

    private static class Styles {
        final CellStyle header;
        final CellStyle text;
        final CellStyle number;
        final CellStyle numberPast;
        final CellStyle numberCrosshatch;

        Styles(Workbook wb) {
            DataFormat fmt = wb.createDataFormat();

            Font headerFont = wb.createFont();
            headerFont.setBold(true);

            header = wb.createCellStyle();
            header.setFont(headerFont);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setBorderBottom(BorderStyle.THIN);

            text = wb.createCellStyle();
            text.setVerticalAlignment(VerticalAlignment.CENTER);

            number = wb.createCellStyle();
            number.setVerticalAlignment(VerticalAlignment.CENTER);
            number.setDataFormat(fmt.getFormat("#,##0.00"));

            numberPast = wb.createCellStyle();
            numberPast.cloneStyleFrom(number);
            numberPast.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            numberPast.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());

            numberCrosshatch = wb.createCellStyle();
            numberCrosshatch.cloneStyleFrom(number);
            numberCrosshatch.setFillPattern(FillPatternType.FINE_DOTS);
            numberCrosshatch.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        }
    }

}
