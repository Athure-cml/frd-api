package com.furuiduo.quote.quote.dto;

import java.time.LocalDate;
import java.util.List;

import com.furuiduo.quote.quote.entity.QuoteOakType;
import com.furuiduo.quote.quote.entity.QuoteTransportMode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "询价单保存请求（基础信息区）")
public record InquirySaveRequest(
    List<String> serviceTypes,
    Long customerId,
    String customerName,
    QuoteTransportMode transportMode,
    String routeSummary,
    LocalDate quoteDate,
    LocalDate validUntil,
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
    String remark) {}
