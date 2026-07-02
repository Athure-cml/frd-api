package com.furuiduo.quote.currency.entity;

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
@Table(name = "currency")
public class Currency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 8)
  private String code;

  @Column(nullable = false, length = 64)
  private String name;

  @Column(length = 8)
  private String symbol;

  @Column(name = "decimal_places", nullable = false)
  private Integer decimalPlaces = 2;

  @Column(name = "is_base", nullable = false)
  private Boolean base = false;

  @Column(nullable = false)
  private Integer sort = 0;

  @Column(nullable = false)
  private Integer status = 1;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
