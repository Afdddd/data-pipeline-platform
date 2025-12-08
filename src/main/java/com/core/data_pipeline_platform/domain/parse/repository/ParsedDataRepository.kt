package com.core.data_pipeline_platform.domain.parse.repository

import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ParsedDataRepository : JpaRepository<ParsedDataEntity?, Long?> {
    fun findByFileId(fileId: Long?): Optional<ParsedDataEntity?>?
}
