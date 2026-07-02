package com.furuiduo.quote.common;

/** JPQL 可选文本筛选：PostgreSQL 18 下 null 字符串参数可能绑定为 bytea，统一用空串表示“不筛选”。 */
public final class SearchText {

  private SearchText() {}

  public static String orEmpty(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim();
  }
}
