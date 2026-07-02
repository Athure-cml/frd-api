package com.furuiduo.quote.sys.entity;

import java.time.LocalDateTime;

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
@Table(name = "sys_operation_log")
public class SysOperationLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id")
  private Long userId;

  @Column(length = 64)
  private String username;

  @Column(name = "real_name", length = 64)
  private String realName;

  @Column(length = 64)
  private String module;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private OperationAction action;

  @Column(name = "resource_type", length = 64)
  private String resourceType;

  @Column(name = "resource_id", length = 64)
  private String resourceId;

  @Column(length = 256)
  private String summary;

  @Column(name = "request_method", length = 8)
  private String requestMethod;

  @Column(name = "request_uri", length = 256)
  private String requestUri;

  @Column(name = "request_body", columnDefinition = "TEXT")
  private String requestBody;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(nullable = false)
  private Boolean success = true;

  @Column(name = "error_message", length = 512)
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
