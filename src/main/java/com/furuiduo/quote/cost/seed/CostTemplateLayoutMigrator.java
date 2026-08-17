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
import com.furuiduo.quote.cost.support.CostTemplateLayoutTools;
import com.furuiduo.quote.cost.support.CostTemplateLayouts;

/**
 * 启动时规范化模板 layout。
 *
 * <p>内置默认模板仅在 layout 为空时回填代码默认；已有配置一律保留，避免重启冲掉用户修改。
 */
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
    fillBuiltinIfEmpty("road", "road_default", CostTemplateLayouts.roadDefault());
    fillBuiltinIfEmpty("sea", "sea_default", CostTemplateLayouts.seaDefault());
    fillBuiltinIfEmpty(
        "fumigation", "fumigation_default", CostTemplateLayouts.fumigationDefault());

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

  private void fillBuiltinIfEmpty(String mode, String code, CostTableTemplateLayout target) {
    repository
        .findByModeAndCode(mode, code)
        .filter(template -> template.getLayout() == null)
        .ifPresent(
            template -> {
              template.setLayout(target);
              template.touch();
              repository.save(template);
            });
  }
}
