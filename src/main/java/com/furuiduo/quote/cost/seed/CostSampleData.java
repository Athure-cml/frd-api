package com.furuiduo.quote.cost.seed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;

/** 三个成本库各 20 条测试样例，与前端业务 mock 对齐。 */
public final class CostSampleData {

  public static final int SAMPLE_SIZE = 20;

  private CostSampleData() {}

  public static List<CostRoad> roadSamples() {
    List<CostRoad> rows = new ArrayList<>(SAMPLE_SIZE);
    rows.add(
        road(
            "2026.06.01",
            "TIIME DISPATCH",
            "44624",
            "DUNDEE",
            "OH",
            "COLUMBUS",
            "NOR/NY",
            425,
            35,
            45,
            200,
            85,
            250,
            888.75,
            1288.75,
            1378.75,
            90,
            300,
            150,
            150,
            "容易产生额外费用"));
    rows.add(
        road(
            "2026.06.01",
            "TIIME DISPATCH",
            "43215",
            "COLUMBUS",
            "OH",
            "COLUMBUS",
            "NOR/NY",
            410,
            35,
            45,
            0,
            0,
            0,
            598.5,
            598.5,
            688.5,
            85,
            280,
            140,
            140,
            "市区限行需提前预约"));
    rows.add(
        road(
            "2026.06.01",
            "MIDWEST DRAYAGE LLC",
            "44114",
            "CLEVELAND",
            "OH",
            "CLEVELAND",
            "NOR/NY",
            455,
            35,
            50,
            180,
            75,
            0,
            819.25,
            1194.25,
            1274.25,
            95,
            310,
            155,
            155,
            null));
    rows.add(
        road(
            "2026.06.01",
            "GREAT LAKES TRUCKING",
            "48201",
            "DETROIT",
            "MI",
            "DETROIT",
            "NOR/NY",
            480,
            36,
            48,
            210,
            90,
            260,
            928.8,
            1318.8,
            1408.8,
            100,
            320,
            160,
            160,
            "冬季附加费另计"));
    rows.add(
        road(
            "2026.06.01",
            "OHIO VALLEY LOGISTICS",
            "45202",
            "CINCINNATI",
            "OH",
            "CINCINNATI",
            "NOR/NY",
            440,
            35,
            45,
            195,
            80,
            240,
            904,
            1299,
            1389,
            90,
            295,
            145,
            145,
            null));
    rows.add(
        road(
            "2026.06.01",
            "TIIME DISPATCH",
            "44308",
            "AKRON",
            "OH",
            "CLEVELAND",
            "BAL",
            395,
            34,
            42,
            175,
            70,
            0,
            571.3,
            816.3,
            0,
            88,
            275,
            135,
            135,
            null));
    rows.add(
        road(
            "2026.06.01",
            "RUST BELT INTERMODAL",
            "15222",
            "PITTSBURGH",
            "PA",
            "PITTSBURGH",
            "NOR/NY",
            465,
            35,
            48,
            205,
            85,
            255,
            917.75,
            1307.75,
            1397.75,
            92,
            305,
            150,
            150,
            "隧道限高 4.2m"));
    rows.add(
        road(
            "2026.06.01",
            "LAKE ERIE CARRIERS",
            "43604",
            "TOLEDO",
            "OH",
            "TOLEDO",
            "NOR/NY",
            418,
            35,
            44,
            0,
            65,
            0,
            608.3,
            673.3,
            763.3,
            85,
            270,
            130,
            130,
            null));
    rows.add(
        road(
            "2026.06.01",
            "TIIME DISPATCH",
            "44720",
            "CANTON",
            "OH",
            "CLEVELAND",
            "NOR/NY",
            430,
            35,
            45,
            190,
            80,
            245,
            895.5,
            1290.5,
            1380.5,
            90,
            290,
            145,
            145,
            null));
    rows.add(
        road(
            "2026.06.01",
            "HEARTLAND DRAY INC",
            "46204",
            "INDIANAPOLIS",
            "IN",
            "INDIANAPOLIS",
            "NOR/NY",
            450,
            35,
            46,
            200,
            82,
            250,
            913.5,
            1308.5,
            1398.5,
            90,
            300,
            148,
            148,
            "易产生额外费用"));
    rows.add(
        road(
            "2026.06.01",
            "BUCKEYE FREIGHT CO",
            "45402",
            "DAYTON",
            "OH",
            "COLUMBUS",
            "NOR/NY",
            422,
            35,
            45,
            185,
            78,
            235,
            884.7,
            1274.7,
            1364.7,
            88,
            285,
            142,
            142,
            null));
    rows.add(
        road(
            "2026.06.01",
            "TIIME DISPATCH",
            "44515",
            "YOUNGSTOWN",
            "OH",
            "CLEVELAND",
            "NOR/NY",
            408,
            35,
            43,
            170,
            72,
            0,
            586.8,
            828.8,
            918.8,
            86,
            268,
            132,
            132,
            null));
    rows.add(
        road(
            "2026.06.01",
            "NORTHERN OHIO TRUCKING",
            "44870",
            "SANDUSKY",
            "OH",
            "TOLEDO",
            "NOR/NY",
            435,
            35,
            45,
            192,
            83,
            248,
            902.25,
            1297.25,
            1387.25,
            90,
            292,
            146,
            146,
            null));
    rows.add(
        road(
            "2026.06.01",
            "MIDWEST DRAYAGE LLC",
            "46802",
            "FORT WAYNE",
            "IN",
            "CHICAGO",
            "NOR/NY",
            468,
            36,
            47,
            205,
            88,
            258,
            926.48,
            1316.48,
            1406.48,
            95,
            315,
            158,
            158,
            "需提前 24h 预约"));
    rows.add(
        road(
            "2026.06.01",
            "TIIME DISPATCH",
            "44906",
            "MANSFIELD",
            "OH",
            "COLUMBUS",
            "PHI",
            388,
            34,
            42,
            165,
            68,
            0,
            561.92,
            794.92,
            0,
            84,
            265,
            128,
            128,
            null));
    rows.add(
        road(
            "2026.06.01",
            "EASTERN GATE LOGISTICS",
            "19134",
            "PHILADELPHIA",
            "PA",
            "PHILADELPHIA",
            "PHI",
            475,
            35,
            50,
            215,
            92,
            265,
            936.25,
            1326.25,
            0,
            98,
            325,
            162,
            162,
            null));
    rows.add(
        road(
            "2026.06.01",
            "GREAT LAKES TRUCKING",
            "49503",
            "GRAND RAPIDS",
            "MI",
            "CHICAGO",
            "NOR/NY",
            492,
            36,
            49,
            220,
            95,
            270,
            949.12,
            1339.12,
            1429.12,
            100,
            330,
            165,
            165,
            null));
    rows.add(
        road(
            "2026.06.01",
            "TIIME DISPATCH",
            "45840",
            "FINDLAY",
            "OH",
            "TOLEDO",
            "NOR/NY",
            415,
            35,
            44,
            178,
            74,
            238,
            876.25,
            1266.25,
            1356.25,
            87,
            278,
            138,
            138,
            "容易产生额外费用"));
    rows.add(
        road(
            "2026.06.01",
            "OHIO VALLEY LOGISTICS",
            "45701",
            "ATHENS",
            "OH",
            "COLUMBUS",
            "NOR/NY",
            428,
            35,
            45,
            188,
            79,
            242,
            891.8,
            1286.8,
            1376.8,
            89,
            288,
            144,
            144,
            null));
    rows.add(
        road(
            "2026.06.01",
            "RUST BELT INTERMODAL",
            "16501",
            "ERIE",
            "PA",
            "CLEVELAND",
            "NOR/NY",
            442,
            35,
            46,
            198,
            84,
            252,
            908.7,
            1298.7,
            1388.7,
            91,
            298,
            149,
            149,
            "边境查验可能延误"));
    return rows;
  }

  public static List<CostSea> seaSamples() {
    String[][] rows = {
      {"LOUISVILLE", "SHANGHAI/NINGBO/YANTIAN", "YML", "600", "100", "700", "2026-04-13", "2026/6/30", "active", "含 THC，不含拖车费"},
      {"Chicago", "Los Angeles", "COSCO", "585", "95", "680", "2026-05-01", "2026/7/15", "active", "—"},
      {"Qingdao", "Oakland", "EMC", "1500", "120", "1620", "2026-06-10", "2026/8/10", "draft", "—"},
      {"Ningbo", "Long Beach", "ONE", "1420", "115", "1535", "2026-05-20", "2026/7/20", "active", "旺季舱位紧张"},
      {"Shanghai", "New York", "MSC", "2100", "180", "2280", "2026-04-01", "2026/6/30", "active", "美东直航"},
      {"Xiamen", "Savannah", "HMM", "1680", "140", "1820", "2026-05-15", "2026/8/15", "active", "—"},
      {"Yantian", "Houston", "ZIM", "1750", "150", "1900", "2026-06-01", "2026/9/01", "draft", "需确认 BUC"},
      {"Tianjin", "Seattle", "OOCL", "1380", "110", "1490", "2026-04-20", "2026/6/20", "active", "—"},
      {"Busan", "Los Angeles", "HPL", "980", "85", "1065", "2026-05-10", "2026/7/10", "active", "中转航线"},
      {"Kaohsiung", "Oakland", "WHL", "920", "80", "1000", "2026-06-05", "2026/8/05", "active", "—"},
      {"Hong Kong", "Vancouver", "CMA", "1850", "160", "2010", "2026-04-25", "2026/6/25", "active", "加拿大线"},
      {"Singapore", "Rotterdam", "MAERSK", "3200", "250", "3450", "2026-05-01", "2026/7/01", "active", "欧线参考"},
      {"Ho Chi Minh", "Los Angeles", "YML", "1150", "95", "1245", "2026-06-12", "2026/8/12", "draft", "—"},
      {"Jakarta", "Long Beach", "COSCO", "1280", "105", "1385", "2026-05-18", "2026/7/18", "active", "—"},
      {"Laem Chabang", "New York", "EMC", "2250", "190", "2440", "2026-04-08", "2026/6/08", "active", "—"},
      {"Colombo", "Savannah", "ONE", "1920", "165", "2085", "2026-06-20", "2026/8/20", "active", "—"},
      {"Mumbai", "Houston", "MSC", "2400", "200", "2600", "2026-05-25", "2026/7/25", "draft", "印美线"},
      {"Port Klang", "Seattle", "HMM", "1050", "90", "1140", "2026-04-30", "2026/6/30", "active", "—"},
      {"Tokyo", "Los Angeles", "ZIM", "890", "75", "965", "2026-06-15", "2026/8/15", "active", "—"},
      {"Kaohsiung", "New York", "OOCL", "2180", "185", "2365", "2026-05-05", "2026/7/05", "active", "美东中转"}
    };

    List<CostSea> samples = new ArrayList<>(SAMPLE_SIZE);
    for (String[] row : rows) {
      CostSea item = new CostSea();
      item.setOrigin(row[0]);
      item.setDestination(row[1]);
      item.setCarrier(row[2]);
      item.setSpec("");
      item.setUnit("箱");
      item.setUnitPrice(new BigDecimal(row[3]));
      item.setBuc(new BigDecimal(row[4]));
      item.setAllIn(new BigDecimal(row[5]));
      item.setSurchargeValidDate(LocalDate.parse(row[6]));
      item.setValidDate(row[7]);
      item.setCurrency("USD");
      item.setStatus(CostStatus.valueOf(row[8]));
      item.setRemark("—".equals(row[9]) ? null : row[9]);
      item.touch();
      samples.add(item);
    }
    return samples;
  }

  public static List<CostFumigation> fumigationSamples() {
    String[][] rows = {
      {"CHICAGO", "EFM", "800", "900", "850", "850+100", "1480", "1580", "1550", "1680+100", "样例数据"},
      {"LOS ANGELES", "APM", "810", "920", "860", "860+100", "1500", "1600", "1580", "1700+100", "—"},
      {"NEW YORK", "GCT", "820", "930", "870", "870+100", "1520", "1620", "1600", "1720+100", "—"},
      {"HOUSTON", "BAYPORT", "790", "890", "840", "840+100", "1460", "1560", "1530", "1660+100", "夏季报价参考"},
      {"SAVANNAH", "GCT", "805", "915", "855", "855+100", "1490", "1590", "1560", "1690+100", "—"},
      {"SEATTLE", "T18", "830", "940", "880", "880+100", "1540", "1640", "1610", "1740+100", "—"},
      {"OAKLAND", "SSA", "815", "925", "865", "865+100", "1510", "1610", "1580", "1710+100", "—"},
      {"CHARLESTON", "WWT", "795", "895", "845", "845+100", "1470", "1570", "1540", "1670+100", "—"},
      {"MIAMI", "POMTOC", "840", "950", "890", "890+100", "1550", "1650", "1620", "1750+100", "—"},
      {"BALTIMORE", "SEAGIRT", "808", "918", "858", "858+100", "1500", "1600", "1570", "1700+100", "—"},
      {"NORFOLK", "NIT", "802", "912", "852", "852+100", "1485", "1585", "1555", "1685+100", "—"},
      {"TACOMA", "HUSKY", "825", "935", "875", "875+100", "1530", "1630", "1600", "1730+100", "—"},
      {"JACKSONVILLE", "JAXPORT", "798", "898", "848", "848+100", "1475", "1575", "1545", "1675+100", "—"},
      {"MOBILE", "APM", "785", "885", "835", "835+100", "1455", "1555", "1525", "1655+100", "—"},
      {"NEW ORLEANS", "NOLA", "792", "892", "842", "842+100", "1465", "1565", "1535", "1665+100", "—"},
      {"PHILADELPHIA", "PAMT", "812", "922", "862", "862+100", "1505", "1605", "1575", "1705+100", "—"},
      {"BOSTON", "CONLEY", "835", "945", "885", "885+100", "1545", "1645", "1615", "1745+100", "—"},
      {"VANCOUVER", "DP WORLD", "850", "960", "900", "900+100", "1560", "1660", "1630", "1760+100", "加拿大口岸"},
      {"MONTREAL", "MPA", "845", "955", "895", "895+100", "1555", "1655", "1625", "1755+100", "—"},
      {"DETROIT", "BRIDGE", "788", "888", "838", "838+100", "1450", "1550", "1520", "1650+100", "内陆熏蒸点"}
    };

    List<CostFumigation> samples = new ArrayList<>(SAMPLE_SIZE);
    for (String[] row : rows) {
      CostFumigation item = new CostFumigation();
      item.setPort(row[0]);
      item.setStation(row[1]);
      item.setNonOakOutdoor(new BigDecimal(row[2]));
      item.setNonOakIndoor(new BigDecimal(row[3]));
      item.setNonOakQuoteSummer(row[4]);
      item.setNonOakQuoteWinter(row[5]);
      item.setOakOutdoor(new BigDecimal(row[6]));
      item.setOakIndoor(new BigDecimal(row[7]));
      item.setOakQuoteSummer(row[8]);
      item.setOakQuoteWinter(row[9]);
      item.setRemark("—".equals(row[10]) ? null : row[10]);
      item.touch();
      samples.add(item);
    }
    return samples;
  }

  private static CostRoad road(
      String validDate,
      String supplier,
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      double baseFreight,
      double fscPercent,
      double chassis,
      double owTriAxle,
      double split,
      double stopOff,
      double allIn,
      double allInNonOak,
      double allInOak,
      double waitingFee,
      double redelivery,
      double prepull,
      double nsLift,
      String remark) {
    CostRoad item = new CostRoad();
    item.setValidDate(validDate);
    item.setSupplier(supplier);
    item.setLogYardNameAddress("");
    item.setZipCode(zipCode);
    item.setCity(city);
    item.setState(state);
    item.setPor(por);
    item.setPol(pol);
    item.setBaseFreight(decimal(baseFreight));
    item.setFsc(decimal(fscPercent).movePointLeft(2));
    item.setChassis(decimal(chassis));
    item.setOwTriAxle(decimal(owTriAxle));
    item.setSplit(decimal(split));
    item.setStopOff(decimal(stopOff));
    item.setAllIn(decimal(allIn));
    item.setAllInNonOak(decimal(allInNonOak));
    item.setAllInOak(decimal(allInOak));
    item.setWaitingFee(decimal(waitingFee));
    item.setRedelivery(decimal(redelivery));
    item.setPrepull(decimal(prepull));
    item.setNsLift(decimal(nsLift));
    item.setRemark(remark);
    item.touch();
    return item;
  }

  private static BigDecimal decimal(double value) {
    return BigDecimal.valueOf(value);
  }
}
