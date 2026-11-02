package com.furuiduo.quote.quote.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "人工发送报价单邮件（指定收件邮箱）")
public record QuoteMailSendRequest(
    @Schema(description = "收件邮箱列表，可多个") List<String> toEmails) {}
