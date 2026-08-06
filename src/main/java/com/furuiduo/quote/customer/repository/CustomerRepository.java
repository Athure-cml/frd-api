package com.furuiduo.quote.customer.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.customer.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  boolean existsByCode(String code);

  Optional<Customer> findByCode(String code);

  @Query(
      """
      SELECT c FROM Customer c
      WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name))
      """)
  Optional<Customer> findByNameNormalized(@Param("name") String name);

  @Query(
      """
      SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c
      WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name))
      AND (:excludeId IS NULL OR c.id <> :excludeId)
      """)
  boolean existsByNameNormalized(
      @Param("name") String name, @Param("excludeId") Long excludeId);

  @Query(
      """
      SELECT c FROM Customer c WHERE
      (:code = '' OR UPPER(c.code) LIKE UPPER(CONCAT('%', :code, '%')))
      AND (:name = '' OR UPPER(c.name) LIKE UPPER(CONCAT('%', :name, '%')))
      AND (:status IS NULL OR c.status = :status)
      """)
  Page<Customer> search(
      @Param("code") String code,
      @Param("name") String name,
      @Param("status") Integer status,
      Pageable pageable);

  @Query("SELECT c.code FROM Customer c WHERE c.code LIKE :prefix ORDER BY c.code DESC")
  org.springframework.data.domain.Page<String> findCustomerCodesByPrefix(
      @Param("prefix") String prefix, Pageable pageable);
}
