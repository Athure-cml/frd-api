package com.furuiduo.quote.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 请求前端打开的页面")
public record AiOpenPage(
    @Schema(description = "页面键，如 cost_sea") String page,
    @Schema(description = "前端路由 name") String routeName,
    @Schema(description = "展示标题") String title) {}
