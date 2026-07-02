package com.furuiduo.quote.masterdata.entity;

/** 港口档案类型：对应 POL / POD / POR 业务场景。 */
public enum PortType {
  /** 海港 — POL / POD */
  SEAPORT,
  /** 内陆点 / ICD — POR */
  INLAND,
  /** 铁路场站 — POR */
  RAIL,
  /** 机场 */
  AIRPORT,
  /** 其他运输节点 */
  OTHER;

  /** POL/POD 装港卸港 */
  public boolean supportsPolPod() {
    return this == SEAPORT;
  }

  /** POR 内陆提货点 */
  public boolean supportsPor() {
    return this == INLAND || this == RAIL;
  }
}
