package com.furuiduo.quote.cost.seed;

import java.util.Objects;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;
import com.furuiduo.quote.cost.entity.CostGridTemplate;

import static com.furuiduo.quote.cost.entity.CostGridTemplate.LIBRARY_COST;
import static com.furuiduo.quote.cost.entity.CostGridTemplate.LIBRARY_QUOTE;
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
    fillBuiltinIfEmpty(LIBRARY_COST, "road", "road_default", CostTemplateLayouts.roadDefault());
    fillBuiltinIfEmpty(LIBRARY_COST, "sea", "sea_default", CostTemplateLayouts.seaDefault());
    fillBuiltinIfEmpty(
        LIBRARY_COST, "fumigation", "fumigation_v2", CostTemplateLayouts.fumigationDefault());
    fillBuiltinIfEmpty(LIBRARY_QUOTE, "road", "q_road_default", CostTemplateLayouts.roadDefault());
    fillBuiltinIfEmpty(LIBRARY_QUOTE, "sea", "q_sea_default", CostTemplateLayouts.seaDefault());
    fillBuiltinIfEmpty(
        LIBRARY_QUOTE, "fumigation", "q_fumigation_v2", CostTemplateLayouts.fumigationDefault());

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

  private void fillBuiltinIfEmpty(
      String library, String mode, String code, CostTableTemplateLayout target) {
    repository
        .findByLibraryAndModeAndCode(library, mode, code)
        .filter(template -> template.getLayout() == null)
        .ifPresent(
            template -> {
              template.setLayout(target);
              template.touch();
              repository.save(template);
            });
  }
}
