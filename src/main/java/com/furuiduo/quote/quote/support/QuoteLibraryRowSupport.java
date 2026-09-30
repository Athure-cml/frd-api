package com.furuiduo.quote.quote.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.quote.entity.QuoteCostSnapshot;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.entity.QuoteOrder;

/** 报价库行：成本库结构 + 报价单上规则处理后的费用。 */
public final class QuoteLibraryRowSupport {

  private static final Pattern VALIDITY_RANGE =
      Pattern.compile(
          "^(\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\s*[-–—~至到]\\s*(\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})$");

  private static final DateTimeFormatter[] VALIDITY_FORMATS =
      new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy-M-d"),
        DateTimeFormatter.ISO_LOCAL_DATE
      };

  private QuoteLibraryRowSupport() {}

  public static Map<String, Object> build(
      QuoteOrder order,
      QuoteCostSnapshot snapshot,
      QuoteCostType type,
      CostRoad road,
      CostSea sea,
      CostFumigation fumigation) {
    if (snapshot == null) {
      return null;
    }
    Map<String, Object> base = resolveBaseRow(snapshot, type, road, sea, fumigation);
    if (base == null || base.isEmpty()) {
      return null;
    }
    Map<String, Object> row = new HashMap<>(base);
    row.put("costRefId", snapshot.getCostRefId());
    applyQuoteProcessedValues(row, order, type);
    return row;
  }

  private static Map<String, Object> resolveBaseRow(
      QuoteCostSnapshot snapshot,
      QuoteCostType type,
      CostRoad road,
      CostSea sea,
      CostFumigation fumigation) {
    Map<String, Object> live =
        switch (type) {
          case ROAD ->
              road != null ? QuoteCostSnapshotMapper.roadSnapshot(road) : null;
          case SEA -> sea != null ? QuoteCostSnapshotMapper.seaSnapshot(sea) : null;
          case FUMIGATION ->
              fumigation != null ? QuoteCostSnapshotMapper.fumigationSnapshot(fumigation) : null;
        };
    if (live != null && !live.isEmpty()) {
      return live;
    }
    Map<String, Object> stored = snapshot.getSnapshotJson();
    if (stored == null || stored.isEmpty()) {
      return null;
    }
    return new HashMap<>(stored);
  }

  private static void applyQuoteProcessedValues(
      Map<String, Object> row, QuoteOrder order, QuoteCostType type) {
    switch (type) {
      case ROAD -> applyRoadQuoteValues(row, order);
      case SEA -> applySeaQuoteValues(row, order);
      case FUMIGATION -> applyFumigationQuoteValues(row, order);
    }
  }

  private static void applyRoadQuoteValues(Map<String, Object> row, QuoteOrder order) {
    putIfPresent(row, "zipCode", order.getZipCode());
    putIfPresent(row, "city", order.getCity());
    putIfPresent(row, "state", order.getState());
    putIfPresent(row, "por", order.getPor());
    putIfPresent(row, "pol", order.getPol());
    putIfPresent(row, "logYardNameAddress", order.getPickUpAddress());

    if (order.getTruckingFee() != null) {
      row.put("allInNoFm", order.getTruckingFee());
    }
    if (order.getTruckingNonOakUsd() != null) {
      row.put("allInFmOneWay", order.getTruckingNonOakUsd());
    }
    if (order.getTruckingOakUsd() != null) {
      row.put("allInFmRound", order.getTruckingOakUsd());
    }
    if (order.getNsLift() != null) {
      row.put("nsLift", order.getNsLift());
    }
    if (order.getChassis() != null) {
      row.put("chassis", order.getChassis());
    }
    if (order.getWaiting() != null) {
      row.put("waitingFee", order.getWaiting());
    }
    if (order.getRedeliveryFee() != null) {
      row.put("redelivery", order.getRedeliveryFee());
    }
    if (order.getTruckRemark() != null && !order.getTruckRemark().isBlank()) {
      row.put("remark", order.getTruckRemark());
      row.put("cf_road_remark", order.getTruckRemark());
    }
  }

  private static void applySeaQuoteValues(Map<String, Object> row, QuoteOrder order) {
    putIfPresent(row, "por", order.getPor());
    putIfPresent(row, "pol", order.getPol());
    putIfPresent(row, "pod", order.getPod());
    putIfPresent(row, "ssl", order.getSsl());

    BigDecimal processed = firstProcessedOceanFreight(order.getOfUsd());
    if (processed != null) {
      row.put("freight", processed);
      row.put("allIn", processed);
    }
    if (order.getSheetRemark() != null && !order.getSheetRemark().isBlank()) {
      row.put("remark", order.getSheetRemark());
    }
  }

  private static void applyFumigationQuoteValues(Map<String, Object> row, QuoteOrder order) {
    putIfPresent(row, "station", order.getFumigationPoint());
    putIfPresent(row, "address", order.getPickUpAddress());
    if (order.getSheetRemark() != null && !order.getSheetRemark().isBlank()) {
      row.put("remark", order.getSheetRemark());
    }

    LocalDate quoteDate =
        order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : LocalDate.now();
    LocalDate outdoorEnd = parseValidityEnd(text(row.get("outdoorValidity")));
    LocalDate indoorEnd = parseValidityEnd(text(row.get("indoorValidity")));
    boolean outdoorValid = outdoorEnd == null || !quoteDate.isAfter(outdoorEnd);
    boolean indoorValid = indoorEnd == null || !quoteDate.isAfter(indoorEnd);

    if (outdoorValid) {
      if (order.getFmNonOak() != null) {
        row.put("outdoorNonOak", order.getFmNonOak());
      }
      if (order.getFmOak() != null) {
        row.put("outdoorOak", order.getFmOak());
      }
      return;
    }
    if (indoorValid) {
      if (order.getFmNonOak() != null) {
        row.put("indoorNonOak", order.getFmNonOak());
      }
      if (order.getFmOak() != null) {
        row.put("indoorOak", order.getFmOak());
      }
    }
  }

  private static BigDecimal firstProcessedOceanFreight(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    for (String part : raw.split("/")) {
      BigDecimal parsed = QuoteSheetAllInSupport.parseUsdAmount(part.trim());
      if (parsed != null) {
        return parsed;
      }
    }
    return QuoteSheetAllInSupport.parseUsdAmount(raw);
  }

  private static LocalDate parseValidityEnd(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String text = raw.trim();
    Matcher matcher = VALIDITY_RANGE.matcher(text);
    if (matcher.matches()) {
      text = matcher.group(2);
    }
    for (DateTimeFormatter formatter : VALIDITY_FORMATS) {
      try {
        return LocalDate.parse(text, formatter);
      } catch (DateTimeParseException ignored) {
        // try next
      }
    }
    return null;
  }

  private static void putIfPresent(Map<String, Object> row, String key, String value) {
    if (value != null && !value.isBlank()) {
      row.put(key, value);
    }
  }

  private static String text(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }
}
