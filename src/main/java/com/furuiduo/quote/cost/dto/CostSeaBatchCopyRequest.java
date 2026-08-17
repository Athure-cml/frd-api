package com.furuiduo.quote.cost.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "海运成本批量复制")
public record CostSeaBatchCopyRequest(
    @Schema(description = "源记录 ID 列表") List<Long> ids,
    @Schema(description = "是否统一修改可覆盖字段；false 表示原样复制") Boolean applyOverrides,
    @Schema(description = "运费") BigDecimal freight,
    @Schema(description = "箱型") String containerType,
    @Schema(description = "运费生效期（写入 extraFields.cf_sea_freight_eff）") String freightEffDate,
    @Schema(description = "运费有效期") String freightValidDate,
    @Schema(description = "燃油附加费") BigDecimal buc,
    @Schema(description = "燃油附加费生效期（写入 extraFields.cf_sea_bunker_eff）") String bucEffDate,
    @Schema(description = "燃油附加费有效期") String bucValidDate,
    @Schema(description = "OTHERS 附加费") BigDecimal others,
    @Schema(description = "OTHERS 生效期（写入 extraFields.cf_sea_others_eff）") String othersEffDate,
    @Schema(description = "OTHERS 有效期") String othersValidDate) {}
