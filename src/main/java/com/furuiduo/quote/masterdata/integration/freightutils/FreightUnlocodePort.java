package com.furuiduo.quote.masterdata.integration.freightutils;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** FreightUtils /api/unlocode 单条港口记录。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FreightUnlocodePort(
    String code,
    String country,
    String name,
    @JsonProperty("name_ascii") String nameAscii,
    String subdivision,
    List<String> functions,
    String status) {

  public boolean isSeaport() {
    return functions != null && functions.contains("port");
  }
}
