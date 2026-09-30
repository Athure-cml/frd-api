package com.furuiduo.quote.quote.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.approval.service.ApprovalConfigService;
import com.furuiduo.quote.quote.dto.QuoteCostMatchItemDto;
import com.furuiduo.quote.quote.dto.QuoteDetailResponse;
import com.furuiduo.quote.quote.dto.QuoteReviseRequest;
import com.furuiduo.quote.quote.entity.QuoteApprovalAction;
import com.furuiduo.quote.quote.entity.QuoteApprovalLog;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteOrderLine;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.QuoteApprovalPrintSnapshotSupport;
import com.furuiduo.quote.quote.support.QuoteApprovalSupport;
import com.furuiduo.quote.quote.support.QuoteNoGenerator;
import com.furuiduo.quote.quote.support.QuoteServiceTypesSupport;
import com.furuiduo.quote.quote.support.QuoteStatusSupport;
import com.furuiduo.quote.sys.entity.OperationAction;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.OperationLogService;

@Service
public class QuoteWorkflowService {

  private final QuoteOrderRepository quoteOrderRepository;
  private final QuoteAccessService quoteAccessService;
  private final QuoteNoGenerator quoteNoGenerator;
  private final QuoteQueryService quoteQueryService;
  private final QuoteApprovalLogService quoteApprovalLogService;
  private final ApprovalConfigService approvalConfigService;
  private final QuoteCostMatchService quoteCostMatchService;
  private final QuoteCostRiskService quoteCostRiskService;
  private final OperationLogService operationLogService;

  public QuoteWorkflowService(
      QuoteOrderRepository quoteOrderRepository,
      QuoteAccessService quoteAccessService,
      QuoteNoGenerator quoteNoGenerator,
      QuoteQueryService quoteQueryService,
      QuoteApprovalLogService quoteApprovalLogService,
      ApprovalConfigService approvalConfigService,
      QuoteCostMatchService quoteCostMatchService,
      QuoteCostRiskService quoteCostRiskService,
      OperationLogService operationLogService) {
    this.quoteOrderRepository = quoteOrderRepository;
    this.quoteAccessService = quoteAccessService;
    this.quoteNoGenerator = quoteNoGenerator;
    this.quoteQueryService = quoteQueryService;
    this.quoteApprovalLogService = quoteApprovalLogService;
    this.approvalConfigService = approvalConfigService;
    this.quoteCostMatchService = quoteCostMatchService;
    this.quoteCostRiskService = quoteCostRiskService;
    this.operationLogService = operationLogService;
  }

  /** 草稿 → 待审批 */
  @Transactional
  public QuoteDetailResponse submitForApproval(SysUser user, Long id) {
    QuoteOrder order = requireOperableOrder(user, id);
    if (QuoteStatusSupport.normalize(order.getStatus()) != QuoteStatus.DRAFT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅草稿可提交审批");
    }
    if (order.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "报价单已删除");
    }
    order.setStatus(QuoteStatus.PENDING_APPROVAL);
    order.setSubmittedAt(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());
    QuoteOrder saved = quoteOrderRepository.save(order);
    List<QuoteApprovalSupport.FlowNode> flow = currentConfigNodes();
    String printSnapshot =
        QuoteApprovalPrintSnapshotSupport.serialize(
            saved, quoteCostMatchService.listSnapshots(saved.getId(), null));
    String submitComment = null;
    if (saved.getRevisionNo() != null
        && saved.getRevisionNo() > 0
        && saved.getChangeReason() != null
        && !saved.getChangeReason().isBlank()) {
      submitComment = "变更原因：" + saved.getChangeReason().trim();
    }
    quoteApprovalLogService.append(
        saved.getId(),
        QuoteApprovalAction.SUBMIT,
        QuoteStatus.DRAFT,
        QuoteStatus.PENDING_APPROVAL,
        submitComment,
        QuoteApprovalSupport.nodeTitle(QuoteApprovalAction.SUBMIT, List.of(), 0),
        QuoteApprovalSupport.serializeFlow(flow),
        printSnapshot,
        user);
    return quoteQueryService.getById(user, saved.getId());
  }

  /** 待审批 → 下一节点待审批，或最后一节点通过后变为已确认报价 */
  @Transactional
  public QuoteDetailResponse markSent(SysUser user, Long id, String comment) {
    QuoteOrder order = requireOrder(user, id);
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (status != QuoteStatus.PENDING_APPROVAL) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待审批报价可审批通过");
    }
    requireCurrentApprover(user, order.getId());
    List<QuoteApprovalSupport.FlowNode> flow = cycleFlow(order.getId());
    int approvedCount = currentCycleApproveCount(order.getId());
    boolean lastNode = flow.isEmpty() || approvedCount >= flow.size() - 1;
    if (!lastNode) {
      order.setUpdatedAt(LocalDateTime.now());
      QuoteOrder saved = quoteOrderRepository.save(order);
      quoteApprovalLogService.append(
          saved.getId(),
          QuoteApprovalAction.APPROVE,
          QuoteStatus.PENDING_APPROVAL,
          QuoteStatus.PENDING_APPROVAL,
          comment,
          resolveNodeTitle(QuoteApprovalAction.APPROVE, saved.getId()),
          user);
      return quoteQueryService.getById(user, saved.getId());
    }
    order.setStatus(QuoteStatus.SENT);
    order.setApprovedBy(user.getId());
    order.setApprovedByName(user.getRealName());
    order.setApprovedAt(LocalDateTime.now());
    order.setFollowUpBy(user.getId());
    order.setFollowUpByName(user.getRealName());
    order.setUpdatedAt(LocalDateTime.now());
    if (order.getRevisionNo() != null && order.getRevisionNo() > 0) {
      supersedePreviousVersions(order);
      order.setCurrentVersion(true);
      quoteCostRiskService.clearRisk(user, order);
    } else if (order.getRootQuoteId() == null) {
      order.setRootQuoteId(order.getId());
      order.setCurrentVersion(true);
    }
    QuoteOrder saved = quoteOrderRepository.save(order);
    quoteApprovalLogService.append(
        saved.getId(),
        QuoteApprovalAction.APPROVE,
        QuoteStatus.PENDING_APPROVAL,
        QuoteStatus.SENT,
        comment,
        resolveNodeTitle(QuoteApprovalAction.APPROVE, saved.getId()),
        user);
    return quoteQueryService.getById(user, saved.getId());
  }

  /** 待审批 → 草稿（审批驳回，保留审批痕迹） */
  @Transactional
  public QuoteDetailResponse rejectApproval(SysUser user, Long id, String comment) {
    QuoteOrder order = requireOrder(user, id);
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (status != QuoteStatus.PENDING_APPROVAL) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待审批报价可驳回");
    }
    requireCurrentApprover(user, order.getId());
    order.setStatus(QuoteStatus.DRAFT);
    order.setUpdatedAt(LocalDateTime.now());
    QuoteOrder saved = quoteOrderRepository.save(order);
    quoteApprovalLogService.append(
        saved.getId(),
        QuoteApprovalAction.REJECT,
        QuoteStatus.PENDING_APPROVAL,
        QuoteStatus.DRAFT,
        comment,
        resolveNodeTitle(QuoteApprovalAction.REJECT, saved.getId()),
        user);
    return quoteQueryService.getById(user, saved.getId());
  }

  /** 待审批 → 草稿（发起人撤回，本次流程终止，保留审批痕迹） */
  @Transactional
  public QuoteDetailResponse cancelApproval(SysUser user, Long id, String comment) {
    QuoteOrder order = requireOperableOrder(user, id);
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (status != QuoteStatus.PENDING_APPROVAL) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待审批报价可撤回");
    }
    order.setStatus(QuoteStatus.DRAFT);
    order.setUpdatedAt(LocalDateTime.now());
    QuoteOrder saved = quoteOrderRepository.save(order);
    quoteApprovalLogService.append(
        saved.getId(),
        QuoteApprovalAction.ROLLBACK,
        QuoteStatus.PENDING_APPROVAL,
        QuoteStatus.DRAFT,
        comment,
        QuoteApprovalSupport.nodeTitle(QuoteApprovalAction.ROLLBACK, List.of(), 0),
        user);
    return quoteQueryService.getById(user, saved.getId());
  }

  /** 已发送 → 已拒绝 */
  @Transactional
  public QuoteDetailResponse reject(SysUser user, Long id) {
    QuoteOrder order = requireOrder(user, id);
    QuoteStatus status = QuoteStatusSupport.normalize(order.getStatus());
    if (status != QuoteStatus.SENT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已确认报价可拒绝");
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
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已确认报价可标记成交");
    }
    if (!Boolean.TRUE.equals(order.getCurrentVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅当前生效版本可标记成交");
    }
    if (Boolean.TRUE.equals(order.getCostRiskActive())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "存在成本风险的报价不可成交，请先发起变更");
    }
    order.setStatus(QuoteStatus.WON);
    quoteCostRiskService.clearRisk(user, order);
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
    if (status != QuoteStatus.SENT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已确认报价可作废");
    }
    if (!Boolean.TRUE.equals(order.getCurrentVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅当前生效版本可作废");
    }
    if (Boolean.TRUE.equals(order.getCostRiskActive())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "存在成本风险的报价不可作废，请先发起变更");
    }
    order.setStatus(QuoteStatus.VOIDED);
    quoteCostRiskService.clearRisk(user, order);
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
    copy.setServiceTypes(QuoteServiceTypesSupport.copyOf(source.getServiceTypes()));
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
    copy.setParentQuoteId(null);
    copy.setRootQuoteId(null);
    copy.setRevisionNo(0);
    copy.setCurrentVersion(true);
    copy.setChangeReason(null);
    copy.setSupersededByQuoteId(null);
    QuoteOrder savedCopy = quoteOrderRepository.save(copy);
    savedCopy.setRootQuoteId(savedCopy.getId());
    return quoteQueryService.getById(user, quoteOrderRepository.save(savedCopy).getId());
  }

  /**
   * 已确认报价发起变更：复制为新草稿（含成本快照），原单进入「变更中」。
   * 同一版本族同一时间仅允许一张进行中的变更单。
   */
  @Transactional
  public QuoteDetailResponse reviseAsNew(SysUser user, Long id, QuoteReviseRequest request) {
    if (request == null || request.changeReason() == null || request.changeReason().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "变更原因不能为空");
    }
    QuoteOrder source =
        quoteOrderRepository
            .findWithLinesById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报价单不存在"));
    quoteAccessService.assertReadable(user, source);
    quoteAccessService.assertOperable(user, source);

    QuoteStatus status = QuoteStatusSupport.normalize(source.getStatus());
    if (status != QuoteStatus.SENT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已确认报价可发起变更");
    }
    if (!Boolean.TRUE.equals(source.getCurrentVersion())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅当前生效版本可发起变更");
    }

    ensureRootDefaults(source);
    Long rootId = QuoteStatusSupport.resolveRootQuoteId(source);
    boolean openExists =
        quoteOrderRepository.existsOpenRevision(
            rootId,
            0,
            EnumSet.of(QuoteStatus.DRAFT, QuoteStatus.PENDING_APPROVAL));
    if (openExists) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该报价已有进行中的变更单，请先处理完成");
    }

    int nextRevision = quoteOrderRepository.findMaxRevisionNo(rootId) + 1;
    QuoteOrder revision = new QuoteOrder();
    revision.setQuoteNo(quoteNoGenerator.next());
    revision.setStatus(QuoteStatus.DRAFT);
    revision.setCreatedBy(user.getId());
    revision.setCreatedByName(user.getRealName());
    revision.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    revision.setCustomerId(source.getCustomerId());
    revision.setCustomerName(source.getCustomerName());
    revision.setServiceTypes(QuoteServiceTypesSupport.copyOf(source.getServiceTypes()));
    revision.setTransportMode(source.getTransportMode());
    revision.setRouteSummary(source.getRouteSummary());
    revision.setCurrency(source.getCurrency());
    revision.setBaseCurrency(source.getBaseCurrency());
    revision.setExchangeRate(source.getExchangeRate());
    revision.setValidUntil(source.getValidUntil());
    revision.setRemark(source.getRemark());
    revision.setTotalAmount(source.getTotalAmount());
    copySheetFields(source, revision);
    for (QuoteOrderLine line : source.getLines()) {
      QuoteOrderLine cloned = new QuoteOrderLine();
      cloned.setQuoteOrder(revision);
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
      revision.getLines().add(cloned);
    }
    revision.setParentQuoteId(source.getId());
    revision.setRootQuoteId(rootId);
    revision.setRevisionNo(nextRevision);
    revision.setChangeReason(request.changeReason().trim());
    revision.setCurrentVersion(false);
    revision.setSupersededByQuoteId(null);
    revision.setCostRiskActive(false);
    revision.setCostRiskReason(null);
    revision.setCostRiskAt(null);
    revision.setCreatedAt(LocalDateTime.now());
    revision.setUpdatedAt(LocalDateTime.now());

    QuoteOrder saved = quoteOrderRepository.save(revision);
    List<QuoteCostMatchItemDto> snapshots = quoteCostMatchService.listSnapshots(source.getId(), null);
    if (!snapshots.isEmpty()) {
      quoteCostMatchService.persistSnapshots(saved, snapshots);
    }

    source.setStatus(QuoteStatus.REVISING);
    // 保留成本风险标记：变更中列表优先展示「变更中」；
    // 若变更草稿被删除恢复为已确认时，仍应回到成本异常状态
    source.setUpdatedAt(LocalDateTime.now());
    quoteOrderRepository.save(source);

    String reason = request.changeReason().trim();
    // 写入变更单自身的操作日志，便于在变更单页查看原因
    String reasonSummary =
        reason.length() > 80 ? reason.substring(0, 80) + "…" : reason;
    operationLogService.recordSuccess(
        user,
        "quote",
        OperationAction.CREATE,
        "QuoteOrder",
        String.valueOf(saved.getId()),
        "变更创建 "
            + saved.getQuoteNo()
            + "（来源 "
            + source.getQuoteNo()
            + "），原因："
            + reasonSummary,
        "POST",
        "/quotes/" + saved.getId() + "/revise-created",
        null,
        null);

    return quoteQueryService.getById(user, saved.getId());
  }

  public boolean isEditable(QuoteStatus status) {
    return QuoteStatusSupport.isEditable(status);
  }

  private void supersedePreviousVersions(QuoteOrder approvedRevision) {
    Long rootId = QuoteStatusSupport.resolveRootQuoteId(approvedRevision);
    if (rootId == null) {
      return;
    }
    List<QuoteOrder> family =
        quoteOrderRepository.findByRootQuoteIdOrderByRevisionNoAscIdAsc(rootId);
    // 兼容 root_quote_id 尚未回填的旧根单
    if (family.isEmpty()) {
      quoteOrderRepository
          .findById(rootId)
          .ifPresent(family::add);
    }
    for (QuoteOrder member : family) {
      if (member.getId().equals(approvedRevision.getId())) {
        continue;
      }
      QuoteStatus memberStatus = QuoteStatusSupport.normalize(member.getStatus());
      if (memberStatus == QuoteStatus.SUPERSEDED) {
        member.setCurrentVersion(false);
        continue;
      }
      if (memberStatus == QuoteStatus.SENT
          || memberStatus == QuoteStatus.REVISING
          || Boolean.TRUE.equals(member.getCurrentVersion())) {
        member.setStatus(QuoteStatus.SUPERSEDED);
        member.setCurrentVersion(false);
        member.setSupersededByQuoteId(approvedRevision.getId());
        quoteCostRiskService.clearRisk(null, member);
        member.setUpdatedAt(LocalDateTime.now());
        quoteOrderRepository.save(member);
      } else if (Boolean.TRUE.equals(member.getCurrentVersion())) {
        member.setCurrentVersion(false);
        quoteOrderRepository.save(member);
      }
    }
  }

  private void ensureRootDefaults(QuoteOrder order) {
    boolean dirty = false;
    if (order.getRootQuoteId() == null && order.getId() != null) {
      order.setRootQuoteId(order.getId());
      dirty = true;
    }
    if (order.getRevisionNo() == null) {
      order.setRevisionNo(0);
      dirty = true;
    }
    if (order.getCurrentVersion() == null) {
      order.setCurrentVersion(true);
      dirty = true;
    }
    if (dirty) {
      quoteOrderRepository.save(order);
    }
  }

  private String resolveNodeTitle(QuoteApprovalAction action, Long quoteOrderId) {
    List<QuoteApprovalSupport.FlowNode> nodes = cycleFlow(quoteOrderId);
    return QuoteApprovalSupport.nodeTitle(action, nodes, currentCycleApproveCount(quoteOrderId));
  }

  private void requireCurrentApprover(SysUser user, Long quoteOrderId) {
    List<QuoteApprovalSupport.FlowNode> nodes = cycleFlow(quoteOrderId);
    if (nodes.isEmpty()) {
      return;
    }
    List<QuoteApprovalLog> logs = quoteApprovalLogService.listEntities(quoteOrderId);
    if (!QuoteApprovalSupport.isCurrentApprover(user.getId(), logs, nodes)) {
      QuoteApprovalSupport.FlowNode current = QuoteApprovalSupport.currentFlowNode(logs, nodes);
      String name = current != null ? current.approverName() : "指定审批人";
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前节点待 " + name + " 审批");
    }
  }

  private List<QuoteApprovalSupport.FlowNode> currentConfigNodes() {
    return approvalConfigService.findQuoteFlowSteps().stream()
        .map(step -> new QuoteApprovalSupport.FlowNode(step.approverId(), step.approverName()))
        .toList();
  }

  private List<QuoteApprovalSupport.FlowNode> cycleFlow(Long quoteOrderId) {
    List<QuoteApprovalLog> cycle =
        QuoteApprovalSupport.currentCycle(quoteApprovalLogService.listEntities(quoteOrderId));
    return QuoteApprovalSupport.resolveCycleFlow(cycle, currentConfigNodes());
  }

  private int currentCycleApproveCount(Long quoteOrderId) {
    List<QuoteApprovalLog> cycle =
        QuoteApprovalSupport.currentCycle(quoteApprovalLogService.listEntities(quoteOrderId));
    return QuoteApprovalSupport.countCycleApproves(cycle);
  }

  private QuoteOrder requireOrder(SysUser user, Long id) {
    QuoteOrder order = quoteAccessService.requireReadable(user, id);
    if (order.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "报价单已删除");
    }
    if (QuoteStatusSupport.applyExpiredAsVoided(order)) {
      order.setUpdatedAt(LocalDateTime.now());
      quoteOrderRepository.save(order);
    }
    return order;
  }

  private QuoteOrder requireOperableOrder(SysUser user, Long id) {
    QuoteOrder order = requireOrder(user, id);
    quoteAccessService.assertOperable(user, order);
    return order;
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
    target.setFumigationPoint(source.getFumigationPoint());
    target.setFumigationEnabled(source.getFumigationEnabled());
    target.setOakType(source.getOakType());
    target.setDocUsd(source.getDocUsd());
    target.setCargoInsurancePremium(source.getCargoInsurancePremium());
    target.setCargoAgentFee(source.getCargoAgentFee());
    target.setCargoMaxWeightTon(source.getCargoMaxWeightTon());
    target.setCifAmount(source.getCifAmount());
    target.setSheetRemark(source.getSheetRemark());
  }
}
