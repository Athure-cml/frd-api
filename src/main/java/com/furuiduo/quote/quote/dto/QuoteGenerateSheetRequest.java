package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单生成请求")
public record QuoteGenerateSheetRequest(
    @Schema(description = "POR（接货港），必填") String por,
    @Schema(description = "POL（装货港），必填") String pol,
    @Schema(description = "POD（目的港），必填") String pod,
    @Schema(description = "Zip code") String zipCode,
    @Schema(description = "City") String city,
    @Schema(description = "State") String state,
    @Schema(description = "提货地址") String pickUpAddress,
    @Schema(description = "CIF 货值（用于保险费/代理费计算）") BigDecimal cifAmount,
    @Schema(description = "熏蒸点（熏蒸成本库 STATION，空表示不熏蒸）") String fumigationPoint,
    @Schema(description = "是否启用熏蒸（兼容旧入参，优先看 fumigationPoint）") Boolean fumigationEnabled,
    @Schema(description = "报价日期，默认系统当日") LocalDate quoteDate) {}
