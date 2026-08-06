package com.furuiduo.quote.shippingline.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "船公司保存")
public record ShippingLineSaveRequest(
    @Schema(description = "船公司名称") String name,
    @Schema(description = "邮箱") String email,
    @Schema(description = "备注") String remark,
    @Schema(description = "状态 1启用 0停用") Integer status) {}
