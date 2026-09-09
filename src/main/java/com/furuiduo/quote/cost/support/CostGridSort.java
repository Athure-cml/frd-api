package com.furuiduo.quote.cost.support;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;

/** 成本库表格远程排序：标准列走 JPA，extraFields 自定义列在内存排序。 */
public final class CostGridSort {

  private static final Sort DEFAULT_TIE_BREAKER = Sort.by(Sort.Direction.DESC, "id");

  private static final Set<String> ROAD_FIELDS =
      Set.of(
          "id",
          "zipCode",
          "city",
          "state",
          "por",
          "pol",
          "supplier",
          "baseFreight",
          "fsc",
          "chassis",
          "triTandemAxle",
          "split",
          "stopOff",
          "allInNoFm",
          "allInFmOneWay",
          "allInFmRound",
          "waitingFee",
          "redelivery",
          "prepull",
          "nsLift",
          "otherFee",
          "remark",
          "validDate",
          "logYardNameAddress",
          "status",
          "updatedAt");

  private static final Set<String> SEA_FIELDS =
      Set.of(
          "id",
          "por",
          "pol",
          "pod",
          "cnShortName",
          "enProductName",
          "containerType",
          "freight",
          "freightValidDate",
          "buc",
          "bucValidDate",
          "ebs",
          "ebsValidDate",
          "gri",
          "griValidDate",
          "others",
          "othersValidDate",
          "allIn",
          "ssl",
          "agent",
          "remark",
          "status",
          "updatedAt");

  private static final Set<String> FUMIGATION_FIELDS =
      Set.of(
          "id",
          "region",
          "station",
          "outdoorNonOak",
          "outdoorOak",
          "outdoorValidity",
          "indoorNonOak",
          "indoorOak",
          "indoorValidity",
          "address",
          "status",
          "updatedAt");

  private CostGridSort() {}

  public record Parsed(String fieldKey, boolean custom, Sort.Direction direction) {}

  public static Parsed parseRoad(String sortField, String sortOrder) {
    return parse(sortField, sortOrder, ROAD_FIELDS);
  }

  public static Parsed parseSea(String sortField, String sortOrder) {
    return parse(sortField, sortOrder, SEA_FIELDS);
  }

  public static Parsed parseFumigation(String sortField, String sortOrder) {
    return parse(sortField, sortOrder, FUMIGATION_FIELDS);
  }

  public static Parsed parse(String sortField, String sortOrder, Set<String> entityFields) {
    if (sortField == null || sortField.isBlank()) {
      return null;
    }
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
    if (sortField.startsWith("extraFields.")) {
      return new Parsed(sortField.substring("extraFields.".length()), true, direction);
    }
    if (entityFields.contains(sortField)) {
      return new Parsed(sortField, false, direction);
    }
    return null;
  }

  public static boolean needsMemorySort(Parsed parsed) {
    return parsed != null && parsed.custom();
  }

  public static Sort jpaSort(Parsed parsed) {
    return jpaSort(parsed, DEFAULT_TIE_BREAKER);
  }

  public static Sort jpaSort(Parsed parsed, Sort tieBreaker) {
    if (parsed == null || parsed.custom()) {
      return tieBreaker;
    }
    return Sort.by(parsed.direction(), parsed.fieldKey()).and(tieBreaker);
  }

  public static Comparator<CostRoad> roadComparator(Parsed parsed) {
    return entityComparator(parsed, CostGridSort::roadValue, CostRoad::getId);
  }

  public static Comparator<CostSea> seaComparator(Parsed parsed) {
    return entityComparator(parsed, CostGridSort::seaValue, CostSea::getId);
  }

  public static Comparator<CostFumigation> fumigationComparator(Parsed parsed) {
    return entityComparator(parsed, CostGridSort::fumigationValue, CostFumigation::getId);
  }

  private static <T> Comparator<T> entityComparator(
      Parsed parsed, java.util.function.BiFunction<T, String, Object> reader, java.util.function.Function<T, Long> idReader) {
    Comparator<T> base =
        parsed == null
            ? Comparator.comparing(idReader, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
            : Comparator.comparing(
                    (T item) -> reader.apply(item, parsed.fieldKey()),
                    CostGridSort::compareValues);
    if (parsed != null && parsed.direction() == Sort.Direction.DESC) {
      base = base.reversed();
    }
    return base.thenComparing(idReader, Comparator.nullsLast(Comparator.reverseOrder()));
  }

  private static Object roadValue(CostRoad entity, String key) {
    if (key.startsWith("cf_")) {
      Map<String, Object> extra = entity.getExtraFields();
      return extra == null ? null : extra.get(key);
    }
    return switch (key) {
      case "zipCode" -> entity.getZipCode();
      case "city" -> entity.getCity();
      case "state" -> entity.getState();
      case "por" -> entity.getPor();
      case "pol" -> entity.getPol();
      case "supplier" -> entity.getSupplier();
      case "baseFreight" -> entity.getBaseFreight();
      case "fsc" -> entity.getFsc();
      case "chassis" -> entity.getChassis();
      case "triTandemAxle" -> entity.getTriTandemAxle();
      case "split" -> entity.getSplit();
      case "stopOff" -> entity.getStopOff();
      case "allInNoFm" -> entity.getAllInNoFm();
      case "allInFmOneWay" -> entity.getAllInFmOneWay();
      case "allInFmRound" -> entity.getAllInFmRound();
      case "waitingFee" -> entity.getWaitingFee();
      case "redelivery" -> entity.getRedelivery();
      case "prepull" -> entity.getPrepull();
      case "nsLift" -> entity.getNsLift();
      case "otherFee" -> entity.getOtherFee();
      case "remark" -> entity.getRemark();
      case "validDate" -> entity.getValidDate();
      case "logYardNameAddress" -> entity.getLogYardNameAddress();
      case "status" -> entity.getStatus() == null ? null : entity.getStatus().name();
      case "updatedAt" -> entity.getUpdatedAt();
      case "id" -> entity.getId();
      default -> null;
    };
  }

  private static Object seaValue(CostSea entity, String key) {
    if (key.startsWith("cf_")) {
      Map<String, Object> extra = entity.getExtraFields();
      return extra == null ? null : extra.get(key);
    }
    return switch (key) {
      case "por" -> entity.getPor();
      case "pol" -> entity.getPol();
      case "pod" -> entity.getPod();
      case "cnShortName" -> entity.getCnShortName();
      case "enProductName" -> entity.getEnProductName();
      case "containerType" -> entity.getContainerType();
      case "freight" -> entity.getFreight();
      case "freightValidDate" -> entity.getFreightValidDate();
      case "buc" -> entity.getBuc();
      case "bucValidDate" -> entity.getBucValidDate();
      case "ebs" -> entity.getEbs();
      case "ebsValidDate" -> entity.getEbsValidDate();
      case "gri" -> entity.getGri();
      case "griValidDate" -> entity.getGriValidDate();
      case "others" -> entity.getOthers();
      case "othersValidDate" -> entity.getOthersValidDate();
      case "allIn" -> entity.getAllIn();
      case "ssl" -> entity.getSsl();
      case "agent" -> entity.getAgent();
      case "remark" -> entity.getRemark();
      case "status" -> entity.getStatus() == null ? null : entity.getStatus().name();
      case "updatedAt" -> entity.getUpdatedAt();
      case "id" -> entity.getId();
      default -> null;
    };
  }

  private static Object fumigationValue(CostFumigation entity, String key) {
    if (key.startsWith("cf_")) {
      Map<String, Object> extra = entity.getExtraFields();
      return extra == null ? null : extra.get(key);
    }
    return switch (key) {
      case "region" -> entity.getRegion();
      case "station" -> entity.getStation();
      case "outdoorNonOak" -> entity.getOutdoorNonOak();
      case "outdoorOak" -> entity.getOutdoorOak();
      case "outdoorValidity" -> entity.getOutdoorValidity();
      case "indoorNonOak" -> entity.getIndoorNonOak();
      case "indoorOak" -> entity.getIndoorOak();
      case "indoorValidity" -> entity.getIndoorValidity();
      case "address" -> entity.getAddress();
      case "status" -> entity.getStatus() == null ? null : entity.getStatus().name();
      case "updatedAt" -> entity.getUpdatedAt();
      case "id" -> entity.getId();
      default -> null;
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static int compareValues(Object left, Object right) {
    if (left == right) {
      return 0;
    }
    if (left == null) {
      return 1;
    }
    if (right == null) {
      return -1;
    }
    if (left instanceof BigDecimal a && right instanceof BigDecimal b) {
      return a.compareTo(b);
    }
    if (left instanceof Number a && right instanceof Number b) {
      return Double.compare(a.doubleValue(), b.doubleValue());
    }
    BigDecimal leftAmount = tryDecimal(left);
    BigDecimal rightAmount = tryDecimal(right);
    if (leftAmount != null && rightAmount != null) {
      return leftAmount.compareTo(rightAmount);
    }
    return String.valueOf(left).compareToIgnoreCase(String.valueOf(right));
  }

  private static BigDecimal tryDecimal(Object value) {
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    if (value instanceof String text) {
      String normalized = text.trim().replace(",", "");
      if (normalized.isEmpty()) {
        return null;
      }
      try {
        return new BigDecimal(normalized);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }
}
