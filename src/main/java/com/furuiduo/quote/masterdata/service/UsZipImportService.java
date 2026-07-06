package com.furuiduo.quote.masterdata.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.cost.dto.CostImportResult;
import com.furuiduo.quote.masterdata.entity.MdDestCity;
import com.furuiduo.quote.masterdata.entity.MdDestZip;
import com.furuiduo.quote.masterdata.entity.MdUsState;
import com.furuiduo.quote.masterdata.repository.MdDestCityRepository;
import com.furuiduo.quote.masterdata.repository.MdDestZipRepository;
import com.furuiduo.quote.masterdata.repository.MdUsStateRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** 从 GeoNames {@code US.txt}（tab 分隔）导入美国州/城市/邮编主数据。 */
@Service
public class UsZipImportService {

  private static final Logger log = LoggerFactory.getLogger(UsZipImportService.class);
  private static final int BATCH_SIZE = 500;
  private static final Map<String, String> STATE_NAME_ZH = buildStateNameZh();

  private final MdUsStateRepository stateRepository;
  private final MdDestCityRepository cityRepository;
  private final MdDestZipRepository zipRepository;

  @PersistenceContext
  private EntityManager entityManager;

  public UsZipImportService(
      MdUsStateRepository stateRepository,
      MdDestCityRepository cityRepository,
      MdDestZipRepository zipRepository) {
    this.stateRepository = stateRepository;
    this.cityRepository = cityRepository;
    this.zipRepository = zipRepository;
  }

  public boolean hasZipData() {
    return zipRepository.count() > 0;
  }

  @Transactional
  public CostImportResult importFromPath(Path path) throws IOException {
    if (!Files.isRegularFile(path)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件不存在：" + path);
    }
    log.info("开始导入美国邮编数据：{}", path);
    try (InputStream inputStream = Files.newInputStream(path)) {
      return importFromStream(inputStream);
    }
  }

  @Transactional
  public CostImportResult importGeonamesFile(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择 US.txt 文件");
    }
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase().endsWith(".txt")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 GeoNames US.txt 文本文件");
    }
    log.info("开始导入美国邮编数据：{}", filename);
    try (InputStream inputStream = file.getInputStream()) {
      return importFromStream(inputStream);
    }
  }

  @Transactional
  public CostImportResult importFromStream(InputStream inputStream) throws IOException {
    ParsedData parsed = parseGeonamesFile(inputStream);
    Map<String, Long> stateIdByCode = upsertStates(parsed.stateNames());
    Map<String, Long> cityIdByKey = insertCities(parsed.cityNameByKey(), stateIdByCode);
    int zipCount = insertZips(parsed.zipRows(), cityIdByKey);

    log.info(
        "美国邮编数据导入完成：州 {} 个，城市 {} 个，邮编 {} 条",
        stateIdByCode.size(),
        cityIdByKey.size(),
        zipCount);

    return new CostImportResult(
        zipCount,
        0,
        List.of(
            "州 "
                + stateIdByCode.size()
                + " 个，城市 "
                + cityIdByKey.size()
                + " 个，邮编 "
                + zipCount
                + " 条"));
  }

  private ParsedData parseGeonamesFile(InputStream inputStream) throws IOException {
    Map<String, String> stateNames = new LinkedHashMap<>();
    Map<String, String> cityNameByKey = new LinkedHashMap<>();
    List<ZipRow> zipRows = new ArrayList<>();
    Set<String> zipKeys = new HashSet<>();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        String[] parts = line.split("\t", -1);
        if (parts.length < 5) {
          continue;
        }
        if (!"US".equalsIgnoreCase(parts[0].trim())) {
          continue;
        }

        String zipCode = parts[1].trim();
        String cityName = parts[2].trim();
        String stateName = parts[3].trim();
        String stateCode = parts[4].trim().toUpperCase();
        if (zipCode.isEmpty() || cityName.isEmpty() || stateCode.isEmpty()) {
          continue;
        }

        stateNames.putIfAbsent(stateCode, stateName);
        String cityKey = stateCode + "\0" + cityName.toLowerCase();
        cityNameByKey.putIfAbsent(cityKey, cityName);

        String zipKey = cityKey + "\0" + zipCode.toLowerCase();
        if (zipKeys.add(zipKey)) {
          zipRows.add(new ZipRow(stateCode, cityName, zipCode));
        }
      }
    }

    if (zipRows.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "未解析到有效邮编数据，请确认文件为 GeoNames US.txt");
    }

    return new ParsedData(stateNames, cityNameByKey, zipRows);
  }

  private Map<String, Long> upsertStates(Map<String, String> stateNames) {
    Map<String, Long> stateIdByCode = new HashMap<>();
    for (MdUsState existing : stateRepository.findAll()) {
      stateIdByCode.put(existing.getCode().toUpperCase(), existing.getId());
    }

    List<MdUsState> toSave = new ArrayList<>();
    for (Map.Entry<String, String> entry : stateNames.entrySet()) {
      String code = entry.getKey();
      if (stateIdByCode.containsKey(code)) {
        continue;
      }
      MdUsState state = new MdUsState();
      state.setCode(code);
      state.setNameZh(STATE_NAME_ZH.getOrDefault(code, entry.getValue()));
      toSave.add(state);
    }

    if (!toSave.isEmpty()) {
      for (MdUsState saved : stateRepository.saveAll(toSave)) {
        stateIdByCode.put(saved.getCode().toUpperCase(), saved.getId());
      }
    }
    return stateIdByCode;
  }

  private Map<String, Long> insertCities(
      Map<String, String> cityNameByKey, Map<String, Long> stateIdByCode) {
    Map<String, Long> cityIdByKey = new HashMap<>();
    for (Object[] row : cityRepository.findExistingCityKeys()) {
      String stateCode = String.valueOf(row[0]).toUpperCase();
      String cityName = String.valueOf(row[1]);
      cityIdByKey.put(stateCode + "\0" + cityName, ((Number) row[2]).longValue());
    }

    List<MdDestCity> batch = new ArrayList<>();
    List<String> batchKeys = new ArrayList<>();
    for (Map.Entry<String, String> entry : cityNameByKey.entrySet()) {
      String cityKey = entry.getKey();
      if (cityIdByKey.containsKey(cityKey)) {
        continue;
      }
      String stateCode = cityKey.substring(0, cityKey.indexOf('\0'));
      Long stateId = stateIdByCode.get(stateCode);
      if (stateId == null) {
        continue;
      }

      MdDestCity city = new MdDestCity();
      city.setStateId(stateId);
      city.setName(entry.getValue());
      batch.add(city);
      batchKeys.add(cityKey);

      if (batch.size() >= BATCH_SIZE) {
        flushCities(batch, batchKeys, cityIdByKey);
      }
    }
    flushCities(batch, batchKeys, cityIdByKey);
    return cityIdByKey;
  }

  private void flushCities(
      List<MdDestCity> batch, List<String> batchKeys, Map<String, Long> cityIdByKey) {
    if (batch.isEmpty()) {
      return;
    }
    List<MdDestCity> saved = cityRepository.saveAll(batch);
    for (int i = 0; i < saved.size(); i++) {
      cityIdByKey.put(batchKeys.get(i), saved.get(i).getId());
    }
    entityManager.flush();
    entityManager.clear();
    batch.clear();
    batchKeys.clear();
  }

  private int insertZips(List<ZipRow> zipRows, Map<String, Long> cityIdByKey) {
    Set<String> existingZipKeys = new HashSet<>();
    for (Object[] row : zipRepository.findExistingZipKeys()) {
      existingZipKeys.add(row[0] + "\0" + row[1]);
    }

    int inserted = 0;
    List<MdDestZip> batch = new ArrayList<>();

    for (ZipRow row : zipRows) {
      String cityKey = row.stateCode() + "\0" + row.cityName().toLowerCase();
      Long cityId = cityIdByKey.get(cityKey);
      if (cityId == null) {
        continue;
      }

      String zipKey = cityId + "\0" + row.zipCode().toLowerCase();
      if (existingZipKeys.contains(zipKey)) {
        continue;
      }
      existingZipKeys.add(zipKey);

      MdDestZip zip = new MdDestZip();
      zip.setCityId(cityId);
      zip.setZipCode(row.zipCode());
      batch.add(zip);
      inserted++;

      if (batch.size() >= BATCH_SIZE) {
        zipRepository.saveAll(batch);
        entityManager.flush();
        entityManager.clear();
        batch.clear();
      }
    }

    if (!batch.isEmpty()) {
      zipRepository.saveAll(batch);
      entityManager.flush();
      entityManager.clear();
    }
    return inserted;
  }

  private static Map<String, String> buildStateNameZh() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("AL", "阿拉巴马州");
    map.put("AK", "阿拉斯加州");
    map.put("AZ", "亚利桑那州");
    map.put("AR", "阿肯色州");
    map.put("CA", "加利福尼亚州");
    map.put("CO", "科罗拉多州");
    map.put("CT", "康涅狄格州");
    map.put("DE", "特拉华州");
    map.put("DC", "华盛顿特区");
    map.put("FL", "佛罗里达州");
    map.put("GA", "佐治亚州");
    map.put("HI", "夏威夷州");
    map.put("ID", "爱达荷州");
    map.put("IL", "伊利诺伊州");
    map.put("IN", "印第安纳州");
    map.put("IA", "艾奥瓦州");
    map.put("KS", "堪萨斯州");
    map.put("KY", "肯塔基州");
    map.put("LA", "路易斯安那州");
    map.put("ME", "缅因州");
    map.put("MD", "马里兰州");
    map.put("MA", "马萨诸塞州");
    map.put("MI", "密歇根州");
    map.put("MN", "明尼苏达州");
    map.put("MS", "密西西比州");
    map.put("MO", "密苏里州");
    map.put("MT", "蒙大拿州");
    map.put("NE", "内布拉斯加州");
    map.put("NV", "内华达州");
    map.put("NH", "新罕布什尔州");
    map.put("NJ", "新泽西州");
    map.put("NM", "新墨西哥州");
    map.put("NY", "纽约州");
    map.put("NC", "北卡罗来纳州");
    map.put("ND", "北达科他州");
    map.put("OH", "俄亥俄州");
    map.put("OK", "俄克拉荷马州");
    map.put("OR", "俄勒冈州");
    map.put("PA", "宾夕法尼亚州");
    map.put("RI", "罗得岛州");
    map.put("SC", "南卡罗来纳州");
    map.put("SD", "南达科他州");
    map.put("TN", "田纳西州");
    map.put("TX", "德克萨斯州");
    map.put("UT", "犹他州");
    map.put("VT", "佛蒙特州");
    map.put("VA", "弗吉尼亚州");
    map.put("WA", "华盛顿州");
    map.put("WV", "西弗吉尼亚州");
    map.put("WI", "威斯康星州");
    map.put("WY", "怀俄明州");
    map.put("PR", "波多黎各");
    map.put("VI", "美属维尔京群岛");
    map.put("GU", "关岛");
    map.put("AS", "美属萨摩亚");
    map.put("MP", "北马里亚纳群岛");
    map.put("FM", "密克罗尼西亚");
    map.put("MH", "马绍尔群岛");
    map.put("PW", "帕劳");
    return map;
  }

  private record ParsedData(
      Map<String, String> stateNames,
      Map<String, String> cityNameByKey,
      List<ZipRow> zipRows) {}

  private record ZipRow(String stateCode, String cityName, String zipCode) {}
}
