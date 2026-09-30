package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.furuiduo.quote.cost.dto.CostExportColumn;
import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;
import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.service.CostGridTemplateService;
import com.furuiduo.quote.quote.dto.QuoteLibraryExportColumnSpec;
import com.furuiduo.quote.quote.support.QuoteLibraryExcelExporter;
import com.furuiduo.quote.quote.support.QuoteLibraryExportColumns;
import com.furuiduo.quote.quote.support.QuoteLibraryResponseReaders;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteLibraryExportService {

  private final ObjectMapper objectMapper;
  private final CostGridTemplateService templateService;
  private final QuoteLibraryService quoteLibraryService;

  public QuoteLibraryExportService(
      ObjectMapper objectMapper,
      CostGridTemplateService templateService,
      QuoteLibraryService quoteLibraryService) {
    this.objectMapper = objectMapper;
    this.templateService = templateService;
    this.quoteLibraryService = quoteLibraryService;
  }

  public byte[] exportRoad(
      SysUser user,
      String zipCode,
      String city,
      String state,
      String por,
      String pol,
      String supplier,
      BigDecimal redelivery,
      String effectiveDate,
      String validDate,
      String status,
      Boolean highlightOnly,
      List<Long> ids,
      String columnsJson) {
    List<RoadCostResponse> rows =
        quoteLibraryService.listRoadForExport(
            user,
            zipCode,
            city,
            state,
            por,
            pol,
            supplier,
            redelivery,
            effectiveDate,
            validDate,
            status,
            highlightOnly,
            ids);
    if (rows.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "没有可导出的报价库数据");
    }
    List<CostExportColumn> columns =
        resolveColumns(columnsJson, QuoteLibraryExportColumns.roadDefaults());
    return QuoteLibraryExcelExporter.export(
        "road",
        rows,
        columns,
        (row, field) -> QuoteLibraryResponseReaders.readRoad((RoadCostResponse) row, field));
  }

  public byte[] exportSea(
      SysUser user,
      String por,
      String pol,
      String pod,
      String ssl,
      String containerType,
      String agent,
      String freightValidDate,
      String freightEffDate,
      String status,
      String remark,
      Boolean highlightOnly,
      List<Long> ids,
      String columnsJson) {
    List<FreightCostResponse> rows =
        quoteLibraryService.listSeaForExport(
            user,
            por,
            pol,
            pod,
            ssl,
            containerType,
            agent,
            freightValidDate,
            freightEffDate,
            status,
            remark,
            highlightOnly,
            ids);
    if (rows.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "没有可导出的报价库数据");
    }
    List<CostExportColumn> columns =
        resolveColumns(columnsJson, QuoteLibraryExportColumns.seaDefaults());
    return QuoteLibraryExcelExporter.export(
        "sea",
        rows,
        columns,
        (row, field) -> QuoteLibraryResponseReaders.readSea((FreightCostResponse) row, field));
  }

  public byte[] exportFumigation(
      SysUser user,
      String region,
      String station,
      String outdoorValidity,
      String indoorValidity,
      String status,
      Boolean highlightOnly,
      List<Long> ids,
      String columnsJson) {
    CostTableTemplateLayout layout = templateService.resolveExportLayout("fumigation", null);
    List<FumigationCostResponse> rows =
        quoteLibraryService.listFumigationForExport(
            user, region, station, outdoorValidity, indoorValidity, status, highlightOnly, ids);
    if (rows.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "没有可导出的报价库数据");
    }
    List<CostExportColumn> columns =
        resolveColumns(columnsJson, QuoteLibraryExportColumns.fumigationDefaults(layout));
    return QuoteLibraryExcelExporter.export(
        "fumigation",
        rows,
        columns,
        (row, field) ->
            QuoteLibraryResponseReaders.readFumigation((FumigationCostResponse) row, field));
  }

  private List<CostExportColumn> resolveColumns(String columnsJson, List<CostExportColumn> defaults) {
    if (columnsJson == null || columnsJson.isBlank()) {
      return defaults;
    }
    try {
      List<QuoteLibraryExportColumnSpec> specs =
          objectMapper.readValue(columnsJson, new TypeReference<>() {});
      if (specs == null || specs.isEmpty()) {
        return defaults;
      }
      List<CostExportColumn> columns = new ArrayList<>();
      for (QuoteLibraryExportColumnSpec spec : specs) {
        if (spec == null || spec.field() == null || spec.field().isBlank()) {
          continue;
        }
        String field = normalizeField(spec.field());
        if (isSkippedField(field)) {
          continue;
        }
        String header =
            spec.title() == null || spec.title().isBlank() ? field : spec.title().trim();
        columns.add(new CostExportColumn(field, header));
      }
      return columns.isEmpty() ? defaults : columns;
    } catch (Exception ex) {
      return defaults;
    }
  }

  private static String normalizeField(String field) {
    String trimmed = field.trim();
    if (trimmed.startsWith("extraFields.")) {
      return trimmed.substring("extraFields.".length());
    }
    return trimmed;
  }

  private static boolean isSkippedField(String field) {
    return "operation".equals(field) || "relationLink".equals(field);
  }
}
