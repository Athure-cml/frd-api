package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "卡车成本批量复制")
public record CostRoadBatchCopyRequest(
    @Schema(description = "源记录 ID 列表") List<Long> ids,
    @Schema(description = "是否统一修改燃油/有效期；false 表示原样复制") Boolean applyOverrides,
    @Schema(description = "统一燃油（%），仅 applyOverrides=true 时生效") BigDecimal fsc,
    @Schema(description = "统一有效期，仅 applyOverrides=true 时生效") String validDate) {}
