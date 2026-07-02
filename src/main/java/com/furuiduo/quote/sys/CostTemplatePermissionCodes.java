package com.furuiduo.quote.sys;

public final class CostTemplatePermissionCodes {

  private CostTemplatePermissionCodes() {}

  public static final String ROAD_VIEW = "cost:road:template:view";
  public static final String ROAD_EDIT = "cost:road:template:edit";
  public static final String ROAD_DELETE = "cost:road:template:delete";

  public static final String SEA_VIEW = "cost:sea:template:view";
  public static final String SEA_EDIT = "cost:sea:template:edit";
  public static final String SEA_DELETE = "cost:sea:template:delete";

  public static final String FUMIGATION_VIEW = "cost:fumigation:template:view";
  public static final String FUMIGATION_EDIT = "cost:fumigation:template:edit";
  public static final String FUMIGATION_DELETE = "cost:fumigation:template:delete";

  /** @deprecated */
  public static final String RAIL_VIEW = "cost:rail:template:view";
  /** @deprecated */
  public static final String RAIL_EDIT = "cost:rail:template:edit";
  /** @deprecated */
  public static final String RAIL_DELETE = "cost:rail:template:delete";

  public static String view(String mode) {
    return switch (mode) {
      case "road" -> ROAD_VIEW;
      case "sea" -> SEA_VIEW;
      case "fumigation" -> FUMIGATION_VIEW;
      case "rail" -> RAIL_VIEW;
      default -> throw new IllegalArgumentException("Invalid mode: " + mode);
    };
  }

  public static String edit(String mode) {
    return switch (mode) {
      case "road" -> ROAD_EDIT;
      case "sea" -> SEA_EDIT;
      case "fumigation" -> FUMIGATION_EDIT;
      case "rail" -> RAIL_EDIT;
      default -> throw new IllegalArgumentException("Invalid mode: " + mode);
    };
  }

  public static String delete(String mode) {
    return switch (mode) {
      case "road" -> ROAD_DELETE;
      case "sea" -> SEA_DELETE;
      case "fumigation" -> FUMIGATION_DELETE;
      case "rail" -> RAIL_DELETE;
      default -> throw new IllegalArgumentException("Invalid mode: " + mode);
    };
  }

  public static String[] all() {
    return new String[] {
      ROAD_VIEW,
      ROAD_EDIT,
      ROAD_DELETE,
      SEA_VIEW,
      SEA_EDIT,
      SEA_DELETE,
      FUMIGATION_VIEW,
      FUMIGATION_EDIT,
      FUMIGATION_DELETE
    };
  }
}
