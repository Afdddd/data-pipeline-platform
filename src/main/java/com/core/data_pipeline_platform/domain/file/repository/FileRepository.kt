package com.core.data_pipeline_platform.domain.file.repository

import com.core.data_pipeline_platform.domain.file.entity.FileEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FileRepository : JpaRepository<FileEntity?, Long?> {
    fun existsByOriginName(originName: String?): Boolean
}
