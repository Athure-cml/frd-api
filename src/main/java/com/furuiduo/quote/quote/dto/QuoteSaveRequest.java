package com.furuiduo.quote.quote.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteTransportMode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报价单保存")
public record QuoteSaveRequest(
    @Schema(description = "客户ID") Long customerId,
    @Schema(description = "客户名称") String customerName,
    @Schema(description = "运输方式") String transportMode,
    @Schema(description = "路线摘要") String routeSummary,
    @Schema(description = "币种") String currency,
    @Schema(description = "有效期至") LocalDate validUntil,
    @Schema(description = "备注") String remark,
    @Schema(description = "Zip code") String zipCode,
    @Schema(description = "City") String city,
    @Schema(description = "State") String state,
    @Schema(description = "PICK UP ADDRESS") String pickUpAddress,
    @Schema(description = "POR") String por,
    @Schema(description = "POL") String pol,
    @Schema(description = "POD") String pod,
    @Schema(description = "OCEAN FREIGHT") String oceanFreight,
    @Schema(description = "SSL") String ssl,
    @Schema(description = "TRUCKING FEE") BigDecimal truckingFee,
    @Schema(description = "NS LIFT") BigDecimal nsLift,
    @Schema(description = "CHASSIS") BigDecimal chassis,
    @Schema(description = "WAITING") BigDecimal waiting,
    @Schema(description = "REDELIVERY FEE") BigDecimal redeliveryFee,
    @Schema(description = "TRUCK REMARK") String truckRemark,
    @Schema(description = "FM NON OAK") BigDecimal fmNonOak,
    @Schema(description = "FM OAK") BigDecimal fmOak,
    @Schema(description = "是否熏蒸") Boolean fumigationEnabled,
    @Schema(description = "DOC FEE") String docUsd,
    @Schema(description = "CARGO INSURANCE PREMIUM") String cargoInsurancePremium,
    @Schema(description = "CARGO AGENT FEE") String cargoAgentFee,
    @Schema(description = "REMARK") String sheetRemark,
    @Schema(description = "TRUCKING NON OAK (USD)，兼容") BigDecimal truckingNonOakUsd,
    @Schema(description = "TRUCKING OAK (USD)，兼容") BigDecimal truckingOakUsd,
    @Schema(description = "CARGO Max weight (ton)，兼容") String cargoMaxWeightTon,
    @Schema(description = "CIF 货值") BigDecimal cifAmount,
    @Schema(description = "跟进人ID") Long followUpBy,
    @Schema(description = "跟进人") String followUpByName,
    @Schema(description = "明细行") List<QuoteLineSaveRequest> lines,
    @Schema(description = "成本匹配结果（引入成本库后传入以冻结快照）")
        List<QuoteCostMatchItemDto> costMatches) {

  public QuoteTransportMode parsedTransportMode() {
    return QuoteTransportMode.valueOf(transportMode);
  }

  /** 兼容旧前端 ofUsd 字段名 */
  public String ofUsd() {
    return oceanFreight;
  }

  public static QuoteSaveRequest fromOrder(QuoteOrder order) {
    List<QuoteLineSaveRequest> lines =
        order.getLines().stream().map(QuoteLineSaveRequest::from).toList();
    return new QuoteSaveRequest(
        order.getCustomerId(),
        order.getCustomerName(),
        order.getTransportMode().name(),
        order.getRouteSummary(),
        order.getCurrency(),
        order.getValidUntil(),
        order.getRemark(),
        order.getZipCode(),
        order.getCity(),
        order.getState(),
        order.getPickUpAddress(),
        order.getPor(),
        order.getPol(),
        order.getPod(),
        order.getOfUsd(),
        order.getSsl(),
        order.getTruckingFee() != null ? order.getTruckingFee() : order.getTruckingNonOakUsd(),
        order.getNsLift(),
        order.getChassis(),
        order.getWaiting(),
        order.getRedeliveryFee(),
        order.getTruckRemark(),
        order.getFmNonOak(),
        order.getFmOak(),
        order.getFumigationEnabled(),
        order.getDocUsd(),
        order.getCargoInsurancePremium(),
        order.getCargoAgentFee(),
        order.getSheetRemark(),
        order.getTruckingNonOakUsd(),
        order.getTruckingOakUsd(),
        order.getCargoMaxWeightTon(),
        order.getCifAmount(),
        order.getFollowUpBy(),
        order.getFollowUpByName(),
        new ArrayList<>(lines),
        null);
  }
}
