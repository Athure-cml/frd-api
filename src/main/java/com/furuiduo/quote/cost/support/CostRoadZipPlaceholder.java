package com.furuiduo.quote.cost.support;

/** 卡车成本库：导入阶段 ZIP 列占位提示（非真实邮编，校验时跳过主数据存在性检查）。 */
public final class CostRoadZipPlaceholder {

  /** City+State 对应多个邮编，需后续选定 */
  public static final String PENDING = "待补录";

  /** 主数据中未找到 City+State 对应邮编，City/State 可能有误 */
  public static final String CITY_STATE_INVALID = "CITY、STATE有误";

  /** 历史占位文案（迁移前数据兼容） */
  private static final String LEGACY_CITY_STATE_INVALID = "CITY、PA有误";

  private CostRoadZipPlaceholder() {}

  public static boolean isPending(String zipCode) {
    return zipCode != null && PENDING.equals(zipCode.trim());
  }

  public static boolean isCityStateInvalid(String zipCode) {
    if (zipCode == null) {
      return false;
    }
    String trimmed = zipCode.trim();
    return CITY_STATE_INVALID.equals(trimmed) || LEGACY_CITY_STATE_INVALID.equals(trimmed);
  }

  /** 跳过 ZIP 主数据存在性校验 */
  public static boolean skipsZipMasterCheck(String zipCode) {
    return isPending(zipCode) || isCityStateInvalid(zipCode);
  }

  /** 跳过 CITY 主数据存在性校验（City/State 本身可能有误） */
  public static boolean skipsCityMasterCheck(String zipCode) {
    return isCityStateInvalid(zipCode);
  }
}
