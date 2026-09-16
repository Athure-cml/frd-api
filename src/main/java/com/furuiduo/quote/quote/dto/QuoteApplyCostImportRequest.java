package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单引入成本后按规则重算字段")
public record QuoteApplyCostImportRequest(
    @Schema(description = "成本类型 ROAD|SEA|FUMIGATION") String costType,
    @Schema(description = "成本库记录快照") Map<String, Object> snapshot,
    @Schema(description = "熏蒸点") String fumigationPoint,
    @Schema(description = "是否熏蒸（兼容）") Boolean fumigationEnabled,
    @Schema(description = "POD（规则上下文）") String pod,
    @Schema(description = "POR（规则上下文）") String por,
    @Schema(description = "CIF 货值（规则上下文）") BigDecimal cifAmount,
    @Schema(description = "报价日期 yyyy-MM-dd，熏蒸费匹配用") String quoteDate) {}
