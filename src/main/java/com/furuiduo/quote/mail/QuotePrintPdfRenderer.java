package com.furuiduo.quote.mail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;
import com.furuiduo.quote.shippingline.entity.ShippingLine;
import com.furuiduo.quote.shippingline.repository.ShippingLineRepository;

/**
 * 按前端「报价单样式预览」同源 HTML/CSS，用 Chrome/Edge headless 打印生成 PDF
 *（效果等同预览页点「打印报价单」→ 另存为 PDF）。
 */
@Component
public class QuotePrintPdfRenderer {

  private static final Logger log = LoggerFactory.getLogger(QuotePrintPdfRenderer.class);

  private final QuoteMailProperties mailProperties;
  private final ShippingLineRepository shippingLineRepository;

  public QuotePrintPdfRenderer(
      QuoteMailProperties mailProperties, ShippingLineRepository shippingLineRepository) {
    this.mailProperties = mailProperties;
    this.shippingLineRepository = shippingLineRepository;
  }

  public byte[] render(
      String quoteNo,
      String customerName,
      LocalDate quoteDate,
      QuoteSheetFieldsDto sheet,
      List<String> serviceTypes,
      List<QuoteCostMatchItemDto> costSnapshots)
      throws IOException {
    Objects.requireNonNull(sheet, "sheet");
    String html =
        QuotePrintHtmlBuilder.build(
            customerName,
            quoteDate,
            sheet,
            serviceTypes,
            costSnapshots,
            loadSslRemarkLookup());
    Path tempDir = Files.createTempDirectory("quote-print-");
    Path htmlFile = tempDir.resolve("quote-print.html");
    Path pdfFile = tempDir.resolve("quote-print.pdf");
    try {
      Files.writeString(htmlFile, html, StandardCharsets.UTF_8);
      printHtmlToPdf(htmlFile, pdfFile);
      if (!Files.isRegularFile(pdfFile) || Files.size(pdfFile) < 100) {
        throw new IOException("浏览器打印 PDF 失败，未生成有效文件");
      }
      log.info("报价打印 PDF 已生成 quoteNo={} size={}", quoteNo, Files.size(pdfFile));
      return Files.readAllBytes(pdfFile);
    } finally {
      deleteQuietly(htmlFile);
      deleteQuietly(pdfFile);
      deleteQuietly(tempDir);
    }
  }

  private Map<String, String> loadSslRemarkLookup() {
    Map<String, String> map = new LinkedHashMap<>();
    try {
      List<ShippingLine> lines = shippingLineRepository.findAll();
      for (ShippingLine line : lines) {
        if (line == null || line.getStatus() == null || line.getStatus() != 1) {
          continue;
        }
        String remark = line.getRemark() == null ? "" : line.getRemark().trim();
        if (remark.isEmpty()) {
          continue;
        }
        if (line.getName() != null && !line.getName().isBlank()) {
          map.put(line.getName().trim(), remark);
        }
        if (line.getShortName() != null && !line.getShortName().isBlank()) {
          map.put(line.getShortName().trim(), remark);
        }
      }
    } catch (Exception ex) {
      log.warn("加载船司备注失败，PDF 将不带 SSL REMARK: {}", ex.getMessage());
    }
    return map;
  }

  private void printHtmlToPdf(Path htmlFile, Path pdfFile) throws IOException {
    String browser = resolveBrowserPath();
    if (browser == null) {
      throw new IOException(
          "未找到 Chrome/Edge，无法生成与打印预览一致的 PDF。"
              + "请安装 Chrome/Edge，或配置 quote.mail.chrome-path");
    }
    // 独立用户目录：避免与本机正在运行的浏览器实例互相干扰（单例转交会导致静默不打印）
    Path profileDir = pdfFile.getParent().resolve("chrome-profile");
    List<String> command = new ArrayList<>();
    command.add(browser);
    command.add("--headless=new");
    command.add("--disable-gpu");
    command.add("--no-first-run");
    command.add("--no-default-browser-check");
    command.add("--allow-file-access-from-files");
    command.add("--no-pdf-header-footer");
    command.add("--print-to-pdf-no-header");
    // Linux 服务器（systemd 常以 root 运行）：不加 --no-sandbox 会被 Chrome 直接拒绝启动；
    // /dev/shm 偏小（容器/小内存机器）时加 --disable-dev-shm-usage 避免渲染崩溃。
    if (!isWindows()) {
      command.add("--no-sandbox");
      command.add("--disable-dev-shm-usage");
    }
    command.add("--user-data-dir=" + profileDir.toAbsolutePath());
    command.add("--print-to-pdf=" + pdfFile.toAbsolutePath());
    command.add(htmlFile.toAbsolutePath().toUri().toString());

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    Process process = pb.start();
    String output;
    try {
      output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      boolean finished = process.waitFor(60, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new IOException("浏览器打印 PDF 超时");
      }
      if (process.exitValue() != 0 && !Files.isRegularFile(pdfFile)) {
        throw new IOException(
            "浏览器打印 PDF 失败 exit="
                + process.exitValue()
                + " output="
                + trimOutput(output)
                + snapHint(output));
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      throw new IOException("浏览器打印 PDF 被中断", ex);
    }
    // Chrome 154+ 在 Windows 上启动器进程会先退出（exit=0），PDF 由子进程延迟写完，
    // 必须轮询等待文件生成且大小稳定后再判定成败。
    waitUntilPdfReady(pdfFile, output);
  }

  /** 轮询等待 PDF 文件出现且大小连续两次检查不变（视为写完），超时 60s。 */
  private static void waitUntilPdfReady(Path pdfFile, String browserOutput) throws IOException {
    long deadline = System.currentTimeMillis() + 60_000L;
    Long lastSize = null;
    while (System.currentTimeMillis() < deadline) {
      if (Files.isRegularFile(pdfFile)) {
        long size = Files.size(pdfFile);
        if (size >= 100 && lastSize != null && size == lastSize) {
          return;
        }
        lastSize = size;
      } else {
        lastSize = null;
      }
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IOException("等待浏览器生成 PDF 被中断", ex);
      }
    }
    throw new IOException(
        "浏览器打印 PDF 失败，60s 内未生成有效文件 output=" + trimOutput(browserOutput));
  }

  public String resolveBrowserPath() {
    if (mailProperties.getChromePath() != null && !mailProperties.getChromePath().isBlank()) {
      Path configured = Path.of(mailProperties.getChromePath().trim());
      if (Files.isRegularFile(configured)) {
        if (isSnapWrapper(configured)) {
          log.warn("quote.mail.chrome-path 指向 snap 版浏览器，systemd 服务内通常无法启动: {}", configured);
        }
        return configured.toAbsolutePath().toString();
      }
      log.warn("quote.mail.chrome-path 无效: {}", configured);
    }
    String env = System.getenv("QUOTE_MAIL_CHROME_PATH");
    if (env != null && !env.isBlank() && Files.isRegularFile(Path.of(env.trim()))) {
      return Path.of(env.trim()).toAbsolutePath().toString();
    }
    for (String candidate :
        List.of(
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
            "/usr/bin/google-chrome",
            "/usr/bin/google-chrome-stable",
            "/opt/google/chrome/chrome",
            "/usr/bin/chromium",
            "/usr/bin/chromium-browser",
            "/usr/bin/microsoft-edge",
            "/snap/bin/chromium")) {
      Path path = Path.of(candidate);
      if (!Files.isRegularFile(path)) {
        continue;
      }
      // Ubuntu 的 chromium-browser / snap/bin/chromium 是 snap 转接器，systemd 服务内会报
      // 「is not a snap cgroup」，直接跳过，避免选到必然失败的浏览器
      if (isSnapWrapper(path)) {
        log.warn("跳过 snap 版浏览器（systemd 内无法启动）: {}", candidate);
        continue;
      }
      return candidate;
    }
    return null;
  }

  /** 是否为 snap 转接脚本或 snap 目录下的可执行文件。 */
  static boolean isSnapWrapper(Path path) {
    try {
      if (path == null) {
        return false;
      }
      String text = path.toAbsolutePath().toString().replace('\\', '/');
      if (text.startsWith("/snap/") || text.contains("/snap/bin/")) {
        return true;
      }
      if (!Files.isRegularFile(path)) {
        return false;
      }
      byte[] head = new byte[2048];
      try (var in = Files.newInputStream(path)) {
        int read = in.read(head);
        if (read <= 0) {
          return false;
        }
        String content = new String(head, 0, read, StandardCharsets.UTF_8);
        return content.startsWith("#!") && content.contains("snap");
      }
    } catch (Exception ex) {
      return false;
    }
  }

  /** 浏览器版本探测（诊断接口用）。 */
  public static String browserVersion(String path) {
    if (path == null || path.isBlank()) {
      return null;
    }
    // Windows 上 chrome.exe 不认 --version，会直接弹一个窗口出来，跳过探测
    if (isWindows()) {
      return null;
    }
    Path profile = null;
    try {
      // 独立 profile + --no-first-run：避免挂到已运行的浏览器实例上，只返回一句提示
      profile = Files.createTempDirectory("chrome-version-");
      Process process =
          new ProcessBuilder(
                  path,
                  "--version",
                  "--no-first-run",
                  "--user-data-dir=" + profile.toAbsolutePath())
              .redirectErrorStream(true)
              .start();
      String output;
      try (var in = process.getInputStream()) {
        // Chrome 在 Windows 上按系统代码页输出，用 default charset 解码避免中文乱码
        output = new String(in.readAllBytes(), java.nio.charset.Charset.defaultCharset()).trim();
      }
      if (!process.waitFor(10, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return "探测超时";
      }
      return extractVersionLine(output);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return "探测被中断";
    } catch (Exception ex) {
      return "探测失败: " + ex.getMessage();
    } finally {
      deleteTreeQuietly(profile);
    }
  }

  /** 从 --version 输出里挑出真正的版本行（Chrome 会额外打印 updater 报错到 stderr）。 */
  private static String extractVersionLine(String output) {
    if (output == null || output.isBlank()) {
      return "";
    }
    String fallback = null;
    for (String line : output.split("\\R")) {
      String text = line.trim();
      if (text.isEmpty()) {
        continue;
      }
      if (text.matches("(?i)^(google chrome|chromium|microsoft edge|brave).*")) {
        return text;
      }
      if (fallback == null && text.matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
        fallback = text;
      }
    }
    if (fallback != null) {
      return fallback;
    }
    String trimmed = output.trim();
    return trimmed.length() > 300 ? trimmed.substring(0, 300) : trimmed;
  }

  /** snap 版浏览器在 systemd 服务内的典型报错 → 给出可执行的处理建议。 */
  private static String snapHint(String output) {
    if (output == null) {
      return "";
    }
    String text = output.toLowerCase();
    if (text.contains("is not a snap cgroup")
        || text.contains("xdg-settings: not found")
        || text.contains("/snap/")) {
      return "\n提示：当前使用的是 snap 版 chromium，systemd 服务内无法启动。"
          + "请安装官方 Chrome（apt install ./google-chrome-stable_current_amd64.deb），"
          + "或用 QUOTE_MAIL_CHROME_PATH 指向真实可执行文件（如 /opt/google/chrome/chrome）。";
    }
    return "";
  }

  private static boolean isWindows() {
    String os = System.getProperty("os.name", "");
    return os.toLowerCase().contains("win");
  }

  private static String trimOutput(String output) {
    if (output == null) {
      return "";
    }
    String text = output.trim();
    return text.length() > 500 ? text.substring(0, 500) : text;
  }

  private static void deleteQuietly(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (Exception ignore) {
      // ignore
    }
  }

  private static void deleteTreeQuietly(Path dir) {
    if (dir == null || !Files.exists(dir)) {
      return;
    }
    try (var walk = Files.walk(dir)) {
      walk.sorted(java.util.Comparator.reverseOrder())
          .forEach(QuotePrintPdfRenderer::deleteQuietly);
    } catch (Exception ignore) {
      // ignore
    }
  }
}
