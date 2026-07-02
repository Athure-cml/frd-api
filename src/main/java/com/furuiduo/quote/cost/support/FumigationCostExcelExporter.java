package com.furuiduo.quote.cost.support;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

  private static final DateTimeFormatter UPDATED_AT_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private static final byte[] HEADER_BLUE = new byte[] {(byte) 0x1D, (byte) 0x4E, (byte) 0x7B};
  private static final byte[] HEADER_YELLOW = new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0x00};

  private FumigationCostExcelExporter() {}

  public static byte[] export(List<CostFumigation> items) {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("fumigation");
      writeNestedHeaders(workbook, sheet);
      int rowIndex = 2;
      for (CostFumigation item : items) {
        Row row = sheet.createRow(rowIndex++);
        int col = 0;
        setText(row, col++, item.getPort());
        setText(row, col++, item.getStation());
        setDecimal(row, col++, item.getNonOakOutdoor());
        setDecimal(row, col++, item.getNonOakIndoor());
        setText(row, col++, item.getNonOakQuoteSummer());
        setText(row, col++, item.getNonOakQuoteWinter());
        setDecimal(row, col++, item.getOakOutdoor());
        setDecimal(row, col++, item.getOakIndoor());
        setText(row, col++, item.getOakQuoteSummer());
        setText(row, col++, item.getOakQuoteWinter());
        setText(row, col++, item.getRemark());
        if (item.getUpdatedAt() != null) {
          setText(row, col, UPDATED_AT_FORMATTER.format(item.getUpdatedAt()));
        }
      }
      for (int i = 0; i < 12; i++) {
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

    createMergedHeader(sheet, row0, 0, 0, 1, 0, "PORT", blueStyle);
    createMergedHeader(sheet, row0, 1, 1, 1, 1, "STATION", blueStyle);
    createMergedHeader(sheet, row0, 2, 5, 0, 2, "NON-OAK", blueStyle);
    createMergedHeader(sheet, row0, 6, 9, 0, 6, "OAK", blueStyle);
    createMergedHeader(sheet, row0, 10, 10, 0, 10, "备注", blueStyle);
    createMergedHeader(sheet, row0, 11, 11, 0, 11, "更新时间", blueStyle);

    String[] subHeaders = {
      "OUTDOOR", "IN DOOR", "报价(夏季)", "报价(冬季)",
      "OUTDOOR", "IN DOOR", "报价(夏季)", "报价(冬季)"
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
      int col,
      String title,
      CellStyle style) {
    Cell cell = row.createCell(col);
    cell.setCellValue(title);
    cell.setCellStyle(style);
    if (firstCol != lastCol || firstRow != row.getRowNum()) {
      sheet.addMergedRegion(new CellRangeAddress(firstRow, row.getRowNum(), firstCol, lastCol));
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
