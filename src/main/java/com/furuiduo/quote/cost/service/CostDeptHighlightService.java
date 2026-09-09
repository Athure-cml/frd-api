package com.furuiduo.quote.cost.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.RequestIds;
import com.furuiduo.quote.cost.dto.CostHighlightBatchRequest;
import com.furuiduo.quote.cost.dto.CostHighlightView;
import com.furuiduo.quote.cost.dto.FreightCostResponse;
import com.furuiduo.quote.cost.dto.FumigationCostResponse;
import com.furuiduo.quote.cost.dto.RoadCostResponse;
import com.furuiduo.quote.cost.entity.CostDeptHighlight;
import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.cost.repository.CostDeptHighlightRepository;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.repository.CostSeaRepository;
import com.furuiduo.quote.cost.support.CostHighlightAccessService;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysDepartmentRepository;

@Service
public class CostDeptHighlightService {

  private static final Pattern HEX_COLOR =
      Pattern.compile("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$");

  private final CostDeptHighlightRepository highlightRepository;
  private final CostHighlightAccessService accessService;
  private final SysDepartmentRepository departmentRepository;
  private final CostRoadRepository roadRepository;
  private final CostSeaRepository seaRepository;
  private final CostFumigationRepository fumigationRepository;

  public CostDeptHighlightService(
      CostDeptHighlightRepository highlightRepository,
      CostHighlightAccessService accessService,
      SysDepartmentRepository departmentRepository,
      CostRoadRepository roadRepository,
      CostSeaRepository seaRepository,
      CostFumigationRepository fumigationRepository) {
    this.highlightRepository = highlightRepository;
    this.accessService = accessService;
    this.departmentRepository = departmentRepository;
    this.roadRepository = roadRepository;
    this.seaRepository = seaRepository;
    this.fumigationRepository = fumigationRepository;
  }

  @Transactional
  public int mark(CostHighlightMode mode, SysUser user, CostHighlightBatchRequest request) {
    List<Long> ids = RequestIds.distinctPositive(request.ids());
    if (ids.isEmpty()) {
      return 0;
    }
    assertCostsExist(mode, ids);
    String color = normalizeColor(request.color());
    String remark = trimRemark(request.remark());
    Long deptId = accessService.requireDeptId(user);
    var department =
        departmentRepository
            .findById(deptId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "部门不存在"));
    int updated = 0;
    for (Long costId : ids) {
      CostDeptHighlight highlight =
          highlightRepository
              .findByCostModeAndDepartmentIdAndCostId(mode, deptId, costId)
              .orElseGet(
                  () -> {
                    CostDeptHighlight created = new CostDeptHighlight();
                    created.setCostMode(mode);
                    created.setCostId(costId);
                    created.setDepartment(department);
                    created.setMarkedBy(user);
                    return created;
                  });
      highlight.setColor(color);
      highlight.setRemark(remark);
      highlight.setMarkedBy(user);
      highlight.setAdminShared(accessService.isAdminRole(user));
      highlightRepository.save(highlight);
      updated++;
    }
    return updated;
  }

  @Transactional
  public int unmark(CostHighlightMode mode, SysUser user, List<Long> rawIds) {
    List<Long> ids = RequestIds.distinctPositive(rawIds);
    if (ids.isEmpty()) {
      return 0;
    }
    if (accessService.isGlobalViewer(user)) {
      return highlightRepository.deleteAllByModeAndCostIds(mode, ids);
    }
    Long deptId = accessService.requireDeptId(user);
    highlightRepository.deleteByCostModeAndDepartmentIdAndCostIdIn(mode, deptId, ids);
    return ids.size();
  }

  public Set<Long> visibleHighlightedCostIds(CostHighlightMode mode, SysUser user) {
    if (accessService.isGlobalViewer(user)) {
      return new LinkedHashSet<>(
          highlightRepository.findCostIdsByModeAndDeptIds(
              mode, departmentRepository.findAll().stream().map(d -> d.getId()).toList()));
    }
    Long deptId = accessService.requireDeptId(user);
    return new LinkedHashSet<>(
        highlightRepository.findVisibleCostIdsForBusinessUser(mode, deptId));
  }

  public Map<Long, CostHighlightView> buildViewMap(
      CostHighlightMode mode, SysUser user, List<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return Map.of();
    }
    List<CostDeptHighlight> highlights =
        highlightRepository.findByCostModeAndCostIdIn(mode, costIds);
    Map<Long, List<CostDeptHighlight>> grouped =
        highlights.stream().collect(Collectors.groupingBy(CostDeptHighlight::getCostId));

    Map<Long, CostHighlightView> views = new HashMap<>();
    boolean global = accessService.isGlobalViewer(user);
    Long viewerDeptId =
        user.getDepartment() != null ? user.getDepartment().getId() : null;

    for (Long costId : costIds) {
      List<CostDeptHighlight> rows = grouped.getOrDefault(costId, List.of());
      resolveHighlightView(rows, viewerDeptId, global)
          .ifPresent(view -> views.put(costId, view));
    }
    return views;
  }

  /**
   * 展示优先级：本部门标记 > 管理员共享标记 >（仅全局用户）其他部门最新标记。
   * 管理员与业务员同时标记同一行时，各自看到本部门颜色。
   */
  private Optional<CostHighlightView> resolveHighlightView(
      List<CostDeptHighlight> rows, Long viewerDeptId, boolean global) {
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    Optional<CostDeptHighlight> own =
        viewerDeptId == null
            ? Optional.empty()
            : rows.stream()
                .filter(row -> row.getDepartment().getId().equals(viewerDeptId))
                .findFirst();
    if (own.isPresent()) {
      CostDeptHighlight row = own.get();
      return Optional.of(
          new CostHighlightView(
              row.getColor(),
              row.getRemark(),
              row.getDepartment().getName(),
              global ? rows.size() : 1,
              true));
    }
    List<CostDeptHighlight> adminShared =
        rows.stream()
            .filter(CostDeptHighlight::isAdminShared)
            .sorted(Comparator.comparing(CostDeptHighlight::getUpdatedAt).reversed())
            .toList();
    if (!adminShared.isEmpty()) {
      CostDeptHighlight row = adminShared.getFirst();
      return Optional.of(
          new CostHighlightView(
              row.getColor(),
              row.getRemark(),
              row.getDepartment().getName(),
              global ? rows.size() : 1,
              false));
    }
    if (!global) {
      return Optional.empty();
    }
    List<CostDeptHighlight> sorted =
        rows.stream()
            .sorted(Comparator.comparing(CostDeptHighlight::getUpdatedAt).reversed())
            .toList();
    CostDeptHighlight latest = sorted.getFirst();
    return Optional.of(
        new CostHighlightView(
            latest.getColor(),
            latest.getRemark(),
            latest.getDepartment().getName(),
            sorted.size(),
            false));
  }

  public PageResult<RoadCostResponse> enrichRoadPage(
      SysUser user, PageResult<RoadCostResponse> page) {
    return enrichPage(
        CostHighlightMode.road,
        user,
        page,
        (item, view) -> view == null ? item : item.withHighlight(view));
  }

  public PageResult<FreightCostResponse> enrichSeaPage(
      SysUser user, PageResult<FreightCostResponse> page) {
    return enrichPage(
        CostHighlightMode.sea,
        user,
        page,
        (item, view) -> view == null ? item : item.withHighlight(view));
  }

  public PageResult<FumigationCostResponse> enrichFumigationPage(
      SysUser user, PageResult<FumigationCostResponse> page) {
    return enrichPage(
        CostHighlightMode.fumigation,
        user,
        page,
        (item, view) -> view == null ? item : item.withHighlight(view));
  }

  private <T> PageResult<T> enrichPage(
      CostHighlightMode mode,
      SysUser user,
      PageResult<T> page,
      java.util.function.BiFunction<T, CostHighlightView, T> mapper) {
    if (page.items() == null || page.items().isEmpty()) {
      return page;
    }
    List<Long> ids = extractIds(page.items());
    Map<Long, CostHighlightView> viewMap = buildViewMap(mode, user, ids);
    List<T> enriched = new ArrayList<>();
    for (T item : page.items()) {
      Long id = readId(item);
      enriched.add(mapper.apply(item, viewMap.get(id)));
    }
    return new PageResult<>(enriched, page.total());
  }

  private List<Long> extractIds(List<?> items) {
    return items.stream().map(this::readId).filter(id -> id != null && id > 0).toList();
  }

  private Long readId(Object item) {
    if (item instanceof com.furuiduo.quote.cost.dto.RoadCostResponse road) {
      return road.id();
    }
    if (item instanceof com.furuiduo.quote.cost.dto.FreightCostResponse sea) {
      return sea.id();
    }
    if (item instanceof com.furuiduo.quote.cost.dto.FumigationCostResponse fumigation) {
      return fumigation.id();
    }
    return null;
  }

  private void assertCostsExist(CostHighlightMode mode, List<Long> ids) {
    long found =
        switch (mode) {
          case road -> roadRepository.countByIdIn(ids);
          case sea -> seaRepository.countByIdIn(ids);
          case fumigation -> fumigationRepository.countByIdIn(ids);
        };
    if (found != ids.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "部分成本数据不存在或已删除");
    }
  }

  private String normalizeColor(String raw) {
    String color = raw == null ? "" : raw.trim();
    if (!HEX_COLOR.matcher(color).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "颜色格式无效，请使用 #RRGGBB");
    }
    if (color.length() == 4) {
      return "#"
          + color.charAt(1)
          + color.charAt(1)
          + color.charAt(2)
          + color.charAt(2)
          + color.charAt(3)
          + color.charAt(3);
    }
    return color.toUpperCase();
  }

  private String trimRemark(String raw) {
    if (raw == null) {
      return null;
    }
    String remark = raw.trim();
    return remark.isEmpty() ? null : remark;
  }
}
