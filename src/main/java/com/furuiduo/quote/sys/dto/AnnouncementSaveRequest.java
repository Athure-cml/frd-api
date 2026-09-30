package com.furuiduo.quote.sys.dto;

import java.time.LocalDateTime;

import com.furuiduo.quote.sys.entity.AnnouncementDisplayType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "保存系统公告")
public record AnnouncementSaveRequest(
    @Size(max = 128) @Schema(description = "标题；仅滚动条时可留空") String title,
    @Schema(description = "正文") String content,
    @Schema(description = "有效天数，空表示长期有效") Integer validDays,
    @NotNull @Schema(description = "展示方式") AnnouncementDisplayType displayType,
    @NotNull @Schema(description = "保存动作") AnnouncementSaveAction saveAction,
    @Schema(description = "预约发布时间") LocalDateTime scheduledAt) {}
