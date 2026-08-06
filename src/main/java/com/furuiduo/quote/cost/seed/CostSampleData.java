package com.furuiduo.quote.cost.seed;

import java.math.BigDecimal;
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
            "43215",
            "Columbus",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "TIIME DISPATCH",
            425, 35, 45, 200, 25, 85,
            1288.75, 888.75, 1378.75,
            90, 300, 150, 150, 75,
            "容易产生额外费用",
            "2026.06.01",
            "712 SR 1830 Brookeville, PA 15825"));
    rows.add(
        road(
            "43215",
            "Columbus",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "TIIME DISPATCH",
            410, 35, 45, 0, 25, 0,
            598.5, 598.5, 688.5,
            85, 280, 140, 140, 70,
            "市区限行需提前预约",
            "2026.06.01",
            null));
    rows.add(
        road(
            "44114",
            "Cleveland",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "MIDWEST DRAYAGE LLC",
            455, 35, 50, 180, 25, 75,
            1194.25, 819.25, 1274.25,
            95, 310, 155, 155, 80,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "48226",
            "Detroit",
            "MI",
            "NEW YORK",
            "NOR/NY",
            "GREAT LAKES TRUCKING",
            480, 36, 48, 210, 25, 90,
            1318.8, 928.8, 1408.8,
            100, 320, 160, 160, 85,
            "冬季附加费另计",
            "2026.06.01",
            null));
    rows.add(
        road(
            "45202",
            "Cincinnati",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "OHIO VALLEY LOGISTICS",
            440, 35, 45, 195, 25, 80,
            1299, 904, 1389,
            90, 295, 145, 145, 75,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "44114",
            "Cleveland",
            "OH",
            "BALTIMORE",
            "BAL",
            "TIIME DISPATCH",
            395, 34, 42, 175, 25, 70,
            816.3, 571.3, 0,
            88, 275, 135, 135, 70,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "15222",
            "Pittsburgh",
            "PA",
            "NEW YORK",
            "NOR/NY",
            "RUST BELT INTERMODAL",
            465, 35, 48, 205, 25, 85,
            1307.75, 917.75, 1397.75,
            92, 305, 150, 150, 78,
            "隧道限高 4.2m",
            "2026.06.01",
            null));
    rows.add(
        road(
            "43604",
            "Toledo",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "LAKE ERIE CARRIERS",
            418, 35, 44, 0, 25, 65,
            673.3, 608.3, 763.3,
            85, 270, 130, 130, 68,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "44114",
            "Cleveland",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "TIIME DISPATCH",
            430, 35, 45, 190, 25, 80,
            1290.5, 895.5, 1380.5,
            90, 290, 145, 145, 72,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "46204",
            "Indianapolis",
            "IN",
            "NEW YORK",
            "NOR/NY",
            "HEARTLAND DRAY INC",
            450, 35, 46, 200, 25, 82,
            1308.5, 913.5, 1398.5,
            90, 300, 148, 148, 76,
            "易产生额外费用",
            "2026.06.01",
            null));
    rows.add(
        road(
            "43215",
            "Columbus",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "BUCKEYE FREIGHT CO",
            422, 35, 45, 185, 25, 78,
            1274.7, 884.7, 1364.7,
            88, 285, 142, 142, 74,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "44114",
            "Cleveland",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "TIIME DISPATCH",
            408, 35, 43, 170, 25, 72,
            828.8, 586.8, 918.8,
            86, 268, 132, 132, 69,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "43604",
            "Toledo",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "NORTHERN OHIO TRUCKING",
            435, 35, 45, 192, 25, 83,
            1297.25, 902.25, 1387.25,
            90, 292, 146, 146, 73,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "60601",
            "Chicago",
            "IL",
            "NEW YORK",
            "NOR/NY",
            "MIDWEST DRAYAGE LLC",
            468, 36, 47, 205, 25, 88,
            1316.48, 926.48, 1406.48,
            95, 315, 158, 158, 82,
            "需提前 24h 预约",
            "2026.06.01",
            null));
    rows.add(
        road(
            "43215",
            "Columbus",
            "OH",
            "PHILADELPHIA",
            "PHI",
            "TIIME DISPATCH",
            388, 34, 42, 165, 25, 68,
            794.92, 561.92, 0,
            84, 265, 128, 128, 65,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "19107",
            "Philadelphia",
            "PA",
            "PHILADELPHIA",
            "PHI",
            "EASTERN GATE LOGISTICS",
            475, 35, 50, 215, 25, 92,
            1326.25, 936.25, 0,
            98, 325, 162, 162, 88,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "60601",
            "Chicago",
            "IL",
            "NEW YORK",
            "NOR/NY",
            "GREAT LAKES TRUCKING",
            492, 36, 49, 220, 25, 95,
            1339.12, 949.12, 1429.12,
            100, 330, 165, 165, 90,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "43604",
            "Toledo",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "TIIME DISPATCH",
            415, 35, 44, 178, 25, 74,
            1266.25, 876.25, 1356.25,
            87, 278, 138, 138, 71,
            "容易产生额外费用",
            "2026.06.01",
            null));
    rows.add(
        road(
            "43215",
            "Columbus",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "OHIO VALLEY LOGISTICS",
            428, 35, 45, 188, 25, 79,
            1286.8, 891.8, 1376.8,
            89, 288, 144, 144, 73,
            null,
            "2026.06.01",
            null));
    rows.add(
        road(
            "44114",
            "Cleveland",
            "OH",
            "NEW YORK",
            "NOR/NY",
            "RUST BELT INTERMODAL",
            442, 35, 46, 198, 25, 84,
            1298.7, 908.7, 1388.7,
            91, 298, 149, 149, 77,
            "边境查验可能延误",
            "2026.06.01",
            null));
    return rows;
  }

  public static List<CostSea> seaSamples() {
    String[][] rows = {
      {
        "NEW YORK",
        "NEW YORK",
        "SHANGHAI",
        "上海",
        "LOG/LUMBER/GENERAL",
        "40HQ/40GP/20GP/40RF/20RF",
        "2100",
        "2026/6/30",
        "180",
        "2026/6/30",
        "",
        "",
        "",
        "",
        "",
        "",
        "2280",
        "MSC",
        "",
        "美东直航"
      },
      {
        "NEW YORK",
        "NEW YORK",
        "NINGBO",
        "宁波",
        "LOG/LUMBER/GENERAL",
        "40HQ/40GP/20GP/40RF/20RF",
        "2050",
        "2026/6/30",
        "175",
        "2026/6/30",
        "",
        "",
        "",
        "",
        "",
        "",
        "2225",
        "MSC",
        "",
        "—"
      },
      {
        "LOUISVILLE",
        "LOUISVILLE",
        "SHANGHAI/NINGBO/YANTIAN",
        "上海/宁波/盐田",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "600",
        "2026/6/30",
        "100",
        "2026/4/13",
        "",
        "",
        "",
        "",
        "",
        "",
        "700",
        "YML",
        "",
        "含 THC，不含拖车费"
      },
      {
        "Chicago",
        "Chicago",
        "Los Angeles",
        "洛杉矶",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "585",
        "2026/7/15",
        "95",
        "2026/5/01",
        "",
        "",
        "",
        "",
        "",
        "",
        "680",
        "COSCO",
        "",
        "—"
      },
      {
        "Qingdao",
        "Qingdao",
        "Oakland",
        "奥克兰",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "1500",
        "2026/8/10",
        "120",
        "2026/6/10",
        "",
        "",
        "",
        "",
        "",
        "",
        "1620",
        "EMC",
        "",
        "—"
      },
      {
        "Ningbo",
        "Ningbo",
        "Long Beach",
        "长滩",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "1420",
        "2026/7/20",
        "115",
        "2026/5/20",
        "",
        "",
        "",
        "",
        "",
        "",
        "1535",
        "ONE",
        "",
        "旺季舱位紧张"
      },
      {
        "Shanghai",
        "Shanghai",
        "New York",
        "纽约",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "2100",
        "2026/6/30",
        "180",
        "2026/4/01",
        "",
        "",
        "",
        "",
        "",
        "",
        "2280",
        "MSC",
        "",
        "美东直航"
      },
      {
        "Xiamen",
        "Xiamen",
        "Savannah",
        "萨凡纳",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "1680",
        "2026/8/15",
        "140",
        "2026/5/15",
        "",
        "",
        "",
        "",
        "",
        "",
        "1820",
        "HMM",
        "",
        "—"
      },
      {
        "Yantian",
        "Yantian",
        "Houston",
        "休斯顿",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "1750",
        "2026/9/01",
        "150",
        "2026/6/01",
        "",
        "",
        "",
        "",
        "",
        "",
        "1900",
        "ZIM",
        "",
        "需确认 BUC"
      },
      {
        "Tianjin",
        "Tianjin",
        "Seattle",
        "西雅图",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "1380",
        "2026/6/20",
        "110",
        "2026/4/20",
        "",
        "",
        "",
        "",
        "",
        "",
        "1490",
        "OOCL",
        "",
        "—"
      },
      {
        "Busan",
        "Busan",
        "Los Angeles",
        "洛杉矶",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "980",
        "2026/7/10",
        "85",
        "2026/5/10",
        "",
        "",
        "",
        "",
        "",
        "",
        "1065",
        "HPL",
        "",
        "中转航线"
      },
      {
        "Kaohsiung",
        "Kaohsiung",
        "Oakland",
        "奥克兰",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "920",
        "2026/8/05",
        "80",
        "2026/6/05",
        "",
        "",
        "",
        "",
        "",
        "",
        "1000",
        "WHL",
        "",
        "—"
      },
      {
        "Hong Kong",
        "Hong Kong",
        "Vancouver",
        "温哥华",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "1850",
        "2026/6/25",
        "160",
        "2026/4/25",
        "",
        "",
        "",
        "",
        "",
        "",
        "2010",
        "CMA",
        "",
        "加拿大线"
      },
      {
        "Singapore",
        "Singapore",
        "Rotterdam",
        "鹿特丹",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "3200",
        "2026/7/01",
        "250",
        "2026/5/01",
        "",
        "",
        "",
        "",
        "",
        "",
        "3450",
        "MAERSK",
        "",
        "欧线参考"
      },
      {
        "Ho Chi Minh",
        "Ho Chi Minh",
        "Los Angeles",
        "洛杉矶",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "1150",
        "2026/8/12",
        "95",
        "2026/6/12",
        "",
        "",
        "",
        "",
        "",
        "",
        "1245",
        "YML",
        "",
        "—"
      },
      {
        "Jakarta",
        "Jakarta",
        "Long Beach",
        "长滩",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "1280",
        "2026/7/18",
        "105",
        "2026/5/18",
        "",
        "",
        "",
        "",
        "",
        "",
        "1385",
        "COSCO",
        "",
        "—"
      },
      {
        "Laem Chabang",
        "Laem Chabang",
        "New York",
        "纽约",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "2250",
        "2026/6/08",
        "190",
        "2026/4/08",
        "",
        "",
        "",
        "",
        "",
        "",
        "2440",
        "EMC",
        "",
        "—"
      },
      {
        "Colombo",
        "Colombo",
        "Savannah",
        "萨凡纳",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "1920",
        "2026/8/20",
        "165",
        "2026/6/20",
        "",
        "",
        "",
        "",
        "",
        "",
        "2085",
        "ONE",
        "",
        "—"
      },
      {
        "Mumbai",
        "Mumbai",
        "Houston",
        "休斯顿",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "2400",
        "2026/7/25",
        "200",
        "2026/5/25",
        "",
        "",
        "",
        "",
        "",
        "",
        "2600",
        "MSC",
        "",
        "印美线"
      },
      {
        "Tokyo",
        "Tokyo",
        "Los Angeles",
        "洛杉矶",
        "LOG/LUMBER/GENERAL",
        "40HQ",
        "890",
        "2026/8/15",
        "75",
        "2026/6/15",
        "",
        "",
        "",
        "",
        "",
        "",
        "965",
        "ZIM",
        "",
        "—"
      }
    };

    List<CostSea> samples = new ArrayList<>(SAMPLE_SIZE);
    for (String[] row : rows) {
      CostSea item = new CostSea();
      item.setPor(row[0]);
      item.setPol(row[1]);
      item.setPod(row[2]);
      item.setCnShortName(row[3]);
      item.setEnProductName(row[4]);
      item.setContainerType(row[5]);
      item.setFreight(new BigDecimal(row[6]));
      item.setFreightValidDate(row[7]);
      item.setBuc(blankToNullDecimal(row[8]));
      item.setBucValidDate(blankToNull(row[9]));
      item.setEbs(blankToNullDecimal(row[10]));
      item.setEbsValidDate(blankToNull(row[11]));
      item.setGri(blankToNullDecimal(row[12]));
      item.setGriValidDate(blankToNull(row[13]));
      item.setOthers(blankToNullDecimal(row[14]));
      item.setOthersValidDate(blankToNull(row[15]));
      item.setAllIn(new BigDecimal(row[16]));
      item.setSsl(row[17]);
      item.setAgent(blankToNull(row[18]));
      item.setRemark("—".equals(row[19]) ? null : blankToNull(row[19]));
      item.setStatus(CostStatus.active);
      item.touch();
      samples.add(item);
    }
    return samples;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static BigDecimal blankToNullDecimal(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new BigDecimal(value);
  }

  public static List<CostFumigation> fumigationSamples() {
    String[][] rows = {
      {
        "NEW YORK",
        "STAAR Trucking, LLC",
        "1005",
        "1525",
        "2026/1/1-2026/12/31",
        "1060",
        "1575",
        "2026/1/1-2026/12/31",
        "712 SR 1830 Brookeville, PA 15825"
      },
      {
        "CHICAGO",
        "EFM",
        "800",
        "1480",
        "2026/1/1-2026/12/31",
        "900",
        "1580",
        "2026/1/1-2026/12/31",
        "样例数据"
      },
      {
        "LOS ANGELES",
        "APM",
        "810",
        "1500",
        "2026/1/1-2026/12/31",
        "920",
        "1600",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "HOUSTON",
        "BAYPORT",
        "790",
        "1460",
        "2026/1/1-2026/12/31",
        "890",
        "1560",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "SAVANNAH",
        "GCT",
        "805",
        "1490",
        "2026/1/1-2026/12/31",
        "915",
        "1590",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "SEATTLE",
        "T18",
        "830",
        "1540",
        "2026/1/1-2026/12/31",
        "940",
        "1640",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "OAKLAND",
        "SSA",
        "815",
        "1510",
        "2026/1/1-2026/12/31",
        "925",
        "1610",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "CHARLESTON",
        "WWT",
        "795",
        "1470",
        "2026/1/1-2026/12/31",
        "895",
        "1570",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "MIAMI",
        "POMTOC",
        "840",
        "1550",
        "2026/1/1-2026/12/31",
        "950",
        "1650",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "BALTIMORE",
        "SEAGIRT",
        "808",
        "1500",
        "2026/1/1-2026/12/31",
        "918",
        "1600",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "NORFOLK",
        "NIT",
        "802",
        "1485",
        "2026/1/1-2026/12/31",
        "912",
        "1585",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "TACOMA",
        "HUSKY",
        "825",
        "1530",
        "2026/1/1-2026/12/31",
        "935",
        "1630",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "JACKSONVILLE",
        "JAXPORT",
        "798",
        "1475",
        "2026/1/1-2026/12/31",
        "898",
        "1575",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "MOBILE",
        "APM",
        "785",
        "1455",
        "2026/1/1-2026/12/31",
        "885",
        "1555",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "NEW ORLEANS",
        "NOLA",
        "792",
        "1465",
        "2026/1/1-2026/12/31",
        "892",
        "1565",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "PHILADELPHIA",
        "PAMT",
        "812",
        "1505",
        "2026/1/1-2026/12/31",
        "922",
        "1605",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "BOSTON",
        "CONLEY",
        "835",
        "1545",
        "2026/1/1-2026/12/31",
        "945",
        "1645",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "VANCOUVER",
        "DP WORLD",
        "850",
        "1560",
        "2026/1/1-2026/12/31",
        "960",
        "1660",
        "2026/1/1-2026/12/31",
        "加拿大口岸"
      },
      {
        "MONTREAL",
        "MPA",
        "845",
        "1555",
        "2026/1/1-2026/12/31",
        "955",
        "1655",
        "2026/1/1-2026/12/31",
        "—"
      },
      {
        "DETROIT",
        "BRIDGE",
        "788",
        "1450",
        "2026/1/1-2026/12/31",
        "888",
        "1550",
        "2026/1/1-2026/12/31",
        "内陆熏蒸点"
      }
    };

    List<CostFumigation> samples = new ArrayList<>(SAMPLE_SIZE);
    for (String[] row : rows) {
      CostFumigation item = new CostFumigation();
      item.setRegion(row[0]);
      item.setStation(row[1]);
      item.setOutdoorNonOak(new BigDecimal(row[2]));
      item.setOutdoorOak(new BigDecimal(row[3]));
      item.setOutdoorValidity(row[4]);
      item.setIndoorNonOak(new BigDecimal(row[5]));
      item.setIndoorOak(new BigDecimal(row[6]));
      item.setIndoorValidity(row[7]);
      item.setAddress("—".equals(row[8]) ? null : row[8]);
      item.touch();
      samples.add(item);
    }
    return samples;
  }

  @SuppressWarnings("unused")
  private static CostRoad road(
      String zipCode,
      String city,
      String state,
      String pod,
      String pol,
      String supplier,
      double baseFreight,
      double fsc,
      double chassis,
      double triTandemAxle,
      double split,
      double stopOff,
      double allInNoFm,
      double allInFmOneWay,
      double allInFmRound,
      double waitingFee,
      double redelivery,
      double prepull,
      double nsLift,
      double otherFee,
      String remark,
      String validDate,
      String logYardNameAddress) {
    CostRoad item = new CostRoad();
    item.setZipCode(zipCode);
    item.setCity(city);
    item.setState(state);
    item.setPor(city);
    item.setPol(pol);
    item.setSupplier(supplier);
    item.setBaseFreight(decimal(baseFreight));
    item.setFsc(decimal(fsc));
    item.setChassis(decimal(chassis));
    item.setTriTandemAxle(decimal(triTandemAxle));
    item.setSplit(decimal(split));
    item.setStopOff(decimal(stopOff));
    item.setAllInNoFm(decimal(allInNoFm));
    item.setAllInFmOneWay(decimal(allInFmOneWay));
    item.setAllInFmRound(allInFmRound > 0 ? decimal(allInFmRound) : null);
    item.setWaitingFee(decimal(waitingFee));
    item.setRedelivery(decimal(redelivery));
    item.setPrepull(decimal(prepull));
    item.setNsLift(decimal(nsLift));
    item.setOtherFee(decimal(otherFee));
    item.setRemark(remark);
    item.setValidDate(validDate);
    item.setLogYardNameAddress(logYardNameAddress);
    item.touch();
    return item;
  }

  private static BigDecimal decimal(double value) {
    return BigDecimal.valueOf(value);
  }
}
