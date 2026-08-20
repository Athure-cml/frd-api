package com.furuiduo.quote.cost.support;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.DateUtil;

/** 成本库列表/导出：按生效期、有效期做日期比较搜索。 */
public final class CostDateSearchFilter {

  private static final Pattern NUMERIC_TEXT = Pattern.compile("^\\d+(\\.\\d+)?$");

  private CostDateSearchFilter() {}

  /** 生效期搜索：记录生效日 &gt;= 搜索日；未填搜索条件则不过滤。 */
  public static boolean matchesEffFrom(String recordEff, String searchFrom) {
    if (isBlank(searchFrom)) {
      return true;
    }
    LocalDate from = CostValidityStatus.tryParseDateForSearch(searchFrom);
    if (from == null) {
      return true;
    }
    LocalDate eff = parseRecordDate(recordEff);
    if (eff == null) {
      return false;
    }
    return !eff.isBefore(from);
  }

  /** 有效期搜索：记录有效日 &lt;= 搜索日；未填搜索条件则不过滤。 */
  public static boolean matchesValidTo(String recordValid, String searchTo) {
    if (isBlank(searchTo)) {
      return true;
    }
    LocalDate to = CostValidityStatus.tryParseDateForSearch(searchTo);
    if (to == null) {
      return true;
    }
    LocalDate valid = parseRecordDate(recordValid);
    if (valid == null) {
      return false;
    }
    return !valid.isAfter(to);
  }

  /** 同时填生效期与有效期：区间内（生效 &gt;= 起，有效 &lt;= 止）。 */
  public static boolean matchesRange(
      String recordEff, String recordValid, String searchFrom, String searchTo) {
    return matchesEffFrom(recordEff, searchFrom) && matchesValidTo(recordValid, searchTo);
  }

  /** 海运运费生效期：extraFields 优先，否则取 freightValidDate 区间起始日。 */
  public static String resolveSeaFreightEff(
      Map<String, Object> extraFields, String freightValidDate) {
    String eff =
        readExtraText(extraFields, "cf_sea_freight_eff", "cf_seaFreightEff");
    if (!isBlank(eff)) {
      return eff;
    }
    if (isBlank(freightValidDate)) {
      return null;
    }
    String text = freightValidDate.trim();
    if (RANGE_LIKE.matcher(text).matches()) {
      int split = findRangeSplit(text);
      if (split > 0) {
        return text.substring(0, split).trim();
      }
    }
    return null;
  }

  public static String readExtraText(Map<String, Object> extra, String... keys) {
    if (extra == null || keys == null) {
      return null;
    }
    for (String key : keys) {
      if (key == null || key.isBlank()) {
        continue;
      }
      Object value = extra.get(key);
      String text = normalizeStoredDateText(value);
      if (!isBlank(text)) {
        return text;
      }
    }
    return null;
  }

  static String normalizeStoredDateText(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return excelSerialToIsoDate(number.doubleValue());
    }
    String text = String.valueOf(value).trim();
    if (text.isEmpty()) {
      return null;
    }
    if (NUMERIC_TEXT.matcher(text).matches()) {
      try {
        return excelSerialToIsoDate(Double.parseDouble(text));
      } catch (NumberFormatException ignored) {
        // keep original text
      }
    }
    return text;
  }

  private static LocalDate parseRecordDate(String raw) {
    if (isBlank(raw)) {
      return null;
    }
    String text = normalizeStoredDateText(raw);
    if (isBlank(text)) {
      return null;
    }
    LocalDate parsed = CostValidityStatus.tryParseDateForSearch(text);
    if (parsed != null) {
      return parsed;
    }
    return CostValidityStatus.tryParseRangeStart(text);
  }

  private static String excelSerialToIsoDate(double serial) {
    if (serial <= 0 || serial >= 100_000) {
      return null;
    }
    try {
      Date date = DateUtil.getJavaDate(serial);
      LocalDate local =
          date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      return local.format(DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static final Pattern RANGE_LIKE =
      Pattern.compile("^.+\\s*[-–—~至到]\\s*.+$");

  private static int findRangeSplit(String text) {
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '-' || ch == '–' || ch == '—' || ch == '~' || ch == '至' || ch == '到') {
        if (i > 0 && i < text.length() - 1) {
          return i;
        }
      }
    }
    return -1;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
