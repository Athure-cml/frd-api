package com.furuiduo.quote.cost.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "模板 Excel 导出结果")
public record TemplateExcelFile(
    @Schema(description = "下载文件名") String filename,
    @Schema(description = "xlsx 二进制内容") byte[] content) {}
