package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "提交审批")
public record QuoteSubmitRequest(
    @Schema(description = "提交备注（审批人可见，选填）")
        @Size(max = 500, message = "提交备注不能超过 500 字")
        String comment) {}
