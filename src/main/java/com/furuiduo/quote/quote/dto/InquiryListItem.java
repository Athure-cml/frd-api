package com.furuiduo.quote.quote.dto;

import java.util.List;

import com.furuiduo.quote.quote.entity.InquiryOrder;
import com.furuiduo.quote.quote.support.QuoteDateTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "询价单列表项")
public record InquiryListItem(
    Long id,
    String inquiryNo,
    String status,
    List<String> serviceTypes,
    Long customerId,
    String customerName,
    String por,
    String pol,
    String pod,
    String zipCode,
    String city,
    String state,
    String pickUpAddress,
    String fumigationPoint,
    String oakType,
    String quoteDate,
    String validUntil,
    Long quoteId,
    String createdByName,
    String updatedAt) {

  public static InquiryListItem from(InquiryOrder order) {
    return new InquiryListItem(
        order.getId(),
        order.getInquiryNo(),
        order.getStatus() == null ? null : order.getStatus().name(),
        order.getServiceTypes(),
        order.getCustomerId(),
        order.getCustomerName(),
        order.getPor(),
        order.getPol(),
        order.getPod(),
        order.getZipCode(),
        order.getCity(),
        order.getState(),
        order.getPickUpAddress(),
        order.getFumigationPoint(),
        order.getOakType() == null ? null : order.getOakType().name(),
        QuoteDateTimes.format(order.getQuoteDate()),
        QuoteDateTimes.format(order.getValidUntil()),
        order.getQuoteId(),
        order.getCreatedByName(),
        QuoteDateTimes.format(order.getUpdatedAt()));
  }
}
