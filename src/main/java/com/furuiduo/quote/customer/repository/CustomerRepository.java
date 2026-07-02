package com.furuiduo.quote.customer.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.customer.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  boolean existsByCode(String code);

  @Query(
      """
      SELECT c FROM Customer c WHERE
      (:code IS NULL OR :code = '' OR LOWER(c.code) LIKE LOWER(CONCAT('%', :code, '%')))
      AND (:name IS NULL OR :name = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
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
