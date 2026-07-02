package com.furuiduo.quote.masterdata.integration.freightutils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * FreightUtils 开放 API 客户端（UN/LOCODE 港口数据）。
 *
 * @see <a href="https://www.freightutils.com/api-docs">FreightUtils API</a>
 */
@Component
public class FreightUtilsClient {

  private static final Logger log = LoggerFactory.getLogger(FreightUtilsClient.class);
  private static final int MAX_LIMIT = 100;
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  /** 全球主流海运国家/地区（按 UN/LOCODE 国家码批量拉取）。 */
  public static final List<String> MAINSTREAM_COUNTRIES =
      List.of(
          "CN", "HK", "TW", "MO",
          "JP", "KR",
          "SG", "MY", "TH", "VN", "ID", "PH", "MM", "KH", "LA", "BN",
          "IN", "PK", "BD", "LK",
          "AE", "SA", "OM", "QA", "KW", "BH", "IQ", "IL", "JO",
          "EG", "ZA", "NG", "KE", "MA", "TZ",
          "US", "CA", "MX", "PA",
          "BR", "AR", "CL", "CO", "PE", "EC", "UY", "VE",
          "NL", "BE", "DE", "FR", "GB", "IE", "ES", "PT", "IT", "GR", "TR",
          "PL", "RU", "FI", "SE", "NO", "DK", "LT", "LV", "EE",
          "AU", "NZ", "FJ", "PG");

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final String apiKey;
  private final Duration requestDelay;

  public FreightUtilsClient(
      ObjectMapper objectMapper,
      @Value("${quote.masterdata.global-port.freightutils.base-url:https://www.freightutils.com/api}")
          String baseUrl,
      @Value("${quote.masterdata.global-port.freightutils.api-key:}") String apiKey,
      @Value("${quote.masterdata.global-port.freightutils.request-delay-ms:300}")
          long requestDelayMs) {
    this.objectMapper = objectMapper;
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.requestDelay =
        requestDelayMs > 0 ? Duration.ofMillis(requestDelayMs) : Duration.ZERO;
    this.httpClient =
        HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
  }

  /** 按国家拉取海港（function=port，每国最多 100 条）。 */
  public List<FreightUnlocodePort> fetchPortsByCountry(String countryCode) {
    return fetchPortsByCountry(countryCode, MAX_LIMIT);
  }

  public List<FreightUnlocodePort> fetchPortsByCountry(String countryCode, int limit) {
    int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
    String query =
        "country="
            + encode(countryCode.toUpperCase())
            + "&function=port&limit="
            + safeLimit;
    FreightUnlocodeResponse response = get("/unlocode?" + query, FreightUnlocodeResponse.class);
    if (response.results() == null) {
      return List.of();
    }
    return response.results().stream().filter(FreightUnlocodePort::isSeaport).toList();
  }

  /** 批量拉取主流国家港口，按 UN/LOCODE 去重。 */
  public List<FreightUnlocodePort> fetchMainstreamPorts() {
    return fetchMainstreamPorts(MAINSTREAM_COUNTRIES);
  }

  public List<FreightUnlocodePort> fetchMainstreamPorts(List<String> countryCodes) {
    Map<String, FreightUnlocodePort> unique = new LinkedHashMap<>();
    for (String country : countryCodes) {
      try {
        for (FreightUnlocodePort port : fetchPortsByCountry(country)) {
          if (port.code() != null && !port.code().isBlank()) {
            unique.putIfAbsent(port.code().toUpperCase(), port);
          }
        }
        sleepBetweenRequests();
      } catch (Exception ex) {
        log.warn("FreightUtils 拉取 {} 港口失败：{}", country, ex.getMessage());
      }
    }
    return new ArrayList<>(unique.values());
  }

  private void sleepBetweenRequests() {
    if (requestDelay.isZero()) {
      return;
    }
    try {
      Thread.sleep(requestDelay.toMillis());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private <T> T get(String path, Class<T> type) {
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + path))
              .timeout(REQUEST_TIMEOUT)
              .GET();
      if (!apiKey.isEmpty()) {
        builder.header("Authorization", "Bearer " + apiKey);
      }
      HttpResponse<String> response =
          httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 429) {
        throw new IllegalStateException("FreightUtils API 触发限流，请配置 api-key 或稍后重试");
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "FreightUtils API 错误 HTTP " + response.statusCode() + ": " + response.body());
      }
      return objectMapper.readValue(response.body(), type);
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new IllegalStateException("FreightUtils API 请求失败：" + path, ex);
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
