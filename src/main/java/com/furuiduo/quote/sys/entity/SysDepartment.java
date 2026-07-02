package com.furuiduo.quote.sys.entity;

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
@Table(name = "sys_department")
public class SysDepartment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 32)
  private String code;

  @Column(nullable = false, length = 64)
  private String name;

  @Column(name = "parent_id", nullable = false)
  private Long parentId = 0L;

  @Column(nullable = false)
  private Integer sort = 0;

  @Column(nullable = false)
  private Integer status = 1;
}
