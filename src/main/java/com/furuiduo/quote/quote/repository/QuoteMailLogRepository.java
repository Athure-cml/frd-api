package com.furuiduo.quote.quote.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.quote.entity.QuoteMailLog;

public interface QuoteMailLogRepository extends JpaRepository<QuoteMailLog, Long> {

  Optional<QuoteMailLog> findFirstByQuoteIdOrderByCreatedAtDescIdDesc(Long quoteId);

  List<QuoteMailLog> findByQuoteIdOrderByCreatedAtDescIdDesc(Long quoteId);

  boolean existsByQuoteIdAndStatus(Long quoteId, com.furuiduo.quote.quote.entity.QuoteMailStatus status);

  /**
   * 指定操作人（报价单创建人）名下，最新一条发信记录为 FAILED 的报价单，用于待办/通知。
   *
   * @return [quoteId, quoteNo, customerName, toEmail, errorMessage, failedAt]
   */
  @Query(
      value =
          """
          SELECT q.id,
                 q.quote_no,
                 q.customer_name,
                 l.to_email,
                 l.error_message,
                 l.finished_at
          FROM quote_mail_log l
          JOIN quote_order q ON q.id = l.quote_id
          WHERE l.status = 'FAILED'
            AND q.deleted_at IS NULL
            AND q.created_by = :operatorId
            AND l.id = (
              SELECT max(l2.id) FROM quote_mail_log l2 WHERE l2.quote_id = l.quote_id
            )
          ORDER BY l.created_at DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<Object[]> findLatestFailedByOperator(
      @Param("operatorId") Long operatorId, @Param("limit") int limit);

  /** 供定时任务兜底：把长时间停留在 SENDING 的记录标记为失败。 */
  List<QuoteMailLog> findByStatusAndStartedAtBefore(
      com.furuiduo.quote.quote.entity.QuoteMailStatus status, LocalDateTime before);
}
