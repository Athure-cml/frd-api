package com.furuiduo.quote.quote.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.entity.QuoteTransportMode;

public interface QuoteOrderRepository extends JpaRepository<QuoteOrder, Long> {

  @EntityGraph(attributePaths = "lines")
  Optional<QuoteOrder> findWithLinesById(Long id);

  @Query(
      """
      SELECT q FROM QuoteOrder q WHERE
      (:quoteNo IS NULL OR :quoteNo = '' OR LOWER(q.quoteNo) LIKE LOWER(CONCAT('%', :quoteNo, '%')))
      AND (:customerName IS NULL OR :customerName = '' OR LOWER(q.customerName) LIKE LOWER(CONCAT('%', :customerName, '%')))
      AND (:transportMode IS NULL OR q.transportMode = :transportMode)
      AND (:status IS NULL OR q.status = :status)
      AND (:zipCode IS NULL OR :zipCode = '' OR LOWER(q.zipCode) LIKE LOWER(CONCAT('%', :zipCode, '%')))
      AND (:city IS NULL OR :city = '' OR LOWER(q.city) LIKE LOWER(CONCAT('%', :city, '%')))
      AND (:state IS NULL OR :state = '' OR UPPER(TRIM(q.state)) = UPPER(TRIM(:state)))
      AND (:por IS NULL OR :por = '' OR UPPER(TRIM(q.por)) = UPPER(TRIM(:por)))
      AND (:pol IS NULL OR :pol = '' OR UPPER(TRIM(q.pol)) = UPPER(TRIM(:pol)))
      AND (:pod IS NULL OR :pod = '' OR UPPER(TRIM(q.pod)) = UPPER(TRIM(:pod)))
      AND (:ssl IS NULL OR :ssl = '' OR UPPER(TRIM(q.ssl)) LIKE UPPER(CONCAT('%', TRIM(:ssl), '%')))
      AND (:followUpByName IS NULL OR :followUpByName = '' OR LOWER(q.followUpByName) LIKE LOWER(CONCAT('%', :followUpByName, '%')))
      AND (
        :scopeAll = TRUE OR
        (:scopeDept = TRUE AND q.deptId = :deptId) OR
        (:scopeSelf = TRUE AND q.createdBy = :userId)
      )
      """)
  Page<QuoteOrder> search(
      @Param("quoteNo") String quoteNo,
      @Param("customerName") String customerName,
      @Param("transportMode") QuoteTransportMode transportMode,
      @Param("status") QuoteStatus status,
      @Param("zipCode") String zipCode,
      @Param("city") String city,
      @Param("state") String state,
      @Param("por") String por,
      @Param("pol") String pol,
      @Param("pod") String pod,
      @Param("ssl") String ssl,
      @Param("followUpByName") String followUpByName,
      @Param("scopeAll") boolean scopeAll,
      @Param("scopeDept") boolean scopeDept,
      @Param("scopeSelf") boolean scopeSelf,
      @Param("deptId") Long deptId,
      @Param("userId") Long userId,
      Pageable pageable);

  @Query(
      "SELECT q.quoteNo FROM QuoteOrder q WHERE q.quoteNo LIKE :prefix ORDER BY q.quoteNo DESC")
  Page<String> findQuoteNosByPrefix(@Param("prefix") String prefix, Pageable pageable);

  long countByCustomerId(Long customerId);

  boolean existsByCurrency(String currency);
}
