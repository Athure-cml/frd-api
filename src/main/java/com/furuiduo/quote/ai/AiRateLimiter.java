package com.furuiduo.quote.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AiRateLimiter {

  private final int limitPerMinute;
  private final Map<Long, Deque<Long>> windows = new ConcurrentHashMap<>();

  public AiRateLimiter(
      @Value("${quote.ai.rate-limit-per-minute:20}") int limitPerMinute) {
    this.limitPerMinute = Math.max(limitPerMinute, 1);
  }

  public void check(Long userId) {
    long now = System.currentTimeMillis();
    long cutoff = now - 60_000L;
    Deque<Long> q = windows.computeIfAbsent(userId, id -> new ArrayDeque<>());
    synchronized (q) {
      while (!q.isEmpty() && q.peekFirst() < cutoff) {
        q.pollFirst();
      }
      if (q.size() >= limitPerMinute) {
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI 调用过于频繁，请稍后再试");
      }
      q.addLast(now);
    }
  }
}
