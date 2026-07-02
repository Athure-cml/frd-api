package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;

import com.furuiduo.quote.quote.entity.QuoteOrder;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单业务表字段（与 Excel 列名英文一致）")
public record QuoteSheetFieldsDto(
    @Schema(description = "Zip code") String zipCode,
    @Schema(description = "City") String city,
    @Schema(description = "State") String state,
    @Schema(description = "POR") String por,
    @Schema(description = "POL") String pol,
    @Schema(description = "POD") String pod,
    @Schema(description = "O/F (USD)") String ofUsd,
    @Schema(description = "SSL") String ssl,
    @Schema(description = "TRUCKING NON OAK (USD)") BigDecimal truckingNonOakUsd,
    @Schema(description = "TRUCKING OAK (USD)") BigDecimal truckingOakUsd,
    @Schema(description = "FM NON OAK") BigDecimal fmNonOak,
    @Schema(description = "FM OAK") BigDecimal fmOak,
    @Schema(description = "DOC (USD)") String docUsd,
    @Schema(description = "CARGO Max weight (ton)") String cargoMaxWeightTon,
    @Schema(description = "REMARK") String sheetRemark) {

  public static QuoteSheetFieldsDto from(QuoteOrder order) {
    return new QuoteSheetFieldsDto(
        order.getZipCode(),
        order.getCity(),
        order.getState(),
        order.getPor(),
        order.getPol(),
        order.getPod(),
        order.getOfUsd(),
        order.getSsl(),
        order.getTruckingNonOakUsd(),
        order.getTruckingOakUsd(),
        order.getFmNonOak(),
        order.getFmOak(),
        order.getDocUsd(),
        order.getCargoMaxWeightTon(),
        order.getSheetRemark());
  }

  public boolean hasAnyMatchKey() {
    return isNotBlank(zipCode)
        || isNotBlank(city)
        || isNotBlank(state)
        || isNotBlank(por)
        || isNotBlank(pol)
        || isNotBlank(pod)
        || isNotBlank(ssl);
  }

  private static boolean isNotBlank(String value) {
    return value != null && !value.isBlank();
  }
}
