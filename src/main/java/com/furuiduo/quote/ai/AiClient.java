package com.furuiduo.quote.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AiClient {

  private static final Logger log = LoggerFactory.getLogger(AiClient.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(90);

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final boolean enabled;
  private final String baseUrl;
  private final String apiKey;
  private final String model;

  public AiClient(
      ObjectMapper objectMapper,
      @Value("${quote.ai.enabled:true}") boolean enabled,
      @Value("${quote.ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
          String baseUrl,
      @Value("${quote.ai.api-key:}") String apiKey,
      @Value("${quote.ai.model:qwen-plus}") String model) {
    this.objectMapper = objectMapper;
    this.enabled = enabled;
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model == null || model.isBlank() ? "qwen-plus" : model.trim();
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
  }

  public String model() {
    return model;
  }

  public void ensureConfigured() {
    if (!enabled) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 已关闭");
    }
    if (apiKey.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 未配置");
    }
  }

  public JsonNode chatCompletions(Map<String, Object> body) {
    ensureConfigured();
    try {
      String payload = objectMapper.writeValueAsString(body);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + "/chat/completions"))
              .timeout(TIMEOUT)
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + apiKey)
              .POST(HttpRequest.BodyPublishers.ofString(payload))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("DashScope HTTP {}: {}", response.statusCode(), truncate(response.body()));
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "AI 服务调用失败（HTTP " + response.statusCode() + "）");
      }
      return objectMapper.readTree(response.body());
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.error("DashScope 请求异常", ex);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 服务请求失败", ex);
    }
  }

  private static String truncate(String body) {
    if (body == null) {
      return "";
    }
    return body.length() > 400 ? body.substring(0, 400) + "…" : body;
  }
}
