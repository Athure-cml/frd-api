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
      (:quoteNo = '' OR UPPER(q.quoteNo) LIKE UPPER(CONCAT('%', :quoteNo, '%')))
      AND (:customerName = '' OR UPPER(q.customerName) LIKE UPPER(CONCAT('%', :customerName, '%')))
      AND (:transportMode IS NULL OR q.transportMode = :transportMode)
      AND (:status IS NULL OR q.status = :status)
      AND (:zipCode = '' OR UPPER(q.zipCode) LIKE UPPER(CONCAT('%', :zipCode, '%')))
      AND (:city = '' OR UPPER(q.city) LIKE UPPER(CONCAT('%', :city, '%')))
      AND (:state = '' OR UPPER(TRIM(q.state)) = UPPER(:state))
      AND (:por = '' OR UPPER(TRIM(q.por)) = UPPER(:por))
      AND (:pol = '' OR UPPER(TRIM(q.pol)) = UPPER(:pol))
      AND (:pod = '' OR UPPER(TRIM(q.pod)) = UPPER(:pod))
      AND (:ssl = '' OR UPPER(TRIM(q.ssl)) LIKE UPPER(CONCAT('%', :ssl, '%')))
      AND (:followUpByName = '' OR UPPER(q.followUpByName) LIKE UPPER(CONCAT('%', :followUpByName, '%')))
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
