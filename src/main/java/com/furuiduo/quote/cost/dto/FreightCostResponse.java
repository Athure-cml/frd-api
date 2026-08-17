package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.support.CostValidityStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "海运成本")
public record FreightCostResponse(
    @Schema(description = "ID") Long id,
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
    @Schema(description = "状态") CostStatus status,
    @Schema(description = "自定义字段值") Map<String, Object> extraFields,
    @Schema(description = "更新时间") String updatedAt) {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public static FreightCostResponse fromSea(CostSea entity) {
    CostStatus status =
        CostValidityStatus.resolve(entity.getStatus(), entity.getFreightValidDate());
    return new FreightCostResponse(
        entity.getId(),
        entity.getPor(),
        entity.getPol(),
        entity.getPod(),
        entity.getCnShortName(),
        entity.getEnProductName(),
        entity.getContainerType(),
        entity.getFreight(),
        entity.getFreightValidDate(),
        entity.getBuc(),
        entity.getBucValidDate(),
        entity.getEbs(),
        entity.getEbsValidDate(),
        entity.getGri(),
        entity.getGriValidDate(),
        entity.getOthers(),
        entity.getOthersValidDate(),
        entity.getAllIn(),
        entity.getSsl(),
        entity.getAgent(),
        entity.getRemark(),
        status,
        migrateSeaExtraFields(entity.getExtraFields()),
        entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().format(FORMATTER));
  }

  private static Map<String, Object> migrateSeaExtraFields(Map<String, Object> extraFields) {
    if (extraFields == null || extraFields.isEmpty()) {
      return extraFields;
    }
    Map<String, Object> next = new java.util.LinkedHashMap<>(extraFields);
    boolean changed = false;
    changed |= moveExtraKey(next, "cf_seaFreightEff", "cf_sea_freight_eff");
    changed |= moveExtraKey(next, "cf_seaBunkerEff", "cf_sea_bunker_eff");
    changed |= moveExtraKey(next, "cf_seaOthersEff", "cf_sea_others_eff");
    return changed ? next : extraFields;
  }

  private static boolean moveExtraKey(
      Map<String, Object> map, String from, String to) {
    if (!map.containsKey(from)) {
      return false;
    }
    if (!map.containsKey(to) || map.get(to) == null || String.valueOf(map.get(to)).isBlank()) {
      map.put(to, map.get(from));
    }
    map.remove(from);
    return true;
  }
}
