package com.opsfactor.community.platform.utility.fileprocessing;

import com.opsfactor.community.platform.exception.DataUploadException;
import com.pivovarit.function.ThrowingFunction;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Representa um arquivo ja normalizado para as rotinas de data upload.
 *
 * <p>O Community usa esta estrutura tanto para gerar templates simples quanto
 * para transformar linhas de arquivo em DTOs de integracao. A classe nao decide
 * quais colunas pertencem a Community ou Enterprise; essa fronteira fica nos
 * mappers especificos de cada carga.</p>
 **/
@Slf4j
@Data
@NoArgsConstructor
public class ProcessedFile {

    @Getter
    private List<ProcessedFileRow> fileRows = new ArrayList<>();
    private String name;

    /**
     * Método para extração de qualquer objeto para um arquivo. Usa o nome dos campos como headers
     * O valor de cada campo será incorporado com toString
     * @param <T>
     * @param objetoCollection
     */
    public <T> ProcessedFile(Collection<T> objetoCollection) {
        if (objetoCollection.size() == 0) return;

        /*
         * A colecao ja foi validada como nao vazia. Materializar o primeiro
         * objeto uma unica vez evita duas buscas independentes e deixa claro
         * que headers e nome do arquivo derivam da mesma classe de origem.
         */
        T primeiroObjeto = objetoCollection.iterator().next();
        name = primeiroObjeto.getClass().getSimpleName();
        Field[] fields = primeiroObjeto.getClass().getDeclaredFields();

        ProcessedFileRow headerRow = new ProcessedFileRow();
        Arrays.stream(fields)
                .forEach(field -> {
                    field.setAccessible(true);
                    headerRow.addContent(field.getName());
                });
        fileRows.add(headerRow);

        objetoCollection.stream()
                .map(x -> Arrays.stream(fields)
                       .map(ThrowingFunction.unchecked(field -> (field.get(x) == null) ? null : field.get(x)))
                       .collect(Collectors.toList()))
                .forEach(linhaArquivo -> {
                    fileRows.add(new ProcessedFileRow(linhaArquivo));
                });

    }


    public void addNewRow(){
        fileRows.add(new ProcessedFileRow());
    }

    public void addRows(List<ProcessedFileRow> processedFileRowList) {
        for (ProcessedFileRow processedFileRow : processedFileRowList) {
            addRow(processedFileRow);
        }
    }

    public void addRow(ProcessedFileRow processedFileRow){
        fileRows.add(processedFileRow);
    }

    public void addToLastRow(Object content) {
        fileRows.get(fileRows.size()-1).addContent(content);
    }

    /**
     * Removes one named column, including its header and every data value.
     *
     * @param columnHeader exact header generated for the column to remove
     */
    public void removeColumn(String columnHeader) {

        if (fileRows.isEmpty()) {
            return;
        }

        int columnPosition = fileRows.get(0).getRowAsObjectList().indexOf(columnHeader);
        if (columnPosition < 0) {
            throw new IllegalArgumentException("Column '" + columnHeader + "' does not exist in the processed file");
        }

        fileRows.forEach(processedFileRow -> processedFileRow.getRowAsObjectList().remove(columnPosition));
    }

    /**
     * Remove linhas do arquivo partindo do início
     * @param numberRows
     */
    public void removeRowsFromBeginning(int numberRows) {
        int numeroRemocoes = Math.min(fileRows.size(), numberRows);
        for (int i=1; i <= numeroRemocoes; i++) {
            fileRows.remove(0);
        }
    }

    /**
     * Converte todo o arquivo para uma lista de DTOs que serão salvos
     * Somente usado para carga manual de arquivos, e não para integração via JSON
     * @param processedFile
     * @return
     * @throws DataUploadException
     */
    public <T> List<T> getDTOListFromProcessedFile(int linhaInicialConsiderada, int numeroColunasConsiderado, Function<ProcessedFileRow, T> funcaoConversaoLinhaArquivoParaDTO) throws DataUploadException {

        List<T> listaDTOs = new ArrayList<>();

        // pula a primeira linha, que contém o cabeçalho
        for (int i=linhaInicialConsiderada; i < fileRows.size(); i++) {

            ProcessedFileRow linhaAtual = fileRows.get(i);

            // se todos os valores da linha estiverem em branco, não tentar converter para DTO (pular)
            if (linhaAtual.isEmpty()) continue;

            // se número de colunas preenchidas para esta linha superar o número máximo de colunas, gerar exceção
            if (linhaAtual.getRowSize() > numeroColunasConsiderado) {
                throw new ArrayIndexOutOfBoundsException("Number of columns is greater than " + numeroColunasConsiderado + " at line " + (i+1));
            }

            try {
                T dto = funcaoConversaoLinhaArquivoParaDTO.apply(linhaAtual);
                listaDTOs.add(dto);
            } catch (RuntimeException runtimeException) {
                /*
                 * A funcao de conversao e uma Function Java padrao, portanto
                 * falhas de mapper chegam aqui como excecoes unchecked. Esta
                 * borda apenas acrescenta o numero da linha ao erro funcional
                 * de data upload sem descartar a causa tecnica original.
                 */
                log.error("Erro ao converter linha {} do arquivo processado {}", i, name, runtimeException);
                throw new DataUploadException(runtimeException.getMessage() + " at line " + i, runtimeException);
            }

        }

        return listaDTOs;

    }

    public ProcessedFileRow getProcessedFileRow(int rowPosition) {
        return fileRows.get(rowPosition);
    }

    public List<Object> getFileRowAsObjectList(int rowPosition) {
        return fileRows.get(rowPosition).getRowAsObjectList();
    }

    public List<List<Object>> getFileContentsAsObjects() {
        return IntStream.range(0, fileRows.size())
                .mapToObj(rowPosition -> getFileRowAsObjectList(rowPosition))
                .collect(Collectors.toList());
    }

    public List<String> getFileRowAsStringListWithEmptyFieldsAsNull(int rowPosition) {
        return fileRows.get(rowPosition).getRowAsStringListWithEmptyFieldsAsNull();
    }

    public List<List<String>> getFileContentsWithEmptyFieldsAsNull() {
        return IntStream.range(0, fileRows.size())
                .mapToObj(rowPosition -> getFileRowAsStringListWithEmptyFieldsAsNull(rowPosition))
                .collect(Collectors.toList());
    }

    public List<String> getFileRowAsStringListWithEmptyFieldsAsEmptyStrings(int rowPosition) {
        return fileRows.get(rowPosition).getRowAsStringListWithEmptyFieldsAsEmptyStrings();
    }

    public List<List<String>> getFileContentsWithEmptyFieldsAsEmptyStrings() {
        return IntStream.range(0, fileRows.size())
                .mapToObj(rowPosition -> getFileRowAsStringListWithEmptyFieldsAsEmptyStrings(rowPosition))
                .collect(Collectors.toList());
    }

    public String getFileContentsAsSingleStringWithEmptyFieldsAsEmptyStrings(String separator) {
        return IntStream.range(0, fileRows.size())
                .mapToObj(rowPosition -> getFileRowAsStringListWithEmptyFieldsAsEmptyStrings(rowPosition)
                        .stream()
                        .collect(Collectors.joining(separator)))
                .collect(Collectors.joining(System.lineSeparator()));
    }

}
