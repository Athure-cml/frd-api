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

  /** cost：成本库；quote：报价库。两套模板不共用。 */
  @Column(length = 16)
  private String library = LIBRARY_COST;

  @Column(nullable = false, length = 16)
  private String mode;

  public static final String LIBRARY_COST = "cost";
  public static final String LIBRARY_QUOTE = "quote";

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "is_default", nullable = false)
  private boolean defaultTemplate;

  /** legacy：旧版锁定模板，模板管理隐藏，仅历史快照展示用 */
  @Column(nullable = false)
  private boolean legacy = false;

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
