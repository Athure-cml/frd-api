package com.furuiduo.quote.quote.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.quote.dto.QuoteDetailResponse;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteOrderLine;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.QuoteNoGenerator;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteWorkflowService {

  private static final Set<QuoteStatus> EDITABLE =
      EnumSet.of(
          QuoteStatus.DRAFT,
          QuoteStatus.EFFECTIVE,
          QuoteStatus.FOLLOWING,
          QuoteStatus.SENT,
          QuoteStatus.PENDING);

  private final QuoteOrderRepository quoteOrderRepository;
  private final QuoteAccessService quoteAccessService;
  private final QuoteNoGenerator quoteNoGenerator;
  private final QuoteQueryService quoteQueryService;

  public QuoteWorkflowService(
      QuoteOrderRepository quoteOrderRepository,
      QuoteAccessService quoteAccessService,
      QuoteNoGenerator quoteNoGenerator,
      QuoteQueryService quoteQueryService) {
    this.quoteOrderRepository = quoteOrderRepository;
    this.quoteAccessService = quoteAccessService;
    this.quoteNoGenerator = quoteNoGenerator;
    this.quoteQueryService = quoteQueryService;
  }

  @Transactional
  public QuoteDetailResponse submitEffective(SysUser user, Long id) {
    QuoteOrder order = requireOrder(user, id);
    if (order.getStatus() != QuoteStatus.DRAFT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅草稿可提交生效");
    }
    order.setStatus(QuoteStatus.EFFECTIVE);
    order.setSubmittedAt(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());
    return quoteQueryService.getById(user, quoteOrderRepository.save(order).getId());
  }

  @Transactional
  public QuoteDetailResponse markFollowing(SysUser user, Long id) {
    QuoteOrder order = requireOrder(user, id);
    if (order.getStatus() != QuoteStatus.EFFECTIVE && order.getStatus() != QuoteStatus.SENT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已生效报价可标记跟进中");
    }
    order.setStatus(QuoteStatus.FOLLOWING);
    order.setFollowUpBy(user.getId());
    order.setFollowUpByName(user.getRealName());
    order.setUpdatedAt(LocalDateTime.now());
    return quoteQueryService.getById(user, quoteOrderRepository.save(order).getId());
  }

  @Transactional
  public QuoteDetailResponse markWon(SysUser user, Long id) {
    QuoteOrder order = requireOrder(user, id);
    if (order.getStatus() == QuoteStatus.VOIDED || order.getStatus() == QuoteStatus.WON) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "当前状态不可成交");
    }
    order.setStatus(QuoteStatus.WON);
    order.setUpdatedAt(LocalDateTime.now());
    return quoteQueryService.getById(user, quoteOrderRepository.save(order).getId());
  }

  @Transactional
  public QuoteDetailResponse voidQuote(SysUser user, Long id) {
    QuoteOrder order = requireOrder(user, id);
    if (order.getStatus() == QuoteStatus.VOIDED || order.getStatus() == QuoteStatus.LOST) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "报价单已作废");
    }
    if (order.getStatus() == QuoteStatus.WON) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "已成交报价不可作废");
    }
    order.setStatus(QuoteStatus.VOIDED);
    order.setUpdatedAt(LocalDateTime.now());
    return quoteQueryService.getById(user, quoteOrderRepository.save(order).getId());
  }

  @Transactional
  public QuoteDetailResponse copyAsNew(SysUser user, Long id) {
    QuoteOrder source =
        quoteOrderRepository
            .findWithLinesById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报价单不存在"));
    quoteAccessService.assertReadable(user, source);

    QuoteOrder copy = new QuoteOrder();
    copy.setQuoteNo(quoteNoGenerator.next());
    copy.setStatus(QuoteStatus.DRAFT);
    copy.setCreatedBy(user.getId());
    copy.setCreatedByName(user.getRealName());
    copy.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    copy.setCustomerId(source.getCustomerId());
    copy.setCustomerName(source.getCustomerName());
    copy.setTransportMode(source.getTransportMode());
    copy.setRouteSummary(source.getRouteSummary());
    copy.setCurrency(source.getCurrency());
    copy.setBaseCurrency(source.getBaseCurrency());
    copy.setExchangeRate(source.getExchangeRate());
    copy.setValidUntil(source.getValidUntil());
    copy.setRemark(source.getRemark());
    copy.setTotalAmount(source.getTotalAmount());
    copySheetFields(source, copy);
    for (QuoteOrderLine line : source.getLines()) {
      QuoteOrderLine cloned = new QuoteOrderLine();
      cloned.setQuoteOrder(copy);
      cloned.setSort(line.getSort());
      cloned.setItemName(line.getItemName());
      cloned.setSpec(line.getSpec());
      cloned.setCostMode(line.getCostMode());
      cloned.setCostRefId(line.getCostRefId());
      cloned.setQuantity(line.getQuantity());
      cloned.setUnit(line.getUnit());
      cloned.setUnitPrice(line.getUnitPrice());
      cloned.setAmount(line.getAmount());
      cloned.setExtraJson(line.getExtraJson());
      copy.getLines().add(cloned);
    }
    copy.setCreatedAt(LocalDateTime.now());
    copy.setUpdatedAt(LocalDateTime.now());
    return quoteQueryService.getById(user, quoteOrderRepository.save(copy).getId());
  }

  public boolean isEditable(QuoteStatus status) {
    return EDITABLE.contains(status);
  }

  private QuoteOrder requireOrder(SysUser user, Long id) {
    QuoteOrder order = quoteAccessService.requireReadable(user, id);
    refreshExpiredStatus(order);
    if (order.getStatus() == QuoteStatus.EXPIRED) {
      quoteOrderRepository.save(order);
    }
    return order;
  }

  private void refreshExpiredStatus(QuoteOrder order) {
    if (order.getValidUntil() == null) {
      return;
    }
    if (order.getValidUntil().isBefore(LocalDate.now())
        && order.getStatus() != QuoteStatus.VOIDED
        && order.getStatus() != QuoteStatus.LOST
        && order.getStatus() != QuoteStatus.WON) {
      order.setStatus(QuoteStatus.EXPIRED);
    }
  }

  private void copySheetFields(QuoteOrder source, QuoteOrder target) {
    target.setZipCode(source.getZipCode());
    target.setCity(source.getCity());
    target.setState(source.getState());
    target.setPor(source.getPor());
    target.setPol(source.getPol());
    target.setPod(source.getPod());
    target.setOfUsd(source.getOfUsd());
    target.setSsl(source.getSsl());
    target.setTruckingNonOakUsd(source.getTruckingNonOakUsd());
    target.setTruckingOakUsd(source.getTruckingOakUsd());
    target.setFmNonOak(source.getFmNonOak());
    target.setFmOak(source.getFmOak());
    target.setDocUsd(source.getDocUsd());
    target.setCargoMaxWeightTon(source.getCargoMaxWeightTon());
    target.setSheetRemark(source.getSheetRemark());
  }
}
