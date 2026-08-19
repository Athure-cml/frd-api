package com.furuiduo.quote.masterdata.dto;

import java.util.List;

/**
 * city+state → zip 解析结果。
 *
 * <p>status: unique | ambiguous | notFound | skipped
 *
 * <p>canonicalCity：主数据中规范城市名（大小写与系统一致），匹配到则返回。
 */
public record DestZipResolveItemResponse(
    String city,
    String state,
    String status,
    String zipCode,
    List<String> candidates,
    String message,
    String canonicalCity) {

  public static DestZipResolveItemResponse skipped(
      String city, String state, String message, String canonicalCity) {
    return new DestZipResolveItemResponse(
        city, state, "skipped", null, List.of(), message, canonicalCity);
  }

  public static DestZipResolveItemResponse skipped(String city, String state, String message) {
    return skipped(city, state, message, null);
  }

  public static DestZipResolveItemResponse unique(
      String city, String state, String zipCode, String canonicalCity) {
    return new DestZipResolveItemResponse(
        city, state, "unique", zipCode, List.of(zipCode), null, canonicalCity);
  }

  public static DestZipResolveItemResponse unique(String city, String state, String zipCode) {
    return unique(city, state, zipCode, null);
  }

  public static DestZipResolveItemResponse ambiguous(
      String city, String state, List<String> candidates, String canonicalCity) {
    return new DestZipResolveItemResponse(
        city,
        state,
        "ambiguous",
        null,
        candidates,
        "City+State 对应多个邮编，请填写邮编",
        canonicalCity);
  }

  public static DestZipResolveItemResponse ambiguous(
      String city, String state, List<String> candidates) {
    return ambiguous(city, state, candidates, null);
  }

  public static DestZipResolveItemResponse notFound(String city, String state) {
    return new DestZipResolveItemResponse(
        city, state, "notFound", null, List.of(), "主数据中未找到 City+State 对应的邮编", null);
  }
}
