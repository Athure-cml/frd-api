package com.furuiduo.quote.masterdata.dto;

import java.util.List;

/**
 * city+state → zip 解析结果。
 *
 * <p>status: unique | ambiguous | notFound | skipped
 */
public record DestZipResolveItemResponse(
    String city,
    String state,
    String status,
    String zipCode,
    List<String> candidates,
    String message) {

  public static DestZipResolveItemResponse skipped(String city, String state, String message) {
    return new DestZipResolveItemResponse(city, state, "skipped", null, List.of(), message);
  }

  public static DestZipResolveItemResponse unique(String city, String state, String zipCode) {
    return new DestZipResolveItemResponse(city, state, "unique", zipCode, List.of(zipCode), null);
  }

  public static DestZipResolveItemResponse ambiguous(
      String city, String state, List<String> candidates) {
    return new DestZipResolveItemResponse(
        city,
        state,
        "ambiguous",
        null,
        candidates,
        "City+State 对应多个邮编，请填写邮编");
  }

  public static DestZipResolveItemResponse notFound(String city, String state) {
    return new DestZipResolveItemResponse(
        city, state, "notFound", null, List.of(), "主数据中未找到 City+State 对应的邮编");
  }
}
