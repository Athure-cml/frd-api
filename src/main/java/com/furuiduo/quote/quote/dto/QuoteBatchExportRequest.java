package com.furuiduo.quote.quote.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "批量导出请求")
public record QuoteBatchExportRequest(@Schema(description = "报价单 ID 列表") List<Long> ids) {}
