package com.core.data_pipeline_platform.domain.parse.service

import com.core.data_pipeline_platform.domain.file.enums.FileType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

@Service
class BinDataParser : DataParser {
    override fun parseData(fileType: FileType, inputStream: InputStream): MutableList<MutableMap<String, Any>> {
        if (fileType != FileType.BIN) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 타입 불일치: BIN이어야 합니다.")
        }

        val records: MutableList<MutableMap<String, Any>> = ArrayList()

        try {
            val dataStream = DataInputStream(inputStream)
            val recordCount = dataStream.readInt()

            if (recordCount < 0) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 레코드 수")
            }
            for (i in 0..<recordCount) {
                val record: MutableMap<String, Any> = HashMap()
                record[SENSOR_ID] = readString(dataStream)
                record[VALUE] = dataStream.readDouble()
                record[TIMESTAMP] = readString(dataStream)
                record[STATUS] = readString(dataStream)
                records.add(record)
            }

            return records
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "BIN 파일 파싱 실패 : ${e.message}")
        }
    }

    override val supportedFileType: FileType
        get() = FileType.BIN

    @Throws(IOException::class)
    private fun readString(dataStream: DataInputStream): String {
        val length = dataStream.readInt()
        if (length == 0) return ""
        val bytes = ByteArray(length)
        dataStream.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    companion object {
        private const val SENSOR_ID = "sensorId"
        private const val VALUE = "value"
        private const val TIMESTAMP = "timestamp"
        private const val STATUS = "status"
    }
}
