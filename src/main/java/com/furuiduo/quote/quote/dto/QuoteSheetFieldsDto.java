package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.furuiduo.quote.quote.entity.QuoteOrder;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单业务表字段（与列表 Excel 表头一致）")
public record QuoteSheetFieldsDto(
    @Schema(description = "Zip code（成本匹配）") String zipCode,
    @Schema(description = "City（成本匹配）") String city,
    @Schema(description = "State（成本匹配）") String state,
    @Schema(description = "PICK UP ADDRESS") String pickUpAddress,
    @Schema(description = "POR") String por,
    @Schema(description = "POL") String pol,
    @Schema(description = "POD") String pod,
    @Schema(description = "OCEAN FREIGHT") String oceanFreight,
    @Schema(description = "SSL（成本匹配）") String ssl,
    @Schema(description = "TRUCKING FEE") BigDecimal truckingFee,
    @Schema(description = "NS LIFT") BigDecimal nsLift,
    @Schema(description = "CHASSIS") BigDecimal chassis,
    @Schema(description = "WAITING") BigDecimal waiting,
    @Schema(description = "REDELIVERY FEE") BigDecimal redeliveryFee,
    @Schema(description = "TRUCK REMARK") String truckRemark,
    @Schema(description = "FM (NON-OAK)") BigDecimal fmNonOak,
    @Schema(description = "FM (OAK)") BigDecimal fmOak,
    @Schema(description = "是否熏蒸") Boolean fumigationEnabled,
    @Schema(description = "DOC FEE") String docUsd,
    @Schema(description = "CARGO INSURANCE PREMIUM") String cargoInsurancePremium,
    @Schema(description = "CARGO AGENT FEE") String cargoAgentFee,
    @Schema(description = "REMARK") String sheetRemark,
    @Schema(description = "TRUCKING NON OAK (USD)，兼容旧数据") BigDecimal truckingNonOakUsd,
    @Schema(description = "TRUCKING OAK (USD)，兼容旧数据") BigDecimal truckingOakUsd,
    @Schema(description = "CARGO Max weight (ton)，兼容旧数据") String cargoMaxWeightTon,
    @Schema(description = "CIF 货值（保险/代理费计算）") BigDecimal cifAmount) {

  public static QuoteSheetFieldsDto from(QuoteOrder order) {
    return new QuoteSheetFieldsDto(
        order.getZipCode(),
        order.getCity(),
        order.getState(),
        resolvePickUpAddress(order),
        order.getPor(),
        order.getPol(),
        order.getPod(),
        order.getOfUsd(),
        order.getSsl(),
        defaultTruckingFee(order),
        order.getNsLift(),
        order.getChassis(),
        order.getWaiting(),
        order.getRedeliveryFee(),
        order.getTruckRemark(),
        order.getFmNonOak(),
        order.getFmOak(),
        order.getFumigationEnabled(),
        order.getDocUsd(),
        firstNonBlank(order.getCargoInsurancePremium(), order.getCargoMaxWeightTon()),
        order.getCargoAgentFee(),
        order.getSheetRemark(),
        order.getTruckingNonOakUsd(),
        order.getTruckingOakUsd(),
        order.getCargoMaxWeightTon(),
        order.getCifAmount());
  }

  public String porPol() {
    if (isNotBlank(por) && isNotBlank(pol) && !por.equals(pol)) {
      return por + "/" + pol;
    }
    return firstNonBlank(por, pol);
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

  private static String resolvePickUpAddress(QuoteOrder order) {
    if (isNotBlank(order.getPickUpAddress())) {
      return order.getPickUpAddress().trim();
    }
    List<String> parts = new ArrayList<>();
    if (isNotBlank(order.getZipCode())) {
      parts.add(order.getZipCode().trim());
    }
    if (isNotBlank(order.getCity())) {
      parts.add(order.getCity().trim());
    }
    if (isNotBlank(order.getState())) {
      parts.add(order.getState().trim());
    }
    if (parts.isEmpty()) {
      return null;
    }
    return parts.stream().collect(Collectors.joining(", "));
  }

  private static BigDecimal defaultTruckingFee(QuoteOrder order) {
    if (order.getTruckingFee() != null) {
      return order.getTruckingFee();
    }
    return order.getTruckingNonOakUsd();
  }

  private static String firstNonBlank(String primary, String fallback) {
    if (isNotBlank(primary)) {
      return primary.trim();
    }
    if (isNotBlank(fallback)) {
      return fallback.trim();
    }
    return null;
  }

  private static boolean isNotBlank(String value) {
    return value != null && !value.isBlank();
  }
}
