package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.repository.CostSeaRepository;
import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.repository.MdGlobalPortRepository;
import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.dto.QuoteGenerateSheetRequest;
import com.furuiduo.quote.quote.dto.QuoteGenerateSheetResponse;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;
import com.furuiduo.quote.quote.support.QuoteCostMatchKeys;
import com.furuiduo.quote.quote.support.QuoteCostMatchSupport;
import com.furuiduo.quote.quote.support.QuoteCostSnapshotMapper;
import com.furuiduo.quote.quoterule.QuoteRuleContext;
import com.furuiduo.quote.quoterule.service.QuoteRuleEngine;

@Service
public class QuoteSheetGenerateService {

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

  private final CostRoadRepository costRoadRepository;
  private final CostSeaRepository costSeaRepository;
  private final CostFumigationRepository costFumigationRepository;
  private final MdGlobalPortRepository globalPortRepository;
  private final QuoteRuleEngine quoteRuleEngine;

  public QuoteSheetGenerateService(
      CostRoadRepository costRoadRepository,
      CostSeaRepository costSeaRepository,
      CostFumigationRepository costFumigationRepository,
      MdGlobalPortRepository globalPortRepository,
      QuoteRuleEngine quoteRuleEngine) {
    this.costRoadRepository = costRoadRepository;
    this.costSeaRepository = costSeaRepository;
    this.costFumigationRepository = costFumigationRepository;
    this.globalPortRepository = globalPortRepository;
    this.quoteRuleEngine = quoteRuleEngine;
  }

  public QuoteGenerateSheetResponse generate(QuoteGenerateSheetRequest request) {
    String por = trim(request.por());
    String pol = trim(request.pol());
    String pod = trim(request.pod());

    // 必填：POR、POL 与 POD 未选择时不允许生成
    if (isBlank(por)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择 POR");
    }
    if (isBlank(pol)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择 POL");
    }
    if (isBlank(pod)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择 POD");
    }

    String fumigationPoint = trim(request.fumigationPoint());
    boolean fumigationEnabled =
        !isBlank(fumigationPoint) || Boolean.TRUE.equals(request.fumigationEnabled());

    // Date：自动读取系统当前日期，格式 yyyy-MM-dd
    LocalDate quoteDate =
        request.quoteDate() != null ? request.quoteDate() : LocalDate.now();
    String polForSea = QuoteCostMatchKeys.seaPol(request);
    String porForSea = QuoteCostMatchKeys.seaPor(request);

    Map<String, Object> matchKeys = new HashMap<>();
    putIfPresent(matchKeys, "por", por);
    putIfPresent(matchKeys, "pol", pol);
    putIfPresent(matchKeys, "fumigationPoint", fumigationPoint);
    putIfPresent(matchKeys, "station", QuoteCostMatchKeys.fumigationStation(request));
    putIfPresent(matchKeys, "pod", pod);
    putIfPresent(matchKeys, "city", request.city());
    putIfPresent(matchKeys, "state", request.state());
    putIfPresent(matchKeys, "zipCode", request.zipCode());

    List<QuoteCostMatchItemDto> matches = new ArrayList<>();

    QuoteRuleContext ruleContext =
        new QuoteRuleContext(
            fumigationEnabled, isChinaPort(pod), request.cifAmount(), por);

    String oceanFreight = null;
    String ssl = null;
    BigDecimal truckingFee = null;
    BigDecimal truckingNonOakUsd = null;
    BigDecimal truckingOakUsd = null;
    BigDecimal nsLift = null;
    BigDecimal chassis = null;
    BigDecimal waiting = null;
    BigDecimal redeliveryFee = null;
    String truckRemark = null;
    BigDecimal fmNonOakAmount = BigDecimal.ZERO;
    BigDecimal fmOakAmount = BigDecimal.ZERO;

    // 海运费：通过 POR、POL、POD 匹配海运成本库；仅引入生效中；取值 = ALL IN + 规则
    List<CostSea> seas =
        costSeaRepository.matchByRoute(
            SearchText.orEmpty(porForSea), SearchText.orEmpty(pod), "");
    var activeSea = QuoteCostMatchSupport.firstActiveSeaByPol(seas, polForSea);
    if (activeSea.isPresent()) {
      CostSea sea = activeSea.get();
      matches.add(QuoteCostSnapshotMapper.fromSea(sea, matchKeys));
      BigDecimal allIn = sea.getAllIn();
      if (allIn != null) {
        oceanFreight = quoteRuleEngine.formatDecimalTarget("OCEAN_FREIGHT", allIn, ruleContext);
      }
      ssl = trim(sea.getSsl());
    }

    // 卡车费：city/state 均已填时才匹配；熏蒸否取 ALL IN NO FM，熏蒸是取 ALL IN FM NON OAK / OAK
    if (QuoteCostMatchSupport.hasRoadLocationKeys(request.city(), request.state())) {
      List<CostRoad> roads =
          costRoadRepository.matchByRoute(
              SearchText.orEmpty(request.zipCode()),
              SearchText.orEmpty(request.city()),
              SearchText.orEmpty(request.state()),
              "",
              "",
              "");
      var activeRoad = QuoteCostMatchSupport.firstActiveRoad(roads);
      if (activeRoad.isPresent()) {
        CostRoad road = activeRoad.get();
        matches.add(QuoteCostSnapshotMapper.fromRoad(road, matchKeys));
        if (fumigationEnabled) {
          if (road.getAllInFmOneWay() != null) {
            truckingNonOakUsd =
                quoteRuleEngine.applyDecimalTarget(
                    "TRUCKING_FEE", road.getAllInFmOneWay(), ruleContext);
          }
          if (road.getAllInFmRound() != null) {
            truckingOakUsd =
                quoteRuleEngine.applyDecimalTarget(
                    "TRUCKING_FEE", road.getAllInFmRound(), ruleContext);
          }
        } else if (road.getAllInNoFm() != null) {
          truckingFee =
              quoteRuleEngine.applyDecimalTarget(
                  "TRUCKING_FEE", road.getAllInNoFm(), ruleContext);
        }
        nsLift = road.getNsLift();
        chassis = resolveExtraChassis(road);
        waiting = road.getWaitingFee();
        redeliveryFee = road.getRedelivery();
        truckRemark = trim(QuoteCostMatchSupport.resolveRoadRemark(road));
      }
    }

    // 熏蒸费：勾选「是否熏蒸」后，仅引入生效中记录，按报价日期匹配 FM-OUTDOOR / FM-INDOOR
    List<CostFumigation> fums =
        costFumigationRepository.matchByStation(
            SearchText.orEmpty(QuoteCostMatchKeys.fumigationStation(request)));
    var activeFum = QuoteCostMatchSupport.firstActiveFumigation(fums);
    if (fumigationEnabled && activeFum.isPresent()) {
      CostFumigation fum = activeFum.get();
      matches.add(QuoteCostSnapshotMapper.fromFumigation(fum, matchKeys));
      FumigationRates rates = resolveFumigationRates(fum, quoteDate);
      BigDecimal nonOakBase =
          rates.nonOak() != null ? rates.nonOak() : BigDecimal.ZERO;
      BigDecimal oakBase = rates.oak() != null ? rates.oak() : BigDecimal.ZERO;
      fmNonOakAmount =
          quoteRuleEngine.applyDecimalTarget("FM_NON_OAK", nonOakBase, ruleContext);
      fmOakAmount = quoteRuleEngine.applyDecimalTarget("FM_OAK", oakBase, ruleContext);
    }

    // 单证费、保险费、代理费：按主数据-报价单规则计算
    String docUsd = quoteRuleEngine.applyDocFee(ruleContext);
    String cargoInsurancePremium =
        quoteRuleEngine.applyCifTarget("CARGO_INSURANCE", ruleContext);
    String cargoAgentFee = quoteRuleEngine.applyCifTarget("CARGO_AGENT", ruleContext);

    QuoteSheetFieldsDto sheet =
        new QuoteSheetFieldsDto(
            trim(request.zipCode()),
            trim(request.city()),
            trim(request.state()),
            trim(request.pickUpAddress()),
            por,
            pol,
            pod,
            oceanFreight,
            ssl,
            truckingFee,
            nsLift,
            chassis,
            waiting,
            redeliveryFee,
            truckRemark,
            fmNonOakAmount,
            fmOakAmount,
            fumigationPoint,
            fumigationEnabled,
            docUsd,
            cargoInsurancePremium,
            cargoAgentFee,
            null,
            truckingNonOakUsd,
            truckingOakUsd,
            null,
            request.cifAmount());

    return new QuoteGenerateSheetResponse(
        quoteDate.format(DateTimeFormatter.ISO_LOCAL_DATE), sheet, matches);
  }

  /** 单证费：POD 对应港口国家为中国则 300，否则 350（美元） */
  private boolean isChinaPort(String pod) {
    List<MdGlobalPort> ports = globalPortRepository.findByNameEnIgnoreCase(pod);
    for (MdGlobalPort port : ports) {
      String code = trim(port.getCode());
      if (code.length() >= 2 && code.substring(0, 2).equalsIgnoreCase("CN")) {
        return true;
      }
      String country = trim(port.getCountryRegion());
      if (country != null) {
        if ("CN".equalsIgnoreCase(country)
            || "CHINA".equalsIgnoreCase(country)
            || country.toUpperCase(Locale.ROOT).contains("CHINA")) {
          return true;
        }
      }
      if ("China".equalsIgnoreCase(trim(port.getRoute()))) {
        return true;
      }
    }
    return false;
  }

  /** 额外底盘费 EXTRA CHASSIS：来自卡车成本库扩展字段 */
  private BigDecimal resolveExtraChassis(CostRoad road) {
    if (road.getExtraFields() != null) {
      Object extra = road.getExtraFields().get(ROAD_EXTRA_CHASSIS_KEY);
      BigDecimal parsed = toBigDecimal(extra);
      if (parsed != null) {
        return parsed;
      }
    }
    return road.getChassis();
  }

  /**
   * 熏蒸报价：报价日期落在 FM-OUTDOOR 有效期内优先取 outdoor，否则取 FM-INDOOR；
   * 有效期字段支持 YYYY-MM-DD 或历史区间（取结束日）。
   */
  private FumigationRates resolveFumigationRates(CostFumigation fum, LocalDate quoteDate) {
    LocalDate outdoorEnd = parseValidityEnd(fum.getOutdoorValidity());
    LocalDate indoorEnd = parseValidityEnd(fum.getIndoorValidity());
    boolean outdoorValid = outdoorEnd == null || !quoteDate.isAfter(outdoorEnd);
    boolean indoorValid = indoorEnd == null || !quoteDate.isAfter(indoorEnd);

    if (outdoorValid) {
      return new FumigationRates(fum.getOutdoorNonOak(), fum.getOutdoorOak());
    }
    if (indoorValid) {
      return new FumigationRates(fum.getIndoorNonOak(), fum.getIndoorOak());
    }
    return new FumigationRates(fum.getOutdoorNonOak(), fum.getOutdoorOak());
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

  private String trim(String value) {
    if (value == null) {
      return null;
    }
    String text = value.trim();
    return text.isEmpty() ? null : text;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String firstNonBlank(String primary, String fallback) {
    if (!isBlank(primary)) {
      return primary;
    }
    return fallback;
  }

  private void putIfPresent(Map<String, Object> map, String key, String value) {
    if (!isBlank(value)) {
      map.put(key, value.trim());
    }
  }

  private record FumigationRates(BigDecimal nonOak, BigDecimal oak) {}
}
