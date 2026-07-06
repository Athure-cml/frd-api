package com.furuiduo.quote.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "热门线路")
public record WorkspaceRouteDto(
    @Schema(description = "线路名称") String name, @Schema(description = "报价次数") long value) {}
