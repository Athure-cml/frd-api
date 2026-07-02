package com.furuiduo.quote.sys.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.furuiduo.quote.sys.entity.SysUser;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {

  @EntityGraph(attributePaths = {"department", "roles", "roles.permissions"})
  Optional<SysUser> findWithDetailsByUsername(String username);

  @EntityGraph(attributePaths = {"department", "roles", "roles.permissions"})
  Optional<SysUser> findWithDetailsById(Long id);

  @EntityGraph(attributePaths = {"department", "roles"})
  @Query("select u from SysUser u order by u.id")
  List<SysUser> findAllWithDetails();

  boolean existsByUsername(String username);

  long countByDepartment_Id(Long departmentId);

  long countByRoles_Code(String roleCode);
}
