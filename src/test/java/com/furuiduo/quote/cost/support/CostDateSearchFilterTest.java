package com.furuiduo.quote.cost.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class CostDateSearchFilterTest {

  @Test
  void matchesEffFrom_supportsIsoSearchAndSlashStorage() {
    assertTrue(CostDateSearchFilter.matchesEffFrom("2026/08/01", "2026-08-01"));
    assertTrue(CostDateSearchFilter.matchesEffFrom("2026/08/19", "2026-08-01"));
    assertFalse(CostDateSearchFilter.matchesEffFrom("2026/07/31", "2026-08-01"));
  }

  @Test
  void matchesValidTo_supportsIsoSearchAndSlashStorage() {
    assertTrue(CostDateSearchFilter.matchesValidTo("2026/08/19", "2026-08-19"));
    assertTrue(CostDateSearchFilter.matchesValidTo("2026/08/01", "2026-08-19"));
    assertFalse(CostDateSearchFilter.matchesValidTo("2026/08/20", "2026-08-19"));
  }

  @Test
  void matchesValidTo_supportsMonthDayOnlyStorage() {
    assertTrue(CostDateSearchFilter.matchesValidTo("08/19", "2026-08-19"));
    assertFalse(CostDateSearchFilter.matchesValidTo("08/20", "2026-08-19"));
  }

  @Test
  void matchesRange_withBothSearchDates() {
    assertTrue(
        CostDateSearchFilter.matchesRange(
            "2026/08/01", "2026/08/19", "2026-08-01", "2026-08-31"));
    assertFalse(
        CostDateSearchFilter.matchesRange(
            "2026/07/01", "2026/08/19", "2026-08-01", "2026-08-31"));
    assertFalse(
        CostDateSearchFilter.matchesRange(
            "2026/08/01", "2026/09/01", "2026-08-01", "2026-08-31"));
  }

  @Test
  void resolveSeaFreightEff_readsExtraOrRangeStart() {
    assertTrue(
        CostDateSearchFilter.matchesEffFrom(
            CostDateSearchFilter.resolveSeaFreightEff(
                Map.of("cf_sea_freight_eff", "2026/08/01"), null),
            "2026-08-01"));
    assertTrue(
        CostDateSearchFilter.matchesEffFrom(
            CostDateSearchFilter.resolveSeaFreightEff(
                Map.of(), "2026/08/01 - 2026/12/31"),
            "2026-08-01"));
  }

  @Test
  void matchesValidTo_usesRangeEndDate() {
    assertTrue(
        CostDateSearchFilter.matchesValidTo("2026/08/01 - 2026/08/19", "2026-08-19"));
    assertFalse(
        CostDateSearchFilter.matchesValidTo("2026/08/01 - 2026/08/19", "2026-08-18"));
  }
}
