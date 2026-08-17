package com.furuiduo.quote.supplier.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.furuiduo.quote.supplier.entity.SupplierType;
import com.furuiduo.quote.supplier.repository.SupplierTypeRepository;

/** 其他供应商类型：types 存类型 ID 字符串。 */
public final class SupplierOtherTypes {

  private SupplierOtherTypes() {}

  public static List<String> normalizeIds(
      List<String> raw, SupplierTypeRepository typeRepository) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    Set<String> ordered = new LinkedHashSet<>();
    for (String item : raw) {
      if (item == null || item.isBlank()) {
        continue;
      }
      String token = item.trim();
      Long id;
      try {
        id = Long.parseLong(token);
      } catch (NumberFormatException ex) {
        // 兼容 Excel 按名称导入
        SupplierType byName =
            typeRepository
                .findByNameIgnoreCase(token)
                .orElseThrow(
                    () -> new IllegalArgumentException("无效的供应商类型: " + token));
        id = byName.getId();
      }
      if (!typeRepository.existsById(id)) {
        throw new IllegalArgumentException("无效的供应商类型: " + token);
      }
      ordered.add(String.valueOf(id));
    }
    return new ArrayList<>(ordered);
  }

  public static String toExcelValue(List<String> typeIds, Map<Long, String> idToName) {
    if (typeIds == null || typeIds.isEmpty()) {
      return "";
    }
    List<String> labels = new ArrayList<>();
    for (String idText : typeIds) {
      try {
        Long id = Long.parseLong(idText);
        String name = idToName.get(id);
        if (name != null) {
          labels.add(name);
        }
      } catch (NumberFormatException ignored) {
        // skip
      }
    }
    return String.join(",", labels);
  }

  public static List<String> parseExcelValue(
      String raw, SupplierTypeRepository typeRepository) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    String[] parts = raw.split("[,，;；、|/]");
    List<String> tokens = new ArrayList<>();
    for (String part : parts) {
      if (part != null && !part.isBlank()) {
        tokens.add(part.trim());
      }
    }
    return normalizeIds(tokens, typeRepository);
  }

  public static Map<Long, String> loadNameMap(SupplierTypeRepository typeRepository) {
    return typeRepository.findAll().stream()
        .collect(Collectors.toMap(SupplierType::getId, SupplierType::getName, (a, b) -> a));
  }

  public static String usageJson(Long typeId) {
    return "[\"" + typeId + "\"]";
  }
}
