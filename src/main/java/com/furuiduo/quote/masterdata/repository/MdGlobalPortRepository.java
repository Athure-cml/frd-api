package com.furuiduo.quote.masterdata.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.furuiduo.quote.masterdata.entity.MdGlobalPort;
import com.furuiduo.quote.masterdata.entity.PortType;

public interface MdGlobalPortRepository extends JpaRepository<MdGlobalPort, Long> {

  boolean existsByCode(String code);

  Optional<MdGlobalPort> findByCode(String code);

  List<MdGlobalPort> findByCodeIn(Collection<String> codes);

  @Query("SELECT UPPER(p.code) FROM MdGlobalPort p")
  List<String> findAllCodes();

  @Query(
      """
      SELECT p FROM MdGlobalPort p
      WHERE (:code IS NULL OR LOWER(p.code) LIKE LOWER(CONCAT('%', :code, '%')))
        AND (:nameEn IS NULL OR LOWER(p.nameEn) LIKE LOWER(CONCAT('%', :nameEn, '%')))
        AND (:nameZh IS NULL OR p.nameZh LIKE CONCAT('%', :nameZh, '%'))
        AND (:route IS NULL OR LOWER(p.route) LIKE LOWER(CONCAT('%', :route, '%')))
        AND (:countryRegion IS NULL OR LOWER(p.countryRegion) LIKE LOWER(CONCAT('%', :countryRegion, '%')))
        AND (:portType IS NULL OR p.portType = :portType)
      ORDER BY p.code ASC
      """)
  List<MdGlobalPort> search(
      @Param("code") String code,
      @Param("nameEn") String nameEn,
      @Param("nameZh") String nameZh,
      @Param("route") String route,
      @Param("countryRegion") String countryRegion,
      @Param("portType") PortType portType);
}
