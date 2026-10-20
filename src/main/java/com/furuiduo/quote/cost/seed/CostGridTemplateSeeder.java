package com.furuiduo.quote.cost.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.cost.entity.CostGridTemplate;

import static com.furuiduo.quote.cost.entity.CostGridTemplate.LIBRARY_COST;
import static com.furuiduo.quote.cost.entity.CostGridTemplate.LIBRARY_QUOTE;
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
    repository.backfillCostLibrary();
    seedIfEmpty(LIBRARY_COST, "road", "road_default", CostTemplateLayouts.roadDefault());
    seedIfEmpty(LIBRARY_COST, "sea", "sea_default", CostTemplateLayouts.seaDefault());
    seedIfEmpty(
        LIBRARY_COST, "fumigation", "fumigation_default", CostTemplateLayouts.fumigationDefault());
    seedIfEmpty(LIBRARY_QUOTE, "road", "q_road_default", CostTemplateLayouts.roadDefault());
    seedIfEmpty(LIBRARY_QUOTE, "sea", "q_sea_default", CostTemplateLayouts.seaDefault());
    seedIfEmpty(
        LIBRARY_QUOTE,
        "fumigation",
        "q_fumigation_default",
        CostTemplateLayouts.fumigationDefault());
  }

  private void seedIfEmpty(
      String library,
      String mode,
      String code,
      com.furuiduo.quote.cost.dto.CostTableTemplateLayout layout) {
    if (repository.countByLibraryAndMode(library, mode) > 0) {
      return;
    }
    CostGridTemplate template = new CostGridTemplate();
    template.setLibrary(library);
    template.setMode(mode);
    template.setCode(code);
    template.setName(
        LIBRARY_QUOTE.equals(library)
            ? "page.quote.library.template.defaultName"
            : "page.costLibrary.template.defaultName");
    template.setDefaultTemplate(true);
    template.setLayout(layout);
    template.touch();
    repository.save(template);
  }
}
