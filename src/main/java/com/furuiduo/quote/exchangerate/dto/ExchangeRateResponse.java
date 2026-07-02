package com.furuiduo.quote.exchangerate.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.furuiduo.quote.exchangerate.entity.ExchangeRate;
import com.furuiduo.quote.quote.support.QuoteDateTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "汇率")
public record ExchangeRateResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "源币种") String fromCurrency,
    @Schema(description = "目标币种") String toCurrency,
    @Schema(description = "汇率") BigDecimal rate,
    @Schema(description = "生效日期") LocalDate effectiveDate,
    @Schema(description = "状态") Integer status,
    @Schema(description = "更新时间") String updatedAt) {

  public static ExchangeRateResponse from(ExchangeRate rate) {
    return new ExchangeRateResponse(
        rate.getId(),
        rate.getFromCurrency(),
        rate.getToCurrency(),
        rate.getRate(),
        rate.getEffectiveDate(),
        rate.getStatus(),
        QuoteDateTimes.format(rate.getUpdatedAt()));
  }
}
