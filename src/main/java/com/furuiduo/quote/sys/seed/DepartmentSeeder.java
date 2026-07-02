package com.furuiduo.quote.sys.seed;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.sys.entity.SysDepartment;
import com.furuiduo.quote.sys.repository.SysDepartmentRepository;

/** 为已有库补充部门主数据（新库由 {@link DataSeeder} 一次性写入）。 */
@Component
@Order(100)
public class DepartmentSeeder implements ApplicationRunner {

  private record DeptDef(String code, String name, int sort) {}

  private static final List<DeptDef> DEPARTMENTS =
      List.of(
          new DeptDef("CS", "客服部", 1),
          new DeptDef("DOC", "单证部", 2),
          new DeptDef("OPS", "海外操作部", 3),
          new DeptDef("BKG", "订舱部", 4),
          new DeptDef("FIN", "财务部", 5),
          new DeptDef("TECH", "技术部", 6),
          new DeptDef("GMO", "总经办", 7));

  private final SysDepartmentRepository departmentRepository;

  public DepartmentSeeder(SysDepartmentRepository departmentRepository) {
    this.departmentRepository = departmentRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (departmentRepository.count() == 0) {
      return;
    }

    for (DeptDef def : DEPARTMENTS) {
      departmentRepository
          .findByCode(def.code())
          .ifPresentOrElse(
              existing -> {
                if (!def.name().equals(existing.getName()) || existing.getSort() != def.sort()) {
                  existing.setName(def.name());
                  existing.setSort(def.sort());
                  departmentRepository.save(existing);
                }
              },
              () -> {
                SysDepartment department = new SysDepartment();
                department.setCode(def.code());
                department.setName(def.name());
                department.setSort(def.sort());
                departmentRepository.save(department);
              });
    }
  }
}
