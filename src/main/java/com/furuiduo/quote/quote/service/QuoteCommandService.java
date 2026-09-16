package com.furuiduo.quote.quote.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.currency.service.CurrencyCommandService;
import com.furuiduo.quote.customer.entity.Customer;
import com.furuiduo.quote.customer.service.CustomerCommandService;
import com.furuiduo.quote.exchangerate.support.ExchangeRateResolver;
import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.dto.QuoteDetailResponse;
import com.furuiduo.quote.quote.dto.QuoteLineSaveRequest;
import com.furuiduo.quote.quote.dto.QuoteSaveRequest;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteOrderLine;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.QuoteNoGenerator;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteCommandService {

  private final QuoteOrderRepository quoteOrderRepository;
  private final QuoteQueryService quoteQueryService;
  private final QuoteAccessService quoteAccessService;
  private final QuoteNoGenerator quoteNoGenerator;
  private final CustomerCommandService customerCommandService;
  private final CurrencyCommandService currencyCommandService;
  private final ExchangeRateResolver exchangeRateResolver;
  private final QuoteCostMatchService quoteCostMatchService;

  public QuoteCommandService(
      QuoteOrderRepository quoteOrderRepository,
      QuoteQueryService quoteQueryService,
      QuoteAccessService quoteAccessService,
      QuoteNoGenerator quoteNoGenerator,
      CustomerCommandService customerCommandService,
      CurrencyCommandService currencyCommandService,
      ExchangeRateResolver exchangeRateResolver,
      QuoteCostMatchService quoteCostMatchService) {
    this.quoteOrderRepository = quoteOrderRepository;
    this.quoteQueryService = quoteQueryService;
    this.quoteAccessService = quoteAccessService;
    this.quoteNoGenerator = quoteNoGenerator;
    this.customerCommandService = customerCommandService;
    this.currencyCommandService = currencyCommandService;
    this.exchangeRateResolver = exchangeRateResolver;
    this.quoteCostMatchService = quoteCostMatchService;
  }

  @Transactional
  public QuoteDetailResponse create(SysUser user, QuoteSaveRequest request) {
    validateSaveRequest(request);

    QuoteOrder order = new QuoteOrder();
    order.setQuoteNo(quoteNoGenerator.next());
    order.setStatus(QuoteStatus.DRAFT);
    order.setCreatedBy(user.getId());
    order.setCreatedByName(user.getRealName());
    order.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    applySaveRequest(order, request);
    QuoteOrder saved = quoteOrderRepository.save(order);
    if (request.costMatches() != null && !request.costMatches().isEmpty()) {
      quoteCostMatchService.replaceSnapshots(saved, request.costMatches());
    }
    return quoteQueryService.getById(user, saved.getId());
  }

  @Transactional
  public QuoteDetailResponse update(SysUser user, Long id, QuoteSaveRequest request) {
    validateSaveRequest(request);

    QuoteOrder order = requireEditable(user, id);
    applySaveRequest(order, request);
    order.setUpdatedAt(LocalDateTime.now());

    QuoteOrder saved = quoteOrderRepository.save(order);
    if (QuoteStatusSupport.normalize(order.getStatus()) == QuoteStatus.DRAFT
        && request.costMatches() != null
        && !request.costMatches().isEmpty()) {
      quoteCostMatchService.replaceSnapshots(saved, request.costMatches());
    }
    return quoteQueryService.getById(user, saved.getId());
  }

  @Transactional
  public void delete(SysUser user, Long id) {
    QuoteOrder order = requireDeletable(user, id);
    quoteOrderRepository.delete(order);
  }

  private QuoteOrder requireEditable(SysUser user, Long id) {
    QuoteOrder order =
        quoteOrderRepository
            .findWithLinesById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报价单不存在"));
    quoteQueryService.assertReadable(user, order);
    quoteAccessService.assertOperable(user, order);
    if (!QuoteStatusSupport.isEditable(order.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "当前状态不可编辑");
    }
    return order;
  }

  private QuoteOrder requireDeletable(SysUser user, Long id) {
    QuoteOrder order =
        quoteOrderRepository
            .findWithLinesById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报价单不存在"));
    quoteQueryService.assertReadable(user, order);
    quoteAccessService.assertOperable(user, order);
    if (!QuoteStatusSupport.isDeletable(order.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅草稿或已作废报价可删除");
    }
    return order;
  }

  private void validateSaveRequest(QuoteSaveRequest request) {
    if (request.customerId() == null
        && (request.customerName() == null || request.customerName().isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择客户");
    }
    if (request.transportMode() == null || request.transportMode().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "运输方式不能为空");
    }
    try {
      request.parsedTransportMode();
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的运输方式");
    }
  }

  private void applySaveRequest(QuoteOrder order, QuoteSaveRequest request) {
    if (request.customerId() != null) {
      Customer customer = customerCommandService.requireEnabled(request.customerId());
      order.setCustomerId(customer.getId());
      order.setCustomerName(customer.getName());
    } else {
      order.setCustomerId(null);
      order.setCustomerName(request.customerName().trim());
    }
    order.setTransportMode(request.parsedTransportMode());
    order.setRouteSummary(trimToNull(request.routeSummary()));
    applyCurrencySnapshot(order, request);
    order.setValidUntil(request.validUntil());
    order.setRemark(trimToNull(request.remark()));
    applySheetFields(order, request);
    order.getLines().clear();

    BigDecimal total = BigDecimal.ZERO;
    var lines = request.lines() == null ? new ArrayList<QuoteLineSaveRequest>() : request.lines();
    for (int i = 0; i < lines.size(); i++) {
      QuoteLineSaveRequest lineReq = lines.get(i);
      if (lineReq.itemName() == null || lineReq.itemName().isBlank()) {
        continue;
      }
      QuoteOrderLine line = new QuoteOrderLine();
      line.setQuoteOrder(order);
      line.setSort(lineReq.sort() != null ? lineReq.sort() : i);
      line.setItemName(lineReq.itemName().trim());
      line.setSpec(trimToNull(lineReq.spec()));
      line.setCostMode(lineReq.parsedCostMode());
      line.setCostRefId(lineReq.costRefId());
      line.setQuantity(defaultDecimal(lineReq.quantity(), BigDecimal.ONE));
      line.setUnit(trimToNull(lineReq.unit()));
      line.setUnitPrice(defaultDecimal(lineReq.unitPrice(), BigDecimal.ZERO));
      BigDecimal amount =
          line.getQuantity().multiply(line.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
      line.setAmount(amount);
      if (lineReq.extraJson() != null) {
        line.setExtraJson(lineReq.extraJson());
      }
      order.getLines().add(line);
      total = total.add(amount);
    }
    order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
    order.setUpdatedAt(LocalDateTime.now());
  }

  private void applySheetFields(QuoteOrder order, QuoteSaveRequest request) {
    order.setZipCode(trimToNull(request.zipCode()));
    order.setCity(trimToNull(request.city()));
    order.setState(trimToNull(request.state()));
    order.setPickUpAddress(trimToNull(request.pickUpAddress()));
    order.setPor(trimToNull(request.por()));
    order.setPol(trimToNull(request.pol()));
    order.setPod(trimToNull(request.pod()));
    order.setOfUsd(trimToNull(request.oceanFreight()));
    order.setSsl(trimToNull(request.ssl()));
    order.setTruckingFee(request.truckingFee());
    order.setNsLift(request.nsLift());
    order.setChassis(request.chassis());
    order.setWaiting(request.waiting());
    order.setRedeliveryFee(request.redeliveryFee());
    order.setTruckRemark(trimToNull(request.truckRemark()));
    order.setTruckingNonOakUsd(
        request.truckingNonOakUsd() != null ? request.truckingNonOakUsd() : request.truckingFee());
    order.setTruckingOakUsd(request.truckingOakUsd());
    order.setFmNonOak(request.fmNonOak());
    order.setFmOak(request.fmOak());
    order.setFumigationPoint(trimToNull(request.fumigationPoint()));
    order.setFumigationEnabled(resolveFumigationEnabled(request));
    order.setDocUsd(trimToNull(request.docUsd()));
    order.setCargoInsurancePremium(trimToNull(request.cargoInsurancePremium()));
    order.setCargoAgentFee(trimToNull(request.cargoAgentFee()));
    order.setCargoMaxWeightTon(trimToNull(request.cargoMaxWeightTon()));
    order.setCifAmount(request.cifAmount());
    order.setSheetRemark(trimToNull(request.sheetRemark()));
    if (request.followUpBy() != null) {
      order.setFollowUpBy(request.followUpBy());
    }
    if (request.followUpByName() != null && !request.followUpByName().isBlank()) {
      order.setFollowUpByName(request.followUpByName().trim());
    }
  }

  private void applyCurrencySnapshot(QuoteOrder order, QuoteSaveRequest request) {
    String currencyCode =
        request.currency() == null || request.currency().isBlank()
            ? currencyCommandService.getBaseCurrencyCode()
            : request.currency().trim().toUpperCase();
    var currency = currencyCommandService.requireEnabled(currencyCode);
    String baseCode = currencyCommandService.getBaseCurrencyCode();
    order.setCurrency(currency.getCode());
    order.setBaseCurrency(baseCode);
    if (currency.getCode().equalsIgnoreCase(baseCode)) {
      order.setExchangeRate(null);
      return;
    }
    LocalDate asOf = request.validUntil() != null ? request.validUntil() : LocalDate.now();
    order.setExchangeRate(exchangeRateResolver.resolve(currency.getCode(), baseCode, asOf));
  }

  private BigDecimal defaultDecimal(BigDecimal value, BigDecimal fallback) {
    return value == null ? fallback : value;
  }

  private boolean resolveFumigationEnabled(QuoteSaveRequest request) {
    if (trimToNull(request.fumigationPoint()) != null) {
      return true;
    }
    return Boolean.TRUE.equals(request.fumigationEnabled());
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
