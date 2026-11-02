package com.furuiduo.quote.quote.dto;

import java.util.List;

import com.furuiduo.quote.quote.entity.InquiryOrder;
import com.furuiduo.quote.quote.entity.QuoteOakType;
import com.furuiduo.quote.quote.entity.QuoteTransportMode;
import com.furuiduo.quote.quote.support.QuoteDateTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "询价单详情")
public record InquiryDetailResponse(
    Long id,
    String inquiryNo,
    String status,
    List<String> serviceTypes,
    Long customerId,
    String customerName,
    QuoteTransportMode transportMode,
    String routeSummary,
    String quoteDate,
    String validUntil,
    String zipCode,
    String city,
    String state,
    String por,
    String pol,
    String pod,
    Boolean fumigationEnabled,
    String fumigationPoint,
    QuoteOakType oakType,
    String pickUpAddress,
    String remark,
    Long quoteId,
    String quoteNo,
    String submittedAt,
    String createdByName,
    String createdAt,
    String updatedAt) {

  public static InquiryDetailResponse from(InquiryOrder order, String quoteNo) {
    return new InquiryDetailResponse(
        order.getId(),
        order.getInquiryNo(),
        order.getStatus() == null ? null : order.getStatus().name(),
        order.getServiceTypes(),
        order.getCustomerId(),
        order.getCustomerName(),
        order.getTransportMode(),
        order.getRouteSummary(),
        QuoteDateTimes.format(order.getQuoteDate()),
        QuoteDateTimes.format(order.getValidUntil()),
        order.getZipCode(),
        order.getCity(),
        order.getState(),
        order.getPor(),
        order.getPol(),
        order.getPod(),
        order.getFumigationEnabled(),
        order.getFumigationPoint(),
        order.getOakType(),
        order.getPickUpAddress(),
        order.getRemark(),
        order.getQuoteId(),
        quoteNo,
        QuoteDateTimes.format(order.getSubmittedAt()),
        order.getCreatedByName(),
        QuoteDateTimes.format(order.getCreatedAt()),
        QuoteDateTimes.format(order.getUpdatedAt()));
  }
}
