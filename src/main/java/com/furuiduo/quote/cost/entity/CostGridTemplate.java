package com.furuiduo.quote.cost.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;

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
@Table(name = "cost_table_template")
public class CostGridTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 16)
  private String mode;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "is_default", nullable = false)
  private boolean defaultTemplate;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private CostTableTemplateLayout layout;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now();

  public void touch() {
    updatedAt = LocalDateTime.now();
  }
}
