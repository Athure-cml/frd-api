package com.furuiduo.quote.masterdata.integration.freightutils;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FreightUnlocodeResponse(
    String country, String function, int count, List<FreightUnlocodePort> results) {}
