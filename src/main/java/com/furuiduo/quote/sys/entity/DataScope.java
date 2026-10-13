package com.furuiduo.quote.sys.entity;

public enum DataScope {
  ALL(4),
  DEPT(3),
  /** 本人 + 角色指定用户 */
  ASSIGNED(2),
  SELF(1);

  private final int priority;

  DataScope(int priority) {
    this.priority = priority;
  }

  public int priority() {
    return priority;
  }

  public static DataScope widest(DataScope left, DataScope right) {
    return left.priority >= right.priority ? left : right;
  }
}
