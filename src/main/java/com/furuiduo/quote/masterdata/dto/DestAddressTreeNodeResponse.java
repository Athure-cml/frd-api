package com.furuiduo.quote.masterdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "目的地址树节点")
public record DestAddressTreeNodeResponse(
    @Schema(description = "节点类型 state|city|zip") String nodeType,
    @Schema(description = "节点 ID") String id,
    @Schema(description = "父节点 ID") String parentId,
    @Schema(description = "州代码") String stateCode,
    @Schema(description = "城市") String city,
    @Schema(description = "邮编") String zipCode,
    @Schema(description = "州 ID") Long stateId,
    @Schema(description = "城市 ID") Long cityId,
    @Schema(description = "邮编 ID") Long zipId,
    @Schema(description = "是否有子节点（懒加载）") Boolean hasChild) {}
