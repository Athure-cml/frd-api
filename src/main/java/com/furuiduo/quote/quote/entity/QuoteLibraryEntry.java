package com.furuiduo.quote.quote.entity;

import java.time.LocalDateTime;

import com.furuiduo.quote.cost.entity.CostHighlightMode;
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
@Table(name = "quote_library_entry")
public class QuoteLibraryEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "cost_mode", nullable = false, length = 16)
  private CostHighlightMode costMode;

  @Column(name = "cost_id", nullable = false)
  private Long costId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promoted_by")
  private SysUser promotedBy;

  @Column(name = "promoted_at", nullable = false)
  private LocalDateTime promotedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (promotedAt == null) {
      promotedAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
