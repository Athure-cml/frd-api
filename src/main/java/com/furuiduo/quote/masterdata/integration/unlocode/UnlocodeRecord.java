package com.furuiduo.quote.masterdata.integration.unlocode;

import com.furuiduo.quote.masterdata.entity.PortType;

/** 清洗后的 UN/LOCODE 单条记录。 */
public record UnlocodeRecord(
    String code,
    String country,
    String nameEn,
    String subdivision,
    String functionCode,
    String locodeStatus,
    String changeFlag,
    PortType portType) {

  public boolean markedDeleted() {
    return changeFlag != null && changeFlag.toUpperCase().startsWith("X");
  }

  public boolean importable() {
    return !markedDeleted() && portType != PortType.OTHER;
  }
}
