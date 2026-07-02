package com.furuiduo.quote.sys.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 128)
  private String passwordHash;

  @Column(name = "password_strength")
  private Integer passwordStrength;

  @Column(name = "password_updated_at")
  private LocalDateTime passwordUpdatedAt;

  @Column(name = "real_name", nullable = false, length = 64)
  private String realName;

  @Column(length = 512)
  private String avatar;

  @Column(length = 20)
  private String phone;

  @Column(length = 128)
  private String email;

  @Column(name = "home_path", length = 128)
  private String homePath = "/workspace";

  @Column(nullable = false)
  private Integer status = 1;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dept_id", nullable = false)
  private SysDepartment department;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "sys_user_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<SysRole> roles = new HashSet<>();
}
