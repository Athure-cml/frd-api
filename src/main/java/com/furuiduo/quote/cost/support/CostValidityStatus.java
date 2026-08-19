package com.furuiduo.quote.cost.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.furuiduo.quote.cost.entity.CostStatus;

/** 根据有效期文本刷新成本状态：过期标 expired；否则 active。 */
public final class CostValidityStatus {

  private static final Pattern RANGE =
      Pattern.compile(
          "^(\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2})\\s*[-–—~至到]\\s*(\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2})$");

  private static final DateTimeFormatter EXPORT_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd");

  private static final Pattern US_SLASH_DATE =
      Pattern.compile("^(\\d{1,2})/(\\d{1,2})/(\\d{2,4})$");

  private static final DateTimeFormatter[] FORMATTERS =
      new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("yyyy-M-d"),
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy.M.d"),
        DateTimeFormatter.ISO_LOCAL_DATE
      };

  private CostValidityStatus() {}

  public static CostStatus resolve(CostStatus current, String... validityTexts) {
    // 成本库不再使用草稿；历史 draft 按有效期重算为 active/expired
    LocalDate end = latestEndDate(validityTexts);
    if (end != null && end.isBefore(LocalDate.now())) {
      return CostStatus.expired;
    }
    return CostStatus.active;
  }

  /** 列表/导出筛选：空则不过滤；按「有效期推算后的状态」匹配。 */
  public static boolean matchesFilter(
      CostStatus current, String statusFilter, String... validityTexts) {
    if (statusFilter == null || statusFilter.isBlank()) {
      return true;
    }
    CostStatus expected;
    try {
      expected = CostStatus.valueOf(statusFilter.trim().toLowerCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return false;
    }
    return resolve(current, validityTexts) == expected;
  }

  private static LocalDate latestEndDate(String... validityTexts) {
    LocalDate latest = null;
    if (validityTexts == null) {
      return null;
    }
    for (String text : validityTexts) {
      LocalDate end = parseEndDate(text);
      if (end == null) {
        continue;
      }
      if (latest == null || end.isAfter(latest)) {
        latest = end;
      }
    }
    return latest;
  }

  private static LocalDate parseEndDate(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String text = raw.trim();
    Matcher range = RANGE.matcher(text);
    if (range.matches()) {
      return parseSingleDate(range.group(2));
    }
    return parseSingleDate(text);
  }

  /** 导出 Excel：统一格式化为 yyyy/MM/dd；区间用「 - 」连接。 */
  public static String formatExportDate(String raw) {
    if (raw == null || raw.isBlank()) {
      return raw;
    }
    String text = raw.trim();
    Matcher range = RANGE.matcher(text);
    if (range.matches()) {
      String start = formatSingleExportDate(range.group(1));
      String end = formatSingleExportDate(range.group(2));
      if (start != null && end != null) {
        return start + " - " + end;
      }
      return text;
    }
    String single = formatSingleExportDate(text);
    return single != null ? single : text;
  }

  private static String formatSingleExportDate(String text) {
    LocalDate date = parseSingleDate(text);
    return date == null ? null : date.format(EXPORT_DATE_FORMATTER);
  }

  /** 解析单日或区间结束日；解析失败返回 null。 */
  public static LocalDate tryParseDate(String raw) {
    return parseEndDate(raw);
  }

  private static LocalDate parseSingleDate(String text) {
    String normalized = text.trim().replace('.', '-');
    for (DateTimeFormatter formatter : FORMATTERS) {
      try {
        return LocalDate.parse(normalized, formatter);
      } catch (DateTimeParseException ignored) {
        // try next
      }
    }
    try {
      return LocalDate.parse(
          normalized.toLowerCase(Locale.ROOT), DateTimeFormatter.ofPattern("yyyy/M/d"));
    } catch (DateTimeParseException ignored) {
      // try US slash
    }
    return parseUsSlashDate(text.trim());
  }

  /** 兼容 Excel 美式短日期：8/1/26、08/01/2026。 */
  private static LocalDate parseUsSlashDate(String text) {
    Matcher matcher = US_SLASH_DATE.matcher(text);
    if (!matcher.matches()) {
      return null;
    }
    int month = Integer.parseInt(matcher.group(1));
    int day = Integer.parseInt(matcher.group(2));
    int year = Integer.parseInt(matcher.group(3));
    if (year < 100) {
      year += 2000;
    }
    try {
      return LocalDate.of(year, month, day);
    } catch (RuntimeException ignored) {
      return null;
    }
  }
}
