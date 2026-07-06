package com.furuiduo.quote.cost.seed;

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
import com.furuiduo.quote.cost.support.CostTemplateLayoutTools;
import com.furuiduo.quote.cost.support.CostTemplateLayouts;

/** 同步内置默认模板列顺序，并清除历史 layout 中的必填标记。 */
@Component
@Order(17)
public class CostTemplateLayoutMigrator implements ApplicationRunner {

  private final CostGridTemplateRepository repository;

  public CostTemplateLayoutMigrator(CostGridTemplateRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    migrateBuiltin("road", "road_default", CostTemplateLayouts.roadDefault());
    migrateBuiltin("sea", "sea_default", CostTemplateLayouts.seaDefault());
    migrateBuiltin("fumigation", "fumigation_default", CostTemplateLayouts.fumigationDefault());

    for (CostGridTemplate template : repository.findAll()) {
      CostTableTemplateLayout current = template.getLayout();
      CostTableTemplateLayout normalized =
          CostTemplateLayoutTools.normalize(current, template.getMode());
      if (!Objects.equals(current, normalized)) {
        template.setLayout(normalized);
        template.touch();
        repository.save(template);
      }
    }
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
    return !Objects.equals(
        CostFieldCatalog.resolveFieldKeys(current), CostFieldCatalog.resolveFieldKeys(target));
  }
}
