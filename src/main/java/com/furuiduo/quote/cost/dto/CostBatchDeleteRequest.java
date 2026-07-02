package com.furuiduo.quote.cost.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "批量删除")
public record CostBatchDeleteRequest(@Schema(description = "ID 列表") List<Long> ids) {}
