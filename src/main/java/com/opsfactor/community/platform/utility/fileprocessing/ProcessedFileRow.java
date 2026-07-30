package com.opsfactor.community.platform.utility.fileprocessing;

import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Linha de arquivo processado com conversores tipados usados pelos mappers.
 *
 * <p>Strings vazias sao normalizadas para {@code null} no momento da inclusao
 * para que os mappers de integracao possam diferenciar campo ausente de valor
 * textual real sem repetir essa regra em cada carga.</p>
 **/
@Data
@NoArgsConstructor
@Accessors(chain = true, fluent = true)
public class ProcessedFileRow {

    /**
     * Valores normalizados da linha processada.
     *
     * <p>A lista guarda objetos tipados quando a origem ja trouxe tipo forte
     * (datas, numeros, booleanos) e strings para valores textuais. Campos
     * vazios sao armazenados como {@code null}; mappers de integracao devem
     * interpretar {@code null} como campo ausente no arquivo, nao como texto
     * vazio.</p>
     */
    private List<Object> row = new ArrayList<>();
    
    public ProcessedFileRow(List<Object> rowContent) {
        for (Object content : rowContent) {
            addContent(content);
        }
    }

    /**
     * Adiciona uma celula ao snapshot da linha preservando tipos relevantes
     * para os conversores posteriores.
     *
     * <p>Enums sao exportados pelo valor JSON publico para que templates de
     * data upload nao vazem nomes internos de constantes Java. Strings vazias
     * viram {@code null}, mantendo um unico significado para campo ausente.</p>
     */
    public void addContent(Object content) {
        
        if (content == null) row.add(null);
        else if (content instanceof String) {
            String contentAsString = (String) content;
            if (contentAsString.equals("")) {
                row.add(null);
            } else {
                row.add(contentAsString);
            }
        }
        else if (content instanceof Long) row.add((Long) content);
        else if (content instanceof Double) row.add((Double) content);
        else if (content instanceof Float) row.add((Float) content);
        else if (content instanceof Integer) row.add((Integer) content);
        else if (content instanceof Boolean) row.add((Boolean) content);
        else if (content.getClass().isEnum()) row.add(MetodosUtilidade.getValorJsonPropertyDeEnum((Enum) content));
        else row.add(content.toString());
        
    }

    /**
     * Retorna o valor bruto da coluna ou {@code null} quando a coluna nao foi
     * enviada.
     *
     * <p>O retorno {@code null} para coluna fora do tamanho fisico da linha e
     * intencional: varios mappers compartilham templates que podem ganhar
     * colunas opcionais no Enterprise, e o Community deve tratar ausencia como
     * campo nao informado.</p>
     */
    public Object getColumnValue(int columnPosition) {
        
        if (columnPosition >= row.size()) return null;
        
        // sempre será uma string diferente de "" , pois strings vazias são
        // substituídas por null em addContent
        return row.get(columnPosition);
                
    }
    
    public String getColumnValueAsString(int columnPosition) {
        
        Object columnValue = getColumnValue(columnPosition);
        
        return (columnValue == null) ? null : columnValue.toString();
                
    }
    
    public LocalDateTime getColumnValueAsLocalDateTime(int columnPosition) {
        
        Object columnValue = getColumnValue(columnPosition);
        
        if (columnValue == null) return null;
        if (columnValue.equals("")) return null;

        if (columnValue instanceof LocalDateTime) {
            return (LocalDateTime) columnValue;
        } else if (columnValue instanceof LocalDate) {
            return ((LocalDate) columnValue).atTime(0, 0, 0);
        } else {
            try {
                return Calendario.stringToLocalDateTime(columnValue.toString());
            } catch (DateTimeParseException dateTimeParseException) {
                throw new DataUploadException(
                        "Incompatible type for column " + (columnPosition + 1) + " : should be date/time but found " + columnValue.toString(),
                        dateTimeParseException);
            }
        }       
        
    }

    public LocalTime getColumnValueAsLocalTime(int columnPosition) {

        Object columnValue = getColumnValue(columnPosition);

        if (columnValue == null) return null;
        if (columnValue.equals("")) return null;

        if (columnValue instanceof LocalTime) {
            return (LocalTime) columnValue;
        } else if (columnValue instanceof LocalDateTime) {
            return ((LocalDateTime) columnValue).toLocalTime();
        } else if (columnValue instanceof LocalDate) {
            return ((LocalDate) columnValue).atTime(0, 0, 0).toLocalTime();
        } else {
            try {
                return Calendario.stringToLocalTime(columnValue.toString());
            } catch (DateTimeParseException dateTimeParseException) {
                throw new DataUploadException(
                        "Incompatible type for column " + (columnPosition + 1) + " : should be time but found " + columnValue.toString(),
                        dateTimeParseException);
            }
        }

    }

    public LocalDate getColumnValueAsLocalDate(int columnPosition) {
        
        Object columnValue = getColumnValue(columnPosition);
        
        if (columnValue == null) return null;
        
        if (columnValue instanceof LocalDateTime) {
            return ((LocalDateTime) columnValue).toLocalDate();
        } else if (columnValue instanceof LocalDate) {
            return (LocalDate) columnValue;
        } else {
            try {
                return Calendario.stringToLocalDate(columnValue.toString());
            } catch (DateTimeParseException dateTimeParseException) {
                throw new DataUploadException(
                        "Incompatible type for column " + (columnPosition + 1) + " : should be date/time but found " + columnValue.toString(),
                        dateTimeParseException);
            }
        }
                        
    }
    
    public Float getColumnValueAsFloat(int columnPosition) {
        
        Object columnValue = getColumnValue(columnPosition);

        if (columnValue == null) return null;
        
        if (columnValue instanceof Number) {
            return ((Number) columnValue).floatValue();
        } else {
            try {
                return Float.valueOf(columnValue.toString());
            } catch (NumberFormatException numberFormatException) {
                throw new DataUploadException(
                        "Incompatible type for column " + (columnPosition + 1) + " : should be a decimal number (with . decimal separator) but found " + columnValue,
                        numberFormatException);
            }
        }
                        
    }
    
    public Long getColumnValueAsLong(int columnPosition) {
        
        Object columnValue = getColumnValue(columnPosition);

        if (columnValue == null) return null;
        
        if (columnValue instanceof Number) {
            return ((Number) columnValue).longValue();
        } else {
            try {
                return Long.valueOf(columnValue.toString());
            } catch (NumberFormatException numberFormatException) {
                throw new DataUploadException(
                        "Incompatible type for column " + (columnPosition + 1) + " : should be a long number (no decimal separator) but found " + columnValue,
                        numberFormatException);
            }
        }
                        
    }
    
    public Integer getColumnValueAsInteger(int columnPosition) {
        
        Object columnValue = getColumnValue(columnPosition);

        if (columnValue == null) return null;
        
        if (columnValue instanceof Number) {
            return ((Number) columnValue).intValue();
        } else {
            try {
                return Integer.valueOf(columnValue.toString());
            } catch (NumberFormatException numberFormatException) {
                throw new DataUploadException(
                        "Incompatible type for column " + (columnPosition + 1) + " : should be an integer number (no decimal separator) but found " + columnValue,
                        numberFormatException);
            }
        }
                        
    }
    
    public Double getColumnValueAsDouble(int columnPosition) {
        
        Object columnValue = getColumnValue(columnPosition);

        if (columnValue == null) return null;
        
        if (columnValue instanceof Number) {
            return ((Number) columnValue).doubleValue();
        } else {
            try {
                return Double.valueOf(columnValue.toString());
            } catch (NumberFormatException numberFormatException) {
                throw new DataUploadException(
                        "Incompatible type for column " + (columnPosition + 1) + " : should be a decimal number (with . decimal separator) but found " + columnValue,
                        numberFormatException);
            }
        }
                        
    }

    /**
     * Converte uma coluna para booleano aceitando somente o vocabulario publico
     * de data upload: `1`, `0`, `true` e `false`.
     *
     * <p>Valores ausentes retornam {@code null}. Valores preenchidos mas fora
     * do contrato falham como {@link DataUploadException}, preservando a coluna
     * que precisa ser ajustada pelo usuario.</p>
     */
    public Boolean getColumnValueAsBoolean(int columnPosition) {
        
        Object columnValue = getColumnValue(columnPosition);

        if (columnValue == null) return null;
        
        if (columnValue instanceof Boolean) {
            return (Boolean) columnValue;
        } else {
            try {
                return MetodosUtilidade.converteStringParaBoolean(columnValue.toString());
            } catch (IllegalArgumentException illegalArgumentException) {
                /*
                 * O helper conhece o vocabulario booleano aceito; esta camada
                 * acrescenta o contexto operacional da carga, em especial a
                 * posicao da coluna que precisa ser corrigida no arquivo, sem
                 * descartar a causa tecnica original.
                 */
                throw new DataUploadException(
                        "Incompatible type for column " + (columnPosition + 1) + " : should be a binary number (0/1 or true/false) but found " + columnValue,
                        illegalArgumentException);
            }
        }
                        
    }
    
    public boolean isEmpty() {
        return !row.stream().anyMatch(x -> x != null);
    }
    
    public int getRowSize() {
        return row.size();
    }
    
    public List<Object> getRowAsObjectList() {
        return row;
    }
    
    public List<String> getRowAsStringListWithEmptyFieldsAsNull() {
        return row.stream()
                .map(x -> (x == null) ? null : x.toString())
                .collect(Collectors.toList());
    }
    
    public List<String> getRowAsStringListWithEmptyFieldsAsEmptyStrings() {
        return row.stream()
                .map(x -> (x == null) ? "" : x.toString())
                .collect(Collectors.toList());
    }
    
}
