package com.furuiduo.quote.masterdata.sync;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.furuiduo.quote.masterdata.dto.GlobalPortSyncResult;
import com.furuiduo.quote.masterdata.dto.GlobalPortSyncStatus;

@Component
public class GlobalPortSyncTaskHolder {

  private final AtomicReference<GlobalPortSyncStatus> status =
      new AtomicReference<>(GlobalPortSyncStatus.idle());

  public GlobalPortSyncStatus current() {
    return status.get();
  }

  public boolean isRunning() {
    return "RUNNING".equals(status.get().status());
  }

  public synchronized boolean tryStart() {
    if (isRunning()) {
      return false;
    }
    LocalDateTime startedAt = LocalDateTime.now();
    status.set(GlobalPortSyncStatus.running("LOADING", startedAt));
    return true;
  }

  public void markPhase(String phase) {
    GlobalPortSyncStatus current = status.get();
    if (!"RUNNING".equals(current.status())) {
      return;
    }
    status.set(GlobalPortSyncStatus.running(phase, current.startedAt()));
  }

  public void complete(GlobalPortSyncResult result) {
    GlobalPortSyncStatus current = status.get();
    status.set(GlobalPortSyncStatus.completed(result, current.startedAt()));
  }

  public void fail(String message) {
    GlobalPortSyncStatus current = status.get();
    status.set(GlobalPortSyncStatus.failed(message, current.startedAt()));
  }

  public void resetToIdle() {
    status.set(GlobalPortSyncStatus.idle());
  }

  public synchronized void forceReset() {
    status.set(GlobalPortSyncStatus.idle());
  }
}
