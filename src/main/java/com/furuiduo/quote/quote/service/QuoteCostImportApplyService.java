package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.repository.MdGlobalPortRepository;
import com.furuiduo.quote.quote.dto.QuoteApplyCostImportRequest;
import com.furuiduo.quote.quote.dto.QuoteApplyCostImportResponse;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;
import com.furuiduo.quote.quote.entity.QuoteCostType;
import com.furuiduo.quote.quote.support.QuoteCostMatchSupport;
import com.furuiduo.quote.quoterule.QuoteRuleContext;
import com.furuiduo.quote.quoterule.service.QuoteRuleEngine;

@Service
public class QuoteCostImportApplyService {

  private static final String ROAD_EXTRA_CHASSIS_KEY = "cf_road_extra_chassis";

  private static final Pattern VALIDITY_RANGE =
      Pattern.compile(
          "^(\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\s*[-–—]\\s*(\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})$");

  private static final DateTimeFormatter[] VALIDITY_FORMATS =
      new DateTimeFormatter[] {
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy-M-d"),
        DateTimeFormatter.ISO_LOCAL_DATE
      };

  private final MdGlobalPortRepository globalPortRepository;
  private final QuoteRuleEngine quoteRuleEngine;

  public QuoteCostImportApplyService(
      MdGlobalPortRepository globalPortRepository, QuoteRuleEngine quoteRuleEngine) {
    this.globalPortRepository = globalPortRepository;
    this.quoteRuleEngine = quoteRuleEngine;
  }

  public QuoteApplyCostImportResponse apply(QuoteApplyCostImportRequest request) {
    if (request.costType() == null || request.costType().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "costType 不能为空");
    }
    if (request.snapshot() == null || request.snapshot().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshot 不能为空");
    }
    QuoteCostType type;
    try {
      type = QuoteCostType.valueOf(request.costType().trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效 costType");
    }

    boolean fumigationEnabled =
        (request.fumigationPoint() != null && !request.fumigationPoint().isBlank())
            || Boolean.TRUE.equals(request.fumigationEnabled());
    QuoteRuleContext ruleContext =
        new QuoteRuleContext(
            fumigationEnabled,
            isChinaPort(request.pod()),
            request.cifAmount(),
            request.por());
    LocalDate quoteDate =
        request.quoteDate() != null && !request.quoteDate().isBlank()
            ? LocalDate.parse(request.quoteDate().trim())
            : LocalDate.now();

    QuoteSheetFieldsDto fields =
        switch (type) {
          case ROAD -> applyRoad(request.snapshot(), ruleContext, fumigationEnabled);
          case SEA -> applySea(request.snapshot(), ruleContext);
          case FUMIGATION -> applyFumigation(request.snapshot(), ruleContext, quoteDate);
        };
    return new QuoteApplyCostImportResponse(fields);
  }

  private QuoteSheetFieldsDto applyRoad(
      Map<String, Object> snap, QuoteRuleContext ctx, boolean fumigationEnabled) {
    BigDecimal truckingFee = null;
    BigDecimal truckingNonOakUsd = null;
    BigDecimal truckingOakUsd = null;

    if (fumigationEnabled) {
      BigDecimal fmNonOakBase = toBigDecimal(snap.get("allInFmOneWay"));
      BigDecimal fmOakBase = toBigDecimal(snap.get("allInFmRound"));
      if (fmNonOakBase != null) {
        truckingNonOakUsd = quoteRuleEngine.applyDecimalTarget("TRUCKING_FEE", fmNonOakBase, ctx);
      }
      if (fmOakBase != null) {
        truckingOakUsd = quoteRuleEngine.applyDecimalTarget("TRUCKING_FEE", fmOakBase, ctx);
      }
    } else {
      BigDecimal noFmBase = toBigDecimal(snap.get("allInNoFm"));
      if (noFmBase != null) {
        truckingFee = quoteRuleEngine.applyDecimalTarget("TRUCKING_FEE", noFmBase, ctx);
      }
    }

    return new QuoteSheetFieldsDto(
        text(snap.get("zipCode")),
        text(snap.get("city")),
        text(snap.get("state")),
        text(snap.get("logYardNameAddress")),
        text(snap.get("por")),
        text(snap.get("pol")),
        null,
        null,
        null,
        truckingFee,
        toBigDecimal(snap.get("nsLift")),
        resolveExtraChassis(snap),
        toBigDecimal(snap.get("waitingFee")),
        toBigDecimal(snap.get("redelivery")),
        QuoteCostMatchSupport.resolveRoadRemarkFromSnapshot(snap),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        truckingNonOakUsd,
        truckingOakUsd,
        null,
        null);
  }

  private QuoteSheetFieldsDto applySea(Map<String, Object> snap, QuoteRuleContext ctx) {
    BigDecimal allIn = toBigDecimal(snap.get("allIn"));
    if (allIn == null) {
      allIn = toBigDecimal(snap.get("freight"));
    }
    String oceanFreight = null;
    if (allIn != null) {
      oceanFreight = quoteRuleEngine.formatDecimalTarget("OCEAN_FREIGHT", allIn, ctx);
    }
    return new QuoteSheetFieldsDto(
        null,
        null,
        null,
        null,
        text(snap.get("por")),
        text(snap.get("pol")),
        text(snap.get("pod")),
        oceanFreight,
        text(snap.get("ssl")),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private QuoteSheetFieldsDto applyFumigation(
      Map<String, Object> snap, QuoteRuleContext ctx, LocalDate quoteDate) {
    FumigationRates rates = resolveFumigationRatesFromSnapshot(snap, quoteDate);
    BigDecimal nonOakBase =
        rates.nonOak() != null ? rates.nonOak() : BigDecimal.ZERO;
    BigDecimal oakBase = rates.oak() != null ? rates.oak() : BigDecimal.ZERO;
    BigDecimal fmNonOak =
        quoteRuleEngine.applyDecimalTarget("FM_NON_OAK", nonOakBase, ctx);
    BigDecimal fmOak = quoteRuleEngine.applyDecimalTarget("FM_OAK", oakBase, ctx);
    return new QuoteSheetFieldsDto(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        fmNonOak,
        fmOak,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private BigDecimal resolveExtraChassis(Map<String, Object> snap) {
    Object extraObj = snap.get("extraFields");
    if (extraObj instanceof Map<?, ?> extra) {
      Object value = extra.get(ROAD_EXTRA_CHASSIS_KEY);
      BigDecimal parsed = toBigDecimal(value);
      if (parsed != null) {
        return parsed;
      }
    }
    return toBigDecimal(snap.get("chassis"));
  }

  private FumigationRates resolveFumigationRatesFromSnapshot(
      Map<String, Object> snap, LocalDate quoteDate) {
    LocalDate outdoorEnd = parseValidityEnd(text(snap.get("outdoorValidity")));
    LocalDate indoorEnd = parseValidityEnd(text(snap.get("indoorValidity")));
    boolean outdoorValid = outdoorEnd == null || !quoteDate.isAfter(outdoorEnd);
    boolean indoorValid = indoorEnd == null || !quoteDate.isAfter(indoorEnd);

    if (outdoorValid) {
      return new FumigationRates(
          toBigDecimal(snap.get("outdoorNonOak")), toBigDecimal(snap.get("outdoorOak")));
    }
    if (indoorValid) {
      return new FumigationRates(
          toBigDecimal(snap.get("indoorNonOak")), toBigDecimal(snap.get("indoorOak")));
    }
    return new FumigationRates(
        toBigDecimal(snap.get("outdoorNonOak")), toBigDecimal(snap.get("outdoorOak")));
  }

  private LocalDate parseValidityEnd(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String text = raw.trim();
    Matcher matcher = VALIDITY_RANGE.matcher(text);
    if (matcher.matches()) {
      text = matcher.group(2);
    }
    for (DateTimeFormatter formatter : VALIDITY_FORMATS) {
      try {
        return LocalDate.parse(text, formatter);
      } catch (DateTimeParseException ignored) {
        // try next
      }
    }
    return null;
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

  private record FumigationRates(BigDecimal nonOak, BigDecimal oak) {}
}
