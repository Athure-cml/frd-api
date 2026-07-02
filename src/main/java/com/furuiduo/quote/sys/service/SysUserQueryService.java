package com.furuiduo.quote.sys.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.sys.dto.UserListItem;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysUserRepository;

@Service
public class SysUserQueryService {

  private final SysUserRepository userRepository;
  private final PermissionService permissionService;

  public SysUserQueryService(
      SysUserRepository userRepository, PermissionService permissionService) {
    this.userRepository = userRepository;
    this.permissionService = permissionService;
  }

  public PageResult<UserListItem> listUsers(
      int page, int pageSize, String username, Long deptId, Integer status) {
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 200);

    List<SysUser> filtered =
        userRepository.findAllWithDetails().stream()
            .filter(user -> matchesUsername(user, username))
            .filter(user -> deptId == null || user.getDepartment().getId().equals(deptId))
            .filter(user -> status == null || user.getStatus().equals(status))
            .sorted(Comparator.comparing(SysUser::getId))
            .toList();

    int total = filtered.size();
    int fromIndex = (safePage - 1) * safePageSize;
    if (fromIndex >= total) {
      return new PageResult<>(List.of(), total);
    }
    int toIndex = Math.min(fromIndex + safePageSize, total);
    List<UserListItem> items =
        filtered.subList(fromIndex, toIndex).stream()
            .map(user -> UserListItem.from(user, permissionService))
            .toList();
    return new PageResult<>(items, total);
  }

  private boolean matchesUsername(SysUser user, String username) {
    if (username == null || username.isBlank()) {
      return true;
    }
    String keyword = username.trim().toLowerCase(Locale.ROOT);
    return user.getUsername().toLowerCase(Locale.ROOT).contains(keyword)
        || user.getRealName().toLowerCase(Locale.ROOT).contains(keyword);
  }
}
