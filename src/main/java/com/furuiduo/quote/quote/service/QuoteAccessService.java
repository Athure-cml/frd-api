package com.furuiduo.quote.quote.service;

import java.util.Set;

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
    if (scope == DataScope.ASSIGNED) {
      // 尚未认领操作人的报价单对「指定数据」范围可见，便于报价员接手
      if (order.getCreatedBy() == null) {
        return;
      }
      Set<Long> assigned = permissionService.resolveAssignedDataUserIds(user);
      if (assigned.contains(order.getCreatedBy())) {
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
    // 尚未认领操作人的报价单（询价递交生成）：可读即可操作，首次操作时写入操作人
    if (order.getCreatedBy() == null) {
      return true;
    }
    return isSuperAdmin(user) || user.getId().equals(order.getCreatedBy());
  }

  /**
   * 询价递交生成的报价单操作人初始为空；首次保存/确认等操作时认领为当前用户。
   * 仅改内存实体，由调用方随后 save。
   */
  public void claimOperatorIfAbsent(QuoteOrder order, SysUser user) {
    if (order.getCreatedBy() != null) {
      return;
    }
    order.setCreatedBy(user.getId());
    order.setCreatedByName(user.getRealName());
    if (order.getDeptId() == null && user.getDepartment() != null) {
      order.setDeptId(user.getDepartment().getId());
    }
  }

  /** 单据生命周期操作：操作人、超级管理员，或尚未认领的待报价单 */
  public void assertOperable(SysUser user, QuoteOrder order) {
    assertReadable(user, order);
    if (!canOperate(user, order)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅操作人或超级管理员可操作");
    }
  }
}
