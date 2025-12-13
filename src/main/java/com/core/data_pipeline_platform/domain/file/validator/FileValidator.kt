package com.core.data_pipeline_platform.domain.file.validator

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@Component
class FileValidator(@param:Value("\${spring.servlet.multipart.max-file-size}") private val maxFileSize: DataSize) {
    /**
     * 파일 업로드 기본 검증
     * @param file 업로드된 파일
     * @throws ResponseStatusException 검증 실패 시 BAD_REQUEST
     */
    fun validateFile(file: MultipartFile) {
        validateFileExists(file)
        validateFileName(file)
        validateFileSize(file)
        validateFileExtension(file)
    }

    /**
     * 파일 존재 여부 검증
     */
    private fun validateFileExists(file: MultipartFile) {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 비어있습니다.")
        }
    }

    /**
     * 파일명 검증
     */
    private fun validateFileName(file: MultipartFile) {
        val fileName = file.originalFilename
        if (fileName == null || fileName.trim { it <= ' ' }.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "파일명이 없습니다.")
        }
    }

    /**
     * 파일 크기 검증
     */
    private fun validateFileSize(file: MultipartFile) {
        if (file.size > maxFileSize.toBytes()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format(
                    "파일 크기가 제한을 초과합니다. 최대: %s, 현재: %s바이트",
                    maxFileSize.toMegabytes().toString() + "MB",
                    file.size
                )
            )
        }
    }

    /**
     * 파일 확장자 검증
     */
    private fun validateFileExtension(file: MultipartFile) {
        val fileName = file.originalFilename
        val idx = fileName!!.lastIndexOf('.')
        if (idx == -1) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "확장자가 없습니다.")
        }
    }
}
