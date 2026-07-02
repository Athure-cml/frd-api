package com.furuiduo.quote.user;

import java.time.LocalDateTime;

import com.furuiduo.quote.sys.entity.SysUser;

public final class PasswordStrengthEvaluator {

  private PasswordStrengthEvaluator() {}

  /** 与前端 Vben 密码强度算法一致，返回 0-5 */
  public static int evaluate(String password) {
    if (password == null || password.isBlank()) {
      return 0;
    }
    int strength = 0;
    if (password.length() >= 8) {
      strength++;
    }
    if (password.chars().anyMatch(ch -> Character.isLowerCase(ch))) {
      strength++;
    }
    if (password.chars().anyMatch(ch -> Character.isUpperCase(ch))) {
      strength++;
    }
    if (password.chars().anyMatch(Character::isDigit)) {
      strength++;
    }
    if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
      strength++;
    }
    return strength;
  }

  public static void apply(SysUser user, String rawPassword) {
    user.setPasswordStrength(evaluate(rawPassword));
    user.setPasswordUpdatedAt(LocalDateTime.now());
  }
}
