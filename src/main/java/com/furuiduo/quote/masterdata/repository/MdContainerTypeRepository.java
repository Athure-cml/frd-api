package com.furuiduo.quote.masterdata.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.masterdata.entity.MdContainerType;

public interface MdContainerTypeRepository extends JpaRepository<MdContainerType, Long> {

  boolean existsByCode(String code);

  Optional<MdContainerType> findByCode(String code);

  @Query(
      """
      SELECT t FROM MdContainerType t
      WHERE (:code = '' OR UPPER(t.code) LIKE UPPER(CONCAT('%', :code, '%')))
        AND (:name = '' OR UPPER(t.name) LIKE UPPER(CONCAT('%', :name, '%')))
      ORDER BY t.sort ASC, t.code ASC
      """)
  List<MdContainerType> search(@Param("code") String code, @Param("name") String name);

  List<MdContainerType> findByStatusOrderBySortAscCodeAsc(Integer status);
}
