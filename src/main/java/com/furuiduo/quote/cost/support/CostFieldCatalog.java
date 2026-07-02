package com.furuiduo.quote.cost.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.furuiduo.quote.cost.dto.CostTableCustomFieldDef;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;

public final class CostFieldCatalog {

  private CostFieldCatalog() {}

  private static final Set<String> ROAD_FIELDS =
      Set.of(
          "validDate",
          "supplier",
          "logYardNameAddress",
          "city",
          "state",
          "por",
          "pol",
          "baseFreight",
          "fsc",
          "chassis",
          "owTriAxle",
          "split",
          "stopOff",
          "allIn",
          "allInNonOak",
          "allInOak",
          "waitingFee",
          "redelivery",
          "prepull",
          "nsLift",
          "remark");

  private static final Set<String> SEA_FIELDS =
      Set.of(
          "origin",
          "destination",
          "unitPrice",
          "buc",
          "surchargeValidDate",
          "allIn",
          "carrier",
          "remark",
          "validDate",
          "status");

  private static final Set<String> FUMIGATION_FIELDS =
      Set.of(
          "port",
          "station",
          "nonOakOutdoor",
          "nonOakIndoor",
          "nonOakQuoteSummer",
          "nonOakQuoteWinter",
          "oakOutdoor",
          "oakIndoor",
          "oakQuoteSummer",
          "oakQuoteWinter",
          "remark",
          "updatedAt");

  private static final Set<String> RAIL_FIELDS =
      Set.of(
          "origin",
          "destination",
          "carrier",
          "spec",
          "unit",
          "unitPrice",
          "currency",
          "validFrom",
          "validTo",
          "status",
          "remark",
          "updatedAt");

  public static Set<String> fieldsForMode(String mode) {
    return switch (mode) {
      case "road" -> ROAD_FIELDS;
      case "sea" -> SEA_FIELDS;
      case "fumigation" -> FUMIGATION_FIELDS;
      case "rail" -> RAIL_FIELDS;
      default -> Set.of();
    };
  }

  public static void validateLayout(String mode, CostTableTemplateLayout layout) {
    if (layout == null) {
      throw new IllegalArgumentException("layout is required");
    }

    Set<String> allowed = new LinkedHashSet<>(fieldsForMode(mode));
    if (layout.customFields() != null) {
      for (CostTableCustomFieldDef custom : layout.customFields()) {
        validateCustomFieldDef(custom);
        if (!allowed.add(custom.field())) {
          throw new IllegalArgumentException("Duplicate custom field: " + custom.field());
        }
      }
    }

    List<String> ordered = resolveFieldKeys(layout);
    if (ordered.isEmpty()) {
      throw new IllegalArgumentException("layout must contain at least one field");
    }

    Set<String> used = new LinkedHashSet<>();
    for (String field : ordered) {
      assertField(mode, allowed, used, field);
    }
  }

  public static List<String> resolveFieldKeys(CostTableTemplateLayout layout) {
    if (layout.fieldOrder() != null && !layout.fieldOrder().isEmpty()) {
      return List.copyOf(layout.fieldOrder());
    }
    if (layout.fields() != null && !layout.fields().isEmpty()) {
      return List.copyOf(layout.fields());
    }
    if (layout.groups() != null && !layout.groups().isEmpty()) {
      List<String> keys = new ArrayList<>();
      layout.groups().forEach(group -> keys.addAll(group.fields()));
      return keys;
    }
    return List.of();
  }

  private static void validateCustomFieldDef(CostTableCustomFieldDef def) {
    if (def == null) {
      throw new IllegalArgumentException("custom field cannot be null");
    }
    if (def.field() == null || !def.field().matches("cf_[a-z0-9_]{2,48}")) {
      throw new IllegalArgumentException("custom field code must match cf_[a-z0-9_]{2,48}");
    }
    if (def.title() == null || def.title().isBlank()) {
      throw new IllegalArgumentException("custom field title is required");
    }
    if (def.dataType() != null
        && !def.dataType().equals("text")
        && !def.dataType().equals("number")) {
      throw new IllegalArgumentException("custom field dataType must be text or number");
    }
  }

  private static void assertField(
      String mode, Set<String> allowed, Set<String> used, String field) {
    if (field == null || field.isBlank()) {
      throw new IllegalArgumentException("field name cannot be blank");
    }
    String normalized = field.trim();
    if (!allowed.contains(normalized)) {
      throw new IllegalArgumentException("Unknown field for mode " + mode + ": " + normalized);
    }
    if (!used.add(normalized)) {
      throw new IllegalArgumentException("Duplicate field in layout: " + normalized);
    }
  }

  public static String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code is required");
    }
    String normalized = code.trim().toLowerCase(Locale.ROOT);
    if (!normalized.matches("[a-z0-9_]{2,64}")) {
      throw new IllegalArgumentException("code must be 2-64 chars: lowercase letters, digits, underscore");
    }
    return normalized;
  }

  public static String normalizeCustomFieldCode(String code) {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("field code is required");
    }
    String normalized = code.trim().toLowerCase(Locale.ROOT);
    if (!normalized.matches("cf_[a-z0-9_]{2,48}")) {
      throw new IllegalArgumentException("custom field code must match cf_[a-z0-9_]{2,48}");
    }
    return normalized;
  }
}
