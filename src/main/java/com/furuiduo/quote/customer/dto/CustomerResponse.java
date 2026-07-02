package com.furuiduo.quote.customer.dto;

import java.time.LocalDateTime;

import com.furuiduo.quote.customer.entity.Customer;
import com.furuiduo.quote.quote.support.QuoteDateTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "客户")
public record CustomerResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "客户编码") String code,
    @Schema(description = "客户名称") String name,
    @Schema(description = "联系人") String contactName,
    @Schema(description = "电话") String phone,
    @Schema(description = "邮箱") String email,
    @Schema(description = "地址") String address,
    @Schema(description = "备注") String remark,
    @Schema(description = "状态") Integer status,
    @Schema(description = "创建人") String createdByName,
    @Schema(description = "创建时间") String createdAt,
    @Schema(description = "更新时间") String updatedAt) {

  public static CustomerResponse from(Customer customer) {
    return new CustomerResponse(
        customer.getId(),
        customer.getCode(),
        customer.getName(),
        customer.getContactName(),
        customer.getPhone(),
        customer.getEmail(),
        customer.getAddress(),
        customer.getRemark(),
        customer.getStatus(),
        customer.getCreatedByName(),
        QuoteDateTimes.format(customer.getCreatedAt()),
        QuoteDateTimes.format(customer.getUpdatedAt()));
  }
}
