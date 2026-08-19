package com.furuiduo.quote.sys;

import java.util.Locale;

/** 供应商按分类的权限码：supplier:{truck|fumigation|yard|other}:{view|create|edit|delete} */
public final class SupplierPermissionCodes {

  public static final String TRUCK = "truck";
  public static final String FUMIGATION = "fumigation";
  public static final String YARD = "yard";
  public static final String OTHER = "other";

  public static final String[] CATEGORIES = {TRUCK, FUMIGATION, YARD, OTHER};
  public static final String[] ACTIONS = {"view", "create", "edit", "delete"};

  private SupplierPermissionCodes() {}

  public static String normalizeCategory(String category) {
    if (category == null || category.isBlank()) {
      return TRUCK;
    }
    String code = category.trim().toLowerCase(Locale.ROOT);
    return switch (code) {
      case "truck", "fumigation", "yard", "other" -> code;
      default -> TRUCK;
    };
  }

  public static String of(String category, String action) {
    return "supplier:" + normalizeCategory(category) + ":" + action;
  }

  public static String view(String category) {
    return of(category, "view");
  }

  public static String create(String category) {
    return of(category, "create");
  }

  public static String edit(String category) {
    return of(category, "edit");
  }

  public static String delete(String category) {
    return of(category, "delete");
  }

  public static String[] allViews() {
    return new String[] {
      view(TRUCK), view(FUMIGATION), view(YARD), view(OTHER)
    };
  }

  public static String[] all() {
    String[] codes = new String[CATEGORIES.length * ACTIONS.length];
    int i = 0;
    for (String category : CATEGORIES) {
      for (String action : ACTIONS) {
        codes[i++] = of(category, action);
      }
    }
    return codes;
  }
}
