package com.furuiduo.quote.masterdata.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.furuiduo.quote.masterdata.entity.MdDataSyncMeta;

public interface MdDataSyncMetaRepository extends JpaRepository<MdDataSyncMeta, Long> {

  Optional<MdDataSyncMeta> findBySyncKey(String syncKey);
}
