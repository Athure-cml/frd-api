package com.furuiduo.quote.customer.entity;

import java.time.LocalDateTime;

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
@Table(name = "customer")
public class Customer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 32)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "contact_name", length = 64)
  private String contactName;

  @Column(length = 32)
  private String phone;

  @Column(length = 128)
  private String email;

  @Column(length = 256)
  private String address;

  @Column(length = 512)
  private String remark;

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
}
