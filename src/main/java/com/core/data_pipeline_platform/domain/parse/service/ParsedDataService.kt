package com.core.data_pipeline_platform.domain.parse.service

import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity
import com.core.data_pipeline_platform.domain.parse.repository.ParsedDataRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import lombok.RequiredArgsConstructor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.function.Supplier

@Service
@RequiredArgsConstructor
class ParsedDataService {
    private val parsedDataRepository: ParsedDataRepository? = null
    private val objectMapper: ObjectMapper? = null

    /**
     * 모든 파싱된 데이터 조회 (페이징)
     */
    fun getAllParsedData(pageable: Pageable): Page<ParsedDataEntity?> {
        return parsedDataRepository!!.findAll(pageable)
    }

    /**
     * 파일 ID로 파싱된 데이터 조회
     */
    fun getParsedDataByFileId(fileId: Long?): ParsedDataEntity {
        return parsedDataRepository!!.findByFileId(fileId)
            .orElseThrow(Supplier {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "파싱된 데이터를 찾을 수 없습니다."
                )
            })
    }

    /**
     * 파싱된 데이터의 JSON 데이터를 List<Map>으로 변환하여 반환
    </Map> */
    fun getParsedDataAsMap(fileId: Long?): MutableList<MutableMap<String?, Any?>?>? {
        val parsedData = getParsedDataByFileId(fileId)

        try {
            return objectMapper!!.readValue<MutableList<MutableMap<String?, Any?>?>?>(
                parsedData.data,
                object : TypeReference<MutableList<MutableMap<String?, Any?>?>?>() {})
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 변환 실패 : ${e.message}")
        }
    }

    /**
     * 파싱된 데이터 삭제
     */
    fun deleteParsedData(fileId: Long?) {
        val parsedData = getParsedDataByFileId(fileId)
        parsedDataRepository!!.delete(parsedData)
    }

    val parsedDataCount: Long
        /**
         * 파싱된 데이터 개수 조회
         */
        get() = parsedDataRepository!!.count()
}
