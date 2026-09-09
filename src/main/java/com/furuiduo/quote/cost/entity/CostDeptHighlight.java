package com.furuiduo.quote.cost.entity;

import java.time.LocalDateTime;

import com.furuiduo.quote.sys.entity.SysDepartment;
import com.furuiduo.quote.sys.entity.SysUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cost_dept_highlight")
public class CostDeptHighlight {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "dept_id", nullable = false)
  private SysDepartment department;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_mode", nullable = false, length = 16)
  private CostHighlightMode costMode;

  @Column(name = "cost_id", nullable = false)
  private Long costId;

  @Column(nullable = false, length = 16)
  private String color;

  @Column(length = 128)
  private String remark;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "marked_by", nullable = false)
  private SysUser markedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /** 超级管理员/系统管理员标记，业务部门可见 */
  @Column(name = "admin_shared", nullable = false)
  private boolean adminShared = false;

  @PrePersist
  void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
