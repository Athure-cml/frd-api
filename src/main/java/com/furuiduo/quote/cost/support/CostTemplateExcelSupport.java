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
          Map.entry("zipCode", "ZIP CODE"),
          Map.entry("city", "City"),
          Map.entry("state", "State"),
          Map.entry("por", "POR"),
          Map.entry("pol", "POL"),
          Map.entry("supplier", "SUPPLIER"),
          Map.entry("baseFreight", "BASE FREIGHT"),
          Map.entry("fsc", "FSC (%)"),
          Map.entry("chassis", "CHASSIS"),
          Map.entry("triTandemAxle", "OW/TRI-AXCEL"),
          Map.entry("split", "SPLIT"),
          Map.entry("stopOff", "STOP OFF"),
          Map.entry("allInNoFm", "ALL IN - NO FM"),
          Map.entry("allInFmOneWay", "ALL IN - FM (NON OAK)"),
          Map.entry("allInFmRound", "ALL IN - FM (OAK)"),
          Map.entry("waitingFee", "WAITING FEE"),
          Map.entry("redelivery", "REDELIVERY"),
          Map.entry("prepull", "PREPULL"),
          Map.entry("nsLift", "NS LIFT"),
          Map.entry("otherFee", "OTHER FEE"),
          Map.entry("remark", "REMARK"),
          Map.entry("validDate", "有效期"),
          Map.entry("logYardNameAddress", "LOG YARD NAME & ADDRESS"));

  private static final Map<String, String> SEA_LABELS =
      Map.ofEntries(
          Map.entry("por", "POR"),
          Map.entry("pol", "POL"),
          Map.entry("pod", "POD"),
          Map.entry("cnShortName", "中文简称"),
          Map.entry("enProductName", "英文品名"),
          Map.entry("containerType", "箱型"),
          Map.entry("freight", "运费"),
          Map.entry("freightValidDate", "有效期"),
          Map.entry("buc", "BUC"),
          Map.entry("bucValidDate", "BUC有效期"),
          Map.entry("ebs", "EBS"),
          Map.entry("ebsValidDate", "EBS有效期"),
          Map.entry("gri", "GRI"),
          Map.entry("griValidDate", "GRI有效期"),
          Map.entry("others", "OTHERS"),
          Map.entry("othersValidDate", "OTHERS有效期"),
          Map.entry("allIn", "ALL IN (小计)"),
          Map.entry("ssl", "SSL (船公司)"),
          Map.entry("agent", "AGENT (代理)"),
          Map.entry("remark", "REMARK 备注"));

  private static final Map<String, String> FUMIGATION_LABELS =
      Map.ofEntries(
          Map.entry("region", "REGION"),
          Map.entry("station", "STATION"),
          Map.entry("outdoorNonOak", "FM-OUTDOOR NON OAK"),
          Map.entry("outdoorOak", "FM-OUTDOOR OAK"),
          Map.entry("outdoorValidity", "有效期"),
          Map.entry("indoorNonOak", "FM-INDOOR NON OAK"),
          Map.entry("indoorOak", "FM-INDOOR OAK"),
          Map.entry("indoorValidity", "有效期"),
          Map.entry("address", "ADDRESS"));

  private static final Map<String, String> RAIL_LABELS = copyRailLabels();

  private CostTemplateExcelSupport() {}

  public static List<CostExportColumn> exportColumns(
      String mode, CostTableTemplateLayout layout) {
    return resolveVisibleExportColumns(mode, layout);
  }

  public static byte[] buildWorkbook(
      String mode, String code, String name, CostTableTemplateLayout layout) {
    // 与成本库列表页导出表头保持一致：熏蒸/海运为双行合并表头
    if ("fumigation".equals(mode)) {
      return FumigationCostExcelExporter.export(List.of());
    }
    if ("sea".equals(mode)) {
      return SeaCostExcelExporter.export(List.of());
    }

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
    return modeTemplateLabel(mode) + ".xlsx";
  }

  public static String buildPreviewFilename(String mode) {
    return modeTemplateLabel(mode) + "预览.xlsx";
  }

  private static String modeTemplateLabel(String mode) {
    if (mode == null) {
      return "成本模板";
    }
    return switch (mode) {
      case "road" -> "卡车成本模板";
      case "sea" -> "海运成本模板";
      case "fumigation" -> "熏蒸成本模板";
      default -> mode + "成本模板";
    };
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
    Map<String, String> labels = new LinkedHashMap<>();
    labels.put("origin", "发站");
    labels.put("destination", "到站");
    labels.put("carrier", "承运商");
    labels.put("spec", "箱型");
    labels.put("unit", "单位");
    labels.put("unitPrice", "铁路运费");
    labels.put("currency", "币种");
    labels.put("validFrom", "有效期起");
    labels.put("validTo", "有效期止");
    labels.put("status", "状态");
    labels.put("remark", "备注");
    labels.put("updatedAt", "更新时间");
    return Map.copyOf(labels);
  }
}
