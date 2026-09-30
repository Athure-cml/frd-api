package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "发起报价变更")
public record QuoteReviseRequest(
    @Schema(description = "变更原因", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "变更原因不能为空")
        @Size(max = 512)
        String changeReason) {}
