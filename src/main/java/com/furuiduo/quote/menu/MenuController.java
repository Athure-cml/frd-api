package com.furuiduo.quote.menu;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.menu.dto.MenuRouteDto;
import com.furuiduo.quote.sys.entity.SysUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "菜单", description = "动态侧边栏菜单")
@RestController
@RequestMapping("/menu")
public class MenuController {

  private final AuthService authService;
  private final MenuService menuService;

  public MenuController(AuthService authService, MenuService menuService) {
    this.authService = authService;
    this.menuService = menuService;
  }

  @Operation(
      summary = "获取当前用户菜单树",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @GetMapping("/all")
  public ApiResponse<List<MenuRouteDto>> all(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    SysUser user = authService.requireUser(authorization);
    return ApiResponse.ok(menuService.getMenusForUser(user));
  }
}
