package com.furuiduo.quote.cost.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.furuiduo.quote.cost.dto.CostTableCustomFieldDef;
import com.furuiduo.quote.cost.dto.CostTableFieldOverride;
import com.furuiduo.quote.cost.dto.CostTableTemplateGroup;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;

public final class CostTemplateLayoutTools {

  private CostTemplateLayoutTools() {}

  public static CostTableTemplateLayout normalize(CostTableTemplateLayout layout, String mode) {
    if (layout == null) {
      return null;
    }
    CostTableTemplateLayout result = clearRequiredFlags(stripAsteriskTitles(layout));
    if ("road".equals(mode)) {
      result = insertZipCodeIfMissing(result);
    }
    return result;
  }

  public static CostTableTemplateLayout clearRequiredFlags(CostTableTemplateLayout layout) {
    Map<String, CostTableFieldOverride> overrides = layout.fieldOverrides();
    boolean overrideNeedsClear =
        overrides != null
            && overrides.values().stream()
                .anyMatch(item -> item != null && Boolean.TRUE.equals(item.required()));

    boolean customNeedsClear =
        layout.customFields() != null
            && layout.customFields().stream()
                .anyMatch(item -> item != null && Boolean.TRUE.equals(item.required()));

    if (!overrideNeedsClear && !customNeedsClear) {
      return layout;
    }

    Map<String, CostTableFieldOverride> clearedOverrides = overrides;
    if (overrideNeedsClear && overrides != null) {
      clearedOverrides = new LinkedHashMap<>();
      for (Map.Entry<String, CostTableFieldOverride> entry : overrides.entrySet()) {
        CostTableFieldOverride override = entry.getValue();
        if (override == null) {
          continue;
        }
        clearedOverrides.put(
            entry.getKey(),
            new CostTableFieldOverride(
                override.visible(),
                override.width(),
                override.minWidth(),
                override.fixed(),
                override.title(),
                Boolean.FALSE,
                override.align()));
      }
    }

    List<CostTableCustomFieldDef> clearedCustomFields = layout.customFields();
    if (customNeedsClear && layout.customFields() != null) {
      clearedCustomFields = new ArrayList<>();
      for (CostTableCustomFieldDef custom : layout.customFields()) {
        clearedCustomFields.add(
            new CostTableCustomFieldDef(
                custom.field(), custom.title(), Boolean.FALSE, custom.dataType()));
      }
    }

    return new CostTableTemplateLayout(
        layout.groups(),
        layout.fields(),
        clearedOverrides,
        layout.fieldOrder(),
        clearedCustomFields);
  }

  public static CostTableTemplateLayout stripAsteriskTitles(CostTableTemplateLayout layout) {
    Map<String, CostTableFieldOverride> overrides = layout.fieldOverrides();
    if (overrides == null || overrides.isEmpty()) {
      return layout;
    }

    Map<String, CostTableFieldOverride> stripped = new LinkedHashMap<>();
    boolean changed = false;
    for (Map.Entry<String, CostTableFieldOverride> entry : overrides.entrySet()) {
      CostTableFieldOverride override = entry.getValue();
      if (override == null) {
        continue;
      }
      String title = override.title();
      String normalizedTitle = stripLeadingAsterisk(title);
      if (!Objects.equals(title, normalizedTitle)) {
        changed = true;
        stripped.put(
            entry.getKey(),
            new CostTableFieldOverride(
                override.visible(),
                override.width(),
                override.minWidth(),
                override.fixed(),
                normalizedTitle,
                override.required(),
                override.align()));
      } else {
        stripped.put(entry.getKey(), override);
      }
    }

    if (!changed) {
      return layout;
    }

    return new CostTableTemplateLayout(
        layout.groups(),
        layout.fields(),
        stripped,
        layout.fieldOrder(),
        layout.customFields());
  }

  public static CostTableTemplateLayout insertZipCodeIfMissing(CostTableTemplateLayout layout) {
    List<String> keys = CostFieldCatalog.resolveFieldKeys(layout);
    if (keys.contains("zipCode")) {
      return layout;
    }
    if (!keys.contains("logYardNameAddress")) {
      return layout;
    }

    List<CostTableTemplateGroup> groups = insertIntoGroups(layout.groups());
    List<String> fieldOrder = insertAfter(layout.fieldOrder(), "logYardNameAddress", "zipCode");
    List<String> fields = insertAfter(layout.fields(), "logYardNameAddress", "zipCode");

    return new CostTableTemplateLayout(
        groups,
        fields,
        layout.fieldOverrides(),
        fieldOrder,
        layout.customFields());
  }

  private static List<CostTableTemplateGroup> insertIntoGroups(
      List<CostTableTemplateGroup> groups) {
    if (groups == null || groups.isEmpty()) {
      return groups;
    }
    List<CostTableTemplateGroup> updated = new ArrayList<>();
    for (CostTableTemplateGroup group : groups) {
      List<String> groupFields = insertAfter(group.fields(), "logYardNameAddress", "zipCode");
      updated.add(
          new CostTableTemplateGroup(
              group.key(), group.labelKey(), group.headerClassName(), groupFields));
    }
    return updated;
  }

  private static List<String> insertAfter(
      List<String> source, String anchor, String inserted) {
    if (source == null || source.isEmpty()) {
      return source;
    }
    if (source.contains(inserted)) {
      return source;
    }
    int index = source.indexOf(anchor);
    if (index < 0) {
      return source;
    }
    List<String> result = new ArrayList<>(source);
    result.add(index + 1, inserted);
    return result;
  }

  private static String stripLeadingAsterisk(String title) {
    if (title == null || title.isBlank()) {
      return title;
    }
    return title.startsWith("*") ? title.substring(1).trim() : title;
  }
}
