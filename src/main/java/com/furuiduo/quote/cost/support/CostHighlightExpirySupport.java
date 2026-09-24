package com.furuiduo.quote.cost.support;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostHighlightMode;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.repository.CostSeaRepository;

@Component
public class CostHighlightExpirySupport {

  private final CostRoadRepository roadRepository;
  private final CostSeaRepository seaRepository;
  private final CostFumigationRepository fumigationRepository;

  public CostHighlightExpirySupport(
      CostRoadRepository roadRepository,
      CostSeaRepository seaRepository,
      CostFumigationRepository fumigationRepository) {
    this.roadRepository = roadRepository;
    this.seaRepository = seaRepository;
    this.fumigationRepository = fumigationRepository;
  }

  public Set<Long> findExpiredIds(CostHighlightMode mode, Collection<Long> costIds) {
    if (costIds == null || costIds.isEmpty()) {
      return Set.of();
    }
    Set<Long> expired = new HashSet<>();
    switch (mode) {
      case road ->
          roadRepository.findAllById(costIds).stream()
              .filter(this::isRoadExpired)
              .map(CostRoad::getId)
              .forEach(expired::add);
      case sea ->
          seaRepository.findAllById(costIds).stream()
              .filter(this::isSeaExpired)
              .map(CostSea::getId)
              .forEach(expired::add);
      case fumigation ->
          fumigationRepository.findAllById(costIds).stream()
              .filter(this::isFumigationExpired)
              .map(CostFumigation::getId)
              .forEach(expired::add);
    }
    return expired;
  }

  private boolean isRoadExpired(CostRoad entity) {
    return CostValidityStatus.resolveRoad(
            entity.getStatus(), entity.getExtraFields(), entity.getValidDate())
        == CostStatus.expired;
  }

  private boolean isSeaExpired(CostSea entity) {
    return CostValidityStatus.resolve(entity.getStatus(), entity.getFreightValidDate())
        == CostStatus.expired;
  }

  private boolean isFumigationExpired(CostFumigation entity) {
    return CostValidityStatus.resolve(
            entity.getStatus(), entity.getOutdoorValidity(), entity.getIndoorValidity())
        == CostStatus.expired;
  }
}
