package com.furuiduo.quote.dashboard.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furuiduo.quote.dashboard.entity.SysUserNotificationState;

public interface SysUserNotificationStateRepository
    extends JpaRepository<SysUserNotificationState, Long> {

  Optional<SysUserNotificationState> findByUserIdAndNoticeId(Long userId, String noticeId);

  List<SysUserNotificationState> findByUserIdAndNoticeIdIn(
      Long userId, Collection<String> noticeIds);
}
