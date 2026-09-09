package com.furuiduo.quote.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furuiduo.quote.sys.entity.SysAnnouncementRead;
import com.furuiduo.quote.sys.entity.SysAnnouncementReadId;

public interface SysAnnouncementReadRepository
    extends JpaRepository<SysAnnouncementRead, SysAnnouncementReadId> {

  long countByAnnouncementId(Long announcementId);
}
