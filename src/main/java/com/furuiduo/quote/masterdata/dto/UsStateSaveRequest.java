package com.furuiduo.quote.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "美国州保存请求")
public record UsStateSaveRequest(
    @Schema(description = "州代码") String code, @Schema(description = "中文名称") String nameZh) {}
