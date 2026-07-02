package com.furuiduo.quote.masterdata.seed;

import java.util.LinkedHashMap;
import java.util.Map;

/** 将 ISO 国家码映射为业务航线（Route）与国家/地区显示名。 */
public final class GlobalPortRouteResolver {

  private static final Map<String, String> COUNTRY_NAMES = buildCountryNames();
  private static final Map<String, String> COUNTRY_ROUTES = buildCountryRoutes();

  private GlobalPortRouteResolver() {}

  public static String resolveCountryRegion(String countryCode) {
    if (countryCode == null || countryCode.isBlank()) {
      return null;
    }
    return COUNTRY_NAMES.getOrDefault(countryCode.toUpperCase(), countryCode.toUpperCase());
  }

  public static String resolveRoute(String countryCode) {
    if (countryCode == null || countryCode.isBlank()) {
      return null;
    }
    return COUNTRY_ROUTES.getOrDefault(countryCode.toUpperCase(), "其他");
  }

  private static Map<String, String> buildCountryNames() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("CN", "China");
    map.put("HK", "Hong Kong");
    map.put("TW", "Taiwan");
    map.put("MO", "Macau");
    map.put("JP", "Japan");
    map.put("KR", "Korea");
    map.put("SG", "Singapore");
    map.put("MY", "Malaysia");
    map.put("TH", "Thailand");
    map.put("VN", "Vietnam");
    map.put("ID", "Indonesia");
    map.put("PH", "Philippines");
    map.put("MM", "Myanmar");
    map.put("KH", "Cambodia");
    map.put("LA", "Laos");
    map.put("BN", "Brunei");
    map.put("IN", "India");
    map.put("PK", "Pakistan");
    map.put("BD", "Bangladesh");
    map.put("LK", "Sri Lanka");
    map.put("AE", "UAE");
    map.put("SA", "Saudi Arabia");
    map.put("OM", "Oman");
    map.put("QA", "Qatar");
    map.put("KW", "Kuwait");
    map.put("BH", "Bahrain");
    map.put("IQ", "Iraq");
    map.put("IL", "Israel");
    map.put("JO", "Jordan");
    map.put("EG", "Egypt");
    map.put("ZA", "South Africa");
    map.put("NG", "Nigeria");
    map.put("KE", "Kenya");
    map.put("MA", "Morocco");
    map.put("TZ", "Tanzania");
    map.put("US", "United States");
    map.put("CA", "Canada");
    map.put("MX", "Mexico");
    map.put("PA", "Panama");
    map.put("BR", "Brazil");
    map.put("AR", "Argentina");
    map.put("CL", "Chile");
    map.put("CO", "Colombia");
    map.put("PE", "Peru");
    map.put("EC", "Ecuador");
    map.put("UY", "Uruguay");
    map.put("VE", "Venezuela");
    map.put("NL", "Netherlands");
    map.put("BE", "Belgium");
    map.put("DE", "Germany");
    map.put("FR", "France");
    map.put("GB", "United Kingdom");
    map.put("IE", "Ireland");
    map.put("ES", "Spain");
    map.put("PT", "Portugal");
    map.put("IT", "Italy");
    map.put("GR", "Greece");
    map.put("TR", "Turkey");
    map.put("PL", "Poland");
    map.put("RU", "Russia");
    map.put("FI", "Finland");
    map.put("SE", "Sweden");
    map.put("NO", "Norway");
    map.put("DK", "Denmark");
    map.put("LT", "Lithuania");
    map.put("LV", "Latvia");
    map.put("EE", "Estonia");
    map.put("AU", "Australia");
    map.put("NZ", "New Zealand");
    map.put("FJ", "Fiji");
    map.put("PG", "Papua New Guinea");
    return map;
  }

  private static Map<String, String> buildCountryRoutes() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("CN", "China");
    map.put("HK", "China");
    map.put("TW", "China");
    map.put("MO", "China");
    map.put("JP", "Far East");
    map.put("KR", "Far East");
    map.put("SG", "Southeast Asia");
    map.put("MY", "Southeast Asia");
    map.put("TH", "Southeast Asia");
    map.put("VN", "Southeast Asia");
    map.put("ID", "Southeast Asia");
    map.put("PH", "Southeast Asia");
    map.put("MM", "Southeast Asia");
    map.put("KH", "Southeast Asia");
    map.put("LA", "Southeast Asia");
    map.put("BN", "Southeast Asia");
    map.put("IN", "South Asia");
    map.put("PK", "South Asia");
    map.put("BD", "South Asia");
    map.put("LK", "South Asia");
    map.put("AE", "Middle East");
    map.put("SA", "Middle East");
    map.put("OM", "Middle East");
    map.put("QA", "Middle East");
    map.put("KW", "Middle East");
    map.put("BH", "Middle East");
    map.put("IQ", "Middle East");
    map.put("IL", "Middle East");
    map.put("JO", "Middle East");
    map.put("EG", "Africa");
    map.put("ZA", "Africa");
    map.put("NG", "Africa");
    map.put("KE", "Africa");
    map.put("MA", "Africa");
    map.put("TZ", "Africa");
    map.put("US", "North America");
    map.put("CA", "North America");
    map.put("MX", "Latin America");
    map.put("PA", "Latin America");
    map.put("BR", "Latin America");
    map.put("AR", "Latin America");
    map.put("CL", "Latin America");
    map.put("CO", "Latin America");
    map.put("PE", "Latin America");
    map.put("EC", "Latin America");
    map.put("UY", "Latin America");
    map.put("VE", "Latin America");
    map.put("NL", "Europe");
    map.put("BE", "Europe");
    map.put("DE", "Europe");
    map.put("FR", "Europe");
    map.put("GB", "Europe");
    map.put("IE", "Europe");
    map.put("ES", "Europe");
    map.put("PT", "Europe");
    map.put("IT", "Europe");
    map.put("GR", "Mediterranean");
    map.put("TR", "Mediterranean");
    map.put("PL", "Europe");
    map.put("RU", "Europe");
    map.put("FI", "Europe");
    map.put("SE", "Europe");
    map.put("NO", "Europe");
    map.put("DK", "Europe");
    map.put("LT", "Europe");
    map.put("LV", "Europe");
    map.put("EE", "Europe");
    map.put("AU", "Oceania");
    map.put("NZ", "Oceania");
    map.put("FJ", "Oceania");
    map.put("PG", "Oceania");
    return map;
  }
}
