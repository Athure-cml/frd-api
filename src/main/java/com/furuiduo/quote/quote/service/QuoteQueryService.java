package com.furuiduo.quote.quote.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.quote.dto.QuoteDetailResponse;
import com.furuiduo.quote.quote.dto.QuoteListItem;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.entity.QuoteTransportMode;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.sys.entity.DataScope;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.OperationLogService;
import com.furuiduo.quote.sys.service.PermissionService;

@Service
public class QuoteQueryService {

  private final QuoteOrderRepository quoteOrderRepository;
  private final PermissionService permissionService;
  private final QuoteAccessService quoteAccessService;
  private final QuoteCostMatchService quoteCostMatchService;
  private final QuoteFollowUpService quoteFollowUpService;
  private final OperationLogService operationLogService;

  public QuoteQueryService(
      QuoteOrderRepository quoteOrderRepository,
      PermissionService permissionService,
      QuoteAccessService quoteAccessService,
      QuoteCostMatchService quoteCostMatchService,
      QuoteFollowUpService quoteFollowUpService,
      OperationLogService operationLogService) {
    this.quoteOrderRepository = quoteOrderRepository;
    this.permissionService = permissionService;
    this.quoteAccessService = quoteAccessService;
    this.quoteCostMatchService = quoteCostMatchService;
    this.quoteFollowUpService = quoteFollowUpService;
    this.operationLogService = operationLogService;
  }

  public PageResult<QuoteListItem> list(
      SysUser user,
      int page,
      int pageSize,
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
    DataScope scope = permissionService.getEffectiveDataScope(user);
    Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;

    var pageable =
        PageRequest.of(
            Math.max(page - 1, 0),
            Math.max(pageSize, 1),
            Sort.by(Sort.Direction.DESC, "updatedAt"));

    var result =
        quoteOrderRepository.search(
            SearchText.orEmpty(quoteNo),
            SearchText.orEmpty(customerName),
            parseTransportMode(transportMode),
            parseStatus(status),
            SearchText.orEmpty(zipCode),
            SearchText.orEmpty(city),
            SearchText.orEmpty(state),
            SearchText.orEmpty(por),
            SearchText.orEmpty(pol),
            SearchText.orEmpty(pod),
            SearchText.orEmpty(ssl),
            SearchText.orEmpty(followUpByName),
            scope == DataScope.ALL,
            scope == DataScope.DEPT,
            scope == DataScope.SELF,
            deptId,
            user.getId(),
            pageable);

    return new PageResult<>(
        result.getContent().stream().map(QuoteListItem::from).toList(), result.getTotalElements());
  }

  /** 导出用：按列表同款筛选条件返回实体（无筛选即权限范围内全部）。 */
  public List<QuoteOrder> findOrdersForExport(
      SysUser user,
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
    DataScope scope = permissionService.getEffectiveDataScope(user);
    Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
    var pageable = Pageable.unpaged(Sort.by(Sort.Direction.DESC, "updatedAt"));
    return quoteOrderRepository
        .search(
            SearchText.orEmpty(quoteNo),
            SearchText.orEmpty(customerName),
            parseTransportMode(transportMode),
            parseStatus(status),
            SearchText.orEmpty(zipCode),
            SearchText.orEmpty(city),
            SearchText.orEmpty(state),
            SearchText.orEmpty(por),
            SearchText.orEmpty(pol),
            SearchText.orEmpty(pod),
            SearchText.orEmpty(ssl),
            SearchText.orEmpty(followUpByName),
            scope == DataScope.ALL,
            scope == DataScope.DEPT,
            scope == DataScope.SELF,
            deptId,
            user.getId(),
            pageable)
        .getContent();
  }

  public QuoteDetailResponse getById(SysUser user, Long id) {
    QuoteOrder order =
        quoteOrderRepository
            .findWithLinesById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报价单不存在"));
    quoteAccessService.assertReadable(user, order);
    return QuoteDetailResponse.from(
        order,
        quoteCostMatchService.listSnapshots(id, null),
        quoteFollowUpService.list(user, id));
  }

  public PageResult<com.furuiduo.quote.sys.dto.OperationLogResponse> listOperationLogs(
      SysUser user, Long quoteId, int page, int pageSize) {
    quoteAccessService.requireReadable(user, quoteId);
    return operationLogService.listForQuote(quoteId, page, pageSize);
  }

  private QuoteTransportMode parseTransportMode(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return QuoteTransportMode.valueOf(value);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的运输方式");
    }
  }

  private QuoteStatus parseStatus(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return QuoteStatus.valueOf(value);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的报价状态");
    }
  }

  void assertReadable(SysUser user, QuoteOrder order) {
    quoteAccessService.assertReadable(user, order);
  }
}
