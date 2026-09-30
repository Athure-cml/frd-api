package com.furuiduo.quote.cost.support;

public final class CostBatchPreviewSupport {

  private CostBatchPreviewSupport() {}

  public static int resolvePreviewLimit(boolean previewOnly, Integer previewLimit) {
    if (!previewOnly) {
      return 0;
    }
    if (previewLimit == null || previewLimit <= 0) {
      return 50;
    }
    return Math.min(previewLimit, 200);
  }
}
