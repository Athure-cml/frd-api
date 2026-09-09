package com.furuiduo.quote.sys.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@IdClass(SysAnnouncementReadId.class)
@Table(name = "sys_announcement_read")
public class SysAnnouncementRead {

  @Id
  @Column(name = "announcement_id")
  private Long announcementId;

  @Id
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "read_at", nullable = false)
  private LocalDateTime readAt;

  @PrePersist
  void onCreate() {
    if (readAt == null) {
      readAt = LocalDateTime.now();
    }
  }
}
