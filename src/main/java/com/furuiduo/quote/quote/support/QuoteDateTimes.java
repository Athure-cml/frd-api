package com.furuiduo.quote.quote.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class QuoteDateTimes {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private QuoteDateTimes() {}

  public static String format(LocalDateTime value) {
    return value == null ? null : value.format(FORMATTER);
  }

  public static String formatDate(LocalDateTime value) {
    return value == null ? null : value.format(DATE_FORMATTER);
  }
}
