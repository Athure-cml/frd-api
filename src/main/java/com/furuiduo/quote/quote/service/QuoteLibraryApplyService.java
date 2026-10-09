package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.repository.MdGlobalPortRepository;
import com.furuiduo.quote.quoterule.QuoteRuleContext;
import com.furuiduo.quote.quoterule.service.QuoteRuleEngine;

/** 报价库：在成本库行上套用报价规则（仅改费用列）。 */
@Service
public class QuoteLibraryApplyService {

  private final MdGlobalPortRepository globalPortRepository;
  private final QuoteRuleEngine quoteRuleEngine;

  public QuoteLibraryApplyService(
      MdGlobalPortRepository globalPortRepository, QuoteRuleEngine quoteRuleEngine) {
    this.globalPortRepository = globalPortRepository;
    this.quoteRuleEngine = quoteRuleEngine;
  }

  public RoadCostResponse applyRoad(RoadCostResponse row) {
    if (row == null) {
      return null;
    }
    String por = text(row.por());
    QuoteRuleContext noFmCtx = buildContext(por, null, false);
    QuoteRuleContext fmCtx = buildContext(por, null, true);
    BigDecimal allInNoFm = applyTruckingFee(row.allInNoFm(), noFmCtx);
    BigDecimal allInFmOneWay = applyTruckingFee(row.allInFmOneWay(), fmCtx);
    BigDecimal allInFmRound = applyTruckingFee(row.allInFmRound(), fmCtx);
    return new RoadCostResponse(
        row.id(),
        row.zipCode(),
        row.city(),
        row.state(),
        row.por(),
        row.station(),
        row.pol(),
        row.supplier(),
        row.baseFreight(),
        row.fsc(),
        row.chassis(),
        row.triTandemAxle(),
        row.split(),
        row.stopOff(),
        allInNoFm,
        allInFmOneWay,
        allInFmRound,
        row.waitingFee(),
        row.redelivery(),
        row.prepull(),
        row.nsLift(),
        row.otherFee(),
        row.remark(),
        row.validDate(),
        row.logYardNameAddress(),
        row.status(),
        row.extraFields(),
        row.updatedAt(),
        row.highlight(),
        row.inQuoteLibrary(),
        row.quoteOrderLocked(),
        row.quoteCount());
  }

  public FreightCostResponse applySea(FreightCostResponse row) {
    if (row == null) {
      return null;
    }
    Map<String, Object> snap = seaSnapshot(row);
    QuoteRuleContext ctx = buildContext(text(row.por()), text(row.pod()), false);
    BigDecimal base = toBigDecimal(snap.get("allIn"));
    if (base == null) {
      base = toBigDecimal(snap.get("freight"));
    }
    BigDecimal processed =
        base != null ? quoteRuleEngine.applyDecimalTarget("OCEAN_FREIGHT", base, ctx) : null;
    return new FreightCostResponse(
        row.id(),
        row.por(),
        row.pol(),
        row.pod(),
        row.cnShortName(),
        row.enProductName(),
        row.containerType(),
        processed,
        row.freightValidDate(),
        row.buc(),
        row.bucValidDate(),
        row.ebs(),
        row.ebsValidDate(),
        row.gri(),
        row.griValidDate(),
        row.others(),
        row.othersValidDate(),
        processed,
        row.ssl(),
        row.agent(),
        row.remark(),
        row.status(),
        row.extraFields(),
        row.updatedAt(),
        row.highlight(),
        row.inQuoteLibrary(),
        row.quoteOrderLocked(),
        row.quoteCount());
  }

  /** 熏蒸报价库：四列 FM 费用分别套加价规则，不判断有效期。 */
  public RoadCostResponse applyRoadOverrides(
      RoadCostResponse row, Map<String, BigDecimal> overrides) {
    if (row == null || overrides == null || overrides.isEmpty()) {
      return row;
    }
    return new RoadCostResponse(
        row.id(),
        row.zipCode(),
        row.city(),
        row.state(),
        row.por(),
        row.station(),
        row.pol(),
        row.supplier(),
        row.baseFreight(),
        row.fsc(),
        row.chassis(),
        row.triTandemAxle(),
        row.split(),
        row.stopOff(),
        pick("allInNoFm", row.allInNoFm(), overrides),
        pick("allInFmOneWay", row.allInFmOneWay(), overrides),
        pick("allInFmRound", row.allInFmRound(), overrides),
        pick("waitingFee", row.waitingFee(), overrides),
        pick("redelivery", row.redelivery(), overrides),
        row.prepull(),
        pick("nsLift", row.nsLift(), overrides),
        row.otherFee(),
        row.remark(),
        row.validDate(),
        row.logYardNameAddress(),
        row.status(),
        row.extraFields(),
        row.updatedAt(),
        row.highlight(),
        row.inQuoteLibrary(),
        row.quoteOrderLocked(),
        row.quoteCount());
  }

  public FreightCostResponse applySeaOverrides(
      FreightCostResponse row, Map<String, BigDecimal> overrides) {
    if (row == null || overrides == null || overrides.isEmpty()) {
      return row;
    }
    BigDecimal allIn = pick("allIn", row.allIn(), overrides);
    BigDecimal freight = pick("freight", row.freight(), overrides);
    if (overrides.containsKey("allIn") && !overrides.containsKey("freight")) {
      freight = allIn;
    } else if (overrides.containsKey("freight") && !overrides.containsKey("allIn")) {
      allIn = freight;
    }
    return new FreightCostResponse(
        row.id(),
        row.por(),
        row.pol(),
        row.pod(),
        row.cnShortName(),
        row.enProductName(),
        row.containerType(),
        allIn,
        row.freightValidDate(),
        row.buc(),
        row.bucValidDate(),
        row.ebs(),
        row.ebsValidDate(),
        row.gri(),
        row.griValidDate(),
        row.others(),
        row.othersValidDate(),
        freight,
        row.ssl(),
        row.agent(),
        row.remark(),
        row.status(),
        row.extraFields(),
        row.updatedAt(),
        row.highlight(),
        row.inQuoteLibrary(),
        row.quoteOrderLocked(),
        row.quoteCount());
  }

  public FumigationCostResponse applyFumigationOverrides(
      FumigationCostResponse row, Map<String, BigDecimal> overrides) {
    if (row == null || overrides == null || overrides.isEmpty()) {
      return row;
    }
    return new FumigationCostResponse(
        row.id(),
        row.region(),
        row.station(),
        pick("outdoorNonOak", row.outdoorNonOak(), overrides),
        pick("outdoorOak", row.outdoorOak(), overrides),
        row.outdoorValidity(),
        pick("indoorNonOak", row.indoorNonOak(), overrides),
        pick("indoorOak", row.indoorOak(), overrides),
        row.indoorValidity(),
        row.address(),
        row.remark(),
        row.status(),
        row.extraFields(),
        row.updatedAt(),
        row.highlight(),
        row.inQuoteLibrary(),
        row.quoteOrderLocked(),
        row.quoteCount());
  }

  public FumigationCostResponse applyFumigation(FumigationCostResponse row) {
    if (row == null) {
      return null;
    }
    QuoteRuleContext ctx = buildContext(null, null, false);
    BigDecimal outdoorNonOak = applyFmNonOak(row.outdoorNonOak(), ctx);
    BigDecimal outdoorOak = applyFmOak(row.outdoorOak(), ctx);
    BigDecimal indoorNonOak = applyFmNonOak(row.indoorNonOak(), ctx);
    BigDecimal indoorOak = applyFmOak(row.indoorOak(), ctx);
    return new FumigationCostResponse(
        row.id(),
        row.region(),
        row.station(),
        outdoorNonOak,
        outdoorOak,
        row.outdoorValidity(),
        indoorNonOak,
        indoorOak,
        row.indoorValidity(),
        row.address(),
        row.remark(),
        row.status(),
        row.extraFields(),
        row.updatedAt(),
        row.highlight(),
        row.inQuoteLibrary(),
        row.quoteOrderLocked(),
        row.quoteCount());
  }

  private static BigDecimal pick(
      String field, BigDecimal current, Map<String, BigDecimal> overrides) {
    return overrides.containsKey(field) ? overrides.get(field) : current;
  }

  private BigDecimal applyTruckingFee(BigDecimal base, QuoteRuleContext ctx) {
    if (base == null) {
      return null;
    }
    return quoteRuleEngine.applyDecimalTarget("TRUCKING_FEE", base, ctx);
  }

  private BigDecimal applyFmNonOak(BigDecimal base, QuoteRuleContext ctx) {
    if (base == null) {
      return null;
    }
    return quoteRuleEngine.applyDecimalTarget(
        "FM_NON_OAK", base, ctx);
  }

  private BigDecimal applyFmOak(BigDecimal base, QuoteRuleContext ctx) {
    if (base == null) {
      return null;
    }
    return quoteRuleEngine.applyDecimalTarget("FM_OAK", base, ctx);
  }

  private QuoteRuleContext buildContext(String por, String pod, boolean fumigationEnabled) {
    return new QuoteRuleContext(fumigationEnabled, isChinaPort(pod), null, por);
  }

  private Map<String, Object> seaSnapshot(FreightCostResponse row) {
    return Map.of(
        "allIn", row.allIn() != null ? row.allIn() : "",
        "freight", row.freight() != null ? row.freight() : "",
        "por", row.por() != null ? row.por() : "",
        "pod", row.pod() != null ? row.pod() : "");
  }

  private boolean isChinaPort(String pod) {
    if (pod == null || pod.isBlank()) {
      return false;
    }
    List<MdGlobalPort> ports = globalPortRepository.findByNameEnIgnoreCase(pod.trim());
    for (MdGlobalPort port : ports) {
      String code = text(port.getCode());
      if (code != null && code.length() >= 2 && code.substring(0, 2).equalsIgnoreCase("CN")) {
        return true;
      }
      String country = text(port.getCountryRegion());
      if (country != null) {
        if ("CN".equalsIgnoreCase(country)
            || "CHINA".equalsIgnoreCase(country)
            || country.toUpperCase(Locale.ROOT).contains("CHINA")) {
          return true;
        }
      }
      if ("China".equalsIgnoreCase(text(port.getRoute()))) {
        return true;
      }
    }
    return false;
  }

  private String text(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private BigDecimal toBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    try {
      return new BigDecimal(String.valueOf(value).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
