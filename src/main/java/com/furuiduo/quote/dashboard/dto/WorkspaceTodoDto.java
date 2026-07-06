package com.furuiduo.quote.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工作台待办")
public record WorkspaceTodoDto(
    @Schema(description = "报价单 ID") Long id,
    @Schema(description = "报价单号") String quoteNo,
    @Schema(description = "客户名称") String customer,
    @Schema(
            description = "待办类型：completeDraft/followSent/confirmWon/archiveLost")
        String todoType,
    @Schema(description = "优先级：urgent/high/medium") String priority,
    @Schema(description = "时间标签") String time,
    @Schema(description = "是否已完成") boolean done) {}
