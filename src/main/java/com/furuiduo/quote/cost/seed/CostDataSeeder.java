package com.furuiduo.quote.cost.seed;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.cost.entity.CostFumigation;
import com.furuiduo.quote.cost.entity.CostRoad;
import com.furuiduo.quote.cost.entity.CostSea;
import com.furuiduo.quote.cost.entity.CostStatus;
import com.furuiduo.quote.cost.repository.CostFumigationRepository;
import com.furuiduo.quote.cost.repository.CostRoadRepository;
import com.furuiduo.quote.cost.repository.CostSeaRepository;

@Component
@Order(20)
public class CostDataSeeder implements ApplicationRunner {

  private final CostRoadRepository roadRepository;
  private final CostSeaRepository seaRepository;
  private final CostFumigationRepository fumigationRepository;

  public CostDataSeeder(
      CostRoadRepository roadRepository,
      CostSeaRepository seaRepository,
      CostFumigationRepository fumigationRepository) {
    this.roadRepository = roadRepository;
    this.seaRepository = seaRepository;
    this.fumigationRepository = fumigationRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (roadRepository.count() == 0) {
      seedRoad();
    }
    if (seaRepository.count() == 0) {
      seedSea();
    }
    if (fumigationRepository.count() == 0) {
      seedFumigation();
    }
  }

  private void seedRoad() {
    CostRoad item = new CostRoad();
    item.setValidDate("2026.06.01");
    item.setSupplier("TIIME DISPATCH");
    item.setLogYardNameAddress("44624");
    item.setCity("DUNDEE");
    item.setState("OH");
    item.setPor("COLUMBUS");
    item.setPol("NOR/NY");
    item.setBaseFreight(new BigDecimal("425"));
    item.setFsc(new BigDecimal("0.35"));
    item.setChassis(new BigDecimal("45"));
    item.setOwTriAxle(new BigDecimal("200"));
    item.setSplit(new BigDecimal("85"));
    item.setStopOff(new BigDecimal("250"));
    item.setAllIn(new BigDecimal("888.75"));
    item.setAllInNonOak(new BigDecimal("1288.75"));
    item.setAllInOak(new BigDecimal("1378.75"));
    item.setWaitingFee(new BigDecimal("90"));
    item.setRedelivery(new BigDecimal("300"));
    item.setPrepull(new BigDecimal("150"));
    item.setNsLift(new BigDecimal("150"));
    item.setRemark("容易产生额外费用");
    item.touch();
    roadRepository.save(item);
  }

  private void seedSea() {
    CostSea item = new CostSea();
    item.setOrigin("上海港");
    item.setDestination("洛杉矶");
    item.setCarrier("COSCO 中远海运");
    item.setSpec("40HQ");
    item.setUnit("箱");
    item.setUnitPrice(new BigDecimal("3200"));
    item.setCurrency("USD");
    item.setValidFrom(LocalDate.parse("2026-03-01"));
    item.setValidTo(LocalDate.parse("2026-03-31"));
    item.setStatus(CostStatus.active);
    item.setRemark("含 THC，不含拖车费");
    item.touch();
    seaRepository.save(item);
  }

  private void seedFumigation() {
    CostFumigation item = new CostFumigation();
    item.setPort("CHICAGO");
    item.setStation("EFM");
    item.setNonOakOutdoor(new BigDecimal("800"));
    item.setNonOakIndoor(new BigDecimal("900"));
    item.setNonOakQuoteSummer("850");
    item.setNonOakQuoteWinter("850+100");
    item.setOakOutdoor(new BigDecimal("1480"));
    item.setOakIndoor(new BigDecimal("1580"));
    item.setOakQuoteSummer("1550");
    item.setOakQuoteWinter("1680+100");
    item.setRemark("样例数据");
    item.touch();
    fumigationRepository.save(item);
  }
}
