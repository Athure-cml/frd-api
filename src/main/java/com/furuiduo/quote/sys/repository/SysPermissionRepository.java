package com.furuiduo.quote.sys.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furuiduo.quote.sys.entity.SysPermission;

public interface SysPermissionRepository extends JpaRepository<SysPermission, Long> {

  Optional<SysPermission> findByCode(String code);
}
