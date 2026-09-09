package com.furuiduo.quote.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 单次导入会话内的编码占用表：库中已有编码 + 本文件已声明/已分配编码，避免「Excel 编码未入库却与当次生成的编码
 * 撞号」导致误更新。
 */
public final class PartyImportContext {

  private final Set<String> dbCodes;
  private final Set<String> sessionCodes = new HashSet<>();

  private PartyImportContext(Set<String> dbCodes) {
    this.dbCodes = dbCodes;
  }

  public static PartyImportContext fromDbCodes(Collection<String> codes) {
    Set<String> normalized = new HashSet<>();
    if (codes != null) {
      for (String code : codes) {
        String value = normalize(code);
        if (value != null) {
          normalized.add(value);
        }
      }
    }
    return new PartyImportContext(normalized);
  }

  public boolean isPreExisting(String normalizedCode) {
    return normalizedCode != null && dbCodes.contains(normalizedCode);
  }

  /** 校验阶段：登记本文件中的编码，重复则返回错误文案。 */
  public String reserveFileCode(String normalizedCode) {
    if (normalizedCode == null) {
      return null;
    }
    if (!sessionCodes.add(normalizedCode)) {
      return "编码与文件中其他行重复：" + normalizedCode;
    }
    return null;
  }

  public boolean isSessionReserved(String normalizedCode) {
    return normalizedCode != null && sessionCodes.contains(normalizedCode);
  }

  /** 保存阶段：为本行分配生成编码，跳过本次导入已占用编码。 */
  public String allocateGenerated(Supplier<String> generator) {
    for (int attempt = 0; attempt < 20_000; attempt++) {
      String candidate = normalize(generator.get());
      if (candidate == null) {
        continue;
      }
      if (sessionCodes.add(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("无法分配唯一编码，请稍后重试");
  }

  /** 保存阶段：新建行使用 Excel 指定编码（已通过校验且不在库中）。 */
  public void reserveAssignedCode(String normalizedCode) {
    if (normalizedCode == null) {
      return;
    }
    sessionCodes.add(normalizedCode);
  }

  private static String normalize(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return raw.trim().toUpperCase(Locale.ROOT);
  }
}
