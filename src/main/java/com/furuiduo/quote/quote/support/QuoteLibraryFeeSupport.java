package com.furuiduo.quote.quote.support;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;

/** 报价库可编辑费用字段及成本原价下限。 */
public final class QuoteLibraryFeeSupport {

  public static final List<String> ROAD_FEE_FIELDS =
      List.of(
          "allInNoFm",
          "allInFmOneWay",
          "allInFmRound",
          "nsLift",
          "chassis",
          "waitingFee",
          "redelivery");

  public static final List<String> SEA_FEE_FIELDS = List.of("allIn", "freight");

  public static final List<String> FUMIGATION_FEE_FIELDS =
      List.of("outdoorNonOak", "outdoorOak", "indoorNonOak", "indoorOak");

  private QuoteLibraryFeeSupport() {}

  public static List<String> editableFields(CostHighlightMode mode) {
    return switch (mode) {
      case road -> ROAD_FEE_FIELDS;
      case sea -> SEA_FEE_FIELDS;
      case fumigation -> FUMIGATION_FEE_FIELDS;
    };
  }

  public static Map<String, BigDecimal> roadCostFloors(CostRoad entity) {
    Map<String, BigDecimal> floors = new LinkedHashMap<>();
    if (entity == null) {
      return floors;
    }
    putFloor(floors, "allInNoFm", entity.getAllInNoFm());
    putFloor(floors, "allInFmOneWay", entity.getAllInFmOneWay());
    putFloor(floors, "allInFmRound", entity.getAllInFmRound());
    putFloor(floors, "nsLift", entity.getNsLift());
    putFloor(floors, "chassis", entity.getChassis());
    putFloor(floors, "waitingFee", entity.getWaitingFee());
    putFloor(floors, "redelivery", entity.getRedelivery());
    return floors;
  }

  public static Map<String, BigDecimal> seaCostFloors(CostSea entity) {
    Map<String, BigDecimal> floors = new LinkedHashMap<>();
    if (entity == null) {
      return floors;
    }
    BigDecimal allIn = entity.getAllIn();
    BigDecimal freight = entity.getFreight();
    putFloor(floors, "allIn", allIn != null ? allIn : freight);
    putFloor(floors, "freight", freight != null ? freight : allIn);
    return floors;
  }

  public static Map<String, BigDecimal> fumigationCostFloors(CostFumigation entity) {
    Map<String, BigDecimal> floors = new LinkedHashMap<>();
    if (entity == null) {
      return floors;
    }
    putFloor(floors, "outdoorNonOak", entity.getOutdoorNonOak());
    putFloor(floors, "outdoorOak", entity.getOutdoorOak());
    putFloor(floors, "indoorNonOak", entity.getIndoorNonOak());
    putFloor(floors, "indoorOak", entity.getIndoorOak());
    return floors;
  }

  public static Map<String, BigDecimal> currentRoadValues(RoadCostResponse row) {
    Map<String, BigDecimal> values = new LinkedHashMap<>();
    if (row == null) {
      return values;
    }
    putFloor(values, "allInNoFm", row.allInNoFm());
    putFloor(values, "allInFmOneWay", row.allInFmOneWay());
    putFloor(values, "allInFmRound", row.allInFmRound());
    putFloor(values, "nsLift", row.nsLift());
    putFloor(values, "chassis", row.chassis());
    putFloor(values, "waitingFee", row.waitingFee());
    putFloor(values, "redelivery", row.redelivery());
    return values;
  }

  public static Map<String, BigDecimal> currentSeaValues(FreightCostResponse row) {
    Map<String, BigDecimal> values = new LinkedHashMap<>();
    if (row == null) {
      return values;
    }
    putFloor(values, "allIn", row.allIn());
    putFloor(values, "freight", row.freight());
    return values;
  }

  public static Map<String, BigDecimal> currentFumigationValues(FumigationCostResponse row) {
    Map<String, BigDecimal> values = new LinkedHashMap<>();
    if (row == null) {
      return values;
    }
    putFloor(values, "outdoorNonOak", row.outdoorNonOak());
    putFloor(values, "outdoorOak", row.outdoorOak());
    putFloor(values, "indoorNonOak", row.indoorNonOak());
    putFloor(values, "indoorOak", row.indoorOak());
    return values;
  }

  public static Map<String, BigDecimal> normalizeFields(
      CostHighlightMode mode, Map<String, ?> raw) {
    if (raw == null || raw.isEmpty()) {
      return Map.of();
    }
    Set<String> allowed = Set.copyOf(editableFields(mode));
    Map<String, BigDecimal> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, ?> entry : raw.entrySet()) {
      if (!allowed.contains(entry.getKey())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "不允许修改字段: " + entry.getKey());
      }
      BigDecimal value = toBigDecimal(entry.getValue());
      if (value != null) {
        normalized.put(entry.getKey(), value);
      }
    }
    return normalized;
  }

  public static void validateFloors(
      Map<String, BigDecimal> floors, Map<String, BigDecimal> fields) {
    for (Map.Entry<String, BigDecimal> entry : fields.entrySet()) {
      BigDecimal floor = floors.get(entry.getKey());
      if (floor == null) {
        continue;
      }
      BigDecimal value = entry.getValue();
      if (value != null && value.compareTo(floor) < 0) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            String.format("字段 %s 不能低于成本原价 %s", entry.getKey(), floor));
      }
    }
  }

  public static Map<String, BigDecimal> toDecimalMap(Map<String, Object> raw) {
    if (raw == null || raw.isEmpty()) {
      return Map.of();
    }
    Map<String, BigDecimal> result = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : raw.entrySet()) {
      BigDecimal value = toBigDecimal(entry.getValue());
      if (value != null) {
        result.put(entry.getKey(), value);
      }
    }
    return result;
  }

  private static void putFloor(Map<String, BigDecimal> map, String key, BigDecimal value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  private static BigDecimal toBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    try {
      String text = String.valueOf(value).trim();
      if (text.isEmpty()) {
        return null;
      }
      return new BigDecimal(text);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
