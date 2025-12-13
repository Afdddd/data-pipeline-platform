package com.core.data_pipeline_platform.domain.parse.dto

import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity

data class ParsedDataResponse(
    val id: Long,
    val fileId: Long,
    val fileName: String,
    val fileType: String,
    val data: List<Map<String, Any>>
) {
    companion object {
        fun from(entity: ParsedDataEntity, parsedData: List<Map<String, Any>>) : ParsedDataResponse {
            return ParsedDataResponse(
                id = entity.id,
                fileId = entity.file.id,
                fileName = entity.file.originName,
                fileType = entity.file.fileType.name,
                data = parsedData
            )
        }
    }
}
