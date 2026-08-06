package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "海运成本保存")
public record FreightCostSaveRequest(
    @Schema(description = "POR") String por,
    @Schema(description = "POL") String pol,
    @Schema(description = "POD") String pod,
    @Schema(description = "中文简称") String cnShortName,
    @Schema(description = "英文品名") String enProductName,
    @Schema(description = "箱型") String containerType,
    @Schema(description = "运费") BigDecimal freight,
    @Schema(description = "运费有效期") String freightValidDate,
    @Schema(description = "BUC") BigDecimal buc,
    @Schema(description = "BUC 有效期") String bucValidDate,
    @Schema(description = "EBS") BigDecimal ebs,
    @Schema(description = "EBS 有效期") String ebsValidDate,
    @Schema(description = "GRI") BigDecimal gri,
    @Schema(description = "GRI 有效期") String griValidDate,
    @Schema(description = "OTHERS") BigDecimal others,
    @Schema(description = "OTHERS 有效期") String othersValidDate,
    @Schema(description = "ALL IN") BigDecimal allIn,
    @Schema(description = "SSL 船公司") String ssl,
    @Schema(description = "AGENT 代理") String agent,
    @Schema(description = "备注") String remark,
    @Schema(description = "状态（由有效期自动判定 active/expired，可忽略）") String status,
    @Schema(description = "自定义字段值") Map<String, Object> extraFields) {}
