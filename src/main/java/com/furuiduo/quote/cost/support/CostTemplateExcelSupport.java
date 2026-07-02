package com.furuiduo.quote.cost.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.furuiduo.quote.cost.dto.CostExportColumn;
import com.furuiduo.quote.cost.dto.CostTableCustomFieldDef;
import com.furuiduo.quote.cost.dto.CostTableFieldOverride;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;

public final class CostTemplateExcelSupport {

  private static final Map<String, String> ROAD_LABELS =
      Map.ofEntries(
          Map.entry("validDate", "*VALID DATE"),
          Map.entry("supplier", "*SUPPLIER"),
          Map.entry("logYardNameAddress", "LOG YARD NAME &ADDRESS"),
          Map.entry("city", "*City"),
          Map.entry("state", "*State"),
          Map.entry("por", "*POR"),
          Map.entry("pol", "*POL"),
          Map.entry("baseFreight", "*BASE FREIGHT"),
          Map.entry("fsc", "FSC"),
          Map.entry("chassis", "CHASSIS"),
          Map.entry("owTriAxle", "OW/TRI-AXCEL"),
          Map.entry("split", "SPLIT"),
          Map.entry("stopOff", "STOP OFF"),
          Map.entry("allIn", "*ALL IN"),
          Map.entry("allInNonOak", "ALL IN (NON OAK)"),
          Map.entry("allInOak", "ALL IN (OAK)"),
          Map.entry("waitingFee", "WAITING FEE"),
          Map.entry("redelivery", "REDILEVEY"),
          Map.entry("prepull", "PREPULL"),
          Map.entry("nsLift", "NS LIFT"),
          Map.entry("remark", "REMARK"));

  private static final Map<String, String> SEA_LABELS =
      Map.ofEntries(
          Map.entry("origin", "POL"),
          Map.entry("destination", "POD"),
          Map.entry("unitPrice", "O/F RATE (USD)"),
          Map.entry("buc", "BUC"),
          Map.entry("surchargeValidDate", "附加费有效期"),
          Map.entry("allIn", "ALL IN"),
          Map.entry("carrier", "SSL"),
          Map.entry("remark", "备注"),
          Map.entry("validDate", "有效期"),
          Map.entry("status", "状态"));

  private static final Map<String, String> FUMIGATION_LABELS =
      Map.ofEntries(
          Map.entry("port", "PORT"),
          Map.entry("station", "STATION"),
          Map.entry("nonOakOutdoor", "NON-OAK OUTDOOR"),
          Map.entry("nonOakIndoor", "NON-OAK IN DOOR"),
          Map.entry("nonOakQuoteSummer", "NON-OAK 报价(夏季)"),
          Map.entry("nonOakQuoteWinter", "NON-OAK 报价(冬季)"),
          Map.entry("oakOutdoor", "OAK OUTDOOR"),
          Map.entry("oakIndoor", "OAK IN DOOR"),
          Map.entry("oakQuoteSummer", "OAK 报价(夏季)"),
          Map.entry("oakQuoteWinter", "OAK 报价(冬季)"),
          Map.entry("remark", "备注"),
          Map.entry("updatedAt", "更新时间"));

  private static final Map<String, String> RAIL_LABELS = copyRailLabels();

  private CostTemplateExcelSupport() {}

  public static List<CostExportColumn> exportColumns(
      String mode, CostTableTemplateLayout layout) {
    return resolveVisibleExportColumns(mode, layout);
  }

  public static byte[] buildWorkbook(
      String mode, String code, String name, CostTableTemplateLayout layout) {
    List<CostExportColumn> columns = resolveVisibleExportColumns(mode, layout);
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet(sanitizeSheetName(code, name));
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < columns.size(); i++) {
        headerRow.createCell(i).setCellValue(columns.get(i).header());
      }
      for (int rowIndex = 1; rowIndex <= 3; rowIndex++) {
        sheet.createRow(rowIndex);
      }
      for (int i = 0; i < columns.size(); i++) {
        String header = columns.get(i).header();
        int width = Math.max(12, Math.min(32, header.length() + 4));
        sheet.setColumnWidth(i, width * 256);
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to build template workbook", ex);
    }
  }

  public static String buildFilename(String code, String mode) {
    String safeCode = code == null || code.isBlank() ? mode : code.trim();
    return safeCode + "-template.xlsx";
  }

  private static List<CostExportColumn> resolveVisibleExportColumns(
      String mode, CostTableTemplateLayout layout) {
    List<CostExportColumn> columns = new ArrayList<>();
    for (String field : CostFieldCatalog.resolveFieldKeys(layout)) {
      if (!isFieldVisible(layout, field)) {
        continue;
      }
      String title = resolveFieldTitle(mode, field, layout);
      boolean required = isFieldRequired(layout, field);
      String header = required && !title.startsWith("*") ? "*" + title : title;
      columns.add(new CostExportColumn(field, header));
    }
    return columns;
  }

  private static boolean isFieldVisible(CostTableTemplateLayout layout, String field) {
    CostTableFieldOverride override = fieldOverride(layout, field);
    return override == null || override.visible() == null || Boolean.TRUE.equals(override.visible());
  }

  private static boolean isFieldRequired(CostTableTemplateLayout layout, String field) {
    CostTableCustomFieldDef custom = findCustomDef(layout, field);
    if (custom != null && Boolean.TRUE.equals(custom.required())) {
      return true;
    }
    CostTableFieldOverride override = fieldOverride(layout, field);
    return override != null && Boolean.TRUE.equals(override.required());
  }

  private static String resolveFieldTitle(
      String mode, String field, CostTableTemplateLayout layout) {
    CostTableCustomFieldDef custom = findCustomDef(layout, field);
    if (custom != null && custom.title() != null && !custom.title().isBlank()) {
      return custom.title().trim();
    }
    CostTableFieldOverride override = fieldOverride(layout, field);
    if (override != null && override.title() != null && !override.title().isBlank()) {
      return override.title().trim();
    }
    return labelsForMode(mode).getOrDefault(field, field);
  }

  private static CostTableCustomFieldDef findCustomDef(
      CostTableTemplateLayout layout, String field) {
    if (layout.customFields() == null) {
      return null;
    }
    return layout.customFields().stream()
        .filter(item -> field.equals(item.field()))
        .findFirst()
        .orElse(null);
  }

  private static CostTableFieldOverride fieldOverride(
      CostTableTemplateLayout layout, String field) {
    if (layout.fieldOverrides() == null) {
      return null;
    }
    return layout.fieldOverrides().get(field);
  }

  private static Map<String, String> labelsForMode(String mode) {
    return switch (mode) {
      case "road" -> ROAD_LABELS;
      case "sea" -> SEA_LABELS;
      case "fumigation" -> FUMIGATION_LABELS;
      case "rail" -> RAIL_LABELS;
      default -> Map.of();
    };
  }

  private static String sanitizeSheetName(String code, String name) {
    String raw = code != null && !code.isBlank() ? code.trim() : name;
    if (raw == null || raw.isBlank()) {
      raw = "template";
    }
    String sanitized = raw.replaceAll("[\\\\/?*\\[\\]:]", "_");
    return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
  }

  private static Map<String, String> copyRailLabels() {
    Map<String, String> labels = new LinkedHashMap<>(SEA_LABELS);
    labels.put("origin", "发站");
    labels.put("destination", "到站");
    labels.put("unitPrice", "铁路运费");
    return Map.copyOf(labels);
  }
}
