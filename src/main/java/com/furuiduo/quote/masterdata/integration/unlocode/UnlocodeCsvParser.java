package com.furuiduo.quote.masterdata.integration.unlocode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.furuiduo.quote.masterdata.entity.PortType;

/** 解析 UNECE UN/LOCODE CSV（code-list.csv 或官方 CodeListPart*.csv）。 */
public final class UnlocodeCsvParser {

  /** UNECE 官方包无表头：Function=6、Status=7。 */
  private static final ColumnIndex UNECE_OFFICIAL_COLUMNS =
      new ColumnIndex(0, 1, 2, 3, 4, 5, 7, 6);

  private UnlocodeCsvParser() {}

  public static List<UnlocodeRecord> parse(String csv) throws IOException {
    Map<String, UnlocodeRecord> unique = new LinkedHashMap<>();
    try (BufferedReader reader = new BufferedReader(new StringReader(csv))) {
      String firstLine = reader.readLine();
      if (firstLine == null) {
        return List.of();
      }
      boolean hasHeader = hasHeaderRow(firstLine);
      ColumnIndex columns = hasHeader ? resolveColumnsFromHeader(firstLine) : UNECE_OFFICIAL_COLUMNS;

      if (!hasHeader) {
        addRecord(unique, firstLine, columns);
      }

      String line;
      while ((line = reader.readLine()) != null) {
        addRecord(unique, line, columns);
      }
    }
    return new ArrayList<>(unique.values());
  }

  private static void addRecord(
      Map<String, UnlocodeRecord> unique, String line, ColumnIndex columns) {
    if (line.isBlank() || line.startsWith("#")) {
      return;
    }
    String[] cols = splitCsvLine(line);
    UnlocodeRecord record = toRecord(cols, columns);
    if (record != null) {
      unique.put(record.code(), record);
    }
  }

  private static boolean hasHeaderRow(String line) {
    for (String header : splitCsvLine(line)) {
      String normalized = normalizeHeader(header);
      if ("country".equals(normalized)
          || "location".equals(normalized)
          || "function".equals(normalized)) {
        return true;
      }
    }
    return false;
  }

  private static UnlocodeRecord toRecord(String[] cols, ColumnIndex columns) {
    String country = get(cols, columns.country).toUpperCase(Locale.ROOT);
    if (country.length() != 2) {
      return null;
    }
    String location = get(cols, columns.location).toUpperCase(Locale.ROOT);
    if (location.isEmpty() || "XXX".equals(location)) {
      return null;
    }
    String nameWo = get(cols, columns.nameWo);
    String name = nameWo.isEmpty() ? get(cols, columns.name) : nameWo;
    if (name.isEmpty() || name.startsWith(".")) {
      return null;
    }
    String functionCode = get(cols, columns.function);
    PortType portType = UnlocodeFunctionClassifier.classify(functionCode);
    if (portType == PortType.OTHER || portType == PortType.AIRPORT) {
      return null;
    }
    String changeFlag = get(cols, columns.change);
    if (changeFlag.toUpperCase(Locale.ROOT).startsWith("X")) {
      return null;
    }
    String code = country + location;
    return new UnlocodeRecord(
        code,
        country,
        name,
        emptyToNull(get(cols, columns.subdivision)),
        emptyToNull(functionCode),
        emptyToNull(get(cols, columns.status)),
        emptyToNull(changeFlag),
        portType);
  }

  private static ColumnIndex resolveColumnsFromHeader(String headerLine) {
    String[] headers = splitCsvLine(headerLine);
    Map<String, Integer> index = new LinkedHashMap<>();
    for (int i = 0; i < headers.length; i++) {
      index.put(normalizeHeader(headers[i]), i);
    }
    return new ColumnIndex(
        index.getOrDefault("change", 0),
        index.getOrDefault("country", 1),
        index.getOrDefault("location", 2),
        index.getOrDefault("name", 3),
        index.getOrDefault("namewodiacritics", 4),
        index.getOrDefault("subdivision", 5),
        index.getOrDefault("status", 6),
        index.getOrDefault("function", 7));
  }

  private static String normalizeHeader(String header) {
    return header.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  private static String get(String[] cols, int index) {
    if (index < 0 || index >= cols.length) {
      return "";
    }
    return cols[index].trim();
  }

  static String[] splitCsvLine(String line) {
    List<String> cols = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (ch == '"') {
        inQuotes = !inQuotes;
        continue;
      }
      if (ch == ',' && !inQuotes) {
        cols.add(current.toString());
        current.setLength(0);
        continue;
      }
      current.append(ch);
    }
    cols.add(current.toString());
    return cols.toArray(String[]::new);
  }

  private static String emptyToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private record ColumnIndex(
      int change,
      int country,
      int location,
      int name,
      int nameWo,
      int subdivision,
      int status,
      int function) {}
}
