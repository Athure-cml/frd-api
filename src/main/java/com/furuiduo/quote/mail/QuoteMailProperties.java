package com.furuiduo.quote.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quote.mail")
public class QuoteMailProperties {

  /** 总开关；未配 SMTP 账号时即便为 true 也会跳过发送 */
  private boolean enabled = false;

  private String from = "";

  private String fromName = "FRD GLOBAL (SH) CO., LTD.";

  private String subjectPrefix = "[福瑞多报价]";

  /** PDF 中文字体路径（ttf/ttc）；空则尝试常见系统字体 */
  private String pdfFontPath = "";

  /** Chrome/Edge 可执行文件路径；空则自动探测，用于生成与打印预览一致的 PDF */
  private String chromePath = "";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getFromName() {
    return fromName;
  }

  public void setFromName(String fromName) {
    this.fromName = fromName;
  }

  public String getSubjectPrefix() {
    return subjectPrefix;
  }

  public void setSubjectPrefix(String subjectPrefix) {
    this.subjectPrefix = subjectPrefix;
  }

  public String getPdfFontPath() {
    return pdfFontPath;
  }

  public void setPdfFontPath(String pdfFontPath) {
    this.pdfFontPath = pdfFontPath;
  }

  public String getChromePath() {
    return chromePath;
  }

  public void setChromePath(String chromePath) {
    this.chromePath = chromePath;
  }
}
