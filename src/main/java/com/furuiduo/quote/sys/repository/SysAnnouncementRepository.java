package com.furuiduo.quote.sys.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.sys.entity.AnnouncementStatus;
import com.furuiduo.quote.sys.entity.SysAnnouncement;

public interface SysAnnouncementRepository extends JpaRepository<SysAnnouncement, Long> {

  @Query(
      """
      SELECT a FROM SysAnnouncement a
      LEFT JOIN FETCH a.createdBy
      WHERE a.status IN :activeStatuses
        AND a.publishedAt IS NOT NULL
        AND a.publishedAt <= :now
        AND (a.expiresAt IS NULL OR a.expiresAt > :now)
        AND NOT EXISTS (
          SELECT 1 FROM SysAnnouncementRead r
          WHERE r.announcementId = a.id AND r.userId = :userId
        )
      ORDER BY a.publishedAt ASC, a.id ASC
      """)
  List<SysAnnouncement> findPendingForUser(
      @Param("userId") Long userId,
      @Param("now") LocalDateTime now,
      @Param("activeStatuses") List<AnnouncementStatus> activeStatuses);

  @Query(
      """
      SELECT a FROM SysAnnouncement a
      LEFT JOIN FETCH a.createdBy
      WHERE a.status = com.furuiduo.quote.sys.entity.AnnouncementStatus.SCHEDULED
        AND a.publishedAt IS NOT NULL
        AND a.publishedAt <= :now
      """)
  List<SysAnnouncement> findDueScheduled(@Param("now") LocalDateTime now);

  @Query(
      """
      SELECT a FROM SysAnnouncement a
      LEFT JOIN FETCH a.createdBy
      ORDER BY a.updatedAt DESC, a.id DESC
      """)
  List<SysAnnouncement> findAllWithCreatorOrderByUpdatedAtDesc();

  @Query(
      """
      SELECT a FROM SysAnnouncement a
      LEFT JOIN FETCH a.createdBy
      WHERE a.id = :id
      """)
  Optional<SysAnnouncement> findByIdWithCreator(@Param("id") Long id);
}
