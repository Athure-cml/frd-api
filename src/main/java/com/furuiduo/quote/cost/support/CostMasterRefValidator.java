package com.furuiduo.quote.cost.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.furuiduo.quote.agent.repository.AgentRepository;
import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.support.CostRoadZipPlaceholder;
import com.furuiduo.quote.masterdata.entity.PortType;
import com.furuiduo.quote.masterdata.repository.MdContainerTypeRepository;
import com.furuiduo.quote.masterdata.repository.MdDestCityRepository;
import com.furuiduo.quote.masterdata.repository.MdDestZipRepository;
import com.furuiduo.quote.masterdata.repository.MdGlobalPortRepository;
import com.furuiduo.quote.masterdata.repository.MdUsStateRepository;
import com.furuiduo.quote.shippingline.repository.ShippingLineRepository;
import com.furuiduo.quote.supplier.repository.SupplierRepository;
import com.furuiduo.quote.unit.repository.UnitRepository;

/**
 * 成本库导入：下拉主数据存在性校验。客商字段支持全称/简称识别，落库统一写全称。
 */
@Component
public class CostMasterRefValidator {

  private static final List<PortType> ROAD_PORT_TYPES =
      List.of(PortType.SEAPORT, PortType.RAIL, PortType.INLAND);
  private static final List<PortType> SEA_POR_TYPES =
      List.of(PortType.SEAPORT, PortType.RAIL, PortType.INLAND);
  private static final List<PortType> SEA_SEAPORT_TYPES = List.of(PortType.SEAPORT);

  private final MdUsStateRepository usStateRepository;
  private final MdDestCityRepository destCityRepository;
  private final MdDestZipRepository destZipRepository;
  private final MdGlobalPortRepository globalPortRepository;
  private final MdContainerTypeRepository containerTypeRepository;
  private final SupplierRepository supplierRepository;
  private final ShippingLineRepository shippingLineRepository;
  private final AgentRepository agentRepository;
  private final UnitRepository unitRepository;

  public CostMasterRefValidator(
      MdUsStateRepository usStateRepository,
      MdDestCityRepository destCityRepository,
      MdDestZipRepository destZipRepository,
      MdGlobalPortRepository globalPortRepository,
      MdContainerTypeRepository containerTypeRepository,
      SupplierRepository supplierRepository,
      ShippingLineRepository shippingLineRepository,
      AgentRepository agentRepository,
      UnitRepository unitRepository) {
    this.usStateRepository = usStateRepository;
    this.destCityRepository = destCityRepository;
    this.destZipRepository = destZipRepository;
    this.globalPortRepository = globalPortRepository;
    this.containerTypeRepository = containerTypeRepository;
    this.supplierRepository = supplierRepository;
    this.shippingLineRepository = shippingLineRepository;
    this.agentRepository = agentRepository;
    this.unitRepository = unitRepository;
  }

  public String validateRoad(CostRoad entity) {
    if (entity == null) {
      return null;
    }
    resolveCityRoad(entity);
    String unitError = resolveRoadUnits(entity);
    if (unitError != null) {
      return unitError;
    }
    String zipCode = entity.getZipCode();
    String error = requireState(entity.getState());
    if (error != null) {
      return error;
    }
    if (!CostRoadZipPlaceholder.skipsCityMasterCheck(zipCode)) {
      error = requireCity(entity.getCity(), entity.getState(), "CITY");
      if (error != null) {
        return error;
      }
    }
    if (!CostRoadZipPlaceholder.skipsZipMasterCheck(zipCode)) {
      error = requireZip(zipCode);
      if (error != null) {
        return error;
      }
    }
    error = requirePort(entity.getPor(), "POR", ROAD_PORT_TYPES);
    if (error != null) {
      return error;
    }
    error = requirePort(entity.getPol(), "POL", ROAD_PORT_TYPES);
    if (error != null) {
      return error;
    }
    return resolveSupplierTruck(entity);
  }

  public String validateSea(CostSea entity) {
    if (entity == null) {
      return null;
    }
    String     error = requirePort(entity.getPor(), "POR", SEA_POR_TYPES);
    if (error != null) {
      return error;
    }
    error = requirePorts(entity.getPol(), "POL", SEA_SEAPORT_TYPES);
    if (error != null) {
      return error;
    }
    error = requirePort(entity.getPod(), "POD", SEA_SEAPORT_TYPES);
    if (error != null) {
      return error;
    }
    error = requireContainerTypes(entity.getContainerType());
    if (error != null) {
      return error;
    }
    error = resolveShippingLine(entity);
    if (error != null) {
      return error;
    }
    return resolveAgent(entity);
  }

  public String validateFumigation(CostFumigation entity) {
    if (entity == null) {
      return null;
    }
    return requireCityName(entity.getRegion(), "REGION");
  }

  private String requireState(String state) {
    if (isBlank(state)) {
      return null;
    }
    String code = normalizeToken(state);
    if (usStateRepository.findByCodeNormalized(code).isEmpty()) {
      return missing("STATE", code.isEmpty() ? state : code);
    }
    return null;
  }

  private String requireCity(String city, String state, String label) {
    if (isBlank(city)) {
      return null;
    }
    String name = normalizeToken(city);
    if (!isBlank(state)) {
      String stateCode = normalizeToken(state);
      var stateEntity = usStateRepository.findByCodeNormalized(stateCode);
      if (stateEntity.isEmpty()) {
        return missing("STATE", stateCode.isEmpty() ? state : stateCode);
      }
      if (destCityRepository.findByStateIdAndNameIgnoreCase(stateEntity.get().getId(), name).isEmpty()) {
        return missing(label, name.isEmpty() ? city : name);
      }
      return null;
    }
    if (!destCityRepository.existsByNameIgnoreCase(name)) {
      return missing(label, name.isEmpty() ? city : name);
    }
    return null;
  }

  private String requireCityName(String city, String label) {
    if (isBlank(city)) {
      return null;
    }
    String name = normalizeToken(city);
    if (!destCityRepository.existsByNameIgnoreCase(name)) {
      return missing(label, name.isEmpty() ? city : name);
    }
    return null;
  }

  private String requireZip(String zipCode) {
    if (isBlank(zipCode)) {
      return null;
    }
    if (CostRoadZipPlaceholder.skipsZipMasterCheck(zipCode)) {
      return null;
    }
    String zip = normalizeToken(zipCode);
    if (!destZipRepository.existsByZipCodeIgnoreCase(zip)) {
      return missing("ZIP CODE", zip.isEmpty() ? zipCode : zip);
    }
    return null;
  }

  private String requirePort(String name, String label, Collection<PortType> types) {
    if (isBlank(name)) {
      return null;
    }
    String port = canonicalPortName(name);
    if (!globalPortRepository.existsByNameEnIgnoreCase(port, types, true)) {
      return missing(label, displayPortName(name));
    }
    return null;
  }

  private String requirePorts(String raw, String label, Collection<PortType> types) {
    for (String name : splitSlashValues(raw)) {
      String error = requirePort(name, label, types);
      if (error != null) {
        return error;
      }
    }
    return null;
  }

  private String requireContainerTypes(String raw) {
    for (String code : splitSlashValues(raw)) {
      if (!containerTypeRepository.existsByCodeIgnoreCase(code, 1)) {
        return missing("箱型", code);
      }
    }
    return null;
  }

  /** 卡车 CITY：按主数据规范大小写回写（City+State 或 ZIP+State 匹配）。 */
  public void resolveCityRoad(CostRoad entity) {
    if (entity == null || isBlank(entity.getCity())) {
      return;
    }
    if (CostRoadZipPlaceholder.skipsCityMasterCheck(entity.getZipCode())) {
      return;
    }
    String city = normalizeToken(entity.getCity());
    String state = normalizeToken(entity.getState());
    String zip = normalizeToken(entity.getZipCode());

    if (!isBlank(state)) {
      var stateEntity = usStateRepository.findByCodeNormalized(state);
      if (stateEntity.isPresent()) {
        var found =
            destCityRepository.findByStateIdAndNameIgnoreCase(stateEntity.get().getId(), city);
        if (found.isPresent()) {
          entity.setCity(found.get().getName());
          return;
        }
      }
    }

    if (!isBlank(zip) && !CostRoadZipPlaceholder.skipsZipMasterCheck(zip)) {
      List<String> names = destZipRepository.findDistinctCityNamesByZipCode(zip, state);
      if (names.size() == 1) {
        entity.setCity(names.get(0));
      }
    }
  }

  /** 卡车费用单位：按单位主数据 code/name 忽略大小写匹配，回写规范 code。 */
  public String resolveRoadUnits(CostRoad entity) {
    if (entity == null || entity.getExtraFields() == null || entity.getExtraFields().isEmpty()) {
      return null;
    }
    Map<String, Object> extra = entity.getExtraFields();
    for (String field : CostTemplateLayoutTools.roadFeeUnitFieldKeys()) {
      Object raw = extra.get(field);
      if (raw == null) {
        continue;
      }
      String text = normalizeToken(String.valueOf(raw));
      if (text.isEmpty()) {
        continue;
      }
      var unit =
          unitRepository
              .findEnabledByCodeIgnoreCase(text)
              .or(() -> unitRepository.findEnabledByNameIgnoreCase(text));
      if (unit.isEmpty()) {
        return missing("单位", text);
      }
      extra.put(field, unit.get().getCode());
    }
    return null;
  }

  /** 卡车供应商：按全称/简称识别，并回写全称。 */
  public String resolveSupplierTruck(CostRoad entity) {
    if (entity == null || isBlank(entity.getSupplier())) {
      return null;
    }
    String raw = entity.getSupplier().trim();
    var found = supplierRepository.findByCategoryAndNameOrShortName("TRUCK", raw);
    if (found.isEmpty()) {
      return missing("SUPPLIER", raw);
    }
    entity.setSupplier(found.get().getName());
    return null;
  }

  /** 船公司：按全称/简称识别，并回写全称。 */
  public String resolveShippingLine(CostSea entity) {
    if (entity == null || isBlank(entity.getSsl())) {
      return null;
    }
    String raw = entity.getSsl().trim();
    var found = shippingLineRepository.findByNameOrShortName(raw);
    if (found.isEmpty()) {
      return missing("SSL", raw);
    }
    entity.setSsl(found.get().getName());
    return null;
  }

  /** 代理：按全称/简称识别，并回写全称。 */
  public String resolveAgent(CostSea entity) {
    if (entity == null || isBlank(entity.getAgent())) {
      return null;
    }
    String raw = entity.getAgent().trim();
    var found = agentRepository.findByNameOrShortName(raw);
    if (found.isEmpty()) {
      return missing("AGENT", raw);
    }
    entity.setAgent(found.get().getName());
    return null;
  }

  /** 多选值仅按 `/` 拆分（港口名如 NEW YORK, NY 含逗号，不能当分隔符）。 */
  private static List<String> splitSlashValues(String raw) {
    if (isBlank(raw)) {
      return List.of();
    }
    String[] parts = raw.split("/");
    List<String> values = new ArrayList<>(parts.length);
    for (String part : parts) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        values.add(trimmed);
      }
    }
    return values;
  }

  /** 港口匹配键：忽略逗号后空格差异（NEW YORK,NY ≡ NEW YORK, NY）。 */
  private static String canonicalPortName(String name) {
    return normalizeToken(name).replaceAll(",\\s*", ",");
  }

  /** 报错展示：统一为「逗号+空格」便于阅读。 */
  private static String displayPortName(String name) {
    String canonical = canonicalPortName(name);
    if (canonical.isEmpty()) {
      return name == null ? "" : name.trim();
    }
    return canonical.replace(",", ", ");
  }

  private static String missing(String label, String value) {
    return label
        + "「"
        + value.trim()
        + "」在系统中不存在，请先将数据维护进系统后再导入";
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * 清洗 Excel 主数据引用：去 BOM/不间断空格等不可见字符，压缩空白。
   * 州码再统一大写，避免 PA / pa / "PA " 误判不存在。
   */
  public static String normalizeToken(String value) {
    if (value == null) {
      return "";
    }
    String cleaned =
        value
            .replace('\u00A0', ' ')
            .replace('\u2007', ' ')
            .replace('\u202F', ' ')
            .replace("\uFEFF", "")
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replaceAll("\\s+", " ")
            .trim();
    return cleaned;
  }
}
