package com.furuiduo.quote.mail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;

import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;

/** 构建与前端 QuotePrintSheet「报价单样式预览」一致的打印 HTML。 */
final class QuotePrintHtmlBuilder {

  private static final int MAX_OCEAN_LINES = 3;
  private static final int MAX_ROAD_MATCHES = 2;

  private QuotePrintHtmlBuilder() {}

  static String build(
      String customerName,
      LocalDate quoteDate,
      QuoteSheetFieldsDto sheet,
      List<String> serviceTypes,
      List<QuoteCostMatchItemDto> costSnapshots,
      Map<String, String> sslRemarkByName) {
    String logoDataUri = logoDataUri();
    String dateText =
        quoteDate == null
            ? "—"
            : quoteDate.format(DateTimeFormatter.ofPattern("yyyy/M/d"));
    StringBuilder fees = new StringBuilder();
    for (FeeRow row : buildFeeRows(sheet, serviceTypes, costSnapshots, sslRemarkByName)) {
      if (row.group) {
        fees.append("<tr class=\"quote-print-sheet__group-row\"><td colspan=\"5\">")
            .append(esc(row.item))
            .append("</td></tr>");
        continue;
      }
      fees.append("<tr class=\"quote-print-sheet__fee-row\">");
      fees.append("<td class=\"col-item\">").append(esc(row.item)).append("</td>");
      if (row.mergeRateMeta) {
        fees.append("<td class=\"col-rate-merged\" colspan=\"4\">")
            .append(esc(blankDash(row.rate)))
            .append("</td>");
      } else {
        fees.append("<td class=\"col-rate\">").append(esc(blankDash(row.rate))).append("</td>");
        if (row.mergeUnitMeta) {
          fees.append("<td class=\"col-unit-merged\" colspan=\"3\">")
              .append(esc(blankDash(row.unit)))
              .append("</td>");
        } else {
          fees.append("<td class=\"col-unit\">").append(esc(blankDash(row.unit))).append("</td>");
          fees.append("<td class=\"col-ssl\">").append(esc(blankDash(row.ssl))).append("</td>");
          fees.append("<td class=\"col-remark\">")
              .append(esc(blankDash(row.remark)))
              .append("</td>");
        }
      }
      fees.append("</tr>");
    }

    // CSS/HTML 与前端 quote-print-sheet.css + quote.css 打印规则保持一致
    return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
        <meta charset="UTF-8"/>
        <style>
        @page { size: a4; margin: 8mm; }
        * { box-sizing: border-box; }
        html, body {
          margin: 0; padding: 0; background: #fff;
          -webkit-print-color-adjust: exact; print-color-adjust: exact;
        }
        .quote-print-sheet {
          box-sizing: border-box;
          display: flex;
          flex-direction: column;
          width: 100%;
          max-width: none;
          min-height: 281mm;
          margin: 0;
          font-family: Calibri, "Segoe UI", Arial, sans-serif;
          font-size: 13px;
          line-height: 1.35;
          color: #000;
          -webkit-print-color-adjust: exact;
          print-color-adjust: exact;
          background: #fff;
          border: 1px solid #b4c6e7;
        }
        .quote-print-sheet__header {
          display: grid;
          grid-template-columns: 1fr auto;
          gap: 12px;
          align-items: center;
          padding: 16px 18px 12px;
          border-bottom: 2px solid #2f5597;
        }
        .quote-print-sheet__brand {
          display: flex;
          gap: 10px;
          align-items: center;
        }
        .quote-print-sheet__logo {
          display: block;
          width: auto;
          max-width: 220px;
          height: 52px;
          object-fit: contain;
          object-position: left center;
        }
        .quote-print-sheet__title {
          margin: 0;
          font-size: 38px;
          font-weight: 700;
          line-height: 1.1;
          color: #1f4e79;
          letter-spacing: 0.16em;
        }
        .quote-print-sheet__info,
        .quote-print-sheet__fees {
          width: 100%;
          border-collapse: collapse;
        }
        .quote-print-sheet__info {
          table-layout: fixed;
        }
        .quote-print-sheet__info-col { width: 16.666%; }
        .quote-print-sheet__info th,
        .quote-print-sheet__info td {
          padding: 6px 10px;
          font-weight: 700;
          vertical-align: middle;
          border: 1px solid #b4c6e7;
        }
        .quote-print-sheet__info th {
          color: #1f4e79;
          text-align: left;
          white-space: nowrap;
          background: #d9e2f3;
        }
        .quote-print-sheet__info th.quote-print-sheet__info-label--wrap {
          line-height: 1.25;
          vertical-align: middle;
          white-space: normal;
        }
        .quote-print-sheet__info-label-line { display: block; }
        .quote-print-sheet__info td {
          min-height: 28px;
          font-weight: 700;
          color: #111;
          background: #fff;
        }
        .quote-print-sheet__fees thead th {
          padding: 8px 10px;
          font-weight: 700;
          color: #1f4e79;
          text-align: center;
          background: #d9e2f3;
          border: 1px solid #b4c6e7;
        }
        .quote-print-sheet__fees .col-item { width: 34%; }
        .quote-print-sheet__fees .col-rate { width: 16%; }
        .quote-print-sheet__fees .col-unit { width: 22%; }
        .quote-print-sheet__fees .col-ssl { width: 12%; }
        .quote-print-sheet__fees .col-remark { width: 16%; }
        .quote-print-sheet__group-row td {
          padding: 6px 10px;
          font-weight: 700;
          color: #1f4e79;
          text-align: center;
          background: #fff2cc;
          border: 1px solid #b4c6e7;
        }
        .quote-print-sheet__fee-row td {
          padding: 5px 8px;
          vertical-align: middle;
          border: 1px solid #b4c6e7;
        }
        .quote-print-sheet__fee-row .col-item { background: #fff; }
        .quote-print-sheet__fee-row .col-rate {
          font-variant-numeric: tabular-nums;
          text-align: right;
          background: #ddebf7;
        }
        .quote-print-sheet__fee-row .col-rate-merged {
          font-size: 12px;
          text-align: left;
          background: #fff;
        }
        .quote-print-sheet__fee-row .col-unit { font-size: 12px; background: #fff; }
        .quote-print-sheet__fee-row .col-ssl,
        .quote-print-sheet__fee-row .col-remark { font-size: 12px; background: #fff; }
        .quote-print-sheet__fee-row .col-unit-merged { font-size: 12px; background: #fff; }
        .quote-print-sheet__footer {
          display: flex;
          flex: 1 1 auto;
          flex-direction: column;
          min-height: 0;
          padding: 12px 16px 18px;
          border-top: 2px solid #2f5597;
        }
        .quote-print-sheet__footer-label {
          margin-bottom: 6px;
          font-weight: 700;
          color: #1f4e79;
        }
        .quote-print-sheet__remark {
          flex: 1 1 auto;
          min-height: 48px;
          padding: 8px 10px;
          white-space: pre-wrap;
          background: #fafafa;
          border: 1px solid #b4c6e7;
        }
        </style>
        </head>
        <body>
        <div class="quote-print-sheet quote-print-sheet--printing">
          <header class="quote-print-sheet__header">
            <div class="quote-print-sheet__brand">
              <img class="quote-print-sheet__logo" alt="FRD GLOBAL (SH) CO., LTD." src="{{LOGO}}"/>
            </div>
            <h1 class="quote-print-sheet__title">报价单</h1>
          </header>
          <table class="quote-print-sheet__info">
            <colgroup>
              <col class="quote-print-sheet__info-col"/>
              <col class="quote-print-sheet__info-col"/>
              <col class="quote-print-sheet__info-col"/>
              <col class="quote-print-sheet__info-col"/>
              <col class="quote-print-sheet__info-col"/>
              <col class="quote-print-sheet__info-col"/>
            </colgroup>
            <tbody>
              <tr>
                <th>Client（客户）</th>
                <td colspan="2">{{CLIENT}}</td>
                <th>Date（报价日期）</th>
                <td colspan="2">{{DATE}}</td>
              </tr>
              <tr>
                <th>POR（起运港）</th>
                <td colspan="2">{{POR}}</td>
                <th>POD（目的港）</th>
                <td colspan="2">{{POD}}</td>
              </tr>
              <tr>
                <th class="quote-print-sheet__info-label--wrap">
                  <span class="quote-print-sheet__info-label-line">PICK UP ADDRESS</span>
                  <span class="quote-print-sheet__info-label-line">（提货地）</span>
                </th>
                <td colspan="5">{{PICKUP}}</td>
              </tr>
            </tbody>
          </table>
          <table class="quote-print-sheet__fees">
            <thead>
              <tr>
                <th class="col-item">ITEM（内容）</th>
                <th class="col-rate">RATE（费率）</th>
                <th class="col-unit">UNIT（单位）</th>
                <th class="col-ssl">SSL（船司）</th>
                <th class="col-remark">REMARK（备注）</th>
              </tr>
            </thead>
            <tbody>
              {{FEES}}
            </tbody>
          </table>
          <div class="quote-print-sheet__footer">
            <div class="quote-print-sheet__footer-label">REMARK（备注）</div>
            <div class="quote-print-sheet__remark">{{REMARK}}</div>
          </div>
        </div>
        </body>
        </html>
        """
            .replace("{{LOGO}}", logoDataUri)
            // 客户字段不再取值，PDF 与打印预览保持一致，固定显示横杠
            .replace("{{CLIENT}}", "—")
            .replace("{{DATE}}", esc(dateText))
            .replace("{{POR}}", esc(blankDash(sheet.por())))
            .replace("{{POD}}", esc(blankDash(sheet.pod())))
            .replace("{{PICKUP}}", esc(blankDash(sheet.pickUpAddress())))
            .replace("{{FEES}}", fees.toString())
            .replace("{{REMARK}}", esc(nullToEmpty(sheet.sheetRemark())));
  }

  private static List<FeeRow> buildFeeRows(
      QuoteSheetFieldsDto sheet,
      List<String> serviceTypes,
      List<QuoteCostMatchItemDto> costSnapshots,
      Map<String, String> sslRemarkByName) {
    List<FeeRow> rows = new ArrayList<>();
    boolean showSea = hasType(serviceTypes, "SEA");
    boolean showTruck = hasType(serviceTypes, "TRUCK");
    boolean showFumigation =
        serviceTypes == null || serviceTypes.isEmpty()
            ? Boolean.TRUE.equals(sheet.fumigationEnabled())
                || notBlank(sheet.fumigationPoint())
            : hasType(serviceTypes, "FUMIGATION");
    boolean showInsurance = hasType(serviceTypes, "INSURANCE");
    boolean showTrade = hasType(serviceTypes, "TRADE");

    rows.add(FeeRow.group("Service Fees — 主费用"));
    if (showSea) {
      List<OceanEntry> oceans = resolveOceanEntries(sheet, costSnapshots, sslRemarkByName);
      for (int index = 0; index < oceans.size(); index++) {
        OceanEntry entry = oceans.get(index);
        String label =
            oceans.size() <= 1
                ? "海运费 Ocean Freight"
                : ("海运费" + (index + 1) + " Ocean Freight");
        if (!notBlank(entry.rate) && oceans.size() == 1) {
          rows.add(
              FeeRow.fee(label, "", "Per Container", entry.ssl, entry.remark, false));
          continue;
        }
        if (!notBlank(entry.rate)) {
          continue;
        }
        rows.add(
            FeeRow.fee(
                label,
                formatMoney(entry.rate),
                "Per Container",
                entry.ssl,
                entry.remark,
                false));
      }
    }
    if (showTruck) {
      List<BigDecimal> truckingFees = resolveTruckingFees(sheet, costSnapshots);
      for (int index = 0; index < truckingFees.size(); index++) {
        String label =
            truckingFees.size() <= 1
                ? "卡车费 Trucking fee"
                : ("卡车费" + (index + 1) + " Trucking fee");
        rows.add(
            FeeRow.fee(
                label,
                formatMoney(truckingFees.get(index)),
                "Per Container",
                "",
                "",
                true));
      }
    }
    if (showFumigation) {
      rows.add(
          FeeRow.fee(
              "熏蒸费 FM (NON-OAK)",
              formatMoney(sheet.fmNonOak()),
              "Per Container",
              "",
              "",
              true));
      rows.add(
          FeeRow.fee("熏蒸费 FM (OAK)", formatMoney(sheet.fmOak()), "Per Container", "", "", true));
    }
    rows.add(
        FeeRow.fee("单证费 DOC FEE", formatMoney(sheet.docUsd()), "PER BILL", "", "", true));
    if (showInsurance && notBlank(sheet.cargoInsurancePremium())) {
      rows.add(
          FeeRow.fee(
              "保险费 CARGO INSURANCE PREMIUM",
              sheet.cargoInsurancePremium().trim(),
              "PER CIF AMOUNT",
              "",
              "",
              true));
    }
    if (showTrade && notBlank(sheet.cargoAgentFee())) {
      rows.add(
          FeeRow.fee(
              "贸易代理费 CARGO AGENT FEE",
              sheet.cargoAgentFee().trim(),
              "BANK FEE AT COST",
              "",
              "",
              true));
    }
    if (showTruck) {
      rows.add(FeeRow.group("Extra Fee at cost — 可能产生的额外费用"));
      rows.add(
          FeeRow.fee(
              "火车上下车费 NS LIFT",
              formatMoney(sheet.nsLift()),
              "Per Container /Per Time if need",
              "",
              "",
              true));
      rows.add(
          FeeRow.fee(
              "额外底盘费 Chassis",
              formatMoney(sheet.chassis()),
              "Per Container /Per Day if need",
              "",
              "",
              true));
      rows.add(
          FeeRow.fee(
              "额外等待费 Waiting",
              formatMoney(sheet.waiting()),
              "Per Hour, 2H Free if need",
              "",
              "",
              true));
      rows.add(
          FeeRow.fee("二次送箱费 REDELIVERY FEE", "AT COST", "Per Container", "", "", true));
      if (notBlank(sheet.truckRemark())) {
        rows.add(
            FeeRow.fee("备注 (Remark)", sheet.truckRemark().trim(), "", "", "", false)
                .asMergeRate());
      }
    }
    return rows;
  }

  /**
   * 对齐前端 resolveTruckingFeeValues：最多 2 条卡车费。
   * sheet.truckingFee / truckingFee2 优先；缺省时从 ROAD 成本快照补全。
   */
  private static List<BigDecimal> resolveTruckingFees(
      QuoteSheetFieldsDto sheet, List<QuoteCostMatchItemDto> costSnapshots) {
    List<BigDecimal> fromSnap = roadRatesFromSnapshots(costSnapshots);
    BigDecimal first = sheet.truckingFee() != null ? sheet.truckingFee() : firstOrNull(fromSnap, 0);
    BigDecimal second =
        sheet.truckingFee2() != null ? sheet.truckingFee2() : firstOrNull(fromSnap, 1);
    boolean hasSecondSlot = second != null || fromSnap.size() >= MAX_ROAD_MATCHES;
    if (hasSecondSlot) {
      List<BigDecimal> fees = new ArrayList<>(2);
      fees.add(first);
      fees.add(second);
      return fees;
    }
    // List.of 不允许 null 元素；费用缺省时 first 可能为 null，需用 ArrayList（与上面分支一致）
    List<BigDecimal> fees = new ArrayList<>(1);
    fees.add(first);
    return fees;
  }

  private static List<BigDecimal> roadRatesFromSnapshots(
      List<QuoteCostMatchItemDto> costSnapshots) {
    List<BigDecimal> rates = new ArrayList<>();
    if (costSnapshots == null) {
      return rates;
    }
    for (QuoteCostMatchItemDto item : costSnapshots) {
      if (item == null || !"ROAD".equalsIgnoreCase(item.costType())) {
        continue;
      }
      if (rates.size() >= MAX_ROAD_MATCHES) {
        break;
      }
      Map<String, Object> snap = nullToEmptyMap(item.snapshot());
      BigDecimal rate =
          parseMoney(
              firstNonBlank(
                  textOf(snap, "allInNoFm"),
                  textOf(snap, "allIn"),
                  textOf(snap, "truckingFee"),
                  textOf(snap, "allInFmOneWay")));
      rates.add(rate);
    }
    return rates;
  }

  private static BigDecimal firstOrNull(List<BigDecimal> values, int index) {
    if (values == null || index < 0 || index >= values.size()) {
      return null;
    }
    return values.get(index);
  }

  private static BigDecimal parseMoney(String text) {
    if (!notBlank(text)) {
      return null;
    }
    try {
      return new BigDecimal(text.trim().replace(",", "").replace("US$", "").trim());
    } catch (Exception ignore) {
      return null;
    }
  }

  /** 对齐前端 resolveOceanFreightEntries：sheet 费率优先，快照补 SSL/备注。 */
  private static List<OceanEntry> resolveOceanEntries(
      QuoteSheetFieldsDto sheet,
      List<QuoteCostMatchItemDto> costSnapshots,
      Map<String, String> sslRemarkByName) {
    List<String> rates = splitSlash(sheet.oceanFreight());
    List<String> ssls = splitSlash(sheet.ssl());
    List<QuoteCostMatchItemDto> seaMatches = new ArrayList<>();
    if (costSnapshots != null) {
      for (QuoteCostMatchItemDto item : costSnapshots) {
        if (item != null && "SEA".equalsIgnoreCase(item.costType())) {
          seaMatches.add(item);
        }
      }
    }
    if (rates.isEmpty() && seaMatches.isEmpty()) {
      return List.of(
          new OceanEntry(
              "",
              firstNonBlank(sheet.ssl(), ""),
              lookupSslRemark(firstNonBlank(sheet.ssl(), ""), sslRemarkByName)));
    }
    int count = Math.min(MAX_OCEAN_LINES, Math.max(rates.size(), seaMatches.size()));
    if (count == 0) {
      count = 1;
    }
    List<OceanEntry> entries = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      QuoteCostMatchItemDto match = index < seaMatches.size() ? seaMatches.get(index) : null;
      Map<String, Object> snap = match == null ? Map.of() : nullToEmptyMap(match.snapshot());
      String ssl =
          index < ssls.size()
              ? ssls.get(index)
              : firstNonBlank(textOf(snap, "ssl"), index == 0 ? sheet.ssl() : "");
      String rate =
          index < rates.size()
              ? rates.get(index)
              : firstNonBlank(
                  textOf(snap, "allIn"),
                  textOf(snap, "freight"),
                  textOf(snap, "baseFreight"),
                  textOf(snap, "ofRateUsd"));
      String remark = lookupSslRemark(ssl, sslRemarkByName);
      if (!notBlank(remark)) {
        remark = firstNonBlank(textOf(snap, "remark"), "");
      }
      entries.add(new OceanEntry(nullToEmpty(rate), nullToEmpty(ssl), nullToEmpty(remark)));
    }
    return entries;
  }

  private static List<String> splitSlash(String value) {
    if (!notBlank(value)) {
      return List.of();
    }
    String[] parts = value.split("/");
    List<String> result = new ArrayList<>();
    for (String part : parts) {
      if (result.size() >= MAX_OCEAN_LINES) {
        break;
      }
      if (notBlank(part)) {
        result.add(part.trim());
      }
    }
    return result;
  }

  private static String lookupSslRemark(String ssl, Map<String, String> sslRemarkByName) {
    if (!notBlank(ssl) || sslRemarkByName == null || sslRemarkByName.isEmpty()) {
      return "";
    }
    String trimmed = ssl.trim();
    String direct = sslRemarkByName.get(trimmed);
    if (notBlank(direct)) {
      return direct.trim();
    }
    String upper = trimmed.toUpperCase(Locale.ROOT);
    for (Map.Entry<String, String> entry : sslRemarkByName.entrySet()) {
      if (entry.getKey() != null
          && entry.getKey().trim().toUpperCase(Locale.ROOT).equals(upper)
          && notBlank(entry.getValue())) {
        return entry.getValue().trim();
      }
    }
    return "";
  }

  private static Map<String, Object> nullToEmptyMap(Map<String, Object> snap) {
    return snap == null ? Map.of() : snap;
  }

  private static String logoDataUri() {
    try {
      ClassPathResource resource = new ClassPathResource("quote-print/frd-quote-logo.png");
      byte[] bytes = resource.getContentAsByteArray();
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    } catch (Exception ex) {
      return "";
    }
  }

  private static boolean hasType(List<String> serviceTypes, String type) {
    if (serviceTypes == null || serviceTypes.isEmpty()) {
      return true;
    }
    for (String item : serviceTypes) {
      if (type.equalsIgnoreCase(item)) {
        return true;
      }
    }
    return false;
  }

  private static String esc(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  private static String blankDash(String value) {
    return notBlank(value) ? value.trim() : "—";
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static boolean notBlank(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private static String textOf(Map<String, Object> snap, String key) {
    if (snap == null || key == null) {
      return null;
    }
    Object value = snap.get(key);
    return value == null ? null : String.valueOf(value);
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (notBlank(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private static String formatMoney(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof BigDecimal bd) {
      return "US$" + moneyPlain(bd);
    }
    String text = String.valueOf(value).trim();
    if (text.isEmpty()) {
      return "";
    }
    if (text.startsWith("US$") || text.toUpperCase(Locale.ROOT).contains("CIF")) {
      return text;
    }
    try {
      BigDecimal bd = new BigDecimal(text.replace(",", ""));
      return "US$" + moneyPlain(bd);
    } catch (Exception ignore) {
      return text;
    }
  }

  private static String moneyPlain(BigDecimal value) {
    DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
    DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
    return format.format(value.setScale(2, RoundingMode.HALF_UP));
  }

  private record OceanEntry(String rate, String ssl, String remark) {}

  private static final class FeeRow {
    private final boolean group;
    private final String item;
    private final String rate;
    private final String unit;
    private final String ssl;
    private final String remark;
    private final boolean mergeUnitMeta;
    private final boolean mergeRateMeta;

    private FeeRow(
        boolean group,
        String item,
        String rate,
        String unit,
        String ssl,
        String remark,
        boolean mergeUnitMeta,
        boolean mergeRateMeta) {
      this.group = group;
      this.item = item;
      this.rate = rate;
      this.unit = unit;
      this.ssl = ssl;
      this.remark = remark;
      this.mergeUnitMeta = mergeUnitMeta;
      this.mergeRateMeta = mergeRateMeta;
    }

    static FeeRow group(String label) {
      return new FeeRow(true, label, "", "", "", "", false, false);
    }

    static FeeRow fee(
        String item, String rate, String unit, String ssl, String remark, boolean mergeUnitMeta) {
      return new FeeRow(false, item, rate, unit, ssl, remark, mergeUnitMeta, false);
    }

    FeeRow asMergeRate() {
      return new FeeRow(false, item, rate, unit, ssl, remark, false, true);
    }
  }

}
