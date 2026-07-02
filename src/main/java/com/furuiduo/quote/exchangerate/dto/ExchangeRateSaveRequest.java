package com.furuiduo.quote.exchangerate.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "汇率保存")
public record ExchangeRateSaveRequest(
    @Schema(description = "源币种") String fromCurrency,
    @Schema(description = "目标币种") String toCurrency,
    @Schema(description = "汇率") BigDecimal rate,
    @Schema(description = "生效日期") LocalDate effectiveDate,
    @Schema(description = "状态") Integer status) {}
