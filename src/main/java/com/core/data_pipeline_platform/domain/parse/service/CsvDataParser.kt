package com.core.data_pipeline_platform.domain.parse.service

import com.core.data_pipeline_platform.domain.file.enums.FileType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

@Service
class CsvDataParser : DataParser {
    override fun parseData(fileType: FileType, inputStream: InputStream): MutableList<MutableMap<String, Any>> {
        if (fileType != FileType.CSV) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 타입 불일치: CSV이어야 합니다.")
        }

        val records: MutableList<MutableMap<String, Any>> = ArrayList()
        val reader = BufferedReader(InputStreamReader(inputStream))

        try {
            val headerLine =
                reader.readLine() ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV 파일이 비어있습니다.")
            val regex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"
            val headers: Array<String?> = headerLine.split(regex.toRegex()).toTypedArray()

            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                val columns: Array<String?> = line!!.split(regex.toRegex()).toTypedArray()
                for (i in columns.indices) {
                    if (columns[i]!!.startsWith("\"") && columns[i]!!.endsWith("\"")) {
                        columns[i] = columns[i]!!.substring(1, columns[i]!!.length - 1)
                    }
                }

                if (columns.size != headers.size) {
                    continue
                }
                val record: MutableMap<String, Any> = HashMap()
                for (i in headers.indices) {
                    headers[i]?.let { columns[i]?.let { value -> record.put(it, value) } }
                }
                records.add(record)
            }
            return records
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CSV 파싱 실패 : ${e.message}")
        }
    }

    override val supportedFileType: FileType
        get() = FileType.CSV
}
