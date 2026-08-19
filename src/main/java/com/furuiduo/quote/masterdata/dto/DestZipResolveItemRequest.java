package com.furuiduo.quote.masterdata.dto;

public record DestZipResolveItemRequest(String city, String state, String zipCode) {

  public DestZipResolveItemRequest(String city, String state) {
    this(city, state, null);
  }
}
