package com.furuiduo.quote.masterdata.entity;

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
@Table(name = "md_global_port")
public class MdGlobalPort {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 8)
  private String code;

  @Column(name = "name_en", nullable = false, length = 128)
  private String nameEn;

  @Column(name = "name_zh", length = 128)
  private String nameZh;

  @Column(length = 64)
  private String route;

  @Column(name = "country_region", length = 128)
  private String countryRegion;

  @Enumerated(EnumType.STRING)
  @Column(name = "port_type", length = 16)
  private PortType portType;

  @Column(name = "function_code", length = 16)
  private String functionCode;

  @Column(name = "locode_status", length = 8)
  private String locodeStatus;

  @Column(name = "data_version", length = 32)
  private String dataVersion;
}
