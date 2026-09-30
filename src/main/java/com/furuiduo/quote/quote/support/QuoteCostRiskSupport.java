package com.furuiduo.quote.quote.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.furuiduo.quote.quote.entity.QuoteCostType;

public final class QuoteCostRiskSupport {

  private static final List<String> MODE_ORDER = List.of("road", "sea", "fumigation");

  private QuoteCostRiskSupport() {}

  public static List<String> parseModes(String reason) {
    if (reason == null || reason.isBlank()) {
      return List.of();
    }
    Set<String> modes = new LinkedHashSet<>();
    for (String part : reason.split("[,;，；]+")) {
      String normalized = normalizeModeToken(part.trim());
      if (normalized != null) {
        modes.add(normalized);
      }
    }
    if (modes.isEmpty()) {
      String normalized = normalizeModeToken(reason.trim());
      if (normalized != null) {
        modes.add(normalized);
      }
    }
    List<String> ordered = new ArrayList<>();
    for (String mode : MODE_ORDER) {
      if (modes.contains(mode)) {
        ordered.add(mode);
      }
    }
    return ordered;
  }

  public static String fromCostType(QuoteCostType costType) {
    if (costType == null) {
      return null;
    }
    return switch (costType) {
      case ROAD -> "road";
      case SEA -> "sea";
      case FUMIGATION -> "fumigation";
    };
  }

  public static List<String> orderModes(Collection<String> modes) {
    if (modes == null || modes.isEmpty()) {
      return List.of();
    }
    List<String> ordered = new ArrayList<>();
    for (String mode : MODE_ORDER) {
      if (modes.contains(mode)) {
        ordered.add(mode);
      }
    }
    return ordered;
  }

  public static String mergeModes(String existing, String incomingMode) {
    if (incomingMode == null || incomingMode.isBlank()) {
      return existing;
    }
    String incoming = incomingMode.trim().toLowerCase(Locale.ROOT);
    Set<String> modes = new LinkedHashSet<>(parseModes(existing));
    if (modes.isEmpty() && existing != null && !existing.isBlank()) {
      return incoming;
    }
    modes.add(incoming);
    return String.join(",", MODE_ORDER.stream().filter(modes::contains).toList());
  }

  private static String normalizeModeToken(String token) {
    if (token == null || token.isBlank()) {
      return null;
    }
    String lower = token.toLowerCase(Locale.ROOT);
    if ("road".equals(lower)) {
      return "road";
    }
    if ("sea".equals(lower)) {
      return "sea";
    }
    if ("fumigation".equals(lower)) {
      return "fumigation";
    }
    if (token.contains("卡车")) {
      return "road";
    }
    if (token.contains("海运")) {
      return "sea";
    }
    if (token.contains("熏蒸")) {
      return "fumigation";
    }
    return null;
  }
}
