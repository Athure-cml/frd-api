package com.furuiduo.quote.quoterule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PorListMatcherTest {

  private static final String REMARK =
      "NEW YORK,NY,NORFOLK,VA,SAVANNAH,GA,CHARLESTON,SC";

  @Test
  void matchesNewYorkWithSpaceAfterComma() {
    assertTrue(PorListMatcher.matches("NEW YORK, NY", REMARK));
  }

  @Test
  void matchesNewYorkWithoutSpace() {
    assertTrue(PorListMatcher.matches("NEW YORK,NY", REMARK));
  }

  @Test
  void rejectsLosAngeles() {
    assertFalse(PorListMatcher.matches("LOS ANGELES,CA", REMARK));
  }
}
