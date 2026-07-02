package com.furuiduo.quote.quote.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class QuoteDateTimes {

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private QuoteDateTimes() {}

  public static String format(LocalDateTime value) {
    return value == null ? null : value.format(FORMATTER);
  }
}
