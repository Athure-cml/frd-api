package com.furuiduo.quote.quote.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import com.furuiduo.quote.quote.support.QuoteStatusSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteWorkflowService {

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

  /** 草稿 → 待审批 */
  @Transactional
  public QuoteDetailResponse submitForApproval(SysUser user, Long id) {
    QuoteOrder order = requireOperableOrder(user, id);
    if (QuoteStatusSupport.normalize(order.getStatus()) != QuoteStatus.DRAFT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅草稿可提交审批");
    }
    order.setStatus(QuoteStatus.PENDING_APPROVAL);
    order.setSubmittedAt(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());
    return quoteQueryService.getById(user, quoteOrderRepository.save(order).getId());
  }

  /** 待审批 → 已发送 */
  @Transactional
  public QuoteDetailResponse markSent(SysUser user, Long id) {
    QuoteOrder order = requireOrder(user, id);
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (status != QuoteStatus.PENDING_APPROVAL) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待审批报价可发送");
    }
    order.setStatus(QuoteStatus.SENT);
    order.setFollowUpBy(user.getId());
    order.setFollowUpByName(user.getRealName());
    order.setUpdatedAt(LocalDateTime.now());
    return quoteQueryService.getById(user, quoteOrderRepository.save(order).getId());
  }

  /** 待审批 → 草稿（取消审批） */
  @Transactional
  public QuoteDetailResponse cancelApproval(SysUser user, Long id) {
    QuoteOrder order = requireOperableOrder(user, id);
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (status != QuoteStatus.PENDING_APPROVAL) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待审批报价可取消审批");
    }
    order.setStatus(QuoteStatus.DRAFT);
    order.setSubmittedAt(null);
    order.setUpdatedAt(LocalDateTime.now());
    return quoteQueryService.getById(user, quoteOrderRepository.save(order).getId());
  }

  /** 已发送 → 已拒绝 */
  @Transactional
  public QuoteDetailResponse reject(SysUser user, Long id) {
    QuoteOrder order = requireOrder(user, id);
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (status != QuoteStatus.SENT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已发送报价可拒绝");
    }
    order.setStatus(QuoteStatus.REJECTED);
    order.setUpdatedAt(LocalDateTime.now());
    return quoteQueryService.getById(user, quoteOrderRepository.save(order).getId());
  }

  /** 已发送 → 已成交 */
  @Transactional
  public QuoteDetailResponse markWon(SysUser user, Long id) {
    QuoteOrder order = requireOrder(user, id);
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (status == QuoteStatus.WON) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "报价单已成交");
    }
    if (QuoteStatusSupport.isAbandoned(status)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "已放弃的报价不可成交");
    }
    if (status != QuoteStatus.SENT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已发送报价可标记成交");
    }
    order.setStatus(QuoteStatus.WON);
    order.setUpdatedAt(LocalDateTime.now());
    return quoteQueryService.getById(user, quoteOrderRepository.save(order).getId());
  }

  /** 作废 → 已作废 */
  @Transactional
  public QuoteDetailResponse voidQuote(SysUser user, Long id) {
    QuoteOrder order = requireOperableOrder(user, id);
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (status == QuoteStatus.VOIDED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "报价单已作废");
    }
    if (status == QuoteStatus.WON) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "已成交报价不可作废");
    }
    if (QuoteStatusSupport.isAbandoned(status)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "报价单已放弃");
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
    return QuoteStatusSupport.isEditable(status);
  }

  private QuoteOrder requireOrder(SysUser user, Long id) {
    QuoteOrder order = quoteAccessService.requireReadable(user, id);
    refreshExpiredStatus(order);
    if (order.getStatus() == QuoteStatus.EXPIRED) {
      quoteOrderRepository.save(order);
    }
    return order;
  }

  private QuoteOrder requireOperableOrder(SysUser user, Long id) {
    QuoteOrder order = requireOrder(user, id);
    quoteAccessService.assertOperable(user, order);
    return order;
  }

  private void refreshExpiredStatus(QuoteOrder order) {
    if (order.getValidUntil() == null) {
      return;
    }
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (order.getValidUntil().isBefore(LocalDate.now())
        && status != QuoteStatus.VOIDED
        && status != QuoteStatus.REJECTED
        && status != QuoteStatus.WON
        && status != QuoteStatus.EXPIRED) {
      order.setStatus(QuoteStatus.EXPIRED);
    }
  }

  private void copySheetFields(QuoteOrder source, QuoteOrder target) {
    target.setZipCode(source.getZipCode());
    target.setCity(source.getCity());
    target.setState(source.getState());
    target.setPickUpAddress(source.getPickUpAddress());
    target.setPor(source.getPor());
    target.setPol(source.getPol());
    target.setPod(source.getPod());
    target.setOfUsd(source.getOfUsd());
    target.setSsl(source.getSsl());
    target.setTruckingFee(source.getTruckingFee());
    target.setNsLift(source.getNsLift());
    target.setChassis(source.getChassis());
    target.setWaiting(source.getWaiting());
    target.setRedeliveryFee(source.getRedeliveryFee());
    target.setTruckRemark(source.getTruckRemark());
    target.setTruckingNonOakUsd(source.getTruckingNonOakUsd());
    target.setTruckingOakUsd(source.getTruckingOakUsd());
    target.setFmNonOak(source.getFmNonOak());
    target.setFmOak(source.getFmOak());
    target.setDocUsd(source.getDocUsd());
    target.setCargoInsurancePremium(source.getCargoInsurancePremium());
    target.setCargoAgentFee(source.getCargoAgentFee());
    target.setCargoMaxWeightTon(source.getCargoMaxWeightTon());
    target.setCifAmount(source.getCifAmount());
    target.setSheetRemark(source.getSheetRemark());
  }
}
