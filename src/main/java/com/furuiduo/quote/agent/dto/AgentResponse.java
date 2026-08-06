package com.furuiduo.quote.agent.dto;

import com.furuiduo.quote.agent.entity.Agent;
import com.furuiduo.quote.quote.support.QuoteDateTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "代理商")
public record AgentResponse(
    @Schema(description = "ID") Long id,
    @Schema(description = "代理商编码") String code,
    @Schema(description = "代理商名称") String name,
    @Schema(description = "邮箱") String email,
    @Schema(description = "备注") String remark,
    @Schema(description = "状态") Integer status,
    @Schema(description = "创建人") String createdByName,
    @Schema(description = "创建时间") String createdAt,
    @Schema(description = "更新时间") String updatedAt) {

  public static AgentResponse from(Agent entity) {
    return new AgentResponse(
        entity.getId(),
        entity.getCode(),
        entity.getName(),
        entity.getEmail(),
        entity.getRemark(),
        entity.getStatus(),
        entity.getCreatedByName(),
        QuoteDateTimes.format(entity.getCreatedAt()),
        QuoteDateTimes.format(entity.getUpdatedAt()));
  }
}
