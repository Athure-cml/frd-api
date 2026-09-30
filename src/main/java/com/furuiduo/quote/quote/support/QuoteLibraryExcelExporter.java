package com.furuiduo.quote.quote.support;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.furuiduo.quote.cost.dto.CostExportColumn;
import com.furuiduo.quote.cost.support.CostExcelSupport;

public final class QuoteLibraryExcelExporter {

  @FunctionalInterface
  public interface ValueReader {
    Object read(Object row, String field);
  }

  private QuoteLibraryExcelExporter() {}

  public static byte[] export(
      String sheetName, List<?> rows, List<CostExportColumn> columns, ValueReader reader) {
    if (columns == null || columns.isEmpty()) {
      throw new IllegalArgumentException("导出列不能为空");
    }
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet(sheetName);
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < columns.size(); i++) {
        headerRow.createCell(i).setCellValue(columns.get(i).header());
      }
      int rowIndex = 1;
      for (Object item : rows) {
        Row row = sheet.createRow(rowIndex++);
        for (int col = 0; col < columns.size(); col++) {
          String field = columns.get(col).field();
          Object value = QuoteLibraryResponseReaders.formatExportValue(field, reader.read(item, field));
          writeCell(row, col, value);
        }
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new IllegalStateException("导出失败", ex);
    }
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
