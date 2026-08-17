package com.furuiduo.quote.cost.support;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.furuiduo.quote.cost.entity.CostSea;

/** 海运成本导出：表头对齐业务 Excel 底层模板 */
public final class SeaCostExcelExporter {

  private static final byte[] HEADER_ORANGE = new byte[] {(byte) 0xFF, (byte) 0xC0, (byte) 0x00};
  private static final byte[] HEADER_GRAY = new byte[] {(byte) 0xBF, (byte) 0xBF, (byte) 0xBF};
  private static final byte[] HEADER_GREEN = new byte[] {(byte) 0xC6, (byte) 0xEF, (byte) 0xCE};
  private static final byte[] HEADER_BLUE = new byte[] {(byte) 0x9B, (byte) 0xC2, (byte) 0xE6};

  private static final String CF_FREIGHT_EFF = "cf_sea_freight_eff";
  private static final String CF_BUNKER_EFF = "cf_sea_bunker_eff";
  private static final String CF_OTHERS_EFF = "cf_sea_others_eff";

  private SeaCostExcelExporter() {}

  public static byte[] export(List<CostSea> items) {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("sea");
      writeNestedHeaders(workbook, sheet);
      int rowIndex = 2;
      for (CostSea item : items) {
        Row row = sheet.createRow(rowIndex++);
        int col = 0;
        setText(row, col++, item.getPor());
        setText(row, col++, item.getPol());
        setText(row, col++, item.getPod());
        setDecimal(row, col++, item.getFreight());
        setText(row, col++, item.getContainerType());
        setText(row, col++, readExtra(item, CF_FREIGHT_EFF));
        setText(row, col++, item.getFreightValidDate());
        setDecimal(row, col++, item.getBuc());
        setText(row, col++, readExtra(item, CF_BUNKER_EFF));
        setText(row, col++, item.getBucValidDate());
        setDecimal(row, col++, item.getOthers());
        setText(row, col++, readExtra(item, CF_OTHERS_EFF));
        setText(row, col++, item.getOthersValidDate());
        setDecimal(row, col++, item.getAllIn());
        setText(row, col++, item.getSsl());
        setText(row, col++, item.getAgent());
        setText(row, col++, item.getRemark());
        setText(row, col, item.getEnProductName());
      }
      for (int i = 0; i < 18; i++) {
        sheet.autoSizeColumn(i);
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("导出失败", ex);
    }
  }

  private static void writeNestedHeaders(XSSFWorkbook workbook, Sheet sheet) {
    CellStyle orangeStyle =
        createHeaderStyle(workbook, HEADER_ORANGE, IndexedColors.BLACK.getIndex());
    CellStyle grayStyle = createHeaderStyle(workbook, HEADER_GRAY, IndexedColors.BLACK.getIndex());
    CellStyle greenStyle =
        createHeaderStyle(workbook, HEADER_GREEN, IndexedColors.BLACK.getIndex());
    CellStyle blueStyle = createHeaderStyle(workbook, HEADER_BLUE, IndexedColors.BLACK.getIndex());

    Row row0 = sheet.createRow(0);
    Row row1 = sheet.createRow(1);

    String[] orange = {"POR", "POL", "POD", "运费", "箱型", "生效期", "有效期"};
    for (int i = 0; i < orange.length; i++) {
      createMergedHeader(sheet, row0, i, i, 1, i, orange[i], orangeStyle);
    }

    createMergedHeader(sheet, row0, 7, 12, 0, 7, "附加费", grayStyle);
    String[] surchargeSubs = {"燃油附加费", "生效期", "有效期", "OTHERS", "生效期", "有效期"};
    for (int i = 0; i < surchargeSubs.length; i++) {
      Cell cell = row1.createCell(7 + i);
      cell.setCellValue(surchargeSubs[i]);
      cell.setCellStyle(grayStyle);
    }

    createMergedHeader(sheet, row0, 13, 13, 1, 13, "ALL IN (小计)", greenStyle);

    String[] blue = {"SSL (船公司)", "AGENT (代理)", "REMARK 备注", "英文品名"};
    for (int i = 0; i < blue.length; i++) {
      createMergedHeader(sheet, row0, 14 + i, 14 + i, 1, 14 + i, blue[i], blueStyle);
    }
  }

  private static void createMergedHeader(
      Sheet sheet,
      Row row,
      int firstCol,
      int lastCol,
      int otherRow,
      int col,
      String title,
      CellStyle style) {
    Cell cell = row.createCell(col);
    cell.setCellValue(title);
    cell.setCellStyle(style);
    int rowNum = row.getRowNum();
    if (firstCol != lastCol || otherRow != rowNum) {
      sheet.addMergedRegion(
          new CellRangeAddress(
              Math.min(rowNum, otherRow),
              Math.max(rowNum, otherRow),
              firstCol,
              lastCol));
    }
  }

  private static CellStyle createHeaderStyle(
      XSSFWorkbook workbook, byte[] rgb, short fontColor) {
    CellStyle style = workbook.createCellStyle();
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setFillForegroundColor(new XSSFColor(rgb, null));
    style.setAlignment(HorizontalAlignment.CENTER);
    Font font = workbook.createFont();
    font.setBold(true);
    font.setColor(fontColor);
    style.setFont(font);
    return style;
  }

  private static String readExtra(CostSea item, String key) {
    Map<String, Object> extra = item.getExtraFields();
    if (extra == null || extra.isEmpty()) {
      return null;
    }
    Object value = extra.get(key);
    return value == null ? null : String.valueOf(value);
  }

  private static void setText(Row row, int col, String value) {
    Cell cell = row.createCell(col);
    if (value != null && !value.isBlank()) {
      cell.setCellValue(value);
    } else {
      cell.setBlank();
    }
  }

  private static void setDecimal(Row row, int col, BigDecimal value) {
    Cell cell = row.createCell(col);
    if (value != null) {
      cell.setCellValue(value.doubleValue());
    } else {
      cell.setBlank();
    }
  }
}
