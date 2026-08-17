package com.furuiduo.quote.supplier.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "supplier")
public class Supplier {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "short_name", length = 64)
  private String shortName;

  /** TRUCK / FUMIGATION / YARD / OTHER */
  @Column(nullable = false, length = 32)
  private String category = "TRUCK";

  /** 其他供应商：类型 ID 字符串列表 */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "types", nullable = false)
  private List<String> types = new ArrayList<>();

  @Column(name = "contact_name", length = 64)
  private String contactName;

  @Column(length = 64)
  private String phone;

  @Column(length = 128)
  private String email;

  @Column(length = 512)
  private String remark;

  @Column(name = "non_fumigation_package_formula", columnDefinition = "TEXT")
  private String nonFumigationPackageFormula;

  @Column(name = "fumigation_non_oak_package_formula", columnDefinition = "TEXT")
  private String fumigationNonOakPackageFormula;

  @Column(name = "fumigation_oak_package_formula", columnDefinition = "TEXT")
  private String fumigationOakPackageFormula;

  @Column(nullable = false)
  private Integer status = 1;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "created_by_name", length = 64)
  private String createdByName;

  @Column(name = "dept_id")
  private Long deptId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @Column(name = "pinned_at")
  private LocalDateTime pinnedAt;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;
}
