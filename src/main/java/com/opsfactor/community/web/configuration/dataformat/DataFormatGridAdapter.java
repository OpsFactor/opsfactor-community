package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts the existing AgGridDTO view contract into tabular export rows. This
 * shared adapter preserves view-column order and labels without changing report
 * controllers or their JSON response. Rows are materialized one at a time.
 */
final class DataFormatGridAdapter {

    private record Column(String header, String field) { }

    private DataFormatGridAdapter() {

    }

    static boolean supports(Class<?> type) {

        for (Class<?> candidate = type; candidate != null; candidate = candidate.getSuperclass()) {
            if (candidate.getSimpleName().equals("AgGridDTO")) {
                return true;
            }
        }
        return false;

    }

    static List<List<Object>> matrix(Object grid, ObjectMapper mapper) throws IOException {

        Object data = property(grid, "data");
        Object definitions = property(grid, "columnDefs");
        if (!(data instanceof List<?> records) || !(definitions instanceof List<?>)) {
            throw new IOException("A report export requires AgGridDTO data and columnDefs lists.");
        }
        List<Column> columns = new ArrayList<>();
        collectColumns(definitions, "", columns);
        Set<String> headers = new HashSet<>();
        List<Object> headerRow = new ArrayList<>();
        for (Column column : columns) {
            String label = headers.add(column.header) ? column.header : column.header + " (" + column.field + ")";
            if (!label.equals(column.header) && !headers.add(label)) {
                throw new IOException("Duplicate report export column: " + label);
            }
            headerRow.add(label);
        }
        return new AbstractList<>() {
            @Override
            public int size() {

                return records.size() + 1;

            }

            @Override
            public List<Object> get(int index) {

                if (index == 0) {
                    return headerRow;
                }
                JsonNode record = mapper.valueToTree(records.get(index - 1));
                List<Object> row = new ArrayList<>(columns.size());
                for (Column column : columns) {
                    JsonNode value = record.get(column.field);
                    if (value == null) {
                        value = record;
                        for (String part : column.field.split("\\.")) {
                            value = value == null || value.isNull() ? null : value.get(part);
                        }
                    }
                    if (value == null || value.isNull()) row.add(null);
                    else if (value.isTextual()) row.add(value.textValue());
                    else if (value.isBoolean()) row.add(value.booleanValue());
                    // Preserve approximate versus exact numbers until the file codec writes them.
                    else if (value.isNumber()) row.add(value.numberValue());
                    else throw new IllegalArgumentException("Report column is not scalar: " + column.field);
                }
                return row;

            }
        };

    }

    private static void collectColumns(Object definitions, String parent, List<Column> result) throws IOException {

        if (!(definitions instanceof List<?> columns)) {
            throw new IOException("Invalid report column definitions.");
        }
        for (Object definition : columns) {
            Object children = property(definition, "children");
            String label = (String) property(definition, "headerName");
            String field = (String) property(definition, "field");
            String path = parent.isBlank() ? label : parent + " / " + label;
            if (children instanceof List<?> nested && !nested.isEmpty()) {
                collectColumns(nested, path == null ? parent : path, result);
            } else if (field != null && !field.isBlank()) {
                result.add(new Column(label == null || label.isBlank() ? field : path, field));
            }
        }

    }

    private static Object property(Object bean, String name) throws IOException {

        try {
            Field field = bean.getClass().getField(name);
            return field.get(bean);
        } catch (ReflectiveOperationException exception) {
            throw new IOException("Invalid AgGridDTO property: " + name, exception);
        }

    }
}
