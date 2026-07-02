package com.furuiduo.quote.cost.seed;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;
import com.furuiduo.quote.cost.entity.CostGridTemplate;
import com.furuiduo.quote.cost.repository.CostGridTemplateRepository;
import com.furuiduo.quote.cost.support.CostFieldCatalog;
import com.furuiduo.quote.cost.support.CostTemplateLayouts;

/** 将内置海运/熏蒸默认模板的列配置与业务 Excel 表头对齐。 */
@Component
@Order(16)
public class FreightTemplateLayoutMigrator implements ApplicationRunner {

  private final CostGridTemplateRepository repository;

  public FreightTemplateLayoutMigrator(CostGridTemplateRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    migrateBuiltin("sea", "sea_default", CostTemplateLayouts.seaDefault());
    migrateBuiltin("fumigation", "fumigation_default", CostTemplateLayouts.fumigationDefault());
  }

  private void migrateBuiltin(String mode, String code, CostTableTemplateLayout target) {
    repository
        .findByModeAndCode(mode, code)
        .filter(template -> shouldUpgrade(template.getLayout(), target))
        .ifPresent(
            template -> {
              template.setLayout(target);
              template.touch();
              repository.save(template);
            });
  }

  private boolean shouldUpgrade(
      CostTableTemplateLayout current, CostTableTemplateLayout target) {
    if (current == null) {
      return true;
    }
    List<String> currentFields = CostFieldCatalog.resolveFieldKeys(current);
    List<String> targetFields = CostFieldCatalog.resolveFieldKeys(target);
    if (currentFields.isEmpty()) {
      return true;
    }
    return !Objects.equals(currentFields, targetFields);
  }
}
