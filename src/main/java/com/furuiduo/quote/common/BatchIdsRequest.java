package com.furuiduo.quote.common;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "批量 ID 请求")
public record BatchIdsRequest(@Schema(description = "ID 列表") List<Long> ids) {}
