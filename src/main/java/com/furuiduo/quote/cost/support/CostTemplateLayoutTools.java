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

  private static final Map<String, String> SEA_CUSTOM_FIELD_RENAMES =
      Map.of(
          "cf_seaFreightEff", "cf_sea_freight_eff",
          "cf_seaBunkerEff", "cf_sea_bunker_eff",
          "cf_seaOthersEff", "cf_sea_others_eff");

  private static final java.util.Set<String> SEA_SURCHARGE_FIELDS =
      java.util.Set.of(
          "buc",
          "cf_sea_bunker_eff",
          "bucValidDate",
          "ebs",
          "ebsValidDate",
          "gri",
          "griValidDate",
          "others",
          "cf_sea_others_eff",
          "othersValidDate");

  private static final java.util.Set<String> FUM_OUTDOOR_FIELDS =
      java.util.Set.of(
          "outdoorNonOak", "outdoorOak", "cf_fum_outdoor_eff", "outdoorValidity");

  private static final java.util.Set<String> FUM_INDOOR_FIELDS =
      java.util.Set.of(
          "indoorNonOak", "indoorOak", "cf_fum_indoor_eff", "indoorValidity");

  private CostTemplateLayoutTools() {}

  public static CostTableTemplateLayout normalize(CostTableTemplateLayout layout, String mode) {
    if (layout == null) {
      return null;
    }
    CostTableTemplateLayout next = stripAsteriskTitles(layout);
    if ("sea".equals(mode)) {
      next = migrateSeaCustomFieldCodes(next);
      next = ensureSeaSurchargeGroupIncludesEffDates(next);
    }
    if ("fumigation".equals(mode)) {
      next = ensureFumigationGroupsIncludeEffDates(next);
    }
    if ("road".equals(mode)) {
      next = ensureRoadFeeUnitFields(next);
    }
    return next;
  }

  private record RoadFeeUnitPair(String amountField, String unitField, String unitTitle) {}

  private static final List<RoadFeeUnitPair> ROAD_FEE_UNIT_PAIRS =
      List.of(
          new RoadFeeUnitPair("waitingFee", "cf_road_waiting_unit", "WAITING UNIT"),
          new RoadFeeUnitPair(
              "cf_road_yard_storage", "cf_road_yard_storage_unit", "YARD STORAGE UNIT"),
          new RoadFeeUnitPair(
              "cf_road_extra_chassis", "cf_road_extra_chassis_unit", "EXTRA CHASSIS UNIT"));

  /**
   * 为 WAITING / YARD STORAGE / EXTRA CHASSIS 补齐单位列。 按字段 key 配对插入，不依赖原顺序相邻；
   * 若金额列存在而单位列缺失，则插在金额列之后（中间有别的字段也不影响识别）。
   */
  public static CostTableTemplateLayout ensureRoadFeeUnitFields(CostTableTemplateLayout layout) {
    if (layout == null) {
      return null;
    }
    List<String> order = new ArrayList<>(CostFieldCatalog.resolveFieldKeys(layout));
    Map<String, CostTableCustomFieldDef> customByField = new LinkedHashMap<>();
    if (layout.customFields() != null) {
      for (CostTableCustomFieldDef def : layout.customFields()) {
        if (def != null && def.field() != null) {
          customByField.put(def.field(), def);
        }
      }
    }
    Map<String, CostTableFieldOverride> overrides =
        layout.fieldOverrides() == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(layout.fieldOverrides());

    boolean changed = false;
    for (RoadFeeUnitPair pair : ROAD_FEE_UNIT_PAIRS) {
      if (!order.contains(pair.amountField())) {
        continue;
      }
      if (!order.contains(pair.unitField())) {
        int amountIndex = order.indexOf(pair.amountField());
        order.add(amountIndex + 1, pair.unitField());
        changed = true;
      }
      CostTableCustomFieldDef existing = customByField.get(pair.unitField());
      if (existing == null) {
        customByField.put(
            pair.unitField(),
            new CostTableCustomFieldDef(pair.unitField(), pair.unitTitle(), null, "text"));
        changed = true;
      } else if (existing.title() == null || existing.title().isBlank()) {
        customByField.put(
            pair.unitField(),
            new CostTableCustomFieldDef(
                pair.unitField(),
                pair.unitTitle(),
                existing.required(),
                existing.dataType() == null ? "text" : existing.dataType()));
        changed = true;
      }
      CostTableFieldOverride override = overrides.get(pair.unitField());
      if (override == null || override.title() == null || override.title().isBlank()) {
        overrides.put(
            pair.unitField(),
            mergeOverrideTitle(override, pair.unitTitle()));
        changed = true;
      }
    }

    if (!changed) {
      return layout;
    }
    List<CostTableCustomFieldDef> nextCustom = new ArrayList<>(customByField.values());
    return new CostTableTemplateLayout(
        layout.groups(), order, overrides, order, nextCustom);
  }

  private static CostTableFieldOverride mergeOverrideTitle(
      CostTableFieldOverride override, String title) {
    if (override == null) {
      return new CostTableFieldOverride(null, null, null, null, title, null, null, null, null);
    }
    return new CostTableFieldOverride(
        override.visible(),
        override.width(),
        override.minWidth(),
        override.fixed(),
        title,
        override.required(),
        override.align(),
        override.bgColor(),
        override.sortable());
  }

  /** 旧版驼峰自定义字段码 → 合规小写码（cf_[a-z0-9_]） */
  public static CostTableTemplateLayout migrateSeaCustomFieldCodes(CostTableTemplateLayout layout) {
    if (layout == null) {
      return null;
    }
    boolean changed = false;

    List<CostTableCustomFieldDef> customFields = layout.customFields();
    List<CostTableCustomFieldDef> nextCustom = null;
    if (customFields != null && !customFields.isEmpty()) {
      nextCustom = new ArrayList<>(customFields.size());
      for (CostTableCustomFieldDef def : customFields) {
        String renamed = renameSeaCustomField(def.field());
        if (!Objects.equals(renamed, def.field())) {
          changed = true;
          nextCustom.add(
              new CostTableCustomFieldDef(renamed, def.title(), def.required(), def.dataType()));
        } else {
          nextCustom.add(def);
        }
      }
    }

    List<String> fieldOrder = renameSeaFieldList(layout.fieldOrder());
    List<String> fields = renameSeaFieldList(layout.fields());
    if (!Objects.equals(fieldOrder, layout.fieldOrder()) || !Objects.equals(fields, layout.fields())) {
      changed = true;
    }

    Map<String, CostTableFieldOverride> overrides = layout.fieldOverrides();
    Map<String, CostTableFieldOverride> nextOverrides = overrides;
    if (overrides != null && !overrides.isEmpty()) {
      nextOverrides = new LinkedHashMap<>();
      for (Map.Entry<String, CostTableFieldOverride> entry : overrides.entrySet()) {
        String renamed = renameSeaCustomField(entry.getKey());
        if (!Objects.equals(renamed, entry.getKey())) {
          changed = true;
        }
        nextOverrides.put(renamed, entry.getValue());
      }
    }

    List<CostTableTemplateGroup> groups = layout.groups();
    List<CostTableTemplateGroup> nextGroups = groups;
    if (groups != null && !groups.isEmpty()) {
      nextGroups = new ArrayList<>(groups.size());
      for (CostTableTemplateGroup group : groups) {
        List<String> groupFields = renameSeaFieldList(group.fields());
        if (!Objects.equals(groupFields, group.fields())) {
          changed = true;
        }
        nextGroups.add(
            new CostTableTemplateGroup(
                group.key(), group.labelKey(), group.headerClassName(), groupFields));
      }
    }

    if (!changed) {
      return layout;
    }
    return new CostTableTemplateLayout(
        nextGroups, fields, nextOverrides, fieldOrder, nextCustom);
  }

  /**
   * 附加费一级表头需覆盖燃油/OTHERS 及其生效期、有效期；旧布局漏了生效期自定义列会导致表头断裂。
   */
  public static CostTableTemplateLayout ensureSeaSurchargeGroupIncludesEffDates(
      CostTableTemplateLayout layout) {
    if (layout == null || layout.groups() == null || layout.groups().isEmpty()) {
      return layout;
    }
    List<String> order = CostFieldCatalog.resolveFieldKeys(layout);
    List<String> surchargeFields =
        order.stream().filter(SEA_SURCHARGE_FIELDS::contains).toList();
    if (surchargeFields.isEmpty()) {
      return layout;
    }

    boolean changed = false;
    List<CostTableTemplateGroup> nextGroups = new ArrayList<>();
    boolean replaced = false;
    for (CostTableTemplateGroup group : layout.groups()) {
      if ("surcharge".equals(group.key())) {
        if (!Objects.equals(group.fields(), surchargeFields)) {
          changed = true;
        }
        nextGroups.add(
            new CostTableTemplateGroup(
                group.key(),
                group.labelKey(),
                group.headerClassName() == null ? "sea-header-surcharge" : group.headerClassName(),
                surchargeFields));
        replaced = true;
      } else {
        nextGroups.add(group);
      }
    }
    if (!replaced) {
      nextGroups.add(
          new CostTableTemplateGroup(
              "surcharge",
              "page.costLibrary.seaGroups.surcharge",
              "sea-header-surcharge",
              surchargeFields));
      changed = true;
    }
    if (!changed) {
      return layout;
    }
    return new CostTableTemplateLayout(
        nextGroups,
        layout.fields(),
        layout.fieldOverrides(),
        layout.fieldOrder(),
        layout.customFields());
  }

  /**
   * FM-OUTDOOR / FM-INDOOR 一级表头需覆盖价格、生效期、有效期；旧布局漏了生效期会导致表头断裂。
   */
  public static CostTableTemplateLayout ensureFumigationGroupsIncludeEffDates(
      CostTableTemplateLayout layout) {
    if (layout == null || layout.groups() == null || layout.groups().isEmpty()) {
      return layout;
    }
    List<String> order = CostFieldCatalog.resolveFieldKeys(layout);
    List<String> outdoorFields =
        order.stream().filter(FUM_OUTDOOR_FIELDS::contains).toList();
    List<String> indoorFields =
        order.stream().filter(FUM_INDOOR_FIELDS::contains).toList();
    if (outdoorFields.isEmpty() && indoorFields.isEmpty()) {
      return layout;
    }

    boolean changed = false;
    List<CostTableTemplateGroup> nextGroups = new ArrayList<>();
    boolean hasOutdoor = false;
    boolean hasIndoor = false;
    for (CostTableTemplateGroup group : layout.groups()) {
      if ("outdoor".equals(group.key()) && !outdoorFields.isEmpty()) {
        if (!Objects.equals(group.fields(), outdoorFields)) {
          changed = true;
        }
        nextGroups.add(
            new CostTableTemplateGroup(
                group.key(), group.labelKey(), group.headerClassName(), outdoorFields));
        hasOutdoor = true;
      } else if ("indoor".equals(group.key()) && !indoorFields.isEmpty()) {
        if (!Objects.equals(group.fields(), indoorFields)) {
          changed = true;
        }
        nextGroups.add(
            new CostTableTemplateGroup(
                group.key(), group.labelKey(), group.headerClassName(), indoorFields));
        hasIndoor = true;
      } else {
        nextGroups.add(group);
      }
    }
    if (!hasOutdoor && !outdoorFields.isEmpty()) {
      nextGroups.add(
          new CostTableTemplateGroup(
              "outdoor",
              "page.costLibrary.fumigationGroups.outdoor",
              null,
              outdoorFields));
      changed = true;
    }
    if (!hasIndoor && !indoorFields.isEmpty()) {
      nextGroups.add(
          new CostTableTemplateGroup(
              "indoor",
              "page.costLibrary.fumigationGroups.indoor",
              null,
              indoorFields));
      changed = true;
    }
    if (!changed) {
      return layout;
    }
    return new CostTableTemplateLayout(
        nextGroups,
        layout.fields(),
        layout.fieldOverrides(),
        layout.fieldOrder(),
        layout.customFields());
  }

  private static String renameSeaCustomField(String field) {
    if (field == null) {
      return null;
    }
    return SEA_CUSTOM_FIELD_RENAMES.getOrDefault(field, field);
  }

  private static List<String> renameSeaFieldList(List<String> source) {
    if (source == null) {
      return null;
    }
    List<String> next = new ArrayList<>(source.size());
    boolean changed = false;
    for (String field : source) {
      String renamed = renameSeaCustomField(field);
      if (!Objects.equals(renamed, field)) {
        changed = true;
      }
      next.add(renamed);
    }
    return changed ? next : source;
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
                override.align(),
                override.bgColor(),
                override.sortable()));
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
