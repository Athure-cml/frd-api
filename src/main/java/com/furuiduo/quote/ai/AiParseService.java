package com.furuiduo.quote.ai;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.furuiduo.quote.ai.dto.AiParseRequest;
import com.furuiduo.quote.ai.dto.AiParseResponse;

@Service
public class AiParseService {

  private static final int MAX_SOURCE_CHARS = 12000;
  private static final int EXCERPT_CHARS = 500;

  private final AiClient aiClient;
  private final ObjectMapper objectMapper;

  public AiParseService(AiClient aiClient, ObjectMapper objectMapper) {
    this.aiClient = aiClient;
    this.objectMapper = objectMapper;
  }

  public AiParseResponse parseText(AiParseRequest request) {
    if (request == null || request.text() == null || request.text().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text 不能为空");
    }
    return parseSource(request.text(), request.hint());
  }

  public AiParseResponse parseFile(MultipartFile file, String hint) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传文件");
    }
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    try {
      String text;
      if (name.endsWith(".pdf")) {
        text = extractPdf(file.getBytes());
      } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
        text = extractExcel(file.getInputStream());
      } else if (name.endsWith(".txt")
          || name.endsWith(".md")
          || name.endsWith(".csv")
          || name.endsWith(".eml")
          || name.endsWith(".html")
          || name.endsWith(".htm")) {
        text = new String(file.getBytes(), StandardCharsets.UTF_8);
      } else {
        // 尝试当纯文本
        text = new String(file.getBytes(), StandardCharsets.UTF_8);
      }
      if (text == null || text.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未能从文件中提取文本");
      }
      return parseSource(text, hint);
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件解析失败：" + ex.getMessage(), ex);
    }
  }

  private AiParseResponse parseSource(String raw, String hint) {
    aiClient.ensureConfigured();
    String source = raw.length() > MAX_SOURCE_CHARS ? raw.substring(0, MAX_SOURCE_CHARS) : raw;
    String system =
        """
        你是物流报价信息抽取助手。根据用户提供的邮件/表格/文本，抽取结构化字段。
        只输出一个 JSON 对象，不要 Markdown。字段尽量包含：
        customerName, contact, por, pol, pod, containerType, quantity,
        cargoDesc, ssl, supplier, fumigationNeeded, validUntil, remark,
        estimatedFees（数组：{name, amount, currency, note}）。
        找不到的字段用 null。金额保持原文数字字符串。
        """;
    String user =
        (hint == null || hint.isBlank() ? "请抽取报价关键字段。" : hint.trim())
            + "\n\n===== 原文 =====\n"
            + source;

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", aiClient.model());
    body.put(
        "messages",
        List.of(
            Map.of("role", "system", "content", system),
            Map.of("role", "user", "content", user)));
    body.put("temperature", 0.1);

    JsonNode response = aiClient.chatCompletions(body);
    String content = response.path("choices").path(0).path("message").path("content").asText("");
    Map<String, Object> fields = parseJsonObject(content);
    String excerpt = source.length() > EXCERPT_CHARS ? source.substring(0, EXCERPT_CHARS) + "…" : source;
    return new AiParseResponse(fields, content, excerpt);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseJsonObject(String content) {
    if (content == null || content.isBlank()) {
      return Map.of();
    }
    String trimmed = content.trim();
    if (trimmed.startsWith("```")) {
      int firstNl = trimmed.indexOf('\n');
      int lastFence = trimmed.lastIndexOf("```");
      if (firstNl > 0 && lastFence > firstNl) {
        trimmed = trimmed.substring(firstNl + 1, lastFence).trim();
      }
    }
    try {
      JsonNode node = objectMapper.readTree(trimmed);
      if (node.isObject()) {
        return objectMapper.convertValue(node, Map.class);
      }
    } catch (Exception ignored) {
      // fall through
    }
    Map<String, Object> fallback = new LinkedHashMap<>();
    fallback.put("_raw", content);
    return fallback;
  }

  private static String extractPdf(byte[] bytes) throws Exception {
    try (PDDocument doc = Loader.loadPDF(bytes)) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(doc);
    }
  }

  private static String extractExcel(InputStream in) throws Exception {
    DataFormatter formatter = new DataFormatter();
    StringBuilder sb = new StringBuilder();
    try (Workbook workbook = WorkbookFactory.create(in)) {
      int sheets = Math.min(workbook.getNumberOfSheets(), 3);
      for (int s = 0; s < sheets; s++) {
        Sheet sheet = workbook.getSheetAt(s);
        sb.append("## Sheet: ").append(sheet.getSheetName()).append('\n');
        int maxRow = Math.min(sheet.getLastRowNum(), 80);
        for (int r = 0; r <= maxRow; r++) {
          Row row = sheet.getRow(r);
          if (row == null) {
            continue;
          }
          StringBuilder line = new StringBuilder();
          short last = row.getLastCellNum();
          for (int c = 0; c < last && c < 30; c++) {
            Cell cell = row.getCell(c);
            String v = cell == null ? "" : formatter.formatCellValue(cell).trim();
            if (!v.isEmpty()) {
              if (!line.isEmpty()) {
                line.append('\t');
              }
              line.append(v);
            }
          }
          if (!line.isEmpty()) {
            sb.append(line).append('\n');
          }
        }
      }
    }
    return sb.toString();
  }
}
