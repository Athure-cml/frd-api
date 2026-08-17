package com.furuiduo.quote.cost.support;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.furuiduo.quote.cost.dto.CostExportColumn;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;

public final class CostDataExcelExporter {

  private static final DateTimeFormatter UPDATED_AT_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private CostDataExcelExporter() {}

  public static byte[] exportRoad(List<CostRoad> items, CostTableTemplateLayout layout) {
    List<CostExportColumn> columns = columnsWithStatus("road", layout);
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("road");
      writeHeaderRow(sheet, columns);
      int rowIndex = 1;
      for (CostRoad item : items) {
        Row row = sheet.createRow(rowIndex++);
        for (int col = 0; col < columns.size(); col++) {
          writeCell(row, col, readRoadValue(item, columns.get(col).field()));
        }
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("导出失败", ex);
    }
  }

  public static byte[] exportSea(List<CostSea> items, CostTableTemplateLayout layout) {
    return exportFreight("sea", items, layout, CostDataExcelExporter::readSeaValue);
  }

  public static byte[] exportFumigation(
      List<CostFumigation> items, CostTableTemplateLayout layout) {
    return exportFreight(
        "fumigation", items, layout, CostDataExcelExporter::readFumigationValue);
  }

  /** 模板列之外固定追加「状态」，与列表展示一致。 */
  private static List<CostExportColumn> columnsWithStatus(
      String mode, CostTableTemplateLayout layout) {
    List<CostExportColumn> columns =
        new ArrayList<>(CostTemplateExcelSupport.exportColumns(mode, layout));
    boolean hasStatus = columns.stream().anyMatch(column -> "status".equals(column.field()));
    if (!hasStatus) {
      columns.add(new CostExportColumn("status", "状态"));
    }
    return columns;
  }

  private static <T> byte[] exportFreight(
      String mode,
      List<T> items,
      CostTableTemplateLayout layout,
      FreightValueReader<T> reader) {
    List<CostExportColumn> columns = columnsWithStatus(mode, layout);
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet(mode);
      writeHeaderRow(sheet, columns);
      int rowIndex = 1;
      for (T item : items) {
        Row row = sheet.createRow(rowIndex++);
        for (int col = 0; col < columns.size(); col++) {
          writeCell(row, col, reader.read(item, columns.get(col).field()));
        }
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("导出失败", ex);
    }
  }

  @FunctionalInterface
  private interface FreightValueReader<T> {
    Object read(T item, String field);
  }

  private static void writeHeaderRow(Sheet sheet, List<CostExportColumn> columns) {
    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < columns.size(); i++) {
      headerRow.createCell(i).setCellValue(columns.get(i).header());
    }
  }

  private static Object readRoadValue(CostRoad item, String field) {
    if (isCustomField(field)) {
      return readExtraField(item.getExtraFields(), field);
    }
    return switch (field) {
      case "zipCode" -> item.getZipCode();
      case "city" -> item.getCity();
      case "state" -> item.getState();
      case "por" -> item.getPor();
      case "pol" -> item.getPol();
      case "supplier" -> item.getSupplier();
      case "baseFreight" -> item.getBaseFreight();
      case "fsc" -> item.getFsc();
      case "chassis" -> item.getChassis();
      case "triTandemAxle" -> item.getTriTandemAxle();
      case "split" -> item.getSplit();
      case "stopOff" -> item.getStopOff();
      case "allInNoFm" -> item.getAllInNoFm();
      case "allInFmOneWay" -> item.getAllInFmOneWay();
      case "allInFmRound" -> item.getAllInFmRound();
      case "waitingFee" -> item.getWaitingFee();
      case "redelivery" -> item.getRedelivery();
      case "prepull" -> item.getPrepull();
      case "nsLift" -> item.getNsLift();
      case "otherFee" -> item.getOtherFee();
      case "remark" -> item.getRemark();
      case "validDate" -> item.getValidDate();
      case "logYardNameAddress" -> item.getLogYardNameAddress();
      case "status" ->
          formatStatusLabel(
              CostValidityStatus.resolve(item.getStatus(), item.getValidDate()));
      default -> null;
    };
  }

  private static Object readSeaValue(CostSea item, String field) {
    if (isCustomField(field)) {
      return readExtraField(item.getExtraFields(), field);
    }
    return switch (field) {
      case "por" -> item.getPor();
      case "pol" -> item.getPol();
      case "pod" -> item.getPod();
      case "cnShortName" -> item.getCnShortName();
      case "enProductName" -> item.getEnProductName();
      case "containerType" -> item.getContainerType();
      case "freight" -> item.getFreight();
      case "freightValidDate" -> item.getFreightValidDate();
      case "buc" -> item.getBuc();
      case "bucValidDate" -> item.getBucValidDate();
      case "ebs" -> item.getEbs();
      case "ebsValidDate" -> item.getEbsValidDate();
      case "gri" -> item.getGri();
      case "griValidDate" -> item.getGriValidDate();
      case "others" -> item.getOthers();
      case "othersValidDate" -> item.getOthersValidDate();
      case "allIn" -> item.getAllIn();
      case "ssl" -> item.getSsl();
      case "agent" -> item.getAgent();
      case "remark" -> item.getRemark();
      case "status" ->
          formatStatusLabel(
              CostValidityStatus.resolve(item.getStatus(), item.getFreightValidDate()));
      case "updatedAt" ->
          item.getUpdatedAt() == null ? null : item.getUpdatedAt().format(UPDATED_AT_FORMATTER);
      default -> null;
    };
  }

  private static Object readFumigationValue(CostFumigation item, String field) {
    if (isCustomField(field)) {
      return readExtraField(item.getExtraFields(), field);
    }
    return switch (field) {
      case "region" -> item.getRegion();
      case "station" -> item.getStation();
      case "outdoorNonOak" -> item.getOutdoorNonOak();
      case "outdoorOak" -> item.getOutdoorOak();
      case "outdoorValidity" -> item.getOutdoorValidity();
      case "indoorNonOak" -> item.getIndoorNonOak();
      case "indoorOak" -> item.getIndoorOak();
      case "indoorValidity" -> item.getIndoorValidity();
      case "address" -> item.getAddress();
      case "status" ->
          formatStatusLabel(
              CostValidityStatus.resolve(
                  item.getStatus(), item.getOutdoorValidity(), item.getIndoorValidity()));
      case "updatedAt" ->
          item.getUpdatedAt() == null ? null : item.getUpdatedAt().format(UPDATED_AT_FORMATTER);
      default -> null;
    };
  }

  private static String formatStatusLabel(CostStatus status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case active -> "生效中";
      case draft -> "草稿";
      case expired -> "已过期";
    };
  }

  private static Object readFreightValue(
      Map<String, Object> extraFields,
      String field,
      String origin,
      String destination,
      String carrier,
      String spec,
      String unit,
      BigDecimal unitPrice,
      String currency,
      LocalDate validFrom,
      LocalDate validTo,
      CostStatus status,
      String remark,
      LocalDateTime updatedAt) {
    if (isCustomField(field)) {
      return readExtraField(extraFields, field);
    }
    return switch (field) {
      case "origin" -> origin;
      case "destination" -> destination;
      case "carrier" -> carrier;
      case "spec" -> spec;
      case "unit" -> unit;
      case "unitPrice" -> unitPrice;
      case "currency" -> currency;
      case "validFrom" -> validFrom == null ? null : validFrom.toString();
      case "validTo" -> validTo == null ? null : validTo.toString();
      case "status" -> status == null ? null : status.name();
      case "remark" -> remark;
      case "updatedAt" -> updatedAt == null ? null : updatedAt.format(UPDATED_AT_FORMATTER);
      default -> null;
    };
  }

  private static boolean isCustomField(String field) {
    return field != null && field.startsWith("cf_");
  }

  private static Object readExtraField(Map<String, Object> extraFields, String field) {
    if (extraFields == null || extraFields.isEmpty()) {
      return null;
    }
    return extraFields.get(field);
  }

  private static void writeCell(Row row, int col, Object value) {
    Cell cell = row.createCell(col);
    if (value == null) {
      cell.setBlank();
      return;
    }
    if (value instanceof BigDecimal decimal) {
      cell.setCellValue(decimal.doubleValue());
      return;
    }
    if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
      return;
    }
    cell.setCellValue(String.valueOf(value));
  }
}
