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

import com.furuiduo.quote.cost.entity.CostFumigation;

public final class FumigationCostExcelExporter {

  private static final byte[] HEADER_BLUE = new byte[] {(byte) 0x1D, (byte) 0x4E, (byte) 0x7B};
  private static final byte[] HEADER_YELLOW = new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0x00};

  private static final String CF_OUTDOOR_EFF = "cf_fum_outdoor_eff";
  private static final String CF_INDOOR_EFF = "cf_fum_indoor_eff";

  private FumigationCostExcelExporter() {}

  public static byte[] export(List<CostFumigation> items) {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("fumigation");
      writeNestedHeaders(workbook, sheet);
      int rowIndex = 2;
      for (CostFumigation item : items) {
        Row row = sheet.createRow(rowIndex++);
        int col = 0;
        setText(row, col++, item.getRegion());
        setText(row, col++, item.getStation());
        setDecimal(row, col++, item.getOutdoorNonOak());
        setDecimal(row, col++, item.getOutdoorOak());
        setText(row, col++, readExtra(item, CF_OUTDOOR_EFF));
        setText(row, col++, item.getOutdoorValidity());
        setDecimal(row, col++, item.getIndoorNonOak());
        setDecimal(row, col++, item.getIndoorOak());
        setText(row, col++, readExtra(item, CF_INDOOR_EFF));
        setText(row, col++, item.getIndoorValidity());
        setText(row, col, item.getAddress());
      }
      for (int i = 0; i < 11; i++) {
        sheet.autoSizeColumn(i);
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("导出失败", ex);
    }
  }

  private static void writeNestedHeaders(XSSFWorkbook workbook, Sheet sheet) {
    CellStyle blueStyle = createHeaderStyle(workbook, HEADER_BLUE, IndexedColors.WHITE.getIndex());
    CellStyle yellowStyle =
        createHeaderStyle(workbook, HEADER_YELLOW, IndexedColors.BLACK.getIndex());

    Row row0 = sheet.createRow(0);
    Row row1 = sheet.createRow(1);

    createMergedHeader(sheet, row0, 0, 0, 1, 0, "REGION", blueStyle);
    createMergedHeader(sheet, row0, 1, 1, 1, 1, "STATION", blueStyle);
    createMergedHeader(sheet, row0, 2, 5, 0, 2, "FM-OUTDOOR", blueStyle);
    createMergedHeader(sheet, row0, 6, 9, 0, 6, "FM-INDOOR", blueStyle);
    createMergedHeader(sheet, row0, 10, 10, 1, 10, "ADDRESS", blueStyle);

    String[] subHeaders = {
      "NON OAK", "OAK", "生效期", "VALIDITY", "NON OAK", "OAK", "生效期", "VALIDITY"
    };
    CellStyle[] subStyles = {
      blueStyle, blueStyle, yellowStyle, yellowStyle,
      blueStyle, blueStyle, yellowStyle, yellowStyle
    };
    for (int i = 0; i < subHeaders.length; i++) {
      Cell cell = row1.createCell(2 + i);
      cell.setCellValue(subHeaders[i]);
      cell.setCellStyle(subStyles[i]);
    }
  }

  private static void createMergedHeader(
      Sheet sheet,
      Row row,
      int firstCol,
      int lastCol,
      int firstRow,
      int lastRow,
      String title,
      CellStyle style) {
    Cell cell = row.createCell(firstCol);
    cell.setCellValue(title);
    cell.setCellStyle(style);
    if (firstCol != lastCol || firstRow != lastRow) {
      sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
    }
    for (int c = firstCol + 1; c <= lastCol; c++) {
      Cell filler = row.createCell(c);
      filler.setCellStyle(style);
    }
  }

  private static CellStyle createHeaderStyle(
      XSSFWorkbook workbook, byte[] rgb, short fontColor) {
    CellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(rgb, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    Font font = workbook.createFont();
    font.setBold(true);
    font.setColor(fontColor);
    style.setFont(font);
    return style;
  }

  private static String readExtra(CostFumigation item, String key) {
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
