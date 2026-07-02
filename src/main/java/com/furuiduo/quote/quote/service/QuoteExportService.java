package com.furuiduo.quote.quote.service;

import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.cost.support.CostExcelSupport;
import com.furuiduo.quote.quote.dto.QuoteSheetFieldsDto;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteExportService {

  private static final String[] HEADERS = {
    "Zip code",
    "City",
    "State",
    "POR",
    "POL",
    "POD",
    "O/F (USD)",
    "SSL",
    "TRUCKING NON OAK (USD)",
    "TRUCKING OAK (USD)",
    "FM NON OAK",
    "FM OAK",
    "DOC (USD)",
    "CARGO Max weight (ton)",
    "REMARK",
    "Customer",
    "Currency",
    "Valid Until",
    "Status",
    "Follow Up By",
    "Quote No"
  };

  private final QuoteOrderRepository quoteOrderRepository;
  private final QuoteAccessService quoteAccessService;

  public QuoteExportService(
      QuoteOrderRepository quoteOrderRepository, QuoteAccessService quoteAccessService) {
    this.quoteOrderRepository = quoteOrderRepository;
    this.quoteAccessService = quoteAccessService;
  }

  public byte[] exportByIds(SysUser user, List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要导出的报价单");
    }
    List<QuoteOrder> orders =
        quoteOrderRepository.findAllById(ids).stream()
            .peek(order -> quoteAccessService.assertReadable(user, order))
            .toList();
    if (orders.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到可导出的报价单");
    }
    try (var workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Quotes");
      CostExcelSupport.writeHeaderRow(sheet, HEADERS);
      int rowIndex = 1;
      for (QuoteOrder order : orders) {
        Row row = sheet.createRow(rowIndex++);
        QuoteSheetFieldsDto sheetFields = QuoteSheetFieldsDto.from(order);
        int col = 0;
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.zipCode()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.city()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.state()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.por()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.pol()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.pod()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.ofUsd()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.ssl()));
        setDecimal(row.createCell(col++), sheetFields.truckingNonOakUsd());
        setDecimal(row.createCell(col++), sheetFields.truckingOakUsd());
        setDecimal(row.createCell(col++), sheetFields.fmNonOak());
        setDecimal(row.createCell(col++), sheetFields.fmOak());
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.docUsd()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.cargoMaxWeightTon()));
        row.createCell(col++).setCellValue(nullToEmpty(sheetFields.sheetRemark()));
        row.createCell(col++).setCellValue(nullToEmpty(order.getCustomerName()));
        row.createCell(col++).setCellValue(nullToEmpty(order.getCurrency()));
        row.createCell(col++)
            .setCellValue(order.getValidUntil() != null ? order.getValidUntil().toString() : "");
        row.createCell(col++).setCellValue(order.getStatus().name());
        row.createCell(col++).setCellValue(nullToEmpty(order.getFollowUpByName()));
        row.createCell(col).setCellValue(nullToEmpty(order.getQuoteNo()));
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
