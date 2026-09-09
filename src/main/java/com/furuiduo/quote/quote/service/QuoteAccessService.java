package com.furuiduo.quote.quote.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.sys.entity.DataScope;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

@Service
public class QuoteAccessService {

  private final QuoteOrderRepository quoteOrderRepository;
  private final PermissionService permissionService;

  public QuoteAccessService(
      QuoteOrderRepository quoteOrderRepository, PermissionService permissionService) {
    this.quoteOrderRepository = quoteOrderRepository;
    this.permissionService = permissionService;
  }

  public QuoteOrder requireReadable(SysUser user, Long id) {
    QuoteOrder order =
        quoteOrderRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报价单不存在"));
    assertReadable(user, order);
    return order;
  }

  public void assertReadable(SysUser user, QuoteOrder order) {
    DataScope scope = permissionService.getEffectiveDataScope(user);
    if (scope == DataScope.ALL) {
      return;
    }
    if (scope == DataScope.DEPT) {
      Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
      if (deptId != null && deptId.equals(order.getDeptId())) {
        return;
      }
    }
    if (scope == DataScope.SELF && user.getId().equals(order.getCreatedBy())) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看该报价单");
  }

  public boolean isSuperAdmin(SysUser user) {
    return permissionService.getRoleCodes(user).contains("super_admin");
  }

  public boolean canOperate(SysUser user, QuoteOrder order) {
    return isSuperAdmin(user) || user.getId().equals(order.getCreatedBy());
  }

  /** 单据生命周期操作：仅创建人或超级管理员 */
  public void assertOperable(SysUser user, QuoteOrder order) {
    assertReadable(user, order);
    if (!canOperate(user, order)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅创建人或超级管理员可操作");
    }
  }
}
