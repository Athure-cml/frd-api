package com.furuiduo.quote.cost.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "成本行部门常用标记（列表展示）")
public record CostHighlightView(
    @Schema(description = "行背景色") String color,
    @Schema(description = "备注") String remark,
    @Schema(description = "标记部门名称") String deptName,
    @Schema(description = "标记部门数（全局用户多部门标记时 >1）") Integer deptCount,
    @Schema(description = "当前用户所在部门是否已标记") Boolean viewerDeptMarked) {}
