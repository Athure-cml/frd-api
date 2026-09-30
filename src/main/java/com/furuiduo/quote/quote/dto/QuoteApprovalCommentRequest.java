package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "审批意见")
public record QuoteApprovalCommentRequest(
    @Schema(description = "审批意见", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "请填写审批意见")
        String comment) {}
