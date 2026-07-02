package com.furuiduo.quote.cost.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.cost.entity.CostGridTemplate;

public interface CostGridTemplateRepository extends JpaRepository<CostGridTemplate, Long> {

  List<CostGridTemplate> findByModeOrderByIdAsc(String mode);

  long countByMode(String mode);

  Optional<CostGridTemplate> findByModeAndCode(String mode, String code);

  Optional<CostGridTemplate> findFirstByModeAndDefaultTemplateTrue(String mode);

  @Query("SELECT t.code FROM CostGridTemplate t WHERE t.code LIKE :prefix ORDER BY t.code DESC")
  Page<String> findCodesByPrefix(@Param("prefix") String prefix, Pageable pageable);
}
