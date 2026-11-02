package com.furuiduo.quote.mail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.service.QuoteCostMatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "邮件诊断", description = "企业邮箱配置检查与测试发送（本地排查用）")
@RestController
@RequestMapping("/mail/debug")
public class QuoteMailDebugController {

  private final QuoteMailProperties mailProperties;
  private final ObjectProvider<MailProperties> springMailProperties;
  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final QuoteQuotedMailService quotedMailService;
  private final QuoteOrderRepository quoteOrderRepository;
  private final QuoteCostMatchService quoteCostMatchService;
  private final QuotePrintPdfRenderer pdfRenderer;

  public QuoteMailDebugController(
      QuoteMailProperties mailProperties,
      ObjectProvider<MailProperties> springMailProperties,
      ObjectProvider<JavaMailSender> mailSenderProvider,
      QuoteQuotedMailService quotedMailService,
      QuoteOrderRepository quoteOrderRepository,
      QuoteCostMatchService quoteCostMatchService,
      QuotePrintPdfRenderer pdfRenderer) {
    this.mailProperties = mailProperties;
    this.springMailProperties = springMailProperties;
    this.mailSenderProvider = mailSenderProvider;
    this.quotedMailService = quotedMailService;
    this.quoteOrderRepository = quoteOrderRepository;
    this.quoteCostMatchService = quoteCostMatchService;
    this.pdfRenderer = pdfRenderer;
  }

  @Operation(summary = "查看邮件配置状态（不含密码）")
  @GetMapping("/status")
  public ApiResponse<Map<String, Object>> status() {
    MailProperties springMail = springMailProperties.getIfAvailable();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("quoteMailEnabled", mailProperties.isEnabled());
    data.put("from", mailProperties.getFrom());
    data.put("fromName", mailProperties.getFromName());
    data.put("subjectPrefix", mailProperties.getSubjectPrefix());
    data.put("pdfFontPath", mailProperties.getPdfFontPath());
    data.put("pdfFontExists", fontExists(mailProperties.getPdfFontPath()));
    data.put("chromePath", mailProperties.getChromePath());
    String resolvedChrome = pdfRenderer.resolveBrowserPath();
    data.put("chromeResolvedPath", resolvedChrome);
    data.put(
        "chromeSnapWrapper",
        resolvedChrome != null && QuotePrintPdfRenderer.isSnapWrapper(Path.of(resolvedChrome)));
    data.put(
        "chromeVersion",
        resolvedChrome == null ? null : QuotePrintPdfRenderer.browserVersion(resolvedChrome));
    data.put("mailSenderPresent", mailSenderProvider.getIfAvailable() != null);
    if (springMail != null) {
      data.put("smtpHost", springMail.getHost());
      data.put("smtpPort", springMail.getPort());
      data.put("smtpUsername", springMail.getUsername());
      data.put("smtpPasswordConfigured", StringUtils.hasText(springMail.getPassword()));
    } else {
      data.put("smtpHost", null);
      data.put("smtpPort", null);
      data.put("smtpUsername", null);
      data.put("smtpPasswordConfigured", false);
    }
    data.put("userDir", System.getProperty("user.dir"));
    return ApiResponse.ok(data);
  }

  @Operation(summary = "发送一封测试邮件（纯文本，无 PDF）")
  @PostMapping("/send-test")
  public ApiResponse<Map<String, Object>> sendTest(
      @RequestParam(value = "to", required = false) String to) throws Exception {
    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null) {
      return ApiResponse.fail("JavaMailSender 未就绪，请检查 spring.mail 配置是否已加载");
    }
    MailProperties springMail = springMailProperties.getIfAvailable();
    String from =
        firstNonBlank(
            mailProperties.getFrom(), springMail == null ? null : springMail.getUsername());
    if (!StringUtils.hasText(from)) {
      return ApiResponse.fail("未配置发件人 quote.mail.from / spring.mail.username");
    }
    String target = firstNonBlank(to, from);
    if (!StringUtils.hasText(target)) {
      return ApiResponse.fail("未指定收件人");
    }

    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper =
        new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
    helper.setFrom(
        new InternetAddress(
            from,
            firstNonBlank(mailProperties.getFromName(), "FRD GLOBAL"),
            StandardCharsets.UTF_8.name()));
    helper.setTo(target);
    helper.setSubject(
        (mailProperties.getSubjectPrefix() == null ? "" : mailProperties.getSubjectPrefix())
            + " 邮件通道测试");
    helper.setText(
        "这是福瑞多报价系统企业邮箱 SMTP 测试邮件。\n"
            + "若收到此信，说明 spring.mail 配置可用。\n"
            + "from="
            + from
            + "\nuserDir="
            + System.getProperty("user.dir"),
        false);
    mailSender.send(message);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("sent", true);
    data.put("to", target);
    data.put("from", from);
    return ApiResponse.ok(data);
  }

  @Operation(summary = "按报价单 ID 立即补发已报价 PDF 邮件")
  @PostMapping("/resend-quoted")
  public ApiResponse<Map<String, Object>> resendQuoted(@RequestParam("quoteId") Long quoteId)
      throws Exception {
    quotedMailService.sendQuotedPdf(quoteId);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("quoteId", quoteId);
    data.put("triggered", true);
    return ApiResponse.ok(data);
  }

  @Operation(summary = "下载与「报价单样式预览→打印」一致的 PDF（本地核对用）")
  @GetMapping("/preview-pdf")
  public ResponseEntity<byte[]> previewPdf(@RequestParam("quoteId") Long quoteId)
      throws Exception {
    QuoteOrder order =
        quoteOrderRepository
            .findById(quoteId)
            .orElseThrow(() -> new IllegalArgumentException("报价单不存在: " + quoteId));
    QuoteSheetFieldsDto sheet = QuoteSheetFieldsDto.from(order);
    List<QuoteCostMatchItemDto> snapshots =
        quoteCostMatchService.listSnapshots(order.getId(), null);
    LocalDate quoteDate =
        order.getCreatedAt() == null ? LocalDate.now() : order.getCreatedAt().toLocalDate();
    byte[] pdf =
        pdfRenderer.render(
            order.getQuoteNo(),
            order.getCustomerName(),
            quoteDate,
            sheet,
            order.getServiceTypes(),
            snapshots);
    String fileName = "quote-print-" + sanitize(order.getQuoteNo()) + ".pdf";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  private static String sanitize(String quoteNo) {
    if (!StringUtils.hasText(quoteNo)) {
      return "unknown";
    }
    return quoteNo.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private static boolean fontExists(String path) {
    if (!StringUtils.hasText(path)) {
      return Files.isRegularFile(Path.of("C:\\Windows\\Fonts\\msyh.ttc"))
          || Files.isRegularFile(Path.of("C:\\Windows\\Fonts\\simsun.ttc"));
    }
    return Files.isRegularFile(Path.of(path.trim()));
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }
}
