package com.furuiduo.quote.sys.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_role")
public class SysRole {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String code;

  @Column(nullable = false, length = 64)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_scope", nullable = false, length = 16)
  private DataScope dataScope;

  @Column(nullable = false)
  private Integer status = 1;

  @Column(length = 255)
  private String remark;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "sys_role_permission",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id"))
  private Set<SysPermission> permissions = new HashSet<>();
}
