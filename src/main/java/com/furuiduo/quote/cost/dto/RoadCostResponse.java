package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.furuiduo.quote.cost.entity.CostRoad;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "卡车成本")
public record RoadCostResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "有效期") String validDate,
    @Schema(description = "供应商") String supplier,
    @Schema(description = "堆场地址") String logYardNameAddress,
    @Schema(description = "城市") String city,
    @Schema(description = "州") String state,
    @Schema(description = "POR") String por,
    @Schema(description = "POL") String pol,
    @Schema(description = "基础运费") BigDecimal baseFreight,
    @Schema(description = "FSC") BigDecimal fsc,
    @Schema(description = "CHASSIS") BigDecimal chassis,
    @Schema(description = "OW/TRI-AXLE") BigDecimal owTriAxle,
    @Schema(description = "SPLIT") BigDecimal split,
    @Schema(description = "STOP OFF") BigDecimal stopOff,
    @Schema(description = "ALL IN") BigDecimal allIn,
    @Schema(description = "ALL IN NON OAK") BigDecimal allInNonOak,
    @Schema(description = "ALL IN OAK") BigDecimal allInOak,
    @Schema(description = "WAITING FEE") BigDecimal waitingFee,
    @Schema(description = "REDELIVERY") BigDecimal redelivery,
    @Schema(description = "PREPULL") BigDecimal prepull,
    @Schema(description = "NS LIFT") BigDecimal nsLift,
    @Schema(description = "备注") String remark,
    @Schema(description = "自定义字段值") Map<String, Object> extraFields,
    @Schema(description = "更新时间") String updatedAt) {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public static RoadCostResponse from(CostRoad entity) {
    return new RoadCostResponse(
        entity.getId(),
        entity.getValidDate(),
        entity.getSupplier(),
        entity.getLogYardNameAddress(),
        entity.getCity(),
        entity.getState(),
        entity.getPor(),
        entity.getPol(),
        entity.getBaseFreight(),
        entity.getFsc(),
        entity.getChassis(),
        entity.getOwTriAxle(),
        entity.getSplit(),
        entity.getStopOff(),
        entity.getAllIn(),
        entity.getAllInNonOak(),
        entity.getAllInOak(),
        entity.getWaitingFee(),
        entity.getRedelivery(),
        entity.getPrepull(),
        entity.getNsLift(),
        entity.getRemark(),
        entity.getExtraFields(),
        entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().format(FORMATTER));
  }
}
