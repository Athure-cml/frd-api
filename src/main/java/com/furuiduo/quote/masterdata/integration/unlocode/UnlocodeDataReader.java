package com.furuiduo.quote.masterdata.integration.unlocode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 读取 UNECE 官方 UN/LOCODE 离线包，解析为全量运输节点记录。
 *
 * <p>优先级：本地 ZIP → 本地 CSV → 下载官方 ZIP。
 */
@Component
public class UnlocodeDataReader {

  private static final Logger log = LoggerFactory.getLogger(UnlocodeDataReader.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(120);
  private static final Pattern VERSION_PATTERN =
      Pattern.compile("loc(\\d{2})(\\d)csv", Pattern.CASE_INSENSITIVE);

  private final HttpClient httpClient;
  private final String localZipPath;
  private final String localCsvPath;
  private final String officialZipUrl;

  public UnlocodeDataReader(
      @Value("${quote.masterdata.global-port.unlocode-zip-path:}") String localZipPath,
      @Value("${quote.masterdata.global-port.unlocode-csv-path:}") String localCsvPath,
      @Value(
              "${quote.masterdata.global-port.unlocode-official-zip-url:https://service.unece.org/trade/locode/loc242csv.zip}")
          String officialZipUrl,
      @Value(
              "${quote.masterdata.global-port.unlocode-csv-url:https://raw.githubusercontent.com/datasets/un-locode/main/data/code-list.csv}")
          String ignoredMirrorCsvUrl) {
    this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.localZipPath = trim(localZipPath);
    this.localCsvPath = trim(localCsvPath);
    this.officialZipUrl = officialZipUrl;
  }

  public UnlocodeDataset load() throws IOException {
    if (!localZipPath.isEmpty()) {
      Path zip = Path.of(localZipPath).toAbsolutePath().normalize();
      if (Files.isRegularFile(zip)) {
        log.info("从本地 UNECE ZIP 读取 UN/LOCODE：{}", zip);
        return parseZipFile(zip, resolveVersionFromPath(zip.getFileName().toString()));
      }
      log.warn("本地 ZIP 不存在，将尝试在线下载：{}", zip);
    }

    if (!localCsvPath.isEmpty()) {
      Path csv = Path.of(localCsvPath).toAbsolutePath().normalize();
      if (Files.isRegularFile(csv)) {
        log.info("从本地 CSV 读取 UN/LOCODE：{}", csv);
        String content = Files.readString(csv, StandardCharsets.UTF_8);
        return new UnlocodeDataset(UnlocodeCsvParser.parse(content), null);
      }
    }

    log.info("下载 UNECE 官方 UN/LOCODE ZIP：{}", officialZipUrl);
    byte[] zipBytes = downloadBytes(officialZipUrl);
    cacheZipIfConfigured(zipBytes);
    String version = resolveVersionFromPath(officialZipUrl);
    return parseZipBytes(zipBytes, version);
  }

  private void cacheZipIfConfigured(byte[] zipBytes) {
    if (localZipPath.isEmpty()) {
      return;
    }
    try {
      Path zip = Path.of(localZipPath).toAbsolutePath().normalize();
      Files.createDirectories(zip.getParent());
      Files.write(zip, zipBytes);
      log.info("已缓存 UN/LOCODE ZIP 到本地：{}", zip);
    } catch (IOException ex) {
      log.warn("缓存 UN/LOCODE ZIP 失败：{}", ex.getMessage());
    }
  }

  private UnlocodeDataset parseZipFile(Path zipPath, String version) throws IOException {
    Map<String, UnlocodeRecord> merged = new LinkedHashMap<>();
    int fileCount = 0;
    try (ZipFile zipFile = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
      for (ZipEntry entry : zipFile.stream().toList()) {
        if (entry.isDirectory() || !isLocodeDataCsv(entry.getName())) {
          continue;
        }
        fileCount++;
        log.info("解析 UN/LOCODE 文件 {} …", entry.getName());
        String csv;
        try (InputStream input = zipFile.getInputStream(entry)) {
          csv = readStream(input);
        }
        mergeRecords(merged, csv, entry.getName());
      }
    }
  return buildDataset(merged, fileCount, version);
  }

  private UnlocodeDataset parseZipBytes(byte[] zipBytes, String version) throws IOException {
    Map<String, UnlocodeRecord> merged = new LinkedHashMap<>();
    int fileCount = 0;
    try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory() || !isLocodeDataCsv(entry.getName())) {
          continue;
        }
        fileCount++;
        log.info("解析 UN/LOCODE 文件 {} …", entry.getName());
        String csv = readStream(zis);
        mergeRecords(merged, csv, entry.getName());
      }
    }
    return buildDataset(merged, fileCount, version);
  }

  private void mergeRecords(Map<String, UnlocodeRecord> merged, String csv, String fileName)
      throws IOException {
    for (UnlocodeRecord record : UnlocodeCsvParser.parse(csv)) {
      merged.put(record.code(), record);
    }
    log.info("已解析 {}，累计 {} 条", fileName, merged.size());
  }

  private UnlocodeDataset buildDataset(
      Map<String, UnlocodeRecord> merged, int fileCount, String version) throws IOException {
    if (merged.isEmpty()) {
      throw new IOException("ZIP 中未找到可解析的 UN/LOCODE CSV（匹配文件数=" + fileCount + "）");
    }
    log.info("UN/LOCODE ZIP 解析完成，共 {} 条", merged.size());
    return new UnlocodeDataset(new ArrayList<>(merged.values()), version);
  }

  static boolean isLocodeDataCsv(String entryName) {
    String lower = entryName.toLowerCase(Locale.ROOT);
    if (!lower.endsWith(".csv")) {
      return false;
    }
    return !lower.contains("subdivision");
  }

  private static String readStream(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    input.transferTo(buffer);
    return buffer.toString(StandardCharsets.UTF_8);
  }

  private byte[] downloadBytes(String url) throws IOException {
    HttpResponse<byte[]> response = send(url, HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("HTTP " + response.statusCode());
    }
    return response.body();
  }

  private <T> HttpResponse<T> send(String url, HttpResponse.BodyHandler<T> handler)
      throws IOException {
    try {
      HttpRequest request =
          HttpRequest.newBuilder().uri(URI.create(url)).timeout(TIMEOUT).GET().build();
      return httpClient.send(request, handler);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("下载中断", ex);
    }
  }

  static String resolveVersionFromPath(String path) {
    if (path == null) {
      return null;
    }
    Matcher matcher = VERSION_PATTERN.matcher(path);
    if (!matcher.find()) {
      return null;
    }
    return "20" + matcher.group(1) + "-" + matcher.group(2);
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
