package com.furuiduo.quote.sys.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furuiduo.quote.sys.entity.SysDepartment;

public interface SysDepartmentRepository extends JpaRepository<SysDepartment, Long> {

  boolean existsByCode(String code);

  Optional<SysDepartment> findByCode(String code);
}
