package com.furuiduo.quote.masterdata.integration.unlocode;

import com.furuiduo.quote.masterdata.entity.PortType;

final class UnlocodeFunctionClassifier {

  private UnlocodeFunctionClassifier() {}

  static PortType classify(String functionCode) {
    if (functionCode == null || functionCode.isBlank()) {
      return PortType.OTHER;
    }
    String fn = functionCode.trim();
    if (fn.length() >= 1 && fn.charAt(0) == '1') {
      return PortType.SEAPORT;
    }
    if (fn.length() >= 6 && fn.charAt(5) == '6') {
      return PortType.INLAND;
    }
    if (fn.length() >= 3 && fn.charAt(2) == '3') {
      return PortType.INLAND;
    }
    if (fn.length() >= 2 && fn.charAt(1) == '2') {
      return PortType.RAIL;
    }
    if (fn.length() >= 4 && fn.charAt(3) == '4') {
      return PortType.AIRPORT;
    }
    return PortType.OTHER;
  }
}
