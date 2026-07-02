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
      WHERE (:code = '' OR UPPER(s.code) LIKE UPPER(CONCAT('%', :code, '%')))
        AND (:nameZh = '' OR s.nameZh LIKE CONCAT('%', :nameZh, '%'))
      ORDER BY s.code ASC
      """)
  List<MdUsState> search(@Param("code") String code, @Param("nameZh") String nameZh);

  @Query(
      """
      SELECT DISTINCT s FROM MdUsState s, MdDestCity c, MdDestZip z
      WHERE c.stateId = s.id
        AND z.cityId = c.id
        AND (:stateCode = '' OR UPPER(s.code) = UPPER(:stateCode))
        AND (
          :keyword = ''
          OR UPPER(s.code) LIKE CONCAT('%', UPPER(:keyword), '%')
          OR UPPER(c.name) LIKE UPPER(CONCAT('%', :keyword, '%'))
          OR UPPER(z.zipCode) LIKE UPPER(CONCAT('%', :keyword, '%'))
        )
      ORDER BY s.code ASC
      """)
  List<MdUsState> findForDestAddressTree(
      @Param("stateCode") String stateCode, @Param("keyword") String keyword);
}
