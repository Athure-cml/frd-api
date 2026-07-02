package com.furuiduo.quote.sys.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.furuiduo.quote.sys.entity.SysRole;

public interface SysRoleRepository extends JpaRepository<SysRole, Long> {

  @EntityGraph(attributePaths = {"permissions"})
  Optional<SysRole> findWithPermissionsByCode(String code);

  @EntityGraph(attributePaths = {"permissions"})
  @Query("select r from SysRole r order by r.id")
  List<SysRole> findAllWithPermissions();

  @EntityGraph(attributePaths = {"permissions"})
  @Query("select r from SysRole r where r.id = :id")
  Optional<SysRole> findWithPermissionsById(Long id);

  Optional<SysRole> findByCode(String code);
}
