package com.furuiduo.quote.cost.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import com.furuiduo.quote.cost.dto.CostImportResult;

public final class CostExcelSupport {

  private CostExcelSupport() {}

  public static String cellString(Cell cell) {
    if (cell == null || cell.getCellType() == CellType.BLANK) {
      return "";
    }
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      type = cell.getCachedFormulaResultType();
    }
    return switch (type) {
      case NUMERIC -> formatNumeric(cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case STRING -> cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
      case BLANK, _NONE, ERROR -> "";
      case FORMULA -> ""; // unreachable after unwrap
    };
  }

  public static BigDecimal cellDecimal(Cell cell) {
    if (cell == null || cell.getCellType() == CellType.BLANK) {
      return null;
    }
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      type = cell.getCachedFormulaResultType();
    }
    if (type == CellType.NUMERIC) {
      return BigDecimal.valueOf(cell.getNumericCellValue());
    }
    if (type == CellType.BOOLEAN) {
      return cell.getBooleanCellValue() ? BigDecimal.ONE : BigDecimal.ZERO;
    }
    String text = cellString(cell);
    if (text.isBlank()) {
      return null;
    }
    return new BigDecimal(text.replace(",", ""));
  }

  private static String formatNumeric(double value) {
    if (value == Math.floor(value) && !Double.isInfinite(value)) {
      return String.valueOf((long) value);
    }
    return String.valueOf(value);
  }

  public static Map<String, Integer> readHeaderMap(Row headerRow) {
    Map<String, Integer> map = new LinkedHashMap<>();
    if (headerRow == null) {
      return map;
    }
    for (int i = 0; i < headerRow.getLastCellNum(); i++) {
      Cell cell = headerRow.getCell(i);
      if (cell == null) {
        continue;
      }
      String key = normalizeHeader(cellString(cell));
      if (!key.isBlank()) {
        map.put(key, i);
      }
    }
    return map;
  }

  public static String normalizeHeader(String header) {
    return header
        .replace('*', ' ')
        .replaceAll("\\s+", " ")
        .trim()
        .toUpperCase(Locale.ROOT);
  }

  public static int findColumn(Map<String, Integer> headers, String... candidates) {
    for (String candidate : candidates) {
      String key = normalizeHeader(candidate);
      Integer exact = headers.get(key);
      if (exact != null) {
        return exact;
      }
    }
    for (String candidate : candidates) {
      String key = normalizeHeader(candidate);
      for (Map.Entry<String, Integer> entry : headers.entrySet()) {
        if (entry.getKey().contains(key)) {
          return entry.getValue();
        }
      }
    }
    for (String candidate : candidates) {
      String key = normalizeHeader(candidate);
      for (Map.Entry<String, Integer> entry : headers.entrySet()) {
        String header = entry.getKey();
        if (header.length() >= 4 && key.contains(header)) {
          return entry.getValue();
        }
      }
    }
    return -1;
  }

  public static String readByHeader(Row row, Map<String, Integer> headers, String... candidates) {
    int index = findColumn(headers, candidates);
    if (index < 0) {
      return "";
    }
    return cellString(row.getCell(index));
  }

  public static BigDecimal readDecimalByHeader(
      Row row, Map<String, Integer> headers, String... candidates) {
    int index = findColumn(headers, candidates);
    if (index < 0) {
      return null;
    }
    return cellDecimal(row.getCell(index));
  }

  public static byte[] writeWorkbook(Workbook workbook) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to write workbook", ex);
    }
  }

  public static <T> CostImportResult importRows(
      MultipartFile file,
      String[] headerRow,
      Function<Row, T> rowMapper,
      Function<T, String> validator,
      BiConsumer<Integer, T> saver)
      throws IOException {
    List<String> errors = new ArrayList<>();
    int imported = 0;
    int failed = 0;

    try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
      Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
      if (sheet == null) {
        return new CostImportResult(0, 0, List.of("Excel 工作表为空"));
      }
      int headerRows = detectHeaderRowCount(sheet);
      int startRow = headerRows;

      for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }
        // 数据行号从 1 起，不含表头
        int dataRowNo = i - startRow + 1;
        try {
          T mapped = rowMapper.apply(row);
          if (mapped == null) {
            continue;
          }
          String validationError = validator.apply(mapped);
          if (validationError != null && !validationError.isBlank()) {
            failed++;
            errors.add("第 " + dataRowNo + " 行: " + validationError);
            continue;
          }
          saver.accept(dataRowNo, mapped);
          imported++;
        } catch (Exception ex) {
          failed++;
          errors.add("第 " + dataRowNo + " 行: " + ex.getMessage());
        }
      }
    }

    if (errors.size() > 20) {
      errors = new ArrayList<>(errors.subList(0, 20));
      errors.add("...");
    }
    return new CostImportResult(imported, failed, errors);
  }

  /**
   * Detect how many leading header rows to skip. Supports single-row headers and nested
   * sea/fumigation two-row headers.
   */
  public static int detectHeaderRowCount(Sheet sheet) {
    Row row0 = sheet.getRow(0);
    if (row0 == null) {
      return 0;
    }
    Map<String, Integer> headers = readHeaderMap(row0);
    if (headers.isEmpty()) {
      return 0;
    }
    if (headers.containsKey(normalizeHeader("FM-OUTDOOR"))
        || headers.containsKey(normalizeHeader("FM-INDOOR"))
        || headers.containsKey(normalizeHeader("附加费"))) {
      return 2;
    }
    Row row1 = sheet.getRow(1);
    if (row1 == null) {
      return 1;
    }
    int headerLike = 0;
    int lastCell = Math.max(row1.getLastCellNum(), 0);
    for (int i = 0; i < lastCell; i++) {
      String text = cellString(row1.getCell(i)).trim().toUpperCase(Locale.ROOT);
      if (text.isBlank()) {
        continue;
      }
      if (text.equals("NON OAK")
          || text.equals("OAK")
          || text.equals("VALIDITY")
          || text.equals("BUC")
          || text.equals("EBS")
          || text.equals("GRI")
          || text.equals("OTHERS")
          || text.equals("有效期")) {
        headerLike++;
      }
    }
    return headerLike >= 2 ? 2 : 1;
  }

  public static void writeHeaderRow(Sheet sheet, String[] headers) {
    Row row = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      row.createCell(i).setCellValue(headers[i]);
    }
  }
}
