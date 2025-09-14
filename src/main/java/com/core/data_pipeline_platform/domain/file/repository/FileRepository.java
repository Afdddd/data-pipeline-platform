package com.core.data_pipeline_platform.domain.file.repository;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    boolean existsByOriginName(String originName);
}
