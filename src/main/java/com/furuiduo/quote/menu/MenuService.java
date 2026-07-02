package com.furuiduo.quote.menu;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.furuiduo.quote.menu.dto.MenuRouteDto;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

@Service
public class MenuService {

  private final PermissionService permissionService;

  public MenuService(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  public List<MenuRouteDto> getMenusForUser(SysUser user) {
    return filterRoutes(user, MenuRegistry.all());
  }

  private List<MenuRouteDto> filterRoutes(SysUser user, List<MenuRouteDto> routes) {
    List<MenuRouteDto> visible = new ArrayList<>();
    for (MenuRouteDto route : routes) {
      MenuRouteDto filtered = filterRoute(user, route);
      if (filtered != null) {
        visible.add(filtered);
      }
    }
    return visible;
  }

  private MenuRouteDto filterRoute(SysUser user, MenuRouteDto route) {
    boolean hadChildren = !route.getChildren().isEmpty();
    List<MenuRouteDto> children = filterRoutes(user, route.getChildren());
    boolean selfVisible = isVisible(user, route);

    if (hadChildren && children.isEmpty()) {
      return null;
    }
    if (!hadChildren && !selfVisible) {
      return null;
    }

    route.getChildren().clear();
    route.getChildren().addAll(children);
    return route;
  }

  private boolean isVisible(SysUser user, MenuRouteDto route) {
    if (!route.getRequiredRoles().isEmpty()) {
      List<String> roleCodes = permissionService.getRoleCodes(user);
      boolean roleMatched =
          route.getRequiredRoles().stream().anyMatch(roleCodes::contains);
      if (!roleMatched) {
        return false;
      }
    }
    if (!route.getRequiredPermissions().isEmpty()) {
      return route.getRequiredPermissions().stream()
          .anyMatch(code -> permissionService.hasPermission(user, code));
    }
    return true;
  }
}
