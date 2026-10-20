package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "审批意见")
public record QuoteApprovalCommentRequest(
    @Schema(description = "审批意见（同意非必填；驳回必填）") String comment) {}
