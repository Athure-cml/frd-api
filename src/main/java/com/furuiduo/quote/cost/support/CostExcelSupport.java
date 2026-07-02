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
    if (cell == null) {
      return "";
    }
    if (cell.getCellType() == CellType.NUMERIC) {
      double value = cell.getNumericCellValue();
      if (value == Math.floor(value)) {
        return String.valueOf((long) value);
      }
      return String.valueOf(value);
    }
    if (cell.getCellType() == CellType.BOOLEAN) {
      return String.valueOf(cell.getBooleanCellValue());
    }
    return cell.getStringCellValue().trim();
  }

  public static BigDecimal cellDecimal(Cell cell) {
    if (cell == null || cell.getCellType() == CellType.BLANK) {
      return null;
    }
    if (cell.getCellType() == CellType.NUMERIC) {
      return BigDecimal.valueOf(cell.getNumericCellValue());
    }
    String text = cellString(cell);
    if (text.isBlank()) {
      return null;
    }
    return new BigDecimal(text.replace(",", ""));
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
      int startRow = 0;
      Row first = sheet.getRow(0);
      Map<String, Integer> headerMap = readHeaderMap(first);
      if (headerMap.isEmpty()) {
        startRow = 0;
      } else {
        startRow = 1;
      }

      for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }
        try {
          T mapped = rowMapper.apply(row);
          if (mapped == null) {
            continue;
          }
          String validationError = validator.apply(mapped);
          if (validationError != null && !validationError.isBlank()) {
            failed++;
            errors.add("第 " + (i + 1) + " 行: " + validationError);
            continue;
          }
          saver.accept(i + 1, mapped);
          imported++;
        } catch (Exception ex) {
          failed++;
          errors.add("第 " + (i + 1) + " 行: " + ex.getMessage());
        }
      }
    }

    if (errors.size() > 20) {
      errors = new ArrayList<>(errors.subList(0, 20));
      errors.add("...");
    }
    return new CostImportResult(imported, failed, errors);
  }

  public static void writeHeaderRow(Sheet sheet, String[] headers) {
    Row row = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      row.createCell(i).setCellValue(headers[i]);
    }
  }
}
