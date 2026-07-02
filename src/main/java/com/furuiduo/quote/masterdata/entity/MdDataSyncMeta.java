package com.furuiduo.quote.masterdata.entity;

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
@Table(name = "md_data_sync_meta")
public class MdDataSyncMeta {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String syncKey;

  @Column(name = "data_version", length = 32)
  private String dataVersion;

  @Column(name = "last_sync_at")
  private LocalDateTime lastSyncAt;

  @Column(name = "total_records")
  private Integer totalRecords;

  @Column(name = "inserted_count")
  private Integer insertedCount;

  @Column(name = "updated_count")
  private Integer updatedCount;

  @Column(length = 512)
  private String remark;
}
