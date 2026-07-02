package com.furuiduo.quote.masterdata.integration.unlocode;

import java.util.List;

/** UN/LOCODE 数据集及版本标识。 */
public record UnlocodeDataset(List<UnlocodeRecord> records, String dataVersion) {}
