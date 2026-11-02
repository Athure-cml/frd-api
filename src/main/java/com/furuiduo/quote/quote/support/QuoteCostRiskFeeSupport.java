package com.furuiduo.quote.quote.support;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.quote.entity.QuoteCostType;

/**
 * 成本异常只盯报价会用到的关键价格字段：续期回填有效期等非价格变更不应触发。
 *
 * <ul>
 *   <li>卡车：ALL IN / ALL IN FM NON OAK / ALL IN FM OAK
 *   <li>海运：ALL IN（小计）
 *   <li>熏蒸：室外/室内 NON OAK、室外/室内 OAK
 * </ul>
 */
public final class QuoteCostRiskFeeSupport {

  private QuoteCostRiskFeeSupport() {}

  public static String roadSignature(CostRoad entity) {
    if (entity == null) {
      return "";
    }
    return roadSignature(entity.getAllInNoFm(), entity.getAllInFmOneWay(), entity.getAllInFmRound());
  }

  public static String roadSignature(
      BigDecimal allInNoFm, BigDecimal allInFmOneWay, BigDecimal allInFmRound) {
    return join(decimalKey(allInNoFm), decimalKey(allInFmOneWay), decimalKey(allInFmRound));
  }

  public static String seaSignature(CostSea entity) {
    if (entity == null) {
      return "";
    }
    return seaSignature(entity.getAllIn());
  }

  public static String seaSignature(BigDecimal allIn) {
    return decimalKey(allIn);
  }

  public static String fumigationSignature(CostFumigation entity) {
    if (entity == null) {
      return "";
    }
    return fumigationSignature(
        entity.getOutdoorNonOak(),
        entity.getOutdoorOak(),
        entity.getIndoorNonOak(),
        entity.getIndoorOak());
  }

  public static String fumigationSignature(
      BigDecimal outdoorNonOak,
      BigDecimal outdoorOak,
      BigDecimal indoorNonOak,
      BigDecimal indoorOak) {
    return join(
        decimalKey(outdoorNonOak),
        decimalKey(outdoorOak),
        decimalKey(indoorNonOak),
        decimalKey(indoorOak));
  }

  /** 从报价引入时的快照 JSON 提取费用签名，用于对照当前报价库有效价。 */
  public static String signatureFromSnapshot(QuoteCostType type, Map<String, Object> snapshot) {
    if (type == null || snapshot == null || snapshot.isEmpty()) {
      return "";
    }
    return switch (type) {
      case ROAD ->
          join(
              decimalKey(
                  firstDecimal(
                      snapshot.get("allInNoFm"),
                      snapshot.get("allInNonOak"),
                      snapshot.get("allIn"))),
              decimalKey(
                  firstDecimal(
                      snapshot.get("allInFmOneWay"),
                      snapshot.get("allInOak"),
                      snapshot.get("allIn"))),
              decimalKey(
                  firstDecimal(snapshot.get("allInFmRound"), snapshot.get("allIn"))));
      case SEA ->
          decimalKey(firstDecimal(snapshot.get("allIn"), snapshot.get("freight")));
      case FUMIGATION ->
          join(
              decimalKey(readDecimal(snapshot.get("outdoorNonOak"))),
              decimalKey(readDecimal(snapshot.get("outdoorOak"))),
              decimalKey(readDecimal(snapshot.get("indoorNonOak"))),
              decimalKey(readDecimal(snapshot.get("indoorOak"))));
    };
  }

  private static BigDecimal firstDecimal(Object... candidates) {
    for (Object candidate : candidates) {
      BigDecimal value = readDecimal(candidate);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  public static boolean changed(String before, String after) {
    return !Objects.equals(nullToEmpty(before), nullToEmpty(after));
  }

  private static String join(String... parts) {
    return String.join("|", parts);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String decimalKey(BigDecimal value) {
    if (value == null) {
      return "";
    }
    return value.stripTrailingZeros().toPlainString();
  }

  private static BigDecimal readDecimal(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof BigDecimal decimal) {
      return decimal;
    }
    if (raw instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    String text = String.valueOf(raw).trim();
    if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
      return null;
    }
    try {
      return new BigDecimal(text);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
