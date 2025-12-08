package com.core.data_pipeline_platform.domain.parse.repository;

import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParsedDataRepository extends JpaRepository<ParsedDataEntity, Long> {
    Optional<ParsedDataEntity> findByFileId(Long fileId);
}
