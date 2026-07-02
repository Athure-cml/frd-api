package com.furuiduo.quote.masterdata.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.masterdata.entity.MdUsState;

public interface MdUsStateRepository extends JpaRepository<MdUsState, Long> {

  boolean existsByCode(String code);

  Optional<MdUsState> findByCode(String code);

  @Query(
      """
      SELECT s FROM MdUsState s
      WHERE (:code IS NULL OR LOWER(s.code) LIKE LOWER(CONCAT('%', :code, '%')))
        AND (:nameZh IS NULL OR s.nameZh LIKE CONCAT('%', :nameZh, '%'))
      ORDER BY s.code ASC
      """)
  List<MdUsState> search(@Param("code") String code, @Param("nameZh") String nameZh);

  @Query(
      """
      SELECT DISTINCT s FROM MdUsState s
      JOIN MdDestCity c ON c.stateId = s.id
      LEFT JOIN MdDestZip z ON z.cityId = c.id
      WHERE (:stateCode IS NULL OR TRIM(:stateCode) = '' OR UPPER(s.code) = UPPER(:stateCode))
        AND (
          :keyword IS NULL OR TRIM(:keyword) = ''
          OR UPPER(s.code) LIKE CONCAT('%', UPPER(:keyword), '%')
          OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(z.zipCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
      ORDER BY s.code ASC
      """)
  List<MdUsState> findForDestAddressTree(
      @Param("stateCode") String stateCode, @Param("keyword") String keyword);
}
