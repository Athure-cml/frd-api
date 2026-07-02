package com.furuiduo.quote.cost.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.cost.entity.CostGridTemplate;
import com.furuiduo.quote.cost.repository.CostGridTemplateRepository;
import com.furuiduo.quote.cost.support.CostTemplateLayouts;

@Component
@Order(15)
public class CostGridTemplateSeeder implements ApplicationRunner {

  private final CostGridTemplateRepository repository;

  public CostGridTemplateSeeder(CostGridTemplateRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    seedIfEmpty("road", "road_default", CostTemplateLayouts.roadDefault());
    seedIfEmpty("sea", "sea_default", CostTemplateLayouts.seaDefault());
    seedIfEmpty("fumigation", "fumigation_default", CostTemplateLayouts.fumigationDefault());
  }

  private void seedIfEmpty(
      String mode, String code, com.furuiduo.quote.cost.dto.CostTableTemplateLayout layout) {
    if (repository.countByMode(mode) > 0) {
      return;
    }
    CostGridTemplate template = new CostGridTemplate();
    template.setMode(mode);
    template.setCode(code);
    template.setName("page.costLibrary.template.defaultName");
    template.setDefaultTemplate(true);
    template.setLayout(layout);
    template.touch();
    repository.save(template);
  }
}
