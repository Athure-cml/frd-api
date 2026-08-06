package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "卡车成本保存")
public record RoadCostSaveRequest(
    @Schema(description = "邮编") String zipCode,
    @Schema(description = "城市") String city,
    @Schema(description = "州") String state,
    @Schema(description = "POR 接货地（美国城市）") String por,
    @Schema(description = "POL 港口") String pol,
    @Schema(description = "供应商") String supplier,
    @Schema(description = "基础运费") BigDecimal baseFreight,
    @Schema(description = "FSC") BigDecimal fsc,
    @Schema(description = "CHASSIS") BigDecimal chassis,
    @Schema(description = "OW/TRI-AXCEL") BigDecimal triTandemAxle,
    @Schema(description = "SPLIT") BigDecimal split,
    @Schema(description = "STOP OFF") BigDecimal stopOff,
    @Schema(description = "ALL IN - NO FM") BigDecimal allInNoFm,
    @Schema(description = "ALL IN - FM (NON OAK)") BigDecimal allInFmOneWay,
    @Schema(description = "ALL IN - FM (OAK)") BigDecimal allInFmRound,
    @Schema(description = "WAITING FEE") BigDecimal waitingFee,
    @Schema(description = "REDELIVERY") BigDecimal redelivery,
    @Schema(description = "PREPULL") BigDecimal prepull,
    @Schema(description = "NS LIFT") BigDecimal nsLift,
    @Schema(description = "OTHER FEE") BigDecimal otherFee,
    @Schema(description = "备注") String remark,
    @Schema(description = "有效期") String validDate,
    @Schema(description = "堆场地址") String logYardNameAddress,
    @Schema(description = "状态（由有效期自动判定 active/expired，可忽略）") String status,
    @Schema(description = "自定义字段值") Map<String, Object> extraFields) {}
