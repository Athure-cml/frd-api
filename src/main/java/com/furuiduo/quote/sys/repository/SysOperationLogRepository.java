package com.furuiduo.quote.sys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.furuiduo.quote.sys.entity.SysOperationLog;

public interface SysOperationLogRepository
    extends JpaRepository<SysOperationLog, Long>, JpaSpecificationExecutor<SysOperationLog> {}
