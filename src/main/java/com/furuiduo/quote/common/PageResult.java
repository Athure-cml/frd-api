package com.furuiduo.quote.common;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "分页结果")
public record PageResult<T>(
    @Schema(description = "当前页数据") List<T> items,
    @Schema(description = "总条数") long total) {}
