package com.furuiduo.quote.quote.service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteExportService {

  private static final String[] HEADERS = {
    "QUOTE NO",
    "CLINET",
    "PICK UP ADDRESS",
    "POR/POL",
    "POD",
    "OCEAN FREIGHT",
    "TRUCKING FEE",
    "NS LIFT",
    "CHASSIS",
    "WAITING",
    "REDELIVERY FEE",
    "TRUCK REMARK",
    "FM (NON-OAK)",
    "FM (OAK)",
    "DOC FEE",
    "CARGO INSURANCE PREMIUM",
    "CARGO AGENT FEE",
    "REMARK",
    "QUOTE DATA",
    "状态"
  };

  private final QuoteOrderRepository quoteOrderRepository;
  private final QuoteAccessService quoteAccessService;
  private final QuoteQueryService quoteQueryService;

  public QuoteExportService(
      QuoteOrderRepository quoteOrderRepository,
      QuoteAccessService quoteAccessService,
      QuoteQueryService quoteQueryService) {
    this.quoteOrderRepository = quoteOrderRepository;
    this.quoteAccessService = quoteAccessService;
    this.quoteQueryService = quoteQueryService;
  }

  /** 勾选优先；否则按搜索条件；无条件则导出权限范围内全部。 */
  public byte[] export(
      SysUser user,
      List<Long> ids,
      String quoteNo,
      String customerName,
      String transportMode,
      String status,
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      String pod,
      String ssl,
      String followUpByName) {
    List<QuoteOrder> orders;
    if (RequestIds.present(ids)) {
      orders =
          quoteOrderRepository.findAllById(ids).stream()
              .peek(order -> quoteAccessService.assertReadable(user, order))
              .sorted(Comparator.comparing(QuoteOrder::getId))
              .toList();
    } else {
      orders =
          quoteQueryService.findOrdersForExport(
              user,
              quoteNo,
              customerName,
              transportMode,
              status,
              zipCode,
              city,
              state,
              por,
              pol,
              pod,
              ssl,
              followUpByName);
    }
    if (orders.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "没有可导出的报价单");
    }
    return writeWorkbook(orders);
  }

  /** @deprecated 请使用 {@link #export} */
  public byte[] exportByIds(SysUser user, List<Long> ids) {
    return export(user, ids, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  private byte[] writeWorkbook(List<QuoteOrder> orders) {
    try (var workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Quotes");
      CostExcelSupport.writeHeaderRow(sheet, HEADERS);
      int rowIndex = 1;
      for (QuoteOrder order : orders) {
        Row row = sheet.createRow(rowIndex++);
        QuoteSheetFieldsDto sheetFields = QuoteSheetFieldsDto.from(order);
        int col = 0;
        row.createCell(col++).setCellValue(nullToEmpty(order.getQuoteNo()));
        row.createCell(col++).setCellValue(nullToEmpty(order.getCustomerName()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.pickUpAddress()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.porPol()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.pod()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.oceanFreight()));
        setDecimal(row.createCell(col++), sheetFields.truckingFee());
        setDecimal(row.createCell(col++), sheetFields.nsLift());
        setDecimal(row.createCell(col++), sheetFields.chassis());
        setDecimal(row.createCell(col++), sheetFields.waiting());
        setDecimal(row.createCell(col++), sheetFields.redeliveryFee());
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.truckRemark()));
        setDecimal(row.createCell(col++), sheetFields.fmNonOak());
        setDecimal(row.createCell(col++), sheetFields.fmOak());
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.docUsd()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.cargoInsurancePremium()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.cargoAgentFee()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.sheetRemark()));
        row.createCell(col++)
            .setCellValue(
                order.getCreatedAt() != null
                    ? QuoteDateTimes.format(order.getCreatedAt()).substring(0, 10)
                    : "");
        row.createCell(col++)
            .setCellValue(
                order.getStatus() == null
                    ? ""
                    : QuoteStatusSupport.displayStatus(order.getStatus()));
      }
      return CostExcelSupport.writeWorkbook(workbook);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "导出失败");
    }
  }

  private void setDecimal(org.apache.poi.ss.usermodel.Cell cell, java.math.BigDecimal value) {
    if (value == null) {
      cell.setBlank();
      return;
    }
    cell.setCellValue(value.doubleValue());
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
