package com.furuiduo.quote.shippingline.dto;

import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.shippingline.entity.ShippingLine;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "船公司")
public record ShippingLineResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "船公司编码") String code,
    @Schema(description = "船公司名称") String name,
    @Schema(description = "简称") String shortName,
    @Schema(description = "联系人") String contactName,
    @Schema(description = "电话") String phone,
    @Schema(description = "邮箱") String email,
    @Schema(description = "备注") String remark,
    @Schema(description = "状态") Integer status,
    @Schema(description = "创建人") String createdByName,
    @Schema(description = "创建时间") String createdAt,
    @Schema(description = "更新时间") String updatedAt,
    @Schema(description = "置顶时间") String pinnedAt) {

  public static ShippingLineResponse from(ShippingLine entity) {
    return new ShippingLineResponse(
        entity.getId(),
        entity.getCode(),
        entity.getName(),
        entity.getShortName(),
        entity.getContactName(),
        entity.getPhone(),
        entity.getEmail(),
        entity.getRemark(),
        entity.getStatus(),
        entity.getCreatedByName(),
        QuoteDateTimes.format(entity.getCreatedAt()),
        QuoteDateTimes.format(entity.getUpdatedAt()),
        QuoteDateTimes.format(entity.getPinnedAt()));
  }
}
