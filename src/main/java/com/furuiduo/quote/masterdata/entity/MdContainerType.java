package com.furuiduo.quote.masterdata.entity;

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
@Table(name = "md_container_type")
public class MdContainerType {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 32)
  private String code;

  @Column(nullable = false, length = 64)
  private String name;

  @Column(nullable = false)
  private Integer sort = 0;

  /** 1=启用 0=停用 */
  @Column(nullable = false)
  private Integer status = 1;

  @Column(length = 256)
  private String remark;
}
