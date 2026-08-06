package com.furuiduo.quote.cost.support;

import java.io.IOException;
import java.math.BigDecimal;
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

import com.furuiduo.quote.cost.entity.CostSea;

public final class SeaCostExcelExporter {

  private static final byte[] HEADER_YELLOW = new byte[] {(byte) 0xFF, (byte) 0xC0, (byte) 0x00};
  private static final byte[] HEADER_GRAY = new byte[] {(byte) 0xBF, (byte) 0xBF, (byte) 0xBF};
  private static final byte[] HEADER_BLUE = new byte[] {(byte) 0x9B, (byte) 0xC2, (byte) 0xE6};

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
        setText(row, col++, item.getCnShortName());
        setText(row, col++, item.getEnProductName());
        setText(row, col++, item.getContainerType());
        setDecimal(row, col++, item.getFreight());
        setText(row, col++, item.getFreightValidDate());
        setDecimal(row, col++, item.getBuc());
        setText(row, col++, item.getBucValidDate());
        setDecimal(row, col++, item.getEbs());
        setText(row, col++, item.getEbsValidDate());
        setDecimal(row, col++, item.getGri());
        setText(row, col++, item.getGriValidDate());
        setDecimal(row, col++, item.getOthers());
        setText(row, col++, item.getOthersValidDate());
        setDecimal(row, col++, item.getAllIn());
        setText(row, col++, item.getSsl());
        setText(row, col++, item.getAgent());
        setText(row, col, item.getRemark());
      }
      for (int i = 0; i < 20; i++) {
        sheet.autoSizeColumn(i);
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("导出失败", ex);
    }
  }

  private static void writeNestedHeaders(XSSFWorkbook workbook, Sheet sheet) {
    CellStyle yellowStyle =
        createHeaderStyle(workbook, HEADER_YELLOW, IndexedColors.BLACK.getIndex());
    CellStyle grayStyle = createHeaderStyle(workbook, HEADER_GRAY, IndexedColors.BLACK.getIndex());
    CellStyle blueStyle = createHeaderStyle(workbook, HEADER_BLUE, IndexedColors.BLACK.getIndex());

    Row row0 = sheet.createRow(0);
    Row row1 = sheet.createRow(1);

    String[] topSingles = {
      "POR", "POL", "POD", "中文简称", "英文品名", "箱型", "运费", "有效期"
    };
    for (int i = 0; i < topSingles.length; i++) {
      createMergedHeader(sheet, row0, i, i, 1, i, topSingles[i], yellowStyle);
    }

    createMergedHeader(sheet, row0, 8, 15, 0, 8, "附加费", grayStyle);
    String[] surchargeSubs = {
      "BUC", "有效期", "EBS", "有效期", "GRI", "有效期", "OTHERS", "有效期"
    };
    for (int i = 0; i < surchargeSubs.length; i++) {
      Cell cell = row1.createCell(8 + i);
      cell.setCellValue(surchargeSubs[i]);
      cell.setCellStyle(grayStyle);
    }

    String[] tail = {"ALL IN (小计)", "SSL (船公司)", "AGENT (代理)", "REMARK 备注"};
    for (int i = 0; i < tail.length; i++) {
      createMergedHeader(sheet, row0, 16 + i, 16 + i, 1, 16 + i, tail[i], blueStyle);
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
    // POI 要求 firstRow <= lastRow；原先把「跨到第 1 行」写成 (1,0) 会直接抛错 → 导出 500
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
