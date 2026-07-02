package com.furuiduo.quote.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "客户保存")
public record CustomerSaveRequest(
    @Schema(description = "客户名称") String name,
    @Schema(description = "联系人") String contactName,
    @Schema(description = "电话") String phone,
    @Schema(description = "邮箱") String email,
    @Schema(description = "地址") String address,
    @Schema(description = "备注") String remark,
    @Schema(description = "状态 1启用 0停用") Integer status) {}
