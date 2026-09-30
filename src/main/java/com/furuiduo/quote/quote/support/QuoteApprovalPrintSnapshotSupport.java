package com.furuiduo.quote.quote.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.dto.QuoteDetailResponse;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;
import com.furuiduo.quote.quote.entity.QuoteApprovalLog;
import com.furuiduo.quote.quote.entity.QuoteOrder;

/** 审批提交时的报价单打印模板快照。 */
public final class QuoteApprovalPrintSnapshotSupport {

  private static final ObjectMapper JSON =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private QuoteApprovalPrintSnapshotSupport() {}

  public static String serialize(
      QuoteOrder order, List<QuoteCostMatchItemDto> costSnapshots) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("quoteNo", order.getQuoteNo());
    payload.put("customerName", order.getCustomerName());
    payload.put(
        "quoteDate",
        order.getCreatedAt() == null
            ? null
            : order.getCreatedAt().toLocalDate().toString());
    payload.put("sheet", QuoteSheetFieldsDto.from(order));
    payload.put("costSnapshots", costSnapshots == null ? List.of() : costSnapshots);
    try {
      return JSON.writeValueAsString(payload);
    } catch (Exception ex) {
      throw new IllegalStateException("序列化审批打印快照失败", ex);
    }
  }

  public static QuoteDetailResponse applyToDetail(
      QuoteDetailResponse live, String printSnapshotJson) {
    if (live == null || printSnapshotJson == null || printSnapshotJson.isBlank()) {
      return live;
    }
    try {
      Map<String, Object> payload =
          JSON.readValue(printSnapshotJson, new TypeReference<Map<String, Object>>() {});
      String customerName =
          payload.get("customerName") instanceof String text && !text.isBlank()
              ? text
              : live.customerName();
      QuoteSheetFieldsDto sheet =
          payload.get("sheet") == null
              ? live.sheet()
              : JSON.convertValue(payload.get("sheet"), QuoteSheetFieldsDto.class);
      List<QuoteCostMatchItemDto> costSnapshots =
          payload.get("costSnapshots") == null
              ? live.costSnapshots()
              : JSON.convertValue(
                  payload.get("costSnapshots"),
                  new TypeReference<List<QuoteCostMatchItemDto>>() {});
      return new QuoteDetailResponse(
          live.id(),
          live.quoteNo(),
          live.serviceTypes(),
          live.oakType(),
          live.customerId(),
          customerName,
          live.transportMode(),
          live.routeSummary(),
          live.status(),
          live.totalAmount(),
          live.currency(),
          live.baseCurrency(),
          live.exchangeRate(),
          live.validUntil(),
          live.remark(),
          live.followUpBy(),
          live.followUpByName(),
          live.expired(),
          live.voided(),
          live.editable(),
          live.operable(),
          live.costRiskActive(),
          live.costRiskReason(),
          live.costRiskModes(),
          live.costRiskAt(),
          sheet,
          live.createdBy(),
          live.createdByName(),
          live.deptId(),
          live.submittedAt(),
          live.createdAt(),
          live.updatedAt(),
          live.parentQuoteId(),
          live.rootQuoteId(),
          live.revisionNo(),
          live.revisionLabel(),
          live.changeReason(),
          live.currentVersion(),
          live.supersededByQuoteId(),
          live.lines(),
          costSnapshots == null ? List.of() : costSnapshots,
          live.followUps());
    } catch (Exception ex) {
      return live;
    }
  }

  public static String findCyclePrintSnapshot(List<QuoteApprovalLog> cycle) {
    if (cycle == null || cycle.isEmpty()) {
      return null;
    }
    for (QuoteApprovalLog log : cycle) {
      if (log.getAction() == com.furuiduo.quote.quote.entity.QuoteApprovalAction.SUBMIT
          && log.getPrintSnapshot() != null
          && !log.getPrintSnapshot().isBlank()) {
        return log.getPrintSnapshot();
      }
    }
    return null;
  }
}
