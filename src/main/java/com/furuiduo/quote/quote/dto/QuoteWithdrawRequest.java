package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "撤回审批")
public record QuoteWithdrawRequest(
    @Schema(description = "撤回原因", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "请填写撤回原因")
    String comment) {}
