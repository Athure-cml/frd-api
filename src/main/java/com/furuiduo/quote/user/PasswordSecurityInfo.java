package com.furuiduo.quote.user;

import java.time.LocalDateTime;

import com.furuiduo.quote.sys.entity.SysUser;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "账户密码安全评估")
public record PasswordSecurityInfo(
    @Schema(description = "密码强度 0-5") Integer strength,
    @Schema(description = "强度等级：UNKNOWN/WEAK/MEDIUM/STRONG") String level,
    @Schema(description = "是否建议更改") boolean needsUpdate,
    @Schema(description = "密码最近更新时间") String updatedAt) {

  public static PasswordSecurityInfo from(SysUser user) {
    Integer strength = user.getPasswordStrength();
    LocalDateTime updatedAt = user.getPasswordUpdatedAt();
    String level;
    boolean needsUpdate;

    if (strength == null) {
      level = "UNKNOWN";
      needsUpdate = true;
    } else if (strength <= 2) {
      level = "WEAK";
      needsUpdate = true;
    } else if (strength == 3) {
      level = "MEDIUM";
      needsUpdate = true;
    } else {
      level = "STRONG";
      needsUpdate = false;
    }

    if (updatedAt != null && updatedAt.isBefore(LocalDateTime.now().minusDays(90))) {
      needsUpdate = true;
    }

    String updatedAtText =
        updatedAt == null ? null : updatedAt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    return new PasswordSecurityInfo(strength, level, needsUpdate, updatedAtText);
  }
}
