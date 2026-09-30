package com.furuiduo.quote.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "单证费预览")
public record QuoteDocFeeResponse(@Schema(description = "DOC FEE") String docUsd) {}
