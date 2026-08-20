package com.furuiduo.quote.cost.support;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.furuiduo.quote.cost.dto.CostExportColumn;
import com.furuiduo.quote.cost.dto.CostTableCustomFieldDef;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;

/** 按成本库模板校验导入必填，并读取自定义列。 */
public final class CostTemplateImportSupport {

  private CostTemplateImportSupport() {}

  public static String validateRequired(
      String mode, CostTableTemplateLayout layout, Function<String, Object> valueGetter) {
    if (layout == null) {
      return null;
    }
    for (CostExportColumn column : CostTemplateExcelSupport.exportColumns(mode, layout)) {
      String field = column.field();
      if (!CostTemplateExcelSupport.isFieldRequired(layout, field)) {
        continue;
      }
      if (isEmpty(valueGetter.apply(field))) {
        String title = stripRequiredMark(column.header());
        return title + "为必填项";
      }
    }
    return null;
  }

  /** 按导出列顺序读取一行所有可见字段（可正确处理重复「生效期/有效期」表头）。 */
  public static Map<String, String> readTemplateRowValues(
      String mode, CostTableTemplateLayout layout, Row row) {
    Map<String, String> values = new HashMap<>();
    if (layout == null || row == null) {
      return values;
    }
    Map<String, Integer> headers = CostExcelSupport.readHeaderMap(row.getSheet().getRow(0));
    List<CostExportColumn> columns = CostTemplateExcelSupport.exportColumns(mode, layout);
    Set<Integer> usedColumns = new HashSet<>();
    for (int i = 0; i < columns.size(); i++) {
      CostExportColumn column = columns.get(i);
      if (column == null || column.field() == null || column.field().isBlank()) {
        continue;
      }
      if ("status".equals(column.field())) {
        continue;
      }
      if (!CostTemplateExcelSupport.isFieldVisible(layout, column.field())) {
        continue;
      }
      String text = readLayoutCell(row, headers, column, i, usedColumns);
      if (text != null && !text.isBlank()) {
        values.put(column.field(), text.trim());
      }
    }
    return values;
  }

  public static void applyCustomFields(
      String mode,
      CostTableTemplateLayout layout,
      Row row,
      Map<String, Integer> headers,
      Map<String, Object> extraFields) {
    applyCustomFieldsFromValues(mode, layout, readTemplateRowValues(mode, layout, row), extraFields);
  }

  public static void applyCustomFieldsFromValues(
      String mode,
      CostTableTemplateLayout layout,
      Map<String, String> values,
      Map<String, Object> extraFields) {
    if (layout == null || layout.customFields() == null || layout.customFields().isEmpty()) {
      return;
    }
    Map<String, Object> target = extraFields == null ? new HashMap<>() : extraFields;
    Map<String, String> dataTypeByField = dataTypeByField(layout);
    for (CostTableCustomFieldDef custom : layout.customFields()) {
      if (custom == null || custom.field() == null || custom.field().isBlank()) {
        continue;
      }
      if (!CostTemplateExcelSupport.isFieldVisible(layout, custom.field())) {
        continue;
      }
      String text = values.get(custom.field());
      if (text == null || text.isBlank()) {
        continue;
      }
      target.put(custom.field(), coerceCustomValue(text, dataTypeByField.get(custom.field()), layout, custom.field()));
    }
  }

  public static String normalizeImportDateValue(
      String mode, CostTableTemplateLayout layout, String field, String raw) {
    if (raw == null || raw.isBlank()) {
      return raw;
    }
    if (!isImportDateField(mode, layout, field)) {
      return raw.trim();
    }
    return CostValidityStatus.normalizeImportDate(raw);
  }

  public static void normalizeExtraDateFields(
      String mode, CostTableTemplateLayout layout, Map<String, Object> extraFields) {
    if (extraFields == null || extraFields.isEmpty()) {
      return;
    }
    for (Map.Entry<String, Object> entry : extraFields.entrySet()) {
      String field = entry.getKey();
      Object value = entry.getValue();
      if (value == null || !isImportDateField(mode, layout, field)) {
        continue;
      }
      String text = String.valueOf(value).trim();
      if (!text.isEmpty()) {
        entry.setValue(CostValidityStatus.normalizeImportDate(text));
      }
    }
  }

  public static boolean isImportDateField(
      String mode, CostTableTemplateLayout layout, String field) {
    if (field == null || field.isBlank()) {
      return false;
    }
    if (field.endsWith("ValidDate") || field.endsWith("Validity") || field.endsWith("_eff")) {
      return true;
    }
    if ("validDate".equals(field) || "validFrom".equals(field) || "validTo".equals(field)) {
      return true;
    }
    CostTableCustomFieldDef custom = findCustomDef(layout, field);
    return custom != null && "date".equalsIgnoreCase(custom.dataType());
  }

  private static CostTableCustomFieldDef findCustomDef(
      CostTableTemplateLayout layout, String field) {
    if (layout == null || layout.customFields() == null) {
      return null;
    }
    return layout.customFields().stream()
        .filter(item -> field.equals(item.field()))
        .findFirst()
        .orElse(null);
  }

  private static Map<String, String> dataTypeByField(CostTableTemplateLayout layout) {
    Map<String, String> map = new HashMap<>();
    if (layout.customFields() == null) {
      return map;
    }
    for (CostTableCustomFieldDef custom : layout.customFields()) {
      if (custom != null && custom.field() != null) {
        map.put(custom.field(), custom.dataType());
      }
    }
    return map;
  }

  private static Object coerceCustomValue(
      String text, String dataType, CostTableTemplateLayout layout, String field) {
    String trimmed = text.trim();
    if ("number".equalsIgnoreCase(dataType)) {
      try {
        return new BigDecimal(trimmed.replace(",", ""));
      } catch (NumberFormatException ignored) {
        return trimmed;
      }
    }
    if ("date".equalsIgnoreCase(dataType)) {
      return CostValidityStatus.normalizeImportDate(trimmed);
    }
    if (isImportDateField(null, layout, field)) {
      return CostValidityStatus.normalizeImportDate(trimmed);
    }
    return trimmed;
  }

  static String readLayoutCell(
      Row row,
      Map<String, Integer> headers,
      CostExportColumn column,
      int exportIndex,
      Set<Integer> usedColumns) {
    Row headerRow = row.getSheet().getRow(0);
    if (headerRow != null) {
      String expected = CostExcelSupport.normalizeHeader(column.header());
      int lastCell = Math.max(headerRow.getLastCellNum(), 0);
      for (int col = 0; col < lastCell; col++) {
        if (usedColumns.contains(col)) {
          continue;
        }
        Cell cell = headerRow.getCell(col);
        if (cell == null) {
          continue;
        }
        String header = CostExcelSupport.normalizeHeader(CostExcelSupport.cellString(cell));
        if (header.equals(expected)
            || header.equals(CostExcelSupport.normalizeHeader(column.field()))) {
          usedColumns.add(col);
          return CostExcelSupport.cellImportText(row.getCell(col));
        }
      }
      if (exportIndex < lastCell && !usedColumns.contains(exportIndex)) {
        String header =
            CostExcelSupport.normalizeHeader(
                CostExcelSupport.cellString(headerRow.getCell(exportIndex)));
        if (header.equals(expected)) {
          usedColumns.add(exportIndex);
          return CostExcelSupport.cellImportText(row.getCell(exportIndex));
        }
      }
    }
    Integer mapped = headers.get(CostExcelSupport.normalizeHeader(column.header()));
    if (mapped != null && !usedColumns.contains(mapped)) {
      usedColumns.add(mapped);
      return CostExcelSupport.cellImportText(row.getCell(mapped));
    }
    return "";
  }

  public static boolean isEmpty(Object value) {
    if (value == null) {
      return true;
    }
    if (value instanceof String text) {
      return text.isBlank();
    }
    if (value instanceof BigDecimal) {
      return false;
    }
    if (value instanceof Number) {
      return false;
    }
    return String.valueOf(value).isBlank();
  }

  private static String stripRequiredMark(String header) {
    if (header == null) {
      return "";
    }
    String trimmed = header.trim();
    if (trimmed.startsWith("*")) {
      return trimmed.substring(1).trim();
    }
    return trimmed;
  }
}
