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

  List<CostGridTemplate> findByLibraryAndModeOrderByIdAsc(String library, String mode);

  long countByLibraryAndMode(String library, String mode);

  Optional<CostGridTemplate> findByLibraryAndModeAndCode(String library, String mode, String code);

  Optional<CostGridTemplate> findFirstByLibraryAndModeAndDefaultTemplateTrue(
      String library, String mode);

  @Query(
      "SELECT t.code FROM CostGridTemplate t WHERE t.library = :library AND t.code LIKE :prefix ORDER BY t.code DESC")
  Page<String> findCodesByLibraryAndPrefix(
      @Param("library") String library, @Param("prefix") String prefix, Pageable pageable);

  @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
  @Query(
      "UPDATE CostGridTemplate t SET t.library = 'cost' WHERE t.library IS NULL OR t.library = ''")
  int backfillCostLibrary();
}
