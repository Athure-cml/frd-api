package com.furuiduo.quote.sys.support;

/** 历史报价分析页已下线，统一落到工作台。 */
public final class HomePathSupport {

  public static final String WORKSPACE = "/workspace";

  private HomePathSupport() {}

  public static String resolve(String homePath) {
    if (homePath == null || homePath.isBlank()) {
      return WORKSPACE;
    }
    String path = homePath.trim();
    if ("/analytics".equals(path) || "/dashboard/analytics".equals(path)) {
      return WORKSPACE;
    }
    return path;
  }
}
