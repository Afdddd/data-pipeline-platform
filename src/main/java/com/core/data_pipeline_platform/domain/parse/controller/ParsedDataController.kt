package com.core.data_pipeline_platform.domain.parse.controller

import com.core.data_pipeline_platform.domain.parse.dto.ParsedDataResponse
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity
import com.core.data_pipeline_platform.domain.parse.service.ParsedDataService
import lombok.RequiredArgsConstructor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/parsed-data")
@RequiredArgsConstructor
class ParsedDataController(
    private val parsedDataService: ParsedDataService
) {


    /**
     * 모든 파싱된 데이터 조회 (페이징)
     */
    @GetMapping
    fun getAllParsedData(pageable: Pageable?): ResponseEntity<Page<ParsedDataEntity?>?> {
        val parsedData = parsedDataService.getAllParsedData(pageable)
        return ResponseEntity.ok<Page<ParsedDataEntity?>?>(parsedData)
    }

    /**
     * 파일 ID로 파싱된 데이터 조회
     */
    @GetMapping("/{fileId}")
    fun getParsedDataByFileId(@PathVariable fileId: Long?): ResponseEntity<ParsedDataResponse?> {
        val entity = parsedDataService.getParsedDataByFileId(fileId)
        val data = parsedDataService.getParsedDataAsMap(fileId)

        val response: ParsedDataResponse = ParsedDataResponse.from(entity!!, data)
        return ResponseEntity.ok<ParsedDataResponse?>(response)
    }

    /**
     * 파싱된 데이터 삭제
     */
    @DeleteMapping("/{fileId}")
    fun deleteParsedData(@PathVariable fileId: Long?): ResponseEntity<Void?> {
        parsedDataService.deleteParsedData(fileId)
        return ResponseEntity.noContent().build<Void?>()
    }

    @get:GetMapping("/count")
    val parsedDataCount: ResponseEntity<Long?>
        /**
         * 파싱된 데이터 개수 조회
         */
        get() {
            val count = parsedDataService.parsedDataCount
            return ResponseEntity.ok<Long?>(count)
        }
}
