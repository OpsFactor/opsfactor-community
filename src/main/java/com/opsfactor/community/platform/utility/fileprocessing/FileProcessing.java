package com.opsfactor.community.platform.utility.fileprocessing;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;


/**
 * Utilitarios de leitura e escrita de arquivos usados pelas cargas Community.
 *
 * <p>A fronteira de quais arquivos podem ser enviados fica nos controllers e
 * mappers de integracao. Esta classe apenas transforma CSV/XLSX em
 * {@link ProcessedFile} e adiciona valores a workbooks quando fluxos
 * funcionais precisam devolver templates ou resultados tabulares.</p>
 */
public abstract class FileProcessing {

    static {
        IOUtils.setByteArrayMaxOverride(500_000_000);
    }

    public static ProcessedFile getProcessedFileFromMultipartFile(MultipartFile multipartFile) throws IOException {

        if (multipartFile == null) {
            throw new IllegalArgumentException("Multipart file payload is required.");
        }

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Multipart file original filename is required.");
        }

        if (originalFilename.endsWith(".csv")) {
            return getProcessedFileFromCSVFile(multipartFile);
        } else if (originalFilename.endsWith(".xls")
                || originalFilename.endsWith(".xlsx")
                || originalFilename.endsWith(".xlsm")) {
            return getProcessedFileFromXLSXFile(multipartFile);
        } else {
            return getProcessedFileFromCSVFile(multipartFile);
        }

    }

    private static ProcessedFile getProcessedFileFromCSVFile(MultipartFile multipartFile) throws IOException {

        ProcessedFile processedFile = new ProcessedFile();
        try (InputStream inputStream = multipartFile.getInputStream();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                processedFile.addNewRow();

                line = line.replaceAll("\"", "");
                List<String> linhaAtual = Arrays.asList(line.split(";"));

                for (String valorColuna : linhaAtual) {
                    processedFile.addToLastRow(valorColuna);
                }
            }
        }
        processedFile.setName(multipartFile.getOriginalFilename());
        return processedFile;

    }

    private static ProcessedFile getProcessedFileFromXLSXFile(MultipartFile multipartFile) throws IOException {

        ProcessedFile processedFile = new ProcessedFile();
        try (InputStream inputStream = multipartFile.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {

            // pega a primeira planilha, que representa o arquivo funcional enviado.
            XSSFSheet sheet = workbook.getSheetAt(0);
            if (sheet.getRow(0) == null) {
                throw new IOException("First row on first sheet (" + sheet.getSheetName() + ") is empty");
            }

            for (Row row : sheet) {
                processedFile.addNewRow();
                for (int col = 0; col < row.getLastCellNum(); col++) {
                    Cell cell = row.getCell(col);
                    Object valorCelula = "";
                    if (cell == null) {
                        valorCelula = "";
                    } else {
                        CellType cellType = cell.getCellType();
                        // campos numericos: numeros ou datas
                        if (cellType == CellType.NUMERIC) {
                            // o campo numerico representa uma data
                            if (DateUtil.isCellDateFormatted(cell)) {
                                valorCelula = cell.getDateCellValue().toInstant()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime();
                            } else {
                                // campo numerico representa um numero
                                valorCelula = cell.getNumericCellValue();
                                // se numero nao tiver casas decimais, traduzir como long para evitar .0 na string
                                if (((double) valorCelula % 1) == 0) {
                                    valorCelula = ((Double) valorCelula).longValue();
                                }
                            }
                        } else {
                            // campo nao-numerico
                            valorCelula = cell.toString();
                        }
                    }
                    processedFile.addToLastRow(valorCelula);
                }
            }
        }

        return processedFile;

    }
    
    /**
     * Adiciona valor a uma aba de um workbook em uma posição linha/coluna (iniciados sempre por 0)
     * @param wb
     * @param sheetName
     * @param numeroLinha 0 equivale à primeira linha
     * @param numeroColuna 0 equivale à primeira coluna
     * @param valorDaCelula double, string, boolean, data
     */
    public static void addValueToSheet(XSSFWorkbook wb, String sheetName, int numeroLinha, int numeroColuna, Object valorDaCelula) {
        
        String sheetName31Caracteres = sheetName.length() > 31 ? sheetName.substring(0, 30) : sheetName;
        
        // nome da aba no Excel não pode exceder 31 caracteres
        XSSFSheet sheet = wb.getSheet(sheetName31Caracteres);
        if (sheet == null) {
            sheet = wb.createSheet(sheetName31Caracteres);
        }
        
        XSSFRow row = sheet.getRow(numeroLinha);
        if (row == null) {
            row = sheet.createRow(numeroLinha);
        }
        
        XSSFCell cell = row.createCell(numeroColuna);
        if (valorDaCelula instanceof Integer && valorDaCelula != null) {
            cell.setCellValue(((Integer) valorDaCelula).intValue());
        } else if (valorDaCelula instanceof Float && valorDaCelula != null) {
            cell.setCellValue(((Float) valorDaCelula).floatValue());
        } else if (valorDaCelula instanceof Double && valorDaCelula != null) {
            cell.setCellValue(((Double) valorDaCelula).doubleValue());
        } else if (valorDaCelula instanceof LocalDate && valorDaCelula != null) {
            cell.setCellValue(Date.from(((LocalDate) valorDaCelula).atStartOfDay().toInstant(ZoneOffset.UTC)));
        } else if (valorDaCelula instanceof LocalDateTime && valorDaCelula != null) {
            cell.setCellValue(Date.from(((LocalDateTime) valorDaCelula).toInstant(ZoneOffset.UTC)));
        } else if (valorDaCelula instanceof Boolean && valorDaCelula != null) {
            cell.setCellValue(((Boolean) valorDaCelula).booleanValue());
        } else if (valorDaCelula != null) {
            cell.setCellValue(valorDaCelula.toString().length() <= 32767 ? valorDaCelula.toString() : valorDaCelula.toString().substring(0, 32766)); // substring para respeitar o limite do campo Text excel
        } else {
            cell.setBlank();
        }
        
    }
    
    /**
     * Adiciona valor a uma aba de um workbook na última linha/coluna disponíveis
     * @param wb
     * @param sheetName
     * @param valorDaCelula double, string, boolean, data
     */
    public static void addValueToSheet(XSSFWorkbook wb, String sheetName, int numeroColunasSheet, Object valorDaCelula) {
        
        // nome da aba no Excel não pode exceder 31 caracteres
        String sheetName31Caracteres = sheetName.length() > 31 ? sheetName.substring(0, 30) : sheetName;
        
        XSSFSheet sheet = wb.getSheet(sheetName31Caracteres);
        if (sheet == null) {
            sheet = wb.createSheet(sheetName31Caracteres);
        }
        
        int numeroLinha = sheet.getLastRowNum();
        XSSFRow row = sheet.getRow(numeroLinha);
        if (row == null) {
            row = sheet.createRow(numeroLinha);
        }
        
        int colunaAtual = Math.max(0, row.getLastCellNum());
        
        if (colunaAtual < numeroColunasSheet) {
            addValueToSheet(wb, sheetName31Caracteres, numeroLinha, colunaAtual, valorDaCelula);
        } else {
            addValueToSheet(wb, sheetName31Caracteres, numeroLinha+1, 0, valorDaCelula);
        }
        
    }
    
    public static void saveExcelWorkbookToFile(XSSFWorkbook wb, String diretorioSemUltimaBarra, String nomeArquivoSemExtensao) throws FileNotFoundException, IOException {
        
        String excelFileName = diretorioSemUltimaBarra + "/" + nomeArquivoSemExtensao + ".xlsx";//"C:/Test.xlsx";//name of excel file
        
        FileOutputStream fileOut = new FileOutputStream(excelFileName);

        // write this workbook to an Outputstream.
        wb.write(fileOut);
        fileOut.flush();
        fileOut.close();

    }
    

}
