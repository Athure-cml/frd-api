package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.support.CostValidityStatus;
import com.furuiduo.quote.quote.support.QuoteDateTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "熏蒸成本")
public record FumigationCostResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "REGION") String region,
    @Schema(description = "STATION") String station,
    @Schema(description = "FM-OUTDOOR NON OAK") BigDecimal outdoorNonOak,
    @Schema(description = "FM-OUTDOOR OAK") BigDecimal outdoorOak,
    @Schema(description = "FM-OUTDOOR 有效期") String outdoorValidity,
    @Schema(description = "FM-INDOOR NON OAK") BigDecimal indoorNonOak,
    @Schema(description = "FM-INDOOR OAK") BigDecimal indoorOak,
    @Schema(description = "FM-INDOOR 有效期") String indoorValidity,
    @Schema(description = "ADDRESS") String address,
    @Schema(description = "状态") CostStatus status,
    @Schema(description = "扩展字段") Map<String, Object> extraFields,
    @Schema(description = "更新时间") String updatedAt) {

  public static FumigationCostResponse from(CostFumigation entity) {
    CostStatus status =
        CostValidityStatus.resolve(
            entity.getStatus(), entity.getOutdoorValidity(), entity.getIndoorValidity());
    return new FumigationCostResponse(
        entity.getId(),
        entity.getRegion(),
        entity.getStation(),
        entity.getOutdoorNonOak(),
        entity.getOutdoorOak(),
        entity.getOutdoorValidity(),
        entity.getIndoorNonOak(),
        entity.getIndoorOak(),
        entity.getIndoorValidity(),
        entity.getAddress(),
        status,
        entity.getExtraFields(),
        QuoteDateTimes.format(entity.getUpdatedAt()));
  }
}
