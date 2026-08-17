package com.furuiduo.quote.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PartyReorderSupport {

  private PartyReorderSupport() {}

  /**
   * 将当前页拖拽后的顺序写回同「置顶分组」内的 sort_order。
   *
   * @param orderedIds 拖拽后的 ID 顺序（同页）
   * @param loadById 按 ID 加载实体
   * @param idGetter 读取实体 ID
   * @param pinnedGetter 是否置顶
   * @param loadGroup 加载同一置顶分组（及供应商同分类）的全部实体，已按当前排序
   * @param setSortOrder 写入 sort_order
   * @param saveAll 批量保存
   */
  public static <T> void reorder(
      List<Long> orderedIds,
      Function<Long, T> loadById,
      Function<T, Long> idGetter,
      Function<T, Boolean> pinnedGetter,
      Function<Boolean, List<T>> loadGroup,
      BiConsumer<T, Integer> setSortOrder,
      Consumer<List<T>> saveAll) {
    if (orderedIds == null || orderedIds.isEmpty()) {
      return;
    }
    LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
    for (Long id : orderedIds) {
      if (id != null) {
        uniqueIds.add(id);
      }
    }
    if (uniqueIds.isEmpty()) {
      return;
    }

    List<T> ordered = new ArrayList<>(uniqueIds.size());
    Boolean pinned = null;
    for (Long id : uniqueIds) {
      T entity = loadById.apply(id);
      if (entity == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在: " + id);
      }
      boolean isPinned = Boolean.TRUE.equals(pinnedGetter.apply(entity));
      if (pinned == null) {
        pinned = isPinned;
      } else if (!Objects.equals(pinned, isPinned)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能跨置顶分组拖拽排序");
      }
      ordered.add(entity);
    }

    List<T> group = loadGroup.apply(pinned);
    Set<Long> moving = new HashSet<>(uniqueIds);
    Map<Long, T> byId = new HashMap<>();
    for (T entity : group) {
      byId.put(idGetter.apply(entity), entity);
    }
    for (T entity : ordered) {
      byId.putIfAbsent(idGetter.apply(entity), entity);
    }

    int insertAt = 0;
    for (int i = 0; i < group.size(); i++) {
      if (moving.contains(idGetter.apply(group.get(i)))) {
        insertAt = i;
        break;
      }
    }

    List<T> result = new ArrayList<>(group.size());
    boolean inserted = false;
    for (int i = 0; i < group.size(); i++) {
      T current = group.get(i);
      Long currentId = idGetter.apply(current);
      if (moving.contains(currentId)) {
        if (!inserted) {
          for (T item : ordered) {
            result.add(byId.get(idGetter.apply(item)));
          }
          inserted = true;
        }
        continue;
      }
      if (!inserted && i == insertAt) {
        for (T item : ordered) {
          result.add(byId.get(idGetter.apply(item)));
        }
        inserted = true;
      }
      result.add(current);
    }
    if (!inserted) {
      result.addAll(ordered);
    }

    for (int i = 0; i < result.size(); i++) {
      setSortOrder.accept(result.get(i), i);
    }
    saveAll.accept(result);
  }
}
