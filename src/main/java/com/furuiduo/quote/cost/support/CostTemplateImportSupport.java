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

  public static void applyCustomFields(
      String mode,
      CostTableTemplateLayout layout,
      Row row,
      Map<String, Integer> headers,
      Map<String, Object> extraFields) {
    if (layout == null || layout.customFields() == null || layout.customFields().isEmpty()) {
      return;
    }
    Map<String, Object> target = extraFields == null ? new HashMap<>() : extraFields;
    List<CostExportColumn> columns = CostTemplateExcelSupport.exportColumns(mode, layout);
    Set<Integer> usedColumns = new HashSet<>();

    // 优先按导出列顺序对齐（可正确处理多个同名「生效期」）
    Map<String, String> dataTypeByField = dataTypeByField(layout);
    for (int i = 0; i < columns.size(); i++) {
      CostExportColumn column = columns.get(i);
      if (column == null || column.field() == null || !column.field().startsWith("cf_")) {
        continue;
      }
      if (!CostTemplateExcelSupport.isFieldVisible(layout, column.field())) {
        continue;
      }
      String text = readCustomCell(row, headers, column, i, usedColumns);
      if (text != null && !text.isBlank()) {
        target.put(
            column.field(),
            coerceCustomValue(text, dataTypeByField.get(column.field())));
      }
    }

    // 兜底：未写入的自定义字段再按标题/字段码查找
    for (CostTableCustomFieldDef custom : layout.customFields()) {
      if (custom == null || custom.field() == null || custom.field().isBlank()) {
        continue;
      }
      if (target.containsKey(custom.field())) {
        continue;
      }
      if (!CostTemplateExcelSupport.isFieldVisible(layout, custom.field())) {
        continue;
      }
      String title = CostTemplateExcelSupport.resolveFieldTitle(mode, custom.field(), layout);
      String text =
          CostExcelSupport.readByHeader(row, headers, title, "*" + title, custom.field());
      if (text != null && !text.isBlank()) {
        target.put(custom.field(), coerceCustomValue(text, custom.dataType()));
      }
    }
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

  private static Object coerceCustomValue(String text, String dataType) {
    String trimmed = text.trim();
    if ("number".equalsIgnoreCase(dataType)) {
      try {
        return new BigDecimal(trimmed.replace(",", ""));
      } catch (NumberFormatException ignored) {
        return trimmed;
      }
    }
    return trimmed;
  }

  private static String readCustomCell(
      Row row,
      Map<String, Integer> headers,
      CostExportColumn column,
      int exportIndex,
      Set<Integer> usedColumns) {
    Row headerRow = row.getSheet().getRow(0);
    if (headerRow != null) {
      String expected = CostExcelSupport.normalizeHeader(column.header());
      // 从左到右找尚未占用、标题匹配的列（支持重复表头）
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
          return CostExcelSupport.cellString(row.getCell(col));
        }
      }
      // 表头与导出列完全同序时，直接按索引读取
      if (exportIndex < lastCell && !usedColumns.contains(exportIndex)) {
        String header =
            CostExcelSupport.normalizeHeader(
                CostExcelSupport.cellString(headerRow.getCell(exportIndex)));
        if (header.equals(expected)) {
          usedColumns.add(exportIndex);
          return CostExcelSupport.cellString(row.getCell(exportIndex));
        }
      }
    }
    Integer mapped = headers.get(CostExcelSupport.normalizeHeader(column.header()));
    if (mapped != null && !usedColumns.contains(mapped)) {
      usedColumns.add(mapped);
      return CostExcelSupport.cellString(row.getCell(mapped));
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
