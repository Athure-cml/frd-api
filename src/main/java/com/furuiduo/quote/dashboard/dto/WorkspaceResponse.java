package com.furuiduo.quote.dashboard.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工作台聚合数据")
public record WorkspaceResponse(
    @Schema(description = "KPI 指标") List<WorkspaceMetricDto> metrics,
    @Schema(description = "待办任务") List<WorkspaceTodoDto> todos,
    @Schema(description = "报价进度") List<WorkspacePipelineDto> pipeline,
    @Schema(description = "系统通知") List<WorkspaceNoticeDto> notices,
    @Schema(description = "热门线路 TOP") List<WorkspaceRouteDto> topRoutes) {}
