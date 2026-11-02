package com.furuiduo.quote.mail;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.mail.SendFailedException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;
import com.furuiduo.quote.quote.entity.InquiryOrder;
import com.furuiduo.quote.quote.entity.QuoteMailLog;
import com.furuiduo.quote.quote.entity.QuoteMailSkipReason;
import com.furuiduo.quote.quote.entity.QuoteMailStage;
import com.furuiduo.quote.quote.entity.QuoteMailStatus;
import com.furuiduo.quote.quote.entity.QuoteMailTrigger;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.repository.InquiryOrderRepository;
import com.furuiduo.quote.quote.repository.QuoteMailLogRepository;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.service.QuoteCostMatchService;
import com.furuiduo.quote.sys.entity.OperationAction;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysUserRepository;
import com.furuiduo.quote.sys.service.OperationLogService;

/**
 * 报价进入「已报价」后，将打印模板 PDF 发到关联询价单操作人邮箱。
 *
 * <p>企业微信邮箱走腾讯企业邮 SMTP（smtp.exmail.qq.com）。发送全路径落库
 * {@link QuoteMailLog}：SENDING → SENT / FAILED / SKIPPED，失败原因映射成人话，
 * 并写入操作日志（module=quote-mail）。
 */
@Service
public class QuoteQuotedMailService {

  private static final Logger log = LoggerFactory.getLogger(QuoteQuotedMailService.class);

  private final QuoteMailProperties mailProperties;
  private final ObjectProvider<MailProperties> springMailProperties;
  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final ObjectProvider<AsyncMailNotifier> notifierProvider;
  private final QuoteOrderRepository quoteOrderRepository;
  private final InquiryOrderRepository inquiryOrderRepository;
  private final QuoteMailLogRepository mailLogRepository;
  private final SysUserRepository sysUserRepository;
  private final OperationLogService operationLogService;
  private final QuoteCostMatchService quoteCostMatchService;
  private final QuotePrintPdfRenderer pdfRenderer;

  public QuoteQuotedMailService(
      QuoteMailProperties mailProperties,
      ObjectProvider<MailProperties> springMailProperties,
      ObjectProvider<JavaMailSender> mailSenderProvider,
      ObjectProvider<AsyncMailNotifier> notifierProvider,
      QuoteOrderRepository quoteOrderRepository,
      InquiryOrderRepository inquiryOrderRepository,
      QuoteMailLogRepository mailLogRepository,
      SysUserRepository sysUserRepository,
      OperationLogService operationLogService,
      QuoteCostMatchService quoteCostMatchService,
      QuotePrintPdfRenderer pdfRenderer) {
    this.mailProperties = mailProperties;
    this.springMailProperties = springMailProperties;
    this.mailSenderProvider = mailSenderProvider;
    this.notifierProvider = notifierProvider;
    this.quoteOrderRepository = quoteOrderRepository;
    this.inquiryOrderRepository = inquiryOrderRepository;
    this.mailLogRepository = mailLogRepository;
    this.sysUserRepository = sysUserRepository;
    this.operationLogService = operationLogService;
    this.quoteCostMatchService = quoteCostMatchService;
    this.pdfRenderer = pdfRenderer;
  }

  /** 事务提交后再异步发送，避免拖慢确认报价接口。 */
  public void scheduleAfterQuoted(
      Long quoteId, Long operatorId, String operatorName, QuoteMailTrigger trigger) {
    if (quoteId == null) {
      return;
    }
    if (!mailProperties.isEnabled()) {
      // 总开关关闭也落一条 SKIPPED，避免业务以为已发送
      persistOnly(quoteId, operatorId, operatorName, trigger);
      log.warn("quote.mail.enabled=false，跳过已报价发信 quoteId={}", quoteId);
      return;
    }
    AsyncMailNotifier notifier = notifierProvider.getIfAvailable();
    if (notifier == null) {
      log.error("AsyncMailNotifier 未注入，无法异步发信 quoteId={}", quoteId);
      return;
    }
    log.info("已报价发信已登记，事务提交后异步执行 quoteId={} trigger={}", quoteId, trigger);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              notifier.sendQuotedPdfAsync(quoteId, operatorId, operatorName, trigger);
            }
          });
      return;
    }
    notifier.sendQuotedPdfAsync(quoteId, operatorId, operatorName, trigger);
  }

  /** 兼容调试接口：直接同步发送（trigger=CONFIRM，操作人取报价单创建人）。 */
  public void sendQuotedPdf(Long quoteId) throws Exception {
    QuoteOrder order = quoteOrderRepository.findById(quoteId).orElse(null);
    Long operatorId = order == null ? null : order.getCreatedBy();
    String operatorName = order == null ? null : order.getCreatedByName();
    sendQuotedPdf(quoteId, operatorId, operatorName, QuoteMailTrigger.CONFIRM);
  }

  /** 人工指定邮箱发送（页面「发送报价单」）：事务提交后异步执行。 */
  public void scheduleManualSend(
      Long quoteId,
      List<String> recipients,
      Long operatorId,
      String operatorName) {
    if (quoteId == null || recipients == null || recipients.isEmpty()) {
      return;
    }
    AsyncMailNotifier notifier = notifierProvider.getIfAvailable();
    if (notifier == null) {
      log.error("AsyncMailNotifier 未注入，无法异步发信 quoteId={}", quoteId);
      return;
    }
    List<String> targets = List.copyOf(recipients);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              notifier.sendQuotedPdfToAsync(
                  quoteId, targets, operatorId, operatorName, QuoteMailTrigger.MANUAL);
            }
          });
      return;
    }
    notifier.sendQuotedPdfToAsync(
        quoteId, targets, operatorId, operatorName, QuoteMailTrigger.MANUAL);
  }

  /** 人工发送前的配置自检：返回不可发送原因，null 表示可用。 */
  public String manualSendBlockedReason() {
    if (!mailProperties.isEnabled()) {
      return "系统未开启发信（QUOTE_MAIL_ENABLED=false），请先配置邮件服务";
    }
    if (mailSenderProvider.getIfAvailable() == null) {
      return "未配置 SMTP（QUOTE_MAIL_USERNAME / QUOTE_MAIL_PASSWORD）";
    }
    return null;
  }

  /** 全量发信：全路径落库 quote_mail_log，并同步报价单冗余状态与操作日志。 */
  public void sendQuotedPdf(
      Long quoteId,
      Long operatorId,
      String operatorName,
      QuoteMailTrigger trigger) {
    sendQuotedPdf(quoteId, null, operatorId, operatorName, trigger);
  }

  public void sendQuotedPdf(
      Long quoteId,
      List<String> recipients,
      Long operatorId,
      String operatorName,
      QuoteMailTrigger trigger) {
    QuoteMailLog mailLog =
        newMailLog(quoteId, operatorId, operatorName, trigger, QuoteMailStatus.SENDING);
    try {
      sendAndFinalize(mailLog, quoteId, recipients, operatorId, operatorName, trigger);
    } catch (Exception ex) {
      // 兜底：任何未预期异常都按 FAILED 落库，避免永久停留在 SENDING
      failLog(
          mailLog,
          QuoteMailStage.SMTP,
          null,
          "发送异常：" + describeException(ex),
          operatorId,
          operatorName,
          trigger);
    }
  }

  private void sendAndFinalize(
      QuoteMailLog mailLog,
      Long quoteId,
      List<String> manualRecipients,
      Long operatorId,
      String operatorName,
      QuoteMailTrigger trigger) {
    QuoteOrder order = quoteOrderRepository.findById(quoteId).orElse(null);
    if (order == null) {
      failLog(
          mailLog,
          QuoteMailStage.PREPARE,
          null,
          "报价单不存在: " + quoteId,
          operatorId,
          operatorName,
          trigger);
      return;
    }

    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    MailProperties springMail = springMailProperties.getIfAvailable();
    String username = springMail == null ? null : trimToNull(springMail.getUsername());
    String from = firstNonBlank(mailProperties.getFrom(), username);
    if (mailSender == null || !StringUtils.hasText(from)) {
      skipLog(
          mailLog,
          QuoteMailSkipReason.NO_FROM,
          QuoteMailStage.PREPARE,
          order,
          operatorId,
          operatorName,
          trigger);
      return;
    }
    if (!mailProperties.isEnabled()) {
      skipLog(
          mailLog,
          QuoteMailSkipReason.MAIL_DISABLED,
          QuoteMailStage.PREPARE,
          order,
          operatorId,
          operatorName,
          trigger);
      return;
    }

    boolean manual =
        manualRecipients != null
            && !manualRecipients.stream().filter(StringUtils::hasText).toList().isEmpty();
    List<String> targets =
        manual
            ? manualRecipients.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList()
            : List.of();

    Optional<InquiryOrder> inquiryOpt = resolveInquiry(order);
    // 人工发送邮件不依赖询价单：有就带上单号，没有也能发
    if (inquiryOpt.isEmpty() && !manual) {
      skipLog(
          mailLog,
          QuoteMailSkipReason.NO_INQUIRY,
          QuoteMailStage.PREPARE,
          order,
          operatorId,
          operatorName,
          trigger);
      return;
    }
    InquiryOrder inquiry = inquiryOpt.orElse(null);
    if (inquiry != null) {
      mailLog.setInquiryId(inquiry.getId());
    }
    mailLogRepository.save(mailLog);

    // 自动发信收件人固定为询价单操作人；人工发送用页面填写的邮箱
    SysUser recipient =
        inquiry == null ? null : sysUserRepository.findById(inquiry.getCreatedBy()).orElse(null);
    String toEmail =
        manual
            ? abbreviate(String.join("; ", targets), 255)
            : (recipient == null ? null : trimToNull(recipient.getEmail()));
    if (!StringUtils.hasText(toEmail)) {
      skipLog(
          mailLog,
          QuoteMailSkipReason.NO_OPERATOR_EMAIL,
          QuoteMailStage.PREPARE,
          order,
          operatorId,
          operatorName,
          trigger);
      return;
    }

    // 先落收件人，PDF 阶段失败也能在日志里看出是发给谁的
    mailLog.setToEmail(toEmail);
    mailLogRepository.save(mailLog);

    // PDF 渲染单独计阶段
    byte[] pdf;
    try {
      QuoteSheetFieldsDto sheet = QuoteSheetFieldsDto.from(order);
      List<QuoteCostMatchItemDto> snapshots =
          quoteCostMatchService.listSnapshots(order.getId(), null);
      LocalDate quoteDate =
          order.getCreatedAt() == null
              ? LocalDate.now()
              : order.getCreatedAt().toLocalDate();
      pdf =
          pdfRenderer.render(
              order.getQuoteNo(),
              order.getCustomerName(),
              quoteDate,
              sheet,
              order.getServiceTypes(),
              snapshots);
    } catch (Exception ex) {
      log.error("报价邮件 PDF 渲染失败 quoteId={}", quoteId, ex);
      failLog(
          mailLog,
          QuoteMailStage.PDF,
          "PDF_RENDER",
          "打印模板渲染 PDF 失败：" + describeException(ex),
          operatorId,
          operatorName,
          trigger);
      return;
    }

    String subject =
        "POR "
            + subjectPart(order.getPor())
            + " POD "
            + subjectPart(order.getPod())
            + " CITY "
            + subjectPart(order.getCity());
    mailLog.setSubject(subject);
    mailLog.setToEmail(toEmail);
    mailLog.setAttachmentName("quote-" + sanitizeFileName(order.getQuoteNo()) + ".pdf");
    mailLogRepository.save(mailLog);

    String body =
        manual
            ? """
            您好，

            附件为报价单 %s 的打印模板 PDF，请查收。
            客户：%s

            —— 福瑞多报价系统
            """
                .formatted(
                    nullToEmpty(order.getQuoteNo()), nullToEmpty(order.getCustomerName()))
            : """
            您好 %s，

            关联询价单 %s 的报价单 %s 已确认（已报价）。
            客户：%s
            附件为报价单打印模板 PDF，请查收。

            —— 福瑞多报价系统自动发送
            """
                .formatted(
                    firstNonBlank(
                        inquiry == null ? null : inquiry.getCreatedByName(),
                        recipient == null ? null : recipient.getRealName(),
                        "同事"),
                    nullToEmpty(inquiry == null ? null : inquiry.getInquiryNo()),
                    nullToEmpty(order.getQuoteNo()),
                    nullToEmpty(order.getCustomerName()));

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
      helper.setFrom(
          new InternetAddress(
              from,
              firstNonBlank(mailProperties.getFromName(), "FRD GLOBAL"),
              StandardCharsets.UTF_8.name()));
      if (manual) {
        helper.setTo(targets.toArray(new String[0]));
      } else {
        helper.setTo(toEmail);
      }
      helper.setSubject(subject);
      helper.setText(body, false);
      helper.addAttachment(
          "quote-" + sanitizeFileName(order.getQuoteNo()) + ".pdf",
          new ByteArrayResource(pdf),
          "application/pdf");
      mailSender.send(message);
    } catch (Exception ex) {
      String[] codeAndText = mapSmtpError(ex);
      failLog(
          mailLog,
          QuoteMailStage.SMTP,
          codeAndText[0],
          codeAndText[1],
          operatorId,
          operatorName,
          trigger);
      return;
    }

    succeedLog(mailLog, order, operatorId, operatorName, trigger);
    log.info(
        "报价邮件已发送 quoteId={} quoteNo={} to={} inquiryNo={} trigger={}",
        quoteId,
        order.getQuoteNo(),
        toEmail,
        inquiry == null ? null : inquiry.getInquiryNo(),
        trigger);
  }

  /** 仅落一条 SKIPPED（总开关关闭时，不渲染 PDF、不查库）。 */
  private void persistOnly(
      Long quoteId, Long operatorId, String operatorName, QuoteMailTrigger trigger) {
    QuoteOrder order = quoteOrderRepository.findById(quoteId).orElse(null);
    if (order == null) {
      return;
    }
    QuoteMailLog mailLog =
        newMailLog(quoteId, operatorId, operatorName, trigger, QuoteMailStatus.SKIPPED);
    skipLog(
        mailLog,
        QuoteMailSkipReason.MAIL_DISABLED,
        QuoteMailStage.PREPARE,
        order,
        operatorId,
        operatorName,
        trigger);
  }

  private QuoteMailLog newMailLog(
      Long quoteId,
      Long operatorId,
      String operatorName,
      QuoteMailTrigger trigger,
      QuoteMailStatus status) {
    QuoteMailLog mailLog = new QuoteMailLog();
    mailLog.setQuoteId(quoteId);
    mailLog.setStatus(status);
    mailLog.setTriggerType(trigger);
    mailLog.setOperatorId(operatorId);
    mailLog.setOperatorName(operatorName);
    mailLog.setCreatedAt(LocalDateTime.now());
    mailLog.setStartedAt(LocalDateTime.now());
    return mailLog;
  }

  private void succeedLog(
      QuoteMailLog mailLog,
      QuoteOrder order,
      Long operatorId,
      String operatorName,
      QuoteMailTrigger trigger) {
    mailLog.setStatus(QuoteMailStatus.SENT);
    mailLog.setStage(null);
    mailLog.setFinishedAt(LocalDateTime.now());
    mailLogRepository.save(mailLog);
    syncQuoteOrder(order, QuoteMailStatus.SENT, null);

    SysUser logUser = loadUser(operatorId);
    operationLogService.recordSuccess(
        logUser,
        "quote",
        OperationAction.UPDATE,
        "QuoteOrder",
        String.valueOf(order.getId()),
        "报价单 "
            + nullToEmpty(order.getQuoteNo())
            + " 邮件发送成功 → "
            + (mailLog.getToEmail() == null ? "" : mailLog.getToEmail())
            + "（"
            + triggerLabel(trigger)
            + "）",
        null,
        null,
        null,
        null);
  }

  private void failLog(
      QuoteMailLog mailLog,
      QuoteMailStage stage,
      String errorCode,
      String errorMessage,
      Long operatorId,
      String operatorName,
      QuoteMailTrigger trigger) {
    String text = abbreviate(errorMessage, 1000);
    mailLog.setStatus(QuoteMailStatus.FAILED);
    mailLog.setStage(stage);
    mailLog.setErrorCode(errorCode);
    mailLog.setErrorMessage(text);
    mailLog.setFinishedAt(LocalDateTime.now());
    mailLogRepository.save(mailLog);

    QuoteOrder order = quoteOrderRepository.findById(mailLog.getQuoteId()).orElse(null);
    if (order != null) {
      syncQuoteOrder(order, QuoteMailStatus.FAILED, text);
      SysUser logUser = loadUser(operatorId);
      operationLogService.recordFailure(
          logUser,
          "quote",
          OperationAction.UPDATE,
          "QuoteOrder",
          String.valueOf(order.getId()),
          "报价单 "
              + nullToEmpty(order.getQuoteNo())
              + " 邮件发送失败（"
              + triggerLabel(trigger)
              + " → "
              + (mailLog.getToEmail() == null ? "未确定收件人" : mailLog.getToEmail())
              + "）",
          null,
          null,
          null,
          null,
          text);
    }
    log.error("已报价邮件发送失败 quoteId={} stage={} code={}", mailLog.getQuoteId(), stage, errorCode);
  }

  private void skipLog(
      QuoteMailLog mailLog,
      QuoteMailSkipReason reason,
      QuoteMailStage stage,
      QuoteOrder order,
      Long operatorId,
      String operatorName,
      QuoteMailTrigger trigger) {
    mailLog.setStatus(QuoteMailStatus.SKIPPED);
    mailLog.setSkipReason(reason);
    mailLog.setStage(stage);
    mailLog.setFinishedAt(LocalDateTime.now());
    if (order != null) {
      mailLog.setQuoteNo(order.getQuoteNo());
      mailLog.setInquiryId(order.getInquiryId());
    }
    mailLogRepository.save(mailLog);
    if (order != null) {
      syncQuoteOrder(order, QuoteMailStatus.SKIPPED, skipText(reason));
    }
    log.warn(
        "已报价发信跳过 quoteId={} reason={}", mailLog.getQuoteId(), reason);
  }

  private void syncQuoteOrder(QuoteOrder order, QuoteMailStatus status, String error) {
    order.setLastMailStatus(status);
    order.setLastMailAt(LocalDateTime.now());
    order.setLastMailError(abbreviate(error, 500));
    quoteOrderRepository.save(order);
  }

  private SysUser loadUser(Long userId) {
    return userId == null ? null : sysUserRepository.findById(userId).orElse(null);
  }

  /** SMTP 异常 → [错误码, 人话]。 */
  private String[] mapSmtpError(Exception ex) {
    String raw = ex.getMessage() == null ? "" : ex.getMessage();
    String code = null;
    for (String candidate : new String[] {"535", "550", "551", "552", "554", "421"}) {
      if (raw.contains(candidate)) {
        code = candidate;
        break;
      }
    }
    if (ex instanceof SendFailedException && code == null) {
      code = "SEND_FAILED";
    }
    String human =
        switch (code == null ? "" : code) {
          case "535" -> "发件邮箱账号或授权码错误，请检查 SMTP 配置";
          case "550", "551" -> "收件邮箱不存在或被对方拒收，请确认收件人邮箱";
          case "552" -> "收件方邮箱容量已满或被限制";
          case "554" -> "邮件被服务器判定为垃圾邮件而拒收";
          case "421" -> "邮件服务器暂时不可用，可稍后重发";
          case "SEND_FAILED" -> "邮件发送被服务器拒绝";
          default -> "邮件发送失败";
        };
    String message = code == null ? human : human + "（SMTP " + code + "）";
    if (StringUtils.hasText(raw)) {
      message = message + "：" + abbreviate(raw, 200);
    }
    return new String[] {code == null ? "SMTP_ERROR" : code, message};
  }

  private static String skipText(QuoteMailSkipReason reason) {
    return switch (reason) {
      case MAIL_DISABLED -> "系统未开启自动发信（quote.mail.enabled=false）";
      case NO_INQUIRY -> "报价单未关联询价单，无收件对象";
      case NO_OPERATOR_EMAIL -> "询价单操作人未绑定邮箱";
      case NO_FROM -> "未配置发件邮箱 / SMTP";
    };
  }

  private static String triggerLabel(QuoteMailTrigger trigger) {
    return switch (trigger) {
      case CONFIRM -> "确认报价";
      case APPROVE -> "审批通过";
      case REVISION -> "变更保存";
      case RESEND -> "人工重发";
      case MANUAL -> "人工发送";
    };
  }

  private static String abbreviate(String value, int max) {
    if (value == null) {
      return null;
    }
    String text = value.trim();
    return text.length() <= max ? text : text.substring(0, max);
  }

  private Optional<InquiryOrder> resolveInquiry(QuoteOrder order) {
    if (order.getInquiryId() != null) {
      Optional<InquiryOrder> byId =
          inquiryOrderRepository.findByIdAndDeletedAtIsNull(order.getInquiryId());
      if (byId.isPresent()) {
        return byId;
      }
    }
    return inquiryOrderRepository.findByQuoteIdAndDeletedAtIsNull(order.getId());
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  /** 异常摘要：message 可能为 null（如 List.of(null) 的 NPE），补上类名保证日志可读。 */
  private static String describeException(Exception ex) {
    String message = ex.getMessage();
    if (message == null || message.isBlank()) {
      return ex.getClass().getName() + "（无异常信息，详见后端日志）";
    }
    return ex.getClass().getSimpleName() + ": " + message;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      String trimmed = trimToNull(value);
      if (trimmed != null) {
        return trimmed;
      }
    }
    return null;
  }

  private static String sanitizeFileName(String quoteNo) {
    String raw = nullToEmpty(quoteNo);
    if (raw.isEmpty()) {
      return "unknown";
    }
    return raw.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  /** 邮件主题字段：空值用 —，并去掉换行以免破坏邮件头。 */
  private static String subjectPart(String value) {
    String text = trimToNull(value);
    if (text == null) {
      return "—";
    }
    return text.replaceAll("[\\r\\n\\t]+", " ").trim();
  }

  /** 独立 Bean，保证 @Async 经代理生效。 */
  @org.springframework.stereotype.Service
  public static class AsyncMailNotifier {

    private final QuoteQuotedMailService mailService;

    public AsyncMailNotifier(QuoteQuotedMailService mailService) {
      this.mailService = mailService;
    }

    @Async
    public void sendQuotedPdfAsync(
        Long quoteId, Long operatorId, String operatorName, QuoteMailTrigger trigger) {
      try {
        mailService.sendQuotedPdf(quoteId, operatorId, operatorName, trigger);
      } catch (Exception ex) {
        org.slf4j.LoggerFactory.getLogger(AsyncMailNotifier.class)
            .error("已报价邮件发送失败 quoteId={}: {}", quoteId, ex.getMessage(), ex);
      }
    }

    @Async
    public void sendQuotedPdfToAsync(
        Long quoteId,
        List<String> recipients,
        Long operatorId,
        String operatorName,
        QuoteMailTrigger trigger) {
      try {
        mailService.sendQuotedPdf(quoteId, recipients, operatorId, operatorName, trigger);
      } catch (Exception ex) {
        org.slf4j.LoggerFactory.getLogger(AsyncMailNotifier.class)
            .error("报价邮件发送失败 quoteId={}: {}", quoteId, ex.getMessage(), ex);
      }
    }
  }
}
