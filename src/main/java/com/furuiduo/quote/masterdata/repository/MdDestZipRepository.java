package com.furuiduo.quote.masterdata.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.masterdata.dto.DestAddressRowResponse;
import com.furuiduo.quote.masterdata.entity.MdDestZip;

public interface MdDestZipRepository extends JpaRepository<MdDestZip, Long> {

  @Query(
      value =
          """
          SELECT new com.furuiduo.quote.masterdata.dto.DestAddressRowResponse(
            z.id, s.code, s.id, c.name, c.id, z.zipCode)
          FROM MdDestZip z, MdDestCity c, MdUsState s
          WHERE c.id = z.cityId
            AND s.id = c.stateId
            AND (:stateCode = '' OR UPPER(s.code) = UPPER(:stateCode))
            AND (
              :keyword = ''
              OR UPPER(s.code) LIKE CONCAT('%', UPPER(:keyword), '%')
              OR UPPER(c.name) LIKE UPPER(CONCAT('%', :keyword, '%'))
              OR UPPER(z.zipCode) LIKE UPPER(CONCAT('%', :keyword, '%'))
            )
          ORDER BY s.code ASC, c.name ASC, z.zipCode ASC
          """,
      countQuery =
          """
          SELECT COUNT(z)
          FROM MdDestZip z, MdDestCity c, MdUsState s
          WHERE c.id = z.cityId
            AND s.id = c.stateId
            AND (:stateCode = '' OR UPPER(s.code) = UPPER(:stateCode))
            AND (
              :keyword = ''
              OR UPPER(s.code) LIKE CONCAT('%', UPPER(:keyword), '%')
              OR UPPER(c.name) LIKE UPPER(CONCAT('%', :keyword, '%'))
              OR UPPER(z.zipCode) LIKE UPPER(CONCAT('%', :keyword, '%'))
            )
          """)
  Page<DestAddressRowResponse> searchRows(
      @Param("stateCode") String stateCode,
      @Param("keyword") String keyword,
      Pageable pageable);

  @Query(
      value =
          """
          SELECT new com.furuiduo.quote.masterdata.dto.DestAddressRowResponse(
            z.id, s.code, s.id, c.name, c.id, z.zipCode)
          FROM MdDestZip z, MdDestCity c, MdUsState s
          WHERE c.id = z.cityId
            AND s.id = c.stateId
            AND UPPER(z.zipCode) LIKE UPPER(CONCAT(:keyword, '%'))
          ORDER BY z.zipCode ASC, c.name ASC
          """,
      countQuery =
          """
          SELECT COUNT(z)
          FROM MdDestZip z
          WHERE UPPER(z.zipCode) LIKE UPPER(CONCAT(:keyword, '%'))
          """)
  Page<DestAddressRowResponse> searchRowsByZipPrefix(
      @Param("keyword") String keyword, Pageable pageable);

  @Query(
      """
      SELECT z FROM MdDestZip z
      WHERE z.cityId = :cityId
        AND (
          :keyword = ''
          OR UPPER(z.zipCode) LIKE UPPER(CONCAT('%', :keyword, '%'))
        )
      ORDER BY z.zipCode ASC
      """)
  List<MdDestZip> findByCityIdWithKeyword(
      @Param("cityId") Long cityId, @Param("keyword") String keyword);

  List<MdDestZip> findByCityIdOrderByZipCodeAsc(Long cityId);

  List<MdDestZip> findAllByOrderByCityIdAscZipCodeAsc();

  Optional<MdDestZip> findByCityIdAndZipCodeIgnoreCase(Long cityId, String zipCode);

  void deleteByCityId(Long cityId);

  boolean existsByCityId(Long cityId);

  @Query("SELECT z.cityId, LOWER(z.zipCode) FROM MdDestZip z")
  List<Object[]> findExistingZipKeys();
}
