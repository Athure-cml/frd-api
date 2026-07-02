package com.furuiduo.quote.menu.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuRouteDto {

  private String name;
  private String path;
  private String component;
  private String redirect;
  private Map<String, Object> meta = new LinkedHashMap<>();
  private List<MenuRouteDto> children = new ArrayList<>();
  private List<String> requiredPermissions = new ArrayList<>();
  private List<String> requiredRoles = new ArrayList<>();

  public static MenuRouteDto of(String name, String path) {
    MenuRouteDto route = new MenuRouteDto();
    route.name = name;
    route.path = path;
    return route;
  }

  public MenuRouteDto component(String component) {
    this.component = component;
    return this;
  }

  public MenuRouteDto redirect(String redirect) {
    this.redirect = redirect;
    return this;
  }

  public MenuRouteDto meta(String key, Object value) {
    this.meta.put(key, value);
    return this;
  }

  public MenuRouteDto child(MenuRouteDto child) {
    this.children.add(child);
    return this;
  }

  public MenuRouteDto requirePermission(String... permissions) {
    this.requiredPermissions = List.of(permissions);
    return this;
  }

  public MenuRouteDto requireAnyPermission(String... permissions) {
    this.requiredPermissions = List.of(permissions);
    return this;
  }

  public MenuRouteDto requireRole(String... roles) {
    this.requiredRoles = List.of(roles);
    return this;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public String getComponent() {
    return component;
  }

  public String getRedirect() {
    return redirect;
  }

  public Map<String, Object> getMeta() {
    return meta;
  }

  public List<MenuRouteDto> getChildren() {
    return children;
  }

  @JsonIgnore
  public List<String> getRequiredPermissions() {
    return requiredPermissions;
  }

  @JsonIgnore
  public List<String> getRequiredRoles() {
    return requiredRoles;
  }
}
