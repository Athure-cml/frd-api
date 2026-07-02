package com.furuiduo.quote.currency.dto;

import com.furuiduo.quote.currency.entity.Currency;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "币种")
public record CurrencyResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "编码") String code,
    @Schema(description = "名称") String name,
    @Schema(description = "符号") String symbol,
    @Schema(description = "小数位") Integer decimalPlaces,
    @Schema(description = "是否基准币") Boolean base,
    @Schema(description = "排序") Integer sort,
    @Schema(description = "状态") Integer status) {

  public static CurrencyResponse from(Currency currency) {
    return new CurrencyResponse(
        currency.getId(),
        currency.getCode(),
        currency.getName(),
        currency.getSymbol(),
        currency.getDecimalPlaces(),
        currency.getBase(),
        currency.getSort(),
        currency.getStatus());
  }
}
