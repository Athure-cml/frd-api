package com.furuiduo.quote.currency.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "币种保存")
public record CurrencySaveRequest(
    @Schema(description = "编码", example = "USD") String code,
    @Schema(description = "名称") String name,
    @Schema(description = "符号") String symbol,
    @Schema(description = "小数位") Integer decimalPlaces,
    @Schema(description = "是否基准币") Boolean base,
    @Schema(description = "排序") Integer sort,
    @Schema(description = "状态") Integer status) {}
