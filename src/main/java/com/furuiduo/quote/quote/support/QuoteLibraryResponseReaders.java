package com.furuiduo.quote.quote.support;

import java.math.BigDecimal;
import java.util.Map;

import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.support.CostValidityStatus;

public final class QuoteLibraryResponseReaders {

  private QuoteLibraryResponseReaders() {}

  public static Object readRoad(RoadCostResponse row, String field) {
    if (row == null || field == null || field.isBlank()) {
      return null;
    }
    if (isCustomField(field)) {
      return formatExportDate(readExtra(row.extraFields(), field));
    }
    return switch (field) {
      case "zipCode" -> row.zipCode();
      case "city" -> row.city();
      case "state" -> row.state();
      case "por" -> row.por();
      case "station" -> row.station();
      case "pol" -> row.pol();
      case "supplier" -> row.supplier();
      case "allInNoFm" -> row.allInNoFm();
      case "allInFmOneWay" -> row.allInFmOneWay();
      case "allInFmRound" -> row.allInFmRound();
      case "waitingFee" -> row.waitingFee();
      case "redelivery" -> row.redelivery();
      case "nsLift" -> row.nsLift();
      case "chassis" -> row.chassis();
      case "validDate" -> formatExportDate(row.validDate());
      case "logYardNameAddress" -> row.logYardNameAddress();
      case "remark" -> row.remark();
      case "status" -> formatStatus(row.status());
      default -> null;
    };
  }

  public static Object readSea(FreightCostResponse row, String field) {
    if (row == null || field == null || field.isBlank()) {
      return null;
    }
    if (isCustomField(field)) {
      return formatExportDate(readExtra(row.extraFields(), field));
    }
    return switch (field) {
      case "por" -> row.por();
      case "pol" -> row.pol();
      case "pod" -> row.pod();
      case "containerType" -> row.containerType();
      case "freightValidDate" -> formatExportDate(row.freightValidDate());
      case "allIn" -> row.allIn();
      case "freight" -> row.freight();
      case "ssl" -> row.ssl();
      case "agent" -> row.agent();
      case "remark" -> row.remark();
      case "enProductName" -> row.enProductName();
      case "cnShortName" -> row.cnShortName();
      case "status" -> formatStatus(row.status());
      default -> null;
    };
  }

  public static Object readFumigation(FumigationCostResponse row, String field) {
    if (row == null || field == null || field.isBlank()) {
      return null;
    }
    if (isCustomField(field)) {
      return formatExportDate(readExtra(row.extraFields(), field));
    }
    return switch (field) {
      case "region" -> row.region();
      case "station" -> row.station();
      case "outdoorNonOak" -> row.outdoorNonOak();
      case "outdoorOak" -> row.outdoorOak();
      case "outdoorValidity" -> formatExportDate(row.outdoorValidity());
      case "indoorNonOak" -> row.indoorNonOak();
      case "indoorOak" -> row.indoorOak();
      case "indoorValidity" -> formatExportDate(row.indoorValidity());
      case "address" -> row.address();
      case "remark" -> row.remark();
      case "status" -> formatStatus(row.status());
      default -> null;
    };
  }

  public static Object formatExportValue(String field, Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal || value instanceof Number) {
      return value;
    }
    if (isDateField(field)) {
      return formatExportDate(value);
    }
    return value;
  }

  private static boolean isDateField(String field) {
    if (field == null) {
      return false;
    }
    return field.endsWith("ValidDate")
        || field.endsWith("Validity")
        || field.endsWith("_eff")
        || "validDate".equals(field);
  }

  private static boolean isCustomField(String field) {
    return field.startsWith("cf_");
  }

  private static Object readExtra(Map<String, Object> extraFields, String field) {
    if (extraFields == null || extraFields.isEmpty()) {
      return null;
    }
    return extraFields.get(field);
  }

  private static String formatExportDate(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    if (text.isEmpty()) {
      return null;
    }
    return CostValidityStatus.formatExportDate(text);
  }

  private static String formatStatus(CostStatus status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case active -> "生效中";
      case pending -> "未生效";
      case draft -> "草稿";
      case expired -> "已过期";
    };
  }
}
