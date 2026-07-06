package com.furuiduo.quote.cost.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
@Table(name = "cost_road")
public class CostRoad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "valid_date", length = 32)
  private String validDate;

  @Column(length = 128)
  private String supplier;

  @Column(name = "log_yard_name_address", length = 512)
  private String logYardNameAddress;

  @Column(name = "zip_code", length = 32)
  private String zipCode;

  @Column(length = 64)
  private String city;

  @Column(length = 32)
  private String state;

  @Column(length = 64)
  private String por;

  @Column(length = 64)
  private String pol;

  @Column(name = "base_freight", precision = 14, scale = 2)
  private BigDecimal baseFreight;

  @Column(precision = 8, scale = 4)
  private BigDecimal fsc;

  @Column(precision = 14, scale = 2)
  private BigDecimal chassis;

  @Column(name = "ow_tri_axle", precision = 14, scale = 2)
  private BigDecimal owTriAxle;

  @Column(precision = 14, scale = 2)
  private BigDecimal split;

  @Column(name = "stop_off", precision = 14, scale = 2)
  private BigDecimal stopOff;

  @Column(name = "all_in", precision = 14, scale = 2)
  private BigDecimal allIn;

  @Column(name = "all_in_non_oak", precision = 14, scale = 2)
  private BigDecimal allInNonOak;

  @Column(name = "all_in_oak", precision = 14, scale = 2)
  private BigDecimal allInOak;

  @Column(name = "waiting_fee", precision = 14, scale = 2)
  private BigDecimal waitingFee;

  @Column(precision = 14, scale = 2)
  private BigDecimal redelivery;

  @Column(precision = 14, scale = 2)
  private BigDecimal prepull;

  @Column(name = "ns_lift", precision = 14, scale = 2)
  private BigDecimal nsLift;

  @Column(length = 512)
  private String remark;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extra_fields")
  private Map<String, Object> extraFields = new HashMap<>();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now();

  public void touch() {
    updatedAt = LocalDateTime.now();
  }
}
