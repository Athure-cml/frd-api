package com.furuiduo.quote.sys;

public final class QuoteLibraryTemplatePermissionCodes {

  private QuoteLibraryTemplatePermissionCodes() {}

  public static final String ROAD_VIEW = "quote:library:road:template:view";
  public static final String ROAD_EDIT = "quote:library:road:template:edit";
  public static final String ROAD_DELETE = "quote:library:road:template:delete";

  public static final String SEA_VIEW = "quote:library:sea:template:view";
  public static final String SEA_EDIT = "quote:library:sea:template:edit";
  public static final String SEA_DELETE = "quote:library:sea:template:delete";

  public static final String FUMIGATION_VIEW = "quote:library:fumigation:template:view";
  public static final String FUMIGATION_EDIT = "quote:library:fumigation:template:edit";
  public static final String FUMIGATION_DELETE = "quote:library:fumigation:template:delete";

  public static String view(String mode) {
    return switch (mode) {
      case "road" -> ROAD_VIEW;
      case "sea" -> SEA_VIEW;
      case "fumigation" -> FUMIGATION_VIEW;
      default -> throw new IllegalArgumentException("Invalid mode: " + mode);
    };
  }

  public static String edit(String mode) {
    return switch (mode) {
      case "road" -> ROAD_EDIT;
      case "sea" -> SEA_EDIT;
      case "fumigation" -> FUMIGATION_EDIT;
      default -> throw new IllegalArgumentException("Invalid mode: " + mode);
    };
  }

  public static String delete(String mode) {
    return switch (mode) {
      case "road" -> ROAD_DELETE;
      case "sea" -> SEA_DELETE;
      case "fumigation" -> FUMIGATION_DELETE;
      default -> throw new IllegalArgumentException("Invalid mode: " + mode);
    };
  }
}
