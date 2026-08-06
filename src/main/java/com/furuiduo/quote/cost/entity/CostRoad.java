package com.furuiduo.quote.cost.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @Column(name = "zip_code", length = 32)
  private String zipCode;

  @Column(length = 128)
  private String city;

  @Column(length = 32)
  private String state;

  /** 接货地（美国城市） */
  @Column(length = 128)
  private String por;

  /** 装货港 */
  @Column(length = 128)
  private String pol;

  @Column(length = 128)
  private String supplier;

  @Column(name = "base_freight", precision = 14, scale = 2)
  private BigDecimal baseFreight;

  @Column(precision = 14, scale = 2)
  private BigDecimal fsc;

  @Column(precision = 14, scale = 2)
  private BigDecimal chassis;

  @Column(name = "tri_tandem_axle", precision = 14, scale = 2)
  private BigDecimal triTandemAxle;

  @Column(precision = 14, scale = 2)
  private BigDecimal split;

  @Column(name = "stop_off", precision = 14, scale = 2)
  private BigDecimal stopOff;

  @Column(name = "all_in_no_fm", precision = 14, scale = 2)
  private BigDecimal allInNoFm;

  @Column(name = "all_in_fm_one_way", precision = 14, scale = 2)
  private BigDecimal allInFmOneWay;

  @Column(name = "all_in_fm_round", precision = 14, scale = 2)
  private BigDecimal allInFmRound;

  @Column(name = "waiting_fee", precision = 14, scale = 2)
  private BigDecimal waitingFee;

  @Column(precision = 14, scale = 2)
  private BigDecimal redelivery;

  @Column(precision = 14, scale = 2)
  private BigDecimal prepull;

  @Column(name = "ns_lift", precision = 14, scale = 2)
  private BigDecimal nsLift;

  @Column(name = "other_fee", precision = 14, scale = 2)
  private BigDecimal otherFee;

  @Column(length = 512)
  private String remark;

  @Column(name = "valid_date", length = 64)
  private String validDate;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private CostStatus status = CostStatus.active;

  @Column(name = "log_yard_name_address", length = 512)
  private String logYardNameAddress;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extra_fields")
  private Map<String, Object> extraFields = new HashMap<>();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now();

  public void touch() {
    updatedAt = LocalDateTime.now();
  }
}
